package cherry.android.camera;

import android.support.annotation.NonNull;

import cherry.android.camera.body.CaptureBody;

/**
 * Created by Administrator on 2017/4/7.
 */

public interface CaptureCallback {

    void onCaptured(@NonNull CaptureBody captureBody);

    void onBurstComplete();

    void onPictureSaved(@NonNull CaptureBody captureBody);
}
