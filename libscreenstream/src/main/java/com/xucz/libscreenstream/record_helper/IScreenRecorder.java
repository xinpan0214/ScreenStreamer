package com.xucz.libscreenstream.record_helper;

import android.content.Intent;

/**
 * Description:屏幕录制接口：向后录制共用这套接口，方便拓展变更实现
 *
 * @author 杜乾, Created on 2018/4/24 - 20:52.
 * E-mail:duqian2010@gmail.com
 */
public interface IScreenRecorder {
    void initRecorder(PrepareInfo prepareInfo);

    void recordScreen(Intent mediaResultData, int mOrientation);

    void stopRecord();

    void setCallback(RecordCallBack callback);
}
