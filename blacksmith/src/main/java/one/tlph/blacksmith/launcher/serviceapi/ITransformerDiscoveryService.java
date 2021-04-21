package one.tlph.blacksmith.launcher.serviceapi;

import java.nio.file.Path;
import java.util.List;

public interface ITransformerDiscoveryService {
    /**
     * Return a list of additional paths to be added to transformer service discovery during loading.
     * @param gameDirectory The root game directory
     * @return The list of services
     */
    List<Path> candidates(final Path gameDirectory);
}