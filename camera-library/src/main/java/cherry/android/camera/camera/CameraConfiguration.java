package cherry.android.camera.camera;

import android.support.annotation.NonNull;

import cherry.android.camera.SizeSupplier;

/**
 * Created by LHEE on 2018/3/18.
 */

public class CameraConfiguration<T> {

    private SizeSupplier<T> previewSizeSupplier;
    private SizeSupplier<T> pictureSizeSupplier;

    private CameraConfiguration(Builder<T> builder) {
        this.previewSizeSupplier = builder.previewSizeSupplier;
        this.pictureSizeSupplier = builder.pictureSizeSupplier;
    }

    public static <S> Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private SizeSupplier<T> previewSizeSupplier;
        private SizeSupplier<T> pictureSizeSupplier;

        private Builder() {

        }

        public Builder<T> previewSize(@NonNull SizeSupplier<T> previewSizeSupplier) {
            this.previewSizeSupplier = previewSizeSupplier;
            return this;
        }

        public Builder<T> pictureSize(@NonNull SizeSupplier<T> pictureSizeSupplier) {
            this.pictureSizeSupplier = pictureSizeSupplier;
            return this;
        }

        public CameraConfiguration<T> build() {
            return new CameraConfiguration(this);
        }
    }
}
