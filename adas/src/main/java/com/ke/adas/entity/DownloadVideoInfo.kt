package com.ke.adas.entity

data class DownloadVideoInfo(
    /**
     * 视频名称
     */
    val videoName: String,
    /**
     * 下载进度
     */
    val progress: Long = 0,
    /**
     * 下载完成时视频路径
     */
    val filePath: String? = null,
    /**
     * 下载完成式视频md5值
     */
    val fileMd5: String? = null,

    /**
     * 是否正在下载
     */
    val downloading: Boolean = true
)