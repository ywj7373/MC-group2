package com.example.bluecatapp.ui.appblocking

import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R

class AppBlockingAdapter(private val BlockedAppList: List<List<Any?>>) :
    RecyclerView.Adapter<AppBlockingAdapter.AppViewHolder>() {

    class AppViewHolder(appListItem: View) : RecyclerView.ViewHolder(appListItem) {
        var appName: TextView = appListItem.findViewById(R.id.appItemName)
        var appTime: Chronometer = appListItem.findViewById(R.id.appItemTime)
    }

    // Create new app blocking views invoked by layout manager
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val appListItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_blocking_item, parent, false)
        // set layout settings for each list item
        appListItem.setPadding(20, 10, 10, 10)
        return AppViewHolder(appListItem)
    }

    // Replace contents of view invoked by layout manager
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        // FIXME
        holder.appName.text = BlockedAppList[position][0].toString()
        getBlockCountdown(BlockedAppList[position][1] as Long, holder.appTime).start()
//        holder.appTime.text = BlockedAppList[position][1].toString()
    }

    override fun getItemCount(): Int = BlockedAppList.size


    private fun getBlockCountdown(countDownFromTime: Long, chrono: Chronometer): CountDownTimer {
        val msToFinish = countDownFromTime - System.currentTimeMillis()
        if(msToFinish < 0 ){
            chrono.base = 0
        } else {
            chrono.base = SystemClock.elapsedRealtime() + msToFinish
        }
        chrono.start()

        return object : CountDownTimer(msToFinish, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                chrono.stop()
            }
        }
    }
}