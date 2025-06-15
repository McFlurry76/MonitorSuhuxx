package com.example.monitorsuhu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DeviceModel(
    val name: String,
    val imageResId: Int,
    var isScanning: Boolean = false
)

class DeviceAdapter(
    private val devices: List<DeviceModel>,
    private val onItemClicked: (DeviceModel, Int) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceImage: ImageView = view.findViewById(R.id.image_device)
        val deviceName: TextView = view.findViewById(R.id.text_device_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val actualPosition = position % devices.size
        val device = devices[actualPosition]

        if (device.isScanning) {
            holder.deviceName.text = holder.itemView.context.getString(R.string.scanning)
        } else {
            holder.deviceName.text = device.name
        }
        holder.deviceImage.setImageResource(device.imageResId)

        holder.itemView.setOnClickListener {
            onItemClicked(device, position)
        }
    }

    override fun getItemCount() = Int.MAX_VALUE
}
