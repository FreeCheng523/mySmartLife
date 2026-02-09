package com.deepal.ivi.hmi.smartlife.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShellUtils {
   private static final String TAG = "ShellUtils";
    public static String exec(String[] cmd) {
        // 输出输入命令日志
        Log.i(TAG, "执行命令 Execute command: " + String.join(" ", cmd));

        StringBuilder result = new StringBuilder();
        Process process = null;

        try {
            process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            BufferedReader reader =
                    new BufferedReader(new  InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
                Log.d(TAG, "Output: " + line);
            }

            process.waitFor();

        } catch (Exception e) {
            Log.e(TAG, "Error:", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return result.toString();
    }
}
