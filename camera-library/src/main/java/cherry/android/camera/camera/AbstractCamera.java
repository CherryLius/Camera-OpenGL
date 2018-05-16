package cherry.android.camera.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

/**
 * Created by roothost on 2018/3/16.
 */

public abstract class AbstractCamera<T> implements ICamera {
    protected final Context mContext;
    protected final SurfaceTexture mSurfaceTexture;
    protected int mRealCameraId;
    protected T mCameraDriver;
    protected Handler mCameraHandler;
    protected CameraConfiguration mConfiguration;
    private HandlerThread mBackgroundThread;

    public AbstractCamera(@NonNull Context context, @NonNull SurfaceTexture texture) {
        this.mContext = context;
        this.mSurfaceTexture = texture;
    }

    @CallSuper
    @Override
    public void openCamera(int cameraId) {
        startBackgroundThread();
    }

    @CallSuper
    @Override
    public void closeCamera() {
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        stopBackgroundThread();
        mBackgroundThread = new HandlerThread("Camera");
        mBackgroundThread.start();
        mCameraHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null && mCameraHandler != null) {
            mCameraHandler.removeCallbacksAndMessages(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundThread.quitSafely();
            } else {
                mBackgroundThread.quit();
            }
            try {
                mBackgroundThread.join(500L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mBackgroundThread = null;
            mCameraHandler = null;
        }
    }

    public void apply(CameraConfiguration configuration) {
        final CameraConfiguration oldConfiguration = this.mConfiguration;
        this.mConfiguration = configuration;
        onConfigureChanged(oldConfiguration, configuration);
    }

    protected abstract void onConfigureChanged(CameraConfiguration oldConfig, CameraConfiguration newConfig);

}
