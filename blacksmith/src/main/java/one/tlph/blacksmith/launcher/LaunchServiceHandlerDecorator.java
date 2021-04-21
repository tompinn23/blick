package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ILaunchHandlerService;
import one.tlph.blacksmith.api.launcher.ITransformingClassLoader;
import one.tlph.blacksmith.api.launcher.ITransformingClassLoaderBuilder;

public class LaunchServiceHandlerDecorator {
    private final ILaunchHandlerService service;

    public LaunchServiceHandlerDecorator(ILaunchHandlerService service) {
        this.service = service;
    }

    public void launch(String[] arguments, ITransformingClassLoader classLoader) {
        try {
            this.service.launchService(arguments, classLoader).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void configureTransformationClassLoaderBuilder(ITransformingClassLoaderBuilder builder) {
        this.service.configureTransformationClassLoader(builder);
    }

    ILaunchHandlerService getService() {
        return service;
    }
}
