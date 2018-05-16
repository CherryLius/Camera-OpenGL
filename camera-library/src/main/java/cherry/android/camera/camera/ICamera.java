package cherry.android.camera.camera;

import java.util.List;

import cherry.android.camera.CaptureCallback;
import cherry.android.camera.PreviewCallback;
import cherry.android.camera.annotations.CameraId;

/**
 * Created by Administrator on 2017/4/6.
 */

public interface ICamera {

    void openCamera(@CameraId int cameraId);

    void closeCamera();

    void capture();

    void continuousCapture();

    void startPreview();

    void stopPreview();

    int getOrientation();

    void setCaptureCallback(CaptureCallback cb);

    void setPreviewCallback(PreviewCallback callback);

    List<SizeExt> getSupportPreviewSizes();

    List<SizeExt> getSupportPictureSizes();
}
