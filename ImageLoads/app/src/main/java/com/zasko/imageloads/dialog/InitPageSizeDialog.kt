package com.zasko.imageloads.dialog

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.zasko.imageloads.databinding.DialogInitPageListBinding

class InitPageSizeDialog(private val activity: AppCompatActivity) : CenterDefaultDialog<DialogInitPageListBinding>(activity = activity) {

    override fun createViewBinding(inflater: LayoutInflater): DialogInitPageListBinding {
        return DialogInitPageListBinding.inflate(inflater)
    }


}