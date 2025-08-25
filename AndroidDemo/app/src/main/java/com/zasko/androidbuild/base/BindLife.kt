package com.zasko.androidbuild.base

import com.zasko.androidbuild.components.LogComponent
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
            LogComponent.printD(this@BindLife.javaClass.name, "Next: ${it.toString()}")
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, "Complete")
        }))
    }

    fun <T : Any> Flowable<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(this@BindLife.javaClass.name, "Next: ${it.toString()}")
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, "Complete")
        }))
    }

    fun Completable.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(this@BindLife.javaClass.name, "Complete")
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }))
    }

    fun <T : Any> Single<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }))
    }

    fun <T : Any> Maybe<T>.bindLife() {
        lifeCompositeDisposable.add(this.subscribe({
            LogComponent.printD(this@BindLife.javaClass.name, "Success: $it")
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, "Complete")
        }))
    }

    fun <T : Any> addBindLife(obj: Observable<T>) {
        lifeCompositeDisposable.add(obj.subscribe({
            LogComponent.printD(this@BindLife.javaClass.name, "Success: $it")
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, "Complete")
        }))
    }

    fun <T : Any> addBindLife(obj: Single<T>) {
        lifeCompositeDisposable.add(obj.subscribe({
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }, {
            LogComponent.printD(this@BindLife.javaClass.name, it.toString())
        }))
    }

    val jobs: MutableList<Job>

    fun Job.bindLife() {
        jobs.add(this)
    }

    fun addJobBindLife(job: Job) {
        job.bindLife()
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
             LogComponent.printD("$TAG CoroutineScope start ${Thread.currentThread().name}")
             val result = withContext(Dispatchers.IO) {
                 delay(10000)
                 LogComponent.printD("$TAG CoroutineScope witchContext ${Thread.currentThread().name}")
                 "result-------"
             }
             LogComponent.printD("$TAG CoroutineScope end result:${result} ${Thread.currentThread().name}")
         }.bindLife()*/
    override val jobs: MutableList<Job>
        get() = emptyArray<Job>().toMutableList()
}

