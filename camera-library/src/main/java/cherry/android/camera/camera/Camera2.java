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
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.support.annotation.IntDef;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import cherry.android.camera.ImageManager;
import cherry.android.camera.PreviewCallback;
import cherry.android.camera.annotations.CameraId;
import cherry.android.camera.body.Camera2CaptureBody;
import cherry.android.camera.body.CaptureBody;
import cherry.android.camera.utils.CameraLog;
import cherry.android.camera.utils.CameraUtil;
import cherry.android.camera.utils.InternalCollections;
import ext.java8.function.Function;

/**
 * Created by Administrator on 2017/4/6.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2 extends AbstractCamera<CameraDevice> implements ICamera, ImageReader.OnImageAvailableListener {
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

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraLock = new Semaphore(1);
    private CameraManager mCameraManager;
    private ImageReader mPictureImageReader;
    private ImageReader mPreviewImageReader;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private int mImageFormat = ImageFormat.YUV_420_888;
    private int mPreviewWidth, mPreviewHeight;
    private PreviewCallback mPreviewCallback;
    @StateInternal
    private int mState = StateInternal.PREVIEW;
    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }

        private void process(CaptureResult result) {
            switch (mState) {
                case StateInternal.WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
                            || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null
                                || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = StateInternal.PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case StateInternal.WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null
                            || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE
                            || aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = StateInternal.WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case StateInternal.WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null
                            || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = StateInternal.PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
                case StateInternal.PREVIEW:
                    // We have nothing to do when the camera preview is working normally.
                default:
                    break;
            }
        }
    };
    private int mContinuous = -1;

    public Camera2(@NonNull Context context, @NonNull SurfaceTexture texture) {
        super(context, texture);
        mCameraManager = CameraUtil.getSystemService(context, Context.CAMERA_SERVICE);
    }

    private static int checkCameraId(@CameraId int cameraId) {
        switch (cameraId) {
            case CameraId.CAMERA_FRONT:
                return CameraCharacteristics.LENS_FACING_BACK;
            case CameraId.CAMERA_BACK:
                return CameraCharacteristics.LENS_FACING_FRONT;
            default:
                throw new IllegalStateException("cameraId is Unsupported. cameraId=" + cameraId);
        }
    }

//    @Override
//    public void setPreviewSize(int width, int height) {
//        mPreviewWidth = width;
//        mPreviewHeight = height;
//        if (mCameraDriver != null) {
//            stopPreview();
//            resolvePreviewSize();
//            mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
//            startPreview();
//        }
//    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Override
    public void openCamera(@CameraId int cameraId) {
        super.openCamera(cameraId);
        //0为后 1为前
        mRealCameraId = checkCameraId(cameraId);
        //ImageReader的Format由ImageFormat.JPEG->ImageFormat.YUV_420_888
        //连拍速度快
        List<SizeExt> sizes = getSupportPictureSizes();

        double screenRatio = CameraUtil.findFullscreenRatio(mContext, sizes);
        SizeExt picSize = CameraUtil.getOptimalPictureSize(sizes, screenRatio);
        CameraLog.i(TAG, "picture SizeExt: " + picSize.width() + "x" + picSize.height());

        mPictureImageReader = ImageReader.newInstance(picSize.width(), picSize.height(), mImageFormat, 2);
        mPictureImageReader.setOnImageAvailableListener(this, mCameraHandler);

        resolvePreviewSize();
        CameraLog.e(TAG, "previewOn: " + mPreviewWidth + "x" + mPreviewHeight);
        mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
        mPreviewImageReader = ImageReader.newInstance(mPreviewWidth, mPreviewHeight, mImageFormat, 2);
        mPreviewImageReader.setOnImageAvailableListener(this, mCameraHandler);

        try {
            if (!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraManager.openCamera(String.valueOf(mRealCameraId), new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraLock.release();
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
                    CameraLog.e(TAG, "open Camera err: " + error);
                }
            }, mCameraHandler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeCamera() {
        super.closeCamera();
        try {
            mCameraLock.acquire();
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
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraLock.release();
        }
    }

    @Override
    protected void onConfigureChanged(CameraConfiguration oldConfig, CameraConfiguration newConfig) {

    }

    @Override
    public void capture() {
        if (mCameraDriver == null || mCaptureSession == null) {
            CameraLog.e(TAG, "CameraDevice is null");
            return;
        }
        lockFocus();
    }

    @Override
    public void continuousCapture() {
        if (mCameraDriver == null || mCaptureSession == null) {
            CameraLog.e(TAG, "CameraDevice is null");
            return;
        }
        final int total = 10;
        mContinuous = 0;
        ArrayList<CaptureRequest> captureList = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            captureList.add(createCaptureRequest());
        }
        try {
//            mState = STATE_CAPTURE_BURST;
            mCaptureSession.stopRepeating();
            mCaptureSession.captureBurst(captureList, new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    int count = ++mContinuous;
                    CameraLog.e(TAG, "count=" + count);
                    if (count >= total) {
//                        if (mCallback != null) {
//                            mCallback.onBurstComplete();
//                        }
                        startPreview();
                        mContinuous = -1;
                    }
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            CameraLog.e(TAG, "CameraAccessException", e);
        }
    }

    private CaptureRequest createCaptureRequest() {
        // This is the CaptureRequest.Builder that we use to take a picture.
        try {
            CaptureRequest.Builder requestBuilder = mCameraDriver.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            requestBuilder.addTarget(mPictureImageReader.getSurface());
            // Use the same AE and AF modes as the preview.
            //自动对焦
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //闪光灯
            if (isFlashSupported()) {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }
            // Orientation
            WindowManager wm = CameraUtil.getSystemService(mContext, Context.WINDOW_SERVICE);
            int rotation = wm.getDefaultDisplay().getRotation();
            CameraLog.e(TAG, "display rotation=" + rotation);
            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            return requestBuilder.build();
        } catch (CameraAccessException e) {
            throw new RuntimeException("cannot create CaptureRequest.", e);
        }
    }

    @Override
    public void startPreview() {
        try {
            CameraLog.i(TAG, "startPreview");
            mState = StateInternal.PREVIEW;
            if (mCaptureSession != null && mPreviewRequest != null) {
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mCameraHandler);
                return;
            }

            mPreviewRequestBuilder = mCameraDriver.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            /*glSurface*/
            final Surface glSurface = new Surface(mSurfaceTexture);
            mPreviewRequestBuilder.addTarget(glSurface);

            /*preview Surface*/
            final Surface irSurface = mPreviewImageReader.getSurface();
            mPreviewRequestBuilder.addTarget(irSurface);

            mCameraDriver.createCaptureSession(Arrays.asList(glSurface, irSurface, mPictureImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (mCameraDriver == null) {
                                CameraLog.e(TAG, "CameraDevice is null");
                                return;
                            }
                            mCaptureSession = session;
                            try {
                                //自动对焦
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                //闪光灯
                                if (isFlashSupported()) {
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                }
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mCameraHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            CameraLog.i(TAG, "configure camera failed.");
                        }
                    }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {
        if (mCaptureSession == null) {
            CameraLog.i(TAG, "stop preview failure, no capture session");
            return;
        }
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = StateInternal.WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
//        mState = STATE_CAPTURE_ONCE;
        final CaptureRequest captureRequest = createCaptureRequest();
        try {
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    CameraLog.e(TAG, "onCaptureCompleted");
                    unlockFocus();
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            CameraLog.e(TAG, "captureStillPicture error", e);
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = StateInternal.WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            if (isFlashSupported()) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mCameraHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = StateInternal.PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getOrientation() {
        CameraLog.i(TAG, "getSensorOrientation=%d", getSensorOrientation());
        CameraLog.d(TAG, "getDisplayRotation=%d", CameraUtil.getDisplayRotation(mContext));
        CameraLog.i(TAG, "getOrientation ret=%d", (getSensorOrientation() + 270 - CameraUtil.getDisplayRotation(mContext)) % 360);
        return (getSensorOrientation() + 270 - CameraUtil.getDisplayRotation(mContext)) % 360;
    }

    @Override
    public void setPreviewCallback(PreviewCallback callback) {
        mPreviewCallback = callback;
    }

    @Override
    public List<SizeExt> getSupportPreviewSizes() {
        return InternalCollections.mapList(getSupportPreviewSizesInternal(), new Function<Size, SizeExt>() {
            @Override
            public SizeExt apply(Size size) {
                return new SizeExt(size.getWidth(), size.getHeight());
            }
        });
    }

    @Override
    public List<SizeExt> getSupportPictureSizes() {
        return InternalCollections.mapList(getSupportPictureSizesInternal(), new Function<Size, SizeExt>() {
            @Override
            public SizeExt apply(Size size) {
                return new SizeExt(size.getWidth(), size.getHeight());
            }
        });
    }

    private List<Size> getSupportPictureSizesInternal() {
        CameraCharacteristics characteristics = getCharacteristics();
        if (characteristics != null) {
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(map.getOutputSizes(mImageFormat));
        }
        return Collections.emptyList();
    }

    private List<Size> getSupportPreviewSizesInternal() {
        CameraCharacteristics characteristics = getCharacteristics();
        if (characteristics != null) {
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(map.getOutputSizes(SurfaceTexture.class));
        }
        return Collections.emptyList();
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        if (imageReader.equals(mPictureImageReader)) {
            CameraLog.e(TAG, "continuous=" + mContinuous);
            CaptureBody captureBody = new Camera2CaptureBody(mContext, mContinuous, imageReader.acquireNextImage());
            ImageManager.instance().execute(captureBody, this);
        } else {
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
        CameraLog.e(TAG, "getOrientation=" + ((ORIENTATIONS.get(rotation) + getSensorOrientation() + 270) % 360));
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
                CameraLog.e(TAG, " cameraId= " + cameraId);
                if (!cameraId.equals(mRealCameraId + "")) {
                    continue;
                }
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                return characteristics;
            }
        } catch (CameraAccessException e) {
            throw new IllegalArgumentException("Camera Access Exception", e);
        }
        return null;
    }

    private boolean isFlashSupported() {
        Boolean available = getCharacteristics().get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return available == null ? false : available;
    }

    private void resolvePreviewSize() {
        List<SizeExt> sizeExts = getSupportPreviewSizes();
        SizeExt previewSize;
        if (mPreviewWidth != 0 && mPreviewHeight != 0) {
            previewSize = CameraUtil.getOptimalPreviewSizeWithTarget(sizeExts,
                    mPreviewWidth, mPreviewHeight);
        } else {
            double screenRatio = CameraUtil.findFullscreenRatio(mContext, sizeExts);
            previewSize = CameraUtil.getOptimalPreviewSize(mContext, sizeExts, screenRatio, false);
        }
        mPreviewWidth = previewSize.width();
        mPreviewHeight = previewSize.height();
        if (mPreviewCallback != null) {
            mPreviewCallback.onSizeChanged(mPreviewWidth, mPreviewHeight);
        }
    }

    @IntDef({
            StateInternal.PREVIEW,
            StateInternal.WAITING_LOCK,
            StateInternal.WAITING_PRECAPTURE,
            StateInternal.WAITING_NON_PRECAPTURE,
            StateInternal.PICTURE_TAKEN,
    })
    private @interface StateInternal {
        int PREVIEW = 0;
        int WAITING_LOCK = 1;
        int WAITING_PRECAPTURE = 2;
        int WAITING_NON_PRECAPTURE = 3;
        int PICTURE_TAKEN = 4;
    }
}
