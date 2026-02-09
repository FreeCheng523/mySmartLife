package com.smarlife.tuotiecarimpllibrary

import com.example.tuotuotie_car_interface_library.IDisplayMKStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 天诺威车机实现的显示物理按键状态接口
 */
@Singleton
class TinnoveDisplayMKStatusImpl @Inject constructor() : IDisplayMKStatus {
    
    /**
     * 判断天诺威车机是否有物理按键
     * @return Boolean 是否有物理按键
     */
    override fun displayMKStatus(): Int {
        return 0
    }
}
