package com.lilei.widgets

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.widget_double_slide_seek_view.view.*
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 双向选择器控件
 * @author libai
 * @since 2020-2-26
 * @version 1.0
 */
class DoubleSlideSeekView constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
    : RelativeLayout(context, attrs, defStyle) {
    /** 更新进度指针位置消息*/
    private val MSG_UPDATE_PROGRESS_POSITION = 0x0001
    /** 更新位置间隔时间 */
    private val INTERVAL_DURATION = 20L
    /** 恢复进度条指针位置 */
    private val MSG_RESET_POSITION = 0x0002
    private val MIN_DURATION = 500

    /** 默认手柄宽度 */
    private val DEFAULT_HANDLER_WIDTH = 16
    private val DEFAULT_SEEK_VIEW_WEIGHT = 0
    private val DEFAULT_PROGRESS_WIDTH = 13
    /** 默认总权重 */
    private val DEFAULT_WEIGHTS = 100
    /** 1秒对应的毫秒时长 */
    private val SECOND_VALUE = 1000

    /** 上下文 */
    private var mContext: Context? = null
    /** 双向选择器视图权重 */
    private var mSeekViewWeight = DEFAULT_SEEK_VIEW_WEIGHT
    /** 双向选择器视图宽度 */
    private var mSeekViewWidth = 0
    /** 双向选择器左边界 */
    private var mSeekViewLeft = 0
    /** 右边界 */
    private var mSeekViewRight = 0
    /** 左手柄数值 */
    private var mLeftHandlerValue = 0L
    /** 右手柄数值 */
    private var mRightHandlerValue = 0L
    /** 当前滑条的实际宽度 */
    private var mCurrentWidth = 0
    /** 手柄宽度 */
    private var mHandlerWidth = 0
    /** 进度条宽度 */
    private var mProgressWidth = 0
    /** 进度条间距 */
    private var mMargin = 0
    /** 将时间范围映射到视图宽度的映射比例 */
    private var mMapRate = 1F

    private var mMinDuration = 0L
    private var mMinWidth = 0

    /** 总权重 */
    private var mWeights = DEFAULT_WEIGHTS
    /** 单位权重对应的宽度 */
    private var mOneWeightWidth = 0F

    /** 按下x坐标 */
    private var mDownX = 0

    /** 时间范围改变监听 */
    private var mListener: OnRangeTimeChangedListener? = null

    /** 进度指针运动速度 */
    private var mSpeed = 0F
    /** 当前进度指针右边界坐标 */
    private var mProgressCurrentRight = 0F
    /** 需要动画标志 */
    private var isAnimationNeed = false

    private var mStartTime = 0L

    /** 消息句柄 */
    private var mHandler: Handler? = null
    /** 消息处理回调 */
    private var mCallback = Handler.Callback {
        if (it != null) {
            when (it.what) {
                MSG_UPDATE_PROGRESS_POSITION -> {
                    updateProgressPosition()
                }
                MSG_RESET_POSITION -> {
                    resetProgressPosition()
                }
            }
        }
        return@Callback false
    }

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    /**
     * 更新进度指针位置
     */
    private fun updateProgressPosition() {
        // 没有记录的值
        if (mProgressCurrentRight == 0F) {
            // 获取初始值：为左手柄的右边界
            mProgressCurrentRight = (iv_picker_left.right - mMargin + mProgressWidth).toFloat()
        }
        // 移动进度指针
        mProgressCurrentRight += mSpeed
        // 超出范围标志
        var isOutBound = false
        // 越界了
        if (mProgressCurrentRight >= iv_picker_right.left + mMargin) {
            // 设置为最大值
            mProgressCurrentRight = (iv_picker_right.left + mMargin).toFloat()
            // 记录越界
            isOutBound = true
        }
        // 更新视图位置
        layoutView(ll_progress, (mProgressCurrentRight - mProgressWidth).toInt(), mProgressCurrentRight.toInt())
        // 越界
        if (isOutBound) {
            // 发送回到默认位置消息：左手柄右边界
            mHandler?.sendEmptyMessageDelayed(MSG_RESET_POSITION, INTERVAL_DURATION)
            // 记录不需要动画
            isAnimationNeed = false
        }else
        // 发送更新下个位置消息
            mHandler?.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS_POSITION, INTERVAL_DURATION)
    }

    /**
     * 重置进度指针位置：左手柄右边界
     */
    fun resetProgressPosition() {
        // 清除所有消息
        mHandler?.removeCallbacksAndMessages(null)
        // 更新位置
        layoutProgressBar(iv_picker_left.right - mMargin,
            iv_picker_left.right - mMargin + mProgressWidth)
        updateProgressTime()
    }

    /**
     * 摆放进度指针位置
     * @param left 左边界
     * @param right 右边界
     */
    private fun layoutProgressBar(left: Int, right: Int) {
        ll_progress.layout(left, ll_progress.top, right, ll_progress.bottom)
        // 更新右边界
        mProgressCurrentRight = right.toFloat()
    }

    init {
        mContext = context
        mHandlerWidth = dip2px(mContext, DEFAULT_HANDLER_WIDTH)
        mProgressWidth = dip2px(mContext, DEFAULT_PROGRESS_WIDTH)
        mMargin = dip2px(mContext, 6)
        LayoutInflater.from(mContext).inflate(R.layout.widget_double_slide_seek_view, this, true)
        setListener()
        mHandler = Handler(Looper.getMainLooper(), mCallback)
    }

    /**
     * 转为px值
     * @param context 上下文
     * @param px
     */
    private fun dip2px(context: Context?, px: Int): Int {
        if (context != null) {
            val resources = context.resources
            val density = resources.displayMetrics.density
            return (px * density + 0.5F).toInt()
        }
        return px
    }

    /**
     * 设置监听
     */
    private fun setListener() {
        iv_picker_left.setOnTouchListener(LeftOnTouchListener())
        iv_picker_right.setOnTouchListener(RightOnTouchListener())
        ll_progress.setOnTouchListener(ProgressTouchListener())
    }

    /**
     * 重新计算权重
     * @param timeRange 时间段
     */
    private fun recomputeWeight(timeRange: Long) {
        val numLength = (timeRange / SECOND_VALUE).toString().length
        val mapSize = computeMapSize(numLength)
        if ((timeRange / SECOND_VALUE) < (mapSize / 2)) {
            mWeights /= 2
            mOneWeightWidth *= 2
        }
    }

    /**
     * 计算
     */
    private fun compute(leftTime: Long, rightTime: Long) {
        // 记录时间上下界
        mLeftHandlerValue = leftTime
        mRightHandlerValue = rightTime
        mStartTime = leftTime
        // 计算时间范围
        val timeRange = mRightHandlerValue - mLeftHandlerValue
        // 计算实际宽度
        val realWidth = if (mSeekViewWeight > DEFAULT_SEEK_VIEW_WEIGHT) getFixedWeightWidth()
        else getUnfixedWeightWidth(timeRange)
        // 计算映射比例
        mMapRate = timeRange.toFloat() / realWidth
        // 计算视图控件的左边界
        mSeekViewLeft = (mCurrentWidth - realWidth).shr(1) + dip2px(mContext, 20)
        // 右边界
        mSeekViewRight = mSeekViewLeft + realWidth + mHandlerWidth.shl(1)
        // 初始化
        setDoubleSeekViewPosition(mSeekViewLeft)
        // 设置时间和边界
        tv_time.setDuration(leftTime, (mSeekViewLeft + mHandlerWidth).toFloat(), rightTime,
            (mSeekViewRight - mHandlerWidth).toFloat())
        if (timeRange > 0) {
            computeSpeed(realWidth / timeRange.toDouble() * 20)
        }
        mMinWidth = (mMinDuration / mMapRate).toInt()
    }

    /**
     * 获取固定权重对应宽度
     */
    private fun getFixedWeightWidth(): Int {
        return (mSeekViewWeight.toFloat() / mWeights * mCurrentWidth).toInt()
    }

    /**
     * 获取非固定权重对应宽度
     * @param timeRange 时间范围
     */
    private fun getUnfixedWeightWidth(timeRange: Long): Int {
        recomputeWeight(timeRange)
        // 计算上届的位数，该位数作为映射的对应参考值
        var numLength = (timeRange / SECOND_VALUE).toString().length
        // 计算放缩比例
        val scaleRate = mWeights / computeMapSize(numLength).toFloat()
        // 计算时间范围对应的视图宽度
        return (((timeRange / SECOND_VALUE) * scaleRate) * mOneWeightWidth).toInt()
    }

    /**
     * 计算映射参考比例
     * @param num 位数
     */
    private fun computeMapSize(num: Int): Long {
        var result = 1L
        var numTemp = num
        while (numTemp > 0) {
            result *= 10
            numTemp--
        }
        return result
    }

    /**
     * 设置双向选择器位置
     * @param start 左边界
     * @param end 右边界
     */
    private fun setDoubleSeekViewPosition(start: Int){
        // 对左手柄设置左间距
        val leftLayoutParams = iv_picker_left.layoutParams as MarginLayoutParams
        leftLayoutParams.leftMargin = start
        iv_picker_left.layoutParams = leftLayoutParams
        val rightLayoutParams = iv_picker_right.layoutParams as MarginLayoutParams
        rightLayoutParams.rightMargin = start
        iv_picker_right.layoutParams = rightLayoutParams
    }

    /**
     * 计算运动速度（四舍五入后保留3位小数）
     * @param number 速度值
     */
    private fun computeSpeed(number: Double) {
        val format = DecimalFormat("0.###")
        format.roundingMode = RoundingMode.HALF_UP
        mSpeed = format.format(number).toFloat()
    }

    /**
     * 更新时间
     * @param offset 偏移值
     * @param isLeft 左手柄移动标志
     */
    private fun updateTime(offset: Int, isLeft: Boolean) {
        val timeDelta = (offset * mMapRate).toLong()
        if (isLeft) {
            mLeftHandlerValue += timeDelta
            if (mLeftHandlerValue <= mStartTime) {
                mLeftHandlerValue = mStartTime
            }
        } else {
            mRightHandlerValue += timeDelta
            if (mRightHandlerValue <= mStartTime + mMinDuration) {
                mRightHandlerValue = mStartTime + mMinDuration
                mLeftHandlerValue = mStartTime
            }
        }
        // 更新时间和边界
        tv_time.setDuration(mLeftHandlerValue, (iv_picker_left.left + mHandlerWidth).toFloat(),
            mRightHandlerValue, iv_picker_right.left.toFloat())
        // 通知调用方时间范围改变
        mListener?.onRangeTimeChanged(mLeftHandlerValue, mRightHandlerValue, isLeft)
    }

    inner class LeftOnTouchListener : OnTouchListener{
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event == null){
                return true
            }
            val action = event.action
            when (action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mListener?.onDown(false)
                    mDownX = event.x.toInt()
                    // 重置位置，滑动手柄时都需要重置进度指针位置
                    resetProgressPosition()
                }
                MotionEvent.ACTION_MOVE -> {
                    //计算移动的距离
                    val offsetX = event.x.toInt() - mDownX
                    var left = iv_picker_left.left + offsetX
                    var right = iv_picker_left.right + offsetX
                    if(left <= mSeekViewLeft){
                        left = mSeekViewLeft
                        right = mSeekViewLeft + mHandlerWidth
                    }else if (left >= iv_picker_right.left - mHandlerWidth - mMinWidth){
                        left = iv_picker_right.left - mHandlerWidth - mMinWidth
                        right = iv_picker_right.left - mMinWidth
                    }
                    // 更新时间
                    updateTime(left - iv_picker_left.left, true)
                    //调用layout方法来重新放置它的位置
                    layoutView(iv_picker_left, left, right)
                    // 更新进度针视图位置
                    layoutProgressBar(right - mMargin, right - mMargin + mProgressWidth)
                    // 更新中间视图位置
                    layoutView(tv_picker_central, right, iv_picker_right.left)
                }
                MotionEvent.ACTION_UP -> mListener?.onSelectFinish(mLeftHandlerValue, false)
            }
            return true
        }
    }

    inner class RightOnTouchListener : OnTouchListener{
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event == null){
                return true
            }
            val action = event.action
            when (action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mListener?.onDown(false)
                    mDownX = event.x.toInt()
                    // 重置位置，滑动手柄时都需要重置进度指针位置
                    resetProgressPosition()
                }
                MotionEvent.ACTION_MOVE -> {
                    //计算移动的距离
                    val offsetX = event.x.toInt() - mDownX
                    var left = iv_picker_right.left + offsetX
                    var right = iv_picker_right.right + offsetX
                    if(left <= iv_picker_left.right + mMinWidth){
                        left = iv_picker_left.right + mMinWidth
                        right = iv_picker_left.right + mHandlerWidth + mMinWidth
                    }else if (left >= mSeekViewRight - mHandlerWidth) {
                        left = mSeekViewRight - mHandlerWidth
                        right = mSeekViewRight
                    }
                    updateTime(left - iv_picker_right.left, false)
                    //调用layout方法来重新放置它的位置
                    layoutView(iv_picker_right, left, right)
                    layoutView(tv_picker_central, iv_picker_left.right, left)
                }
                MotionEvent.ACTION_UP -> mListener?.onSelectFinish(mRightHandlerValue, false)
            }
            return true
        }
    }

    inner class ProgressTouchListener: OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event == null){
                return true
            }
            val action = event.action
            when (action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mListener?.onDown(true)
                    mDownX = event.x.toInt()
                    mHandler?.removeCallbacksAndMessages(null)
                }
                MotionEvent.ACTION_MOVE -> {
                    //计算移动的距离
                    val offsetX = event.x.toInt() - mDownX
                    var left = ll_progress.left + offsetX
                    var right = ll_progress.right + offsetX
                    if(left <= iv_picker_left.right - mMargin){
                        left = iv_picker_left.right - mMargin
                        right = left + mProgressWidth
                    }else if (right >= iv_picker_right.left + mMargin) {
                        right = iv_picker_right.left + mMargin
                        left = right - mProgressWidth
                    }
                    //调用layout方法来重新放置它的位置
                    layoutProgressBar(left, right)
                    // 更新进度值
                    updateProgressTime()
                }
                MotionEvent.ACTION_UP -> {
                    // 需要动画，则立即发送更新位置消息
                    if (isAnimationNeed) {
                        mHandler?.sendEmptyMessage(MSG_UPDATE_PROGRESS_POSITION)
                    }
                    mListener?.onSelectFinish(getProgressTime(), true)
                }
            }
            return true
        }
    }

    /**
     * 更新进度时间
     * @param offset 偏移量
     */
    private fun updateProgressTime() {
        val offset = ll_progress.right - mMargin - mSeekViewLeft - mHandlerWidth
        val timeDelta = (offset * mMapRate).toLong()
        mListener?.onProgressTime(timeDelta + mStartTime)
    }

    /**
     * 布局控件
     * @param view 控件
     * @param left 左边界
     * @param right 右边界
     */
    private fun layoutView(view: View, left: Int, right: Int) {
        view.layout(left, view.top, right, view.bottom)
    }

    /**
     * 设置视图宽度和总权重
     * @param width 宽度
     * @param weights 总权重
     */
    fun setWidthAndWeight(width: Int, weights: Int = DEFAULT_WEIGHTS,
                          seekViewWeight: Int = DEFAULT_SEEK_VIEW_WEIGHT) {
        mSeekViewWidth = width
        // 计算实际滑动的距离值，实际为视图控件宽度减去左右手柄宽度
        mCurrentWidth = mSeekViewWidth - mHandlerWidth.shl(1)
        // 保存和计算单位权重对应的宽度
        mWeights = weights
        mSeekViewWeight = seekViewWeight
        mOneWeightWidth = mCurrentWidth / mWeights.toFloat()
    }

    /**
     * 设置时间范围
     * @param leftTime 起始时间
     * @param rightTime 截止时间
     */
    fun setRangeTime(leftTime: Long, rightTime: Long) {
        compute(leftTime, rightTime)
    }

    /**
     * 需要动画标志
     */
    fun isAnimationNeed(): Boolean {
        return isAnimationNeed
    }

    /**
     * 设置双向选择器的最小时长
     * @param minDuration 最小时长
     */
    fun setMinDurationLimit(minDuration: Long) {
        mMinDuration = minDuration
    }

    /**
     * 获取
     */
    private fun getProgressTime(): Long {
        val offset = ll_progress.right - mMargin - mSeekViewLeft - mHandlerWidth
        val timeDelta = (offset * mMapRate).toLong()
        return timeDelta + mStartTime
    }

    /**
     * 切换动画（开启或暂停）
     */
    fun switchAnimation(isStart: Boolean) {
        if (isStart) {
            startAnimation()
        } else {
            pauseAnimation()
        }
    }

    /**
     * 暂停动画
     */
    private fun pauseAnimation() {
        // 清除所有的消息
        mHandler?.removeCallbacksAndMessages(null)
        // 记录动画结束
        isAnimationNeed = false
    }

    /**
     * 开始动画
     */
    private fun startAnimation() {
        mHandler?.removeCallbacksAndMessages(null)
        // 立即发送更新位置消息
        mHandler?.sendEmptyMessage(MSG_UPDATE_PROGRESS_POSITION)
        // 记录有动画
        isAnimationNeed = true
    }

    /**
     * 设置进度条位置
     */
    fun setProgressBarPosition(time: Long) {
        val tempTime = if (time > mRightHandlerValue) mRightHandlerValue else time
        // 计算指定时间与起始时间的差值
        val timeDelta = tempTime - mStartTime
        // 计算进度条左边的位置
        var left = (timeDelta / mMapRate).toInt() + mSeekViewLeft + mHandlerWidth - mMargin
        if (left <= iv_picker_left.right - mMargin) {
            left = iv_picker_left.right - mMargin
        }
        layoutProgressBar(left, left + mProgressWidth)
    }

    /**
     * 释放
     */
    fun release() {
        mHandler?.removeCallbacksAndMessages(null)
        mHandler = null
    }

    /**
     * 设置时间范围变化监听
     * @param listener 监听
     */
    fun setOnRangeTimeChangedListener(listener: OnRangeTimeChangedListener) {
        mListener = listener
    }

    /**
     * 时间范围变化监听
     */
    interface OnRangeTimeChangedListener {
        /**
         * 回调
         * @param startTime 起始时间
         * @param endTime 截止时间
         * @param isStartChang 起始时间改变标志
         */
        fun onRangeTimeChanged(startTime: Long, endTime: Long, isStartChange: Boolean)

        /**
         * 当前进度时间
         * @param currentTime 当前时间
         */
        fun onProgressTime(currentTime: Long)

        /**
         * 选择结束
         * @param time 选中的时间
         * @param isProgressBar 进度条标志
         */
        fun onSelectFinish(time: Long, isProgressBar: Boolean)

        /**
         * 按下监听
         * @param isProgressBar 进度条标志
         */
        fun onDown(isProgressBar: Boolean)
    }
}