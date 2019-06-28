package com.xucz.libscreenstream;

import java.util.Arrays;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class RecognitionSvm {
    private String[] svmFileNames;
    private String[] labels;

    public RecognitionSvm() {
    }

    public static RecognitionSvm wrap(String[] svmFileNames, String[] labels) {
        RecognitionSvm svm = new RecognitionSvm();
        svm.svmFileNames = svmFileNames;
        svm.labels = labels;
        return svm;
    }

    public static boolean check(RecognitionSvm svm) {
        return null != svm && null != svm.getSvmFileNames() && null != svm.getLabels() && svm.getSvmFileNames().length == svm.getLabels().length;
    }

    public String toString() {
        return "RecognitionSvm{svmFileNames=" + Arrays.toString(this.svmFileNames) + ", labels=" + Arrays.toString(this.labels) + '}';
    }

    public String[] getSvmFileNames() {
        return this.svmFileNames;
    }

    public void setSvmFileNames(String[] svmFileNames) {
        this.svmFileNames = svmFileNames;
    }

    public String[] getLabels() {
        return this.labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }
}
