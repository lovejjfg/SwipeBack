/*
 * Copyright (c) 2018.  Joe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lovejjfg.swipeback

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.support.annotation.ColorInt
import android.support.annotation.IntRange
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.view.MotionEvent
import android.view.ViewGroup
import java.util.ArrayList

/**
 * Created by joe on 2018/11/13.
 * Email: lovejjfg@gmail.com
 */
class SwipeBackHelper(
    private val targetView: ViewGroup,
    private val callback: Callback
) {
    private var mBezierPoints: ArrayList<PointF>? = null
    private val mControlPoints = ArrayList<PointF>(5)
    private var xResult: Float = 0F
    private var yResult: Float = 0F
    private var rawX: Float = 0F
    private val path = Path()
    private val arrowPath = Path()
    private val pathPaint = Paint()
    private var animator: ValueAnimator
    private var percent: Float = 0F
    private var arrowLength: Float = 0F
    private var arrowPaint: Paint
    private var maxPeakValue = 0f
    private var xDefaultOffset = 50F
    private var edgeSize = 30F
    private var trackingEdges = 0
    private var currentEdgeType = 0
    private var downEvent: MotionEvent? = null

    private val callbackRunnable = Runnable {
        callback.onBackReleased(currentEdgeType)
        currentEdgeType = 0
    }
    private val context = targetView.context

    init {
        trackingEdges = callback.getEdgeTrackingEnabled()
        val peakValue = callback.getShapeMaxPeak() * REAL_PEAK_RATIO
        maxPeakValue = if (peakValue != 0f) peakValue else context.dip2px(24f * REAL_PEAK_RATIO)
        edgeSize = context.dip2px(24f)
        xDefaultOffset = context.dip2px(24f)
        arrowLength = maxPeakValue * 0.1f
        pathPaint.color = callback.getShapeColor()
        pathPaint.style = Paint.Style.FILL
        pathPaint.isAntiAlias = true

        arrowPaint = Paint(pathPaint)
        arrowPaint.color = callback.getArrowColor()
        arrowPaint.strokeWidth = arrowLength / 3f
        arrowPaint.strokeCap = Paint.Cap.ROUND
        arrowPaint.style = Paint.Style.STROKE

        animator = ValueAnimator()
        animator.setObjectValues(1f, 0)
        animator.interpolator = LINEAR_INTERPOLATOR
        animator.addUpdateListener { animation ->
            val animatedFraction = animation.animatedFraction
            val min = Math.min(maxPeakValue, rawX)
            invalidate((1 - animatedFraction) * min)
        }
    }

    private fun buildBezierPoints() {
        initControlPoint(xResult)
        val order = mControlPoints.size - 1
        for (t in 0..FRAME) {
            val delta = t * 1.0f / FRAME
            PointFPool.setPointF(
                t,
                if (t == 0 || t == FRAME) 0f else calculateX(order, 0, delta),
                calculateY(order, 0, delta)
            )
        }
        mBezierPoints = PointFPool.points
    }

    private fun invalidate(rawX: Float) {
        if (rawX < 0) {
            return
        }
        val min = if (rawX != 0f) Math.min(rawX, maxPeakValue) else 0F
        percent = min / maxPeakValue
        xResult = INTERPOLATOR.getInterpolation(percent) * maxPeakValue
        buildBezierPoints()
        targetView.invalidate()
    }

    fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) {
            return false
        }
        val action = ev.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downEvent = ev
                return isEdges(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                return isEdges(ev)
            }
        }
        return false
    }

    private fun isEdges(ev: MotionEvent): Boolean {
        val downEvent = this.downEvent ?: return false
        return if (ev.x < edgeSize && trackingEdges and trackingEdges and EDGE_LEFT == EDGE_LEFT) {
            if (ev.x - downEvent.x >= 0) {
                currentEdgeType = EDGE_LEFT
            }
            true
        } else if (ev.x > targetView.width - edgeSize && trackingEdges and EDGE_RIGHT == EDGE_RIGHT) {
            if (ev.x - downEvent.x <= 0) {
                currentEdgeType = EDGE_RIGHT
            }
            true
        } else {
            currentEdgeType = 0
            false
        }
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        if (currentEdgeType == 0) {
            return false
        }
        if (animator.isRunning) {
            return false
        }
        calculateRawX(event)
        calculateRawY(event)
        val action = event.action
        when (action) {
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                handleUpEvent()
                return true
            }
        }
        if (currentEdgeType == EDGE_LEFT || currentEdgeType == EDGE_RIGHT) {
            invalidate(rawX)
            return true
        }
        return false
    }

    private fun calculateRawY(event: MotionEvent) {
        yResult = event.y
    }

    private fun calculateRawX(event: MotionEvent) {
        if (currentEdgeType == EDGE_LEFT) {
            rawX = (event.x - xDefaultOffset) * 0.8f
        } else if (currentEdgeType == EDGE_RIGHT) {
            rawX = (targetView.width - event.x - xDefaultOffset * 2) * 0.8f
        }
    }

    private fun handleUpEvent() {
        if (animator.isRunning) {
            animator.cancel()
        }
        val min = Math.min(ANIMATOR_DURATION, Math.abs(rawX).toLong())
        if (percent >= 1) {
            animator.duration = (min * 0.8f).toLong()
            animator.start()
            targetView.postDelayed(callbackRunnable, min)
        } else {
            animator.duration = min
            animator.start()
        }
        downEvent = null
    }

    fun onDispatchDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        val points = mBezierPoints ?: return
        pathPaint.alpha = (percent * callback.getShapeAlpha()).toInt()
        path.reset()
        points.forEachIndexed { index, pointF ->
            if (index == 0) path.moveTo(pointF.x, pointF.y) else path.lineTo(pointF.x, pointF.y)
        }
        path.close()
        canvas.save()
        canvas.drawPath(path, pathPaint)
        drawArrow(canvas)
    }

    private fun drawArrow(canvas: Canvas) {
        val offset = if (currentEdgeType == EDGE_LEFT) 1f else -1f
        arrowPaint.alpha = MIN_ALPHA + (percent * PERCENT_MAX_ALPHA).toInt()
        arrowPath.reset()
        if (percent > GOLDEN_RATIO) {
            val sin = Math.sin(Math.toRadians((START_ANGLE + (1.0 - percent) * PERCENT_MAX_ANGLE))).toFloat()
            val cos = Math.cos(Math.toRadians((START_ANGLE + (1.0 - percent) * PERCENT_MAX_ANGLE))).toFloat()
            val dy = arrowLength * sin
            val dx = arrowLength * cos * offset
            val x = if (currentEdgeType == EDGE_LEFT) xResult * 0.2f else targetView.width - xResult * 0.2f

            arrowPath.moveTo(x, yResult - dy)
            arrowPath.lineTo(x - dx, yResult)
            arrowPath.lineTo(x, yResult + dy)
            canvas.drawPath(arrowPath, arrowPaint)
        } else {
            val dy = arrowLength * 2.5f * percent
            val x =
                if (currentEdgeType == EDGE_LEFT) xResult * 0.2f * 2.5f * percent
                else targetView.width - xResult * 0.2f * 2.5f * percent
            arrowPath.moveTo(x, yResult - dy)
            arrowPath.lineTo(x, yResult + dy)
            canvas.drawPath(arrowPath, arrowPaint)
        }
    }

    private fun initControlPoint(min: Float) {
        if (currentEdgeType == EDGE_LEFT) {
            addControlPoint(0, 0f, yResult - maxPeakValue * 1.5f)
            addControlPoint(1, 0f, yResult - maxPeakValue * 1.5f * GOLDEN_RATIO)
            addControlPoint(2, min, yResult)
            addControlPoint(3, 0f, yResult + maxPeakValue * 1.5f * GOLDEN_RATIO)
            addControlPoint(4, 0f, yResult + maxPeakValue * 1.5f)
        } else if (currentEdgeType == EDGE_RIGHT) {
            val width = targetView.width.toFloat()
            addControlPoint(0, width, yResult - maxPeakValue * 1.5f)
            addControlPoint(1, width, yResult - maxPeakValue * 1.5f * GOLDEN_RATIO)
            addControlPoint(2, width - min, yResult)
            addControlPoint(3, width, yResult + maxPeakValue * 1.5f * GOLDEN_RATIO)
            addControlPoint(4, width, yResult + maxPeakValue * 1.5f)
        }
    }

    private fun addControlPoint(pos: Int, x: Float, y: Float) {
        if (pos >= mControlPoints.size) {
            mControlPoints.add(PointF(x, y))
        } else {
            mControlPoints[pos].set(x, y)
        }
    }

    private fun calculateX(i: Int, j: Int, t: Float): Float {
        return if (i == 1) {
            (1 - t) * mControlPoints[j].x + t * mControlPoints[j + 1].x
        } else (1 - t) * calculateX(i - 1, j, t) + t * calculateX(i - 1, j + 1, t)
    }

    private fun calculateY(i: Int, j: Int, t: Float): Float {
        return if (i == 1) {
            (1 - t) * mControlPoints[j].y + t * mControlPoints[j + 1].y
        } else (1 - t) * calculateY(i - 1, j, t) + t * calculateY(i - 1, j + 1, t)
    }

    private fun Context.dip2px(dpValue: Float): Float {
        val density = this.resources.displayMetrics.density
        return (dpValue * density + 0.5f)
    }

    private object PointFPool {

        val points = ArrayList<PointF>(FRAME + 1)

        fun setPointF(pos: Int, x: Float, y: Float) {
            if (pos >= points.size) {
                points.add(PointF(x, y))
            } else {
                points[pos].set(x, y)
            }
        }
    }

    abstract class Callback {
        open fun onBackReleased(type: Int) = Unit
        @ColorInt
        open fun getShapeColor(): Int {
            return Color.BLACK
        }

        @IntRange(from = 0, to = 255)
        open fun getShapeAlpha(): Int {
            return SHAPE_DEFAULT_ALPHA
        }

        open fun getShapeMaxPeak(): Float = 0f

        @ColorInt
        open fun getArrowColor(): Int {
            return Color.WHITE
        }

        open fun getEdgeTrackingEnabled(): Int = EDGE_LEFT
    }

    companion object {
        private const val FRAME = 50
        private val INTERPOLATOR = LinearOutSlowInInterpolator()
        private val LINEAR_INTERPOLATOR = FastOutLinearInInterpolator()
        private const val MIN_ALPHA = 55
        private const val ANIMATOR_DURATION = 200L
        private const val PERCENT_MAX_ALPHA = 200
        private const val SHAPE_DEFAULT_ALPHA = 210

        private const val START_ANGLE = 50f
        private const val PERCENT_MAX_ANGLE = 65

        private const val GOLDEN_RATIO = 0.382f
        private const val REAL_PEAK_RATIO = 2.7272727f

        const val EDGE_LEFT = 1
        const val EDGE_RIGHT = 2
    }
}
