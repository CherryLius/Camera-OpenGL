package cherry.android.camera.body;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

/**
 * Created by ROOT on 2017/8/9.
 */

public interface CaptureBody {

    void transform();

    @NonNull
    byte[] getBytes();

    @NonNull
    Bitmap getBitmap();

    int getId();
}
