package com.smartlife.fragrance.ui.scanning

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mine.baselibrary.bluetooth.ScanState
import com.smartlife.fragrance.bluetooth.FragranceConfig
import com.smartlife.fragrance.data.model.ConnectionState
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.repository.FragranceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FragranceScanningViewModel @Inject constructor(
    private val fragranceRepository: FragranceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val TAG = "FragranceScanningViewModel"
    }

    // 扫描状态
    private val _scanState = MutableStateFlow<ScanState>(ScanState.NotScanning)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    // 新设备发现事件流
    private val _newDeviceFound = MutableSharedFlow<List<FragranceDevice>>()
    val newDeviceFound: SharedFlow<List<FragranceDevice>> = _newDeviceFound.asSharedFlow()

    init {
        // 监听扫描状态
        viewModelScope.launch {
            fragranceRepository.getScanState().collect { state ->
                _scanState.value = state
                when (state) {
                    is ScanState.ScanFailed -> {
                        Timber.tag(TAG).e("scan failed ${state}")
                    }
                    is ScanState.ScanResult -> {
                        Log.i(TAG, "ScanResult is ${state.devices}")
                        processScannedDevices(state.devices)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun processScannedDevices(devices: List<BluetoothDevice>) {
        viewModelScope.launch {
            Timber.tag(TAG).i("processScannedDevices")
            val needToAddToDbDevices = mutableListOf<BluetoothDevice>()

            val addToDbDevices = mutableListOf<FragranceDevice>()

            for (device in devices) {
                // 检查设备是否存在
                val deviceNow = fragranceRepository.getDeviceByMacAddress(device.address)
                if (deviceNow == null) {
                    // 收集新设备
                    Timber.tag(TAG).i("processScannedDevices emit  $device 不存在")
                    needToAddToDbDevices.add(device)
                }else{
                    Timber.tag(TAG).i("processScannedDevices emit  $device 已存在")
                }
            }

            // 如果有新设备，一次将扫描到的设备添加到本地数据库
            if (needToAddToDbDevices.isNotEmpty()) {
                val addedDevices = addDevicesToDb(needToAddToDbDevices)
                addToDbDevices.addAll(addedDevices)
            }

            if(addToDbDevices.isNotEmpty()) {
                Timber.tag(TAG).i("processScannedDevices emit $addToDbDevices")
                _newDeviceFound.emit(addToDbDevices)
            }else{
                Timber.tag(TAG).i("processScannedDevices addToDbDevices is empty")
            }
        }
    }

    /**
     * 把扫描到的多个设备添加到数据库中
     */
    private suspend fun addDevicesToDb(devices: List<BluetoothDevice>): MutableList<FragranceDevice> {
        // 发出新设备发现事件
        val tempDevices = mutableListOf<FragranceDevice>()
        // 批量添加设备到数据库
        for (device in devices) {
            device.let {
                //添加到数据库中 香氛

                val d1 = FragranceDevice(
                    macAddress = it.address,
                    deviceName = it.name,
                    displayName = it.name.replace(FragranceConfig.NAME_PREFIX,"香氛机"),
                    connectionState = ConnectionState.DISCONNECTED
                )
                fragranceRepository.addDevice(d1)
                tempDevices.add(d1)
            }
        }
        return tempDevices
    }


    /**
     * 开始扫描
     */
    fun startScan() {
        fragranceRepository.startScan()
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        fragranceRepository.stopScan()
    }

    override fun onCleared() {
        super.onCleared()
        // 确保停止扫描
        fragranceRepository.stopScan()
    }
}

