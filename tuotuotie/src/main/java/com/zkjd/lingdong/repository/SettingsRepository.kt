package com.zkjd.lingdong.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 应用设置仓库
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val USE_MEIJIA_CAR_KEY = booleanPreferencesKey("use_meijia_car")
        private val USE_CAR_AUDIO_KEY = booleanPreferencesKey("use_car_audio")
        private val ENABLE_AUTHENTICATION_KEY = booleanPreferencesKey("enable_authentication")
        private val INITIAL_SETTINGS = booleanPreferencesKey("initial_setup")
        private val USE_CAR_TYPE_NAME = stringPreferencesKey("use_car_type_name")
        private val USE_MUSIC_TYPE_NAME = stringPreferencesKey("use_music_type_name")
        private val MUSIC_SETTINGS = booleanPreferencesKey("music_setup")
        private val APP_OPENONE = booleanPreferencesKey("app_openone")
        private val APP_OPENTWO = booleanPreferencesKey("app_opentwo")

        //用于存储梧桐配置字
        private val TINNOVE_CAR_CONFIG = stringPreferencesKey("tinnove_car_config")
    }

    /**
     * 获取选择的车辆类型名称
     */
    val useCarTypeName: Flow<String> = context.dataStore.data
        .map {
            val value=it[USE_CAR_TYPE_NAME] ?: "s05"
            Timber.w("读取DataStore: $value")
            value
        }


    /**
     * 获取选择的车辆类型名称
     */
    val useMusicTypeName: Flow<String> = context.dataStore.data
        .map {
            val value=it[USE_MUSIC_TYPE_NAME] ?: "1"
            Timber.w("读取DataStore: $value")
            value
        }


    /**
     * 单击app是否打开
     */
    val appOpenOne: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APP_OPENONE] ?: false
        }

    /**
     * 单击app是否打开
     */
    val appOpenTwo: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APP_OPENTWO] ?: false
        }


    /**
     * 是否使用镁佳Car执行器
     */
    val useMeijiaCar: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_MEIJIA_CAR_KEY] ?: false
        }

    /**
     * 是否使用音效
     */
    val useMusic: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[MUSIC_SETTINGS] ?: true
        }

    /**
     * 是否使用车机音频
     */
    val useCarAudio: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_CAR_AUDIO_KEY] ?: true
        }
        
    /**
     * 是否启用设备鉴权
     */
    val enableAuthentication: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ENABLE_AUTHENTICATION_KEY] ?: true
        }

    /**
     * 是否首次设置
     */
    val initialSetup: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[INITIAL_SETTINGS] ?: false
        }


    /**
     * 设置选择音效
     */
    suspend fun setUseMusicTypeName(useMusicTypeName: String) {
        context.dataStore.edit { preferences ->
            preferences[USE_MUSIC_TYPE_NAME] = useMusicTypeName
        }
    }

    /**
     * 设置选择车辆类型
     */
    suspend fun setUseCarTypeName(useCarTypeName: String) {
        context.dataStore.edit { preferences ->
            preferences[USE_CAR_TYPE_NAME] = useCarTypeName
        }
    }

    /**
     * 设置是否使用镁佳Car执行器
     */
    suspend fun setUseMeijiaCar(useMeijiaCar: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_MEIJIA_CAR_KEY] = useMeijiaCar
        }
    }

    /**
     * 设置appOpen1
     */
    suspend fun setAppOpen1(appOpen1: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_OPENONE] = appOpen1
        }
    }

    /**
     * 设置是否使用音效
     */
    suspend fun setAppOpen2(appOpen2: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_OPENTWO] = appOpen2
        }
    }


    /**
     * 设置是否使用音效
     */
    suspend fun setUseMusic(useMusic: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MUSIC_SETTINGS] = useMusic
        }
    }

    /**
     * 设置是否使用车机音频
     */
    suspend fun setUseCarAudio(useCarAudio: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_CAR_AUDIO_KEY] = useCarAudio
        }
    }
    
    /**
     * 设置是否启用设备鉴权
     */
    suspend fun setEnableAuthentication(enableAuthentication: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_AUTHENTICATION_KEY] = enableAuthentication
        }
    }

    /**
     * 设置是否首次设置
     */
    suspend fun setInitialSetup(enableAuthentication: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INITIAL_SETTINGS] = enableAuthentication
        }
    }

    suspend fun setTinnoveCarConfig(tinnoveCarConfig: String) {
        context.dataStore.edit { preferences ->
            preferences[TINNOVE_CAR_CONFIG] = tinnoveCarConfig
        }
    }

    /**
     * 获取梧桐配置字
     */
    val tinnoveCarConfig: Flow<String> = context.dataStore.data
        .map { preferences ->
            val value = preferences[TINNOVE_CAR_CONFIG] ?: ""
            Timber.w("读取DataStore梧桐配置: $value")
            value
        }

    /**
     * 获取梧桐配置字（一次性获取）
     */
    suspend fun getTinnoveCarConfig(): String {
        return context.dataStore.data.first()[TINNOVE_CAR_CONFIG] ?: ""
    }
} 