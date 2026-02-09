package com.smartlife.tuotuotiemeijiacarimplibrary

import com.example.tuotuotie_car_interface_library.ICarFunctionExecutor
import com.example.tuotuotie_car_interface_library.ICarMediaExecutor
import com.example.tuotuotie_car_interface_library.IDisplayMKStatus
import com.example.tuotuotie_car_interface_library.IFunctionConfigCheck
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeiJiaCarModule {

    @Binds
    @Singleton
    abstract fun bindFunctionConfig(impl: MeijiaFunctionConfigCheckImpl): IFunctionConfigCheck

    @Binds
    @Singleton
    abstract fun bindDisplayMKStatus(impl: MeijiaDisplayMKStatusImpl): IDisplayMKStatus

    @Binds
    @Singleton
    abstract fun bindCarFunctionExecutor(impl: MeiJiaCarFunctionExecutor): ICarFunctionExecutor

    @Binds
    @Singleton
    abstract fun bindCarMediaExecutor(impl: MeiJiaCarMediaExecutor): ICarMediaExecutor
}