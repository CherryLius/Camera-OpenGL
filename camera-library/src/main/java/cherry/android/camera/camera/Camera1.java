package cherry.android.camera.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import cherry.android.camera.CaptureCallback;
import cherry.android.camera.ImageManager;
import cherry.android.camera.PreviewCallback;
import cherry.android.camera.annotations.CameraId;
import cherry.android.camera.annotations.CameraState;
import cherry.android.camera.body.Camera1CaptureBody;
import cherry.android.camera.utils.CameraLog;
import cherry.android.camera.utils.CameraUtil;
import cherry.android.camera.utils.InternalCollections;
import ext.java8.function.Function;

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

    private static byte[] calculateBuffer(int width, int height) {
        final int size = (width * height) * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
        CameraLog.e(TAG, "open callback buffer size=" + size);
        return new byte[size];
    }

    private static int checkCameraId(@CameraId int cameraId) {
        switch (cameraId) {
            case CameraId.CAMERA_FRONT:
                return Camera.CameraInfo.CAMERA_FACING_FRONT;
            case CameraId.CAMERA_BACK:
                return Camera.CameraInfo.CAMERA_FACING_BACK;
            default:
                throw new IllegalStateException("cameraId is Unsupported. cameraId=" + cameraId);
        }
    }

    @Override
    public void openCamera(@CameraId int cameraId) {
        super.openCamera(cameraId);
        mRealCameraId = checkCameraId(cameraId);

        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCameraLock.lock();
                    CameraLog.i(TAG, "openCamera");
                    mCameraDriver = Camera.open(mRealCameraId);

                    mParameters = mCameraDriver.getParameters();
                    setupCameraParams();
                    mCameraDriver.setParameters(mParameters);
                    Camera.Size previewSize = mParameters.getPreviewSize();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        mSurfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height);
                    }
                    mCameraDriver.setPreviewTexture(mSurfaceTexture);
                    mCallbackBuffer = calculateBuffer(previewSize.width, previewSize.height);
                    //preview onPreview
                    mCameraDriver.addCallbackBuffer(mCallbackBuffer);
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
        super.closeCamera();
        if (mCameraDriver != null) {
            mCameraLock.lock();
            CameraLog.i(TAG, "releaseCamera");
            mCameraDriver.setPreviewCallbackWithBuffer(null);
            mCameraDriver.setPreviewCallback(null);
            mCameraDriver.release();
            mCameraDriver = null;
            mParameters = null;
            mCameraLock.unlock();
        }
    }

    private void resolvePreviewSize() {
        final List<SizeExt> supportPreviewSizes = getSupportPreviewSizes();
        for (SizeExt size : supportPreviewSizes) {
            CameraLog.e(TAG, "support preview size=" + size.width() + "x" + size.height());
        }

        SizeExt previewOn;
        if (mConfiguration != null && mConfiguration.getPreviewSizeExt() != null) {
            final SizeExt sizeExt = mConfiguration.getPreviewSizeExt();
            previewOn = CameraUtil.getOptimalPreviewSizeWithTarget(supportPreviewSizes,
                    sizeExt.width(),
                    sizeExt.height());
        } else {
            double screenRatio = CameraUtil.findFullscreenRatio(mContext, getSupportPictureSizes());
            previewOn = CameraUtil.getOptimalPreviewSize(mContext, supportPreviewSizes, screenRatio, false);
        }
        mParameters.setPreviewSize(previewOn.width(), previewOn.height());
        Log.e(TAG, "optimal preview size: " + previewOn.width() + "x" + previewOn.height());
        if (mPreviewCallback != null) {
            mPreviewCallback.onSizeChanged(previewOn.width(), previewOn.height());
        }
    }

    @Override
    protected void onConfigureChanged(CameraConfiguration oldConfig, CameraConfiguration newConfig) {
        resolvePreviewChange(oldConfig.getPreviewSizeExt(), newConfig.getPreviewSizeExt());
    }

    private void resolvePreviewChange(SizeExt oldSizeExt, SizeExt newSizeExt) {
        if (mCameraDriver == null) {
            CameraLog.w(TAG, "camera not ready.");
            return;
        }
        if (oldSizeExt == null && newSizeExt == null) {
            CameraLog.w(TAG, "skip with invalid size.");
            return;
        }
        if (oldSizeExt != null && newSizeExt != null) {
            Camera.Size previewSize = mParameters.getPreviewSize();
            final int width = previewSize.width;
            final int height = previewSize.height;
            if ((oldSizeExt.width() == newSizeExt.width() && oldSizeExt.height() == newSizeExt.height())
                    || (newSizeExt.width() == width && newSizeExt.height() == height)) {
                CameraLog.w(TAG, "same preview size. skip: " + width + "x" + height);
                return;
            }
        }
        mCameraDriver.setPreviewCallbackWithBuffer(null);
        stopPreview();
        resolvePreviewSize();
        mCameraDriver.setParameters(mParameters);
        Camera.Size previewOn = mParameters.getPreviewSize();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mSurfaceTexture.setDefaultBufferSize(previewOn.width, previewOn.height);
        }
        mCallbackBuffer = calculateBuffer(previewOn.width, previewOn.height);
        mCameraDriver.addCallbackBuffer(mCallbackBuffer);
        mCameraDriver.setPreviewCallbackWithBuffer(this);
        startPreview();
    }

    @Override
    public void capture() {
        mState = STATE_CAPTURE_ONCE;
        capture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                stopPreview();
                ImageManager.instance().execute(new Camera1CaptureBody(mContext, data), Camera1.this);
            }
        });
    }


    @Override
    public void continuousCapture() {
        final int count = 10;
        mContinuous = 0;
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                mState = STATE_CAPTURE_BURST;
                capture(new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {
                        stopPreview();
                        CameraLog.e(TAG, Thread.currentThread().getName());
                        int index = mContinuous++;
                        ImageManager.instance().execute(new Camera1CaptureBody(mContext, index, bytes), Camera1.this);
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
        CameraLog.e(TAG, "orientation=" + mCameraInfo.orientation);
        CameraLog.d(TAG, "getDisplayRotation=" + CameraUtil.getDisplayRotation(mContext));
        CameraLog.v(TAG, "getDisplayOrientation=" + getDisplayOrientation(CameraUtil.getDisplayRotation(mContext), mRealCameraId));
        return (mCameraInfo.orientation - CameraUtil.getDisplayRotation(mContext) + 360) % 360;
    }

    @Override
    public void setCaptureCallback(CaptureCallback cb) {
        mCallback = cb;
    }

    @Override
    public void setPreviewCallback(PreviewCallback callback) {
        this.mPreviewCallback = callback;
    }

    @Override
    public List<SizeExt> getSupportPreviewSizes() {
        if (mParameters == null) {
            CameraLog.w(TAG, "camera not ready.");
            return Collections.emptyList();
        }
        return InternalCollections.mapList(mParameters.getSupportedPreviewSizes(), new Function<Camera.Size, SizeExt>() {
            @Override
            public SizeExt apply(Camera.Size size) {
                return new SizeExt(size.width, size.height);
            }
        });
    }

    @Override
    public List<SizeExt> getSupportPictureSizes() {
        if (mParameters == null) {
            CameraLog.w(TAG, "camera not ready.");
            return Collections.emptyList();
        }
        return InternalCollections.mapList(mParameters.getSupportedPictureSizes(), new Function<Camera.Size, SizeExt>() {
            @Override
            public SizeExt apply(Camera.Size size) {
                return new SizeExt(size.width, size.height);
            }
        });
    }

    private void setupCameraParams() {
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
        final List<SizeExt> supportPictureSizes = getSupportPictureSizes();

        double screenRatio = CameraUtil.findFullscreenRatio(mContext, supportPictureSizes);

        for (SizeExt size : supportPictureSizes) {
            CameraLog.e(TAG, "support picture size=" + size.width() + "x" + size.height());
        }
        //预览尺寸
        resolvePreviewSize();
        //照片尺寸
        SizeExt pictureSize = CameraUtil.getOptimalPictureSize(supportPictureSizes, screenRatio);
        mParameters.setPictureSize(pictureSize.width(), pictureSize.height());
        Log.e(TAG, "optimal pic size: " + pictureSize.width() + "x" + pictureSize.height());
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (mPreviewCallback != null && camera != null) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            mPreviewCallback.onPreview(this, bytes, size.width, size.height);
        }
        camera.addCallbackBuffer(mCallbackBuffer);
    }
}
