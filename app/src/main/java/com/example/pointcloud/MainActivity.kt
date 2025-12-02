package com.example.pointcloud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pointcloud.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AutoStartService.ACTION_GROUND_STATE) {
                val running = intent.getBooleanExtra(AutoStartService.EXTRA_RUNNING, false)
                updateUi(running)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 确保后台服务已启动
        val svc = Intent(this, AutoStartService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
        else startService(svc)

        // 根据真实状态显示，而不是写死
        updateUi(StatusStore.isRunning(this))

        binding.startButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(Intent(this, AutoStartService::class.java))
            else startService(Intent(this, AutoStartService::class.java))
        }

        binding.stopButton.setOnClickListener {
            stopService(Intent(this, AutoStartService::class.java))
            updateUi(false)
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(stateReceiver, IntentFilter(AutoStartService.ACTION_GROUND_STATE))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(stateReceiver)
    }

    private fun updateUi(running: Boolean) {
        binding.sampleText.text = if (running) {
            "Ground 已启动：UDP 14600 → 回调 → WS 9000"
        } else {
            "Ground 未启动"
        }
    }
}
