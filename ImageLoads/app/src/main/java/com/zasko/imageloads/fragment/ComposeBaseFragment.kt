package com.zasko.imageloads.fragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean

abstract class ComposeBaseFragment : Fragment() {

    companion object {
        var screenWidth = 0

        const val KEY_DATA = "key_data"
    }

    var isLoadEnd = AtomicBoolean(false)
    var isLoadingMore = AtomicBoolean(false)

    private var isInitResume = false
    protected var composeRequestScope: CoroutineScope? = null
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (screenWidth <= 0) {
            screenWidth = context.resources.displayMetrics.widthPixels
        }
    }

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?): View {
        composeRequestScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FragmentContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isInitResume) {
            isInitResume = true
            initByResume()
        }
    }

    final override fun onDestroyView() {
        onClearComposeView()
        composeRequestScope?.cancel()
        composeRequestScope = null
        super.onDestroyView()
    }

    @Composable
    protected abstract fun FragmentContent()

    open fun initByResume() {
    }

    protected open fun onClearComposeView() {
    }
}
