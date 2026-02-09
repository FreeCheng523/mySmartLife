package com.smarlife.tuotiecarimpllibrary

import com.example.tuotuotie_car_interface_library.IFunctionConfigCheck
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 梧桐车机功能配置实现类
 */
@Singleton
class TinnoveFunctionConfigCheckImpl @Inject constructor(
    private val tinnoveCarConfig: TinnoveCarConfig
) : IFunctionConfigCheck {
    
    override fun hasFunction(configStr:String): Boolean {
        tinnoveCarConfig.hasElectricTailSpoiler()
        return true // 默认返回true，具体实现需要根据实际需求调整
    }

    //先暂时使用hasTailWings，之后都换成hasFunction todo
    override fun hasTailWings(): Boolean {
       return tinnoveCarConfig.hasElectricTailSpoiler()
    }

    override fun hasAmbientlight(): Boolean {
        return tinnoveCarConfig.hasAmbientLight()
    }
}
