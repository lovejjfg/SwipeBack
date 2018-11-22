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
    private val cb: Callback
) {
    private var edges: Boolean = false
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
    private var xMaxValue = 200F
    private var xDefaultOffset = 50F

    private val callbackRunnable = Runnable {
        cb.onBackReleased()
    }
    private val context = targetView.context

    init {
        xMaxValue = context.dip2px(70f).toFloat()
        xDefaultOffset = context.dip2px(25f).toFloat()
        pathPaint.color = cb.getShapeColor()
        pathPaint.style = Paint.Style.FILL
        pathPaint.isAntiAlias = true

        arrowPaint = Paint(pathPaint)
        arrowPaint.color = cb.getArrowColor()
        arrowPaint.strokeWidth = context.dip2px(2f).toFloat()
        arrowLength = context.dip2px(6f).toFloat()
        arrowPaint.strokeCap = Paint.Cap.ROUND
        arrowPaint.style = Paint.Style.STROKE

        animator = ValueAnimator()
        animator.setObjectValues(1f, 0)
        animator.interpolator = LINEAR_INTERPOLATOR
        animator.duration = 200
        animator.addUpdateListener { animation ->
            val animatedFraction = animation.animatedFraction
            invalidate((1 - animatedFraction) * rawX)
        }
    }

    private fun buildBezierPoints() {
        initControlPoint(xResult)
        val order = mControlPoints.size - 1
        for (t in 0 until FRAME) {
            val delta = t * 1.0f / FRAME
            PointFPool.setPointF(
                t,
                calculateX(order, 0, delta),
                calculateY(order, 0, delta)
            )
        }
        mBezierPoints = PointFPool.points
    }

    private fun invalidate(rawX: Float) {
        if (rawX < 0) {
            return
        }
        val min = if (rawX != 0f) Math.min(rawX, xMaxValue) else 0F
        percent = min / xMaxValue
        xResult = INTERPOLATOR.getInterpolation(percent) * xMaxValue
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
                edges = isEdges(ev)
                return edges
            }
        }
        return false
    }

    private fun isEdges(ev: MotionEvent): Boolean {
        return ev.x < 10
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        if (!edges) {
            return false
        }
        if (animator.isRunning) {
            return false
        }
        rawX = (event.x - xDefaultOffset) * 0.8f
        yResult = event.y
        val action = event.action
        when (action) {
            MotionEvent.ACTION_UP -> {
                handleUpEvent()
                return true
            }
        }
        if (edges) {
            invalidate(rawX)
            return true
        }
        return false
    }

    private fun handleUpEvent() {
        if (percent >= 1) {
            mBezierPoints = null
            targetView.invalidate()
            targetView.post(callbackRunnable)
        } else {
            if (animator.isRunning) {
                animator.cancel()
            }
            animator.start()
        }
    }

    fun onDispatchDraw(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        val points = mBezierPoints ?: return
        pathPaint.alpha = (percent * 210).toInt()
        path.reset()
        for (pointF in points) {
            path.lineTo(pointF.x, pointF.y)
        }
        path.close()
        canvas.drawPath(path, pathPaint)
        if (percent > 0.4) {
            arrowPaint.alpha = 55 + (percent * 200).toInt()
            arrowPath.reset()
            val sin = Math.sin(Math.toRadians((50 + (1 - percent) * 65.0))).toFloat()
            val cos = Math.cos(Math.toRadians((50 + (1 - percent) * 65.0))).toFloat()
            val dy = arrowLength * sin
            val dx = arrowLength * cos
            val x = xResult * 0.2f
            arrowPath.moveTo(x, yResult - dy)
            arrowPath.lineTo(x - dx, yResult)
            arrowPath.lineTo(x, yResult + dy)
            canvas.drawPath(arrowPath, arrowPaint)
        } else {
            if (percent == 0F) {
                return
            }
            arrowPaint.alpha = 55 + (percent * 200).toInt()
            arrowPath.reset()
            val dy = arrowLength * 2.5f * percent
            val x = xResult * 0.2f * 2.5f * percent
            arrowPath.moveTo(x, yResult - dy)
            arrowPath.lineTo(x, yResult + dy)
            canvas.drawPath(arrowPath, arrowPaint)
        }
    }

    private fun initControlPoint(min: Float) {
        addControlPoint(0, 0f, yResult - xMaxValue * 1.5f)
        addControlPoint(1, 0f, yResult - xMaxValue * 0.573f)
        addControlPoint(2, min, yResult)
        addControlPoint(3, 0f, yResult + xMaxValue * 0.573f)
        addControlPoint(4, 0f, yResult + xMaxValue * 1.5f)
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

    private fun Context.dip2px(dpValue: Float): Int {
        val density = this.resources.displayMetrics.density
        return (dpValue * density + 0.5).toInt()
    }

    private object PointFPool {

        internal val points = ArrayList<PointF>(FRAME)

        internal fun setPointF(pos: Int, x: Float, y: Float) {
            if (pos >= points.size) {
                points.add(PointF(x, y))
            } else {
                points[pos].set(x, y)
            }
        }
    }

    abstract class Callback {
        open fun onBackReleased() {}
        @ColorInt
        open fun getShapeColor(): Int {
            return Color.BLACK
        }

        @ColorInt
        open fun getArrowColor(): Int {
            return Color.WHITE
        }
    }

    companion object {
        private const val FRAME = 100
        private val INTERPOLATOR = LinearOutSlowInInterpolator()
        private val LINEAR_INTERPOLATOR = FastOutLinearInInterpolator()
    }
}

