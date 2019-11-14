package com.example.bluecatapp.ui.location

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationItemData

class LocationAdapter : RecyclerView.Adapter<LocationAdapter.LocationItemHolder>() {

    private var locationItems: List<LocationItemData> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationItemHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.location_item, parent, false)
        return LocationItemHolder(itemView)
    }

    override fun onBindViewHolder(holder: LocationItemHolder, position: Int) {
        val locationItem = locationItems[position]
        holder.textViewName.text = locationItem.name
        var time = locationItem.time.substring(0, 16)
        if(locationItem.daysMode)
            time = locationItem.days + " " + locationItem.time.split(" ")[1].substring(0, 5)
        holder.textViewTime.text = time

        holder.loc_img.setOnClickListener {
            val intent = Intent(it.context, MapActivity::class.java)
            intent.putExtra("Longitude", locationItem.x)
            intent.putExtra("Latitude", locationItem.y)
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = locationItems.size

    fun setLocationItems(locationItems: List<LocationItemData>) {
        this.locationItems = locationItems
        notifyDataSetChanged()
    }

    inner class LocationItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewName: TextView = itemView.findViewById(R.id.location_item_name)
        var textViewTime: TextView = itemView.findViewById(R.id.location_item_time)
        var loc_img: Button = itemView.findViewById(R.id.loc_img)
    }

}
