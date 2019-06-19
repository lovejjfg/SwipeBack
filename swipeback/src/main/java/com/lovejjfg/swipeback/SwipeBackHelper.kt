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
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
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
    private var bezierPoints: ArrayList<PointF>? = null
    private val controlPoints = ArrayList<PointF>(5)
    private var xResult: Float = 0F
    private var yResult: Float = 0F
    private var rawX: Float = 0F
    private val path = Path()
    private val arrowPath = Path()
    private val pathPaint = Paint()
    private var returnAnimator: ValueAnimator
    private var percent: Float = 0F
    private var arrowLength: Float = 0F
    private var arrowPaint: Paint
    private var maxPeakValue = 0f
    private var xDefaultOffset = 50F
    private var edgeSize = 30F
    private var trackingEdges = 0
    private var currentEdgeType = 0
    private var animatorDuration: Long = DEFAULT_ANIMATOR_DURATION
    private var downEvent: MotionEvent? = null

    private val callbackRunnable = Runnable {
        callback.onBackReleased(currentEdgeType)
        currentEdgeType = 0
    }
    private val context = targetView.context

    init {
        animatorDuration = callback.getAnimatorDuration()
        trackingEdges = callback.getEdgeTrackingEnabled()
        val peakValue = callback.getShapeMaxPeak() * REAL_PEAK_RATIO
        maxPeakValue = if (peakValue != 0f) peakValue else context.dip2px(24f * REAL_PEAK_RATIO)
        val size = callback.getEdgeSize()
        edgeSize = if (size < 0) context.dip2px(24f) else size
        val defaultXOffset = callback.getDefaultXOffset()
        xDefaultOffset = if (defaultXOffset < 0) context.dip2px(24f) else defaultXOffset
        arrowLength = maxPeakValue * 0.1f
        pathPaint.color = callback.getShapeColor()
        pathPaint.style = Paint.Style.FILL
        pathPaint.isAntiAlias = true

        arrowPaint = Paint(pathPaint)
        arrowPaint.color = callback.getArrowColor()
        arrowPaint.strokeWidth = arrowLength / 3f
        arrowPaint.strokeCap = Paint.Cap.ROUND
        arrowPaint.style = Paint.Style.STROKE

        returnAnimator = ValueAnimator()
        returnAnimator.setObjectValues(1f, 0)
        returnAnimator.interpolator = LINEAR_INTERPOLATOR
        returnAnimator.addUpdateListener { animation ->
            val animatedFraction = animation.animatedFraction
            val min = Math.min(maxPeakValue, rawX)
            invalidate((1 - animatedFraction) * min)
        }
    }

    private fun buildBezierPoints() {
        initControlPoint(xResult)
        val order = controlPoints.size - 1
        for (t in 0..FRAME) {
            val delta = t * 1.0f / FRAME
            PointFPool.setPointF(
                t,
                calculateX(order, 0, delta),
                calculateY(order, 0, delta)
            )
        }
        bezierPoints = PointFPool.points
    }

    private fun invalidate(rawX: Float) {
        if (rawX < 0) {
            return
        }
        val min = if (rawX != 0f) Math.min(rawX, maxPeakValue) else 0F
        percent = min / maxPeakValue
        callback.onShapeChange(percent)
        xResult = INTERPOLATOR.getInterpolation(percent) * maxPeakValue
        buildBezierPoints()
        targetView.invalidate()
    }

    fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) {
            return false
        }
        when (ev.action) {
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
        return if (ev.x < edgeSize && trackingEdges and EDGE_LEFT == EDGE_LEFT) {
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
        if (returnAnimator.isRunning) {
            return false
        }
        calculateRawX(event)
        calculateRawY(event)
        when (event.action) {
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
            rawX = (event.x - xDefaultOffset) * GOLDEN_RATIO_LARGE
        } else if (currentEdgeType == EDGE_RIGHT) {
            rawX = (targetView.width - event.x - xDefaultOffset) * GOLDEN_RATIO_LARGE
        }
    }

    private fun handleUpEvent() {
        if (returnAnimator.isRunning) {
            returnAnimator.cancel()
        }
        val min = Math.min(animatorDuration, Math.abs(rawX).toLong())
        if (percent >= 1) {
            returnAnimator.duration = (min * GOLDEN_RATIO).toLong()
            returnAnimator.start()
            targetView.postDelayed(callbackRunnable, (min * GOLDEN_RATIO_LARGE).toLong())
        } else {
            returnAnimator.duration = min
            returnAnimator.start()
        }
        downEvent = null
    }

    fun onDispatchDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        val points = bezierPoints ?: return
        pathPaint.alpha =
            if (callback.isShapeAlphaGradient()) (percent * callback.getShapeAlpha()).toInt()
            else callback.getShapeAlpha()
        path.reset()
        points.forEachIndexed { index, pointF ->
            if (index == 0) path.moveTo(pointF.x, pointF.y) else path.lineTo(pointF.x, pointF.y)
        }
        path.close()
        canvas.save()
        canvas.translate(if (currentEdgeType == EDGE_LEFT) -1f else 1f, 0f)
        canvas.drawPath(path, pathPaint)
        if (percent == 0f) {
            canvas.restore()
            return
        }
        drawArrow(canvas)
        canvas.restore()
    }

    private fun drawArrow(canvas: Canvas) {
        arrowPaint.alpha = MIN_ALPHA + (percent * PERCENT_MAX_ALPHA).toInt()
        arrowPath.reset()
        if (percent > GOLDEN_RATIO) {
            val sin = Math.sin(Math.toRadians((START_ANGLE + (1.0 - percent) * PERCENT_MAX_ANGLE))).toFloat()
            val cos = Math.cos(Math.toRadians((START_ANGLE + (1.0 - percent) * PERCENT_MAX_ANGLE))).toFloat()
            val dy = arrowLength * sin
            val dx = arrowLength * cos * if (currentEdgeType == EDGE_LEFT) 1f else -1f
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
        if (pos >= controlPoints.size) {
            controlPoints.add(PointF(x, y))
        } else {
            controlPoints[pos].set(x, y)
        }
    }

    private fun calculateX(i: Int, j: Int, t: Float): Float {
        return if (i == 0 || i == FRAME)
            if (currentEdgeType == EDGE_LEFT) 0f
            else targetView.width.toFloat() + 1
        else
            if (i == 1) {
                (1 - t) * controlPoints[j].x + t * controlPoints[j + 1].x
            } else (1 - t) * calculateX(i - 1, j, t) + t * calculateX(i - 1, j + 1, t)
    }

    private fun calculateY(i: Int, j: Int, t: Float): Float {
        return if (i == 1) {
            (1 - t) * controlPoints[j].y + t * controlPoints[j + 1].y
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

        /**
         * Callback release when the shape is fully showed.
         */
        open fun onBackReleased(type: Int) = Unit

        /**
         * Callback current percent when swipe or release, size change in [0f,1f].
         */
        open fun onShapeChange(percent: Float) = Unit

        /**
         * Callback the Shape's color, Default color is FUll BLACK because is's match to the screen border.
         */
        @ColorInt
        open fun getShapeColor(): Int = Color.BLACK

        /**
         * Callback the Shape's alpha , Default value is 255.
         */
        @IntRange(from = 0, to = 255)
        open fun getShapeAlpha(): Int = SHAPE_DEFAULT_ALPHA

        /**
         * Callback whether change the shape's alpha during swiping, Default is depend on current alpha is full (255).
         */
        open fun isShapeAlphaGradient(): Boolean = getShapeAlpha() != SHAPE_DEFAULT_ALPHA

        /**
         * Callback the max peak size of the shape NOTE: it's not a exact value.
         */
        open fun getShapeMaxPeak(): Float = 0f

        /**
         * Callback the Arrow color, the Arrow size width and so on are depend on method getShapeMaxPeak(),
         * DEFAULT color is WHITE.
         */
        @ColorInt
        open fun getArrowColor(): Int = Color.WHITE

        /**
         * Callback the Edge size DEFAULT is 24dp.
         */
        open fun getEdgeSize(): Float = -1F

        /**
         * Callback the support edge swipe side，support EDGE_LEFT EDGE_RIGHT or both.
         */
        @IntRange(from = 1, to = 3)
        open fun getEdgeTrackingEnabled(): Int = EDGE_LEFT

        /**
         * Callback the support edge swipe side，support EDGE_LEFT EDGE_RIGHT or both.
         */
        open fun getAnimatorDuration(): Long = DEFAULT_ANIMATOR_DURATION

        /**
         * Callback the x default offset size DEFAULT is 24dp.
         */
        open fun getDefaultXOffset(): Float = -1F
    }

    companion object {
        private const val FRAME = 50
        private val INTERPOLATOR = LinearOutSlowInInterpolator()
        private val LINEAR_INTERPOLATOR = FastOutLinearInInterpolator()
        private const val MIN_ALPHA = 55
        private const val DEFAULT_ANIMATOR_DURATION = 160L
        private const val PERCENT_MAX_ALPHA = 200
        private const val SHAPE_DEFAULT_ALPHA = 255

        private const val START_ANGLE = 50f
        private const val PERCENT_MAX_ANGLE = 65

        private const val GOLDEN_RATIO = 0.382f
        private const val GOLDEN_RATIO_LARGE = 0.618f
        private const val REAL_PEAK_RATIO = 2.7272727f

        const val EDGE_LEFT = 1
        const val EDGE_RIGHT = 2
        const val EDGE_LEFT_RIGHT = 3
    }
}
