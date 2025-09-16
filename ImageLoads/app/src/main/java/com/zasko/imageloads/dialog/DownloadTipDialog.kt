package com.zasko.imageloads.dialog

import android.app.Activity
import android.view.LayoutInflater
import com.zasko.imageloads.databinding.DialogDownloadTipBinding

class DownloadTipDialog(private val activity: Activity) : CenterDefaultDialog<DialogDownloadTipBinding>(activity = activity) {

    override fun createViewBinding(inflater: LayoutInflater): DialogDownloadTipBinding {
        return DialogDownloadTipBinding.inflate(inflater)
    }


    override fun cancelable(): Boolean {
        return false
    }

    override fun bindingStart(binding: DialogDownloadTipBinding) {
        super.bindingStart(binding)
        setCanceledOnTouchOutside(false)
    }

    fun updateProgress(progress: Float, text: String) {
        binding.waterView.setProgress(progress)
        binding.progressTv.text = text
    }

    fun setTitleText(text: String) {
        binding.titleTv.text = text
    }


    private val stringBuilder = StringBuilder()
    fun addGettingText(text: String) {
        stringBuilder.append(text).append("\n")
        binding.gettingDescTv.text = stringBuilder
    }


}