package cherry.android.camera.camera;

public class SizeExt {
    private int width;
    private int height;

    SizeExt(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }
}
