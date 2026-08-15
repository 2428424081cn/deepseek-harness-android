package cn.zjx521.deepseek.harness

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class HarnessApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Agent task notifications (approval requests, completions)
            val taskChannel = NotificationChannel(
                CHANNEL_AGENT_EVENTS,
                "Agent Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Agent task completion and approval requests"
                enableVibration(true)
            }
            manager.createNotificationChannel(taskChannel)
        }
    }

    companion object {
        const val CHANNEL_AGENT_EVENTS = "agent_events"
    }
}
