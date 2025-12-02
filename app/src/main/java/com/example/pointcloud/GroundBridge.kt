package com.example.pointcloud

/**
 * Kotlin<->C++ 桥。
 * C++ 线程仍由 start/stop 控制；每次解码好一帧后，
 * C++ 通过 JNI 调用 onPacketFromNative(pcStr)。
 * 这里把 pcStr 转发给注册的 listener（由 WS Service 来注册）。
 */
object GroundBridge {

    init {
        // 名称要和 CMake add_library(...) 的 so 名一致
        System.loadLibrary("groundstreamer")
    }

    /** 启动/停止 native 端 GroundUnit（仍然是你现在的实现） */
    @JvmStatic external fun start(udpPort: Int)
    @JvmStatic external fun stop()

    /* ---------------- 回调转发给 WebSocket Service ---------------- */

    interface Listener {
        fun onPcStr(pcStr: String)
    }

    @Volatile private var listener: Listener? = null

    /** 由 WebSocket Service 调用，注册回调 */
    @JvmStatic fun setListener(l: Listener?) { listener = l }

    /**
     * ✅ 由 C++ 直接回调这个静态方法（无需持有 Service 实例）
     * JNI 函数名：Java_com_example_pointcloud_GroundBridge_onPacketFromNative
     * 签名：(Ljava/lang/String;)V
     */
    @JvmStatic fun onPacketFromNative(pcStr: String) {
        listener?.onPcStr(pcStr)
    }
}
