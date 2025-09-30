// HostAdapter.kt
package com.example.meteorssh

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HostAdapter(
    private val hosts: List<Host>,
    private val onHostClick: (Host) -> Unit //калл обратно
) : RecyclerView.Adapter<HostAdapter.HostViewHolder>() {

    class HostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val osIcon: ImageView = view.findViewById(R.id.ivOSIcon)
        val hostName: TextView = view.findViewById(R.id.tvHostName)
        val hostAddress: TextView = view.findViewById(R.id.tvHostAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_host, parent, false)
        return HostViewHolder(view)
    }

    override fun onBindViewHolder(holder: HostViewHolder, position: Int) {
        val host = hosts[position]
        holder.hostName.text = host.name
        holder.hostAddress.text = host.address

        when (host.osType.lowercase()) {
            "linux" -> holder.osIcon.setImageResource(R.mipmap.ic_linux_foreground)
            "windows" -> holder.osIcon.setImageResource(R.mipmap.ic_win11_foreground)
            "macos", "mac" -> holder.osIcon.setImageResource(R.mipmap.ic_macos_foreground)
            else -> holder.osIcon.setImageResource(R.mipmap.ic_default_server_foreground)
        }

        holder.itemView.setOnClickListener {
            onHostClick(host)
        }
    }

    override fun getItemCount() = hosts.size
}