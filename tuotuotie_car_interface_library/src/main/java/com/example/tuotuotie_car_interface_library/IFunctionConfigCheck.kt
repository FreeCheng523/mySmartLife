package com.example.tuotuotie_car_interface_library

interface IFunctionConfigCheck {
    //是否有该配置
    fun hasFunction(configString: String): Boolean

    fun hasTailWings(): Boolean

    fun hasAmbientlight():Boolean
}