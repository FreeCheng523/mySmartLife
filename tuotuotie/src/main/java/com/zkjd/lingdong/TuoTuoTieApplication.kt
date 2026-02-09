package com.zkjd.lingdong

import android.app.Application
import com.adayo.service.utils.FunctionConfigCheck

object TuoTuoTieApplication {
    fun init(context: Application){
        FunctionConfigCheck.getIFunctionConfigCheck(context)
    }
}