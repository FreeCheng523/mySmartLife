package com.smartlife.fragrance.bluetooth

import com.smartlife.fragrance.data.model.CarStartStopCycle
import com.smartlife.fragrance.data.model.Gear
import com.smartlife.fragrance.data.model.LightMode
import com.smartlife.fragrance.data.model.Mode
import com.smartlife.fragrance.data.model.PowerState

/**
 * 香氛设备蓝牙命令构建器
 *
 * 协议格式：[header1=0xAA] [cmd] [data len] [data] [checksum]
 */
object FragranceCommandBuilder {

    // 命令头
    const val HEADER: Byte = 0xA0.toByte()

    // 命令码定义
    object Command {
        const val CMD_POWER: Byte = 0x00
        const val CMD_MODE: Byte = 0x01
        const val CMD_GEAR: Byte = 0x02
        const val CMD_RESTORE_FACTORY: Byte = 0x03
        const val CMD_CAR_START_STOP_ENABLED: Byte = 0x04
        const val CMD_CAR_START_STOP_CYCLE: Byte = 0x05
        const val CMD_LIGHT_MODE: Byte = 0x06
        const val CMD_LIGHT_COLOR: Byte = 0x07
        const val CMD_LIGHT_BRIGHTNESS: Byte = 0x08
        const val CMD_PROGRAM_VERSION: Byte = 0x09
        const val CMD_GET_DEVICE_STATUS: Byte = 0x0a.toByte()
        const val CMD_TIMING_DURATION: Byte = 0x0b.toByte()
    }

    // 恢复出厂设置固定值
    const val RESTORE_FACTORY_VALUE: Byte = 0x05

    // 获取设备状态固定值
    const val GET_DEVICE_STATUS_VALUE: Byte = 0x00

    /**
     * 计算校验和
     * 校验和 = 所有字节的累加和（不包括校验和本身）取低8位
     *
     * @param data 数据包（不包括校验和）
     * @return 校验和字节
     */
    fun calculateChecksum(data: ByteArray): Byte {
        var sum = 0
        for (byte in data) {
            sum += byte.toInt() and 0xFF
        }
        return (sum and 0xFF).toByte()
    }

    /**
     * 构建命令数据包
     *
     * @param cmd 命令码
     * @param data0 数据内容
     * @return 完整的命令数据包（包含header、cmd、data len、data0、checksum）
     */
    fun buildCommand(cmd: Byte, data0: ByteArray): ByteArray {
        val dataLen = data0.size.toByte()

        // 构建数据部分（header + cmd + data len + data0）
        val dataPart = ByteArray(3 + data0.size)
        dataPart[0] = HEADER
        dataPart[1] = cmd
        dataPart[2] = dataLen

        //将data0拷贝进dataPart
        System.arraycopy(data0, 0, dataPart, 3, data0.size)

        // 计算校验和（对所有数据部分计算）
        val checksum = calculateChecksum(dataPart)

        // 构建完整命令（数据部分 + 校验和）
        val command = ByteArray(dataPart.size + 1)
        System.arraycopy(dataPart, 0, command, 0, dataPart.size)
        command[command.size - 1] = checksum

        return command
    }

    /**
     * 构建单字节数据命令
     *
     * @param cmd 命令码
     * @param value 数据值（1字节）
     * @return 完整的命令数据包
     */
    fun buildSingleByteCommand(cmd: Byte, value: Byte): ByteArray {
        return buildCommand(cmd, byteArrayOf(value))
    }

    /**
     * 构建三字节数据命令（用于RGB颜色）
     *
     * @param cmd 命令码
     * @param r 红色分量 (0-255)
     * @param g 绿色分量 (0-255)
     * @param b 蓝色分量 (0-255)
     * @return 完整的命令数据包
     */
    fun buildRgbCommand(cmd: Byte, r: Int, g: Int, b: Int): ByteArray {
        val rByte = (r.coerceIn(0, 255)).toByte()
        val gByte = (g.coerceIn(0, 255)).toByte()
        val bByte = (b.coerceIn(0, 255)).toByte()

        return buildCommand(cmd, byteArrayOf(rByte,gByte, bByte))
    }

    /**
     * 构建开关控制命令 (0x00)
     */
    fun buildPowerCommand(powerState: PowerState): ByteArray {
        return buildSingleByteCommand(Command.CMD_POWER, powerState.value.toByte())
    }

    /**
     * 构建模式切换命令 (0x01)
     */
    fun buildModeCommand(mode: Mode): ByteArray {
        return buildSingleByteCommand(Command.CMD_MODE, mode.value.toByte())
    }

    /**
     * 构建档位切换命令 (0x02)
     */
    fun buildGearCommand(gear: Gear): ByteArray {
        return buildSingleByteCommand(Command.CMD_GEAR, gear.value.toByte())
    }

    /**
     * 构建恢复出厂设置命令 (0x03)
     */
    fun buildRestoreFactoryCommand(): ByteArray {
        return buildSingleByteCommand(Command.CMD_RESTORE_FACTORY, RESTORE_FACTORY_VALUE)
    }

    /**
     * 构建随车启停开关命令 (0x04)
     */
    fun buildCarStartStopEnabledCommand(enabled: Boolean): ByteArray {
        val value = if (enabled) 0x01.toByte() else 0x00.toByte()
        return buildSingleByteCommand(Command.CMD_CAR_START_STOP_ENABLED, value)
    }

    /**
     * 构建随车启停周期命令 (0x05)
     */
    fun buildCarStartStopCycleCommand(cycle: CarStartStopCycle): ByteArray {
        return buildSingleByteCommand(Command.CMD_CAR_START_STOP_CYCLE, cycle.value.toByte())
    }

    /**
     * 构建灯光模式命令 (0x06)
     */
    fun buildLightModeCommand(lightMode: LightMode): ByteArray {
        return buildSingleByteCommand(Command.CMD_LIGHT_MODE, lightMode.value.toByte())
    }

    /**
     * 构建灯光颜色命令 (0x07)
     *
     * @param colorHex RGB颜色字符串，格式为 "GGRRBB" 或 "RRGGBB"（会自动转换）
     */
    fun buildLightColorCommand(colorHex: String): ByteArray {
        // 移除#号（如果有）
        val hex = colorHex.replace("#", "").uppercase()

        // 如果是RRGGBB格式，转换为GGRRBB
        val r: Int
        val g: Int
        val b: Int

        if (hex.length == 6) {
            // 假设输入是RRGGBB格式
            r = hex.substring(0, 2).toInt(16)
            g = hex.substring(2, 4).toInt(16)
            b = hex.substring(4, 6).toInt(16)
        } else {
            // 默认值
            r = 0
            g = 0
            b = 0
        }

        return buildRgbCommand(Command.CMD_LIGHT_COLOR, r, g, b)
    }

    /**
     * 构建灯光颜色命令 (0x07) - 使用RGB值
     */
    fun buildLightColorCommand(r: Int, g: Int, b: Int): ByteArray {
        return buildRgbCommand(Command.CMD_LIGHT_COLOR, r, g, b)
    }

    /**
     * 构建灯光亮度命令 (0x08)
     */
    fun buildLightBrightnessCommand(brightness: Int): ByteArray {
        val value = brightness.coerceIn(0, 100).toByte()
        return buildSingleByteCommand(Command.CMD_LIGHT_BRIGHTNESS, value)
    }

    /**
     * 构建获取程序版本命令 (0x09)
     */
    fun buildGetProgramVersionCommand(): ByteArray {
        // 根据协议，获取版本命令需要发送0x01
        return buildSingleByteCommand(Command.CMD_PROGRAM_VERSION, 0x01.toByte())
    }

    /**
     * 构建获取设备状态命令 (0x0a)
     */
    fun buildGetDeviceStatusCommand(): ByteArray {
        return buildSingleByteCommand(Command.CMD_GET_DEVICE_STATUS, GET_DEVICE_STATUS_VALUE)
    }

    /**
     * 构建定时功能命令 (0x0b)
     *
     * @param minutes 定时时长（分钟），支持：5, 10, 30, 40, 50, 60, 90, 120
     * @return 命令数据包
     */
    fun buildTimingDurationCommand(minutes: Int): ByteArray {
        // 将分钟数转换为命令码（0x01-0x08）
        val cmdValue = when (minutes) {
            5 -> 0x01
            10 -> 0x02
            30 -> 0x03
            40 -> 0x04
            50 -> 0x05
            60 -> 0x06
            90 -> 0x07
            120 -> 0x08
            else -> {
                // 如果不在支持列表中，选择最接近的值
                when {
                    minutes <= 5 -> 0x01
                    minutes <= 10 -> 0x02
                    minutes <= 30 -> 0x03
                    minutes <= 40 -> 0x04
                    minutes <= 50 -> 0x05
                    minutes <= 60 -> 0x06
                    minutes <= 90 -> 0x07
                    else -> 0x08
                }
            }
        }
        return buildSingleByteCommand(Command.CMD_TIMING_DURATION, cmdValue.toByte())
    }
}