package com.lilei.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView

/**
 * 时间文本视图
 * @author lilei
 * @since 2020-2-29
 * @version 1.0
 */
class TimeTextView constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
    : TextView(context, attrs, defStyle) {
    /** 默认文字大小 */
    private val DEFAULT_TEXT_SIZE = 12
    /** 没有值 */
    private val NO_VALUE = 0F

    /** 画笔 */
    private var mPaint: Paint? = null

    /** 屏幕宽度 */
    private var mScreenWidth = 0

    /** 时间可追加串 */
    private var mTimeSB: StringBuffer? = null
    /** 起始时间  */
    private var mTimeStart: String? = null
    /** 起始时间文本右边界*/
    private var mStartRight = NO_VALUE
    /** 起始时间文本 */
    private var mStartLeft = NO_VALUE
    /** 起始时间文本宽度 */
    private var mTimeStartWidth = NO_VALUE

    /** 截止时间 */
    private var mTimeEnd: String? = null
    /** 截止时间宽度 */
    private var mTimeEndWidth = NO_VALUE
    /** 截止时间文本左边界 */
    private var mEndLeft = NO_VALUE

    /** 文本绘制基线*/
    private var mBaseLine = NO_VALUE
    /** 控件高度 */
    private var mHeight = NO_VALUE
    /** 已有高度标志  */
    private var isHaveHeight = false

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    init {
        mTimeSB = StringBuffer()
        initPaint()
    }

    /**
     * 初始化画笔
     */
    private fun initPaint() {
        mPaint = Paint()
        // 设置颜色
        mPaint!!.color = Color.parseColor("#73000000")
        // 获取放缩密度
        val scaleDensity = resources.displayMetrics.scaledDensity
        // 设置文本大小
        mPaint!!.textSize = (scaleDensity * DEFAULT_TEXT_SIZE + 0.5F)
    }

    /**
     * 计算一些信息
     */
    private fun measures() {
        // 有起始文本
        if (!TextUtils.isEmpty(mTimeStart)) {
            // 计算宽度
            mTimeStartWidth = mPaint?.measureText(mTimeStart!!)?: NO_VALUE
            // 计算左边界
            mStartLeft = mStartRight - mTimeStartWidth
        }
        // 有截止文本
        if (!TextUtils.isEmpty(mTimeEnd)) {
            mTimeEndWidth = mPaint?.measureText(mTimeEnd!!)?: NO_VALUE
            if (mScreenWidth > 0 && mEndLeft + mTimeEndWidth >= mScreenWidth) {
                mEndLeft = mScreenWidth - mTimeEndWidth - 30
            }
        }
        // 未计算过基线则计算
        if (mHeight != NO_VALUE && mPaint != null) {
            // 实例化范围矩形
            val bound = Rect()
            // 以数字9为例计算文本高度
            mPaint?.getTextBounds("9", 0, 1, bound)
            val fontMetrics = mPaint!!.fontMetrics
            // 计算绘制基线
            mBaseLine = -(fontMetrics!!.descent + fontMetrics!!.ascent) / 2 + mHeight / 2
            isHaveHeight = true
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            // 获取高度
            mHeight = measuredHeight.toFloat()
            mScreenWidth = context.resources.displayMetrics.widthPixels
            // 测量
            measures()
            // 刷新视图
            if (isHaveHeight) {
                invalidate()
                isHaveHeight = false
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mPaint != null) {
            if (!TextUtils.isEmpty(mTimeStart)) {
                // 绘制起始时间
                canvas?.drawText(mTimeStart!!, mStartLeft, mBaseLine, mPaint!!)
            }
            if (!TextUtils.isEmpty(mTimeEnd)) {
                // 绘制截止时间
                canvas?.drawText(mTimeEnd!!, mEndLeft, mBaseLine, mPaint!!)
            }
        }
    }

    /**
     * 毫秒时间
     * Long类型时间转换成视频时长
     */
    private fun format(time: Long?): String? {
        if (time == null) {
            return null
        } else {
            mTimeSB?.delete(0, mTimeSB?.length?: 0)
            val hour = time / (60 * 60 * 1000)
            val minute = (time - hour * 60 * 60 * 1000) / (60 * 1000)
            val second = (time - hour * 60 * 60 * 1000 - minute * 60 * 1000) / 1000

            if (hour > 0) {
                mTimeSB?.append(if (hour > 9) hour else "0$hour")
                mTimeSB?.append(":")
            }
            if (minute <= 0L) {
                mTimeSB?.append("00")
            } else {
                mTimeSB?.append(if(minute > 9) minute else "0$minute")
            }
            mTimeSB?.append(":")
            if (second <= 0L) {
                mTimeSB?.append("00")
            } else {
                mTimeSB?.append(if (second > 9) second else "0$second")
            }
            return mTimeSB.toString()
        }
    }

    /**
     * 设置时间范围
     * @param timeStart 起始时间
     * @param startRight 起始时间文本右边界
     * @param timeEnd 截止时间
     * @param endLeft 截止时间左边界
     */
    fun setDuration(timeStart: Long, startRight: Float = mStartRight, timeEnd: Long,
                    endLeft: Float = mEndLeft) {
        // 格式化起始时间
        val start = format(timeStart)
        val end = format(timeEnd)
        // 记录信息
        mTimeStart = start
        mTimeEnd = end
        mStartRight = startRight
        mEndLeft = endLeft
        // 测量
        measures()
        // 刷新视图
        invalidate()
    }
}