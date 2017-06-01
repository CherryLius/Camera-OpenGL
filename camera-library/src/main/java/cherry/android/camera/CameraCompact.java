package cherry.android.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by Administrator on 2017/4/6.
 */

public class CameraCompact {
    private ICamera mFunction;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    public CameraCompact(Context context) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mFunction = new Camera1(context);
        } else {
            mFunction = new Camera2(context);
        }
    }

    public void setSurfaceTexture(SurfaceTexture texture) {
        mFunction.setSurfaceTexture(texture);
    }

    public void capture() {
        mFunction.capture();
    }

    public void start() {
        startBackgroundThread();
        mFunction.openCamera(mBackgroundHandler);
    }

    public void stop() {
        mFunction.stopPreview();
        mFunction.closeCamera();
        stopBackgroundThread();
    }

    public void startPreview() {
        mFunction.startPreview();
    }

    public void setCallback(ICameraCallback callback) {
        mFunction.setCameraCallback(callback);
    }

    public int getOrientation() {
        return mFunction.getOrientation();
    }

    public boolean isFrontCamera() {
        return mFunction.isFrontCamera();
    }

    public ICamera getFunction() {
        return mFunction;
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mBackgroundThread.quitSafely();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }
}
