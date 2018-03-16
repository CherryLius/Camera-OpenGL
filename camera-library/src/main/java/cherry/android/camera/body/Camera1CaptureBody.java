package cherry.android.camera.body;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;

import cherry.android.camera.camera.ICamera;

import static cherry.android.camera.util.BitmapUtil.decodeBitmap;

/**
 * Created by ROOT on 2017/8/9.
 */

public class Camera1CaptureBody implements CaptureBody {

    private Context context;
    private byte[] bytes;
    private Bitmap bitmap;
    private ICamera iCamera;
    private Matrix matrix;
    private int id = -1;

    public Camera1CaptureBody(@NonNull Context context, @NonNull byte[] bytes, @NonNull ICamera iCamera) {
        this.context = context;
        this.bytes = bytes;
        this.iCamera = iCamera;
    }

    public Camera1CaptureBody(@NonNull Context context, int id, @NonNull byte[] bytes, @NonNull ICamera iCamera) {
        this.context = context;
        this.bytes = bytes;
        this.iCamera = iCamera;
        this.id = id;
    }

    @Override
    public void transform() {
        if (iCamera.isFrontCamera()) {
            bitmap = rotateBitmap(BitmapFactory.decodeByteArray(this.bytes, 0, this.bytes.length));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            this.bytes = bos.toByteArray();
        } else {
            bitmap = decodeBitmap(context, this.bytes);
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

    private Bitmap rotateBitmap(@NonNull Bitmap bm) {
        if (matrix == null)
            matrix = new Matrix();
        matrix.reset();
        matrix.postRotate(180);
        matrix.postScale(-1, 1);
        Bitmap ret = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        return ret;
    }
}
