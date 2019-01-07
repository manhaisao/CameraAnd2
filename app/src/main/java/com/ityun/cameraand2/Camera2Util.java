package com.ityun.cameraand2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @user xie
 * @date 2019/1/3 0003
 * @email 773675907@qq.com.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Util implements CameraInterface, SurfaceHolder.Callback {

    private Context context;
    private CameraManager cameraManager;
    private Handler childHandler, mainHandler;
    private SurfaceView surfaceView;
    private CameraDevice mCamera;
    private CaptureRequest.Builder mPreviewBuilder;
    //摄像头的id 也就是camera中的前后置的问题
    String camaraType = "0";
    //前置还是后置摄像头
    private int cameratype;
    private CameraCaptureSession mSession;
    private ImageReader mImageReader;
    // 创建拍照需要的CaptureRequest.Builder
    private CaptureRequest.Builder captureRequestBuilder;
    CameraCharacteristics characteristics;
    //焦距
    int cameraZoom;
    //是否打开闪光灯
    private boolean isLightOpen;

    public Camera2Util(Context context) {
        this.context = context;
    }


    @Override
    public void startPreview(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        surfaceView.getHolder().addCallback(this);
    }


    private void init() {
        //很多过程都变成了异步的了，所以这里需要一个子线程的looper
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(context.getMainLooper());
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        surfaceView.getHolder().setKeepScreenOn(true);
        //设置照片的大小
        mImageReader = ImageReader.newInstance(1080, 960, ImageFormat.JPEG, 5);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                // 拿到拍照照片数据
                Image image = imageReader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);//由缓冲区存入字节数组
                image.close();
                //saveBitmap(bytes);//保存照片的处理
            }
        }, mainHandler);
    }

    @Override
    public void setZoom(int zoom) {
        if (zoom == 0) {
            zoom = 1;
        }
        cameraZoom = zoom;
        float maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
        Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int minW = (int) (m.width() / maxZoom);
        int minH = (int) (m.height() / maxZoom);
        int difW = m.width() - minW;
        int difH = m.height() - minH;
        int cropW = difW / 100 * zoom;
        int cropH = difH / 100 * zoom;
        cropW -= cropW & 3;
        cropH -= cropH & 3;
        Rect zooms = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
        mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zooms);
        try {
            updatePreview(mSession);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMaxZoom() {
        /**
         * 这边成5 上面成10 是因为如果值相同你会发现如果set的zoom大于最大值的一半时 有特么的开始缩放了 所以这里就给了一半
         */
        return (int) (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) * 5);
    }

    @Override
    public int nowZoom() {
        return cameraZoom;
    }

    @Override
    public void setCameraType(int cameraType) {
        cameratype = cameraType;
        cameraZoom = 0;
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        init();
        try {
            if (cameraType == 1) {
                camaraType = cameraManager.getCameraIdList()[1];
            } else {
                camaraType = cameraManager.getCameraIdList()[0];
            }
//            characteristics = cameraManager.getCameraCharacteristics(camaraType);
//            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//
//            Size[] sizes = map.getOutputSizes(SurfaceHolder.class);
//            Size size = getCloselyPreSize(surfaceView.getHeight(), surfaceView.getWidth(), sizes);
//            surfaceView.getHolder().setFixedSize(size.getWidth(), size.getHeight());
            cameraManager.openCamera(camaraType, mCameraDeviceStateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCameraType() {
        return cameratype;
    }

    @Override
    public void openLight(boolean open) {
        if (cameratype == 1) {
            //如果是前置摄像头 打开个屁的闪光灯
            return;
        }
        if (open) {
            if (!isLightOpen) {
                mPreviewBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
            }
        } else {
            if (isLightOpen) {
                mPreviewBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
            }
        }
        isLightOpen = open;
        try {
            updatePreview(mSession);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isOpenLight() {
        return isLightOpen;
    }

    @Override
    public void capturePicture() {

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        init();
        isLightOpen = false;
        try {
            characteristics = cameraManager.getCameraCharacteristics(camaraType);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size[] sizes = map.getOutputSizes(SurfaceHolder.class);
            Size size = getCloselyPreSize(surfaceView.getHeight(), surfaceView.getWidth(), sizes);
            surfaceHolder.setFixedSize(size.getWidth(), size.getHeight());
            cameraManager.openCamera(camaraType, mCameraDeviceStateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        isLightOpen = false;
        cameraZoom = 0;
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
    }

    /**
     * 摄像头创建监听
     */
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            try {
                //开启预览
                mCamera = camera;

                start(camera);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            //关闭摄像头
            if (mCamera != null) {
                mCamera.close();
                mCamera = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            //发生错误
        }
    };


    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param surfaceWidth  需要被进行对比的原宽，surface view的宽度
     * @param surfaceHeight 需要被进行对比的原高 surface view的高度
     * @param preSizeList   得到的支持预览尺寸的list，parmeters.getSupportedPreviewSizes()
     *                      需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    protected Size getCloselyPreSize(int surfaceWidth, int surfaceHeight, Size[] preSizeList) {
        int ReqTmpWidth;
        int ReqTmpHeight;

        ReqTmpWidth = surfaceWidth;
        ReqTmpHeight = surfaceHeight;
        // 先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Size size : preSizeList) {
            if ((size.getWidth() == ReqTmpWidth) && (size.getHeight() == ReqTmpHeight)) {
                return size;
            }
        }
        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Size retSize = null;
        for (Size size : preSizeList) {
            curRatio = ((float) size.getWidth()) / size.getHeight();
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    //开始预览，主要是camera.createCaptureSession这段代码很重要，创建会话
    private void start(final CameraDevice camera) throws CameraAccessException {
        try {
            // 创建预览需要的CaptureRequest.Builder
            mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            mPreviewBuilder.addTarget(surfaceView.getHolder().getSurface());
            //默认预览不开启闪光灯
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);

            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            camera.createCaptureSession(Arrays.asList(surfaceView.getHolder().getSurface(), mImageReader.getSurface()), mSessionStateCallback, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 会话状态回调
     */
    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mSession = session;
            if (mCamera != null && captureRequestBuilder == null) {
                try {
                    captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    // 将imageReader的surface作为CaptureRequest.Builder的目标
                    captureRequestBuilder.addTarget(mImageReader.getSurface());

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            try {
                updatePreview(session);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    /**
     * 更新会话，开启预览
     *
     * @param session
     * @throws CameraAccessException
     */
    private void updatePreview(CameraCaptureSession session) throws CameraAccessException {
        session.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, childHandler);
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //需要连拍时，循环保存图片就可以了
        }
    };


}
