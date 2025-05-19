package com.zasko.imageloads.base

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

open class BaseViewModel : ViewModel() {

    lateinit var life: BindLife

    open fun initBindLife(life: BindLife) {
        this.life = life
    }

    fun <T : Any> Single<T>.bindLife() {
        life.addBindLife(this)
    }


    fun <T : Any> Observable<T>.bindLife() {
        life.addBindLife(this)
    }
}