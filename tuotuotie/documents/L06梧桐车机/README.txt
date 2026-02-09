接口查找流程：
1.通过需求里对应的can信号名称
2.到接口《MCU-CPU通信协议》，找到对应的key和对应的manager
3.DDS相关参考《CarService DDS协议接口表-xxx.xlsx》
4.到CarApi文档里找到对应key在该manger里的定义
5.接口使用参考《Carlib-使用说明》


CarApi_ALL 包含所有key和定义
CarApi_simple 包含所有key，缺少部分定义

使用方法：直接点击index.html，可通过浏览器查看所有api
所有的id都在hardware包下面
其中，CarMixManager，CarSensorManager，CarVendorExtensionManager在hardware根目录下
其他manager都在hardware.--对应的包名下，比如hardware.mcu下就是CarMcuManager