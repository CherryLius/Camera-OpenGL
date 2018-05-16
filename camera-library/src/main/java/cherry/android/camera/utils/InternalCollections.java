package cherry.android.camera.utils;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ext.java8.function.Function;
import ext.java8.function.Predicate;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InternalCollections {
    private InternalCollections() {
        throw new AssertionError("no instance.");
    }

    public static <T> void filter(@NonNull Collection<T> collection, @NonNull Predicate<T> predicate) {
        Iterator<T> iterator = collection.iterator();
        while (iterator.hasNext()) {
            if (!predicate.test(iterator.next())) {
                iterator.remove();
            }
        }
    }

    public static <T, R> List<R> mapList(@NonNull List<T> src, @NonNull Function<T, R> map) {
        if (src.isEmpty()) {
            return Collections.emptyList();
        }
        List<R> dst = new ArrayList<>(src.size());
        for (T t : src) {
            dst.add(map.apply(t));
        }
        return dst;
    }
}
