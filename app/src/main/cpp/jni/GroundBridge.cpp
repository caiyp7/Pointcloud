#include <jni.h>
#include <atomic>
#include <thread>
#include "GroundUnit.h"

static std::atomic<bool> g_running{false};
static std::thread g_worker;

extern "C" JNIEXPORT void JNICALL
Java_com_example_pointcloud_GroundBridge_start(JNIEnv* /*env*/, jclass /*clazz*/, jint udpPort) {
    if (g_running.exchange(true)) return;
    g_worker = std::thread([udpPort](){
        GroundUnit u;
        u.start(static_cast<int>(udpPort));
        while (g_running.load()) u.loopOnce();
        u.stop();
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_pointcloud_GroundBridge_stop(JNIEnv* /*env*/, jclass /*clazz*/) {
    if (!g_running.exchange(false)) return;
    if (g_worker.joinable()) g_worker.join();
}
