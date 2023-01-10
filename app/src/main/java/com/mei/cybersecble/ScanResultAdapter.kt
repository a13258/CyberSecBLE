package com.mei.cybersecble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView


class ScanResultAdapter(
    private val items: List<ScanResult>,
    private val onClickListener: ((device: ScanResult) -> Unit)
) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_scan_result, parent, false)
        return ViewHolder(view, onClickListener)
    }


    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    inner class ViewHolder(
        val view: View,
        val onClickListener: ((device: ScanResult) -> Unit)
    ) : RecyclerView.ViewHolder(view) {

        val device_name: TextView = view.findViewById(R.id.device_name)
        val mac_address: TextView = view.findViewById(R.id.mac_address)
        val signal_strength: TextView = view.findViewById(R.id.signal_strength)

        init {
            view.setOnClickListener { v: View ->
                val pos: Int = adapterPosition
                Toast.makeText(view.context, "Item: ${pos+1}", Toast.LENGTH_SHORT).show()
            }
        }

        @SuppressLint("MissingPermission")
        fun bind(result: ScanResult) {
            device_name.text = result.device.name ?: "Unnamed"
            mac_address.text = result.device.address
            signal_strength.text = "${result.rssi} dBm"
            view.setOnClickListener { onClickListener.invoke(result) }
        }
    }
}