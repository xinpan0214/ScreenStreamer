package com.xucz.libscreenstream.entity;

import android.text.TextUtils;
import android.util.Log;

import com.xucz.libscreenstream.RecognitionSvm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class AnimationMap {
    private static final String TAG = "AnimationMap";
    private Map<String, Entry> map = new HashMap();
    private AnimationMap.OnSvmCallback svmCallback = null;

    public AnimationMap() {
    }

    public static AnimationMap create() {
        return new AnimationMap();
    }

    public AnimationMap.Entry get(String name) {
        return (AnimationMap.Entry) this.map.get(name);
    }

    public synchronized AnimationMap updateAnimation(String name, String animation) {
        if (this.map.containsKey(name)) {
            this.update(name, ((AnimationMap.Entry) this.map.get(name)).svmPath, animation);
        }

        return this;
    }

    public synchronized AnimationMap put(String name, String svmPath, String animation) {
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(svmPath) && !TextUtils.isEmpty(animation)) {
            if (this.map.containsKey(name)) {
                this.update(name, svmPath, animation);
            } else {
                AnimationMap.Entry e = new AnimationMap.Entry();
                e.setName(name);
                e.setSvmPath(svmPath);
                e.setAnimation(animation);
                this.map.put(name, e);
            }

            return this;
        } else {
            Log.e("AnimationMap", "You can not put an invalid animation: " + name + ", " + svmPath + ", " + animation);
            return this;
        }
    }

    public synchronized AnimationMap remove(String name) {
        this.map.remove(name);
        return this;
    }

    public synchronized void clear() {
        if (null != this.map) {
            this.map.clear();
        }
    }

    public synchronized Map<String, AnimationMap.Entry> getMap() {
        return this.map;
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.size() < 1;
    }

    private void update(String name, String svmPath, String animation) {
        AnimationMap.Entry e = new AnimationMap.Entry();
        e.setName(name);
        e.setSvmPath(svmPath);
        e.setAnimation(animation);
        this.map.remove(name);
        this.map.put(name, e);
    }

    public synchronized RecognitionSvm getRecognitionSvm() {
        if (this.isEmpty()) {
            return null;
        } else {
            String[] paths = new String[this.size()];
            String[] labels = new String[this.size()];
            int i = 0;

            for (Iterator var4 = this.map.entrySet().iterator(); var4.hasNext(); ++i) {
                java.util.Map.Entry<String, AnimationMap.Entry> e = (java.util.Map.Entry) var4.next();
                paths[i] = ((AnimationMap.Entry) e.getValue()).svmPath;
                labels[i] = ((AnimationMap.Entry) e.getValue()).name;
            }

            return RecognitionSvm.wrap(paths, labels);
        }
    }

    public String toString() {
        return "AnimationMap{map=" + this.map + '}';
    }

    public AnimationMap.OnSvmCallback getSvmCallback() {
        return this.svmCallback;
    }

    public void setSvmCallback(AnimationMap.OnSvmCallback sc) {
        this.svmCallback = sc;
    }

    public interface OnSvmCallback {
        void onSvmError();
    }

    public static class Entry {
        private String name;
        private String svmPath;
        private String animation;

        public Entry() {
        }

        public String toString() {
            return "Entry{name='" + this.name + '\'' + ", svmPath='" + this.svmPath + '\'' + ", animation='" + this.animation + '\'' + '}';
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSvmPath() {
            return this.svmPath;
        }

        public void setSvmPath(String svmPath) {
            this.svmPath = svmPath;
        }

        public String getAnimation() {
            return this.animation;
        }

        public void setAnimation(String animation) {
            this.animation = animation;
        }
    }
}
