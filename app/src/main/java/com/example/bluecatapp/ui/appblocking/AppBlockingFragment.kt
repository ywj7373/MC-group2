package com.example.bluecatapp.ui.appblocking

import android.Manifest
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.bluecatapp.R
import android.util.Log
import android.provider.Settings
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.core.text.bold


class AppBlockingFragment : Fragment() {
    private lateinit var appOps: AppOpsManager
    private lateinit var appBlockingViewModel: AppBlockingViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        appBlockingViewModel =
            ViewModelProviders.of(this).get(AppBlockingViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_appblocking, container, false)
        val textView: TextView = root.findViewById(R.id.text_appblocking)
        appBlockingViewModel.text.observe(this, Observer {
            textView.text = it
        })
        return root
    }

    // Remove null check from Context to prevent error in settings
    override fun onAttach(context: Context) {
        super.onAttach(context)
        appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (hasUsageDataAccessPermission()) {
            Log.d("bcat", "Permissions all good")
        } else {
            // Permission is not granted, show alert dialog to request for permission
            showAlertDialog()
        }
    }

    private fun hasUsageDataAccessPermission(): Boolean {
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context!!.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showAlertDialog() {
        val alert = AlertDialog.Builder(activity!!)

        // Set the alert dialog title
        val titleMessage = SpannableStringBuilder()
            .append("Allow ")
            .bold { append("Bluecat ") }
            .append("to access usage data?")
        alert.setTitle(titleMessage)

        // Display a message on alert dialog
        alert.setMessage("In order to use the App Blocking feature, please enable \"Usage Access Permission\" on your device.")

        // Set a positive button and its click listener on alert dialog
        alert.setPositiveButton("OK"){dialog, which ->
            // Redirect to settings to enable usage access permission
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Display a negative button on alert dialog
        alert.setNegativeButton("Cancel"){dialog,which ->
            Toast.makeText(activity!!.applicationContext,"Permission request denied.",Toast.LENGTH_SHORT).show()
            activity!!.onBackPressed();
        }

        // create alert dialog using builder
        val dialog: AlertDialog = alert.create()

        // Display the alert dialog on app interface
        dialog.show()
    }
}


