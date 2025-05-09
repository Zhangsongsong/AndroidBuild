
#### 仿抖音加载视频动画
```kotlin
<View
android:id = "@+id/bufferLoadingView"
android:layout_width = "match_parent"
android:layout_height = "2dp"
android:layout_gravity = "bottom"
android:layout_marginBottom = "65dp"
android:background = "@color/white"
android:visibility = "visible" / >

binding?.bufferLoadingView?.let {
    it.startAnimation(AnimationHelper.getVideoBufferLoadingAni())
}

fun getVideoBufferLoadingAni(): AnimationSet {
    val scale = ScaleAnimation(0.3f, 1f, 1f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
    val alpha = AlphaAnimation(1f, 0.2f)
    scale.repeatCount = -1
    alpha.repeatCount = -1
    val set = AnimationSet(true)
    set.addAnimation(scale)
    set.addAnimation(alpha)
    set.duration = 500
    return set
}
```