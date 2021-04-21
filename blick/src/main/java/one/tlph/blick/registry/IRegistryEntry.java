package one.tlph.blick.registry;

import javax.annotation.Nullable;

public interface IRegistryEntry<V> {
    V setRegistryName(String resource);

    @Nullable
    String getRegistryName();


    Class<V> getRegistryType();
}
