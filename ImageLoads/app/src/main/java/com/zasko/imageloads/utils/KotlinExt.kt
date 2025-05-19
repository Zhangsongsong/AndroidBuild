package com.zasko.imageloads.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers


fun <T : Any> Single<T>.switchThread(): Single<T> = this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())


inline val Dp.dpToPx: Float
    @Composable get() = with(LocalDensity.current) {
        this@dpToPx.toPx()
    }

