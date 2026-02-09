package com.smartlife.fragrance.service

import android.graphics.Color
import android.content.Context
import com.example.tuotuotie_car_interface_library.ICarFunctionExecutor
import com.smartlife.fragrance.bluetooth.FragranceAbsBleManager
import com.smartlife.fragrance.data.model.FragranceDevice
import com.smartlife.fragrance.repository.FragranceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FragranceCarExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carFunctionExecutor: ICarFunctionExecutor,
    private val fragranceRepository: FragranceRepository,
    private val bleManager: FragranceAbsBleManager,
) {
    companion object {
        const val TAG = "FragranceCarExecutor"
        
        /**
         * 256色值与RGB颜色值的映射表
         * 根据3.11.1 256-color Ambient Light Optical Specification Table (C518 Project)提取
         * 键：色号（1-256），值：RGB颜色值（格式：0xFFRRGGBB）
         */
        private val COLOR_256_TO_RGB_MAP: Map<Int, Int> = buildMap {
            // 行1-19: 从白色到紫色，再到蓝色
            put(1, Color.rgb(255, 255, 255))
            put(2, Color.rgb(242, 223, 255))
            put(3, Color.rgb(232, 193, 255))
            put(4, Color.rgb(222, 162, 255))
            put(5, Color.rgb(214, 130, 255))
            put(6, Color.rgb(207, 92, 255))
            put(7, Color.rgb(200, 0, 255))
            put(8, Color.rgb(174, 0, 255))
            put(9, Color.rgb(148, 0, 255))
            put(10, Color.rgb(119, 0, 255))
            put(11, Color.rgb(84, 0, 255))
            put(12, Color.rgb(0, 0, 255))
            put(13, Color.rgb(0, 75, 255))
            put(14, Color.rgb(0, 105, 255))
            put(15, Color.rgb(0, 129, 255))
            put(16, Color.rgb(0, 151, 255))
            put(17, Color.rgb(0, 172, 255))
            put(18, Color.rgb(0, 192, 255))
            put(19, Color.rgb(0, 212, 255))
            // 行20-68: 从青色到绿色，到黄色，到红色，到品红色
            put(20, Color.rgb(0, 233, 255))
            put(21, Color.rgb(0, 255, 255))
            put(22, Color.rgb(0, 255, 234))
            put(23, Color.rgb(0, 255, 215))
            put(24, Color.rgb(0, 255, 197))
            put(25, Color.rgb(0, 255, 180))
            put(26, Color.rgb(0, 255, 164))
            put(27, Color.rgb(0, 255, 148))
            put(28, Color.rgb(0, 255, 133))
            put(29, Color.rgb(0, 255, 116))
            put(30, Color.rgb(0, 255, 99))
            put(31, Color.rgb(0, 255, 80))
            put(32, Color.rgb(0, 255, 57))
            put(33, Color.rgb(0, 255, 0))
            put(34, Color.rgb(73, 255, 0))
            put(35, Color.rgb(102, 255, 0))
            put(36, Color.rgb(126, 255, 0))
            put(37, Color.rgb(148, 255, 0))
            put(38, Color.rgb(169, 255, 0))
            put(39, Color.rgb(190, 255, 0))
            put(40, Color.rgb(211, 255, 0))
            put(41, Color.rgb(233, 255, 0))
            put(42, Color.rgb(255, 255, 0))
            put(43, Color.rgb(255, 233, 0))
            put(44, Color.rgb(255, 212, 0))
            put(45, Color.rgb(255, 192, 0))
            put(46, Color.rgb(255, 173, 0))
            put(47, Color.rgb(255, 154, 0))
            put(48, Color.rgb(255, 135, 0))
            put(49, Color.rgb(255, 115, 0))
            put(50, Color.rgb(255, 93, 0))
            put(51, Color.rgb(255, 66, 0))
            put(52, Color.rgb(255, 0, 0))
            put(53, Color.rgb(255, 0, 60))
            put(54, Color.rgb(255, 0, 84))
            put(55, Color.rgb(255, 0, 104))
            put(56, Color.rgb(255, 0, 122))
            put(57, Color.rgb(255, 0, 140))
            put(58, Color.rgb(255, 0, 157))
            put(59, Color.rgb(255, 0, 175))
            put(60, Color.rgb(255, 0, 194))
            put(61, Color.rgb(255, 0, 214))
            put(62, Color.rgb(255, 0, 236))
            put(63, Color.rgb(249, 0, 255))
            put(64, Color.rgb(224, 0, 255))
            put(65, Color.rgb(231, 90, 255))
            put(66, Color.rgb(240, 130, 255))
            put(67, Color.rgb(250, 163, 255))
            put(68, Color.rgb(255, 190, 249))
            
            // 行69-117: 从粉紫色到蓝色，到绿色，到黄色，到橙色
            put(69, Color.rgb(255, 211, 237))
            put(70, Color.rgb(255, 229, 225))
            put(71, Color.rgb(240, 255, 230))
            put(72, Color.rgb(202, 255, 213))
            put(73, Color.rgb(213, 255, 237))
            put(74, Color.rgb(219, 247, 255))
            put(75, Color.rgb(210, 221, 255))
            put(76, Color.rgb(203, 196, 255))
            put(77, Color.rgb(196, 170, 255))
            put(78, Color.rgb(190, 144, 255))
            put(79, Color.rgb(184, 116, 255))
            put(80, Color.rgb(179, 81, 255))
            put(81, Color.rgb(152, 79, 255))
            put(82, Color.rgb(123, 76, 255))
            put(83, Color.rgb(89, 74, 255))
            put(84, Color.rgb(91, 105, 255))
            put(85, Color.rgb(93, 129, 255))
            put(86, Color.rgb(95, 151, 255))
            put(87, Color.rgb(98, 171, 255))
            put(88, Color.rgb(101, 192, 255))
            put(89, Color.rgb(104, 212, 255))
            put(90, Color.rgb(107, 232, 255))
            put(91, Color.rgb(111, 253, 255))
            put(92, Color.rgb(106, 255, 236))
            put(93, Color.rgb(102, 255, 217))
            put(94, Color.rgb(98, 255, 200))
            put(95, Color.rgb(95, 255, 183))
            put(96, Color.rgb(91, 255, 168))
            put(97, Color.rgb(89, 255, 153))
            put(98, Color.rgb(86, 255, 138))
            put(99, Color.rgb(84, 255, 124))
            put(100, Color.rgb(82, 255, 109))
            put(101, Color.rgb(80, 255, 93))
            put(102, Color.rgb(78, 255, 75))
            put(103, Color.rgb(76, 255, 52))
            put(104, Color.rgb(100, 255, 54))
            put(105, Color.rgb(121, 255, 55))
            put(106, Color.rgb(140, 255, 57))
            put(107, Color.rgb(158, 255, 59))
            put(108, Color.rgb(176, 255, 61))
            put(109, Color.rgb(195, 255, 63))
            put(110, Color.rgb(214, 255, 66))
            put(111, Color.rgb(235, 255, 68))
            put(112, Color.rgb(255, 253, 71))
            put(113, Color.rgb(255, 232, 68))
            put(114, Color.rgb(255, 211, 66))
            put(115, Color.rgb(255, 192, 64))
            put(116, Color.rgb(255, 173, 62))
            put(117, Color.rgb(255, 155, 60))
            
            // 行118-166: 从橙色到红色，到品红色，到绿色，到蓝色
            put(118, Color.rgb(255, 136, 59))
            put(119, Color.rgb(255, 116, 57))
            put(120, Color.rgb(255, 94, 56))
            put(121, Color.rgb(255, 67, 55))
            put(122, Color.rgb(255, 68, 79))
            put(123, Color.rgb(255, 69, 98))
            put(124, Color.rgb(255, 71, 115))
            put(125, Color.rgb(255, 73, 131))
            put(126, Color.rgb(255, 74, 147))
            put(127, Color.rgb(255, 76, 163))
            put(128, Color.rgb(255, 79, 180))
            put(129, Color.rgb(255, 81, 197))
            put(130, Color.rgb(255, 84, 215))
            put(131, Color.rgb(255, 88, 236))
            put(132, Color.rgb(252, 91, 255))
            put(133, Color.rgb(255, 125, 250))
            put(134, Color.rgb(255, 150, 241))
            put(135, Color.rgb(255, 171, 231))
            put(136, Color.rgb(255, 189, 222))
            put(137, Color.rgb(255, 206, 212))
            put(138, Color.rgb(255, 220, 201))
            put(139, Color.rgb(254, 255, 213))
            put(140, Color.rgb(218, 255, 198))
            put(141, Color.rgb(186, 255, 186))
            put(142, Color.rgb(156, 255, 176))
            put(143, Color.rgb(162, 255, 192))
            put(144, Color.rgb(168, 255, 209))
            put(145, Color.rgb(176, 255, 228))
            put(146, Color.rgb(184, 255, 248))
            put(147, Color.rgb(182, 240, 255))
            put(148, Color.rgb(176, 218, 255))
            put(149, Color.rgb(171, 197, 255))
            put(150, Color.rgb(166, 177, 255))
            put(151, Color.rgb(162, 156, 255))
            put(152, Color.rgb(158, 134, 255))
            put(153, Color.rgb(154, 109, 255))
            put(154, Color.rgb(124, 105, 255))
            put(155, Color.rgb(127, 130, 255))
            put(156, Color.rgb(130, 151, 255))
            put(157, Color.rgb(134, 172, 255))
            put(158, Color.rgb(137, 192, 255))
            put(159, Color.rgb(141, 212, 255))
            put(160, Color.rgb(146, 232, 255))
            put(161, Color.rgb(150, 254, 255))
            put(162, Color.rgb(144, 255, 236))
            put(163, Color.rgb(138, 255, 217))
            put(164, Color.rgb(132, 255, 200))
            put(165, Color.rgb(127, 255, 184))
            put(166, Color.rgb(123, 255, 168))
            
            // 行167-215: 从绿色到黄色，到橙色，到红色，到品红色，再到绿色
            put(167, Color.rgb(119, 255, 153))
            put(168, Color.rgb(116, 255, 139))
            put(169, Color.rgb(112, 255, 124))
            put(170, Color.rgb(109, 255, 109))
            put(171, Color.rgb(107, 255, 94))
            put(172, Color.rgb(104, 255, 76))
            put(173, Color.rgb(125, 255, 79))
            put(174, Color.rgb(145, 255, 81))
            put(175, Color.rgb(164, 255, 84))
            put(176, Color.rgb(183, 255, 87))
            put(177, Color.rgb(203, 255, 90))
            put(178, Color.rgb(223, 255, 94))
            put(179, Color.rgb(245, 255, 98))
            put(180, Color.rgb(255, 241, 98))
            put(181, Color.rgb(255, 219, 94))
            put(182, Color.rgb(255, 199, 91))
            put(183, Color.rgb(255, 179, 88))
            put(184, Color.rgb(255, 159, 85))
            put(185, Color.rgb(255, 140, 83))
            put(186, Color.rgb(255, 119, 81))
            put(187, Color.rgb(255, 96, 79))
            put(188, Color.rgb(255, 99, 99))
            put(189, Color.rgb(255, 101, 117))
            put(190, Color.rgb(255, 104, 134))
            put(191, Color.rgb(255, 107, 151))
            put(192, Color.rgb(255, 111, 168))
            put(193, Color.rgb(255, 115, 186))
            put(194, Color.rgb(255, 119, 205))
            put(195, Color.rgb(255, 124, 225))
            put(196, Color.rgb(255, 146, 217))
            put(197, Color.rgb(255, 164, 208))
            put(198, Color.rgb(255, 180, 199))
            put(199, Color.rgb(255, 195, 189))
            put(200, Color.rgb(255, 208, 178))
            put(201, Color.rgb(255, 240, 189))
            put(202, Color.rgb(236, 255, 187))
            put(203, Color.rgb(205, 255, 175))
            put(204, Color.rgb(176, 255, 166))
            put(205, Color.rgb(148, 255, 158))
            put(206, Color.rgb(143, 255, 143))
            put(207, Color.rgb(139, 255, 128))
            put(208, Color.rgb(136, 255, 113))
            put(209, Color.rgb(132, 255, 97))
            put(210, Color.rgb(153, 255, 100))
            put(211, Color.rgb(172, 255, 103))
            put(212, Color.rgb(193, 255, 107))
            put(213, Color.rgb(214, 255, 112))
            put(214, Color.rgb(236, 255, 117))
            put(215, Color.rgb(255, 250, 120))
            
            // 行216-256: 从黄色到橙色，到红色，到绿色，循环
            put(216, Color.rgb(255, 227, 115))
            put(217, Color.rgb(255, 205, 109))
            put(218, Color.rgb(255, 184, 107))
            put(219, Color.rgb(255, 163, 103))
            put(220, Color.rgb(255, 143, 100))
            put(221, Color.rgb(255, 122, 97))
            put(222, Color.rgb(255, 125, 117))
            put(223, Color.rgb(255, 129, 136))
            put(224, Color.rgb(255, 134, 154))
            put(225, Color.rgb(255, 139, 173))
            put(226, Color.rgb(255, 145, 193))
            put(227, Color.rgb(255, 160, 185))
            put(228, Color.rgb(255, 174, 176))
            put(229, Color.rgb(255, 186, 167))
            put(230, Color.rgb(255, 198, 157))
            put(231, Color.rgb(255, 226, 165))
            put(232, Color.rgb(253, 255, 173))
            put(233, Color.rgb(222, 255, 163))
            put(234, Color.rgb(194, 255, 154))
            put(235, Color.rgb(168, 255, 146))
            put(236, Color.rgb(163, 255, 131))
            put(237, Color.rgb(158, 255, 116))
            put(238, Color.rgb(179, 255, 120))
            put(239, Color.rgb(201, 255, 125))
            put(240, Color.rgb(224, 255, 131))
            put(241, Color.rgb(249, 255, 138))
            put(242, Color.rgb(255, 236, 134))
            put(243, Color.rgb(255, 212, 129))
            put(244, Color.rgb(255, 189, 124))
            put(245, Color.rgb(255, 168, 119))
            put(246, Color.rgb(255, 146, 115))
            put(247, Color.rgb(255, 151, 137))
            put(248, Color.rgb(255, 158, 159))
            put(249, Color.rgb(255, 171, 149))
            put(250, Color.rgb(255, 167, 134))
            put(251, Color.rgb(255, 191, 139))
            put(252, Color.rgb(255, 216, 146))
            put(253, Color.rgb(255, 243, 153))
            put(254, Color.rgb(238, 255, 151))
            put(255, Color.rgb(211, 255, 143))
            put(256, Color.rgb(187, 255, 137))
        }
        
        /**
         * 将256色值转换为RGB颜色值
         * 
         * @param color256 256色值，范围：1-256（0表示未激活）
         * @return RGB颜色值（格式：0xFFRRGGBB），如果输入无效则返回默认颜色
         */
        fun convert256ColorToRgb(color256: Int): Int {
            // 检查输入有效性：0表示未激活，1-256为有效范围
            if (color256 <= 0 || color256 > 256) {
                Timber.tag(TAG).w("无效的256色值: $color256，使用默认颜色")
                return Color.rgb(255, 255, 255) // 默认返回白色
            }
            
            // 从映射表中获取RGB颜色值
            return COLOR_256_TO_RGB_MAP[color256] ?: run {
                Timber.tag(TAG).w("色值 $color256 未在映射表中找到，使用默认颜色")
                Color.rgb(255, 255, 255) // 默认返回白色
            }
        }
    }

    suspend fun syncAtmosphereLight(fragranceDevices: List<FragranceDevice>) {
        Timber.tag(TAG).d("开始随车联动氛围灯颜色同步")
        fragranceDevices.forEach { fragranceDevice ->
            fragranceRepository.getDeviceByMacAddress(fragranceDevice.macAddress)
                ?.let { fragranceDevice ->
                    if (fragranceDevice.syncLightBrightness) {
                        Timber.tag(TAG).i("${fragranceDevice.deviceName}  syncLightBrightness")
                        val atmosphereLightBrightness =
                            carFunctionExecutor.getAtmosphereLightBrightness()
                        val atmosphereLightSwitch =
                            carFunctionExecutor.getAtmosphereLightSwtich()
                        if (atmosphereLightBrightness > 0 || atmosphereLightSwitch ==1) {
                            val ambientLightColor256 = carFunctionExecutor.getAmbientLightColor()//获取车机颜色
                            Timber.tag(TAG).i("颜色值为 $ambientLightColor256")

                            // 将256色值转换为RGB颜色值,
                            val rgbColor = convert256ColorToRgb(ambientLightColor256) //转化位256

                            val red = Color.red(rgbColor)
                            val green = Color.green(rgbColor)
                            val blue = Color.blue(rgbColor)

                            bleManager.setLightColor(fragranceDevice.macAddress, red,green,blue)//车机颜色同步给香氛



                            Timber.tag(TAG).i("转换后RGB颜色: R=$red, G=$green, B=$blue (十六进制: 0x${rgbColor.toString(16).toUpperCase()})")

                        }
                    } else {
                        Timber.tag(TAG).i("${fragranceDevice.deviceName} not syncLightBrightness")
                    }
                }
        }
    }

    fun isIgnition(): Boolean{
        return carFunctionExecutor.isIgnition()
    }

    fun listenIsIgnition(callBack: (isIgnition: Boolean) -> Unit){
        carFunctionExecutor.listenIsIgnition {
            callBack(it)
        }
    }
}