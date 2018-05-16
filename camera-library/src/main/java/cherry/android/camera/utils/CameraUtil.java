package cherry.android.camera.utils;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.List;

import cherry.android.camera.camera.SizeExt;

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

    public static double findFullscreenRatio(@NonNull Context context,
                                             @NonNull List<SizeExt> choiceSizes) {
        double find = 4d / 3;
        if (context != null && choiceSizes != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point point = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(point);
            } else {
                display.getSize(point);
            }

            double fullscreen;
            if (point.x > point.y) {
                fullscreen = (double) point.x / point.y;
            } else {
                fullscreen = (double) point.y / point.x;
            }
            CameraLog.i(TAG, "fullscreen = " + fullscreen + " x = " + point.x + " y = " + point.y);
            for (int i = 0; i < RATIOS.length; i++) {
                if (Math.abs(RATIOS[i] - fullscreen) < Math.abs(fullscreen - find)) {
                    find = RATIOS[i];
                }
            }
            for (SizeExt size : choiceSizes) {
                if (toleranceRatio(find, (double) size.width() / size.height())) {
                    CameraLog.i(TAG, "findFullscreenRatio(" + choiceSizes + ") return " + find);
                    return find;
                }
            }
            find = 4d / 3;
        }
        CameraLog.d(TAG, "findFullscreenRatio(" + choiceSizes + ") return " + find);
        return find;
    }

    private static boolean toleranceRatio(double target, double candidate) {
        boolean tolerance = true;
        if (candidate > 0) {
            tolerance = Math.abs(target - candidate) <= ASPECT_TOLERANCE;
        }
        CameraLog.d(TAG, "toleranceRatio(" + target + ", " + candidate + ") return " + tolerance);
        return tolerance;
    }

    public static SizeExt getOptimalPreviewSize2(@NonNull Context context,
                                                 List<SizeExt> sizes,
                                                 double targetRatio,
                                                 boolean findMinalRatio) {
        // Use a very small tolerance because we want an exact match.
        // final double EXACTLY_EQUAL = 0.001;
        if (sizes == null) {
            return null;
        }

        // Try to find an size match aspect ratio and size
        SizeExt max = null;
        for (SizeExt size : sizes) {
            if (max == null) {
                max = size;
            }
            final int width = size.width();
            final int height = size.height();
            final int maxW = max.width();
            final int maxH = max.height();
            if (maxW * maxH < width * height) {
                max = size;
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        // / M: This will happen when native return video size and wallpaper
        // want to get specified ratio.
        CameraLog.i(TAG, "max=" + max.width() + ", " + max.height());
        return max;
    }

    public static SizeExt getOptimalPreviewSize(@NonNull Context context,
                                                List<SizeExt> sizes,
                                                double targetRatio,
                                                boolean findMinalRatio) {
        // Use a very small tolerance because we want an exact match.
        // final double EXACTLY_EQUAL = 0.001;
        if (sizes == null) {
            return null;
        }

        SizeExt optimalSize = null;

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
            for (SizeExt size : sizes) {
                double aspectRatio = (double) size.width() / size.height();
                if (Math.abs(aspectRatio - targetRatio) <= Math.abs(minAspectio - targetRatio)) {
                    minAspectio = aspectRatio;
                }
            }
            CameraLog.d(TAG, "getOptimalPreviewSize(" + targetRatio + ") minAspectio=" + minAspectio);
            targetRatio = minAspectio;
        }

        // Try to find an size match aspect ratio and size
        for (SizeExt size : sizes) {
            double ratio = (double) size.width() / size.height();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(size.height() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height() - targetHeight);
                minDiffWidth = Math.abs(size.width() - targetWidth);
            } else if ((Math.abs(size.height() - targetHeight) == minDiff)
                    && Math.abs(size.width() - targetWidth) < minDiffWidth) {
                optimalSize = size;
                minDiffWidth = Math.abs(size.width() - targetWidth);
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        // / M: This will happen when native return video size and wallpaper
        // want to get specified ratio.
        if (optimalSize == null) {
            CameraLog.w(TAG, "No preview size match the aspect ratio" + targetRatio + ","
                    + "then use the standard(4:3) preview size");
            minDiff = Double.MAX_VALUE;
            targetRatio = Double.parseDouble("1.3333");
            for (SizeExt size : sizes) {
                double ratio = (double) size.width() / size.height();
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                    continue;
                }
                if (Math.abs(size.height() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public static SizeExt getOptimalPreviewSizeWithTarget(List<SizeExt> sizes,
                                                          int targetWidth,
                                                          int targetHeight) {
        // Use a very small tolerance because we want an exact match.
        // final double EXACTLY_EQUAL = 0.001;
        if (sizes == null) {
            return null;
        }

        SizeExt optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        double minDiffWidth = Double.MAX_VALUE;

        double targetRatio = Math.max(targetWidth, targetHeight) * 1.0d / Math.min(targetWidth, targetHeight);

        // Try to find an size match aspect ratio and size
        for (SizeExt size : sizes) {
            double ratio = (double) size.width() / size.height();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(size.height() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height() - targetHeight);
                minDiffWidth = Math.abs(size.width() - targetWidth);
            } else if ((Math.abs(size.height() - targetHeight) == minDiff)
                    && Math.abs(size.width() - targetWidth) < minDiffWidth) {
                optimalSize = size;
                minDiffWidth = Math.abs(size.width() - targetWidth);
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        // / M: This will happen when native return video size and wallpaper
        // want to get specified ratio.
        if (optimalSize == null) {
            CameraLog.w(TAG, "No preview size match the aspect ratio" + targetRatio + ","
                    + "then use the standard(4:3) preview size");
            minDiff = Double.MAX_VALUE;
            targetRatio = Double.parseDouble("1.3333");
            for (SizeExt size : sizes) {
                double ratio = (double) size.width() / size.height();
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                    continue;
                }
                if (Math.abs(size.height() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public static SizeExt getOptimalPictureSize(List<SizeExt> sizes,
                                                double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        // final double ASPECT_TOLERANCE = 0.003;
        if (sizes == null)
            return null;

        SizeExt optimalSize = null;

        // Try to find a size matches aspect ratio and has the largest width
        SizeExt minSize = null;
        for (SizeExt size : sizes) {
            double ratio = (double) size.width() / size.height();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (minSize == null) {
                minSize = size;
                optimalSize = size;
            } else if (size.width() < minSize.width()) {
                optimalSize = minSize;
                minSize = size;
            } else if (optimalSize == minSize
                    || size.width() < optimalSize.width()) {
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
            for (SizeExt size : sizes) {
                if (optimalSize == null || size.width() > optimalSize.width()) {
                    optimalSize = size;
                }
            }
        }
        return optimalSize;
    }

    public static int getDisplayRotation(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }
}
