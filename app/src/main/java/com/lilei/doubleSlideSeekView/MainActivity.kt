package com.lilei.doubleSlideSeekView

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lilei.widgets.DoubleSlideSeekView

class MainActivity : AppCompatActivity() {
    private val ALL_WEIGHT = 100
    private val VIEW_WEIGHT = 100
    private val MIN_DURATION = 1000L
    private val START_TIME = 0L
    private val END_TIME = 9999999L

    private var dss_progress: DoubleSlideSeekView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dss_progress = findViewById(R.id.dssv_progress)
        init()
    }

    private fun init() {
        val width = applicationContext.resources.displayMetrics.widthPixels
        dss_progress?.setWidthAndWeight(width - 66, ALL_WEIGHT, VIEW_WEIGHT)
        dss_progress?.setMinDurationLimit(MIN_DURATION)
        dss_progress?.setRangeTime(START_TIME, END_TIME)
    }
}
