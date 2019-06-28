package com.xucz.libscreenstream.callback;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public interface IConnectionListener {
    void onOpenConnectionCallback(int var1);

    void onWriteError(int var1, int var2);

    void onCloseConnectionCallback(int var1);

    void onChangeMediaCodeParamCallback(int var1, boolean var2, int var3, float var4);

    void onSoftEncodeCarton();

    void onNoKeyframeError();

    public static class IWriteErrorRunnable implements Runnable {
        IConnectionListener connectionListener;
        int frameType;
        int errorCount;

        public IWriteErrorRunnable(IConnectionListener connectionListener, int frameType, int errorCount) {
            this.connectionListener = connectionListener;
            this.frameType = frameType;
            this.errorCount = errorCount;
        }

        public void run() {
            if (this.connectionListener != null) {
                this.connectionListener.onWriteError(this.frameType, this.errorCount);
            }

        }
    }
}
