package com.example.bluecatapp.ui.location

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.bluecatapp.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchPlaceDialog: DialogFragment(), View.OnClickListener {
    private lateinit var placeText: EditText
    private lateinit var searchButton: Button
    private lateinit var loc1: TextView
    private lateinit var loc2: TextView
    private lateinit var loc3: TextView
    private lateinit var loc4: TextView
    private lateinit var loc5: TextView
    private lateinit var btn_no: Button
    private lateinit var btn_yes: Button
    private lateinit var currentLocation: String
    private var isStart: Int = 0
    private lateinit var results: List<SearchPlacePlaces>
    private var mListener: OnButtonClick? = null
    private lateinit var place: SearchPlacePlaces
    private val TAG = "SearchPlaceDialog"

    //new instance to pass data from activity to dialog
    companion object {
        fun newInstance(str: String, isStart: Int) = SearchPlaceDialog().apply {
            arguments = Bundle().apply {
                putString("EXTRA_DATA", str)
                putInt("EXTRA_INT", isStart)
            }
        }
    }

    //attch listener to pass data from dialog to activity
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = context as OnButtonClick
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //receive data from activity
        arguments?.let {
            val str = it.getString("EXTRA_DATA")
            if (str != null) {
                currentLocation = str
            }
            isStart = it.getInt("EXTRA_INT")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_search_place, container, false)

        placeText = view.findViewById(R.id.placeText)
        searchButton = view.findViewById(R.id.searchButton)
        loc1 = view.findViewById(R.id.loc1)
        loc2 = view.findViewById(R.id.loc2)
        loc3 = view.findViewById(R.id.loc3)
        loc4 = view.findViewById(R.id.loc4)
        loc5 = view.findViewById(R.id.loc5)
        btn_no = view.findViewById(R.id.btn_no)
        btn_yes = view.findViewById(R.id.btn_yes)

        searchButton.setOnClickListener(this)
        btn_no.setOnClickListener(this)
        btn_yes.setOnClickListener(this)
        loc1.setOnClickListener(this)
        loc2.setOnClickListener(this)
        loc3.setOnClickListener(this)
        loc4.setOnClickListener(this)
        loc5.setOnClickListener(this)

        return view
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.searchButton -> requestSearch()
            R.id.btn_yes -> saveAddr()
            R.id.btn_no -> quitDialog()
            R.id.loc1 -> saveLocation(1, results[0])
            R.id.loc2 -> saveLocation(2, results[1])
            R.id.loc3 -> saveLocation(3, results[2])
            R.id.loc4 -> saveLocation(4, results[3])
            R.id.loc5 -> saveLocation(5, results[4])
        }
    }

    //search for place name using naver api
    private fun requestSearch() {
        NaverRetrofit.getService().requestSearchPlace(placeText.text.toString(), currentLocation).enqueue(object: Callback<SearchPlaceData>{
            override fun onFailure(call: Call<SearchPlaceData>, t: Throwable) {
                Log.e(TAG, "Connection failed")
                Toast.makeText(context, "Unavailable to connect! Please check wifi\"", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<SearchPlaceData>, response: Response<SearchPlaceData>) {
                Log.e(TAG, response.body().toString())
                var len = response.body()!!.meta.totalCount - 1
                if (len > 4) len = 4
                if (len == -1) Toast.makeText(context, "No result!\"", Toast.LENGTH_SHORT).show()
                results = response.body()!!.places
                for (i in 0..len) {
                    when (i) {
                        0 -> loc1.text = results[i].name
                        1 -> loc2.text = results[i].name
                        2 -> loc3.text = results[i].name
                        3 -> loc4.text = results[i].name
                        4 -> loc5.text = results[i].name
                    }
                }
            }
        })
    }

    //save place user clicked
    private fun saveLocation(text: Int, place: SearchPlacePlaces) {
        //hightlight clicked text
        when (text) {
            1 -> {
                loc1.setBackgroundColor(Color.GREEN)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.WHITE)
            }
            2 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.GREEN)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.WHITE)
            }
            3 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.GREEN)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.WHITE)
            }
            4 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.GREEN)
                loc5.setBackgroundColor(Color.WHITE)
            }
            5 -> {
                loc1.setBackgroundColor(Color.WHITE)
                loc2.setBackgroundColor(Color.WHITE)
                loc3.setBackgroundColor(Color.WHITE)
                loc4.setBackgroundColor(Color.WHITE)
                loc5.setBackgroundColor(Color.GREEN)
            }
        }
        //set current place to user's place
        this.place = place
    }

    //pass the location user picked to activity
    private fun saveAddr() {
        mListener!!.onDialogClickListener(isStart, place)
        dismiss()
    }

    private fun quitDialog() {
        dismiss()
    }
}