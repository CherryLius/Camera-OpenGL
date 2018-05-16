package cherry.android.camera.opengl;

/**
 * Created by Administrator on 2017/4/6.
 */

public enum Rotation {

    ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270;

    public static Rotation valueOf(int rotation) {
        switch (rotation) {
            case 0:
                return ROTATION_0;
            case 90:
                return ROTATION_90;
            case 180:
                return ROTATION_180;
            case 270:
                return ROTATION_270;
            default:
                throw new IllegalArgumentException("Unknown Rotation: " + rotation);
        }
    }

    public int value() {
        switch (this) {
            case ROTATION_0:
                return 0;
            case ROTATION_90:
                return 90;
            case ROTATION_180:
                return 180;
            case ROTATION_270:
                return 270;
            default:
                throw new IllegalStateException("Rotation not Support: " + this);
        }
    }
}
