package com.zkjd.lingdong.ui.function

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zkjd.lingdong.data.FunctionsConfig
import com.zkjd.lingdong.model.AppInfo
import com.zkjd.lingdong.service.FunctionExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 功能配置ViewModel
 */
@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functionExecutor: FunctionExecutor
) : ViewModel() {
    
    // 应用列表
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()

    var fruits = listOf("")

    val isMeag=FunctionsConfig.getInstance(context).getIsMegaSys()

    /**
     * 加载应用列表
     */
    fun loadAppList() {

        if(isMeag){
            fruits = listOf(
                "com.mega.carplay"
                ,"com.mega.repair"
                ,"com.mega.sceneblock"
                ,"com.changan.appmarket"
                ,"com.qcarlink.quickapp.project"
                ,"com.mega.scenemode"
                ,"com.mega.mobileconnect"
                ,"com.mega.dlna"
                ,"com.mega.documentsui"
                ,"com.tinnove.plughardware"
                ,"com.qiyi.video.iv"
                ,"com.mega.manual"
                ,"com.mega.btphone"
                ,"com.mega.energy"
                ,"com.mega.dvr"
                ,"com.mega.carsettings"
                ,"com.mega.media"
                ,"cn.kuwo.kwmusiccar"
                ,"com.youku.car"
                ,"cn.cmvideo.car.play"
                ,"com.sohu.automotive"
                ,"com.mgtv.auto"
                ,"com.ajmide.car.a"
                ,"com.ifeng.ecarxfm"
                ,"io.dushu.car"
                ,"com.migu.miguplay"
                ,"com.wt.drmario"
                ,"com.wt.tetris"
                ,"com.tinnove.speedrun"
                ,"com.wt.chinesechess"
                ,"com.ximalaya.ting.android.carkids"
                ,"com.chinamobile.mcloudcar"
                ,"com.baidu.naviauto"
                ,"com.migu.car.music"
                ,"com.leting.car"
                ,"com.lizhi.smartlife.lzbk.car"
                ,"cn.cbct.seefmcar"
                ,"com.cocav.tiemu"
                ,"com.koudaistory.car.changan"
                ,"com.mampod.ergedd.car"
                ,"com.baidu.netdisk.car"
                ,"com.icoolme.car.weather"
                ,"cn.etouch.ecalendar.chancar"
                ,"com.eryanet.game"
                ,"com.mega.ailab"
                ,"com.innovation.db.safetyseat"
                ,"com.netease.newsappf"
                ,"com.douban.book.reader"
                ,"com.lenovo.browser.hd"
                ,"com.edog.car"
                ,"com.mega.exhibit"
                ,"com.smile.gifmaker"
                ,"com.sohu.newsclient"
                ,"com.tinnove.bubblepop"
                ,"com.netease.cloudmusic.iot"
                ,"com.kugou.android.auto"
                ,"com.ximalaya.ting.android.car"
                ,"fm.qingting.qtradio"
//                ,"com.changan.audiovisuallinkage.datawinter"
//                ,"com.changan.audiovisuallinkage.fire"
                ,"com.changan.lightshow"
                ,"com.changba.sd"
                ,"com.loostone.player"
                ,"com.funshion.video.mobile"
                ,"com.blue.horn"
                ,"com.tantrum.qyym"
                ,"com.tingyutech.moving.car"
                ,"com.cmsr.vehiclelife"
                ,"com.bilibili.bilithings"
                ,"com.tencent.karaoketv"
                ,"com.sohu.newsclient"
                ,"com.aha.autocar.vehicle"
                ,"com.bwuni.routeman.hmi"
                ,"com.cmhi.universalapp.car")
        }else{
            fruits = listOf(
                "com.tinnove.aispace"
                ,"com.autopai.smart.sound.effect"
                ,"com.tinnove.cloudcamera"
                //,"com.tinnove.fivechess"
                ,"com.youku.car"
                ,"com.wt.lightsound"
                ,"cn.cmvideo.car.play"
                ,"com.changba.sd"
                ,"com.tinnove.scenemode"
                ,"com.zkjd.lingdong"
                //,"com.tinnove.spacecraft"
                ,"com.tinnove.wecarnavi"
                ,"com.changan.appmarket"
                ,"com.wt.funbox"
                ,"com.wt.scene"
                ,"com.tinnove.customer"
                ,"com.wt.phonelink"
                //,"com.wt.sweepmine"
                ,"com.wtcl.filemanager"
                ,"com.tinnove.plughardware"
                ,"com.wt.maintenance"
                //,"com.tinnove.speedrun"
                ,"com.tinnove.bubblepop"
                ,"com.wt.gamecenter"
                ,"com.tinnove.gamezone"
                ,"com.wtcl.electronicdirections"
                ,"com.autopai.album"
                ,"com.netease.cloudmusic.iot"
                ,"com.autopai.car.dialer"
                ,"com.adayo.app.dvr"
                ,"com.car.supercarsoundcn"
                ,"com.wt.vehiclecenter"
                ,"com.loostone.player"
                ,"cn.kuwo.kwmusiccar"
                ,"com.youku.car"
                ,"cn.cmvideo.car.play"
                ,"com.sohu.automotive"
                ,"com.mgtv.auto"
                ,"com.ajmide.car.a"
                ,"com.ifeng.ecarxfm"
                ,"io.dushu.car"
                ,"com.migu.miguplay"
                ,"com.wt.drmario"
                ,"com.wt.tetris"
                ,"com.tinnove.speedrun"
                ,"com.wt.chinesechess"
                ,"com.ximalaya.ting.android.carkids"
                ,"com.chinamobile.mcloudcar"
                ,"com.baidu.naviauto"
                ,"com.migu.car.music"
                ,"com.leting.car"
                ,"com.lizhi.smartlife.lzbk.car"
                ,"cn.cbct.seefmcar"
                ,"com.cocav.tiemu"
                ,"com.koudaistory.car.changan"
                ,"com.mampod.ergedd.car"
                ,"com.baidu.netdisk.car"
                ,"com.icoolme.car.weather"
                ,"cn.etouch.ecalendar.chancar"
                ,"com.eryanet.game"
                ,"com.mega.ailab"
                ,"com.innovation.db.safetyseat"
                ,"com.netease.newsappf"
                ,"com.douban.book.reader"
                ,"com.lenovo.browser.hd"
                ,"com.edog.car"
                ,"com.mega.exhibit"
                ,"com.smile.gifmaker"
                ,"com.sohu.newsclient"
                //,"com.tinnove.bubblepop"
                ,"com.netease.cloudmusic.iot"
                ,"com.kugou.android.auto"
                ,"com.ximalaya.ting.android.car"
                ,"fm.qingting.qtradio"
                ,"com.changan.audiovisuallinkage.datawinter"
                ,"com.changan.audiovisuallinkage.fire"
                ,"com.changan.lightshow"
                ,"com.changba.sd"
                ,"com.loostone.player"
                ,"com.funshion.video.mobile"
                ,"com.blue.horn"
                ,"com.tantrum.qyym"
                ,"com.tingyutech.moving.car"
                ,"com.cmsr.vehiclelife"
                ,"com.bilibili.bilithings"
                ,"com.tencent.karaoketv"
                ,"com.sohu.newsclient"
                ,"com.aha.autocar.vehicle"
                ,"com.bwuni.routeman.hmi"
                ,"com.cmhi.universalapp.car"
                ,"com.tinnove.mediacenter"
            )
        }

        viewModelScope.launch {
            val serviceAppList = functionExecutor.getInstalledApps()
            // 将service.AppInfo转换为model.AppInfo
            _appList.value = serviceAppList
                .filter { fruits.contains(it.packageName) } // 过滤出白名单中的应用
                .distinctBy { it.packageName }
                .map { serviceApp ->
                    AppInfo(
                        packageName = serviceApp.packageName,
                        appName = serviceApp.appName,
                        iconResId = serviceApp.iconResId
                    )
                }
            Timber.d("已加载应用列表，共 ${_appList.value.size} 个应用")


            //Timber.d("已加载应用列表:"+_appList.value.toString())
        }
    }
    
}