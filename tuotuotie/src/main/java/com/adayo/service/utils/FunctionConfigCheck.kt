package com.adayo.service.utils

import android.content.Context
import com.example.tuotuotie_car_interface_library.IFunctionConfigCheck
import com.zkjd.lingdong.di.CarInterfaceEntryPoint
import dagger.hilt.android.EntryPointAccessors.fromApplication

/**
 * 提给给外部的非Hilt注解的类使用
 */
object FunctionConfigCheck {
    /**
     * 通过 Hilt EntryPoint 获取 IFunctionConfigCheck 实例
     * 不使用注解的方式从 Hilt 中获取依赖
     */
    fun getIFunctionConfigCheck(context:Context): IFunctionConfigCheck {
        val entryPoint: CarInterfaceEntryPoint = fromApplication(
            context.getApplicationContext(),
            CarInterfaceEntryPoint::class.java
        )
        return entryPoint.getFunctionConfigCheck()
    }
}