package cherry.android.camera;

/**
 * Created by ROOT on 2017/9/11.
 */

public interface SizeSupplier<T> {
    int width(T t);

    int height(T t);
}
