package cherry.android.camera.camera;

import java.util.List;

import cherry.android.camera.CaptureCallback;
import cherry.android.camera.PreviewCallback;
import cherry.android.camera.annotations.CameraId;
import cherry.android.camera.annotations.CameraState;

/**
 * Created by Administrator on 2017/4/6.
 */

public interface ICamera {

    void setPreviewSize(int width, int height);

    void openCamera(@CameraId int cameraId) throws Exception;

    void closeCamera();

    void capture() throws Exception;

    void captureBurst();

    void startPreview();

    void stopPreview();

    int getOrientation();

    @CameraState
    int getState();

    void setCaptureCallback(CaptureCallback cb);

    CaptureCallback getCaptureCallback();

    void setPreviewCallback(PreviewCallback callback);

    List<int[]> getSupportPreviewSizes();
}
