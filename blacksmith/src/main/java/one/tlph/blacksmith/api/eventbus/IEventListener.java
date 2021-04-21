package one.tlph.blacksmith.api.eventbus;

public interface IEventListener {
    void invoke(Event event);

    default String listenerName() {
        return getClass().getName();
    }
}
