## SwipeBack

![gif](https://github.com/lovejjfg/SwipeBack/blob/master/art/swipeback.gif?raw=true)

#### how to use:

* 1. Create Your `SwipeBackHelper` in Layout like this:

            private var swipeBackHelper: SwipeBackHelper =
                SwipeBackHelper(this, object : Callback() {
                    override fun onBackReleased() {
                        (getContext() as? Activity)?.finish()
                    }
                })

* 2. Override method:

            override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
                return swipeBackHelper.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(ev: MotionEvent?): Boolean {
                return swipeBackHelper.onTouchEvent(ev)
            }

            override fun dispatchDraw(canvas: Canvas?) {
                super.dispatchDraw(canvas)
                swipeBackHelper.onDispatchDraw(canvas)
            }
