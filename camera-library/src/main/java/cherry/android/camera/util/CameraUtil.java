package cherry.android.camera.util;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by Administrator on 2017/4/10.
 */

public class CameraUtil {

    private static final String TAG = "CameraUtil";

    private static final double[] RATIOS = new double[]{1.3333, 1.5, 1.6667, 1.7778};
    private static final double ASPECT_TOLERANCE = 0.001;

    @NonNull
    public static <T> T getSystemService(Context context, @NonNull String serviceName) {
        return (T) context.getSystemService(serviceName);
    }

    public static double findFullscreenRatio(Context context, Size[] choiceSizes) {
        double find = 4d / 3;
        if (context != null && choiceSizes != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);

            double fullscreen;
            if (point.x > point.y) {
                fullscreen = (double) point.x / point.y;
            } else {
                fullscreen = (double) point.y / point.x;
            }
            Logger.i(TAG, "fullscreen = " + fullscreen + " x = " + point.x + " y = " + point.y);
            for (int i = 0; i < RATIOS.length; i++) {
                if (Math.abs(RATIOS[i] - fullscreen) < Math.abs(fullscreen - find)) {
                    find = RATIOS[i];
                }
            }
            for (Size size : choiceSizes) {
                if (toleranceRatio(find, (double) size.getWidth() / size.getHeight())) {
                    Logger.i(TAG, "findFullscreenRatio(" + choiceSizes + ") return " + find);
                    return find;
                }
            }
            find = 4d / 3;
        }
        Logger.d(TAG, "findFullscreenRatio(" + choiceSizes + ") return " + find);
        return find;
    }

    private static boolean toleranceRatio(double target, double candidate) {
        boolean tolerance = true;
        if (candidate > 0) {
            tolerance = Math.abs(target - candidate) <= ASPECT_TOLERANCE;
        }
        Logger.d(TAG, "toleranceRatio(" + target + ", " + candidate + ") return " + tolerance);
        return tolerance;
    }

    public static Size getOptimalPreviewSize(Context context, Size[] sizes, double targetRatio,
                                             boolean findMinalRatio) {
        // Use a very small tolerance because we want an exact match.
        // final double EXACTLY_EQUAL = 0.001;
        if (sizes == null) {
            return null;
        }

        Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        double minDiffWidth = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        WindowManager windowManager = getSystemService(context, Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int targetHeight = Math.min(point.x, point.y);
        int targetWidth = Math.max(point.x, point.y);
        if (findMinalRatio) {
            // Find minimal aspect ratio for that: special video size maybe not
            // have the mapping preview size.
            double minAspectio = Double.MAX_VALUE;
            for (Size size : sizes) {
                double aspectRatio = (double) size.getWidth() / size.getHeight();
                if (Math.abs(aspectRatio - targetRatio) <= Math.abs(minAspectio - targetRatio)) {
                    minAspectio = aspectRatio;
                }
            }
            Logger.d(TAG, "getOptimalPreviewSize(" + targetRatio + ") minAspectio=" + minAspectio);
            targetRatio = minAspectio;
        }

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
                minDiffWidth = Math.abs(size.getWidth() - targetWidth);
            } else if ((Math.abs(size.getHeight() - targetHeight) == minDiff)
                    && Math.abs(size.getWidth() - targetWidth) < minDiffWidth) {
                optimalSize = size;
                minDiffWidth = Math.abs(size.getWidth() - targetWidth);
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        // / M: This will happen when native return video size and wallpaper
        // want to get specified ratio.
        if (optimalSize == null) {
            Logger.w(TAG, "No preview size match the aspect ratio" + targetRatio + ","
                    + "then use the standard(4:3) preview size");
            minDiff = Double.MAX_VALUE;
            targetRatio = Double.parseDouble("1.3333");
            for (Size size : sizes) {
                double ratio = (double) size.getWidth() / size.getHeight();
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                    continue;
                }
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public static Size getOptimalPictureSize(Size[] sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        // final double ASPECT_TOLERANCE = 0.003;
        if (sizes == null)
            return null;

        Size optimalSize = null;

        // Try to find a size matches aspect ratio and has the largest width
        Size minSize = null;
        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (minSize == null) {
                minSize = size;
                optimalSize = size;
            } else if (size.getWidth() < minSize.getWidth()) {
                optimalSize = minSize;
                minSize = size;
            } else if (optimalSize == minSize
                    || size.getWidth() < optimalSize.getWidth()) {
                optimalSize = size;
            }
//            if (optimalSize == null || size.getWidth() < optimalSize.getWidth()) {
//                optimalSize = size;
//            }
        }

        // Cannot find one that matches the aspect ratio. This should not
        // happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            for (Size size : sizes) {
                if (optimalSize == null || size.getWidth() > optimalSize.getWidth()) {
                    optimalSize = size;
                }
            }
        }
        return optimalSize;
    }
}
