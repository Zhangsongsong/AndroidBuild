package com.zasko.imageloads

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.ActivityMainBinding
import com.zasko.imageloads.manager.ImageLoadsManager
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : BaseActivity() {

    companion object {

        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            contentView()
        }
        ImageLoadsManager.getImageData()
    }

    private val isLoadMore = AtomicBoolean(false)

    private var mAdapter: MainLoadsAdapter? = null
    private var recyclerView: RecyclerView? = null

    @Composable
    private fun contentView() {
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
                LogComponent.printD(tag = TAG, message = "AndroidView ${binding.recyclerView}")
                mAdapter = MainLoadsAdapter()
//                binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
                binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
                    this.isAutoMeasureEnabled = true
                    this.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
                }
                binding.recyclerView.adapter = mAdapter
                binding.recyclerView.itemAnimator = null
            }
        }
        ImageLoadsManager.looperLoadImage {
            ImageLoadsManager.getImageData().doOnSuccess { result ->
                if (!TextUtils.isEmpty(result.data)) {
                    mAdapter?.addData(result)
                    recyclerView?.smoothScrollToPosition(mAdapter?.getDataSize() ?: 0)
                }
            }.bindLife()
        }
    }

    @Preview
    @Composable
    private fun preView() {
        contentView()
    }
}