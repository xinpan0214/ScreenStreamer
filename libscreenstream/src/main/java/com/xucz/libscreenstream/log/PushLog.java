package com.xucz.libscreenstream.log;

import android.util.Log;

import java.util.Locale;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public class PushLog {
    private static final String TAG = "screenstream";

    public static void d(String format, String... args) {
        Log.d(TAG, String.format(Locale.US, format, args));
    }

    public static void e(String format, String... args) {
        Log.e(TAG, String.format(Locale.US, format, args));
    }

    public static void i(String format, String... args) {
        Log.i(TAG, String.format(Locale.US, format, args));
    }
}
