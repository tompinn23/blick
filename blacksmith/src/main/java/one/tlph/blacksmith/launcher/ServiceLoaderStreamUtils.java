package one.tlph.blacksmith.launcher;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utilities for making service loaders more stream friendly
 */
public class ServiceLoaderStreamUtils {
    public static <T> void parallelForEach(ServiceLoader<T> services, Consumer<T> consumer) {
        forEach(services, consumer, true);
    }

    public static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer) {
        forEach(services, consumer, false);
    }

    private static <T> void forEach(ServiceLoader<T> services, Consumer<T> consumer, boolean parallel) {
        StreamSupport.stream(services.spliterator(), parallel).forEach(consumer);
    }

    public static <T, U> Stream<U> map(ServiceLoader<T> services, Function<T, U> function) {
        return StreamSupport.stream(services.spliterator(), false).map(function);
    }

    public static <T> List<T> toList(ServiceLoader<T> services) {
        return StreamSupport.stream(services.spliterator(), false).collect(Collectors.toList());
    }

    public static <K,T> Map<K,T> toMap(ServiceLoader<T> services, Function<T, K> keyFunction) {
        return toMap(services, keyFunction, Function.identity());
    }

    public static <K,V,T> Map<K,V> toMap(ServiceLoader<T> services, Function<T, K> keyFunction, Function<T, V> valueFunction) {
        return StreamSupport.stream(services.spliterator(), false).collect(Collectors.toMap(keyFunction, valueFunction));
    }

    public static <T> ServiceLoader<T> errorHandlingServiceLoader(Class<T> clazz, Consumer<ServiceConfigurationError> errorHandler) {
        return errorHandlingServiceLoader(clazz, null, errorHandler);
    }

    public static <T> ServiceLoader<T> errorHandlingServiceLoader(Class<T> clazz, @Nullable ClassLoader cl, Consumer<ServiceConfigurationError> errorHandler) {
        final ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz, cl);
        for (Iterator<T> iterator = serviceLoader.iterator(); iterator.hasNext(); ) {
            try {
                iterator.next();
            } catch (ServiceConfigurationError e) {
                errorHandler.accept(e);
            }
        }
        return serviceLoader;
    }
}