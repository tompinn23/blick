package one.tlph.blick.registry;

public class RegistryKey<V> {
    private final String key;
    private final RegistryKey parent;

    private RegistryKey(RegistryKey key, String sub) {
        this.parent = key;
        this.key = sub;
    }

    public static <V> RegistryKey<V> create(String key) {
        return new RegistryKey<>(null, key);
    }

    public static <V> RegistryKey<V> create(RegistryKey key, String sub) {
        return new RegistryKey<>(key, sub);
    }
}
