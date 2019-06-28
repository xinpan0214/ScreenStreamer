package com.xucz.libscreenstream.record_helper;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;

import com.xucz.libscreenstream.utils.FileUtils;

import java.io.File;
import java.util.LinkedList;

/**
 * Description:录制工具类
 *
 * @author 杜乾, Created on 2018/5/17 - 19:45.
 * E-mail:duqian2010@gmail.com
 */
public class RecordUtils {

    /**
     * 获取视频时长，耗时几十毫秒
     *
     * @param videoPath
     * @return
     */
    public static long getVideoDuration(String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            return 0;
        }
        long durationInt = 0;
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            durationInt = Integer.parseInt(duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return durationInt;
    }

    /**
     * 获取视频的宽高
     *
     * @param videoPath
     * @return
     */
    public static int[] getVideoHeightAndWidth(String videoPath) {
        int[] size = new int[2];
        if (TextUtils.isEmpty(videoPath)) {
            return null;
        }
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

            size[0] = Integer.parseInt(width);
            size[1] = Integer.parseInt(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Logger.d("dq getVideo width=" + size[0] + ",Height=" + size[1]);
        return size;
    }

    public static boolean moveVideos(String srcPath, String dstPath) {
        boolean isSucc = false;
        if (srcPath == null || dstPath == null) {
            return isSucc;
        }
        FileUtils.deleteFile(dstPath);
        File dest = new File(dstPath);
        isSucc = new File(srcPath).renameTo(dest);
        return isSucc;
    }

    public static boolean copyVideos(String srcPath, String dstPath) {
        if (srcPath == null || dstPath == null) {
            return false;
        }
        FileUtils.deleteFile(dstPath);
        return FileUtils.copyFileUsingFileChannels(srcPath, dstPath);
    }

    /**
     * 拼接视频，要开启线程处理
     *
     * @return
     */
    public static boolean mergeVideos(LinkedList<String> linkedList, String targetPath) {
        if (linkedList == null || linkedList.size() == 0 || TextUtils.isEmpty(targetPath)) {
            return false;
        }
        //生成合成文件列表
        String fileListPath = RecordStoreManager.getInstance().getLiveSavedDir() + "mergelist.txt";
        FileUtils.deleteFile(fileListPath);

        StringBuilder sb = new StringBuilder();
        for (String videoPath : linkedList) {
            if (!FileUtils.isExist(videoPath)) {
                continue;
            }
            sb.append("file '");
            sb.append(videoPath);
            sb.append("'\n");//file '/sdcard/1.mp4'
        }
        String text = sb.toString();
        //把内容写入到文件
        boolean writeRet = FileUtils.write(fileListPath, false, text);
        if (!writeRet) return false;

        //先删除最终文件，若有的话
        FileUtils.deleteFile(targetPath);

        boolean result = false;
        if (linkedList.size() == 1) {
            final File dest = new File(targetPath);
            result = FileUtils.copyFileUsingFileChannels(new File(linkedList.peek()), dest);
        } else {
            //构造合成命令
//            MergeCommand.Builder builder = new MergeCommand.Builder();
//            builder.setFileList(fileListPath);
//            builder.setOutputFilePath(targetPath);
//            result = FFmpegBox.getInstance().execute(builder.build());
        }
        //get file size
        long len = FileUtils.getFileSize(targetPath);
        if (len == 0L) {
            result = false;
        }
        moveMoov2Head(targetPath);
        return result;
    }

    /**
     * 移动mp4的moov到文件开始位置
     *
     * @param targetPath
     * @return
     */
    public static boolean moveMoov2Head(String targetPath) {
        if (TextUtils.isEmpty(targetPath)) {
            return false;
        }
        boolean result = false;
        try {
            String moveMoovMp4Temp = RecordStoreManager.getInstance().getLiveVideoTempDir() + "/moovfast.mp4";
            FileUtils.deleteFile(moveMoovMp4Temp);
//            final boolean fastStartMp4 = Mp4FastStart.fastStartMp4(targetPath, moveMoovMp4Temp);
//            if (fastStartMp4) {
//                FileUtils.deleteFile(targetPath);
//                final File dest = new File(targetPath);
//                result = new File(moveMoovMp4Temp).renameTo(dest);
//            }
            //Logger.d("dq fastStartMp4=" + fastStartMp4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //rotate
//    public static boolean rotateVideo(String videoPath, String targetPath, int degrees) {
//        boolean result = false;
//        if (degrees != 0) {
//            RotateVideoCommand.Builder builder = new RotateVideoCommand.Builder();
//            builder.setInputFilePath(videoPath);
//            builder.setRotateDegrees(degrees);
//            builder.setOutputFilePath(targetPath);
//            result = FFmpegBox.getInstance().execute(builder.build());
//        }
//        return result;
//    }


    /**
     * 通知系统，扫描最新的文件，以便看到最新录制的文件
     *
     * @param context
     * @param videoPath
     */
    public static void notifyScanFile(Context context, String videoPath) {
        Uri localUri = Uri.parse("file://" + videoPath);
        Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        intent.setData(localUri);
        context.sendBroadcast(intent);
    }

}
