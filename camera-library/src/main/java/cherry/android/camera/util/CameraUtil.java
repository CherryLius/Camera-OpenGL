package cherry.android.camera.util;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import cherry.android.camera.SizeSupplier;

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

    public static <T> double findFullscreenRatio(@NonNull Context context,
                                                 @NonNull T[] choiceSizes,
                                                 @NonNull SizeSupplier<T> supplier) {
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
            for (T size : choiceSizes) {
                if (toleranceRatio(find, (double) supplier.width(size) / supplier.height(size))) {
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

    public static <T> T getOptimalPreviewSize2(@NonNull Context context,
                                               T[] sizes,
                                               double targetRatio,
                                               boolean findMinalRatio,
                                               @NonNull SizeSupplier<T> supplier) {
        // Use a very small tolerance because we want an exact match.
        // final double EXACTLY_EQUAL = 0.001;
        if (sizes == null) {
            return null;
        }

        // Try to find an size match aspect ratio and size
        T max = null;
        for (T size : sizes) {
            if (max == null) {
                max = size;
            }
            final int width = supplier.width(size);
            final int height = supplier.height(size);
            final int maxW = supplier.width(max);
            final int maxH = supplier.height(max);
            if (maxW * maxH < width * height) {
                max = size;
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        // / M: This will happen when native return video size and wallpaper
        // want to get specified ratio.
        Logger.i(TAG, "max=" + supplier.width(max) + ", " + supplier.height(max));
        return max;
    }

    public static <T> T getOptimalPreviewSize(@NonNull Context context,
                                              T[] sizes,
                                              double targetRatio,
                                              boolean findMinalRatio,
                                              @NonNull SizeSupplier<T> supplier) {
        // Use a very small tolerance because we want an exact match.
        // final double EXACTLY_EQUAL = 0.001;
        if (sizes == null) {
            return null;
        }

        T optimalSize = null;

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
            for (T size : sizes) {
                double aspectRatio = (double) supplier.width(size) / supplier.height(size);
                if (Math.abs(aspectRatio - targetRatio) <= Math.abs(minAspectio - targetRatio)) {
                    minAspectio = aspectRatio;
                }
            }
            Logger.d(TAG, "getOptimalPreviewSize(" + targetRatio + ") minAspectio=" + minAspectio);
            targetRatio = minAspectio;
        }

        // Try to find an size match aspect ratio and size
        for (T size : sizes) {
            double ratio = (double) supplier.width(size) / supplier.height(size);
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(supplier.height(size) - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(supplier.height(size) - targetHeight);
                minDiffWidth = Math.abs(supplier.width(size) - targetWidth);
            } else if ((Math.abs(supplier.height(size) - targetHeight) == minDiff)
                    && Math.abs(supplier.width(size) - targetWidth) < minDiffWidth) {
                optimalSize = size;
                minDiffWidth = Math.abs(supplier.width(size) - targetWidth);
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
            for (T size : sizes) {
                double ratio = (double) supplier.width(size) / supplier.height(size);
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                    continue;
                }
                if (Math.abs(supplier.height(size) - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(supplier.height(size) - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public static <T> T getOptimalPreviewSizeWithTarget(T[] sizes,
                                                        int targetWidth,
                                                        int targetHeight,
                                                        @NonNull SizeSupplier<T> supplier) {
        // Use a very small tolerance because we want an exact match.
        // final double EXACTLY_EQUAL = 0.001;
        if (sizes == null) {
            return null;
        }

        T optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        double minDiffWidth = Double.MAX_VALUE;

        double targetRatio = Math.max(targetWidth, targetHeight) * 1.0d / Math.min(targetWidth, targetHeight);

        // Try to find an size match aspect ratio and size
        for (T size : sizes) {
            double ratio = (double) supplier.width(size) / supplier.height(size);
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(supplier.height(size) - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(supplier.height(size) - targetHeight);
                minDiffWidth = Math.abs(supplier.width(size) - targetWidth);
            } else if ((Math.abs(supplier.height(size) - targetHeight) == minDiff)
                    && Math.abs(supplier.width(size) - targetWidth) < minDiffWidth) {
                optimalSize = size;
                minDiffWidth = Math.abs(supplier.width(size) - targetWidth);
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
            for (T size : sizes) {
                double ratio = (double) supplier.width(size) / supplier.height(size);
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                    continue;
                }
                if (Math.abs(supplier.height(size) - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(supplier.height(size) - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public static <T> T getOptimalPictureSize(T[] sizes,
                                              double targetRatio,
                                              @NonNull SizeSupplier<T> supplier) {
        // Use a very small tolerance because we want an exact match.
        // final double ASPECT_TOLERANCE = 0.003;
        if (sizes == null)
            return null;

        T optimalSize = null;

        // Try to find a size matches aspect ratio and has the largest width
        T minSize = null;
        for (T size : sizes) {
            double ratio = (double) supplier.width(size) / supplier.height(size);
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (minSize == null) {
                minSize = size;
                optimalSize = size;
            } else if (supplier.width(size) < supplier.width(minSize)) {
                optimalSize = minSize;
                minSize = size;
            } else if (optimalSize == minSize
                    || supplier.width(size) < supplier.width(optimalSize)) {
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
            for (T size : sizes) {
                if (optimalSize == null || supplier.width(size) > supplier.width(optimalSize)) {
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
