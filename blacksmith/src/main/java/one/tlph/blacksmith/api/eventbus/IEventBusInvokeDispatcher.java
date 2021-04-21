package one.tlph.blacksmith.api.eventbus;

public interface IEventBusInvokeDispatcher {
    void invoke(IEventListener listener, Event event);
}
