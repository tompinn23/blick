package one.tlph.blick.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class Registry<V extends IRegistryEntry<V>> implements IRegistry<V>{

    private final String name;
    private final String defaultKey;
    private final BiMap<Integer, V> ids = HashBiMap.create();
    private final BiMap<String, V> names = HashBiMap.create();
    private final BiMap<RegistryKey<V>, V> keys = HashBiMap.create();
    private final Class<V> superType;
    private final BitSet availabilityMap;
    private final int min;
    private final int max;
    private final boolean allowOverrides;

    private V defaultValue = null;

    private final RegistryKey<Registry<V>> key;

    private Registry(String name, String defaultKey, Class<V> superType, int min, int max, boolean allowOverrides) {
        this.name = name;
        this.defaultKey = defaultKey;
        this.superType = superType;
        this.min = min;
        this.max = max;
        this.availabilityMap = new BitSet(Math.min(max + 1, 0x0FFF));
        this.allowOverrides = allowOverrides;
        this.key = RegistryKey.create(name);
    }

    @Override
    public String getRegistryName() {
        return name;
    }

    @Override
    public Class<V> getRegistryType() {
        return null;
    }

    @Override
    public void register(V value) {
        add(-1, value);
    }

    @Override
    public void registerAll(V... values) {
        for(V value : values) {
            register(value);
        }
    }

    @Override
    public boolean containsKey(String key) {
        if(this.names.containsKey(key))
            return true;
        return false;
    }

    @Override
    public boolean containsValue(V value) {
        return this.names.containsValue(value);
    }

    @Override
    public boolean isEmpty() {
        return this.names.isEmpty();
    }

    @Override
    public V getValue(String key) {
        V ret = this.names.get(key);
        return ret == null ? this.defaultValue : ret;
    }

    @Nullable
    @Override
    public String getKey(V value) {
        String ret = this.names.inverse().get(value);
        return ret == null ? this.defaultKey : ret;
    }

    @Nullable
    @Override
    public String getDefaultKey() {
        return this.defaultKey;
    }

    @NotNull
    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(this.names.keySet());
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return Collections.unmodifiableSet(this.names.values());
    }

    @NotNull
    @Override
    public Set<Map.Entry<RegistryKey<V>, V>> entries() {
        return Collections.unmodifiableSet(this.keys.entrySet());
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return new Iterator<V>()
        {
            int cur = -1;
            V next = null;
            { next(); }

            @Override
            public boolean hasNext()
            {
                return next != null;
            }

            @Override
            public V next()
            {
                V ret = next;
                do {
                    cur = availabilityMap.nextSetBit(cur + 1);
                    next = ids.get(cur);
                } while (next == null && cur != -1); // nextSetBit returns -1 when none is found
                return ret;
            }
        };
    }

    public int getID(V value)
    {
        Integer ret = this.ids.inverse().get(value);
        if (ret == null && this.defaultValue != null)
            ret = this.ids.inverse().get(this.defaultValue);
        return ret == null ? -1 : ret.intValue();
    }

    public int getID(String name)
    {
        return getID(this.names.get(name));
    }

    int add(int id, V value) {
        final String owner = "base";
        return add(id, value, owner);
    }

    int add(int id, V value, String owner) {
        String key = value == null ? null : value.getRegistryName();
        Preconditions.checkNotNull(key, "Cant use a null name for registry, object %s.", value);
        Preconditions.checkNotNull(value, "Cant use a null object for registry, name %s.", key);

        int newId = id;
        if(newId < 0 || availabilityMap.get(newId)) {
            newId = availabilityMap.nextClearBit(min);
        }

        if(newId > max) {
            throw new RuntimeException(String.format("Invalid id %d - maximum exceeded.", newId));
        }

        V oldEntry = getRaw(key);
        if(oldEntry == value) {
            //Warning
            return this.getID(value);
        }
        if(oldEntry != null) {
            if (!this.allowOverrides)
                throw new IllegalArgumentException(String.format("The name %s has been registered twice, for %s and %s.", key, getRaw(key), value));
            if (owner == null)
                throw new IllegalStateException(String.format("Could not determine owner for the override on %s. Value: %s", key, value));
            newId = this.getID(oldEntry);
        }

        Integer foundId = this.ids.inverse().get(value); //Is this ever possible to trigger with otherThing being different?
        if (foundId != null)
        {
            V otherThing = this.ids.get(foundId);
            throw new IllegalArgumentException(String.format("The object %s{%x} has been registered twice, using the names %s and %s. (Other object at this id is %s{%x})", value, System.identityHashCode(value), getKey(value), key, otherThing, System.identityHashCode(otherThing)));
        }


        if (defaultKey != null && defaultKey.equals(key))
        {
            if (this.defaultValue != null)
                throw new IllegalStateException(String.format("Attemped to override already set default value. This is not allowed: The object %s (name %s)", value, key));
            this.defaultValue = value;
        }

        this.names.put(key, value);
        this.keys.put(RegistryKey.create(this.key, key), value);
        this.ids.put(newId, value);
        this.availabilityMap.set(newId);

        return newId;
    }

    public V getRaw(String key)
    {
        V ret = this.names.get(key);
        return ret;
    }
}
