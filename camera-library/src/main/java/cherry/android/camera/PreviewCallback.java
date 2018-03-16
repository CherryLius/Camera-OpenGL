package cherry.android.camera;

import cherry.android.camera.camera.ICamera;

/**
 * Created by ROOT on 2017/10/13.
 */

public interface PreviewCallback {
    void onPreview(ICamera iCamera, byte[] data, int width, int height);

    void onSizeChanged(int width, int height);
}
