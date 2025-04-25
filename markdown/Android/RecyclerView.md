
### 设置可滑动判断距离
```kotlin
recyclerView.setScrollingTouchSlop

ViewConfiguration类中有判断是否滑动的若干配置项默认值设置。
getScaledTouchSlop() 手指的移动要大于这个距离才算滑动 ， 返回的mTouchSlop来自TOUCH_SLOP的取值 。
getScaledDoubleTapSlop() 第一个触点和后面触点的距离小于这个数值算双击 ， 返回的mDoubleTapTouchSlop来自mTouchSlop 。
getScaledPagingTouchSlop() 手指的移动要大于这个距离才算翻页 ， 返回的mPagingTouchSlop来自mTouchSlop * 2。
getScaledEdgeSlop() 获得一个触摸移动的最小像素值 。 也就是说 ， 只有超过了这个值 ， 才代表我们该滑屏处理 ；
getScaledMaximumFlingVelocity() 用户滑动时的最大速度 ， 单位是每秒多少像素;
getScaledMinimumFlingVelocity() 用户滑动时的最小速度
```
