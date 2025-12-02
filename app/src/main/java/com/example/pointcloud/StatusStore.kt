package com.example.pointcloud

import android.content.Context
import android.content.SharedPreferences

object StatusStore {
    private const val PREF = "pointcloud_status"
    private const val KEY_RUNNING = "ground_running"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun setRunning(ctx: Context, running: Boolean) {
        sp(ctx).edit().putBoolean(KEY_RUNNING, running).apply()
    }

    fun isRunning(ctx: Context): Boolean =
        sp(ctx).getBoolean(KEY_RUNNING, false)
}
