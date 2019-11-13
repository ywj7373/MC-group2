package com.example.bluecatapp.ui.appblocking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R

class AppBlockingAdapter(private val BlockedAppList: List<List<Any?>>) :
    RecyclerView.Adapter<AppBlockingAdapter.AppViewHolder>() {

    class AppViewHolder(appListItem: View) : RecyclerView.ViewHolder(appListItem) {
        var appName: TextView = appListItem.findViewById(R.id.appItemName)
        var appTime: TextView = appListItem.findViewById(R.id.appItemTime)
    }

    // Create new app blocking views invoked by layout manager
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val appListItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_blocking_item, parent, false)
        // TODO: set layout settings
        appListItem.setPadding(20, 10, 10, 10)
        return AppViewHolder(appListItem)
    }

    // Replace contents of view invoked by layout manager
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        // FIXME
        val appName = BlockedAppList[position][0].toString()
        holder.appName.text = appName
        holder.appTime.text = BlockedAppList[position][1].toString()
    }

    override fun getItemCount(): Int = BlockedAppList.size
}