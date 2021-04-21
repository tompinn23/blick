package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.IEnvironment;
import one.tlph.blacksmith.api.launcher.ITransformationService;
import one.tlph.blacksmith.api.launcher.ITransformer;
import one.tlph.blacksmith.api.launcher.IncompatibleEnvironmentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static one.tlph.blacksmith.launcher.Launcher.LAUNCHER;

/**
 * Decorates {@link ITransformationService} to track state and other runtime metadata.
 */
public class TransformationServiceDecorator {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ITransformationService service;
    private boolean isValid;
    private static Set<String> classPrefixes = new HashSet<>();
    private static Set<String> resourceNames = new HashSet<>();

    TransformationServiceDecorator(ITransformationService service) {
        this.service = service;
    }

    void onLoad(IEnvironment env, Set<String> otherServices) {
        try {
            LOGGER.debug(LAUNCHER,"Loading service {}", this.service::name);
            this.service.onLoad(env, otherServices);
            this.isValid = true;
            LOGGER.debug(LAUNCHER,"Loaded service {}", this.service::name);
        } catch (IncompatibleEnvironmentException e) {
            LOGGER.error(LAUNCHER,"Service failed to load {}", this.service.name(), e);
            this.isValid = false;
        }
    }

    boolean isValid() {
        return isValid;
    }

    void onInitialize(IEnvironment environment) {
        LOGGER.debug(LAUNCHER,"Initializing transformation service {}", this.service::name);
        this.service.initialize(environment);
        LOGGER.debug(LAUNCHER,"Initialized transformation service {}", this.service::name);
    }

    public void gatherTransformers(TransformStore transformStore) {
        LOGGER.debug(LAUNCHER,"Initializing transformers for transformation service {}", this.service::name);
        final List<ITransformer> transformers = this.service.transformers();
        Objects.requireNonNull(transformers, "The transformers list should not be null");
        final Map<Type, List<ITransformer>> transformersByType = transformers.stream().collect(Collectors.groupingBy(
                t ->
                {
                    final Type[] genericInterfaces = t.getClass().getGenericInterfaces();
                    for (Type typ : genericInterfaces) {
                        ParameterizedType pt = (ParameterizedType) typ;
                        if (pt.getRawType().equals(ITransformer.class)) {
                            return pt.getActualTypeArguments()[0];
                        }
                    }
                    throw new RuntimeException("How did a non-transformer get here????");
                }
        ));
        for (Type type : transformersByType.keySet()) {
            final List<TransformTargetLabel.LabelType> labelTypes = TransformTargetLabel.LabelType.getTypeFor(type);
            if (labelTypes.isEmpty()) {
                throw new IllegalArgumentException("Invalid transformer type found");
            }
            for (ITransformer<?> xform : transformersByType.get(type)) {
                final Set<ITransformer.Target> targets = xform.targets();
                if (targets.isEmpty()) continue;
                final Map<TransformTargetLabel.LabelType, List<TransformTargetLabel>> labelTypeListMap = targets.stream().map(TransformTargetLabel::new).collect(Collectors.groupingBy(TransformTargetLabel::getLabelType));
                if (labelTypeListMap.keySet().size() > 1 || labelTypes.stream().noneMatch(labelTypeListMap::containsKey)) {
                    LOGGER.error(LAUNCHER,"Invalid target {} for transformer {}", labelTypes, xform);
                    throw new IllegalArgumentException("The transformer contains invalid targets");
                }
                labelTypeListMap.values().stream().flatMap(Collection::stream).forEach(target -> transformStore.addTransformer(target, xform, service));
            }
        }
        LOGGER.debug(LAUNCHER,"Initialized transformers for transformation service {}", this.service::name);
    }

    ITransformationService getService() {
        return service;
    }

    List<Map.Entry<String, Path>> runScan(final Environment environment) {
        LOGGER.debug(LAUNCHER,"Beginning scan trigger - transformation service {}", this.service::name);
        final List<Map.Entry<String, Path>> scanResults = this.service.runScan(environment);
        LOGGER.debug(LAUNCHER,"End scan trigger - transformation service {}", this.service::name);
        return scanResults;
    }

    Function<String,Optional<URL>> getClassLoader() {
        final Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> classesLocator = this.service.additionalClassesLocator();
        if (classesLocator != null) {
            final HashSet<String> packagePrefixes = new HashSet<>(classesLocator.getKey());
            final Set<String> badPrefixes = packagePrefixes.stream().
                    filter(s ->
                            // No prefixes starting with net.minecraft.
                            s.startsWith("net.minecraft.") ||
                                    // No prefixes starting with net.minecraftforge.
                                    s.startsWith("net.minecraftforge.") ||
                                    // No prefixes already claimed
                                    classPrefixes.contains(s) ||
                                    // No prefixes not ending in a dot
                                    !s.endsWith(".") ||
                                    // No prefixes starting without a dot after the first character
                                    s.indexOf('.') <= 0).
                    collect(Collectors.toSet());
            if (!badPrefixes.isEmpty()) {
                badPrefixes.forEach(s -> LOGGER.error("Illegal prefix specified for {} : {}", this.service.name(), s));
                throw new IllegalArgumentException("Bad prefixes specified");
            }
            classPrefixes.addAll(classesLocator.getKey());
        }

        final Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> resourcesLocator = this.service.additionalResourcesLocator();
        if (resourcesLocator!=null) {
            final HashSet<String> resNames = new HashSet<>(resourcesLocator.getKey());
            final Set<String> badResourceNames = resNames.stream().
                    filter(s -> s.endsWith(".class") || resourceNames.contains(s)).
                    collect(Collectors.toSet());
            if (!badResourceNames.isEmpty()) {
                badResourceNames.forEach(s -> LOGGER.error("Illegal resource name specified for {} : {}", this.service.name(), s));
                throw new IllegalArgumentException("Bad resources specified");
            }
            resourceNames.addAll(resourcesLocator.getKey());
        }
        return s -> getOptionalURL(classesLocator, resourcesLocator, s);
    }

    private Optional<URL> getOptionalURL(@Nullable Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> classes, @Nullable Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> resources, final String name) {
        if (classes != null && name.endsWith(".class")) {
            for (String pfx : classes.getKey()) {
                if (name.startsWith(pfx.replace('.','/'))) {
                    return classes.getValue().get().apply(name);
                }
            }
        } else if (resources != null && !name.endsWith(".class")) {
            for (String pfx : resources.getKey()) {
                if (Objects.equals(name, pfx)) {
                    return resources.getValue().get().apply(name);
                }
            }
        }
        return Optional.empty();
    }
}
