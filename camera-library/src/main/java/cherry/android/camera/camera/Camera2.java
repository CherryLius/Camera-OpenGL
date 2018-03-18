package cherry.android.camera.camera;

import android.Manifest;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cherry.android.camera.CaptureCallback;
import cherry.android.camera.ImageManager;
import cherry.android.camera.PreviewCallback;
import cherry.android.camera.SizeSupplier;
import cherry.android.camera.annotations.CameraId;
import cherry.android.camera.annotations.CameraState;
import cherry.android.camera.body.Camera2CaptureBody;
import cherry.android.camera.body.CaptureBody;
import cherry.android.camera.util.CameraUtil;
import cherry.android.camera.util.Logger;

import static cherry.android.camera.annotations.CameraState.STATE_CAPTURE_BURST;
import static cherry.android.camera.annotations.CameraState.STATE_CAPTURE_ONCE;
import static cherry.android.camera.annotations.CameraState.STATE_PREVIEW;

/**
 * Created by Administrator on 2017/4/6.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
/*public*/ class Camera2 extends AbstractCamera<CameraDevice> implements ICamera, ImageReader.OnImageAvailableListener {
    private static final String TAG = "Camera2";

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    private CameraManager mCameraManager;
    private ImageReader mPictureImageReader;
    private ImageReader mPreviewImageReader;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private CaptureRequest mCaptureRequest;

    private CaptureCallback mCallback;
    private int mImageFormat = ImageFormat.YUV_420_888;
    private int mPreviewWidth, mPreviewHeight;

    private PreviewCallback mPreviewCallback;

    @CameraState
    private int mState = STATE_PREVIEW;
    private int mContinuous = -1;

    public Camera2(@NonNull Context context, @NonNull SurfaceTexture texture) {
        super(context, texture);
        mCameraManager = CameraUtil.getSystemService(context, Context.CAMERA_SERVICE);
    }


    @Override
    public void setPreviewSize(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        if (mCameraDriver != null) {
            stopPreview();
            resolvePreviewSize();
            mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
            startPreview();
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Override
    public void openCamera(@CameraId int cameraId) throws Exception {
        super.openCamera(cameraId);
        //0为后 1为前
        if (cameraId == CameraId.CAMERA_FRONT) {
            mRealCameraId = CameraCharacteristics.LENS_FACING_BACK;
        } else {
            mRealCameraId = CameraCharacteristics.LENS_FACING_FRONT;
        }
        //ImageReader的Format由ImageFormat.JPEG->ImageFormat.YUV_420_888
        //连拍速度快
        StreamConfigurationMap map = getCharacteristics().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(mImageFormat);

        double screenRatio = CameraUtil.findFullscreenRatio(mContext, sizes, mSupplier);
        Size picSize = CameraUtil.getOptimalPictureSize(sizes, screenRatio, mSupplier);
        Logger.i(TAG, "picture Size: " + picSize.getWidth() + "x" + picSize.getHeight());
        mPictureImageReader = ImageReader.newInstance(picSize.getWidth(), picSize.getHeight(), mImageFormat, 1);
        mPictureImageReader.setOnImageAvailableListener(this, mCameraHandler);

        resolvePreviewSize();
        Logger.e(TAG, "previewSize: " + mPreviewWidth + "x" + mPreviewHeight);
        this.mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
        mPreviewImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, mImageFormat, 2);
        mPreviewImageReader.setOnImageAvailableListener(this, mCameraHandler);

        mCameraManager.openCamera(mRealCameraId + "", new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mCameraDriver = camera;
                startPreview();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                closeCamera();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                closeCamera();
                Logger.e(TAG, "open Camera err: " + error);
            }
        }, mCameraHandler);
    }

    @Override
    public void closeCamera() {
        super.closeCamera();
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDriver != null) {
            mCameraDriver.close();
            mCameraDriver = null;
        }
        if (mPreviewImageReader != null) {
            mPreviewImageReader.close();
            mPreviewImageReader = null;
        }
        if (mPictureImageReader != null) {
            mPictureImageReader.close();
            mPictureImageReader = null;
        }
    }

    @Override
    public void capture() throws Exception {
        if (mCameraDriver == null || mCaptureSession == null) {
            Logger.e(TAG, "CameraDevice is null");
            return;
        }
        mState = STATE_CAPTURE_ONCE;
        if (mCaptureRequest != null) {
            mCaptureSession.capture(mCaptureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            }, mCameraHandler);
            return;
        }
        mCaptureRequest = createCaptureRequest();
        mCaptureSession.capture(mCaptureRequest, new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Logger.e(TAG, "onCaptureCompleted");
            }
        }, mCameraHandler);
    }

    @Override
    public void captureBurst() {
        if (mCameraDriver == null || mCaptureSession == null) {
            Logger.e(TAG, "CameraDevice is null");
            return;
        }
        final int total = 10;
        mContinuous = 0;
        ArrayList<CaptureRequest> captureList = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            captureList.add(createCaptureRequest());
        }
        try {
            mState = STATE_CAPTURE_BURST;
            mCaptureSession.stopRepeating();
            mCaptureSession.captureBurst(captureList, new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    int count = ++mContinuous;
                    Logger.e(TAG, "count=" + count);
                    if (count >= total) {
                        if (mCallback != null) {
                            mCallback.onBurstComplete();
                        }
                        startPreview();
                        mContinuous = -1;
                    }
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Logger.e(TAG, "CameraAccessException", e);
        }
    }

    private CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder requestBuilder = mCameraDriver.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            requestBuilder.addTarget(mPictureImageReader.getSurface());
            //自动对焦
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //闪光灯
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //rotation
            WindowManager wm = CameraUtil.getSystemService(mContext, Context.WINDOW_SERVICE);
            int rotation = wm.getDefaultDisplay().getRotation();
            Logger.e(TAG, "display rotation=" + rotation);
            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            return requestBuilder.build();
        } catch (CameraAccessException e) {
            throw new RuntimeException("cannot create CaptureRequest.", e);
        }
    }

    @Override
    public void startPreview() {
        try {
            Logger.i(TAG, "startPreview");
            mState = STATE_PREVIEW;
            if (mCaptureSession != null && mPreviewRequest != null) {
                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mCameraHandler);
                return;
            }
            final CaptureRequest.Builder requestBuilder = mCameraDriver.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            final Surface irSurface = mPreviewImageReader.getSurface();
            requestBuilder.addTarget(irSurface);

            final Surface glSurface = new Surface(mSurfaceTexture);
            requestBuilder.addTarget(glSurface);
            mCameraDriver.createCaptureSession(Arrays.asList(glSurface, irSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCameraDriver == null) {
                        Logger.e(TAG, "CameraDevice is null");
                        return;
                    }
                    mCaptureSession = session;
                    try {
                        //自动对焦
                        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //闪光灯
                        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        mPreviewRequest = requestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Logger.i(TAG, "configure camera failed.");
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {
        if (mCaptureSession == null) {
            Logger.i(TAG, "stop preview failure, no capture session");
            return;
        }
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getOrientation() {
        Logger.i(TAG, "getSensorOrientation=%d", getSensorOrientation());
        Logger.d(TAG, "getDisplayRotation=%d", CameraUtil.getDisplayRotation(mContext));
        Logger.i(TAG, "getOrientation ret=%d", (getSensorOrientation() + 270 - CameraUtil.getDisplayRotation(mContext)) % 360);
        return (getSensorOrientation() + 270 - CameraUtil.getDisplayRotation(mContext)) % 360;
    }

    @Override
    public int getState() {
        return mState;
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
        mPreviewCallback = callback;
    }

    @Override
    public List<int[]> getSupportPreviewSizes() {
        List<int[]> supportedSizes = null;
        Size[] sizes = getSupportPreviewSizesInternal();
        if (sizes != null) {
            supportedSizes = new ArrayList<>(sizes.length);
            for (Size size : sizes) {
                int[] sizeArr = new int[]{mSupplier.width(size), mSupplier.height(size)};
                supportedSizes.add(sizeArr);
            }
        }
        return supportedSizes;
    }

    private Size[] getSupportPreviewSizesInternal() {
        CameraCharacteristics characteristics = getCharacteristics();
        if (characteristics != null) {
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return map.getOutputSizes(SurfaceTexture.class);
        }
        return null;
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
//        stopPreview();
        if (imageReader.equals(mPictureImageReader)) {
            Logger.e(TAG, "continuous=" + mContinuous);
            CaptureBody captureBody = new Camera2CaptureBody(mContext, mContinuous, imageReader.acquireNextImage());
            ImageManager.instance().execute(captureBody, this);
        } else {
            Logger.v(TAG, "preview available " + Thread.currentThread().getName());
            Image image = imageReader.acquireLatestImage();
            if (mPreviewCallback != null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                mPreviewCallback.onPreview(this, data, mPreviewWidth, mPreviewHeight);
            }
            image.close();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        Logger.e(TAG, "getPicOrientation=" + ((ORIENTATIONS.get(rotation) + getSensorOrientation() + 270) % 360));
        return (ORIENTATIONS.get(rotation) + getSensorOrientation() + 270) % 360;
    }

    private int getSensorOrientation() {
        int orientation = 0;
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mRealCameraId + "");
            orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return orientation;
    }

    private CameraCharacteristics getCharacteristics() {
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                Logger.e(TAG, " cameraId= " + cameraId);
                if (!cameraId.equals(mRealCameraId + "")) {
                    continue;
                }
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                return characteristics;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void resolvePreviewSize() {
        Size[] sizes = getSupportPreviewSizesInternal();
        Size previewSize;
        if (mPreviewWidth != 0 && mPreviewHeight != 0) {
            previewSize = CameraUtil.getOptimalPreviewSizeWithTarget(sizes,
                    mPreviewWidth, mPreviewHeight, mSupplier);
        } else {
            double screenRatio = CameraUtil.findFullscreenRatio(mContext, sizes, mSupplier);
            previewSize = CameraUtil.getOptimalPreviewSize(mContext, sizes, screenRatio, false, mSupplier);
        }
        mPreviewWidth = previewSize.getWidth();
        mPreviewHeight = previewSize.getHeight();
        if (mPreviewCallback != null) {
            mPreviewCallback.onSizeChanged(mPreviewWidth, mPreviewHeight);
        }
    }

    private SizeSupplier<Size> mSupplier = new SizeSupplier<Size>() {
        @Override
        public int width(Size size) {
            return size.getWidth();
        }

        @Override
        public int height(Size size) {
            return size.getHeight();
        }
    };
}
