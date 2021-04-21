package one.tlph.blacksmith.api.launcher;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public interface ILaunchHandlerService {
    String name();

    void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder);

    Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader);

    default Path[] getPaths() { return new Path[0]; }
}
