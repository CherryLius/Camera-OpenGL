package cherry.android.camera.annotations;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({CameraId.CAMERA_FRONT,
        CameraId.CAMERA_BACK})
@Retention(RetentionPolicy.SOURCE)
public @interface CameraId {
    int CAMERA_FRONT = 0;
    int CAMERA_BACK = 1;
}
