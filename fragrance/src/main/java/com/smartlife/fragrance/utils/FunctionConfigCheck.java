// FunctionConfigCheck.java
package com.smartlife.fragrance.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.tuotuotie_car_interface_library.IFunctionConfigCheck;
import dagger.hilt.android.EntryPointAccessors;

/**
 * 功能配置检查工具类
 * 提供给外部的非Hilt注解的类使用
 */
public final class FunctionConfigCheck {

    private FunctionConfigCheck() {
        // 工具类 - 防止实例化
        throw new IllegalStateException("Utility class");
    }

    /**
     * 通过 Hilt EntryPoint 获取 IFunctionConfigCheck 实例
     * 不使用注解的方式从 Hilt 中获取依赖
     */
    @NonNull
    public static IFunctionConfigCheck getIFunctionConfigCheck(@NonNull Context context) {
        CarInterfaceEntryPoint entryPoint = EntryPointAccessors.fromApplication(
                context.getApplicationContext(),
                CarInterfaceEntryPoint.class
        );
        return entryPoint.getFunctionConfigCheck();
    }
}