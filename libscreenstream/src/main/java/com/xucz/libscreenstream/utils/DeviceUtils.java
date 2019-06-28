package com.xucz.libscreenstream.utils;

import android.os.Build;
import android.text.TextUtils;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public final class DeviceUtils {
    private static String mBrand = "";
    private static String mModel = "";

    public DeviceUtils() {
    }

    public static String getMobileModel() {
        return getBuildBrand() + getDeviceModel();
    }

    public static String getBuildBrand() {
        if (!TextUtils.isEmpty(mBrand)) {
            return mBrand;
        } else {
            String result = "";

            try {
                result = Build.BRAND;
            } catch (Exception var2) {
                var2.printStackTrace();
            }

            result = handleIllegalCharacterInResult(result);
            mBrand = result;
            return result;
        }
    }

    public static String getDeviceModel() {
        if (!TextUtils.isEmpty(mModel)) {
            return mModel;
        } else {
            String result = "";

            try {
                result = Build.MODEL;
            } catch (Exception var2) {
                var2.printStackTrace();
            }

            result = handleIllegalCharacterInResult(result);
            mModel = result;
            return result;
        }
    }

    private static String handleIllegalCharacterInResult(String result) {
        if (result.indexOf(" ") > 0) {
            result = result.replaceAll(" ", "_");
        }

        return result;
    }
}
