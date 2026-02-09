package com.zkjd.lingdong.ui.led

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.zkjd.lingdong.R
import com.zkjd.lingdong.model.LedColor
import com.zkjd.lingdong.repository.DeviceRepository
import com.zkjd.lingdong.service.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * LED灯颜色设置ViewModel
 */
@HiltViewModel
class PreventAndLedColorAndAudioViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    fun getContext(): Context = context

    // 预设颜色列表
    val presetColors = listOf(
        LedColor.RED,
        LedColor.ORANGE,
        LedColor.YELLOW,
        LedColor.GREEN,
        LedColor.BLUE,

        LedColor.MAGENTA,
        LedColor.CYAN,
        LedColor.BLUE2,

    )
    
    // UI事件
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    fun getUiEventsState(): LiveData<UiEvent> = uiEvents.asLiveData()
    
    // 当前设备MAC地址
    private var currentDeviceMac: String? = null

    private lateinit var soundManager: SoundManager


    fun observedIsPreventAccidental(deviceMac: String,onChange:(isPreventAccidental:Boolean)-> Unit){
        viewModelScope.launch {
            deviceRepository.getDevice(macAddress =deviceMac ).collect { device->
                device?.let {
                    val prevent = if (device.preventAccidental == 0) false else true
                    onChange(prevent)
                }
            }
        }
    }

    /**
     * 加载设备颜色
     */
    fun loadDeviceColor(deviceMac: String,initState:(ledColor: LedColor, isPreventAccidental:Boolean, isCanMusic: Boolean, musicName:String)-> Unit) {


        soundManager = SoundManager(context)

        soundManager.loadSound(R.raw.click_sound)
        soundManager.loadSound(R.raw.dong)
        soundManager.loadSound(R.raw.dack)

        currentDeviceMac = deviceMac
        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) {
                    deviceRepository.getDeviceByMacAddress(deviceMac)
                }
                if (device != null) {
                    // 加载设备的两种颜色
                    val connectedColorInt = device.connectedLedColor
                    val reconnectingColorInt = device.reconnectingLedColor

                    if(device!!.bleName.contains("DeepalTag_L0"))
                    {
                       // _isRoty.value=true
                    }else{
                       // _isRoty.value=true
                    }
                    val musicName = device.musicName
                    val musicCan = if (device.musicCan == 0) false else true
                    val prevent = if (device.preventAccidental == 0) false else true

                    // 找到最接近的预设颜色或创建新的LedColor对象
                    val connectedColor = findClosestPresetColor(connectedColorInt)
                    val reconnectingColor = findClosestPresetColor(reconnectingColorInt)

                    initState(connectedColor,prevent,musicCan,musicName)
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("获取LED颜色失败: ${e.message}"))
            }
        }
    }
    



    /**
     * 设置防误触保存和下发指令
     */
    fun setPreventAccidental(deviceMac: String,isPreventAccidental: Boolean) {
        viewModelScope.launch {
            try {
                val value=when(isPreventAccidental){
                    false ->1
                    true  ->0
                }

                withContext(Dispatchers.IO) {
                    val device = deviceRepository.getDeviceByMacAddress(deviceMac)
                    if (device != null) {
                        if(device.returnControl==1) {
                            device.preventAccidental = value
                            deviceRepository.setPreventAccidental(deviceMac, value)
                            deviceRepository.updateDevice(device)

                            //添加写入
                            deviceRepository.setPreventAccid(deviceMac, value)

                            val message=if(value==0) "防误触关闭" else "防误触开启"
                            _uiEvents.emit(UiEvent.ShowError(message))
                        }else{
                            _uiEvents.emit(UiEvent.ShowError("请长按妥妥贴5-10s\n进入返控模式后设置防误触"))
                        }
                    }


                }

            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("防误触设置失败"))

            }
        }
    }


     fun getPreventAccidental(deviceMac: String, preventResult:(Boolean)->Unit){
         viewModelScope.launch {
             val device = deviceRepository.getDeviceByMacAddress(deviceMac)
             preventResult(device?.returnControl==1)
         }
    }

    fun isReturnControl(deviceMac: String, isReturnControl:(Boolean)->Unit){
        viewModelScope.launch {
            val device = deviceRepository.getDeviceByMacAddress(deviceMac)
            isReturnControl(device?.returnControl==1)
        }
    }

    fun isMusicOn(deviceMac: String, isMusicOn:(Boolean)->Unit){
        viewModelScope.launch {
            val device = deviceRepository.getDeviceByMacAddress(deviceMac)
            isMusicOn(device?.musicCan==1)
        }
    }

    /**
     * 查找最接近的预设颜色
     */
    private fun findClosestPresetColor(colorInt: Int): LedColor {
        // 简单实现：如果颜色与预设颜色完全匹配，则返回该预设颜色
        presetColors.forEach { presetColor ->
            if (presetColor.color.toArgb() == colorInt) {
                return presetColor
            }
        }
        
        // 如果没有匹配的预设颜色，则创建一个新的LedColor对象
        return LedColor(Color(colorInt), "自定义")
    }
    

    
    /**
     * 选择自定义颜色（字符串输入）
     */
    fun selectCustomColor(hexColor: String) {
        try {
            val color = LedColor.fromHex(hexColor)
            applyColorSetting(color)
        } catch (e: Exception) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowError("无效的颜色格式"))
            }
        }
    }

    /**
     *
     */
    fun toggleMusic(mac: String,isCanMusic: Boolean) {
        viewModelScope.launch {
            deviceRepository.setMusicCan(mac,isCanMusic)
            _uiEvents.emit(UiEvent.ShowError("音效已${if (isCanMusic) "开启" else "关闭"}"))
        }
    }

    /**
     * 保存选择的音效
     */
    fun toSaveAudioName(mac: String,id: String) {
        viewModelScope.launch {
            deviceRepository.renameMusicID(mac,id)
            when (id) {
                "1" -> {
                    soundManager.playSound(R.raw.click_sound)
                }

                "2" -> {
                    soundManager.playSound(R.raw.dong)
                }

                "3" -> {
                    soundManager.playSound(R.raw.dack)
                }
            }
        }
    }
    
    /**
     * 选择自定义颜色（Color对象）
     */
    fun selectCustomColor(color: Color, name: String = "自定义") {
        val ledColor = LedColor(color, name)
        applyColorSetting(ledColor)
    }
    
    /**
     * 应用颜色设置到设备
     */
    fun applyColorSetting(ledColor: LedColor) {
        val deviceMac = currentDeviceMac ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val device = deviceRepository.getDeviceByMacAddress(deviceMac)
                    if (device != null) {
                        // 更新设备的两种颜色
                        if(device.returnControl==1) {
                            device.connectedLedColor = ledColor.color.toArgb()

                            deviceRepository.setLedColor(deviceMac, true, device.connectedLedColor)

                            deviceRepository.updateDevice(device)

                            //发送指令
                            deviceRepository.setColor(deviceMac, true, device.connectedLedColor)

                            _uiEvents.emit(UiEvent.ColorApplied)
                        }else{
                            _uiEvents.emit(UiEvent.ShowError("请长按妥妥贴5-10s\n进入返控模式后设置颜色"))
                        }
                    }
                }

            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("保存颜色设置失败: ${e.message}"))
            }
        }
    }
    
    /**
     * UI事件
     */
    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        object ColorApplied : UiEvent()
    }
} 