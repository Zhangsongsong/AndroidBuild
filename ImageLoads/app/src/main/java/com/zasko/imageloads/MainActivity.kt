package com.zasko.imageloads

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.ActivityMainBinding
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.dpToPx
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : BaseActivity() {

    companion object {

        private const val TAG = "MainActivity"

        var screenWidth = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            contentView()
        }
    }

    private val isLoadMore = AtomicBoolean(false)

    private var mAdapter: MainLoadsAdapter? = null
    private var recyclerView: RecyclerView? = null


    @Composable
    private fun contentView() {
        screenWidth = LocalConfiguration.current.screenWidthDp.dp.dpToPx.toInt()

        val statusBarHeight = LocalDensity.current.run {
            WindowInsets.statusBars.getTop(this).toDp()
        }
        ModalNavigationDrawer(drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
//                Text("Drawer title", modifier = Modifier.padding(16.dp))
//                HorizontalDivider()
                NavigationDrawerItem(label = { Text(text = "Drawer Item") }, selected = false, onClick = {})
            }
        }) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                LayoutInflater.from(it).inflate(R.layout.activity_main, null, false)
            }) { rootView ->
                val binding = ActivityMainBinding.bind(rootView)
                mAdapter = MainLoadsAdapter()
                binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
                    this.isAutoMeasureEnabled = true
                    this.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
                }
                binding.recyclerView.adapter = mAdapter
                binding.recyclerView.itemAnimator = null
                recyclerView = binding.recyclerView
            }
        }
        startLoadImages()
    }

    override fun onDestroy() {
        super.onDestroy()
        ImageLoadsManager.disLoadingImage()
    }

    @Preview
    @Composable
    private fun preView() {
        contentView()
    }

    private fun startLoadImages() {
        ImageLoadsManager.looperLoadImage {
            ImageLoadsManager.getImageData().doOnSuccess { result ->
                Glide.with(this).asBitmap().load(result.data).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        mAdapter?.addData(MainLoadsInfo(url = result.data, width = resource.width, height = resource.height))
                        recyclerView?.post {
//                            recyclerView?.smoothScrollToPosition((mAdapter?.getDataSize() ?: 1) - 1)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            }.bindLife()
        }
    }
}