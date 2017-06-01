package cherry.android.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;

import java.io.IOException;
import java.util.List;

import cherry.android.camera.util.CameraUtil;

/**
 * Created by Administrator on 2017/4/6.
 */

public class Camera1 implements ICamera {
    private static final String TAG = "Camera1";

    private Context mContext;
    private int mCameraId;
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private Camera.CameraInfo mCameraInfo;

    private SurfaceTexture mSurfaceTexture;

    private Handler mBackgroundHandler;
    private ICameraCallback mCallback;

    public Camera1(Context context) {
        mContext = context;
        mCameraInfo = new Camera.CameraInfo();
        mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    }


    @Override
    public void openCamera(Handler handler) {
        mBackgroundHandler = handler;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCamera = Camera.open(mCameraId);

                    setupCameraParams();
                    mCamera.setParameters(mParameters);
                    mCamera.setPreviewTexture(mSurfaceTexture);
                    mCamera.startPreview();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void capture() {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                stopPreview();
                //mBackgroundHandler.post(new ImageSaver(mContext, data, Camera1.this));
                ImageManager.instance().execute(mContext, data, Camera1.this);
                //startPreview();
            }
        });
    }

    @Override
    public void startPreview() {
        if (mCamera != null)
            mCamera.startPreview();
    }

    @Override
    public void stopPreview() {
        if (mCamera != null)
            mCamera.stopPreview();
    }

    @Override
    public void setSurfaceTexture(SurfaceTexture texture) {
        mSurfaceTexture = texture;
    }

    @Override
    public int getOrientation() {
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        return mCameraInfo.orientation;
    }

    @Override
    public boolean isFrontCamera() {
        return mCameraId != Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    @Override
    public void setCameraCallback(ICameraCallback cb) {
        mCallback = cb;
    }

    @Override
    public ICameraCallback getCameraCallback() {
        return mCallback;
    }

    private void setupCameraParams() {
        mParameters = mCamera.getParameters();
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

        double screenRatio = CameraUtil.findFullscreenRatio(mContext,
                cameraSizesToArray(mParameters.getSupportedPictureSizes()));

        Size previewSize = CameraUtil.getOptimalPreviewSize(mContext,
                cameraSizesToArray(mParameters.getSupportedPreviewSizes()),
                screenRatio, false);
        mParameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());

        Size pictureSize = CameraUtil.getOptimalPictureSize(
                cameraSizesToArray(mParameters.getSupportedPictureSizes()),
                screenRatio);
        mParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        Log.e(TAG, "optimal pic size: " + pictureSize.getWidth() + "x" + pictureSize.getHeight());
    }

    @NonNull
    private Size[] cameraSizesToArray(@NonNull List<Camera.Size> sizes) {
        Size[] result = new Size[sizes.size()];
        for (int i = 0; i < sizes.size(); i++) {
            result[i] = cameraSizeToSize(sizes.get(i));
        }
        return result;
    }

    private static Size cameraSizeToSize(Camera.Size size) {
        return new Size(size.width, size.height);
    }
}
