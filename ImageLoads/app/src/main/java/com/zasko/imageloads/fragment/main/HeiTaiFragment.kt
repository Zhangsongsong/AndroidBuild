package com.zasko.imageloads.fragment.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zasko.imageloads.databinding.FragmentNormalBinding
import com.zasko.imageloads.fragment.ThemePagerFragment

class HeiTaiFragment : ThemePagerFragment() {

    private lateinit var binding: FragmentNormalBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNormalBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {

    }
}