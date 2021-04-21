package one.tlph.blacksmith.api.launcher;

import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Users who wish to provide a mod service which plugs into this API
 * should implement this interface, and provide a {@link java.util.ServiceLoader}
 * configuration file pointing at their implementation.
 */
public interface ITransformationService {
    /**
     * The name of this mod service. It will be used throughout the system. It should be lower case,
     * the first character should be alphanumeric and it should only consist of standard alphanumeric
     * characters
     *
     * @return the name of the mod service
     */
    @Nonnull
    String name();

    /**
     * Define command line arguments for your mod service. These will be prefixed by your {@link #name()}
     * to prevent collisions.
     *
     * @param argumentBuilder a function mapping name, description to a set of JOptSimple properties for that argument
     */
    default void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
    }

    default void argumentValues(OptionResult option) {
    }

    /**
     * Initialize your service.
     *
     * @param environment environment - query state from here to determine viability
     */
    void initialize(IEnvironment environment);


    /**
     * Scan for mods (but don't classload them), identify metadata that might drive
     * game functionality.
     *
     * @param environment environment
     */
    void beginScanning(IEnvironment environment);

    default List<Map.Entry<String, Path>> runScan(IEnvironment environment) {
        beginScanning(environment);
        return Collections.emptyList();
    }

    /**
     * Load your service. Called immediately on loading with a list of other services found.
     * Use to identify and immediately indicate incompatibilities with other services, and environment
     * configuration. This is to try and immediately abort a guaranteed bad environment.
     *
     * @param env           environment - query state from here
     * @param otherServices other services loaded with the system
     * @throws IncompatibleEnvironmentException if there is an incompatibility detected. Identify specifics in
     *                                          the exception message
     */
    void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException;

    /**
     * The {@link ITransformer} is the fundamental operator of the system.
     *
     * @return A list of transformers for your ITransformationService. This is called after {@link #onLoad(IEnvironment, Set)}
     * and {@link #initialize(IEnvironment)}, so you can return an appropriate Transformer set for the environment
     * you find yourself in.
     */
    @Nonnull
    List<ITransformer> transformers();

    /**
     * Allow transformation services to provide additional classes when asked for.
     * <p>
     * Rules:
     * The Strings in the set must end with a dot. They must have at least one dot. They cannot include "net.minecraft."
     * "net.minecraftforge.". Conflicts with other ITransformationServices will result in an immediate crash.
     *
     * @return a set of strings (tested with "startsWith" for classNames in "internal" format (my.package.Clazz))
     * with a function that receives the full classname and returns an Optional URL for loading that class. The null
     * return value means no classlocator will be used for this transformation service.
     */
    default Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> additionalClassesLocator() {
        return null;
    }

    /**
     * Allow transformation services to provide additional resource files when asked for.
     * <p>
     * Rules:
     * The Strings in the set must not end with ".class". Conflicts with other ITransformationServices will result
     * in an immediate crash.
     *
     * @return a set of strings (tested with "equals" for classResources in "internal" format (my/package/Resource))
     * with a function that receives the full resource being searched and returns an Optional URL for loading that
     * class. The null return value means no classlocator will be used for this transformation service.
     */
    default Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> additionalResourcesLocator() {
        return null;
    }

    interface OptionResult {
        @Nonnull
        <V> V value(OptionSpec<V> options);

        @Nonnull
        <V> List<V> values(OptionSpec<V> options);
    }
}