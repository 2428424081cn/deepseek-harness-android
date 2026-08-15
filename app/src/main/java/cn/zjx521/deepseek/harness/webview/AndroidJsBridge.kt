package cn.zjx521.deepseek.harness.webview

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.zjx521.deepseek.harness.HarnessApp
import cn.zjx521.deepseek.harness.R

/**
 * JavaScript bridge exposed to the Harness Web UI as `window.AndroidBridge`.
 */
class AndroidJsBridge(
    private val webView: WebView,
    private val notificationManager: NotificationManagerCompat,
    private val onModalStateChanged: (isOpen: Boolean) -> Unit = {}
) {
    private var notificationId = 1000

    @JavascriptInterface
    fun notify(title: String, body: String) {
        val notification = NotificationCompat.Builder(webView.context, HarnessApp.CHANNEL_AGENT_EVENTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(notificationId++, notification)
        } catch (_: SecurityException) {
            // Permission not granted; silently ignore
        }
    }

    @JavascriptInterface
    fun vibrate() {
        webView.post {
            val vibrator = webView.context.getSystemService(android.os.Vibrator::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        200L,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
        }
    }

    @JavascriptInterface
    fun reportModalState(isOpen: Boolean) {
        webView.post {
            onModalStateChanged(isOpen)
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return """{"platform":"android","version":"${android.os.Build.VERSION.RELEASE}","model":"${android.os.Build.MODEL}"}"""
    }

    companion object {
        const val BRIDGE_NAME = "AndroidBridge"
    }
}
