package com.xucz.libscreenstream.record_helper;

import android.os.Environment;
import android.text.TextUtils;

import com.xucz.libscreenstream.BuildConfig;
import com.xucz.libscreenstream.utils.FileUtils;
import com.xucz.libscreenstream.utils.StorageUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.xucz.libscreenstream.NonoRecordHelper.RECORD_TIME_MAX_MILLISECOND;

/**
 * Description:录屏逻辑辅助类
 *
 * @author 杜乾, Created on 2018/4/24 - 20:41.
 * E-mail:duqian2010@gmail.com
 */
public class RecordStoreManager {


    public RecordStoreManager() {
        init();
    }

    public static RecordStoreManager getInstance() {
        return RecordStoreManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final RecordStoreManager INSTANCE = new RecordStoreManager();
    }

    private String liveSaveDir;//父目录
    private String systemPhotosDir;//视频相册路径
    private String liveVideoSavedPath;//录屏最终合成的视频临时保存路径
    private String liveWaterCoverTempDir; //生成水印时的临时拷贝路径
    private String gameVideoSavedDir;//手游开播生成的视频保存路径：要单独保存一个目录
    private String liveScreenshotPath;//视频的缩略图，用做封面，临时保存路径
    private String liveTempDir;//开播间、手游开播录制，多个视频临时保存路径
    public Queue<String> mVideoLinkedList = new ConcurrentLinkedQueue<>();//存放开播间向前录制视频的路径，有先后顺序

    public void clearLiveTempFiles() {//非调试模式，清空录制缓存目录
        if (!BuildConfig.DEBUG) {
            FileUtils.delAllFile(getLiveVideoTempDir());
        }
    }

    public void clearWaterCoverTempFiles() { // 清空水印部分目录
        if (!BuildConfig.DEBUG) {
            FileUtils.delAllFile(getLiveWaterCoverTempDir());
        }
    }

    /**
     * 初始化，创建保存录屏文件的路径
     */
    private void init() {
        if (FileUtils.isSdcardExist()) {
            try {
                liveSaveDir = StorageUtils.getExternalNonoDir() + "/live_record/";
                final File rootFile = new File(liveSaveDir);
                if (!rootFile.exists()) {
                    rootFile.mkdirs();
                }
                liveVideoSavedPath = liveSaveDir + "live-latest-video.mp4";
                liveScreenshotPath = liveSaveDir + "live-screenshot.jpg";
                liveTempDir = liveSaveDir + "live_temp";
                liveWaterCoverTempDir = liveSaveDir + "water_temp";
                FileUtils.createFolder(liveTempDir, FileUtils.MODE_UNCOVER);
                FileUtils.createFolder(gameVideoSavedDir, FileUtils.MODE_UNCOVER);
                FileUtils.createFolder(liveWaterCoverTempDir, FileUtils.MODE_UNCOVER);
                getSystemPhotoDir();
                gameVideoSavedDir = systemPhotosDir;//liveSaveDir + "game-live/";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getSystemPhotoDir() {
        if (TextUtils.isEmpty(systemPhotosDir) && FileUtils.isSdcardExist()) {
            systemPhotosDir = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + "/Camera/";
            FileUtils.createFolder(systemPhotosDir, FileUtils.MODE_UNCOVER);
        }
        return systemPhotosDir;
    }

    public String getLiveVideoSavedPath() {
        if (TextUtils.isEmpty(liveVideoSavedPath)) {
            init();
        }
        return liveVideoSavedPath;
    }

    public String getLiveScreenshotPath() {
        if (TextUtils.isEmpty(liveScreenshotPath)) {
            init();
        }
        return liveScreenshotPath;
    }

    public String getLiveVideoTempDir() {//无 斜杠
        if (TextUtils.isEmpty(liveTempDir)) {
            init();
        }
        return liveTempDir;
    }

    public String getGameVideoSavedDir() {
        if (TextUtils.isEmpty(gameVideoSavedDir)) {
            init();
        }
        return gameVideoSavedDir;
    }

    public String getLiveSavedDir() {
        if (TextUtils.isEmpty(liveSaveDir)) {
            init();
        }
        return liveSaveDir;
    }

    public String getLiveWaterCoverTempDir() {
        if (TextUtils.isEmpty(liveWaterCoverTempDir)) {
            init();
        }
        return liveWaterCoverTempDir;
    }

    public boolean isVideoExist() {
        File video = new File(getLiveVideoSavedPath());
        return video.exists() && video.length() > 0;
    }

    /**
     * 保存到系统相册
     */
    public String saveVideo2Local() {
        boolean isSaved = false;
        String newPath = null;
        File videoFile = new File(liveVideoSavedPath);
        if (videoFile.exists() && videoFile.length() > 0) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
            final String newFileName = "nonolive-" + format.format(new Date()) + ".mp4";
            newPath = getSystemPhotoDir() + newFileName;
            isSaved = FileUtils.moveFile(liveVideoSavedPath, newPath);
        }
        if (isSaved) return newPath;
        return null;
    }

    /**
     * 合并直播间录制的视频文件
     *
     * @param targetPath 目标保存文件路径
     * @return
     */
    public boolean mergeLiveRoomVideos(String targetPath) {
        try {
            LinkedList<String> linkedList = copyLinkedList(mVideoLinkedList);
            return RecordUtils.mergeVideos(linkedList, targetPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 合并秀场，手游开播间录制的视频文件
     *
     * @param targetPath 目标保存文件路径
     * @return
     */
    public boolean mergePushLiveVideos(String targetPath) {
        //去掉头尾,防止正在写或者删除的文件异常
        try {
            if (mVideoLinkedList != null && mVideoLinkedList.size() > 0) {
                mVideoLinkedList.poll();//pollFirst()
            }
            LinkedList<String> linkedList = copyLinkedList(mVideoLinkedList);
            return RecordUtils.mergeVideos(linkedList, targetPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * //调用该方法拷贝一份，顺便判断需要哪些文件
     *
     * @param mVideoLinkedList
     * @return
     */
    private LinkedList<String> copyLinkedList(Queue<String> mVideoLinkedList) {
        LinkedList<String> linkedList = new LinkedList<>();
        long totalVideoDuration = 0;//总的视频长度
        for (String videoPath : mVideoLinkedList) {
            File file = new File(videoPath);
            if (file.exists() && file.length() > 10 * 1024) {// 文件长度大于20kb，计算总时长小于30都加入
                final int limitDuration = RECORD_TIME_MAX_MILLISECOND - 1000;
                if (totalVideoDuration <= limitDuration) {//3x秒
                    final long videoDuration = RecordUtils.getVideoDuration(videoPath);
                    if (videoDuration > 0) {
                        linkedList.add(videoPath);
                        totalVideoDuration += videoDuration;
                    }
                }
            }
        }
        if (totalVideoDuration < 20000) {//总时长小于20秒的舍弃
            //Logger.d("dq 总时长不够，duration=" + totalVideoDuration);
            return null;
        }
        return linkedList;
    }

}
