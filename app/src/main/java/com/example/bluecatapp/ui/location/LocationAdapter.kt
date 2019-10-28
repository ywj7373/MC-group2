package com.example.bluecatapp.ui.location

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationItem

class LocationAdapter : RecyclerView.Adapter<LocationAdapter.LocationItemHolder>() {

    private var locationItems: List<LocationItem> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationItemHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_item, parent, false)
        return LocationItemHolder(itemView)
    }

    override fun onBindViewHolder(holder: LocationItemHolder, position: Int) {
        val currentLocationItem = locationItems[position]
        holder.textViewName.text = currentLocationItem.name
        holder.textViewTime.text = currentLocationItem.time
        holder.textViewTimeToDest.text = currentLocationItem.timeToDest
    }

    override fun getItemCount(): Int = locationItems.size

    fun setLocationItems(locationItems: List<LocationItem>) {
        this.locationItems = locationItems
        notifyDataSetChanged()
    }

    inner class LocationItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewName: TextView = itemView.findViewById(R.id.location_item_name)
        var textViewTime: TextView = itemView.findViewById(R.id.location_item_time)
        var textViewTimeToDest: TextView = itemView.findViewById(R.id.location_item_timeToDest)
    }

}
