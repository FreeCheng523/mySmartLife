package com.smartlife.fragrance.ui.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.data.model.PowerState
import com.smartlife.fragrance.repository.FragranceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * 设备状态监听ViewModel
 * 用于监听设备状态的变化
 * 
 * 过滤规则：
 * - CONNECTED -> 非CONNECTED：收到数据
 * - 非CONNECTED -> CONNECTED：收到数据
 * - DISCONNECTED <-> CONNECTING：不收到数据
 */
@HiltViewModel
class DeviceStatusViewModel @Inject constructor(
    private val fragranceRepository: FragranceRepository
) : ViewModel() {

    companion object{
        const val TAG = "DeviceStatusViewModel"
    }

    // 所有设备列表
    val devices: StateFlow<List<FragranceDevice>> = fragranceRepository.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getDeviceList():List<FragranceDevice> =fragranceRepository.getAllDevicesList()

    /**
     * 获取指定设备信息
     * 使用 sample 操作符控制发射频率为 500ms，避免频繁更新
     * 
     * @param macAddress 设备MAC地址
     * @return LiveData<FragranceDevice?> 设备信息LiveData，设备不存在时为null
     */
    fun deviceInfo(macAddress: String): LiveData<FragranceDevice?> {
        return fragranceRepository.getDevice(macAddress)
            //.sample(500.milliseconds) // 每 500ms 最多发射一次数据
            .asLiveData(viewModelScope.coroutineContext)
    }
    /**
     * 删除设备
     */
     fun deleteDevice(macAddress: String,result:(success: Boolean)-> Unit) {
        Timber.tag(TAG).i("deleteDevice begin")
        viewModelScope.launch(Dispatchers.IO) {
            Timber.tag(TAG).i("deleteDevice ${Thread.currentThread()}")
            val count = fragranceRepository.deleteDevice(macAddress)
            Timber.tag(TAG).i("deleteDevice $count")
            result(count > 0)
        }
    }

    /**
     * 重命名设备
     */
    fun renameDevice(macAddress: String, name: String) {
        viewModelScope.launch {
            Timber.tag(TAG).i("重命名设备: $macAddress, 新名称: $name")
            withContext(Dispatchers.IO) {
                fragranceRepository.renameDevice(macAddress, name)
            }
            Timber.tag(TAG).i("设备重命名完成: $macAddress, 新名称: $name")
        }
    }

    fun setSyncLightBrightness(macAddress: String,sync: Boolean){
        viewModelScope.launch {
            fragranceRepository.setSyncLightBrightness(macAddress,sync)
        }
    }

    fun setPowerState(macAddress: String, powerState: PowerState){
        viewModelScope.launch {
            fragranceRepository.setPowerState(macAddress,powerState)
        }
    }

    fun setColor(macAddress: String, color: Int){
        viewModelScope.launch {
            fragranceRepository.setLightColor(macAddress,color)
        }
    }

    fun setColor(macAddress: String, color: String){
        viewModelScope.launch {
            fragranceRepository.setLightColor(macAddress,color)
        }
    }

    /**
     * 更新设备自动连接状态
     */
    fun updateDeviceAutoConnect(macAddress: String, needAutoConnect: Boolean) {
        viewModelScope.launch {
            fragranceRepository.updateDeviceAutoConnect(macAddress, needAutoConnect)
        }
    }

}
