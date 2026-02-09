package com.example.tuotuotie_car_interface_library

/**
 * 车载媒体功能执行器接口
 * 定义媒体控制相关的功能，包括音量调节、音乐切换、播放控制等
 * 
 * 该接口为不同车型的媒体功能实现提供统一的抽象层，
 * 支持镁佳车机、梧桐车机等不同平台的媒体控制功能
 */
interface ICarMediaExecutor {

    fun ensureCarAudioManager()

    fun ensureCarAudioManagerOnChildThread()

    /**
     * 增加音量
     * 支持普通车机和8295车机的音量控制
     */
    fun volumeUp()

    /**
     * 降低音量
     * 支持普通车机和8295车机的音量控制
     */
    fun volumeDown()

    /**
     * 播放下一首音乐
     * 通过发送媒体按键事件实现
     */
    fun nextTrack()

    /**
     * 播放上一首音乐
     * 通过发送媒体按键事件实现
     */
    fun previousTrack()

    /**
     * 播放/暂停音乐
     * 通过发送媒体按键事件实现
     */
    fun playPause()

    /**
     * 静音/取消静音
     * 支持记忆静音前的音量状态
     */
    fun toggleMute()

    /**
     * 释放资源
     * 清理Car服务连接和相关资源
     */
    fun release()
}
