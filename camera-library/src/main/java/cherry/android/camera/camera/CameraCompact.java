package cherry.android.camera.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.List;

import cherry.android.camera.CaptureCallback;
import cherry.android.camera.PreviewCallback;
import cherry.android.camera.annotations.CameraId;

/**
 * Created by Administrator on 2017/4/6.
 */

public class CameraCompact {
    private ICamera mCamera;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    @CameraId
    private int mCameraId;

    public CameraCompact(Context context, SurfaceTexture texture) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mCamera = new Camera1(context, texture);
        } else {
            mCamera = new Camera2(context, texture);
        }
    }

    public void capture() {
        try {
            mCamera.capture();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureBurst() {
        mCamera.captureBurst();
    }

    public void setPreviewSize(final int width, final int height) {
        mCamera.setPreviewSize(width, height);
    }

    public void start(@CameraId int cameraId) {
        try {
            mCameraId = cameraId;
            startBackgroundThread();
            mCamera.openCamera(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    public void stop() {
        mCamera.stopPreview();
        mCamera.closeCamera();
        stopBackgroundThread();
    }

    public void startPreview() {
        mCamera.startPreview();
    }

    public void setCallback(CaptureCallback callback) {
        mCamera.setCaptureCallback(callback);
    }

    public void setPreviewCallback(PreviewCallback callback) {
        mCamera.setPreviewCallback(callback);
    }

    public int getOrientation() {
        return mCamera.getOrientation();
    }

    public boolean isFrontCamera() {
        return mCameraId == CameraId.CAMERA_FRONT;
    }

    public ICamera getCamera() {
        return mCamera;
    }

    public List<int[]> getSupportPreviewSizes() {
        return mCamera.getSupportPreviewSizes();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundThread.quitSafely();
            } else {
                mBackgroundThread.quit();
            }
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }
}
