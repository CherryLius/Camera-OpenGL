package cherry.android.camera.body;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Build;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

import static cherry.android.camera.utils.BitmapUtil.decodeBitmap;

/**
 * Created by ROOT on 2017/8/9.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2CaptureBody implements CaptureBody {
    private Context context;
    private Image image;
    private Bitmap bitmap;
    private byte[] bytes;
    private int id;

    public Camera2CaptureBody(@NonNull Context context,
                              @NonNull Image image) {
        this.context = context;
        this.image = image;
    }

    public Camera2CaptureBody(@NonNull Context context,
                              int id,
                              @NonNull Image image) {
        this.context = context;
        this.image = image;
        this.id = id;
    }

    @Override
    public void transform() {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            this.bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            bitmap = decodeBitmap(context, this.bytes);
        } finally {
            image.close();
        }
    }

    @NonNull
    @Override
    public byte[] getBytes() {
        return this.bytes;
    }

    @NonNull
    @Override
    public Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    public int getId() {
        return id;
    }
}
