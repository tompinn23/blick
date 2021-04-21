package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.IEnvironment;
import one.tlph.blacksmith.api.launcher.ILaunchHandlerService;
import one.tlph.blacksmith.api.launcher.INameMappingService;
import one.tlph.blacksmith.api.launcher.TypesafeMap;
import one.tlph.blacksmith.launcher.serviceapi.ILaunchPluginService;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Environment implements IEnvironment {
    public Environment(Launcher launcher) {
    }

    @Override
    public <T> Optional<T> getProperty(TypesafeMap.Key<T> key) {
        return Optional.empty();
    }

    @Override
    public <T> T computePropertyIfAbsent(TypesafeMap.Key<T> key, Function<? super TypesafeMap.Key<T>, ? extends T> valueFunction) {
        return null;
    }

    @Override
    public Optional<ILaunchPluginService> findLaunchPlugin(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<ILaunchHandlerService> findLaunchHandler(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<BiFunction<INameMappingService.Domain, String, String>> findNameMapping(String targetMapping) {
        return Optional.empty();
    }
}
