package com.smartlife.tuotuotiemeijiacarimplibrary

import com.deepal.smartlifemiddleware.MegaCarInfo
import com.example.tuotuotie_car_interface_library.IDisplayMKStatus
import javax.inject.Inject
import javax.inject.Singleton
/**
 * 梅佳车机实现的显示物理按键状态接口
 */
@Singleton
class MeijiaDisplayMKStatusImpl @Inject constructor() : IDisplayMKStatus {
    
    /**
     * 判断梅佳车机是否有物理按键
     * @return Boolean 是否有物理按键
     */
    override fun displayMKStatus(): Int {
       return MegaCarInfo.getDisplayMKStatus()
    }
}
