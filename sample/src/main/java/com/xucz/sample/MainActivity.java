package com.xucz.sample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.xucz.libscreenstream.record_helper.OrientationHelper;
import com.xucz.libscreenstream.record_helper.PrepareInfo;
import com.xucz.libscreenstream.record_helper.RecordCallBack;
import com.xucz.libscreenstream.record_helper.RecordStoreManager;
import com.xucz.libscreenstream.record_helper.ScreenRecorderImpl;
import com.xucz.libscreenstream.utils.SystemUtils;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_MEDIA_PROJECTION = 101;

    private MediaProjectionManager mMediaProjectionManager;

    private ScreenRecorderImpl liveRecorder;

    private boolean isRecordStarted = false;//开始了录制

    private final RecordCallBack recordCallBack = new RecordCallBack() {
        @Override
        public void onStop(Throwable error) {
            if (!isRecordStarted) {
                return;//避免重复通知，因为回调是多次，只有有错误就会触发
            }
            //Logger.d(TAG, "dq screenRecorder onStop " + error);
            isRecordStarted = false;
        }

        @Override
        public void onStart() {
            isRecordStarted = true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initScreenStream();
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initScreenStream() {
        try {
            mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        } catch (Exception e) {
            mMediaProjectionManager = null;
        }
        int screenWidth = SystemUtils.getScreenWidth(this);
        int screenHeight = SystemUtils.getScreenHeight(this);
        int screenDpi = SystemUtils.getScreenDensity(this);
        String savePath = RecordStoreManager.getInstance().getLiveVideoSavedPath();
        PrepareInfo prepareInfo = new PrepareInfo(screenWidth, screenHeight, screenDpi, savePath, false);
        prepareInfo.context = this;
        prepareInfo.mMediaProjectionManager = mMediaProjectionManager;

        liveRecorder = new ScreenRecorderImpl(false, recordCallBack);
        liveRecorder.initRecorder(prepareInfo);
    }

    public void mayStartRecord(View view) {
        tryRequestPermissions();
    }

    private void tryRequestPermissions() {
        if (hasPermissions()) {
            startCaptureIntent();
        } else if (Build.VERSION.SDK_INT >= M) {
            requestPermissions();
        } else {
            toastNoPermission();
        }
    }

    private boolean hasPermissions() {
        Context context = getApplicationContext();
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        int granted = pm.checkPermission(RECORD_AUDIO, packageName)
                | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName)
                | pm.checkPermission(READ_EXTERNAL_STORAGE, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    private void toastNoPermission() {
        Toast.makeText(this, "无权限", Toast.LENGTH_SHORT).show();
    }

    @TargetApi(M)
    private void requestPermissions() {
        String[] permissions = new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE};
        boolean showRationale = false;
        for (String perm : permissions) {
            showRationale |= shouldShowRequestPermissionRationale(perm);
        }
        if (!showRationale) {
            requestPermissions(permissions, REQUEST_PERMISSIONS);
            return;
        }
        showRequestPermissionDialog(permissions);
    }

    @TargetApi(M)
    private void showRequestPermissionDialog(final String[] permissions) {
        new AlertDialog.Builder(this)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        requestPermissions(permissions, REQUEST_PERMISSIONS);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            int granted = PackageManager.PERMISSION_GRANTED;
            for (int r : grantResults) {
                granted |= r;
            }
            if (granted == PackageManager.PERMISSION_GRANTED) {
                startCaptureIntent();
            } else {
                toastNoPermission();
            }
        }
    }

    @TargetApi(21)
    private void startCaptureIntent() {

        if (mMediaProjectionManager != null) {
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            final boolean isOK = resultCode == Activity.RESULT_OK;
            if (isOK) {
                doStartRecord(data);
            }
        }
    }

    private void doStartRecord(Intent data) {
        if (liveRecorder != null && mMediaProjectionManager != null) {
            liveRecorder.setCallback(recordCallBack);
            int mOrientation = OrientationHelper.getWindowManagerRotation(this);
            liveRecorder.recordScreen(data, mOrientation);
        }
    }

    public void stopRecord(View view) {
        if (liveRecorder != null) {
            liveRecorder.stopRecord();
        }
        isRecordStarted = false;
    }
}
