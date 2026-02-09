package com.zkjd.lingdong.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Xml
import com.adayo.service.utils.FunctionConfigCheck
import com.mine.baselibrary.constants.CarPlatformConstants
import com.mine.baselibrary.constants.VehicleTypeConstants
import com.zkjd.lingdong.R
import com.zkjd.lingdong.model.ButtonFunction
import com.zkjd.lingdong.model.FunctionCategory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

/**
 * 功能配置类，负责从XML文件中加载功能配置
 */
class FunctionsConfig(private val context: Context) {
    
    // 存储所有的功能
    private val allFunctions = mutableListOf<ButtonFunction>()
    
    // 存储旋转功能
    private val rotaryFunctions = mutableListOf<ButtonFunction>()
    
    // 存储非旋转功能
    private val nonRotaryFunctions = mutableListOf<ButtonFunction>()


    // 车型功能
    private val carFunctions = mutableListOf<ButtonFunction>()

    //true 镁佳8155 false 梧桐8155
    private val isMegaSys: Boolean = VehicleTypeConstants.isMega

    //true  8295车机  false  8155车机
    private val is8295: Boolean= VehicleTypeConstants.isMega8295
    init {
        loadFunctionsFromXml()
    }
    
    /**
     * 从XML文件中加载功能配置
     */
    private fun loadFunctionsFromXml() {
        try {
            val inputStream =  context.resources.openRawResource(
               when{
                   VehicleTypeConstants.isMega8155 -> R.raw.functions_config8155
                   VehicleTypeConstants.isMega8295 -> R.raw.functions_config8295
                   else ->  R.raw.functions_config_tinnove
               }

            )
            val functions = parse(inputStream)
            
            allFunctions.clear()
            rotaryFunctions.clear()
            nonRotaryFunctions.clear()
            carFunctions.clear()
            
            allFunctions.addAll(functions)
            
            // 分类存储旋转和非旋转功能
            for (function in functions) {
                //111,splie出来是["",1,1,1,“”]
                val array=function.useType.toString().split("")
                if (array[1].toInt()==2) {
                    rotaryFunctions.add(function)
                } else if(array[1].toInt()==1) {
                    nonRotaryFunctions.add(function)
                } else if(array[1].toInt()==3)
                {
                    carFunctions.add(function)
                }
            }
            
            Timber.d("加载了 ${allFunctions.size} 个功能，其中旋转功能 ${rotaryFunctions.size} 个，非旋转功能 ${nonRotaryFunctions.size} 个")
        } catch (e: Exception) {
            Timber.e(e, "加载功能配置失败")
        }
    }
    
    /**
     * 获取所有功能
     */
    fun getAllFunctions(): List<ButtonFunction> = allFunctions
    
    /**
     * 获取旋转功能
     */
    fun getRotaryFunctions(carTypeName: String): List<ButtonFunction>
    {
        return getTrueValue(carTypeName,rotaryFunctions)
    }

    fun getIsMegaSys(): Boolean
    {
        return isMegaSys
    }

    fun getIs8295(): Boolean
    {
        return is8295
    }
    /**
     * 获取非旋转功能
     */
    fun getNonRotaryFunctions(carTypeName: String): List<ButtonFunction>
    {
        return getTrueValue(carTypeName,nonRotaryFunctions)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun getTrueValue(carTypeName: String, nowFunctionsTrue: List<ButtonFunction>): List<ButtonFunction> {
        val newFunctionsTrue = mutableListOf<ButtonFunction>()

        if (!isMegaSys) {//梧桐车型8678

                // 分类车型
                for (function in nowFunctionsTrue) {
                    val array = function.useType.toString().split("")

                    if (array[2].toInt() == 2 && function.id == "trunk_position") {//选配

                        //array2[0] 是梧桐的配置字值
                        val array2 = function.configWords.split("+")

                        /*val intString = FunctionConfigCheck.getIFunctionConfigCheck(context)
                            .hasFunction(array2[0])*/
                        val intString = FunctionConfigCheck.getIFunctionConfigCheck(context).hasTailWings()
                        if (intString) {
                            newFunctionsTrue.add(function)
                        }
                        //服务已加载完成，可以读取配置
                       /* val typeInt = OfflineConfig.getInstance().getValue(array2[0])
                        if(typeInt>0){
                            newFunctionsTrue.add(function)
                        }*/

                    } else if (array[2].toInt() == 1) {//标配
                        newFunctionsTrue.add(function)
                    }
                }

        } else {
            for (function in nowFunctionsTrue) {
                val array = function.useType.toString().split("")

                if (array[3].toInt() == 2) {//需要根据配置字

                    val array2 = function.configWords.toString().split("+")

                    val intString = FunctionConfigCheck.getIFunctionConfigCheck(context).hasFunction(array2[0])
                       // DriveModelManager.getModels(DriveModelManager.getSettingString(array2[1]))
                    if (intString ) {
                        newFunctionsTrue.add(function)
                    }

                } else if (array[3].toInt() == 1) {//标准配置
                    newFunctionsTrue.add(function)
                }//array[3].toInt() == 0 表示没有这个功能

            }
        }

        return newFunctionsTrue
    }
    /**
     * 获取车型功能
     */
    fun getCarFunctions(): List<ButtonFunction> = carFunctions
    
    /**
     * 获取指定ID的功能
     */
    fun getFunctionById(id: String): ButtonFunction? = allFunctions.find { it.id == id }
    
    /**
     * 获取指定类别的功能
     */
    fun getFunctionsByCategory(category: FunctionCategory): List<ButtonFunction> =
        allFunctions.filter { it.category == category }
    
    /**
     * 解析XML文件
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parse(inputStream: InputStream): List<ButtonFunction> {
        inputStream.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readFunctions(parser)
        }
    }
    
    /**
     * 读取functions标签
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFunctions(parser: XmlPullParser): List<ButtonFunction> {
        val functions = mutableListOf<ButtonFunction>()
        
        parser.require(XmlPullParser.START_TAG, null, "functions")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            
            if (parser.name == "function") {
                val function = readFunction(parser)
                function?.let { functions.add(it) }
            } else {
                skip(parser)
            }
        }
        
        return functions
    }
    
    /**
     * 读取function标签
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFunction(parser: XmlPullParser): ButtonFunction? {
        parser.require(XmlPullParser.START_TAG, null, "function")
        
        var id: String? = null
        var name: String? = null
        var icon: String? = null
        var icon_selected: String? = null
        var usType: Int= 1
        var category: FunctionCategory? = null
        var actionCode: String? = null
        var configWord: String? = null
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            
            when (parser.name) {
                "id" -> id = readText(parser)
                "name" -> name = readText(parser)
                "icon" -> icon = readText(parser)
                "icon_selected" -> icon_selected = readText(parser)
                "ustype" -> usType = readText(parser).toInt()
                "category" -> {
                    try {
                        category = FunctionCategory.valueOf(readText(parser))
                    } catch (e: IllegalArgumentException) {
                        Timber.e(e, "无效的功能类别")
                    }
                }
                "action_code" -> actionCode = readText(parser)
                "config_word" -> configWord=readText(parser)
                else -> skip(parser)
            }
        }
        
        if (id != null && name != null && category != null && actionCode != null && configWord !=null) {
            // 获取图标资源ID
            val iconResId = if (icon != null) {
                context.resources.getIdentifier(
                    icon, "drawable", context.packageName
                )
            } else {
                0
            }
            val iconSelectedResId = if (!icon_selected.isNullOrBlank()) {
                context.resources.getIdentifier(
                    icon_selected, "drawable", context.packageName
                )
            } else {
                0
            }
            
            return ButtonFunction(
                id = id,
                name = name,
                category = category,
                actionCode = actionCode,
                iconResId = iconResId,
                iconSelectedResId = iconSelectedResId,
                configWords= configWord,
                useType = usType

            )
        }
        
        return null
    }
    
    /**
     * 读取标签内的文本
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
    
    /**
     * 跳过不需要处理的标签
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: FunctionsConfig? = null
        
        fun getInstance(context: Context): FunctionsConfig {
            return INSTANCE ?: synchronized(this) {
                val instance = FunctionsConfig(context)
                INSTANCE = instance
                instance
            }
        }
    }
} 