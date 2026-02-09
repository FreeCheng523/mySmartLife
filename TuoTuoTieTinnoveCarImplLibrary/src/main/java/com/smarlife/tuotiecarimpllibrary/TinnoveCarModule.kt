package com.smarlife.tuotiecarimpllibrary

import android.content.Context
import com.example.tuotuotie_car_interface_library.IFunctionConfigCheck
import com.example.tuotuotie_car_interface_library.IDisplayMKStatus
import com.example.tuotuotie_car_interface_library.ICarFunctionExecutor
import com.example.tuotuotie_car_interface_library.ICarMediaExecutor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 梧桐车机Hilt模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TinnoveCarModule {
    
    @Binds
    @Singleton
    abstract fun bindFunctionConfig(impl: TinnoveFunctionConfigCheckImpl): IFunctionConfigCheck
    
    @Binds
    @Singleton
    abstract fun bindDisplayMKStatus(impl: TinnoveDisplayMKStatusImpl): IDisplayMKStatus
    
    @Binds
    @Singleton
    abstract fun bindCarFunctionExecutor(impl: TinnoveCarFunctionExecutorImp): ICarFunctionExecutor

    @Binds
    @Singleton
    abstract fun bindCarMediaExecutor(impl: TinnoveCarMediaExecutor): ICarMediaExecutor

    companion object {
        @Provides
        @Singleton
        fun provideTinnoveCarConfig(@ApplicationContext context: Context): TinnoveCarConfig {
            return TinnoveCarConfig(context)
        }
    }
}
