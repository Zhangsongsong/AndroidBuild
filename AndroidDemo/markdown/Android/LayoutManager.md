
### GridLayoutManager 在item显示之后调用scroll不滑动问题
```kotlin
private var mTopSmoothScroller: TopSmoothScroller? = null

    private fun initView(context: Context) {
        mTopSmoothScroller = TopSmoothScroller(context)
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        super.smoothScrollToPosition(recyclerView, state, position)
        mTopSmoothScroller?.let {
            it.targetPosition = position
            startSmoothScroll(it)
        }
    }


    private class TopSmoothScroller(context: Context?) : LinearSmoothScroller(context) {

        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START
        }
    }
```