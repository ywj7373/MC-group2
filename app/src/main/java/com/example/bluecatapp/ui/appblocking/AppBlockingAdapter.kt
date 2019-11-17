package com.example.bluecatapp.ui.appblocking

import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluecatapp.R

class AppBlockingAdapter(private val BlockedAppList: List<List<Any?>>, private val maxStepCount: Int) :
    RecyclerView.Adapter<AppBlockingAdapter.AppViewHolder>() {

//    private var BlockedAppList = blockedAppList

    class AppViewHolder(appListItem: View) : RecyclerView.ViewHolder(appListItem) {
        var appName: TextView = appListItem.findViewById(R.id.appItemName)
        var appTime: Chronometer = appListItem.findViewById(R.id.appItemTime)
        var appProgress: ProgressBar = appListItem.findViewById(R.id.appItemProgress)
        var appStepCount: TextView = appListItem.findViewById(R.id.appItemStepCount)
        var appIcon: ImageView = appListItem.findViewById(R.id.appItemIcon)
    }

    // Create new app blocking views invoked by layout manager
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val appListItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_blocking_item, parent, false)
        // layout settings for each list item
        appListItem.setPadding(20, 10, 10, 10)
        return AppViewHolder(appListItem)
    }

    // Replace contents of view invoked by layout manager
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.appName.text = BlockedAppList[position][0].toString()
        val finishTimeStamp = BlockedAppList[position][1] as Long
        if(System.currentTimeMillis() < finishTimeStamp) {
            getBlockCountdown(finishTimeStamp, holder.appTime).start()
        }
        holder.appProgress.max = maxStepCount //initialize max progress value
        val stepCount= BlockedAppList[position][2] as Int
        simulatePedometer(stepCount, holder.appStepCount, holder.appProgress, maxStepCount)
//        holder.appProgress.max = maxStepCount
//        holder.appProgress.progress = stepCount
//        holder.appStepCount.setText("$stepCount / $maxStepCount")
        holder.appIcon.setImageDrawable(BlockedAppList[position][3] as Drawable?)
    }

    override fun getItemCount(): Int = BlockedAppList.size


//    fun setAppBlockItems(newItemList: List<List<Any?>>) {
//        this.BlockedAppList = newItemList
//        notifyDataSetChanged()
//    }

    // Get countdown timer for each app in adapter list
    private fun getBlockCountdown(countDownFromTime: Long, chrono: Chronometer): CountDownTimer {
        val msToFinish = countDownFromTime - System.currentTimeMillis()
        chrono.base = SystemClock.elapsedRealtime() + msToFinish
        chrono.start()

        return object : CountDownTimer(msToFinish, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                chrono.stop()
            }
        }
    }

    /**Function to simulate pedometer
     * Increments step count every 2s
     */
    private fun simulatePedometer(initialStepCount: Int, appStepCount: TextView,
                                  appProgress: ProgressBar, totalNumberOfSteps: Int) {
        val countDownFromTime = ((totalNumberOfSteps - initialStepCount) * 2000).toLong()
        var currentStepCount = initialStepCount
        appProgress.max = totalNumberOfSteps
        appProgress.progress = initialStepCount
        appStepCount.setText("$initialStepCount / $totalNumberOfSteps")

        object: CountDownTimer(countDownFromTime, 2000) {
            override fun onTick(millisUntilFinished: Long) {
                currentStepCount++
                appStepCount.setText("$currentStepCount / $totalNumberOfSteps")
                appProgress.progress = currentStepCount
            }
            override fun onFinish() {}
        }.start()
    }
}