package com.ityun.cameraand2;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * @user xie
 * @date 2019/1/3 0003
 * @email 773675907@qq.com.
 */

public class CameraUtil implements CameraInterface, SurfaceHolder.Callback {

    private Camera camera;
    private SurfaceView surfaceView;

    OrientationEventListener orientationEventListener;

    boolean flagRecord = false;// 是否正在录像

    Context context;

    //当前的手机的角度
    int rotationFlag = 90;

    //录像时的手机角度
    int rotationRecord = 90;

    //是前置还是后置
    int camaraType = 0;

    private int previewWidth, previewHeight;

    private boolean isLightOpen;


    public CameraUtil(Context context) {
        this.context = context;
    }

    @Override
    public void startPreview(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void setZoom(int zoom) {
        if (camera != null && camera.getParameters().isZoomSupported()) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setZoom(zoom);
            camera.setParameters(parameters);
        }
    }

    @Override
    public int getMaxZoom() {
        if (camera != null && camera.getParameters().isZoomSupported()) {
            return camera.getParameters().getMaxZoom();
        }
        return 0;
    }

    @Override
    public int nowZoom() {
        if (camera != null && camera.getParameters().isZoomSupported()) {
            return camera.getParameters().getZoom();
        }
        return 0;
    }

    @Override
    public void setCameraType(int cameraType) {
        this.camaraType = cameraType;
        changeCamara(cameraType);
    }

    @Override
    public int getCameraType() {
        return camaraType;
    }

    @Override
    public void openLight(boolean open) {
        if (camaraType == 1 || camera == null) {
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        if (!open && isLightOpen) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);//关闭
            camera.setParameters(parameters);
            isLightOpen = false;
        }
        if (open && !isLightOpen) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//开启
            camera.setParameters(parameters);
            isLightOpen = true;
        }
    }

    @Override
    public boolean isOpenLight() {
        if (camera == null)
            return false;
        return camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH) ? true : false;
    }
    @Override
    public void capturePicture() {
        if (camera == null)
            return;
        camera.takePicture(new ShutterCallback() {
            @Override
            public void onShutter() {

            }
        }, new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

            }
        }, new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // 打开相机
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        isLightOpen = false;
        camera = Camera.open(camaraType);
        rotationUIListener();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        doChange(surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }


    /**
     * 切换摄像头
     */
    public void changeCamara(int camaraType) {
        // 切换前后摄像头
        int cameraCount;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
            if (camaraType == 1) {
                // 现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {// 代表摄像头的方位，CAMERA_FACING_FRONT前置
                    // CAMERA_FACING_BACK后置
                    camera.stopPreview();// 停掉原来摄像头的预览
                    camera.release();// 释放资源
                    camera = null;// 取消原来摄像头
                    camera = Camera.open(i);// 打开当前选中的摄像头
                    try {
                        camera.setPreviewDisplay(surfaceView.getHolder());// 通过surfaceview显示取景画面
                        camera.setDisplayOrientation(90);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    camera.startPreview();// 开始预览
                    break;
                }
            } else {
                // 现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {// 代表摄像头的方位，CAMERA_FACING_FRONT前置
                    // CAMERA_FACING_BACK后置
                    camera.stopPreview();// 停掉原来摄像头的预览
                    camera.release();// 释放资源
                    camera = null;// 取消原来摄像头
                    camera = Camera.open(i);// 打开当前选中的摄像头
                    try {
                        camera.setPreviewDisplay(surfaceView.getHolder());// 通过surfaceview显示取景画面
                        camera.setDisplayOrientation(90);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    camera.startPreview();// 开始预览
                    break;
                }
            }
        }
    }


    // 当我们的程序开始运行，即使我们没有开始录制视频，我们的surFaceView中也要显示当前摄像头显示的内容
    private void doChange(SurfaceHolder holder) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            previewWidth = getCloselyPreSize(surfaceView.getWidth(), surfaceView.getHeight(), parameters.getSupportedPreviewSizes()).width;
            previewHeight = getCloselyPreSize(surfaceView.getWidth(), surfaceView.getHeight(), parameters.getSupportedPreviewSizes()).height;
            if (camaraType == 0) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
                camera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
            }
            if (rotationRecord == 90) {
                parameters.setPreviewSize(previewWidth, previewHeight);
            }
            camera.setParameters(parameters);
            camera.setPreviewDisplay(holder);
            // 设置surfaceView旋转的角度，系统默认的录制是横向的画面，把这句话注释掉运行你就会发现这行代码的作用
//            if (camaraType == 1) {
//            } else {
            camera.setDisplayOrientation(90);
//            }
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param surfaceWidth  需要被进行对比的原宽，surface view的宽度
     * @param surfaceHeight 需要被进行对比的原高 surface view的高度
     * @param preSizeList   得到的支持预览尺寸的list，parmeters.getSupportedPreviewSizes()
     *                      需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    protected Camera.Size getCloselyPreSize(int surfaceWidth, int surfaceHeight, List<Camera.Size> preSizeList) {
        int ReqTmpWidth;
        int ReqTmpHeight;
        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
        if (rotationRecord == 90) {
            ReqTmpWidth = surfaceHeight;
            ReqTmpHeight = surfaceWidth;
        } else {
            ReqTmpWidth = surfaceWidth;
            ReqTmpHeight = surfaceHeight;
        }
        // 先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Camera.Size size : preSizeList) {
            if ((size.width == ReqTmpWidth) && (size.height == ReqTmpHeight)) {
                return size;
            }
        }
        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    /**
     * 旋转界面UI
     */
    private void rotationUIListener() {
        orientationEventListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int rotation) {
                if (!flagRecord) {
                    if (((rotation >= 0) && (rotation <= 30)) || (rotation >= 330)) {
                        // 竖屏拍摄
                        if (rotationFlag != 0) {
                            // 旋转logo
                            // rotationAnimation(rotationFlag, 0);
                            // 这是竖屏视频需要的角度
                            rotationRecord = 90;
                            // 这是记录当前角度的flag
                            rotationFlag = 0;
                        }
                    } else if (((rotation >= 230) && (rotation <= 310))) {
                        // 横屏拍摄
                        if (rotationFlag != 90) {
                            // 旋转logo
                            // rotationAnimation(rotationFlag, 90);
                            // 这是正横屏视频需要的角度
                            rotationRecord = 0;
                            // 这是记录当前角度的flag
                            rotationFlag = 90;
                        }
                    } else if (rotation > 30 && rotation < 95) {
                        // 反横屏拍摄
                        if (rotationFlag != 270) {
                            // 旋转logo
                            // rotationAnimation(rotationFlag, 270);
                            // 这是反横屏视频需要的角度
                            rotationRecord = 180;
                            // 这是记录当前角度的flag
                            rotationFlag = 270;
                        }
                    }
                }
            }
        };
        orientationEventListener.enable();
    }


}
