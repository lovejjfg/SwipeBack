## SwipeBack

![gif](https://github.com/lovejjfg/SwipeBack/blob/master/art/swipeback.gif?raw=true)

[ ![Download](https://api.bintray.com/packages/lovejjfg/maven/SwipeBack/images/download.svg) ](https://bintray.com/lovejjfg/maven/SwipeBack/_latestVersion)

## Release Note:

### 0.0.1
 * support Both LEFT and RIGHT side swipe.
 * support more callback

#### How to use:
* 0. Add dependency:

            implementation 'com.lovejjfg:swipeback:latestVersion'


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
* 3. Callback Methods:

            /**
             * Callback release when the shape is fully showed.
             */
            fun onBackReleased(type: Int) = Unit

            /**
             * Callback current percent when swipe or release, size change in [0f,1f].
             */
            fun onShapeChange(percent: Float) = Unit

            /**
             * Callback the Shape's color, Default color is FUll BLACK because is's match to the screen border.
             */
            @ColorInt
            fun getShapeColor(): Int = Color.BLACK

            /**
             * Callback the Shape's alpha , Default value is 255.
             */
            @IntRange(from = 0, to = 255)
            fun getShapeAlpha(): Int = SHAPE_DEFAULT_ALPHA

            /**
             * Callback whether change the shape's alpha during swiping, Default is depend on current alpha is full (255).
             */
            fun isShapeAlphaGradient(): Boolean = getShapeAlpha() != SHAPE_DEFAULT_ALPHA

            /**
             * Callback the max peak size of the shape NOTE: it's not a exact value.
             */
            fun getShapeMaxPeak(): Float = 0f

            /**
             * Callback the Arrow color, the Arrow size width and so on are depend on method getShapeMaxPeak(),
             * DEFAULT color is WHITE.
             */
            @ColorInt
            fun getArrowColor(): Int = Color.WHITE

            /**
             * Callback the Edge size DEFAULT is 24dp.
             */
            fun getEdgeSize(): Float = -1F

            /**
             * Callback the support edge swipe side，support EDGE_LEFT EDGE_RIGHT or both.
             */
            @IntRange(from = 1, to = 3)
            fun getEdgeTrackingEnabled(): Int = EDGE_LEFT

PS: It's kotlin lib, so you should SUPPORT Kotlin at first!
