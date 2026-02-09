package com.zkjd.lingdong.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.mine.baselibrary.bluetooth.TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TuoTuoTieBleManagerImp@Inject constructor(
    @ApplicationContext override val appContext: Context,
) : TuoTuoTieAbsBleManager(appContext){

    /**
     * 发送命令到设备
     * @param device 蓝牙设备
     * @param header 命令头
     * @param instruction 指令类型
     * @param value1 参数1
     * @param value2 参数2
     * @param value3 参数3
     * @return 发送成功返回true，否则返回false
     */
    private suspend fun sendCommand(
        device: BluetoothDevice,
        header: Byte,
        instruction: Byte,
        value1: Byte,
        value2: Byte,
        value3: Byte,
    ): Boolean = withContext(Dispatchers.IO) {
        val gatt = deviceGattMap[device.address] ?: run {
            Timber.tag(TAG).e("发送命令失败: 设备未连接 ${device.address}")
            return@withContext false
        }

        val service = gatt.getService(mainServiceUUID) ?: run {
            Timber.tag(TAG).e("发送命令失败: 找不到主服务 ${device.address}")
            return@withContext false
        }

        val characteristic = service.getCharacteristic(characteristicWriteUuid) ?: run {
            Timber.tag(TAG).e("发送命令失败: 找不到写入特征 ${device.address}")
            return@withContext false
        }

        // 构建命令数据
        val command = byteArrayOf(header, instruction, value1, value2, value3)
        Timber.tag(TAG).w("发送命令: ${bytesToHex(command)} 到设备 ${device.address}")

        // 使用带超时的写入
        return@withContext writeCharacteristicWithTimeout(gatt, characteristic, command)
    }

    private suspend fun sendCommand2(
        address: String,
        header: Byte,
        instruction: Byte,
        value1: Byte,
        value2: Byte,
        value3: Byte,
    ): Boolean = withContext(Dispatchers.IO) {
        val gatt = deviceGattMap[address] ?: run {
            Timber.tag(TAG).e("发送命令失败: 设备未连接 ${address}")
            return@withContext false
        }

        val service = gatt.getService(mainServiceUUID) ?: run {
            Timber.tag(TAG).e("发送命令失败: 找不到主服务 ${address}")
            return@withContext false
        }

        val characteristic = service.getCharacteristic(characteristicWriteUuid) ?: run {
            Timber.tag(TAG).e("发送命令失败: 找不到写入特征 ${address}")
            return@withContext false
        }

        // 构建命令数据
        val command = byteArrayOf(header, instruction, value1, value2, value3)
        Timber.tag(TAG).w("发送命令: ${bytesToHex(command)} 到设备 ${address}")

        // 使用带超时的写入
        return@withContext writeCharacteristicWithTimeout(gatt, characteristic, command)
    }

    /**
     * 设置设备工作状态下的LED颜色
     * @param device 蓝牙设备
     * @param r 红色分量 (0-255)
     * @param g 绿色分量 (0-255)
     * @param b 蓝色分量 (0-255)
     * @return 设置成功返回true，否则返回false
     */
    override suspend fun setWorkingLedColor(
        device: BluetoothDevice,
        r: Int,
        g: Int,
        b: Int
    ): Boolean = sendCommand(
        device,
        BleConstants.HEADER_CONTROL,
        BleConstants.Instruction.BACKLIGHT_IN_WORKING,
        r.toByte(),
        g.toByte(),
        b.toByte(),
    )

    override suspend fun setWorkingLedColor2(
        device: String,
        r: Int,
        g: Int,
        b: Int
    ): Boolean = sendCommand2(
        device,
        BleConstants.HEADER_CONTROL,
        BleConstants.Instruction.BACKLIGHT_IN_WORKING,
        r.toByte(),
        g.toByte(),
        b.toByte(),
    )

    override suspend fun setPreventAccid(
        device: String,
        pA: Int
    ): Boolean = sendCommand2(
        device,
        BleConstants.HEADER_CONTROL,
        BleConstants.Instruction.KNOB_ANTI_MISOPERATION,
        pA.toByte(),
        0,
        0,
    )

    /**
     * 设置设备重连状态下的LED颜色
     * @param device 蓝牙设备
     * @param r 红色分量 (0-255)
     * @param g 绿色分量 (0-255)
     * @param b 蓝色分量 (0-255)
     * @return 设置成功返回true，否则返回false
     */
    override suspend fun setReconnectingLedColor(
        device: BluetoothDevice,
        r: Int,
        g: Int,
        b: Int
    ): Boolean = sendCommand(
        device,
        BleConstants.HEADER_CONTROL,
        BleConstants.Instruction.BACKLIGHT_IN_WAITING_RECONNECTION,
        r.toByte(),
        g.toByte(),
        b.toByte(),
    )

    /**
     * 设置设备防误触模式
     * @param device 蓝牙设备
     * @param enabled 是否启用防误触
     * @return 设置成功返回true，否则返回false
     */
    override suspend fun setAntiMisoperation(
        device: BluetoothDevice,
        enabled: Boolean
    ): Boolean = sendCommand(
        device,
        BleConstants.HEADER_CONTROL,
        BleConstants.Instruction.KNOB_ANTI_MISOPERATION,
        if (enabled) 0x01 else 0x00,
        0x00,
        0x00,
    )



}