package com.example.bluecatapp.ui.appblocking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R
import kotlinx.android.synthetic.main.pref_dialog_restricted_apps.view.*
import org.w3c.dom.Text

class AppBlockingAdapter() :
    RecyclerView.Adapter<AppBlockingAdapter.AppViewHolder>() {

    // FIXME: pass blocked app list into class
    private var BlockedAppList = arrayOf("Hello World!", "TODO: Add blocked apps here", "Chrome", "Youtube", "ADD ME", "Scroll me")

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
        holder.appName.text = BlockedAppList[position]
        holder.appTime.text = "" +  position
    }

    override fun getItemCount(): Int = BlockedAppList.size
}