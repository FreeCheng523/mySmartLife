package com.zkjd.lingdong.ui.scanning

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.mine.baselibrary.bluetooth.ScanState
import com.zkjd.lingdong.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.zkjd.lingdong.bluetooth.BleConstants
import com.zkjd.lingdong.model.ConnectionState
import com.zkjd.lingdong.model.Device
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ScannningViewModel  @Inject constructor(
    private val deviceRepository: DeviceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object{
        const val TAG ="ScannningViewModel"
    }

    // 扫描状态
    private val _scanState = MutableStateFlow<ScanState>(ScanState.NotScanning)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    // 新设备发现事件流
    private val _newDeviceFound = MutableSharedFlow<List<Device>>()
    val newDeviceFound: SharedFlow<List<Device>> = _newDeviceFound.asSharedFlow()

    init {
        Timber.tag(TAG).i("ScannningViewModel initialized")
        // 监听扫描状态
        viewModelScope.launch {
            deviceRepository.getScanState().collect { state ->
                _scanState.value = state
                Timber.tag(TAG).i("Scan state changed: ${state.javaClass.simpleName}")
                when (state) {
                    is ScanState.ScanFailed -> {
                        Timber.tag(TAG).e("Scan failed: ${state}")
                    }
                    is ScanState.ScanResult -> {
                        Timber.tag(TAG).i("ScanResult received, device count: ${state.devices.size}")
                        processScannedDevices(state.devices)
                    }
                    is ScanState.Scanning -> {
                        Timber.tag(TAG).i("Scanning in progress...")
                    }
                    is ScanState.NotScanning -> {
                        Timber.tag(TAG).i("Not scanning")
                    }
                    else -> {}
                }
            }

        }
    }

    private fun processScannedDevices(devices: List<BluetoothDevice>) {
        viewModelScope.launch {
            Timber.tag(TAG).i("Processing scanned devices, total count: ${devices.size}")
            val needToAddToDbDevices = mutableListOf<BluetoothDevice>()

            val addToDbDevices = mutableListOf<Device>()

            for (device in devices) {
                // 检查设备是否存在
                val deviceNow = deviceRepository.getDeviceByMacAddress(device.address)
                if (deviceNow == null) {
                    // 收集新设备
                    needToAddToDbDevices.add(device)
                    Timber.tag(TAG).i("New device found: ${device.name} (${device.address})")
                }
            }
            
            Timber.tag(TAG).i("Device classification - New: ${needToAddToDbDevices.size}")
            
            // 如果有新设备，一次将扫描到的设备添加到本地数据库
            if (needToAddToDbDevices.isNotEmpty()) {
               val addedDevices = addDevicesToDb(needToAddToDbDevices)
                addToDbDevices.addAll(addedDevices)
            }

            Timber.tag(TAG).i("Emitting devices to UI, total (new: ${addToDbDevices.size},")
            _newDeviceFound.emit(addToDbDevices)
        }
    }
    /**
     * 把扫描到的多个设备添加到数据库中
     */
    private suspend fun addDevicesToDb(devices: List<BluetoothDevice>): MutableList<Device> {
        Timber.tag(TAG).i("Adding ${devices.size} devices to database")
        // 发出新设备发现事件
        val tempDevices = mutableListOf<Device>()
        // 批量添加设备到数据库
        for (device in devices) {
            device.let {
                try {
                    //添加到数据库中 妥妥贴
                    val deviceName =
                        "妥妥贴" + it.name.substring(BleConstants.DEVICE_NAME_HEAD.length)
                    val d1 = Device(
                        macAddress = it.address,
                        name = deviceName,
                        bleName = it.name,
                        musicName = "1",
                        createdAt = System.currentTimeMillis(),
                        lastConnectionState = ConnectionState.DISCONNECTED
                    )
                    Log.d("bleName", "设备bleName: ${it.name}");
                    deviceRepository.addDevice(d1)
                    tempDevices.add(d1)
                    Timber.tag(TAG).i("Device added to DB: $deviceName (${it.address})")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to add device to DB: ${it.name} (${it.address})")
                }
            }
        }
        Timber.tag(TAG).i("Successfully added ${tempDevices.size}/${devices.size} devices to database")
        return tempDevices
    }


    /**
     * 开始扫描
     */
    fun startScan() {
        Timber.tag(TAG).i("Starting scan...")
        deviceRepository.startScan()
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        Timber.tag(TAG).i("Stopping scan...")
        deviceRepository.stopScan()
    }

    override fun onCleared() {
        Timber.tag(TAG).i("ViewModel cleared, stopping scan")
        super.onCleared()
        // 确保停止扫描
        deviceRepository.stopScan()
    }
}