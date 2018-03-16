package cherry.android.camera.annotations;

/**
 * Created by ROOT on 2017/9/20.
 */

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@IntDef({CameraState.STATE_PREVIEW,
        CameraState.STATE_CAPTURE_ONCE,
        CameraState.STATE_CAPTURE_BURST})
@Retention(RetentionPolicy.SOURCE)
public @interface CameraState {
    int STATE_PREVIEW = 0;
    int STATE_CAPTURE_ONCE = 1;
    int STATE_CAPTURE_BURST = 2;
}
