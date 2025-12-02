#include "GroundUnit.h"

// === C++ 标准库 ===
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>
#include <sstream>

// === POSIX / NDK 网络 ===
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>

// === Android 日志 ===
#ifdef __ANDROID__
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "GroundUnit", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "GroundUnit", __VA_ARGS__)
#else
#define LOGI(...) do{}while(0)
  #define LOGE(...) do{}while(0)
#endif

// === Draco（CMake 里已定义 HAVE_DRACO=1 且 add_subdirectory(draco)）===
#ifdef HAVE_DRACO
#include <draco/compression/decode.h>
#include <draco/core/decoder_buffer.h>
#include <draco/point_cloud/point_cloud.h>
#include <draco/attributes/point_attribute.h>
#endif

// 你的协议头（你已用它替代 packet_utils.hpp）
#pragma pack(push, 1)
struct UdpPacketHeader {
    float odom[3];          // 12B
    float quaternion[4];    // 16B
    float rcgoal[3];        // 12B
    uint32_t pointCloudSize;// 4B (network order)
    uint32_t frontierSize;  // 4B (network order)
};
#pragma pack(pop)
static_assert(sizeof(UdpPacketHeader) == 48, "UdpPacketHeader size mismatch");
static_assert(sizeof(float) == 4, "float must be 4 bytes");

// 目的地址：本机 127.0.0.1:14700（后续由 Kotlin WebSocket 转发）
static constexpr const char* RECE_IP  = "127.0.0.1";
static constexpr int   UDP_SEND_PORT = 14701;
static constexpr const char* DEST_IP  = "127.0.0.1";

#ifdef HAVE_DRACO
static std::vector<uint8_t> pointcloudToBytes(const draco::PointCloud& pc) {
    const draco::PointAttribute* pos = pc.GetNamedAttribute(draco::GeometryAttribute::POSITION);
    if (!pos || pos->data_type() != draco::DT_FLOAT32 || pos->num_components() != 3) {
        LOGE("Unsupported draco attribute");
        return {};
    }
    const size_t n = pc.num_points();
    std::vector<uint8_t> out(n * 3 * sizeof(float));
    auto* f = reinterpret_cast<float*>(out.data());
    for (draco::PointIndex i(0); i < draco::PointIndex(n); ++i) {
        draco::AttributeValueIndex idx = pos->mapped_index(i);
        float c[3]; pos->GetValue(idx, c);
        const size_t off = i.value() * 3;
        f[off+0] = c[0]; f[off+1] = c[1]; f[off+2] = c[2];
    }
    return out;
}
#endif

void GroundUnit::start(int udpRecvPort) {
    if (running_) return;
    recv_port_ = udpRecvPort;

    // 1) 接收 socket
    recv_fd_ = ::socket(AF_INET, SOCK_DGRAM, 0);
    if (recv_fd_ < 0) { LOGE("socket(recv) failed"); return; }

    sockaddr_in recv_addr{};
    recv_addr.sin_family = AF_INET;
    recv_addr.sin_port   = htons(static_cast<uint16_t>(recv_port_));
    recv_addr.sin_addr.s_addr = htonl(INADDR_ANY); // 监听所有地址

    if (::bind(recv_fd_,
               reinterpret_cast<sockaddr*>(&recv_addr),
               static_cast<socklen_t>(sizeof(recv_addr))) < 0) {
        LOGE("bind failed");
        ::close(recv_fd_); recv_fd_ = -1;
        return;
    }

    // （可选）非阻塞
    // int flags = fcntl(recv_fd_, F_GETFL, 0);
    // fcntl(recv_fd_, F_SETFL, flags | O_NONBLOCK);

    // 2) 发送 socket
    send_fd_ = ::socket(AF_INET, SOCK_DGRAM, 0);
    if (send_fd_ < 0) { LOGE("socket(send) failed"); ::close(recv_fd_); recv_fd_ = -1; return; }

    send_addr_ = {};
    send_addr_.sin_family = AF_INET;
    send_addr_.sin_port   = htons(static_cast<uint16_t>(UDP_SEND_PORT));
    ::inet_pton(AF_INET, RECE_IP, &send_addr_.sin_addr);

    running_ = true;
    LOGI("GroundUnit started. recv:%d  -> send:%s:%d", recv_port_, RECE_IP, UDP_SEND_PORT);
}

void GroundUnit::loopOnce() {
    if (!running_) { usleep(50*1000); return; }

    // 接收一个完整的 UDP 包
    uint8_t buffer[65536];
    const ssize_t n = ::recvfrom(recv_fd_, buffer, sizeof(buffer), 0, nullptr, nullptr);
    if (n <= 0) { usleep(5*1000); return; }

    if (n < static_cast<ssize_t>(sizeof(UdpPacketHeader))) {
        LOGE("packet too short: %zd", n);
        return;
    }

    // 解析头
    UdpPacketHeader hdr{};
    std::memcpy(&hdr, buffer, sizeof(hdr));

    const uint32_t pc_size       = ntohl(hdr.pointCloudSize);
    const uint32_t frontier_size = ntohl(hdr.frontierSize);
    const size_t expect = sizeof(hdr) + pc_size + frontier_size;
    if (static_cast<size_t>(n) < expect) {
        LOGE("length mismatch: expect=%zu, got=%zd", expect, n);
        return;
    }

    // === 调试输出 Header ===
    LOGI("Recv odom:       %.3f %.3f %.3f", hdr.odom[0], hdr.odom[1], hdr.odom[2]);
    LOGI("Recv quaternion: %.3f %.3f %.3f %.3f", hdr.quaternion[0], hdr.quaternion[1],
         hdr.quaternion[2], hdr.quaternion[3]);
    LOGI("Recv rcgoal:     %.3f %.3f %.3f", hdr.rcgoal[0], hdr.rcgoal[1], hdr.rcgoal[2]);
    LOGI("PointCloudSize=%u bytes, FrontierSize=%u bytes", pc_size, frontier_size);

    const uint8_t* pc_data       = buffer + sizeof(hdr);
    const uint8_t* frontier_data = pc_data + pc_size;

#ifdef HAVE_DRACO
    // Draco 解码
    draco::DecoderBuffer dbuf;
    dbuf.Init(reinterpret_cast<const char*>(pc_data), pc_size);
    draco::Decoder dec;
    auto res = dec.DecodePointCloudFromBuffer(&dbuf);
    if (!res.ok()) {
        LOGE("Draco decode failed");
        return;
    }
    std::unique_ptr<draco::PointCloud> pc = std::move(res).value();
    std::vector<uint8_t> pc_bytes = pointcloudToBytes(*pc);

    size_t num_points = pc_bytes.size() / (3 * sizeof(float));
    LOGI("Decoded points: total=%zu", num_points);
    const float* pf = reinterpret_cast<const float*>(pc_bytes.data());
    for (size_t i = 0; i < std::min<size_t>(5, num_points); ++i) {
        LOGI("pt[%zu] = %.3f %.3f %.3f", i, pf[i*3+0], pf[i*3+1], pf[i*3+2]);
    }
#else
    std::vector<uint8_t> pc_bytes; // 未启 Draco 时为空
#endif

    // frontier 直接转字符串（如需原样二进制，可保留为 vector<uint8_t>）
    std::string frontier(reinterpret_cast<const char*>(frontier_data), frontier_size);

    // 组新包（header + pointcloud bytes + frontier）
    UdpPacketHeader out{};
    for (int i=0;i<3;++i) out.odom[i] = hdr.odom[i];
    for (int i=0;i<4;++i) out.quaternion[i] = hdr.quaternion[i];
    for (int i=0;i<3;++i) out.rcgoal[i] = hdr.rcgoal[i];
    out.pointCloudSize = htonl(static_cast<uint32_t>(pc_bytes.size()));
    out.frontierSize   = htonl(static_cast<uint32_t>(frontier.size()));

    std::vector<uint8_t> pkt;
    pkt.resize(sizeof(UdpPacketHeader) + pc_bytes.size() + frontier.size());
    std::memcpy(pkt.data(), &out, sizeof(out));
    if (!pc_bytes.empty())
        std::memcpy(pkt.data()+sizeof(out), pc_bytes.data(), pc_bytes.size());
    if (!frontier.empty())
        std::memcpy(pkt.data()+sizeof(out)+pc_bytes.size(), frontier.data(), frontier.size());

    const ssize_t sent = ::sendto(
            send_fd_, pkt.data(), pkt.size(), 0,
            reinterpret_cast<sockaddr*>(&send_addr_), static_cast<socklen_t>(sizeof(send_addr_))
    );
    if (sent < 0) LOGE("sendto failed"); else LOGI("sent %zd bytes", sent);
}

void GroundUnit::stop() {
    if (!running_) return;
    running_ = false;
    if (recv_fd_ >= 0) { ::close(recv_fd_); recv_fd_ = -1; }
    if (send_fd_ >= 0) { ::close(send_fd_); send_fd_ = -1; }
    LOGI("GroundUnit stopped");
}
