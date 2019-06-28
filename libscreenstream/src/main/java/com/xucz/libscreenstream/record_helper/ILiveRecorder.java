package com.xucz.libscreenstream.record_helper;

/**
 * Description:开播间、直播间的视频流录制接口
 *
 * @author 杜乾, Created on 2018/4/24 - 20:52.
 * E-mail:duqian2010@gmail.com
 */
public interface ILiveRecorder {

    public void initRecorder();

    public void startRecord();

    public void stopRecord();

    void setCallback(RecordCallBack callback);

}
