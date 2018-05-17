package cherry.android.camera.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;

import java.util.List;

import cherry.android.camera.PreviewCallback;
import cherry.android.camera.annotations.CameraId;
import cherry.android.camera.utils.CameraLog;

/**
 * Created by Administrator on 2017/4/6.
 */

public class CameraCompact {
    private static final String TAG = "CameraCompact";
    private ICamera mCamera;
    @CameraId
    private int mCameraId;

    public CameraCompact(Context context, SurfaceTexture texture) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
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
            CameraLog.e(TAG, "capture error", e);
        }
    }

    public void captureBurst() {
        mCamera.continuousCapture();
    }

    public void start(@CameraId int cameraId) {
        try {
            mCameraId = cameraId;
            mCamera.openCamera(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    public void stop() {
        mCamera.stopPreview();
        mCamera.closeCamera();
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

    public List<SizeExt> getSupportPreviewSizes() {
        return mCamera.getSupportPreviewSizes();
    }

}
