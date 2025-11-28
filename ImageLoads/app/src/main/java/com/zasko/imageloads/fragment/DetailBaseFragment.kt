package com.zasko.imageloads.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.zasko.imageloads.R
import com.zasko.imageloads.adapter.DetailImagesAdapter
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.databinding.FragmentDetailImageBinding
import com.zasko.imageloads.dialog.CenterDefaultDialog
import com.zasko.imageloads.dialog.DownloadTipDialog
import com.zasko.imageloads.dialog.WarningDialog
import com.zasko.imageloads.utils.loadImageWithInside
import com.zasko.imageloads.utils.onClick

open class DetailBaseFragment : LoadBaseFragment() {


    companion object {
        const val KEY_DATA = "key_data"
        private const val TAG = "DetailBaseFragment"
    }


    lateinit var imageLoadsInfo: ImageLoadsInfo
    lateinit var binding: FragmentDetailImageBinding
    lateinit var mAdapter: DetailImagesAdapter

    var downloadDialog: DownloadTipDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageLoadsInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(KEY_DATA, ImageLoadsInfo::class.java)!!
            } else {
                it.getSerializable(KEY_DATA) as ImageLoadsInfo
            }
            LogComponent.printD(tag = TAG, message = "mainLoadInfo:${imageLoadsInfo}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailImageBinding.inflate(inflater)
        bindStart()
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentCons) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        return binding.root
    }

    open fun bindStart() {
        binding.backIv.onClick {
            activity?.finish()
        }
        binding.downloadIv.onClick {
            handleClickDownload()
        }

        mAdapter = DetailImagesAdapter(loadMore = {
            loadMoreData()
        })

        binding.coverIv.loadImageWithInside(url = imageLoadsInfo.url)
        binding.pictureRecycler.layoutManager = GridLayoutManager(context, 2)
        binding.pictureRecycler.adapter = mAdapter
    }

    fun showDownloadDialog() {
        if (downloadDialog == null) {
            runInAct { act ->
                downloadDialog = DownloadTipDialog(activity = act)
            }
        }
        downloadDialog?.show()
    }


    open fun handleClickDownload() {
    }


    fun showWarningDialog(positive: () -> Unit) {
        runInAct { act ->
            val dialog = WarningDialog(act, clickBack = { status, d ->
                when (status) {
                    CenterDefaultDialog.VALUE_POSITIVE -> {
                        positive.invoke()
                    }
                }
                d.dismiss()
            })
            dialog.show()
            dialog.updateALlText(
                content = act.getString(R.string.if_oval_download), negative = act.getString(R.string.no), positive = act.getString(R.string.yes)
            )

        }
    }


}