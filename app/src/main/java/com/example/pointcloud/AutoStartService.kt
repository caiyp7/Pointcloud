package com.example.pointcloud

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.pointcloud.net.UdpToWebSocketService

class AutoStartService : Service() {

    companion object {
        const val ACTION_GROUND_STATE = "com.example.pointcloud.GROUND_STATE"
        const val EXTRA_RUNNING = "running"
        private const val CHANNEL_ID = "pointcloud_autostart"
        private const val NOTIF_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, createNotification())

        var ok = false
        try {
            // 1) 启动 WebSocket 服务
            val wsIntent = Intent(this, UdpToWebSocketService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(wsIntent)
            else startService(wsIntent)

            // 2) 启动 Ground 接收
            GroundBridge.start(14600)
            ok = true
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            StatusStore.setRunning(this, ok)
            sendBroadcast(Intent(ACTION_GROUND_STATE).putExtra(EXTRA_RUNNING, ok))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        try { GroundBridge.stop() } catch (_: Throwable) {}
        try { stopService(Intent(this, UdpToWebSocketService::class.java)) } catch (_: Throwable) {}

        StatusStore.setRunning(this, false)
        sendBroadcast(Intent(ACTION_GROUND_STATE).putExtra(EXTRA_RUNNING, false))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "PointCloud AutoStart",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("PointCloud Ground")
            .setContentText("后台运行中：UDP 14600 → WS 9000")
            .setOngoing(true)
            .build()
    }
}
