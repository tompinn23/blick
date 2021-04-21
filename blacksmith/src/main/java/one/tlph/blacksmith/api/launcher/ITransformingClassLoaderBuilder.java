package one.tlph.blacksmith.api.launcher;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;

public interface ITransformingClassLoaderBuilder {
    void addTransformationPath(Path path);

    void setClassBytesLocator(Function<String, Optional<URL>> additionalClassBytesLocator);

    void setResourceEnumeratorLocator(Function<String, Enumeration<URL>> resourceEnumeratorLocator);

    void setManifestLocator(Function<URLConnection, Optional<Manifest>> manifestLocator);
}
