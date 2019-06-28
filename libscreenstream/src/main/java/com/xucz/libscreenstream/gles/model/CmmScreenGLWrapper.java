package com.xucz.libscreenstream.gles.model;

import com.xucz.libscreenstream.gles.egl.EglBase;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class CmmScreenGLWrapper {
    public EglBase mEglBase;
    public int mProgram;
    public int muMVPMatrixHandle;
    public int muSTMatrixHandle;
    public int musTexture;
    public int maPositionHandle;
    public int maTextureHandle;

    public CmmScreenGLWrapper() {
    }
}

