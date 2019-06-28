package com.xucz.libscreenstream.record_helper;

import android.content.Context;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Description:监听屏幕方向
 *
 * @author 杜乾, Created on 2018/5/21 - 16:42.
 * E-mail:duqian2010@gmail.com
 */
public class OrientationHelper {

    private Context mContext;
    private MyOrientationEventListener mOrientationEventListener;
    private int mOrientation = 0;//屏幕方向

    public OrientationHelper(Context context, RotationCallBack callback) {
        this.mContext = context;
        this.mCallback = callback;
        initListener();
    }

    private void initListener() {
        mOrientationEventListener = new MyOrientationEventListener(mContext);
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    private class MyOrientationEventListener extends OrientationEventListener {

        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                return;
            }

            //保证只返回四个方向
            int newOrientation = ((orientation + 45) / 90 * 90) % 360;
            if (newOrientation != mOrientation) {
                mOrientation = newOrientation;
                //返回的mOrientation就是手机方向，为0°、90°、180°和270°中的一个
                updateOrientation();
            }
        }
    }

    private void updateOrientation() {
        int pendingRotation = -1;
        pendingRotation = getWindowManagerRotation(mContext);
        //Logger.d(mOrientation+",dq window rotation =" + windowRotation+"，pendingRotation="+pendingRotation);

        if (pendingRotation != -1 && mCallback != null) {
            mCallback.rotation(pendingRotation);
        }
    }

    private RotationCallBack mCallback;

    public interface RotationCallBack {
        void rotation(int orientation);
    }

    public void disableOrientationListener() {
        if (mOrientationEventListener != null && mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.disable();
        }
    }

    /**
     * 获取屏幕内容的方向
     * 注意，这个函数在屏幕旋转后，不能实时获取到
     *
     * @return 返回顺时针旋转到正常画面的角度
     */
    public static int getWindowManagerRotation(Context context) {
        int angle = 0;
        if (context == null) {
            return angle;
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return angle;
        int rotation = wm.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                angle = 270;
                break;
            case Surface.ROTATION_180:
                angle = 180;
                break;
            case Surface.ROTATION_270:
                angle = 90;
                break;
            case Surface.ROTATION_0:
            default:
                angle = 0;
                break;
        }
        return angle;
    }


}
