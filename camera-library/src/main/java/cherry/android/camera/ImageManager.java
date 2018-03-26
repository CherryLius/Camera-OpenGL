package cherry.android.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cherry.android.camera.body.CaptureBody;
import cherry.android.camera.camera.ICamera;
import cherry.android.camera.provider.DefaultProvider;
import cherry.android.camera.provider.IProvider;
import cherry.android.camera.util.CameraLog;

import static cherry.android.camera.util.BitmapUtil.fileScan;

/**
 * Created by Administrator on 2017/4/26.
 */

public class ImageManager {

    private static final String TAG = "ImageManager";

    private static ImageManager sInstance;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    public static ImageManager instance() {
        if (sInstance == null)
            synchronized (ImageManager.class) {
                if (sInstance == null)
                    sInstance = new ImageManager();
            }
        return sInstance;
    }

    private ImageManager() {
        startBackgroundThread();
    }

    public void execute(@NonNull final CaptureBody captureBody, @NonNull final ICamera iCamera) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                captureBody.transform();
                if (iCamera.getCaptureCallback() != null) {
                    iCamera.getCaptureCallback().onCaptured(captureBody);
                }
            }
        });
    }

    public void saveImage(@NonNull final Context context,
                          @NonNull CaptureBody captureBody,
                          @NonNull final ICamera iCamera) {
        saveImage(context, captureBody, iCamera, new DefaultProvider(context));
    }

    public void saveImage(@NonNull final Context context,
                          @NonNull final CaptureBody captureBody,
                          @NonNull final ICamera iCamera,
                          final IProvider provider) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (provider == null) {
                    CameraLog.e(TAG, "No Provider to get filename.");
                    return;
                }
                File file = new File(provider.filename());
                CameraLog.i(TAG, "file=" + file);
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    outputStream.write(captureBody.getBytes());
                    outputStream.flush();

                    fileScan(context, file.getAbsolutePath());
                    if (iCamera.getCaptureCallback() != null) {
                        iCamera.getCaptureCallback().onPictureSaved(captureBody);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    CameraLog.e(TAG, "FileNotFound", e);
                } catch (IOException e) {
                    e.printStackTrace();
                    CameraLog.e(TAG, "IOException", e);
                }
            }
        });
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("image");
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

    public void release() {
        stopBackgroundThread();
        sInstance = null;
    }
}
