package com.zasko.imageloads.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.viewbinding.ViewBinding
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BindLife

abstract class CenterDefaultDialog<Binding : ViewBinding>(activity: AppCompatActivity, styleId: Int = R.style.Theme_DefaultDialog) :
    AppCompatDialog(activity, styleId), BindLife by BindLife() {

    companion object {
        const val VALUE_POSITIVE = 0
        const val VALUE_NEGATIVE = 1

        const val VALUE_BY_USER = -1
    }

    val binding: Binding by lazy {
        createViewBinding(LayoutInflater.from(context))
    }


    open val gravity: Int = Gravity.CENTER
    abstract fun createViewBinding(inflater: LayoutInflater): Binding

    open fun cancelable() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setOnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    backPressed()
                }
                return@setOnKeyListener true
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                return@setOnKeyListener true
            }

            return@setOnKeyListener false
        }
        bindingCreate(binding)
    }


    open fun bindingCreate(binding: Binding) {

    }

    open fun bindingStart(binding: Binding) {

    }


    open fun backPressed() {
        if (cancelable()) {
            this.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        bindingStart(binding)
    }

    override fun dismiss() {
        super.dismiss()
        lifeCompositeDisposable.clear()
        cancelJobs()
    }


}