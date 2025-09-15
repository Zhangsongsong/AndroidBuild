package com.zasko.imageloads.detail

interface DownloadListener {

    fun onStartDownload(all: Int, dir: String) {

    }

    fun onOneStartDownload(index: Int, all: Int, dir: String, fileName: String) {

    }

    fun onOneEndDownLoad(index: Int, all: Int, dir: String, fileName: String) {

    }

    fun onEndDownload(all: Int, dir: String) {

    }
}

open class DownloadListenerAbs : DownloadListener {
    override fun onStartDownload(all: Int, dir: String) {
        super.onStartDownload(all, dir)
    }

    override fun onOneStartDownload(index: Int, all: Int, dir: String, fileName: String) {
        super.onOneStartDownload(index, all, dir, fileName)
    }

    override fun onOneEndDownLoad(index: Int, all: Int, dir: String, fileName: String) {
        super.onOneEndDownLoad(index, all, dir, fileName)
    }

    override fun onEndDownload(all: Int, dir: String) {
        super.onEndDownload(all, dir)
    }
}