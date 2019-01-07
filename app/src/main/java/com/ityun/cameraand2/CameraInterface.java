package com.ityun.cameraand2;

import android.view.SurfaceView;

/**
 * @user xie
 * @date 2019/1/3 0003
 * @email 773675907@qq.com.
 */

public interface CameraInterface {


    /**
     * 开始预览
     *
     * @param surfaceView
     */
    void startPreview(SurfaceView surfaceView);

    /**
     * 设置焦距
     *
     * @param zoom
     */
    void setZoom(int zoom);

    /**
     * 获取支持最大的焦距
     *
     * @return
     */
    int getMaxZoom();


    /**
     * 当前的焦距
     *
     * @return
     */
    int nowZoom();

    /**
     * 设置相机正反面
     *
     * @param cameraType
     */
    void setCameraType(int cameraType);

    /**
     * 获取当前是正面还是反面 0反面 1正面
     *
     * @return
     */
    int getCameraType();

    /**
     * 打开闪光灯
     *
     * @param open
     */
    void openLight(boolean open);

    /**
     * 是否打开了闪光灯
     *
     * @return
     */
    boolean isOpenLight();


    /**
     * 拍照
     */
    void  capturePicture();

}
