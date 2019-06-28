package com.xucz.libscreenstream.pusher.controller;

import com.xucz.libscreenstream.callback.IPrepareListener;
import com.xucz.libscreenstream.rtmp.IFlvDataCollect;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public abstract class AbsPushController {
    public AbsPushController() {
    }

    public abstract boolean prepare(IPrepareListener var1);

    public abstract void start(IFlvDataCollect var1);

    public abstract void stop();

    public abstract void release();
}

