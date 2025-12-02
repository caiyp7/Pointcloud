package com.example.pointcloud.net

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class UdpToWebSocketService : Service() {
    companion object {
        private const val TAG = "UdpToWS"
        private const val DECODED_UDP_PORT = 14701      // GroundUnit 发送的本地端口（你代码里写死的）
        private const val WS_PORT = 9000
        private const val HEADER_SIZE = 48               // 你的 UdpPacketHeader 固定 48B
        private const val USE_BINARY_FORMAT = true       // 使用二进制格式（true）或字符串格式（false）
    }

    private var running = false
    private lateinit var udpThread: Thread
    private lateinit var ws: LocalWs

    override fun onCreate() {
        super.onCreate()
        running = true
        startForegroundIfNeeded()
        ws = LocalWs(WS_PORT); ws.start()
        udpThread = Thread(::udpLoop, "udp-loop"); udpThread.start()
    }

    override fun onDestroy() {
        running = false
        try { ws.stop(1000) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundIfNeeded() {
        if (Build.VERSION.SDK_INT >= 26) {
            val id = "udp_ws"
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(id, "UDP→WS", NotificationManager.IMPORTANCE_MIN)
            )
            val n = Notification.Builder(this, id)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("UDP→WebSocket 正在运行")
                .build()
            startForeground(1004, n)
        }
    }

    // 2) 接收循环里加 try/catch，避免异常退出
    private fun udpLoop() {
        DatagramSocket(null).use { ds ->
            ds.reuseAddress = true
            ds.bind(InetSocketAddress(DECODED_UDP_PORT))
            val buf = ByteArray(65535)
            val pkt = DatagramPacket(buf, buf.size)
            Log.i(TAG, "UDP on $DECODED_UDP_PORT; WS on 127.0.0.1:$WS_PORT")

            while (running) {
                try {
                    ds.receive(pkt)
                    val len = pkt.length
                    if (len < HEADER_SIZE) continue
                    val msg = buf.copyOfRange(0, len)
                    if (USE_BINARY_FORMAT) {
                        buildBinaryFromDecoded(msg)?.let { ws.broadcastBinary(it) }
                    } else {
                        buildPcStrFromDecoded(msg)?.let { ws.broadcast(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "udp receive error", e)
                }
            }
        }
    }


    // 按照 BINARY_PROTOCOL.md 构建二进制格式数据
    private fun buildBinaryFromDecoded(msg: ByteArray): ByteArray? {
        return try {
            // --- 1) 解析原始 UDP header ---
            val le = ByteBuffer.wrap(msg).order(ByteOrder.LITTLE_ENDIAN)
            val odom = FloatArray(3) { le.getFloat(it * 4) }
            val quat = FloatArray(4) { le.getFloat((3 + it) * 4) }
            val rc   = FloatArray(3) { le.getFloat((7 + it) * 4) }

            val pcSize = ByteBuffer.wrap(msg, 40, 4).order(ByteOrder.BIG_ENDIAN).int
            val frSize = ByteBuffer.wrap(msg, 44, 4).order(ByteOrder.BIG_ENDIAN).int

            val pcOff = HEADER_SIZE
            val pcEnd = pcOff + pcSize
            val frEnd = pcEnd + frSize
            if (msg.size < frEnd) return null

            // --- 2) 读取点云数据 ---
            val bbAll = ByteBuffer.wrap(msg).order(ByteOrder.LITTLE_ENDIAN)
            bbAll.position(pcOff)
            val pcSlice = bbAll.slice().order(ByteOrder.LITTLE_ENDIAN)
            pcSlice.limit(pcSize)
            val fb = pcSlice.asFloatBuffer()
            val numFloats = fb.remaining()
            val pointCount = numFloats / 3

            // 验证点数
            if (pointCount * 3 != numFloats) {
                Log.e(TAG, "Invalid point cloud: $numFloats floats is not multiple of 3")
                return null
            }

            // 读取点云数据到 FloatArray
            val pointCloud = FloatArray(numFloats)
            fb.position(0)
            fb.get(pointCloud)

            // --- 3) 读取 Frontier 数据 ---
            val frontier = String(msg, pcEnd, frSize, Charsets.UTF_8)
            val frontierBytes = frontier.toByteArray(Charsets.UTF_8)
            val frontierSize = frontierBytes.size

            // 日志输出
            Log.i(TAG, "Binary: PointCount=$pointCount, FrontierSize=$frontierSize bytes")
            Log.i(TAG, "Odom: ${odom.joinToString(", ") { "%.4f".format(it) }}")
            Log.i(TAG, "Quat: ${quat.joinToString(", ") { "%.4f".format(it) }}")
            Log.i(TAG, "RcGoal: ${rc.joinToString(", ") { "%.4f".format(it) }}")

            // --- 4) 按照 BINARY_PROTOCOL.md 构建二进制数据 ---
            // Header: 48 bytes
            // - uint32: pointCount (4 bytes)
            // - uint32: frontierSize (4 bytes)
            // - float32[3]: odom (12 bytes)
            // - float32[4]: quaternion (16 bytes)
            // - float32[3]: rc_goal (12 bytes)
            val headerSize = 48
            val pointCloudSize = pointCount * 3 * 4  // float32[3*N] = 12*N bytes
            val totalSize = headerSize + pointCloudSize + frontierSize

            val buffer = ByteArray(totalSize)
            val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)

            // Header
            bb.putInt(pointCount)           // uint32: 点云点数 (4 bytes)
            bb.putInt(frontierSize)          // uint32: Frontier 字节数 (4 bytes)
            bb.putFloat(odom[0])            // odom[3] (12 bytes)
            bb.putFloat(odom[1])
            bb.putFloat(odom[2])
            bb.putFloat(quat[0])            // quaternion[4] (16 bytes)
            bb.putFloat(quat[1])
            bb.putFloat(quat[2])
            bb.putFloat(quat[3])
            bb.putFloat(rc[0])              // rc_goal[3] (12 bytes)
            bb.putFloat(rc[1])
            bb.putFloat(rc[2])

            // Point Cloud Data: float32[3*N]
            for (i in pointCloud.indices) {
                bb.putFloat(pointCloud[i])
            }

            // Frontier Data: UTF-8 字符串
            bb.put(frontierBytes)

            buffer
        } catch (e: Exception) {
            Log.e(TAG, "buildBinaryFromDecoded error", e)
            null
        }
    }

    // 解析 GroundUnit 解码后再次打包的"解码包"：header + float32(xyz)*N + frontier(utf-8)
    // 保留此函数用于字符串格式（兼容性）
    private fun buildPcStrFromDecoded(msg: ByteArray): String? {
        return try {
            // --- 1) header ---
            val le = ByteBuffer.wrap(msg).order(ByteOrder.LITTLE_ENDIAN)
            val odom = FloatArray(3) { le.getFloat(it * 4) }
            val quat = FloatArray(4) { le.getFloat((3 + it) * 4) }
            val rc   = FloatArray(3) { le.getFloat((7 + it) * 4) }

            val pcSize = ByteBuffer.wrap(msg, 40, 4).order(ByteOrder.BIG_ENDIAN).int
            val frSize = ByteBuffer.wrap(msg, 44, 4).order(ByteOrder.BIG_ENDIAN).int

            val pcOff = HEADER_SIZE
            val pcEnd = pcOff + pcSize
            val frEnd = pcEnd + frSize
            if (msg.size < frEnd) return null

            // --- 2) 点云：用 slice + asFloatBuffer 避免错位 ---
            val bbAll = ByteBuffer.wrap(msg).order(ByteOrder.LITTLE_ENDIAN)
            bbAll.position(pcOff)
            val pcSlice = bbAll.slice().order(ByteOrder.LITTLE_ENDIAN)      // 从偏移处切片
            pcSlice.limit(pcSize)                                           // 限制到点云区
            val fb = pcSlice.asFloatBuffer()                                // 以 float 视图读取
            val numFloats = fb.remaining()
            val triplets  = numFloats / 3

            // 额外日志：尺寸与点数
            Log.i(TAG, "PointCloud Size: $pcSize bytes  => $numFloats floats => $triplets points  Frontier: $frSize bytes")

            // 打印前 5 个点（相对读取）
            val preview = minOf(5, triplets)
            for (i in 0 until preview) {
                val x = fb.get(i * 3)
                val y = fb.get(i * 3 + 1)
                val z = fb.get(i * 3 + 2)
                Log.i(TAG, "Point[$i]: ${"%.4f".format(x)}, ${"%.4f".format(y)}, ${"%.4f".format(z)}")
            }

            // 安全起见，再把 fb 位置复位（避免后续相对读时位置受影响，可选）
            fb.position(0)

            // --- 3) 组装 pcStr ---
            fun fmt(v: Float) = String.format(Locale.US, "%.4f", v)
            val sb = StringBuilder(pcSize / 2)

            for (i in 0 until triplets) {
                val x = fb.get(i * 3)
                val y = fb.get(i * 3 + 1)
                val z = fb.get(i * 3 + 2)
                sb.append(fmt(x)).append(' ')
                    .append(fmt(y)).append(' ')
                    .append(fmt(z)).append(' ')
            }

            val frontier = String(msg, pcEnd, frSize, Charsets.UTF_8)

            // 头部也加日志，便于对齐检查
            Log.i(TAG, "Odom: ${odom.joinToString(", ") { "%.4f".format(it) }}")
            Log.i(TAG, "Quat: ${quat.joinToString(", ") { "%.4f".format(it) }}")
            Log.i(TAG, "RcGoal: ${rc.joinToString(", ") { "%.4f".format(it) }}")
            if (frSize > 0) Log.i(TAG, "Frontier raw: $frontier")

            sb.append("/ ").append(frontier).append(" / ")
            odom.forEach { sb.append(fmt(it)).append(' ') }
            quat.forEach { sb.append(fmt(it)).append(' ') }
            rc.forEach   { sb.append(fmt(it)).append(' ') }

            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "buildPcStrFromDecoded error", e); null
        }
    }


    private class LocalWs(port: Int) : WebSocketServer(InetSocketAddress("127.0.0.1", port)) {
        private val peers = Collections.synchronizedSet(mutableSetOf<WebSocket>())
        override fun onStart() { setConnectionLostTimeout(30) }
        override fun onOpen(conn: WebSocket, hs: ClientHandshake) { peers.add(conn) }
        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) { peers.remove(conn) }
        override fun onMessage(conn: WebSocket, message: String) {}
        override fun onError(conn: WebSocket?, ex: Exception) { ex.printStackTrace() }
        override fun broadcast(text: String) { synchronized(peers) { peers.forEach { if (it.isOpen) it.send(text) } } }
        fun broadcastBinary(data: ByteArray) { synchronized(peers) { peers.forEach { if (it.isOpen) it.send(data) } } }
    }
}
