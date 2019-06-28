package com.xucz.libscreenstream.record_helper;

/**
 * Description:录屏公共回调
 *
 * @author 杜乾, Created on 2018/5/10 - 18:39.
 * E-mail:duqian2010@gmail.com
 */
public interface RecordCallBack {

    void onStart();

    void onStop(Throwable error);

}
