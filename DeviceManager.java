package com.septem.a5dmarkv;

import android.content.Context;
import android.provider.Settings;

/**
 * 设备一些状态的控制，比如显示屏亮度，声音大小等
 * Created by septem on 2016/9/7.
 */
public class DeviceManager {

    /**
     * 关于屏幕亮度的设置
     */
    public static class BrightnessTool {

        public static final int BRIGHTNESS_MODE_MANUAL = 0;
        public static final int BRIGHTNESS_MODE_AUTO = 1;



        /**
         * 获得系统亮度 0-255
         * @return 系统亮度 0-255
         */
        public static int getScreenBrightness(Context context) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    65536);
        }

        /**
         * 获得亮度模式
         * @return 系统亮度模式
         */
        public static int getScreenBrightnessMode(Context context) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    65536);
        }

        /**
         * 设置成自动亮度调节模式
         */
        public static void setAutoBrightness(Context context) {
             Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }

        /**
         * 设置成手动亮度调节模式
         */
        public static void setManualBrightness(Context context) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        /**
         * 设置屏幕亮度，0-255，如果数值大于255，就设置成255；如果小于0，就设置成0
         * @param brightness 希望设定的亮度数值 0-255
         */
        public static void setBrightness(Context context, int brightness) {
            if(brightness >= 255)
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        255);
            else if(brightness <= 0)
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        0);
            else Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        brightness);
        }
    }
}
