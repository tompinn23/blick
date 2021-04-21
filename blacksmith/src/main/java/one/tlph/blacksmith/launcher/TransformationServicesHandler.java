package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.IEnvironment;
import one.tlph.blacksmith.api.launcher.ITransformationService;
import one.tlph.blacksmith.api.launcher.LamdbaExceptionUtils;
import one.tlph.blacksmith.launcher.ohnogoaway.Java9ClassLoaderUtil;
import one.tlph.blacksmith.launcher.serviceapi.ITransformerDiscoveryService;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static one.tlph.blacksmith.launcher.Launcher.LAUNCHER;
import static one.tlph.blacksmith.launcher.ServiceLoaderStreamUtils.*;

public class TransformationServicesHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private ServiceLoader<ITransformationService> transformationServices;
    private Map<String, TransformationServiceDecorator> serviceLookup;
    private final TransformStore transformStore;

    TransformationServicesHandler(TransformStore store) {
        this.transformStore = store;
    }


    public void discoverServices(Path gameDir) {
        LOGGER.debug(LAUNCHER, "Discovering transformation services");
        final ServiceLoader<ITransformerDiscoveryService> discoveryServices = errorHandlingServiceLoader(ITransformerDiscoveryService.class, serviceConfigurationError -> LOGGER.fatal(LAUNCHER, "Encountered fatal error loading transformation discoverer, expect problems", serviceConfigurationError));
        final List<Path> additionalPaths = map(discoveryServices, s -> s.candidates(gameDir)).flatMap(Collection::stream).collect(Collectors.toList());
        LOGGER.debug(LAUNCHER, "Found additional transformation services from discovery services: {}", additionalPaths);
        TransformerClassLoader cl = new TransformerClassLoader(Java9ClassLoaderUtil.getSystemClassPathURLs());
        additionalPaths.stream().map(LamdbaExceptionUtils.rethrowFunction(p->p.toUri().toURL())).forEach(cl::addURL);
        transformationServices = ServiceLoaderStreamUtils.errorHandlingServiceLoader(ITransformationService.class, cl, serviceConfigurationError -> LOGGER.fatal(LAUNCHER, "Encountered serious error loading transformation service, expect problems", serviceConfigurationError));
        serviceLookup = ServiceLoaderStreamUtils.toMap(transformationServices, ITransformationService::name, TransformationServiceDecorator::new);
        final List<Map<String, String>> modlist = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get()).orElseThrow(()->new RuntimeException("The MODLIST isn't set, huh?"));
        serviceLookup.forEach((name, deco)->{
            HashMap<String,String> mod = new HashMap<>();
            mod.put("name", name);
            mod.put("type", "TRANSFORMATIONSERVICE");
            String fName = deco.getService().getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
            mod.put("file", fName.substring(fName.lastIndexOf("/")));
            modlist.add(mod);
        });
        LOGGER.debug(LAUNCHER,"Found transformer services : [{}]", () -> String.join(",",serviceLookup.keySet()));
    }

    public List<Map.Entry<String, Path>> initializeTransformationServices(ArgumentHandler argumentHandler, Environment environment, NameMappingServiceHandler nameMappingServiceHandler) {
        loadTransformationServices(environment);
        validateTransformationServices();
        processArguments(argumentHandler, environment);
        initialiseTransformationServices(environment);
        // force the naming to "mojang" if nothing has been populated during transformer setup
        environment.computePropertyIfAbsent(IEnvironment.Keys.NAMING.get(), a-> "mojang");
        nameMappingServiceHandler.bindNamingServices(environment.getProperty(Environment.Keys.NAMING.get()).orElse("mojang"));
        final List<Map.Entry<String, Path>> scanResults = runScanningTransformationServices(environment);
        initialiseServiceTransformers();
        return scanResults;
    }

    private void processArguments(ArgumentHandler argumentHandler, Environment environment) {
        LOGGER.debug(LAUNCHER,"Configuring option handling for services");

        argumentHandler.processArguments(environment, this::computeArgumentsForServices, this::offerArgumentResultsToServices);
    }

    private void offerArgumentResultsToServices(OptionSet optionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult> resultHandler) {
        parallelForEach(transformationServices,
                service -> service.argumentValues(resultHandler.apply(service.name(), optionSet))
        );
    }

    private void computeArgumentsForServices(OptionParser parser) {
        parallelForEach(transformationServices,
                service -> service.arguments((a, b) -> parser.accepts(service.name() + "." + a, b))
        );
    }

    private void initialiseServiceTransformers() {
        LOGGER.debug(LAUNCHER,"Transformation services loading transformers");

        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));
    }

    private List<Map.Entry<String, Path>> runScanningTransformationServices(Environment environment) {
        LOGGER.debug(LAUNCHER,"Transformation services begin scanning");

        return serviceLookup.values()
                .stream()
                .flatMap(s -> s.runScan(environment).stream())
                .collect(Collectors.toList());
    }

    private void initialiseTransformationServices(Environment environment) {
        LOGGER.debug(LAUNCHER,"Transformation services initializing");

        serviceLookup.values().forEach(s -> s.onInitialize(environment));
    }

    private void validateTransformationServices() {
        if (serviceLookup.values().stream().filter(d -> !d.isValid()).count() > 0) {
            final List<ITransformationService> services = serviceLookup.values().stream().filter(d -> !d.isValid()).map(TransformationServiceDecorator::getService).collect(Collectors.toList());
            final String names = services.stream().map(ITransformationService::name).collect(Collectors.joining(","));
            LOGGER.error(LAUNCHER,"Found {} services that failed to load : [{}]", services.size(), names);
            throw new InvalidLauncherSetupException("Invalid Services found "+names);
        }
    }

    private void loadTransformationServices(Environment environment) {
        LOGGER.debug(LAUNCHER,"Transformation services loading");

        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
    }

    TransformingClassLoader buildTransformingClassLoader(final LaunchPluginHandler pluginHandler, final TransformingClassLoaderBuilder builder, final Environment environment) {
        final List<Function<String, Optional<URL>>> classLocatorList = serviceLookup.values().stream().map(TransformationServiceDecorator::getClassLoader).filter(Objects::nonNull).collect(Collectors.toList());
        Function<String, Enumeration<URL>> resourceEnumeratorLocator = builder.getResourceEnumeratorLocator();

        for (Function<String, Optional<URL>> transformerClassLocator : classLocatorList) {
            resourceEnumeratorLocator = EnumerationHelper.mergeFunctors(resourceEnumeratorLocator, EnumerationHelper.fromOptional(transformerClassLocator));
        }

        builder.setResourceEnumeratorLocator(resourceEnumeratorLocator);
        return new TransformingClassLoader(transformStore, pluginHandler, builder, environment);
    }

    private static class TransformerClassLoader extends URLClassLoader {
        TransformerClassLoader(final URL[] urls) {
            super(urls);
        }

        @Override
        protected void addURL(final URL url) {
            super.addURL(url);
        }
    }
}
