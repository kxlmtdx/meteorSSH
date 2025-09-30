package com.example.meteorssh

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meteorssh.databinding.ActivityMainBinding
import com.example.meteorssh.Host
import com.example.meteorssh.ApiClient
import com.example.meteorssh.LoginActivity
import com.example.meteorssh.ProfileActivity
import com.example.meteorssh.SessionManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val hostList = mutableListOf<Host>()
    private lateinit var adapter: HostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.profileContainer.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        setupRecyclerView()
        setupFab()
        loadHosts()
    }

    private fun loadHosts() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiClient.getHosts(sessionManager.getUserId())
            runOnUiThread {

                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    hostList.clear()
                    val hostsArray = json.getJSONArray("hosts")
                    for (i in 0 until hostsArray.length()) {
                        val h = hostsArray.getJSONObject(i)
                        hostList.add(
                            Host(
                                name = h.getString("name"),
                                address = h.getString("address"),
                                osType = h.optString("os_type", "windows")
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = HostAdapter(hostList) { host -> onHostClicked(host) }
        binding.rvHosts.layoutManager = LinearLayoutManager(this)
        binding.rvHosts.adapter = adapter
    }

    private fun onHostClicked(host: Host) {
        showPasswordDialog(host) { password ->
            val intent = Intent(this, ConsoleActivity::class.java).apply {
                putExtra(ConsoleActivity.EXTRA_HOST, host)
                putExtra(ConsoleActivity.EXTRA_PASSWORD, password)
            }
            startActivity(intent)
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddHostDialog()
        }
    }

    private fun showAddHostDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_host, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.etAddress)
        val rgOSType = dialogView.findViewById<RadioGroup>(R.id.rgOSType)

        AlertDialog.Builder(this)
            .setTitle("Add New Host")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val address = etAddress.text.toString().trim()
                val osType = when (rgOSType.checkedRadioButtonId) {
                    R.id.rbLinux -> "linux"
                    R.id.rbMacOS -> "macos"
                    else -> "windows"
                }

                if (name.isNotEmpty() && address.isNotEmpty()) {
                    addHostToServer(name, address, osType)
                } else {
                    Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addHostToServer(name: String, address: String, osType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiClient.addHost(sessionManager.getUserId(), name, address, osType)
            runOnUiThread {
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        loadHosts()
                    } else {
                        Toast.makeText(this@MainActivity, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Add error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPasswordDialog(host: Host, onConfirm: (String) -> Unit) {
        val input = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Enter Password for ${host.name}")
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) onConfirm(password)
                else Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}