package cherry.android.camera.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import cherry.android.camera.CaptureCallback;
import cherry.android.camera.ImageManager;
import cherry.android.camera.PreviewCallback;
import cherry.android.camera.SizeSupplier;
import cherry.android.camera.annotations.CameraId;
import cherry.android.camera.annotations.CameraState;
import cherry.android.camera.body.Camera1CaptureBody;
import cherry.android.camera.util.CameraUtil;
import cherry.android.camera.util.Logger;

import static cherry.android.camera.annotations.CameraState.STATE_CAPTURE_BURST;
import static cherry.android.camera.annotations.CameraState.STATE_CAPTURE_ONCE;
import static cherry.android.camera.annotations.CameraState.STATE_PREVIEW;

/**
 * Created by Administrator on 2017/4/6.
 */

public class Camera1 extends AbstractCamera<Camera> implements Camera.PreviewCallback {
    private static final String TAG = "Camera1";

    private Camera.Parameters mParameters;
    private Camera.CameraInfo mCameraInfo;

    private CaptureCallback mCallback;

    private int mDefaultPreviewWidth;
    private int mDefaultPreviewHeight;

    private int mContinuous = -1;
    @CameraState
    private int mState = STATE_PREVIEW;

    private ReentrantLock mCameraLock;
    private byte[] mCallbackBuffer;
    private PreviewCallback mPreviewCallback;

    public Camera1(@NonNull Context context, @NonNull SurfaceTexture texture) {
        super(context, texture);
        mCameraInfo = new Camera.CameraInfo();
        mCameraLock = new ReentrantLock();
    }


    @Override
    public void setPreviewSize(int width, int height) {
        if (mDefaultPreviewWidth == width && mDefaultPreviewHeight == height) {
            return;
        }
        mDefaultPreviewWidth = width;
        mDefaultPreviewHeight = height;
        if (mCameraDriver != null) {
            mCameraDriver.setPreviewCallbackWithBuffer(null);
            stopPreview();
            resolvePreviewSize();
            mCameraDriver.setParameters(mParameters);
            Camera.Size previewSize = mParameters.getPreviewSize();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                mSurfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height);
            }
            final int size = (previewSize.width * previewSize.height) * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
            mCallbackBuffer = new byte[size];
            mCameraDriver.addCallbackBuffer(mCallbackBuffer);
            mCameraDriver.setPreviewCallbackWithBuffer(this);
            startPreview();
        }
    }

    @Override
    public void openCamera(@CameraId int cameraId, Handler handler) throws Exception {
        super.openCamera(cameraId, handler);
        if (cameraId == CameraId.CAMERA_FRONT) {
            mRealCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mRealCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCameraLock.lock();
                    Logger.i(TAG, "openCamera");
                    mCameraDriver = Camera.open(mRealCameraId);

                    setupCameraParams();
                    mCameraDriver.setParameters(mParameters);
                    Camera.Size previewSize = mParameters.getPreviewSize();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        mSurfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height);
                    }

                    final int size = (previewSize.width * previewSize.height) * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
                    mCallbackBuffer = new byte[size];

                    mCameraDriver.setPreviewTexture(mSurfaceTexture);
                    //preview onPreview
                    mCameraDriver.addCallbackBuffer(mCallbackBuffer);
                    Logger.e(TAG, "open callback buffer size=" + size);
                    mCameraDriver.setPreviewCallbackWithBuffer(Camera1.this);
                    mCameraDriver.startPreview();
                    mCameraLock.unlock();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void closeCamera() {
        if (mCameraDriver != null) {
            mCameraLock.lock();
            Logger.i(TAG, "releaseCamera");
            mCameraDriver.setPreviewCallbackWithBuffer(null);
            mCameraDriver.setPreviewCallback(null);
            mCameraDriver.release();
            mCameraDriver = null;
            mParameters = null;
            mCameraLock.unlock();
        }
    }

    @Override
    public void capture() throws Exception {
        mState = STATE_CAPTURE_ONCE;
        capture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                stopPreview();
                //mBackgroundHandler.post(new ImageSaver(mContext, data, Camera1.this));
                ImageManager.instance().execute(new Camera1CaptureBody(mContext, data, Camera1.this), Camera1.this);
                //startPreview();
            }
        });
    }

    @Override
    public void captureBurst() {
        final int count = 10;
        mContinuous = 0;
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                mState = STATE_CAPTURE_BURST;
                capture(new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {
                        stopPreview();
                        Logger.e(TAG, Thread.currentThread().getName());
                        int index = mContinuous++;
                        ImageManager.instance().execute(new Camera1CaptureBody(mContext, index, bytes, Camera1.this), Camera1.this);
                        if (index < count - 1) {
                            startPreview();
                            capture(this);
                        } else {
                            startPreview();
                            mContinuous = -1;
                        }
                    }
                });
            }
        });
    }

    private void capture(final Camera.PictureCallback cb) {
        mCameraDriver.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
                if (b) {
                    mCameraDriver.takePicture(null, null, cb);
                }
            }
        });

    }

    @Override
    public void startPreview() {
        if (mCameraDriver != null) {
            mState = STATE_PREVIEW;
            mCameraDriver.startPreview();
        }
    }

    @Override
    public void stopPreview() {
        if (mCameraDriver != null) {
            mCameraDriver.stopPreview();
        }
    }

    @Override
    public int getOrientation() {
        Camera.getCameraInfo(mRealCameraId, mCameraInfo);
        Logger.e(TAG, "orientation=" + mCameraInfo.orientation);
        Logger.d(TAG, "getDisplayRotation=" + CameraUtil.getDisplayRotation(mContext));
        Logger.v(TAG, "getDisplayOrientation=" + getDisplayOrientation(CameraUtil.getDisplayRotation(mContext), mRealCameraId));
        return (mCameraInfo.orientation - CameraUtil.getDisplayRotation(mContext) + 360) % 360;
    }

    @Override
    public int getState() {
        return mState;
    }

    private static int getDisplayOrientation(int degrees, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            // compensate the mirror
            result = (360 - result) % 360;
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Override
    public void setCaptureCallback(CaptureCallback cb) {
        mCallback = cb;
    }

    @Override
    public CaptureCallback getCaptureCallback() {
        return mCallback;
    }

    @Override
    public void setPreviewCallback(PreviewCallback callback) {
        this.mPreviewCallback = callback;
    }

    @Override
    public List<int[]> getSupportPreviewSizes() {
        List<int[]> supportedSizes = null;
        if (mParameters != null) {
            List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
            supportedSizes = new ArrayList<>(sizes.size());
            for (Camera.Size size : sizes) {
                int[] sizeArr = new int[]{mSupplier.width(size), mSupplier.height(size)};
                supportedSizes.add(sizeArr);
            }
        }
        return supportedSizes;
    }

    private void setupCameraParams() {
        mParameters = mCameraDriver.getParameters();
        //mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setRotation(90);

        List<String> focusModes = mParameters.getSupportedFocusModes();
//        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
//            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        }
        if (focusModes.contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        final Camera.Size[] supportPictureSizes = cameraSizesToArray(mParameters.getSupportedPictureSizes());

        double screenRatio = CameraUtil.findFullscreenRatio(mContext, supportPictureSizes, mSupplier);

        for (Camera.Size size : mParameters.getSupportedPictureSizes()) {
            Logger.e(TAG, "support picture size=" + size.width + "x" + size.height);
        }
        //预览尺寸
        resolvePreviewSize();
        //照片尺寸
        Camera.Size pictureSize = CameraUtil.getOptimalPictureSize(supportPictureSizes, screenRatio, mSupplier);
        mParameters.setPictureSize(pictureSize.width, pictureSize.height);
        Log.e(TAG, "optimal pic size: " + pictureSize.width + "x" + pictureSize.height);
    }

    private void resolvePreviewSize() {
        final Camera.Size[] supportPreviewSizes = cameraSizesToArray(mParameters.getSupportedPreviewSizes());
        for (Camera.Size size : mParameters.getSupportedPreviewSizes()) {
            Logger.e(TAG, "support preview size=" + size.width + "x" + size.height);
        }

        Camera.Size previewSize;
        if (mDefaultPreviewWidth != 0 && mDefaultPreviewHeight != 0) {
            previewSize = CameraUtil.getOptimalPreviewSizeWithTarget(supportPreviewSizes,
                    mDefaultPreviewWidth,
                    mDefaultPreviewHeight,
                    mSupplier);
        } else {
            final Camera.Size[] supportPictureSizes = cameraSizesToArray(mParameters.getSupportedPictureSizes());
            double screenRatio = CameraUtil.findFullscreenRatio(mContext, supportPictureSizes, mSupplier);
            previewSize = CameraUtil.getOptimalPreviewSize(mContext, supportPreviewSizes, screenRatio, false, mSupplier);

        }
        mDefaultPreviewWidth = previewSize.width;
        mDefaultPreviewHeight = previewSize.height;
        mParameters.setPreviewSize(previewSize.width, previewSize.height);
        Log.e(TAG, "optimal preview size: " + previewSize.width + "x" + previewSize.height);
        if (mPreviewCallback != null) {
            mPreviewCallback.onSizeChanged(previewSize.width, previewSize.height);
        }
    }

    @NonNull
    private Camera.Size[] cameraSizesToArray(@NonNull List<Camera.Size> sizes) {
        return sizes.toArray(new Camera.Size[]{});
    }

    private final SizeSupplier<Camera.Size> mSupplier = new SizeSupplier<Camera.Size>() {
        @Override
        public int width(Camera.Size size) {
            return size.width;
        }

        @Override
        public int height(Camera.Size size) {
            return size.height;
        }
    };

    private int mLastWidth;

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (mPreviewCallback != null && camera != null) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            if (mLastWidth != size.width) {
                Logger.i(TAG, "onPreview size: " + size.width + "x" + size.height);
                mLastWidth = size.width;
            }
            mPreviewCallback.onPreview(this, bytes, size.width, size.height);
        }
        camera.addCallbackBuffer(mCallbackBuffer);
    }
}
