package com.zasko.imageloads

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.ActivityMainBinding
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.manager.html.HtmlParseManager
import com.zasko.imageloads.utils.dpToPx
import com.zasko.imageloads.viewmodel.MainViewModel

class MainActivity : BaseActivity() {

    companion object {

        private const val TAG = "MainActivity"

        var screenWidth = 0
    }


    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initBindLife(this)
        setContent {
            contentView()
        }

    }


    private var mAdapter: MainLoadsAdapter? = null
    private var recyclerView: RecyclerView? = null

    @Composable
    private fun contentView() {
        screenWidth = LocalConfiguration.current.screenWidthDp.dp.dpToPx.toInt()

        ModalNavigationDrawer(drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                drawerItems()
            }
        }) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                LayoutInflater.from(it).inflate(R.layout.activity_main, null, false)
            }) { rootView ->
                val binding = ActivityMainBinding.bind(rootView)
                mAdapter = MainLoadsAdapter {
                    viewModel.loadMore(context = this, adapter = mAdapter)
                }
                binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
                    this.isAutoMeasureEnabled = true
                    this.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
                }
                binding.recyclerView.adapter = mAdapter
                binding.recyclerView.itemAnimator = null
                recyclerView = binding.recyclerView

                viewModel.drawerItemClick(context = this, id = MainViewModel.DRAWER_ITEMS.first(), adapter = mAdapter)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ImageLoadsManager.disLoadingImage()
    }

    @Composable
    private fun drawerItems() {
        MainViewModel.DRAWER_ITEMS.forEachIndexed { index, info ->
            NavigationDrawerItem(label = { Text(text = getString(info)) }, selected = index == 0, onClick = {
                viewModel.drawerItemClick(context = this@MainActivity, id = info, adapter = mAdapter)
            })
            HorizontalDivider()

        }

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
                        mAdapter?.addData(mutableListOf(MainLoadsInfo(url = result.data, width = resource.width, height = resource.height)))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            }.bindLife()
        }
    }
}