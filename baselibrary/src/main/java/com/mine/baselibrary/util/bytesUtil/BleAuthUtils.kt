package com.mine.baselibrary.util.bytesUtil

import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 蓝牙认证工具类
 * 提供AES加密、解密和SHA256哈希等功能
 */
object BleAuthUtils {
    private const val TAG = "BleAuthUtils"

    /**
     * 生成随机盐值（Salt）
     * @param length 盐值长度
     * @return 随机盐值字节数组
     */
    fun generateRandomSalt(length: Int): ByteArray {
        val salt = ByteArray(length)
        try {
            SecureRandom().nextBytes(salt)
            return salt
        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "生成随机盐值失败")
            // 如果安全随机数生成失败，使用普通随机数
            for (i in 0 until length) {
                salt[i] = (Math.random() * 256).toInt().toByte()
            }
            return salt
        }
    }

    /**
     * 计算SHA256哈希
     * @param data 要计算哈希的数据
     * @return 哈希结果
     */
    fun sha256(data: ByteArray): ByteArray {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(data)
            digest.digest()
        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "SHA256哈希计算失败")
            ByteArray(32) // 返回空哈希
        }
    }

    /**
     * 使用AES-128-CBC模式加密数据
     * @param data 要加密的数据
     * @param key 密钥
     * @param iv 初始化向量
     * @return 加密后的数据
     */
    fun aesEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "AES加密失败")
            ByteArray(16) // 返回空数据
        }
    }

    /**
     * 使用AES-128-CBC模式解密数据
     * @param encrypted 加密的数据
     * @param key 密钥
     * @param iv 初始化向量
     * @return 解密后的数据
     */
    fun aesDecrypt(encrypted: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return try {
//            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            val keySpec = SecretKeySpec(key.reversedArray(), "AES")
            val ivSpec = IvParameterSpec(iv.reversedArray())
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(encrypted.reversedArray())
        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "AES解密失败")
            ByteArray(16) // 返回空数据
        }
    }

    /**
     * 生成鉴权密钥材料
     * 拼接车机Salt、妥妥贴Salt和Label，然后计算SHA256哈希
     * @param hostSalt 车机随机数
     * @param deviceSalt 妥妥贴随机数
     * @param label 固定标签
     * @return Pair<ByteArray, ByteArray> 前16字节作为IV，后16字节作为密钥
     */
    fun generateKeyMaterial(hostSalt: ByteArray, deviceSalt: ByteArray, label: String): Pair<ByteArray, ByteArray> {
        // 将字符串标签转换为字节数组
        val labelBytes = if (label.startsWith("0x")) {
            // 以0x开头的十六进制字符串转换为字节数组
            hexStringToByteArray(label.substring(2))
        } else {
            label.toByteArray(StandardCharsets.UTF_8)
        }

        // 拼接数据
        val buffer = ByteBuffer.allocate(hostSalt.size + deviceSalt.size + labelBytes.size)
        buffer.put(hostSalt)
        buffer.put(deviceSalt)
        buffer.put(labelBytes)

        // 计算SHA256哈希
        val hash = sha256(buffer.array())

        // 分割哈希值为IV和密钥
        val iv = hash.copyOfRange(0, 16)  // 前16字节作为IV
        val key = hash.copyOfRange(16, 32)  // 后16字节作为密钥

        return Pair(iv, key)
    }

    /**
     * 生成认证数据
     * SHA256(0x02 || 0xaa || 0xbb || 车机salt || 妥妥贴salt)
     * @param hostSalt 车机随机数
     * @param deviceSalt 妥妥贴随机数
     * @return 认证数据
     */
    fun generateRawAuthData(hostSalt: ByteArray, deviceSalt: ByteArray): ByteArray {
        // 固定前缀
        val prefix = byteArrayOf(0x02, 0xaa.toByte(), 0xbb.toByte())

        // 拼接数据
        val buffer = ByteBuffer.allocate(prefix.size + hostSalt.size + deviceSalt.size)
        buffer.put(prefix)
        buffer.put(hostSalt)
        buffer.put(deviceSalt)

        // 计算SHA256哈希
        return sha256(buffer.array())
    }

    /**
     * 将十六进制字符串转换为字节数组
     * @param hexString 十六进制字符串
     * @return 字节数组
     */
    fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) +
                    Character.digit(hexString[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    fun byteArrayToHexString(bytes: ByteArray): String {
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            result.append(String.format("%02X", byte))
        }
        return result.toString()
    }

    /**
     * 将设备名称填充为指定长度的字节数组
     * @param deviceName 设备名称
     * @param length 目标长度
     * @return 填充后的字节数组
     */
    fun padDeviceName(deviceName: String, length: Int): ByteArray {
        val nameBytes = deviceName.toByteArray(StandardCharsets.UTF_8)
        val result = ByteArray(length)

        // 复制名称字节到结果数组（小端序）
        for (i in nameBytes.indices) {
            if (i < length) {
                result[i] = nameBytes[i]
            }
        }

        // 其余部分填充0
        for (i in nameBytes.size until length) {
            result[i] = 0
        }

        return result
    }
    /**
     * 将设备名称填充为指定长度的字节数组2 传送的是MAC地址，直接就是16进制字符串
     * @param deviceName 设备名称
     * @param length 目标长度
     * @return 填充后的字节数组
     */
    fun padDeviceName2(deviceName: String, length: Int): ByteArray {
        val nameBytes = hexStringToByteArray(deviceName)//转为字节数组
        val result = ByteArray(length)


        // 复制名称字节到结果数组（小端序）
        for (i in nameBytes.indices) {
            val ints=nameBytes.size-1-i
            if (i < length) {
                result[i] = nameBytes[ints]
            }
        }

        // 其余部分填充0
        for (i in nameBytes.size until length) {
            result[i] = 0
        }

        return result
    }
}