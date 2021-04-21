package one.tlph.blick.registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

public interface IRegistry<V extends IRegistryEntry<V>> extends Iterable<V> {
    String getRegistryName();
    Class<V> getRegistryType();

    void register(V value);

    void registerAll(V... values);

    boolean containsKey(String key);
    boolean containsValue(V value);
    boolean isEmpty();

    @Nullable V getValue(String key);
    @Nullable String getKey(V value);
    @Nullable String getDefaultKey();

    @Nonnull
    Set<String> keys();
    @Nonnull
    Collection<V> values();
    @Nonnull Set<Entry<RegistryKey<V>, V>> entries();

}
