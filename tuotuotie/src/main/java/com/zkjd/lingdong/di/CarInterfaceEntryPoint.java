package com.zkjd.lingdong.di;

import com.example.tuotuotie_car_interface_library.IDisplayMKStatus;
import com.example.tuotuotie_car_interface_library.IFunctionConfigCheck;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt EntryPoint 用于手动获取车载接口相关实例
 */
@EntryPoint
@InstallIn(SingletonComponent.class)
public interface CarInterfaceEntryPoint {
    /**
     * 获取功能配置检查实例
     */
    IFunctionConfigCheck getFunctionConfigCheck();
    
    /**
     * 获取MK显示状态实例
     */
    IDisplayMKStatus getMKDisplayStatus();
}
