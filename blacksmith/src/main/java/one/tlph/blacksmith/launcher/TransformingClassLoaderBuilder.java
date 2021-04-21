package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ITransformingClassLoaderBuilder;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Manifest;

import static one.tlph.blacksmith.api.launcher.LamdbaExceptionUtils.rethrowFunction;

public class TransformingClassLoaderBuilder implements ITransformingClassLoaderBuilder {
    private final List<Path> transformationPaths = new ArrayList<>();
    private Function<String, Enumeration<URL>> resourcesLocator;
    private Function<URLConnection, Optional<Manifest>> manifestLocator;

    URL[] getSpecialJarsAsURLs() {
        return transformationPaths.stream().map(rethrowFunction(path->path.toUri().toURL())).toArray(URL[]::new);
    }

    Function<URLConnection, Optional<Manifest>> getManifestLocator() {
        return manifestLocator;
    }

    @Override
    public void addTransformationPath(final Path path) {
        transformationPaths.add(path);
    }

    @Override
    public void setClassBytesLocator(final Function<String, Optional<URL>> additionalClassBytesLocator) {
        this.resourcesLocator = EnumerationHelper.fromOptional(additionalClassBytesLocator);
    }

    @Override
    public void setResourceEnumeratorLocator(final Function<String, Enumeration<URL>> resourceEnumeratorLocator) {
        this.resourcesLocator = resourceEnumeratorLocator;
    }

    @Override
    public void setManifestLocator(final Function<URLConnection, Optional<Manifest>> manifestLocator) {
        this.manifestLocator = manifestLocator;
    }

    Function<String, Enumeration<URL>> getResourceEnumeratorLocator() {
        return this.resourcesLocator != null ? this.resourcesLocator : input -> Collections.emptyEnumeration();
    }
}
