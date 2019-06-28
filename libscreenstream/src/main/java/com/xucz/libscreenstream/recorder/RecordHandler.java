package com.xucz.libscreenstream.recorder;

import android.os.Handler;
import android.os.Message;

import com.xucz.libscreenstream.log.PushLog;

import java.io.Serializable;
import java.nio.channels.ClosedChannelException;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-26
 */
public class RecordHandler extends Handler {
    private static final int MSG_RECORD_STARTED = 10;
    private static final int MSG_RECORD_PREPARED = 11;
    private static final int MSG_RECORD_EXCEPTION = 12;
    private static final int MSG_RECORD_NO_KEYFRAME = 20;
    private RecordHandler.RecordListener mWeakListener;
    private volatile boolean isRunning = false;
    public static final int ERROR_CODE_DEFAULT = 100;
    public static final int ERROR_CODE_ILLEGAL_ARGUMENT_EXCEPTION = 101;
    public static final int ERROR_CODE_CLOSED_CHANNEL_EXCEPTION = 102;

    public RecordHandler(RecordHandler.RecordListener listener) {
        this.mWeakListener = listener;
        this.isRunning = true;
    }

    public void release() {
        this.isRunning = false;
        this.removeCallbacksAndMessages((Object) null);
    }

    public void notifyRecordStarted(String msg) {
        this.obtainMessage(10, msg).sendToTarget();
    }

    public void notifyRecordPrepared() {
        this.obtainMessage(11).sendToTarget();
    }

    public void notifyRecordException(Exception e, String filePath) {
        int errorCode = 100;
        if (e instanceof IllegalArgumentException) {
            errorCode = 101;
        } else if (e instanceof ClosedChannelException) {
            errorCode = 102;
        }

        RecordHandler.ErrorInfo errorInfo = new RecordHandler.ErrorInfo(e, errorCode, filePath);
        this.obtainMessage(12, errorInfo).sendToTarget();
    }

    public void notifyRecordNoKeyframe() {
        this.obtainMessage(20).sendToTarget();
    }

    public void handleMessage(Message msg) {
        RecordHandler.RecordListener listener = this.mWeakListener;
        if (listener != null && this.isRunning) {
            switch (msg.what) {
                case 10:
                    listener.onRecordStarted((String) msg.obj);
                    break;
                case 11:
                    listener.onRecordPrepared();
                    break;
                case 12:
                    RecordHandler.ErrorInfo errorInfo = (RecordHandler.ErrorInfo) msg.obj;
                    if (errorInfo != null) {
                        listener.onRecordStopped(errorInfo.error, errorInfo.filePath);
                    }
            }

        } else {
            PushLog.d("RecorderHandler handleMessage listener is null========>>>>>" + msg.what);
        }
    }

    public static class ErrorInfo implements Serializable {
        public Exception error;
        public int errorCode;
        public String filePath;

        public ErrorInfo(Exception e, int errorCode, String filePath) {
            this.error = e;
            this.errorCode = errorCode;
            this.filePath = filePath;
        }

        public String toString() {
            return "ErrorInfo{error=" + this.error + ", errorCode=" + this.errorCode + ", filePath='" + this.filePath + '\'' + '}';
        }
    }

    public interface RecordListener {
        void onRecordPrepared();

        void onRecordStarted(String var1);

        void onRecordStopped(Exception var1, String var2);
    }
}
