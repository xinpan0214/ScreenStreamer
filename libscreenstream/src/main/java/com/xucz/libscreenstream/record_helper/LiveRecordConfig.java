package com.xucz.libscreenstream.record_helper;

import static com.xucz.libscreenstream.NonoRecordHelper.RECORD_TIME_LESS_LIVE_TIME_SECOND;
import static com.xucz.libscreenstream.NonoRecordHelper.RECORD_TIME_LIMIT_MAX_SECOND;

/**
 * Description:直播间录制入参
 *
 * @author 杜乾, Created on 2018/5/17 - 12:00.
 * E-mail:duqian2010@gmail.com
 */
public class LiveRecordConfig {
    public String path;//父目录，不带斜杠
    public int total_time = 0;//默认0
    public int cache_time;//录制缓存总时长 毫秒
    public int shoot_time;//合成mp4的实际时长 毫秒

    public LiveRecordConfig(String path, int total_time, int cache_time, int shoot_time) {
        this.path = path;
        this.total_time = total_time;
        this.cache_time = cache_time;
        this.shoot_time = shoot_time;
    }

    public static LiveRecordConfig getDefaultLiveRecordConfig() {
        final String liveVideoTempDir = RecordStoreManager.getInstance().getLiveVideoTempDir();
        int cache_time = RECORD_TIME_LESS_LIVE_TIME_SECOND * 1000;
        int shoot_time = RECORD_TIME_LIMIT_MAX_SECOND * 1000;
        return new LiveRecordConfig(liveVideoTempDir, 0, cache_time, shoot_time);
    }

}
