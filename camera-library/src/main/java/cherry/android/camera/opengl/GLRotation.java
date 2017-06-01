package cherry.android.camera.opengl;

/**
 * Created by Administrator on 2017/4/6.
 */

public class GLRotation {
    /**
     * Android屏幕坐标，左上角为原点
     */
    public static final float TEXTURE_ROTATION_0[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };
    public static final float TEXTURE_ROTATION_90[] = {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
    };
    public static final float TEXTURE_ROTATION_180[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    };
    public static final float TEXTURE_ROTATION_270[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    /**
     * 二维世界坐标系，屏幕中心为原点
     */
    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    public static float[] getRotation(Rotation rotation, boolean flipHorizontal, boolean flipVertical) {
        float[] rotatedTex;
        switch (rotation) {
            case ROTATION_90:
                rotatedTex = TEXTURE_ROTATION_90;
                break;
            case ROTATION_180:
                rotatedTex = TEXTURE_ROTATION_180;
                break;
            case ROTATION_270:
                rotatedTex = TEXTURE_ROTATION_270;
                break;
            case ROTATION_0:
            default:
                rotatedTex = TEXTURE_ROTATION_0;
                break;
        }

        if (flipHorizontal) {
            rotatedTex = new float[]{
                    flip(rotatedTex[0]), rotatedTex[1],
                    flip(rotatedTex[2]), rotatedTex[3],
                    flip(rotatedTex[4]), rotatedTex[5],
                    flip(rotatedTex[6]), rotatedTex[7],
            };
        }

        if (flipVertical) {
            rotatedTex = new float[]{
                    rotatedTex[0], flip(rotatedTex[1]),
                    rotatedTex[2], flip(rotatedTex[3]),
                    rotatedTex[4], flip(rotatedTex[5]),
                    rotatedTex[6], flip(rotatedTex[7]),
            };
        }
        return rotatedTex;
    }

    private static float flip(float i) {
        if (i == 0.0f)
            return 1.0f;
        return 0.0f;
    }
}
