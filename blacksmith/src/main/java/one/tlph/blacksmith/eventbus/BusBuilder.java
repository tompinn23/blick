package one.tlph.blacksmith.eventbus;

import one.tlph.blacksmith.api.eventbus.Event;
import one.tlph.blacksmith.api.eventbus.IEventBus;
import one.tlph.blacksmith.api.eventbus.IEventExceptionHandler;

public final class BusBuilder {
    private IEventExceptionHandler exceptionHandler;

    // true by default
    private boolean trackPhases = true;
    private boolean startShutdown = false;
    private Class<?> markerType = Event.class;

    public static BusBuilder builder() {
        return new BusBuilder();
    }

    public BusBuilder setTrackPhases(boolean trackPhases) {
        this.trackPhases = trackPhases;
        return this;
    }

    public BusBuilder setExceptionHandler(IEventExceptionHandler handler) {
        this.exceptionHandler =  handler;
        return this;
    }

    public BusBuilder startShutdown() {
        this.startShutdown = true;
        return this;
    }

    public BusBuilder markerType(Class<?> type) {
        if (!type.isInterface()) throw new IllegalArgumentException("Cannot specify a class marker type");
        this.markerType = type;
        return this;
    }

    public IEventExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public boolean getTrackPhases() {
        return trackPhases;
    }

    public IEventBus build() {
        return new EventBus(this);
    }

    public boolean isStartingShutdown() {
        return this.startShutdown;
    }

    public Class<?> getMarkerType() {
        return this.markerType;
    }
}
