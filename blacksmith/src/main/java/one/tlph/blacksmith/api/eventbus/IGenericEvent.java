package one.tlph.blacksmith.api.eventbus;

import java.lang.reflect.Type;

public interface IGenericEvent<T> {
    Type getGenericType();
}
