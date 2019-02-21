package com.ke.adas

import android.content.Context
import com.ke.adas.entity.DownloadedVideo
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.text.SimpleDateFormat

internal class DownloadedVideoRepository(applicationContext: Context) {
    private val sharedPreferences =
        applicationContext.getSharedPreferences("adas_downloaded_video", Context.MODE_PRIVATE)


    private val allVideoListSubject = BehaviorSubject.create<String>()
    private val collisionVideoListSubject = BehaviorSubject.create<String>()
    private val alarmVideoListSubject = BehaviorSubject.create<String>()


    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val timeFormat = SimpleDateFormat("HH:mm:ss")

    private val pathFormat = SimpleDateFormat("yyyyMMddHHmmss")


    val allVideoListObservable: Observable<List<DownloadedVideo>>
    val collisionVideoListObservable: Observable<List<DownloadedVideo>>
    val alarmVideoListObservable: Observable<List<DownloadedVideo>>


    private var allVideoSet: MutableSet<String>
        get() {
            return (sharedPreferences.getStringSet(ALL_VIDEO_LIST, emptySet()) ?: emptySet()).toMutableSet()
        }
        set(value) {
            sharedPreferences.edit().putStringSet(ALL_VIDEO_LIST, value).apply()
        }

    private var collisionVideoSet: MutableSet<String>
        get() {
            return (sharedPreferences.getStringSet(COLLISION_VIDEO_LIST, emptySet()) ?: emptySet()).toMutableSet()
        }
        set(value) {
            sharedPreferences.edit().putStringSet(COLLISION_VIDEO_LIST, value).apply()
        }

    private var alarmVideoSet: MutableSet<String>
        get() {
            return (sharedPreferences.getStringSet(ALARM_VIDEO_LIST, emptySet()) ?: emptySet()).toMutableSet()
        }
        set(value) {
            sharedPreferences.edit().putStringSet(ALARM_VIDEO_LIST, value).apply()
        }

    init {

        allVideoListObservable = allVideoListSubject
            .map { path ->
                val videoSet = allVideoSet

                if (path.isEmpty()) {
                    //用于触发获取数据 移除已经不再本地存在的视频
                    this.allVideoSet = videoSet.filter { File(it).exists() }.toMutableSet()
                } else {
                    videoSet.add(path)
                    this.allVideoSet = videoSet
                }


                videoSet
            }
            .map { set ->

                set.map { pathToDownloadedVideo(it) }
                    .toList().sortedByDescending { it.name }

            }


        collisionVideoListObservable = collisionVideoListSubject
            .map {path ->
                val videoSet = collisionVideoSet

                if (path.isEmpty()) {
                    //用于触发获取数据 移除已经不再本地存在的视频
                    this.collisionVideoSet = videoSet.filter { File(it).exists() }.toMutableSet()
                } else {
                    videoSet.add(path)
                    this.collisionVideoSet = videoSet
                }


                videoSet
            }
            .map { set ->

                set.map { pathToDownloadedVideo(it) }
                    .toList().sortedByDescending { it.name }

            }

        alarmVideoListObservable = alarmVideoListSubject
            .map {path ->
                val videoSet = alarmVideoSet

                if (path.isEmpty()) {
                    //用于触发获取数据 移除已经不再本地存在的视频
                    this.alarmVideoSet = videoSet.filter { File(it).exists() }.toMutableSet()
                } else {
                    videoSet.add(path)
                    this.alarmVideoSet = videoSet
                }


                videoSet
            }
            .map { set ->

                set.map { pathToDownloadedVideo(it) }
                    .toList().sortedByDescending { it.name }

            }

    }


    private fun pathToDownloadedVideo(path: String): DownloadedVideo {
        val file = File(path)
        val name = file.name.replace(".mp4", "")
        val date = pathFormat.parse(path)

        return DownloadedVideo(path = path, name = name, date = dateFormat.format(date), time = timeFormat.format(date))
    }


    fun onVideoDownloaded(path: String) {
        allVideoListSubject.onNext(path)
    }

    fun onCollisionVideoDownloaded(path: String) {
        collisionVideoListSubject.onNext(path)
    }

    fun onAlarmVideoDownloaded(path: String) {
        alarmVideoListSubject.onNext(path)
    }


    companion object {
        private const val ALL_VIDEO_LIST = "ALL_VIDEO_LIST"
        private const val COLLISION_VIDEO_LIST = "COLLISION_VIDEO_LIST"
        private const val ALARM_VIDEO_LIST = "ALARM_VIDEO_LIST"
    }

}