package com.zasko.imageloads.base

import com.zasko.imageloads.components.LogComponent
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Job


interface BindLife {
    val lifeCompositeDisposable: CompositeDisposable


    fun <T : Any> Observable<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({

            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Next: ${it.toString()}")
        }, {

            LogComponent.printE(tag = this@BindLife.javaClass.name, message = it.toString())
        }, {
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Complete")
        }))
    }

    fun <T : Any> Flowable<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Next: ${it.toString()}")
        }, {
            LogComponent.printE(tag = this@BindLife.javaClass.name, message = it.toString())
        }, {
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Complete")
        }))
    }

    fun Completable.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Complete")
        }, {
            LogComponent.printE(tag = this@BindLife.javaClass.name, message = it.toString())
        }))
    }

    fun <T : Any> Single<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = it.toString())
        }, {
            LogComponent.printE(tag = this@BindLife.javaClass.name, message = it.toString())
        }))
    }

    fun <T : Any> Maybe<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Success: $it")
        }, {
            LogComponent.printE(tag = this@BindLife.javaClass.name, message = it.toString())
        }, {
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Complete")
        }))
    }

    fun <T : Any> addBindLife(obj: Observable<T>) {
        lifeCompositeDisposable.add(obj.subscribe({
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Success: $it")
        }, {
            LogComponent.printE(tag = this@BindLife.javaClass.name, message = it.toString())
        }, {
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = "Complete")
        }))
    }

    fun <T : Any> addBindLife(obj: Single<T>) {
        lifeCompositeDisposable.add(obj.subscribe({
            LogComponent.printD(tag = this@BindLife.javaClass.name, message = it.toString())
        }, {
            LogComponent.printE(tag = this@BindLife.javaClass.name, message = it.toString())
        }))
    }

    val jobs: MutableList<Job>
    fun addJobBindLife(job: Job) {
        jobs.add(job)
    }

    fun cancelJobs() {
        jobs.forEach {
            it.cancel()
        }
    }
}

fun BindLife(): BindLife = object : BindLife {
    override val lifeCompositeDisposable: CompositeDisposable = CompositeDisposable()

    /*         CoroutineScope(Dispatchers.Main).launch {
             IKLog.d("$TAG CoroutineScope start ${Thread.currentThread().name}")
             val result = withContext(Dispatchers.IO) {
                 delay(10000)
                 IKLog.d("$TAG CoroutineScope witchContext ${Thread.currentThread().name}")
                 "result-------"
             }
             IKLog.d("$TAG CoroutineScope end result:${result} ${Thread.currentThread().name}")
         }.bindLife()*/
    override val jobs: MutableList<Job>
        get() = emptyArray<Job>().toMutableList()
}

