package com.adayo.service.utils

import android.content.Context
import com.example.tuotuotie_car_interface_library.IDisplayMKStatus
import com.zkjd.lingdong.di.CarInterfaceEntryPoint
import dagger.hilt.android.EntryPointAccessors.fromApplication

/**
 * 提给给外部的非Hilt注解的类使用
 */
object MKDisplayStatus {
    fun displayMKStatus(context: Context): Int {
        return getMKDisplayStatus(context)?.displayMKStatus() ?: -1
    }

    /**
     * 通过 Hilt EntryPoint 获取 IDisplayMKStatus 实例
     * 不使用注解的方式从 Hilt 中获取依赖
     */
    private fun getMKDisplayStatus(context: Context): IDisplayMKStatus? {
        val entryPoint: CarInterfaceEntryPoint = fromApplication(
            context.getApplicationContext(),
            CarInterfaceEntryPoint::class.java
        )
        return entryPoint.getMKDisplayStatus()
    }
}