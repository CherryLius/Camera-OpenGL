package cherry.android.camera.source;

import android.support.annotation.NonNull;

public class YuvSource implements Source {
    private byte[] bytes;

    public YuvSource(byte[] bytes, boolean flipHorizontal) {
        this.bytes = bytes;
    }

    @NonNull
    @Override
    public byte[] sources() {
        return this.bytes;
    }
}
