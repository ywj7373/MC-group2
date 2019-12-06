package com.example.bluecatapp.ui.location

import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R
import com.example.bluecatapp.data.LocationItemData

class LocationAdapter internal constructor(locationViewModel: LocationViewModel): RecyclerView.Adapter<LocationAdapter.LocationItemHolder>() {


    private var locationItems: List<LocationItemData> = ArrayList()
    private val locationViewModel = locationViewModel
    private val TIME: Long = 1000

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
            time = repeatingDaysToPrettyFormat(locationItem.days) + " " + locationItem.time.split(" ")[1].substring(0, 5)
        holder.textViewTime.text = time

        // When space is not enough to display all the text slice text and append ...
        if(getWeightedLength(locationItem.name) + getWeightedLength(time) > 40 ) {
            val lengthAfterSlice = (38 - getWeightedLength(time))
            val slicePosition = getSlicePosition(locationItem.name, lengthAfterSlice)
            holder.textViewName.text = locationItem.name.substring(0, slicePosition) + "..."
        }

        holder.loc_img.setOnClickListener {
            val intent = Intent(it.context, MapActivity::class.java)
            intent.putExtra("Longitude", locationItem.x)
            intent.putExtra("Latitude", locationItem.y)
            it.context.startActivity(intent)
        }

        holder.delete_button.setOnClickListener {
            val locationItem = locationItems[position]
            locationViewModel.deleteLocationItem(locationItem.id)
            (locationItems as ArrayList).removeAt(position)
            notifyItemRemoved(position)
            holder.delete_button.isEnabled = false
            //prevents fast double click
            val handler = Handler()
            val runnable = Runnable {
                holder.delete_button.isEnabled = true
            }
            handler.postDelayed(runnable, TIME)
        }

        holder.edit_button.setOnClickListener {
            val intent = Intent(it.context, AddLocationActivity::class.java)
            val locationItem = locationItems[position]
            intent.putExtra("Editmode", true)
            intent.putExtra("Id", locationItem.id)
            intent.putExtra("name", locationItem.name)
            intent.putExtra("x", locationItem.x)
            intent.putExtra("y", locationItem.y)
            intent.putExtra("time", locationItem.time)
            intent.putExtra("daysMode", locationItem.daysMode)
            intent.putExtra("days", locationItem.days)
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = locationItems.size

    fun setLocationItems(locationItems: List<LocationItemData>) {
        this.locationItems = locationItems
        notifyDataSetChanged()
    }

    fun removeItem(position:Int, locationViewModel: LocationViewModel) {
        val locationItem = locationItems[position]
        locationViewModel.deleteLocationItem(locationItem.id)
        (locationItems as ArrayList).removeAt(position)
        notifyItemRemoved(position)
    }

    inner class LocationItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewName: TextView = itemView.findViewById(R.id.location_item_name)
        var textViewTime: TextView = itemView.findViewById(R.id.location_item_time)
        var loc_img: Button = itemView.findViewById(R.id.loc_img)
        var delete_button: Button = itemView.findViewById(R.id.delete_button)
        var edit_button: Button = itemView.findViewById(R.id.edit_button)
    }

}