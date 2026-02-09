package com.smartlife.fragrance.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.mine.baselibrary.bluetooth.BaseBleManagerIml
import com.mine.baselibrary.bluetooth.DeviceEvent
import com.mine.baselibrary.bluetooth.ScanState
import com.mine.baselibrary.util.bytesUtil.BleAuthUtils
import com.smartlife.fragrance.data.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

abstract class FragranceAbsBleManager(override val appContext: Context,
                                  override val mainServiceUUID: UUID = FragranceConfig.MAIN_SERVICE_UUID,
                                  override val characteristicNotifyUuid: UUID = FragranceConfig.CHARACTERISTIC_NOTIFY_UUID,
                                  override val characteristicWriteUuid: UUID = FragranceConfig.CHARACTERISTIC_WRITE_UUID,
                                  override val batteryServiceUuid: UUID = FragranceConfig.BATTERY_SERVICE_UUID,
                                  override val batteryLevelUuid: UUID = FragranceConfig.BATTERY_LEVEL_UUID,
) : BaseBleManagerIml(appContext) {
    companion object{
       private const val TAG = "FragranceAbsBleManager"
    }

    /** 设备信息流 */
    val deviceInfoFlow = MutableSharedFlow<FragranceDevice>(replay = 0)

    override fun isRssiMonitoringEnabled(): Boolean = false

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onAcceptScanResult(result: ScanResult) {
        val device = result.device
        Log.i(TAG,"扫描到设备：${device.address}, 名称=${device.name}, RSSI=${result.rssi}")
        // 只处理名称匹配的设备
        if (device.name != null && device.name.startsWith(FragranceConfig.NAME_PREFIX)) {
            Timber.tag(TAG)
                .w("扫描到设备需要的设备：${device.address}, 名称=${device.name}, RSSI=${result.rssi}")
            //读取广播内容，判断是否配对广播
            try {
                scanResults.add(device)
                coroutineScope.launch {
                    scanStateFlow.emit(ScanState.ScanResult(scanResults.toList()))
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("广播没有FF")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun validateRequiredServices(gatt: BluetoothGatt): Boolean {
        // 1. 检查主服务
        val mainService = gatt.getService(mainServiceUUID) ?: run {
            Timber.tag(TAG).e("设备缺少主服务: ${gatt.device.address}")
            return false
        }

        // 2. 检查写入特征
        val writeCharacteristic = mainService.getCharacteristic(characteristicWriteUuid) ?: run {
            Timber.tag(TAG).e("设备缺少写入特征: ${gatt.device.address}")
            return false
        }

        // 3. 检查通知特征
        val notifyCharacteristic = mainService.getCharacteristic(characteristicNotifyUuid) ?: run {
            Timber.tag(TAG).e("设备缺少通知特征: ${gatt.device.address}")
            return false
        }

        // 4. 正确配置通知特征
        if (!enableCharacteristicNotification(gatt, notifyCharacteristic)) {
            Timber.tag(TAG).e("无法启用通知特征: ${gatt.device.address}")
            return false
        }

        // 5. 检查电池服务（可选）
        val batteryService = gatt.getService(batteryServiceUuid)
        if (batteryService != null) {
            val batteryCharacteristic = batteryService.getCharacteristic(batteryLevelUuid)
            if (batteryCharacteristic == null) {

                Timber.tag(TAG).w("设备有电池服务但缺少电池电量特征: ${gatt.device.address}")
                // 电池特征是可选的，不影响连接结果
            }else{

                val descriptor = batteryCharacteristic.getDescriptor(FragranceConfig.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                if (descriptor != null) {
                    Timber.tag(TAG).w("Client Characteristic Configuration descriptor not found!")
//                    if (!enableCharacteristicNotification(gatt, batteryCharacteristic)) {
//                    Timber.tag(TAG).e("无法启用电池服务通知特征: ${gatt.device.address}")
//                    return false
//                }
                }

            }
        } else {
            Timber.tag(TAG).w("设备缺少电池服务: ${gatt.device.address}")
            // 电池服务是可选的，不影响连接结果
        }

        Timber.tag(TAG).w("设备验证成功: ${gatt.device.address}")
        return true
    }

    override fun authIsEnable(): Boolean {
        return false
    }

    override suspend fun authenticateDevice(device: BluetoothDevice): Boolean {
        return true
    }

    override fun handleAuthResponse(gatt: BluetoothGatt, data: ByteArray) {

    }

    /**
     * 特征值变化回调（用于接收通知）
     */
    override fun onAcceptCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt
    ) {
        Timber.tag(TAG).w("收到香氛 特征值变化回调: ${characteristic.value}")
        synchronized(characteristicChangedLock) {
            when (characteristic.uuid) {
                characteristicNotifyUuid -> {
                    val data = characteristic.value
                    if (data == null) {
                        Timber.tag(TAG).e("收到空数据包")
                        return
                    }

                    val threadID = Thread.currentThread().id

                    // 计算数据包的唯一标识
                    val notificationKey = "${gatt.device.address}_${bytesToHex(data)}"
                    val currentTime = System.currentTimeMillis()

                    // 记录数据包内容
                    Timber.tag(TAG)
                        .w("收到数据包: ${bytesToHex(data)}, 长度: ${data.size}, 线程：${threadID}")

                    // 更新最近处理的通知记录
                    lastProcessedNotification[notificationKey] = currentTime

                    // 清理过期的通知记录
                    clearExpiredNotifications(currentTime)

                    // 验证数据包格式
                    if (data.size >= 2) {
                        when (data[0]) {
                            FragranceResponseParse.HEADER_NOTIFICATION -> {
                                // 处理按键通知
                                if (data.size >= 4) { // 只检查长度

                                    Timber.tag(TAG)
                                        .i("收到香氛: 0x${bytesToHex(data)}")
                                    var name = gatt.device.name
                                    if(name==null){
                                        name = FragranceConfig.NAME_PREFIX+"temp"
                                    }
                                    val fragranceDevice = FragranceResponseParse.parse(gatt.device.address,
                                        name, data)
                                    coroutineScope.launch {
                                        deviceInfoFlow.emit(fragranceDevice)
                                    }
                                } else {
                                    Timber.tag(TAG).e("数据长度出错 0x${data}")
                                }
                            }

                            else -> {
                                Timber.tag(TAG)
                                    .w("收到未知头部的数据包: 0x${data[0].toUByte().toString(16)}")
                            }
                        }
                    } else {
                        Timber.tag(TAG).w("收到的数据包格式不正确")
                    }
                }

                batteryLevelUuid -> {
                    val data = characteristic.value
                    if (data == null) {
                        Timber.tag(TAG).e("电量 收到空数据包")
                        return
                    } else {
                        Timber.tag(TAG).e("电量 $data")
                    }
                }

                else -> {
                    Timber.tag(TAG).e("unkown notify ${characteristic.uuid}")
                }
            }
        }
    }

    /**
     * 特征读取完成回调
     */
    override fun onAcceptCharacteristicRead(
        status: Int,
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt
    ) {
        Timber.tag(TAG).w("收到香氛 特征读取完成回调: ${characteristic.value}")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (characteristic.uuid) {
                batteryLevelUuid -> {
                    val batteryLevel = characteristic.value[0].toInt() and 0xFF
                    Timber.tag(com.mine.baselibrary.bluetooth.TAG).w("设备电量: ${batteryLevel}%")
                    coroutineScope.launch {
                        deviceEventsFlow.emit(
                            DeviceEvent.BatteryLevelChanged(gatt.device, batteryLevel)
                        )
                    }
                }

                characteristicNotifyUuid -> {

                    val uuid = characteristic.uuid.toString()
                    writeCallbacks[uuid]?.let { deferred ->
                        if (!deferred.isCompleted) {
                            deferred.complete(status == BluetoothGatt.GATT_SUCCESS)
                        }
                        writeCallbacks.remove(uuid)
                    }

                    val value = BleAuthUtils.byteArrayToHexString(characteristic.value)

                    Timber.tag(TAG).w("特征读取完成回调: $value")
                }
            }
        }
    }


    override suspend fun readDeviceInfo(
        gatt: BluetoothGatt,
        readCharacteristic: BluetoothGattCharacteristic
    ) {
       getDeviceStatus(gatt.device.address)
    }

    // ========== 香氛设备控制命令抽象方法 ==========

    /**
     * 设置设备开关状态
     * @param macAddress 设备MAC地址
     * @param powerState 电源状态
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setPowerState(macAddress: String, powerState: PowerState): Boolean

    /**
     * 设置设备模式
     * @param macAddress 设备MAC地址
     * @param mode 模式（待机/香型A/香型B）
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setMode(macAddress: String, mode: Mode): Boolean

    /**
     * 设置设备档位
     * @param macAddress 设备MAC地址
     * @param gear 档位（低/中/高）
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setGear(macAddress: String, gear: Gear): Boolean

    /**
     * 恢复出厂设置
     * @param macAddress 设备MAC地址
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun restoreFactorySettings(macAddress: String): Boolean

    /**
     * 设置随车启停开关
     * @param macAddress 设备MAC地址
     * @param enabled 是否启用
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setCarStartStopEnabled(macAddress: String, enabled: Boolean): Boolean

    /**
     * 设置随车启停周期
     * @param macAddress 设备MAC地址
     * @param cycle 周期类型
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setCarStartStopCycle(macAddress: String, cycle: CarStartStopCycle): Boolean

    /**
     * 设置灯光模式
     * @param macAddress 设备MAC地址
     * @param lightMode 灯光模式
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setLightMode(macAddress: String, lightMode: LightMode): Boolean

    /**
     * 设置灯光颜色
     * @param macAddress 设备MAC地址
     * @param r 红色分量 (0-255)
     * @param g 绿色分量 (0-255)
     * @param b 蓝色分量 (0-255)
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setLightColor(macAddress: String, r: Int, g: Int, b: Int): Boolean

    abstract suspend fun setLightColor(macAddress: String, color: Int): Boolean

    abstract suspend fun setLightColor(macAddress: String, color: String): Boolean

    /**
     * 设置灯光亮度
     * @param macAddress 设备MAC地址
     * @param brightness 亮度值 (0-100)
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setLightBrightness(macAddress: String, brightness: Int): Boolean

    /**
     * 设置定时时长
     * @param macAddress 设备MAC地址
     * @param minutes 定时时长（分钟），支持：5, 10, 30, 40, 50, 60, 90, 120
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun setTimingDuration(macAddress: String, minutes: Int): Boolean

    /**
     * 获取程序版本（查询命令）
     * @param macAddress 设备MAC地址
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun getProgramVersion(macAddress: String): Boolean

    /**
     * 获取设备状态（查询命令）
     * 返回信息包括：1.香氛剩余寿命百分比值 2.香氛棒检测是否缺失 3.香氛机器设备电池剩余值
     * @param macAddress 设备MAC地址
     * @return 发送成功返回true，否则返回false
     */
    abstract suspend fun getDeviceStatus(macAddress: String): Boolean
}