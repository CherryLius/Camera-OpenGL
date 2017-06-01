package cherry.android.camera.filter;

/**
 * Created by Administrator on 2017/4/11.
 */

public interface RendererFilter {
    void setup();

    void destroy();

    void render(int textureId, float[] mtx);

    void updateTexture(float[] mtx);
}
