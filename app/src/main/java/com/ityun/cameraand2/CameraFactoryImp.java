package com.ityun.cameraand2;

/**
 * @user xie
 * @date 2019/1/4 0004
 * @email 773675907@qq.com.
 */

public interface CameraFactoryImp {

    /**
     * 前后置摄像头的切换
     *
     * @param type
     */
    void changeCamera(int type);

    /**
     * 获取最大焦距
     *
     * @return
     */
    int getCameraMaxZoom();

    /**
     * 调整焦距
     *
     * @param zoom
     */
    void setCameraZoom(int zoom);

    /**
     * 获取当前摄像头的类型
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
}
