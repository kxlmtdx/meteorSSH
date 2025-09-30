package com.example.meteorssh

import com.jcraft.jsch.*
import android.util.Log
import java.io.InputStream
import java.io.OutputStream

class SSHManager(val host: Host, private val password: String) {

    private var session: Session? = null
    private var channel: ChannelShell? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val TAG = "SSHManager"
    }

    fun connect(
        onConnected: () -> Unit,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val (user, hostPart, portStr) = parseAddress(host.address)
                val port = portStr.toIntOrNull() ?: 22

                val jsch = JSch()
                session = jsch.getSession(user, hostPart, port)

                session?.let { sess ->
                    sess.setPassword(password)
                    sess.setConfig("StrictHostKeyChecking", "no")

                    // Настройки для лучшей совместимости
                    sess.setConfig("PreferredAuthentications", "password,keyboard-interactive")
                    sess.setConfig("MaxAuthTries", "3")

                    // Настройки терминала
                    sess.setConfig("Terminal", "xterm-256color")
                    sess.setConfig("ServerAliveInterval", "60")

                    Log.d(TAG, "Connecting to $hostPart:$port as $user...")
                    sess.connect(30_000)
                }

                if (session?.isConnected == true) {
                    Log.d(TAG, "✅ SSH Session Connected!")

                    channel = session?.openChannel("shell") as? ChannelShell
                    channel?.setPtyType("xterm-256color")
                    channel?.setPtySize(80, 24, 0, 0)

                    inputStream = channel?.inputStream
                    outputStream = channel?.outputStream

                    channel?.connect(15_000)

                    if (channel?.isConnected == true) {
                        Log.d(TAG, "✅ Shell Channel Connected!")

                        startReadingOutput(onOutput)
                        onConnected()
                    } else {
                        onError("Shell channel failed to connect")
                    }
                } else {
                    onError("Session not established")
                }
            } catch (e: JSchException) {
                Log.e(TAG, "SSH Connection error", e)
                when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        onError("Timeout: server unreachable")
                    e.message?.contains("Auth fail", ignoreCase = true) == true ->
                        onError("Authentication failed - wrong password?")
                    e.message?.contains("Connection refused", ignoreCase = true) == true ->
                        onError("Connection refused - check if SSH server is running")
                    else -> onError("SSH Error: ${e.message ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected connection error", e)
                onError("Connection failed: ${e.message}")
            }
        }.start()
    }

    private fun startReadingOutput(onOutput: (String) -> Unit) {
        Thread {
            try {
                val reader = inputStream?.bufferedReader()
                val buffer = CharArray(1024)
                var bytesRead: Int

                while (channel?.isConnected == true && !channel?.isClosed!!) {
                    bytesRead = reader?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val output = String(buffer, 0, bytesRead)
                        Log.d(TAG, "Received output: ${output.replace(Regex("[\\r\\n]"), " ")}")
                        onOutput(output)
                    } else if (bytesRead == -1) {
                        break
                    }
                    Thread.sleep(10)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading shell output", e)
            }
        }.start()
    }

    fun sendCommand(command: String) {
        try {
            Log.d(TAG, "Sending command: $command")
            outputStream?.write("$command\r\n".toByteArray())
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command", e)
        }
    }

    fun sendCharacter(char: Char) {
        try {
            outputStream?.write(char.code)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending character", e)
        }
    }

    fun sendControlCharacter(controlChar: Char) {
        try {
            outputStream?.write(controlChar.code - 64)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending control character", e)
        }
    }

    fun resizeTerminal(cols: Int, rows: Int) {
        try {
            channel?.setPtySize(cols, rows, 0, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing terminal", e)
        }
    }

    fun disconnect() {
        try {
            channel?.disconnect()
            session?.disconnect()
            Log.d(TAG, "Disconnected from ${host.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    private fun parseAddress(address: String): Triple<String, String, String> {
        Log.d(TAG, "Parsing address: $address")
        return try {
            val atIndex = address.indexOf("@")
            val colonIndex = address.lastIndexOf(":")

            if (atIndex != -1 && colonIndex != -1 && colonIndex > atIndex) {
                val user = address.substring(0, atIndex)
                val host = address.substring(atIndex + 1, colonIndex)
                val port = address.substring(colonIndex + 1)
                Log.d(TAG, "Parsed: user=$user, host=$host, port=$port")
                Triple(user, host, port)
            } else if (atIndex != -1) {
                val user = address.substring(0, atIndex)
                val host = address.substring(atIndex + 1)
                Log.d(TAG, "Parsed: user=$user, host=$host, port=22")
                Triple(user, host, "22")
            } else {
                Log.d(TAG, "Parsed: user=root, host=$address, port=22")
                Triple("root", address, "22")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            throw IllegalArgumentException("Cannot parse address: $address")
        }
    }
}