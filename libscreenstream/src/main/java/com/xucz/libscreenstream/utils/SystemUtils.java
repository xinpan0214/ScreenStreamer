package com.xucz.libscreenstream.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.provider.MediaStore.Images;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * <p>Created by 李文龙(LeonLee) on 14-10-9.</p>
 * <p/>
 * 常用系统数据或处理工具类
 * <p/>
 * <ul>
 * <li>{@link #getMemoryClass(Context)} 获取当前系统每个app的内存等级，即最大使用内存</li>
 * <li>{@link #myPid()} 获取进程ID</li>
 * <li>{@link #getCurProcessName(Context)} 获取进程名</li>
 * <li>{@link #getSysModel()} 获取系统型号</li>
 * <li>{@link #getSysVersion()} 获取系统版本号</li>
 * <li>{@link #getVersionCode(Context)} 获取当前系统版本号</li>
 * <li>{@link #getVersionName(Context)} 获取当前系统版本名</li>
 * <li>{@link #isCurAppTop(Context)} 判断当前程序是否前台进程</li>
 * </ul>
 */
public class SystemUtils {

    private static int displayWidth = 0;
    private static int displayHeight = 0;
    private static int screenHeight = 0;
    private static int screenWidth = 0;
    private static int statusBarHeight = 0;

    private SystemUtils() {
    }

    /**
     * 获取当前系统每个app的内存等级，即最大使用内存
     *
     * @param context
     * @return
     */
    public static int getMemoryClass(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.getMemoryClass();
    }

    /**
     * 获取本地显示的语言，用于国际化
     */
    public static String getLocalLanguage(Context context) {
        if (context == null) {
            return null;
        }
        Locale locale = context.getResources().getConfiguration().locale;
        final String language = locale.getLanguage();
        final boolean isEmpty = TextUtils.isEmpty(language);
        return isEmpty ? "" : language.toLowerCase();
    }

    /**
     * 是否为RTL语言：阿拉伯语ar、希伯来语iw、乌干达ug、、波斯语fa、乌尔都语（印度、巴基斯坦ur）、维吾尔语
     */
    public static boolean isRTLLanguage() {
        final boolean isRTL = BidiFormatter.getInstance().isRtlContext();
        return isRtlMode(null) || isRTL;
    }

    /**
     * 当前系统的设置是否是RTL格式, 主要用于开发者模式
     *
     * @param context
     * @return
     */
    private static boolean isRtlMode(Context context) {
        if (context == null) {
            return false;
        }
        Configuration config = context.getResources().getConfiguration();
        return config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    /**
     * RTL语言时反转view
     *
     * @param view
     */
    public static void resolveRTL(View view) {
        if (view == null) {
            return;
        }
        if (isRTLLanguage()) {
            view.setRotationY(180f);
        } else {
            view.setRotationY(0f);
        }
    }

//    /**
//     * @return 手机电池温度，ACTION_BATTERY_CHANGED是粘性广播
//     */
//    public static int getTemperature() {
//        int temp = 0;
//        try {
//            if (ApplicationManager.getAppContext() != null) {
//                Intent tempIntent = ApplicationManager.getAppContext().registerReceiver(null, new IntentFilter(
//                        Intent.ACTION_BATTERY_CHANGED));
//                if (tempIntent != null) {
//                    temp = tempIntent.getIntExtra("temperature", 0) / 10;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return temp;
//    }


    /**
     * 开发，设置RTL布局
     */
    public static void setRTLMode(Context context, boolean isRTL) {
        Locale locale = Locale.getDefault();
        if (isRTL && !isRTLLanguage()) {
            locale = new Locale("ar");
        }
        setLocale(context, locale);
    }

    /**
     * 更改Locale,语言
     */
    public static void setLocale(Context context, Locale locale) {
        if (context == null || locale == null) {
            return;
        }
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        //if (Build.VERSION.SDK_INT >= 17) {
        config.setLayoutDirection(locale);
        //}
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    /**
     * 格式化阿拉伯数字
     *
     * @param content
     * @return
     */
    public static String formatArabicNumber(String content) {
        if (content == null) {
            return null;
        }

        content = content.replace("٠", "0");
        content = content.replace("١", "1");
        content = content.replace("٢", "2");
        content = content.replace("٣", "3");
        content = content.replace("٤", "4");
        content = content.replace("٥", "5");
        content = content.replace("٦", "6");
        content = content.replace("٧", "7");
        content = content.replace("٨", "8");
        content = content.replace("٩", "9");
        return content;
    }


    /**
     * 获取进程ID
     *
     * @return
     */
    public static int myPid() {
        return android.os.Process.myPid();
    }

    /**
     * 获取进程名
     *
     * @param context
     * @return
     */
    public static String getCurProcessName(Context context) {
        if (context != null) {
            ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (mActivityManager == null) {
                return "";
            }
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = mActivityManager.getRunningAppProcesses();
            if (runningAppProcesses == null || runningAppProcesses.size() == 0) {
                return "";
            }
            for (ActivityManager.RunningAppProcessInfo appProcess : runningAppProcesses) {
                if (appProcess.pid == myPid()) {
                    return appProcess.processName;
                }
            }
        }
        return "";
    }

    /**
     * 获取系统型号
     *
     * @return
     */
    public static String getSysModel() {
        return Build.MODEL;
    }

    /**
     * 获取系统版本号
     *
     * @return
     */
    public static String getSysVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * 获取当前系统版本号
     *
     * @return
     */
    public static int getVersionCode(Context context) {
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 获取当前系统版本名
     *
     * @return
     */
    public static String getVersionName(Context context) {
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取屏幕密度
     *
     * @param activity
     * @return
     */
    public static int getDpi(Context activity) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = activity.getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi; // 屏幕密度DPI（120 / 160 / 240）
        return densityDpi;
    }

    public static float getScale(Context context) {
        float scale = context.getResources().getDisplayMetrics().density;
        return scale;
    }

    /**
     * 获取系统相册路径, 耗时操作
     *
     * @param context
     * @return
     */
    public static String getAlbumPath(Context context) {
        if (context == null) {
            return null;
        }
        try {
            ContentResolver cr = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(Images.Media.TITLE, "title");
            values.put(Images.Media.DESCRIPTION, "description");
            values.put(Images.Media.MIME_TYPE, "image/jpeg");
            Uri url = cr.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
            // 查询系统相册数据
            Cursor cursor = Images.Media.query(cr, url, new String[]{Images.Media.DATA});
            String albumPath = null;
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(Images.Media.DATA);
                albumPath = cursor.getString(column_index);
                try {
                    cursor.close();
                } catch (Exception e) {
                }
            }
            cr.delete(url, null, null);
            if (TextUtils.isEmpty(albumPath)) {
                return null;
            }

            albumPath = FileUtils.getParentPath(albumPath);
            File albumDir = new File(albumPath);
            if (!albumDir.exists()) {
                albumDir.mkdirs();
            }
            return albumPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断Intent是否有效
     *
     * @param context
     * @param intent
     * @return true 有效
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * 检查权限
     *
     * @param context    上下文
     * @param permission 需要检查的权限
     */
    public static boolean checkPermissions(Context context, String permission) {
        PackageManager localPackageManager = context.getPackageManager();
        int flag = localPackageManager.checkPermission(permission, context.getPackageName());
        return flag == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 获取屏幕宽
     *
     * @param context
     * @return
     */
    @SuppressWarnings("deprecation")
    public static int getDisplayWidth(Context context) {
        if (displayWidth > 0) {
            return displayWidth;
        }

        if (context == null) {
            return 0;
        }
        //WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = getWindowManager(context).getDefaultDisplay();
        try {
            Class<?> cls = Display.class;
            Class<?>[] parameterTypes = {Point.class};
            Point parameter = new Point();
            Method method = cls.getMethod("getSize", parameterTypes);
            method.invoke(display, parameter);
            displayWidth = parameter.x;
        } catch (Exception e) {
            displayWidth = display.getWidth();
        }
        return displayWidth;
    }

    /**
     * 获取屏幕高
     *
     * @param context
     * @return
     */
    @SuppressWarnings("deprecation")
    public static int getDisplayHeight(Context context) {
        if (displayHeight > 0) {
            return displayHeight;
        }

        if (context == null) {
            return 0;
        }
        //WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = getWindowManager(context).getDefaultDisplay();
        try {
            Class<?> cls = Display.class;
            Class<?>[] parameterTypes = {Point.class};
            Point parameter = new Point();
            Method method = cls.getMethod("getSize", parameterTypes);
            method.invoke(display, parameter);
            displayHeight = parameter.y;
        } catch (Exception e) {
            displayHeight = display.getHeight();
        }
        return displayHeight;
    }

    /**
     * 获取屏幕高(包括底部虚拟按键)
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        if (screenHeight > 0) {
            return screenHeight;
        }

        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        //WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = getWindowManager(context).getDefaultDisplay();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(displayMetrics);
            } else {
                display.getMetrics(displayMetrics);
            }
            screenHeight = displayMetrics.heightPixels;
        } catch (Exception e) {
            screenHeight = display.getHeight();
        }
        return screenHeight;
    }

    /**
     * 获取屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        if (screenWidth > 0) {
            return screenWidth;
        }

        if (context == null) {
            return 0;
        }
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        return screenWidth;
    }

    /**
     * 获取屏幕高度,是否包含导航栏高度
     */
    public static int getScreenHeight(Context context, boolean isIncludeNav) {
        if (context == null) {
            return 0;
        }
        int screenHeight = getScreenHeight(context);
        if (isIncludeNav) {
            return screenHeight;
        } else {
            return screenHeight - getNavigationBarHeight(context);
        }
    }

    /**
     * 获取NavigationBar的高度，当不具备时直接返回0
     */
    public static int getNavigationBarHeight(Context context) {
        if (!hasNavigationBar(context)) {
            return 0;
        }
        return getRawNavigationBarHeight(context);
    }

    /**
     * 获取NavigationBar的高度
     * 不管NavigationBar显示与否,也不一定是虚拟导航栏的(可能是硬件导航栏)，都会获得高度
     *
     * @param context
     * @return
     */
    public static int getRawNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height",
                "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }

    /**
     * 是否有虚拟按键(排除掉硬件导航栏)
     *
     * @return
     */
    public static boolean hasVirtualNavigationBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {

        }
        return hasNavigationBar;
    }

    /**
     * 是否显示NavigationBar
     * 此方法在高版本系统(具有全面屏)的机型一直返回的都是true，所以不适用于这些机型
     * <p>
     * 新增小米全面屏机型虚拟导航栏判断
     */
    public static boolean hasNavigationBar(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //如果是小米并且开启了手势模式，手势模式不显示虚拟键
            if (RomUtils.isMiui()) {
                if (Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) != 0) {
                    return false;
                }
            }
            Display display = getWindowManager(context).getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);
            if (realSize.x == size.x && realSize.y == size.y) {
                return false;
            }
            return !(realSize.x * realSize.y == size.x * size.y);
        } else {
            boolean menu = ViewConfiguration.get(context).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            return !(menu || back);
        }
    }

    /**
     * 获取系统状态栏bar条高
     *
     * @param activity
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    public static int getStatusBarHeight(Activity activity) {
        if (statusBarHeight > 0) {
            return statusBarHeight;
        }

        if (activity == null) {
            return 0;
        }
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        statusBarHeight = frame.top;

        if (0 == statusBarHeight) {
            try {
                Resources resources = activity.getResources();
                int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = resources.getDimensionPixelSize(resourceId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (0 == statusBarHeight) {
            statusBarHeight = getStatusBarHeightByReflect(activity);
        }

        if (0 == statusBarHeight) {
            int systemStatusBarHeight = 25;//25dp
            if (getBuildVersionSDK() >= 23) {
                systemStatusBarHeight = 24;//24dp
            }
            //如果获取不到，返回设计规范的值25dp
            statusBarHeight = dip2px(activity, systemStatusBarHeight);
        }
        return statusBarHeight;
    }

    public static int getStatusBarHeightByReflect(Context context) {
        //int sbHeight;
        if (statusBarHeight > 0) {
            return statusBarHeight;
        }
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int sbHeightId = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(sbHeightId);
        } catch (Exception e1) {
            e1.printStackTrace();
            statusBarHeight = 0;
        }
        return statusBarHeight;
    }

    public static int getBuildVersionSDK() {
        int result = 0;
        try {
            result = Build.VERSION.SDK_INT;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 键盘是否显示
     * 兼容横竖屏和虚拟导航栏机型
     *
     * @return
     */
    public static boolean isKeyboardShowing(Activity activity) {
        if (activity == null) {
            return false;
        }
        boolean isPortrait = false;
        if (activity.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            isPortrait = true;
        }
        final Window window = activity.getWindow();
        final View decorView = window.getDecorView();

        //获取当前屏幕内容的高度(兼容横竖屏)
        int screenHeight = 0;
        int height = decorView.getHeight();
        int width = decorView.getWidth();
        if (isPortrait) {
            screenHeight = (height > width) ? height : width;
        } else {
            screenHeight = (height > width) ? width : height;
        }

        //获取View可见区域的bottom
        Rect rect = new Rect();
        decorView.getWindowVisibleDisplayFrame(rect);
        int difference = screenHeight - rect.bottom;

        //高度差大于屏幕的四分之一高度即为软键盘弹出
        boolean isKeyboardVisible = difference > (screenHeight / 4);
        return isKeyboardVisible;

    }


    /**
     * 是否进行沉浸式处理
     *
     * @return
     */
    public static boolean isSupportTransluent() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }


    /**
     * 获取WindowManager。
     */
    public static WindowManager getWindowManager(Context context) {
        if (context == null) {
            return null;
        }
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * 获取屏幕高，包含状态栏，但不包含虚拟按键，如1920屏幕只有1794
     */
    public static int getScreenHeight2(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager(context).getDefaultDisplay().getMetrics(metric);
        return metric.heightPixels;
    }

    /**
     * 获取屏幕原始尺寸高度，包括状态栏以及虚拟功能键高度
     */
    public static int getAllScreenHeight(Context context) {
        Display display = getWindowManager(context).getDefaultDisplay();
        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            Method method = Class.forName("android.view.Display").getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, displayMetrics);
            return displayMetrics.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取 虚拟按键的高度
     */
    public static int getBottomBarHeight(Context context) {
        return getAllScreenHeight(context) - getScreenHeight2(context);
    }

    /**
     * dp转成px
     *
     * @param context
     * @param dipValue
     * @return
     */
    public static int dip2px(Context context, float dipValue) {
        if (context != null) {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dipValue * scale + 0.5f);
        }
        return 0;
    }

    public static float sp2px(Context context, float spValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.getResources().getDisplayMetrics());
    }

    /**
     * px转成dp
     *
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 显示软键盘
     *
     * @param context
     * @param edit
     */
    public static void showSoftInput(Context context, View edit) {
        if (context == null || edit == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null && inputManager.isActive(edit)) {
            inputManager.showSoftInput(edit, 0);
            //EventBus.getDefault().post(new EventWrapper(EventBusMessageId.MSG_LIVEROOM_KEYBOARD_SHOWN_CHANGED,true));
        }
    }

    /**
     * 隐藏软键盘
     */
    public static void hideSoftInput(Window window) {
        try {
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } catch (Exception e) {
        }
    }

    /**
     * 隐藏软键盘
     */
    public static void hideSoftInput(Activity activity) {
        if (activity == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null && inputManager.isActive()) {
            View focusView = activity.getCurrentFocus();
            if (focusView != null) {
                inputManager.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
            }
        }
    }

    /**
     * 隐藏软键盘
     *
     * @param context
     * @param edit
     */
    public static void hideSoftInput(Context context, View edit) {
        try {
            InputMethodManager inputManager = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(edit.getWindowToken(), 0);
                //EventBus.getDefault().post(new EventWrapper(EventBusMessageId.MSG_LIVEROOM_KEYBOARD_SHOWN_CHANGED,false));
            }
        } catch (Exception e) {
        }
    }


    /**
     * 读取RAW文件内容
     *
     * @param context
     * @param resid
     * @param encoding
     * @return
     */
    public static String getRawFileContent(Context context, int resid, String encoding) {
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(resid);
        } catch (Exception e) {
            // Resource NotFoundException
        }
        if (is != null) {
            ByteArrayBuffer bab = new ByteArrayBuffer(1024);
            int read;
            try {
                while ((read = is.read()) != -1) {
                    bab.append(read);
                }
                return EncodingUtils.getString(bab.toByteArray(), encoding);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return "";
    }

    /**
     * 检测是否支持OpenGL ES 2.0
     *
     * @param context
     * @return
     */
    public static boolean isSupportOpenGLES2(Context context) {
        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
        return supportsEs2;
    }

    /**
     * 程序是否在前台运行
     *
     * @return boolean
     */
    public static boolean isAppOnForeground(Context context) {
        // Returns a list of application processes that are running on the device
        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = context.getApplicationContext().getPackageName();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    public static void checkAppInstalled(final Context context, final String packageName,
                                         final CheckAppInstalledCallback callback) {
        AsyncTask<Integer, Integer, Boolean> task = new AsyncTask<Integer, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Integer... integers) {
                return isAppInstalled(context, packageName);
            }

            @Override
            protected void onPostExecute(Boolean isAppInstalled) {
                if (callback != null) {
                    callback.checkResult(isAppInstalled);
                }
            }
        };
        task.execute();
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
            if (packageInfos == null || packageInfos.size() == 0) {
                return false;
            }
            for (int i = 0; i < packageInfos.size(); i++) {
                String pn = packageInfos.get(i).packageName;
                if (pn.equals(packageName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getDensity(int i) {
        return (int) (((float) i) * Resources.getSystem().getDisplayMetrics().density);
    }

    public static void openBrowser(Context context, String url) {
        Uri uri = Uri.parse(url);
        openBrowser(context, uri);
    }

    public static void openBrowser(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            // 先尝试用google play打开
            intent.setPackage("com.android.vending");
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                // 再尝试用chrome打开
                intent.setPackage("com.android.chrome");
                context.startActivity(intent);
            } catch (Exception ee) {
                // 调用系统支持的其他程序打开
                intent.setPackage(null);
                context.startActivity(intent);
            }
        }
    }

    public static int getScreenDensity(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            return metrics.densityDpi;
        }
        return 0;
    }

    public static void copyTextToClipboard(Context mContext, String text) {
        if (mContext == null || TextUtils.isEmpty(text)) {
            return;
        }
        ClipboardManager cmb = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(text);
    }

    public interface CheckAppInstalledCallback {
        void checkResult(boolean isAppInstalled);
    }

    /**
     * 判断当前程序是否前台进程
     */
    public static boolean isCurAppTop(Context context) {
        if (context == null) {
            return false;
        }
        String curPackageName = context.getPackageName();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ActivityManager.RunningTaskInfo info = list.get(0);
            String topPackageName = info.topActivity.getPackageName();
            String basePackageName = info.baseActivity.getPackageName();
            if (topPackageName.equals(curPackageName) && basePackageName.equals(curPackageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取栈顶activity名字
     */
    public static String getTopActivityName(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Service.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return null;
        }
        List<ActivityManager.RunningTaskInfo> runningTaskInfoList = activityManager.getRunningTasks(Integer.MAX_VALUE);
        if (runningTaskInfoList == null || runningTaskInfoList.isEmpty()) {
            return null;
        }
        for (ActivityManager.RunningTaskInfo taskInfo : runningTaskInfoList) {
            ComponentName topActivity = taskInfo.topActivity;
            String packageName = context.getPackageName();
            if (packageName.equals(topActivity.getPackageName())) {
                return topActivity.getClassName();
            }
        }
        return null;
    }

    public static boolean isAutoRotationEnable(Context context) {
        int state = getAutoRotationState(context);
        return state == 1;
    }

    private static int getAutoRotationState(Context context) {
        int sensorState = 0;
        try {
            sensorState = Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            return sensorState;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sensorState;
    }

    /**
     * 屏幕是否点亮状态：true，此刻手机屏幕是亮的
     */
    public static boolean isScreenOn(Context context) {
        boolean isScreenOn = false;
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            isScreenOn = powerManager.isScreenOn();
        }
        //Logger.d("dq-fw,isScreenOn=" + isScreenOn);
        return isScreenOn;
    }

    /**
     * 手机是否锁住状态：true，此刻手机需要解锁，false未解锁
     */
    public static boolean isPhoneLocked(Context context) {
        boolean isPhoneLocked = false;
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (mKeyguardManager != null) {
            isPhoneLocked = mKeyguardManager.inKeyguardRestrictedInputMode();
        }
        //Logger.d("dq-fw,isPhoneLocked=" + isPhoneLocked);
        return isPhoneLocked;
    }
}