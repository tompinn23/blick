package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.IEnvironment;
import one.tlph.blacksmith.api.launcher.TypesafeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Launcher {
    public static Launcher INSTANCE;
    public static final Marker LAUNCHER = MarkerManager.getMarker("LAUNCHER");
    private final TypesafeMap blackboard;
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final LaunchPluginHandler launchPlugins;
    private final ArgumentHandler argumentHandler;
    private final LaunchServiceHandler launchService;
    private TransformingClassLoader classLoader;

    private Launcher() {
        INSTANCE = this;
        LogManager.getLogger().info(LAUNCHER, "Launcher {} starting: java version {} by {}", ()-> IEnvironment.class.getPackage().getImplementationVersion(), () -> System.getProperty("java.version"), ()->System.getProperty("java.vendor"));
        this.launchService = new LaunchServiceHandler();
        this.blackboard =new TypesafeMap();
        this.environment = new Environment(this);
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLSPEC_VERSION.get(), s->IEnvironment.class.getPackage().getSpecificationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MLIMPL_VERSION.get(), s->IEnvironment.class.getPackage().getImplementationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MODLIST.get(), s->new ArrayList<>());
        environment.computePropertyIfAbsent(IEnvironment.Keys.SECURED_JARS_ENABLED.get(), k->SecureJarHandler.canHandleSecuredJars());
        this.transformStore = new TransformStore();
        this.transformationServicesHandler = new TransformationServicesHandler(this.transformStore);
        this.argumentHandler = new ArgumentHandler();
        this.nameMappingServiceHandler = new NameMappingServiceHandler();
        this.launchPlugins = new LaunchPluginHandler();
    }

    public static void main(String... args) {
        LogManager.getLogger().info(LAUNCHER, "Launcher running: args {}", () -> LaunchServiceHandler.hideAccessToken(args));
        new Launcher().run(args);
    }

    private void run(String[] args) {
        final Path gameDir = this.argumentHandler.setArgs(args);
        this.transformationServicesHandler.discoverServices(gameDir);
        final List<Map.Entry<String, Path>> scanResults = this.transformationServicesHandler.initializeTransformationServices(this.argumentHandler, this.environment, this.nameMappingServiceHandler);
        this.launchPlugins.offerScanResultsToPlugins(scanResults);
        this.launchService.validateLaunchTarget(this.argumentHandler);
        final TransformingClassLoaderBuilder classLoaderBuilder = this.launchService.identifyTransformationTargets(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, classLoaderBuilder, this.environment);
        Thread.currentThread().setContextClassLoader(this.classLoader);
        this.launchService.launch(this.argumentHandler, this.classLoader, this.launchPlugins);
    }

    public Environment environment() {
        return this.environment;
    }
}
