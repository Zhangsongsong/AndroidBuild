
### 超链接点击后会出现背景颜色
```kotlin
textView.highlightColor = context.getColor(R.color.transparent)
```

### 支持滑动
```kotlin
<TextView
    android:scrollbars="vertical"
    ...
/>

textView.movementMethod = ScrollingMovementMethod()

```