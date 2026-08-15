package cn.zjx521.deepseek.harness

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.zjx521.deepseek.harness.config.ServerConfigManager
import cn.zjx521.deepseek.harness.databinding.ActivityMainBinding
import cn.zjx521.deepseek.harness.ui.ConnectionSetupActivity
import cn.zjx521.deepseek.harness.webview.AndroidJsBridge
import cn.zjx521.deepseek.harness.webview.HarnessChromeClient
import cn.zjx521.deepseek.harness.webview.HarnessWebViewClient
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var configManager: ServerConfigManager

    private var isModalActive = false
    private var currentUiState = UiState.LOADING
    private var lastBackPressTime = 0L
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    // -----------------------------------------------------------------------
    // Activity result launchers
    // -----------------------------------------------------------------------

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri> = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { arrayOf(it) } ?: arrayOf()
        } else {
            arrayOf()
        }
        fileUploadCallback?.onReceiveValue(uris)
        fileUploadCallback = null
    }

    private val setupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadHarnessUrl()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silently continue */ }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure app content sits below system status bar and above navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configManager = ServerConfigManager(this)

        setupWebView()
        setupServerPill()
        setupButtonListeners()
        setupSmartBackNavigation()
        requestNotificationPermissionIfNeeded()

        if (!configManager.isConfigured) {
            openSetupScreen()
        } else {
            loadHarnessUrl()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
        updateServerPill()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    // -----------------------------------------------------------------------
    // Smart Back Navigation
    // -----------------------------------------------------------------------

    private fun setupSmartBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val js = """
                    (function() {
                        // Close the mobile sidebar drawer first (if open)
                        if (document.documentElement.classList.contains('dsh-sidebar-open')) {
                            var sbToggle = document.querySelector('[class$="_sidebarCol"] button[class$="_toggle"]');
                            if (sbToggle) {
                                sbToggle.click();
                                return true;
                            }
                        }
                        var dialog = document.querySelector('[role="dialog"], div[class*="SettingsRoot_panel"], div[class*="_panel_"], div[class*="Modal_dialog"]');
                        if (dialog) {
                            var closeBtn = document.querySelector('[role="dialog"] button[class*="close"], div[class*="SettingsRoot"] button[class*="close"], div[class*="Modal"] button[class*="close"]');
                            if (closeBtn) {
                                closeBtn.click();
                                return true;
                            }
                            var mask = document.querySelector('div[class*="overlay"] div[class*="mask"], div[class*="Modal_mask"]');
                            if (mask) {
                                mask.click();
                                return true;
                            }
                            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true }));
                            return true;
                        }
                        var menu = document.querySelector('[role="menu"], [class*="menu"], [class*="popover"]');
                        if (menu) {
                            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true }));
                            return true;
                        }
                        return false;
                    })();
                """.trimIndent()

                binding.webView.evaluateJavascript(js) { result ->
                    val modalClosed = result == "true"
                    if (modalClosed) {
                        return@evaluateJavascript
                    }

                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                        return@evaluateJavascript
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(this@MainActivity, getString(R.string.exit_confirm), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // -----------------------------------------------------------------------
    // Floating Server Pill & Switcher (Draggable & Auto-hiding on modals)
    // -----------------------------------------------------------------------

    private fun setupServerPill() {
        updateServerPill()

        var dX = 0f
        var dY = 0f
        var isDragging = false
        var downRawX = 0f
        var downRawY = 0f

        binding.floatingServerPill.setOnTouchListener { view, event ->
            val parent = view.parent as? View ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().cancel()
                    view.alpha = 1.0f

                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - downRawX
                    val deltaY = event.rawY - downRawY
                    val dist = hypot(deltaX.toDouble(), deltaY.toDouble())
                    if (dist > 10 || isDragging) {
                        isDragging = true
                        val newX = (event.rawX + dX).coerceIn(0f, (parent.width - view.width).toFloat())
                        val newY = (event.rawY + dY).coerceIn(0f, (parent.height - view.height).toFloat())
                        view.x = newX
                        view.y = newY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                        openSetupScreen()
                    }
                    dimServerPill()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dimServerPill()
                    true
                }
                else -> false
            }
        }

        dimServerPill()
    }

    private fun updateServerPill() {
        val server = configManager.config
        binding.tvPillServerName.text = server.label.ifEmpty { server.displayAddress }
    }

    private fun dimServerPill() {
        binding.floatingServerPill.animate()
            .alpha(0.45f)
            .setStartDelay(4000)
            .setDuration(500)
            .start()
    }

    private fun updatePillVisibility() {
        binding.floatingServerPill.visibility =
            if (currentUiState == UiState.ERROR || isModalActive) View.GONE else View.VISIBLE
    }

    // -----------------------------------------------------------------------
    // WebView setup
    // -----------------------------------------------------------------------

    private fun setupWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            textZoom = 100
            userAgentString = buildUserAgent()
        }

        binding.webView.webViewClient = HarnessWebViewClient(
            onPageStarted = { setUiState(UiState.LOADING) },
            onPageFinished = { setUiState(UiState.CONTENT) },
            onError = { _, description -> setUiState(UiState.ERROR, description) }
        )

        binding.webView.webChromeClient = HarnessChromeClient(
            onFileChooserRequest = { callback, params ->
                fileUploadCallback = callback
                filePickerLauncher.launch(params.createIntent())
                true
            },
            onProgressChanged = { progress ->
                binding.progressBar.progress = progress
                binding.progressBar.visibility = if (progress < 100) View.VISIBLE else View.GONE
            }
        )

        binding.webView.addJavascriptInterface(
            AndroidJsBridge(
                webView = binding.webView,
                notificationManager = NotificationManagerCompat.from(this),
                onModalStateChanged = { isOpen ->
                    isModalActive = isOpen
                    updatePillVisibility()
                }
            ),
            AndroidJsBridge.BRIDGE_NAME
        )
    }

    // -----------------------------------------------------------------------
    // Navigation & UI state
    // -----------------------------------------------------------------------

    private fun loadHarnessUrl() {
        val config = configManager.config
        updateServerPill()
        setUiState(UiState.LOADING)
        binding.webView.loadUrl(config.url)
    }

    private fun openSetupScreen() {
        setupLauncher.launch(Intent(this, ConnectionSetupActivity::class.java))
    }

    private fun setupButtonListeners() {
        binding.btnRetry.setOnClickListener { loadHarnessUrl() }
        binding.btnChangeServer.setOnClickListener { openSetupScreen() }
    }

    private fun setUiState(state: UiState, errorMessage: String? = null) {
        currentUiState = state
        binding.loadingLayout.visibility = if (state == UiState.LOADING) View.VISIBLE else View.GONE
        binding.errorLayout.visibility = if (state == UiState.ERROR) View.VISIBLE else View.GONE
        binding.webView.visibility = if (state == UiState.CONTENT) View.VISIBLE else View.INVISIBLE

        updatePillVisibility()

        if (state == UiState.ERROR && errorMessage != null) {
            val config = configManager.config
            binding.tvErrorAddress.text = config.url
            binding.tvErrorDetail.text = errorMessage
        }
    }

    // -----------------------------------------------------------------------
    // Permissions & User Agent
    // -----------------------------------------------------------------------

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun buildUserAgent(): String {
        val base = WebSettings.getDefaultUserAgent(this)
        return base.replace("Mobile ", "") + " DSHAndroid/1.0"
    }

    enum class UiState { LOADING, CONTENT, ERROR }
}
