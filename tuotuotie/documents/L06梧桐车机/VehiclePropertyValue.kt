/**
 * VehiclePropertyValue 接口
 * 用于定义车辆属性的状态值
 */
public final class WuTong {
    
    /**
     * VehiclePropertyValue : int32_t
     */
    public static class VehiclePropertyValue {
        /**
         * 关闭
         */
        public static final int OFF = 0x01;
        
        /**
         * 开启
         */
        public static final int ON = 0x02;
        
        /**
         * 状态异常
         */
        public static final int ERROR = 0x03;
        
        /**
         * 可用，可操作状态等等
         */
        public static final int ACTIVE = 0x04;
        
        /**
         * 不可用，不可操作状态，置灰等等
         */
        public static final int INACTIVE = 0x05;
        
        /**
         * 获取状态
         */
        public static final int STATUS = 0x06;
    }
}
