package one.tlph.blacksmith.api.eventbus;

public interface IEventExceptionHandler {
    void handleException(IEventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable);
}
