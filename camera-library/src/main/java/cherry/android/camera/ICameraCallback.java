package cherry.android.camera;

import android.graphics.Bitmap;

/**
 * Created by Administrator on 2017/4/7.
 */

public interface ICameraCallback {

    void onCaptured(Bitmap bitmap, byte[] data);

    void onPictureSaved(Bitmap bitmap, String filePath);
}
