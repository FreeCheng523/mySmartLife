package com.zkjd.lingdong.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.mine.baselibrary.bluetooth.BaseBleManagerIml
import com.mine.baselibrary.bluetooth.DELAY_SEND
import com.mine.baselibrary.bluetooth.DeviceEvent
import com.mine.baselibrary.bluetooth.ScanState
import com.mine.baselibrary.bluetooth.TAG
import com.mine.baselibrary.util.bytesUtil.BleAuthUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.and
import kotlin.text.clear
import kotlin.text.toInt

/**
 * 等待鉴权响应，设置5秒超时
 */
internal const val WAIT_TO=5000L
/**
 * TuoTuoTie设备特定事件
 */
sealed class TuoTuoTieDeviceEvent {
    data class ButtonPressed(val device: BluetoothDevice, val keyCode: Byte) : TuoTuoTieDeviceEvent()
    data class SetReadVules(val device: BluetoothDevice, val vules: String) : TuoTuoTieDeviceEvent() // 设备回传颜色
}

abstract class TuoTuoTieAbsBleManager(override val appContext: Context) : BaseBleManagerIml(appContext) {

    val TAG = "TuoTuoTieAbsBleManager"
    /**
     * 鉴权数据类
     */
    data class AuthData(
        val hostSalt: ByteArray,          // 车机随机数
        val deviceSalt: ByteArray? = null, // 妥妥贴随机数
        val iv: ByteArray? = null,        // 初始化向量
        val key: ByteArray? = null        // AES密钥
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AuthData) return false

            if (!hostSalt.contentEquals(other.hostSalt)) return false
            if (deviceSalt != null) {
                if (other.deviceSalt == null) return false
                if (!deviceSalt.contentEquals(other.deviceSalt)) return false
            } else if (other.deviceSalt != null) return false
            if (iv != null) {
                if (other.iv == null) return false
                if (!iv.contentEquals(other.iv)) return false
            } else if (other.iv != null) return false
            if (key != null) {
                if (other.key == null) return false
                if (!key.contentEquals(other.key)) return false
            } else if (other.key != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = hostSalt.contentHashCode()
            result = 31 * result + (deviceSalt?.contentHashCode() ?: 0)
            result = 31 * result + (iv?.contentHashCode() ?: 0)
            result = 31 * result + (key?.contentHashCode() ?: 0)
            return result
        }
    }
    /** 存储鉴权信息 */
    val authenticationData = ConcurrentHashMap<String, AuthData>()

    /** TuoTuoTie设备特定事件流 */
    internal val tuoTuoTieDeviceEventsFlow = MutableSharedFlow<TuoTuoTieDeviceEvent>(replay = 0)

    override val mainServiceUUID = BleConstants.SERVICE_UUID

    //
    override val characteristicNotifyUuid = BleConstants.CHARACTERISTIC_NOTIFY_UUID

    override val batteryLevelUuid = BleConstants.BATTERY_LEVEL_UUID

    override val batteryServiceUuid = BleConstants.BATTERY_SERVICE_UUID

    override val characteristicWriteUuid = BleConstants.CHARACTERISTIC_WRITE_UUID


    override fun isRssiMonitoringEnabled(): Boolean = true

    fun getTuoTuoTieDeviceEvents(): SharedFlow<TuoTuoTieDeviceEvent> {
        return tuoTuoTieDeviceEventsFlow
    }

    /**
     * 设置工作模式下的LED颜色
     */
    abstract suspend fun setWorkingLedColor(device: BluetoothDevice, r: Int, g: Int, b: Int): Boolean
    abstract suspend fun setWorkingLedColor2(device: String, r: Int, g: Int, b: Int): Boolean
    /**
     * 设置重连模式下的LED颜色
     */
    abstract suspend fun setReconnectingLedColor(device: BluetoothDevice, r: Int, g: Int, b: Int): Boolean

    /**
     * 设置防误触模式
     */
    abstract suspend fun setAntiMisoperation(device: BluetoothDevice, enabled: Boolean): Boolean

    /**
     * 下发防误触指令
     */
    abstract suspend fun setPreventAccid(device: String, pA: Int): Boolean

    override fun authIsEnable(): Boolean {
        return true
    }

    override fun onAcceptScanResult(result: ScanResult) {
        val device = result.device
        // 只处理名称匹配的设备
        if (device.name != null && device.name.startsWith(BleConstants.DEVICE_NAME_HEAD)) {
            Timber.tag(TAG)
                .w("扫描到设备：${device.address}, 名称=${device.name}, RSSI=${result.rssi}")
            //读取广播内容，判断是否配对广播
            try {
                result.rssi.let { rssi ->
                    device.setRssiValue(rssi)
                }
                val dataGB = result.scanRecord?.bytes
                val dataGBString = BleAuthUtils.byteArrayToHexString(dataGB!!)
                Timber.tag(TAG).e("广播=${dataGBString}")
                //1为配对广播，2为重连广播
                //                        val value = advertisingDataMap?.getValue(255)
                //                        val valueString = value?.let { BleAuthUtils.byteArrayToHexString(it) }
                var valueString = dataGBString.substring(10, 12)
                if (valueString != null) {
                    if (valueString.toInt() == 1) {
                        scanResults.add(device)
                        coroutineScope.launch {
                            scanStateFlow.emit(ScanState.ScanResult(scanResults.toList()))
                        }
                        Log.e(TAG, "收到配对广播")
                    } else if (valueString.toInt() == 2) {
                        Log.e(TAG, "收到重连广播")
                    } else {
                        Log.e(TAG, "广播类型错误")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("广播没有FF")
            }
        }
    }



    /**
     * 验证设备是否具有所有必需的服务和特征
     * @param gatt 蓝牙GATT连接
     * @return 验证成功返回true，否则返回false
     */
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

                val descriptor = batteryCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
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

    /**
     * 对设备进行鉴权
     * @param device 蓝牙设备
     * @return 鉴权成功返回true，否则返回false
     */
    override suspend fun authenticateDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).w("鉴权:开始对设备进行鉴权: ${device.address}")

            // 获取设备GATT连接
            val gatt = deviceGattMap[device.address] ?: run {
                Timber.tag(TAG).e("鉴权:失败, 找不到设备的GATT连接")
                return@withContext false
            }

            // 获取主服务
            val service = gatt.getService(mainServiceUUID) ?: run {
                Timber.tag(TAG).e("鉴权: 失败, 找不到主服务")
                return@withContext false
            }

            // 获取写入特征
            val writeCharacteristic = service.getCharacteristic(characteristicWriteUuid) ?: run {
                Timber.tag(TAG).e("鉴权:失败, 找不到写入特征")
                return@withContext false
            }

            // 创建异步结果
            val authResult = CompletableDeferred<Boolean>()
            authenticationResults[device.address] = authResult

            // 生成随机盐值
            val hostSalt = BleAuthUtils.generateRandomSalt(BleConstants.Auth.SALT_LENGTH) //byteArrayOf(0x11,0x22,0x33,0x44,0x55,0x66,0x77, 0x88.toByte())

            // 存储鉴权信息
            authenticationData[device.address] = AuthData(hostSalt = hostSalt)


            // 构建鉴权请求命令
            val authRequest = ByteArray(2 + BleConstants.Auth.SALT_LENGTH)
            authRequest[0] = BleConstants.HEADER_AUTH
            authRequest[1] = BleConstants.AuthInstruction.AUTH_REQUEST
            System.arraycopy(hostSalt, 0, authRequest, 2, hostSalt.size)


            Timber.tag(TAG).w("鉴权:发送鉴权请求: ${BleAuthUtils.byteArrayToHexString(authRequest)}")

            //延迟发送，首次连接蓝牙，需要延迟发送数据才能成功
            Thread.sleep(DELAY_SEND)

            // 发送鉴权请求
            val writeSuccess = writeCharacteristicWithTimeout(gatt, writeCharacteristic, authRequest)
            if (!writeSuccess) {
                Timber.tag(TAG).e("鉴权:失败, 发送鉴权请求失败")
                authenticationResults.remove(device.address)
                return@withContext false
            }

            // 等待鉴权响应，设置5秒超时
            var authenticated = false
            try {
                authenticated = withTimeout(WAIT_TO) {
                    authResult.await()
                }
            } catch (e: TimeoutCancellationException) {
                Timber.tag(TAG).e("鉴权:超时")
                authenticated = false
            }


            // 清理鉴权数据
            authenticationResults.remove(device.address)

            if (authenticated) {
                Timber.tag(TAG).w("设备鉴权成功: ${device.address}")
            } else {
                Timber.tag(TAG).e("设备鉴权失败: ${device.address}")
            }

            return@withContext authenticated

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "鉴权过程发生异常")
            return@withContext false
        }
    }

    override fun onAcceptCharacteristicRead(
        status: Int,
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (characteristic.uuid) {
                batteryLevelUuid -> {
                    val batteryLevel = characteristic.value[0].toInt() and 0xFF
                    Timber.tag(TAG).w("设备电量: ${batteryLevel}%")
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

                    val readVules = BleAuthUtils.byteArrayToHexString(characteristic.value)

                    Timber.tag(TAG).e("读取设备Read反馈=${readVules}")
                    coroutineScope.launch {
                        tuoTuoTieDeviceEventsFlow.emit(
                            TuoTuoTieDeviceEvent.SetReadVules(gatt.device, readVules)
                        )
                    }
                }
            }
        }
    }

    override fun onAcceptCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt
    ) {
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

                    // 检查是否是重复通知（100毫秒内收到完全相同的数据）
                    //val lastTime = lastProcessedNotification[notificationKey]
                    //if (lastTime != null && (currentTime - lastTime) < 100) {
                    //    Timber.tag(TAG).w("忽略可能的重复通知: ${bytesToHex(data)}, 距上次处理: ${currentTime - lastTime}ms")
                    //    return
                    //}

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
                            BleConstants.HEADER_NOTIFICATION -> {
                                // 处理按键通知
                                if (data.size >= 4) { // 只检查长度
                                    val keyCode = data[1]
                                    Timber.tag(TAG)
                                        .w("收到按键代码: 0x${keyCode.toUByte().toString(16)}")

                                    coroutineScope.launch {
                                        tuoTuoTieDeviceEventsFlow.emit(
                                            TuoTuoTieDeviceEvent.ButtonPressed(
                                                gatt.device,
                                                keyCode
                                            )
                                        )
                                    }
                                } else {
                                    Timber.tag(TAG).e("数据长度出错 0x${data}")
                                }
                            }

                            BleConstants.HEADER_AUTH -> {
                                // 处理鉴权响应
                                Timber.tag(TAG).i("鉴权:处理鉴权响应")
                                handleAuthResponse(gatt, data)
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
                        Timber.tag(TAG).e("收到空数据包")
                        return
                    } else {

                    }
                }

                else -> {
                    Timber.tag(TAG).e("unkown notify ${characteristic.uuid}")
                }
            }
        }
    }


    /**
     * 处理设备的鉴权响应
     * @param gatt 设备GATT连接
     * @param data 响应数据
     */
   override fun handleAuthResponse(gatt: BluetoothGatt, data: ByteArray) {
        val device = gatt.device



        if (data.size < 3) {
            Timber.tag(TAG).e("鉴权响应数据长度不足")
            return
        }

        if (data[1] == BleConstants.AuthInstruction.AUTH_RESPONSE) {
            // 处理AUTH_RESPONSE
            if (data.size < 2 + BleConstants.Auth.SALT_LENGTH + BleConstants.Auth.PUBLIC_ADDR_LENGTH + BleConstants.Auth.AUTH_DATA_LENGTH) {
                Timber.tag(TAG).e("鉴权响应数据长度不足: ${data.size} 字节")
                completeAuthentication(device.address, false)
                return
            }

            try {
                // 获取妥妥贴Salt
                val deviceSalt = data.copyOfRange(2, 2 + BleConstants.Auth.SALT_LENGTH)

                // 获取加密的PublicAddr
                val encryptedAddr = data.copyOfRange(
                    2 + BleConstants.Auth.SALT_LENGTH,
                    2 + BleConstants.Auth.SALT_LENGTH + BleConstants.Auth.PUBLIC_ADDR_LENGTH
                )

                // 获取认证数据
                val authData = data.copyOfRange(
                    2 + BleConstants.Auth.SALT_LENGTH + BleConstants.Auth.PUBLIC_ADDR_LENGTH,
                    2 + BleConstants.Auth.SALT_LENGTH + BleConstants.Auth.PUBLIC_ADDR_LENGTH + BleConstants.Auth.AUTH_DATA_LENGTH
                )

                // 获取之前存储的车机Salt
                val hostSalt = authenticationData[device.address]?.hostSalt ?: run {
                    Timber.tag(TAG).e("鉴权:找不到车机Salt")
                    completeAuthentication(device.address, false)
                    return
                }

                // 生成密钥材料
                val (iv, key) = BleAuthUtils.generateKeyMaterial(
                    hostSalt,
                    deviceSalt,
                    BleConstants.Auth.AUTH_LABEL
                )



                // 1. 验证PublicAddr
                val decryptedAddr = BleAuthUtils.aesDecrypt(encryptedAddr, key, iv)

                val deviceName = device.address ?: ""

                // 使用补0的设备名称进行比对
                val expectedAddr = BleAuthUtils.padDeviceName2(deviceName.replace(":",""), BleConstants.Auth.PUBLIC_ADDR_LENGTH)

                Timber.tag(TAG).e("鉴权:接收0402数据=${BleAuthUtils.byteArrayToHexString(data)}")
                Timber.tag(TAG).e("鉴权:获取加密的PublicAddr=${BleAuthUtils.byteArrayToHexString(encryptedAddr)}")
                Timber.tag(TAG).e("鉴权:车机Salt=${BleAuthUtils.byteArrayToHexString(hostSalt)}")
                Timber.tag(TAG).e("鉴权:妥妥贴Salt=${BleAuthUtils.byteArrayToHexString(deviceSalt)}")
                Timber.tag(TAG).e("鉴权:生成密钥材料: iv=${BleAuthUtils.byteArrayToHexString(iv)}, key=${BleAuthUtils.byteArrayToHexString(key)}")
                Timber.tag(TAG).e("鉴权:车机Addr=${BleAuthUtils.byteArrayToHexString(expectedAddr.reversedArray())}")
                Timber.tag(TAG).e("鉴权:解密的PublicAddr=${BleAuthUtils.byteArrayToHexString(decryptedAddr)}")

                // 检查PublicAddr是否匹配
                val addrMatched = decryptedAddr.contentEquals(expectedAddr.reversedArray())
                if (!addrMatched) {
                    Timber.tag(TAG).e("鉴权:PublicAddr不匹配: 期望=${BleAuthUtils.byteArrayToHexString(expectedAddr.reversedArray())}, 实际=${BleAuthUtils.byteArrayToHexString(decryptedAddr)}")
                    sendAuthStatus(gatt, false)
                    completeAuthentication(device.address, false)
                    return
                }

                // 2. 验证认证数据
                val expectedRawData = BleAuthUtils.generateRawAuthData(hostSalt, deviceSalt).copyOfRange(0,BleConstants.Auth.PUBLIC_ADDR_LENGTH).reversedArray()
                val decryptedAuthData = BleAuthUtils.aesDecrypt(authData, key, iv)

                // Timber.tag(TAG).e("expectedRawData=${BleAuthUtils.byteArrayToHexString(expectedRawData)}")
                // Timber.tag(TAG).e("decryptedAuthData=${BleAuthUtils.byteArrayToHexString(decryptedAuthData)}")


                // 检查认证数据是否匹配
                val dataMatched = decryptedAuthData.contentEquals(expectedRawData)

                if (!dataMatched) {
                    Timber.tag(TAG).e("鉴权:认证数据不匹配")
                    sendAuthStatus(gatt, false)
                    completeAuthentication(device.address, false)
                    return
                }

                // 鉴权成功，发送成功状态
                Timber.tag(TAG).w("鉴权:设备鉴权验证成功")

                // 存储新的鉴权信息（包含设备Salt）
                authenticationData[device.address] = AuthData(
                    hostSalt = hostSalt,
                    deviceSalt = deviceSalt,
                    iv = iv,
                    key = key
                )

                // 发送鉴权成功响应
                sendAuthStatus(gatt, true)

                // 完成鉴权
                completeAuthentication(device.address, true)

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "鉴权:处理鉴权响应时发生异常")
                sendAuthStatus(gatt, false)
                completeAuthentication(device.address, false)
            }
        } else {
            Timber.tag(TAG).e("鉴权:收到未知的鉴权指令: ${data[1]}")
            completeAuthentication(device.address, false)
        }
    }

    /**
     * 完成鉴权并通知结果
     * @param deviceAddress 设备地址
     * @param success 是否鉴权成功
     */
    private fun completeAuthentication(deviceAddress: String, success: Boolean) {
        authenticationResults[deviceAddress]?.let { deferred ->
            if (!deferred.isCompleted) {
                deferred.complete(success)
                Timber.tag(TAG).w("鉴权:完成, ${deviceAddress}, 结果: ${if (success) "成功" else "失败"}")
            }
        }
    }

    /**
     * 发送鉴权状态给设备
     * @param gatt GATT连接
     * @param success 是否鉴权成功
     */
    private fun sendAuthStatus(gatt: BluetoothGatt, success: Boolean) {
        // 获取写入特征
        val service = gatt.getService(mainServiceUUID) ?: run {
            Timber.tag(TAG).e("发送鉴权状态失败: 找不到主服务")
            return
        }

        val writeCharacteristic = service.getCharacteristic(characteristicWriteUuid) ?: run {
            Timber.tag(TAG).e("发送鉴权状态失败: 找不到写入特征")
            return
        }

        // 构建状态响应
        val statusCode = if (success) BleConstants.AuthStatus.AUTH_SUCCESS else BleConstants.AuthStatus.AUTH_FAILED
        val statusResponse = ByteArray(4)
        statusResponse[0] = BleConstants.HEADER_AUTH
        statusResponse[1] = BleConstants.AuthInstruction.AUTH_STATUS_RESPONSE
        statusResponse[2] = (statusCode and 0xFF).toByte()
        statusResponse[3] = (statusCode.toInt() shr 8 and 0xFF).toByte()

        Timber.tag(TAG).w("发送鉴权状态: ${if (success) "成功" else "失败"}")

        // 发送状态响应
        coroutineScope.launch(Dispatchers.IO) {
            writeCharacteristicWithTimeout(gatt, writeCharacteristic, statusResponse)
        }
    }



    override fun close() {
        super.close()
        authenticationData.clear()
    }

   override suspend fun readDeviceInfo(
        gatt: BluetoothGatt,
        readCharacteristic: BluetoothGattCharacteristic
    ) {

       val service = gatt.getService(mainServiceUUID) ?: run {
           Timber.tag(com.mine.baselibrary.bluetooth.TAG).e("发送鉴权状态失败: 找不到主服务")
           return
       }

       val readCharacteristic =
           service.getCharacteristic(characteristicNotifyUuid) ?: run {
               Timber.tag(TAG).e("发送鉴权状态失败: 找不到写入特征")
               return
           }
       readCharacteristicWithTimeout(gatt, readCharacteristic)
    }

}