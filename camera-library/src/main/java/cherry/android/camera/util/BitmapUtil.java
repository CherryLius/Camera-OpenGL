package cherry.android.camera.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.DisplayMetrics;

/**
 * Created by ROOT on 2017/8/9.
 */

public class BitmapUtil {
    private static final String TAG = "BitmapUtil";

    public static Bitmap decodeBitmap(Context context, byte[] bytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        CameraLog.d(TAG, "options: " + options.outWidth + "x" + options.outHeight);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int targetDensityDpi = dm.densityDpi;
        int expectW = dm.widthPixels;
        int expectH = dm.heightPixels;
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, expectW, expectH);

        double xScale = options.outWidth / (float) expectW;
        double yScale = options.outHeight / (float) expectH;
        CameraLog.d(TAG, "xScale=" + xScale + ",yScale=" + yScale
                + ",targetDensity=" + targetDensityDpi);

        options.inTargetDensity = targetDensityDpi;
        options.inJustDecodeBounds = false;
        Bitmap ret = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        return ret;
    }

    private static int calculateInSampleSize(int outWidth, int outHeight, int expectW, int expectH) {
        int inSampleSize = 1;
        if (outWidth > expectW || outHeight > expectH) {
            final int halfHeight = outWidth / 2;
            final int halfWidth = outHeight / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > expectH
                    && (halfWidth / inSampleSize) > expectW) {
                inSampleSize *= 2;
            }
        }
        CameraLog.i(TAG, "inSampleSize=" + inSampleSize);
        return inSampleSize;
    }

    public static void fileScan(Context context, String... paths) {
        MediaScannerConnection.scanFile(context, paths, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                CameraLog.d(TAG, "scan file !!!!!!!!!!!! " + path);
            }
        });
    }
}
