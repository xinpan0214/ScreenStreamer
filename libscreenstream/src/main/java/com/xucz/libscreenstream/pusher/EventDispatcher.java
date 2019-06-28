package com.xucz.libscreenstream.pusher;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public final class EventDispatcher {
    private static EventDispatcher INSTANCE;
    private final Executor mCallbackPoster = new Executor() {
        public void execute(Runnable command) {
            EventDispatcher.this.mainHandler.post(command);
        }
    };
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static EventDispatcher getInstance() {
        return INSTANCE == null ? (INSTANCE = new EventDispatcher()) : INSTANCE;
    }

    private EventDispatcher() {
    }

    public void post(Runnable runnable) {
        this.mCallbackPoster.execute(runnable);
    }
}
