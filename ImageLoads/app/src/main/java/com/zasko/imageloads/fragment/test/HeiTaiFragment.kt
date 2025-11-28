package com.zasko.imageloads.fragment.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zasko.imageloads.databinding.FragmentNormalBinding
import com.zasko.imageloads.fragment.LoadBaseFragment

class HeiTaiFragment : LoadBaseFragment() {

    private lateinit var binding: FragmentNormalBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNormalBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {

    }
}