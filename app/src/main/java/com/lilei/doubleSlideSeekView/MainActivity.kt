package com.lilei.doubleSlideSeekView

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lilei.widgets.DoubleSlideSeekView

/**
 * 演示页
 * @author lilei
 * @email 1542978431@qq.com（有问题或者交流可以发邮件到我的邮箱）
 * @since 2020-2-26
 * @version 1.0
 */
class MainActivity : AppCompatActivity() {
    /** 总权重 */
    private val ALL_WEIGHT = 100
    /** 视图控件权重 */
    private val VIEW_WEIGHT = 100
    /** 最小时长 */
    private val MIN_DURATION = 1000L
    /** 起始时间 */
    private val START_TIME = 0L
    /** 结束时间 */
    private val END_TIME = 9999999L

    /** 双向滑动选择器 */
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
