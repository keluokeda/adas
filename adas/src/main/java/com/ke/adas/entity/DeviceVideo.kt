package com.ke.adas.entity

data class DeviceVideo(
    /**
     * 名称
     */
    val name: String,
    /**
     * 日期
     */
    val date: String,
    /**
     * 时间
     */
    val time: String,
    /**
     * 是否已下载
     */
    val downloaded: Boolean = false,

    /**
     * 下载视频的路径
     */
    val path: String? = null,

    /**
     * 下载进度
     */
    val progress: Int = 0,

    /**
     * 是否正在下载
     */
    val downloading: Boolean = false
) {
    /**
     * 该视频已经被删除
     */
    fun onDelete(): DeviceVideo {
        return DeviceVideo(
            name, date, time, false, null, progress, downloading
        )
    }

    /**
     * 该视频已经下载完成
     */
    fun onDownloaded(path: String): DeviceVideo {
        return DeviceVideo(
            name, date, time, true, path, 0, false
        )
    }

    /**
     * 开始下载
     */
    fun onStartDownload(): DeviceVideo {
        return DeviceVideo(
            name, date, time, downloaded, path, 0, true
        )
    }

    /**
     * 更新进度
     */
    fun onProgressChange(progress: Int): DeviceVideo {
        return DeviceVideo(
            name, date, time, downloaded, path, progress, true
        )
    }
}