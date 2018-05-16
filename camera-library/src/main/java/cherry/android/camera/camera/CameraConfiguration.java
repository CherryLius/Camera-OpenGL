package cherry.android.camera.camera;

/**
 * Created by LHEE on 2018/3/18.
 */

public class CameraConfiguration {
    private SizeExt previewSizeExt;
    private SizeExt pictureSizeExt;

    private CameraConfiguration(Builder builder) {
        this.previewSizeExt = builder.previewSizeExt;
        this.pictureSizeExt = builder.pictureSizeExt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public SizeExt getPreviewSizeExt() {
        return previewSizeExt;
    }

    public SizeExt getPictureSizeExt() {
        return pictureSizeExt;
    }

    public static class Builder {
        private SizeExt previewSizeExt;
        private SizeExt pictureSizeExt;

        private Builder() {

        }

        public Builder previewOn(int width, int height) {
            this.previewSizeExt = new SizeExt(width, height);
            return this;
        }

        public Builder captureOn(int width, int height) {
            this.pictureSizeExt = new SizeExt(width, height);
            return this;
        }

        public CameraConfiguration build() {
            return new CameraConfiguration(this);
        }
    }
}
