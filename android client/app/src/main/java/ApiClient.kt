package com.example.meteorssh

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val BASE_URL = "http://192.168.0.19/meteorssh/" // в прод

    fun register(username: String, email: String, password: String): String {
        return post("${BASE_URL}register.php", buildJson {
            put("username", username)
            put("email", email)
            put("password", password)
        })
    }

    fun login(username: String, password: String): String {
        return post("${BASE_URL}login.php", buildJson {
            put("username", username)
            put("password", password)
        })
    }

    fun getHosts(userId: Int): String {
        val url = "${BASE_URL}hosts.php?user_id=$userId"
        return get(url)
    }

    fun addHost(userId: Int, name: String, address: String, osType: String): String {
        return post("${BASE_URL}add_host.php", buildJson {
            put("user_id", userId)
            put("name", name)
            put("address", address)
            put("os_type", osType)
        })
    }

    private fun post(url: String, jsonBody: String): String {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray())
            }
            readResponse(connection)
        } catch (e: Exception) {
            "{\"success\":false,\"message\":\"Network error: ${e.message}\"}"
        }
    }

    private fun get(url: String): String {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            readResponse(connection)
        } catch (e: Exception) {
            "{\"success\":false,\"message\":\"Network error: ${e.message}\"}"
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        return BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
    }

    private fun buildJson(block: JSONObject.() -> Unit): String {
        return JSONObject().apply(block).toString()
    }
}