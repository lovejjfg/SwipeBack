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

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.lovejjfg.swipeback.SwipeBackHelper
import com.lovejjfg.swipeback.SwipeBackHelper.Callback

/**
 * Created by joe on 2018/10/13.
 * Email: lovejjfg@gmail.com
 */
@Suppress("unused")
class SwipeFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : FrameLayout(context, attrs, defStyleAttr) {
    private var swipeBackHelper: SwipeBackHelper =
        SwipeBackHelper(this, object : Callback() {
            override fun onBackReleased() {
                (getContext() as? Activity)?.finish()
            }
        })

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return swipeBackHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return swipeBackHelper.onTouchEvent(ev) || super.onTouchEvent(ev)
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        swipeBackHelper.onDispatchDraw(canvas)
    }
}
