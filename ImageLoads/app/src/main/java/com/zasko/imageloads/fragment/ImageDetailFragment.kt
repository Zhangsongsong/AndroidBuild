package com.zasko.imageloads.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.FragmentDetailImageBinding

class ImageDetailFragment : MainLoadFragment() {


    companion object {
        const val KEY_DATA = "key_data"
        private const val TAG = "ImageDetailFragment"
    }

    private var mainLoadsInfo: MainLoadsInfo? = null

    private lateinit var binding: FragmentDetailImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val mainLoadsInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(KEY_DATA, MainLoadsInfo::class.java)
            } else {
                it.getSerializable(KEY_DATA)
            }

            LogComponent.printD(tag = TAG, message = "mainLoadInfo:${mainLoadsInfo}")
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailImageBinding.inflate(inflater)
        return binding.root
    }

}