package com.deepal.ivi.hmi.ipcommon.data.bean;

/**
 * @author fucheng
 * @date 2020/11/23
 * todo:用于存储小仪表上下电记忆
 */
public interface SetWrite {

     // 自动开关是否打开，1：打开，0：关闭
     String AUTO_SWITCH_STATUS = "AUTO_SWITCH_STATUS";

     // 用户上次设置的亮度值：20、40、60、80、100
     String SET_LIGHT_VALUE = "LIGHT_VALUE";

     // 用户上次设置的主题
     String SET_THEME = "THEME";

     //用户上次设置的显示模式
     String SHOW_MODE = "SHOW_MODE";
}
