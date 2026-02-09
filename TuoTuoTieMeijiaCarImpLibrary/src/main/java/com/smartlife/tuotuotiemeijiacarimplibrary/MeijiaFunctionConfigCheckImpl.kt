package com.smartlife.tuotuotiemeijiacarimplibrary
import com.example.tuotuotie_car_interface_library.IFunctionConfigCheck
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 镁佳车机功能配置实现类
 */
@Singleton
class MeijiaFunctionConfigCheckImpl @Inject constructor() : IFunctionConfigCheck {
    override fun hasFunction(configString: String): Boolean {
        // 镁佳车机功能配置逻辑
        return DriveModelManager.getModels(DriveModelManager.getSettingString(configString))>0
    }

    //镁佳不要使用这个犯法，直接使用hasFunction
    override fun hasTailWings(): Boolean {
       return true
    }

    override fun hasAmbientlight(): Boolean {
        return DriveModelManager.getModels(DriveModelManager.getSettingString("AMBIENT_LIGHT_AREA"))>0
    }
}
