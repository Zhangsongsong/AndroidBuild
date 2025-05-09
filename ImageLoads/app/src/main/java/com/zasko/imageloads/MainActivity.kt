package com.zasko.imageloads

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.zasko.imageloads.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            contentView()
        }
    }

    private val isLoadMore = AtomicBoolean(false)

    @Composable
    private fun contentView() {
        ModalNavigationDrawer(drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
//                Text("Drawer title", modifier = Modifier.padding(16.dp))
//                HorizontalDivider()
                NavigationDrawerItem(label = { Text(text = "Drawer Item") }, selected = false, onClick = {})
            }
        }) {
            AndroidView(factory = {
                LayoutInflater.from(it).inflate(R.layout.activity_main, null, false)
            }) { rootView ->

            }
        }
    }

    @Preview
    @Composable
    private fun preView() {
        contentView()
    }
}