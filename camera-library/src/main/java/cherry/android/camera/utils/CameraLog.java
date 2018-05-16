package cherry.android.camera.utils;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Administrator on 2017/5/26.
 */

public final class CameraLog {
    private static String sTagPrefix = "CompactCamera.";
    private static boolean sLoggable = true;
    private static boolean sTraceStack = true;

    public static void setPrefixTag(String prefix) {
        if (!TextUtils.isEmpty(prefix)) {
            sTagPrefix = prefix;
        }
    }

    public static void showLog(boolean show) {
        sLoggable = show;
    }

    public static void showStackTrace(boolean show) {
        sTraceStack = show;
    }

    public static void i(String tag, String msg) {
        if (sLoggable)
            Log.i(sTagPrefix + tag, buildMessage(msg));
    }

    public static void i(String tag, String format, Object... args) {
        i(tag, String.format(format, args));
    }

    public static void d(String tag, String msg) {
        if (sLoggable)
            Log.d(sTagPrefix + tag, buildMessage(msg));
    }

    public static void d(String tag, String format, Object... args) {
        d(tag, String.format(format, args));
    }

    public static void v(String tag, String msg) {
        if (sLoggable)
            Log.v(sTagPrefix + tag, buildMessage(msg));
    }

    public static void v(String tag, String format, Object... args) {
        v(tag, String.format(format, args));
    }

    public static void w(String tag, String msg) {
        if (sLoggable)
            Log.w(sTagPrefix + tag, buildMessage(msg));
    }

    public static void w(String tag, String format, Object... args) {
        w(tag, String.format(format, args));
    }

    public static void e(String tag, String msg) {
        if (sLoggable)
            Log.e(sTagPrefix + tag, buildMessage(msg));
    }

    public static void e(String tag, String format, Object... args) {
        e(tag, String.format(format, args));
    }

    public static void e(String tag, String msg, Throwable t) {
        if (sLoggable)
            Log.e(sTagPrefix + tag, buildMessage(msg), t);
    }

    private static String buildMessage(String msg) {
        if (!sTraceStack) return msg;
        Throwable t = new Throwable();
        StackTraceElement[] stackElements = t.getStackTrace();
        if (stackElements != null) {
            StringBuilder sb = new StringBuilder();
            String className = stackElements[2].getClassName();
            String methodName = stackElements[2].getMethodName();
            int lineNumber = stackElements[2].getLineNumber();
            return sb.append("{")
                    .append(className)
                    .append("}")
                    .append(".")
                    .append(methodName)
                    .append("() ")
                    .append("line: ")
                    .append(lineNumber)
                    .append('\n')
                    .append(msg).toString();
        } else {
            return msg;
        }
    }
}
