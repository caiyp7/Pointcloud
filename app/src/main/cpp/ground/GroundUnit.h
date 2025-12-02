#pragma once
#include <cstdint>
#include <vector>
#include <netinet/in.h>

class GroundUnit {
public:
    // 在 JNI 线程里调用：start -> 循环调用 loopOnce -> stop
    void start(int udpRecvPort);
    void loopOnce();        // 每次处理一批数据；阻塞/非阻塞都可
    void stop();

private:
    int   recv_fd_   = -1;
    int   send_fd_   = -1;
    int   recv_port_ = 0;
    bool  running_   = false;

    sockaddr_in send_addr_{};
};
