package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ILaunchHandlerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.*;

import static one.tlph.blacksmith.launcher.Launcher.LAUNCHER;

public class LaunchServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServiceLoader<ILaunchHandlerService> launchHandlerServices;
    private final Map<String, LaunchServiceHandlerDecorator> launchHandlerLookup;

    public LaunchServiceHandler() {
        launchHandlerServices = ServiceLoaderStreamUtils.errorHandlingServiceLoader(ILaunchHandlerService.class, serviceConfigurationError -> LOGGER.fatal("Encountered serious error loading transformation service, expect problems", serviceConfigurationError));
        launchHandlerLookup = ServiceLoaderStreamUtils.toMap(launchHandlerServices, ILaunchHandlerService::name, LaunchServiceHandlerDecorator::new);
        LOGGER.debug(LAUNCHER,"Found launch services [{}]", () -> String.join(",",launchHandlerLookup.keySet()));
    }

    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return Optional.ofNullable(launchHandlerLookup.getOrDefault(name, null)).map(LaunchServiceHandlerDecorator::getService);
    }

    private void launch(String target, String[] arguments, TransformingClassLoader classLoader, final LaunchPluginHandler launchPluginHandler) {
        final LaunchServiceHandlerDecorator launchServiceHandlerDecorator = launchHandlerLookup.get(target);
        final Path[] paths = launchServiceHandlerDecorator.getService().getPaths();
        launchPluginHandler.announceLaunch(classLoader, paths);
        LOGGER.info(LAUNCHER, "Launching target '{}' with arguments {}", target, hideAccessToken(arguments));
        launchServiceHandlerDecorator.launch(arguments, classLoader);
    }

    static List<String> hideAccessToken(String[] arguments) {
        final ArrayList<String> output = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0 && Objects.equals(arguments[i-1], "--accessToken")) {
                output.add("**********");
            } else {
                output.add(arguments[i]);
            }
        }
        return output;
    }

    public void launch(ArgumentHandler argumentHandler, TransformingClassLoader classLoader, final LaunchPluginHandler launchPluginHandler) {
        String launchTarget = argumentHandler.getLaunchTarget();
        String[] args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, classLoader, launchPluginHandler);
    }

    TransformingClassLoaderBuilder identifyTransformationTargets(final ArgumentHandler argumentHandler) {
        final String launchTarget = argumentHandler.getLaunchTarget();
        final TransformingClassLoaderBuilder builder = new TransformingClassLoaderBuilder();
        Arrays.stream(argumentHandler.getSpecialJars()).forEach(builder::addTransformationPath);
        launchHandlerLookup.get(launchTarget).configureTransformationClassLoaderBuilder(builder);
        return builder;
    }

    void validateLaunchTarget(final ArgumentHandler argumentHandler) {
        if (!launchHandlerLookup.containsKey(argumentHandler.getLaunchTarget())) {
            LOGGER.error(LAUNCHER, "Cannot find launch target {}, unable to launch",
                    argumentHandler.getLaunchTarget());
            throw new RuntimeException("Cannot find launch target");
        }
    }
}
