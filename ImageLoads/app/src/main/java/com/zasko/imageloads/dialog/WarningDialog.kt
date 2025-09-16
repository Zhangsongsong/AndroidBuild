package com.zasko.imageloads.dialog

import android.app.Activity
import android.view.LayoutInflater
import com.zasko.imageloads.databinding.DialogWarningBinding
import com.zasko.imageloads.utils.onClick

class WarningDialog(
    private val activity: Activity,
    private val clickBack: (state: Int, dialog: WarningDialog) -> Unit = { _, _ -> },
    private val dismissBack: (state: Int) -> Unit = {}
) : CenterDefaultDialog<DialogWarningBinding>(activity) {

    override fun createViewBinding(inflater: LayoutInflater): DialogWarningBinding {
        return DialogWarningBinding.inflate(inflater)
    }

    override fun bindingStart(binding: DialogWarningBinding) {
        super.bindingStart(binding)
        status = VALUE_NEGATIVE
        binding.negativeTv.onClick {
            status = VALUE_NEGATIVE
            clickBack.invoke(status, this)
        }
        binding.positiveTv.onClick {
            status = VALUE_POSITIVE
            clickBack.invoke(status, this)
        }
    }

    override fun dismiss() {
        super.dismiss()

    }


}