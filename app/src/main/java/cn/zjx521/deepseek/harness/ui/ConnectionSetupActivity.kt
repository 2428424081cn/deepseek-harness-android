package cn.zjx521.deepseek.harness.ui

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.zjx521.deepseek.harness.R
import cn.zjx521.deepseek.harness.config.ServerConfig
import cn.zjx521.deepseek.harness.config.ServerConfigManager
import cn.zjx521.deepseek.harness.databinding.ActivityConnectionSetupBinding
import cn.zjx521.deepseek.harness.databinding.ItemSavedServerBinding

/**
 * Multi-device connection & management screen.
 *
 * Lists all saved servers with one-tap switching, and provides a form to add/edit servers.
 */
class ConnectionSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectionSetupBinding
    private lateinit var configManager: ServerConfigManager
    private var editingServerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configManager = ServerConfigManager(this)

        setupFormDefaults()
        renderSavedServersList()
        setupListeners()
    }

    private fun setupFormDefaults() {
        val active = configManager.config
        binding.etLabel.setText(active.label)
        binding.etHost.setText(active.host)
        binding.etPort.setText(active.port.toString())
        binding.switchSsl.isChecked = active.useSsl
        updateUrlPreview()
    }

    private fun renderSavedServersList() {
        val servers = configManager.getAllServers()
        val active = configManager.config

        if (servers.isEmpty()) {
            binding.sectionSavedServers.visibility = View.GONE
            return
        }

        binding.sectionSavedServers.visibility = View.VISIBLE
        binding.containerSavedServers.removeAllViews()

        val inflater = LayoutInflater.from(this)
        for (server in servers) {
            val itemBinding = ItemSavedServerBinding.inflate(inflater, binding.containerSavedServers, false)
            val isActive = server.id == active.id

            itemBinding.tvServerLabel.text = server.label
            itemBinding.tvServerAddress.text = server.url
            itemBinding.viewActiveDot.setBackgroundResource(
                if (isActive) R.drawable.shape_dot_green else R.drawable.shape_dot_gray
            )

            // Tap item to connect
            itemBinding.btnItemConnect.text = if (isActive) getString(R.string.item_current) else getString(R.string.item_switch)
            itemBinding.btnItemConnect.isEnabled = !isActive
            itemBinding.btnItemConnect.setOnClickListener {
                configManager.setActiveServer(server.id)
                setResult(Activity.RESULT_OK)
                finish()
            }

            itemBinding.cardServerItem.setOnClickListener {
                if (!isActive) {
                    configManager.setActiveServer(server.id)
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    // Populate form to edit
                    populateFormForEdit(server)
                }
            }

            itemBinding.btnItemDelete.setOnClickListener {
                configManager.deleteServer(server.id)
                renderSavedServersList()
            }

            binding.containerSavedServers.addView(itemBinding.root)
        }
    }

    private fun populateFormForEdit(server: ServerConfig) {
        editingServerId = server.id
        binding.tvFormTitle.text = getString(R.string.edit_device_title, server.label)
        binding.etLabel.setText(server.label)
        binding.etHost.setText(server.host)
        binding.etPort.setText(server.port.toString())
        binding.switchSsl.isChecked = server.useSsl
        binding.btnConnect.text = getString(R.string.save_switch_device)
        updateUrlPreview()
    }

    private fun setupListeners() {
        binding.etPort.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveAndConnect(); true
            } else {
                false
            }
        }

        binding.btnConnect.setOnClickListener { saveAndConnect() }

        if (configManager.isConfigured) {
            binding.btnCancel.visibility = View.VISIBLE
            binding.btnCancel.setOnClickListener { finish() }
        }

        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = updateUrlPreview()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.etHost.addTextChangedListener(watcher)
        binding.etPort.addTextChangedListener(watcher)
        binding.switchSsl.setOnCheckedChangeListener { _, _ -> updateUrlPreview() }
    }

    private fun saveAndConnect() {
        clearErrors()

        val label = binding.etLabel.text.toString().trim().ifEmpty { getString(R.string.default_label) }
        val host = binding.etHost.text.toString().trim()
        val portText = binding.etPort.text.toString().trim()
        val useSsl = binding.switchSsl.isChecked

        if (host.isEmpty()) {
            binding.tilHost.error = getString(R.string.error_host_empty)
            return
        }

        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) {
            binding.tilPort.error = getString(R.string.error_port_range)
            return
        }

        val server = ServerConfig(
            id = editingServerId ?: java.util.UUID.randomUUID().toString(),
            label = label,
            host = host,
            port = port,
            useSsl = useSsl
        )

        configManager.saveServer(server, makeActive = true)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun clearErrors() {
        binding.tilHost.error = null
        binding.tilPort.error = null
    }

    private fun updateUrlPreview() {
        val scheme = if (binding.switchSsl.isChecked) "https" else "http"
        val host = binding.etHost.text.toString().ifEmpty { getString(R.string.preview_host_placeholder) }
        val port = binding.etPort.text.toString().ifEmpty { "3080" }
        binding.tvUrlPreview.text = "$scheme://$host:$port"
    }
}
