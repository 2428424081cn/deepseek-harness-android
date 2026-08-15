package cn.zjx521.deepseek.harness.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Custom WebChromeClient for DeepSeek Harness.
 *
 * Responsibilities:
 * - Forward file/image picker requests from the WebView to the hosting Activity
 *   (required for file upload inputs in the Harness Web UI)
 * - Report load progress for the top progress bar
 * - Expose page title changes (could be used for a toolbar or notification)
 */
class HarnessChromeClient(
    private val onFileChooserRequest: (
        callback: ValueCallback<Array<Uri>>,
        params: FileChooserParams,
    ) -> Boolean,
    private val onProgressChanged: (progress: Int) -> Unit = {},
    private val onTitleChanged: (title: String?) -> Unit = {},
) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams,
    ): Boolean {
        return onFileChooserRequest(filePathCallback, fileChooserParams)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        onTitleChanged(title)
    }
}
