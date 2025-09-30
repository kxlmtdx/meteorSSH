package com.example.meteorssh

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.color
import com.google.android.material.textfield.TextInputEditText

class ConsoleActivity : AppCompatActivity() {

    private lateinit var tvOutput: TextView
    private lateinit var etCommand: TextInputEditText
    private lateinit var btnSend: Button
    private lateinit var btnDisconnect: Button
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    private lateinit var sshManager: SSHManager
    private val outputBuffer = SpannableStringBuilder()
    private var isConnected = false

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_PASSWORD = "password"
        private const val PROMPT_COLOR = 0xFFD587FA.toInt()
        private const val OUTPUT_COLOR = 0xFF87CEEB.toInt()
        private const val ERROR_COLOR = 0xFFFF6B6B.toInt()
        private const val SUCCESS_COLOR = 0xFF51CF66.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)

        initViews()

        val host = intent.getSerializableExtra(EXTRA_HOST) as? Host
        val password = intent.getStringExtra(EXTRA_PASSWORD)

        if (host == null || password == null) {
            showError("Missing connection data")
            finish()
            return
        }

        supportActionBar?.title = "Console - ${host.name}"
        sshManager = SSHManager(host, password)

        setupUI()
        connectToHost()
    }

    private fun initViews() {
        tvOutput = findViewById(R.id.tvOutput)
        etCommand = findViewById(R.id.etCommand)
        btnSend = findViewById(R.id.btnSend)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        scrollView = findViewById(R.id.scrollView)

        // Добавляем прогресс-бар и статус (можно добавить в разметку)
        progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }
        tvStatus = TextView(this).apply {
            text = "Connecting..."
            setTextColor(0xFFAAAAAA.toInt())
        }

        tvOutput.movementMethod = ScrollingMovementMethod()
        tvOutput.text = ""
    }

    private fun setupUI() {
        btnSend.isEnabled = false
        etCommand.isEnabled = false

        btnSend.setOnClickListener {
            sendCommand()
        }

        btnDisconnect.setOnClickListener {
            disconnectAndFinish()
        }

        etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCommand()
                true
            } else {
                false
            }
        }
    }

    private fun connectToHost() {
        appendOutput("🔌 Connecting to ${sshManager.host.address}...", PROMPT_COLOR)
        updateStatus("Connecting...", 0xFFAAAAAA.toInt())

        sshManager.connect(
            onConnected = {
                runOnUiThread {
                    isConnected = true
                    btnSend.isEnabled = true
                    etCommand.isEnabled = true
                    etCommand.requestFocus()

                    appendOutput("✅ Connected successfully!\n", SUCCESS_COLOR)
                    appendOutput("💻 Interactive shell ready\n", PROMPT_COLOR)
                    appendOutput("Type 'exit' to disconnect\n\n", PROMPT_COLOR)

                    updateStatus("Connected", SUCCESS_COLOR)

                    showKeyboard()
                }
            },
            onOutput = { output ->
                runOnUiThread {
                    val cleanOutput = removeAnsiEscapeCodes(output)
                    if (cleanOutput.isNotEmpty()) {
                        appendOutput(cleanOutput, OUTPUT_COLOR)
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    appendOutput("❌ Connection failed: $error\n", ERROR_COLOR)
                    updateStatus("Connection failed", ERROR_COLOR)
                    btnSend.isEnabled = false
                    etCommand.isEnabled = false
                    showError("Connection failed: $error")
                }
            }
        )
    }

    private fun sendCommand() {
        val command = etCommand.text.toString().trim()
        if (command.isEmpty()) return

        runOnUiThread {
            appendOutput("$ ", PROMPT_COLOR)
            appendOutput("$command\n", 0xFFFFFFFF.toInt())
            etCommand.setText("")
        }

        Thread {
            try {
                sshManager.sendCommand(command)
            } catch (e: Exception) {
                runOnUiThread {
                    appendOutput("❌ Ошибка отправки: ${e.message}\n", ERROR_COLOR)
                }
            }
        }.start()

        when (command.lowercase()) {
            "clear" -> clearConsole()
            "exit", "logout" -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    disconnectAndFinish()
                }, 500)
            }
        }
    }

    private fun appendOutput(text: String, color: Int) {
        val start = outputBuffer.length
        outputBuffer.append(text)
        outputBuffer.setSpan(
            ForegroundColorSpan(color),
            start,
            outputBuffer.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvOutput.text = outputBuffer

        // автопрокрутка вниз да ну нахуй, добавьте параметр интерфейса такой уже а не разметку в коде
        scrollView.postDelayed({
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }, 50)
    }

    private fun clearConsole() {
        outputBuffer.clear()
        tvOutput.text = ""
        appendOutput("Console cleared\n", PROMPT_COLOR)
    }

    private fun updateStatus(status: String, color: Int) {
        supportActionBar?.subtitle = status
    }

    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(etCommand, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etCommand.windowToken, 0)
    }

    private fun removeAnsiEscapeCodes(input: String): String {
        if (input.isBlank()) return ""

        return input
            .replace(Regex("\\u001B\\[[\\d;]*[A-Za-z]"), "") // ANSI escape sequences
            .replace(Regex("\\u001B\\].*?\\u0007"), "") // OSC sequences
            .replace(Regex("[\\u0000-\\u001F\\u007F-\\u009F]"), "") // Control chars
            .replace(Regex("\\r\\n|\\r"), "\n") // Normalize line endings
    }

    private fun disconnectAndFinish() {
        if (isConnected) {
            sshManager.sendCommand("exit")
            Handler().postDelayed({
                sshManager.disconnect()
                Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
                finish()
            }, 500)
        } else {
            finish()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        disconnectAndFinish()
    }

    override fun onDestroy() {
        sshManager.disconnect()
        hideKeyboard()
        super.onDestroy()
    }
}