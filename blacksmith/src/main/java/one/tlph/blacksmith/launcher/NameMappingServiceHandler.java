package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.INameMappingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static one.tlph.blacksmith.launcher.Launcher.LAUNCHER;

public class NameMappingServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServiceLoader<INameMappingService> namingServices;
    private final Map<String, NameMappingServiceDecorator> namingTable;
    private Map<String, NameMappingServiceDecorator> nameBindings;

    public NameMappingServiceHandler() {
        namingServices = ServiceLoaderStreamUtils.errorHandlingServiceLoader(INameMappingService.class, serviceConfigurationError -> LOGGER.fatal("Encountered serious error loading naming service, expect problems", serviceConfigurationError));
        namingTable = ServiceLoaderStreamUtils.toMap(namingServices, INameMappingService::mappingName, NameMappingServiceDecorator::new);
        LOGGER.debug(LAUNCHER,"Found naming services : [{}]", () -> String.join(",", namingTable.keySet()));
    }


    public Optional<BiFunction<INameMappingService.Domain,String,String>> findNameTranslator(final String targetNaming) {
        return Optional.ofNullable(nameBindings.get(targetNaming)).map(NameMappingServiceDecorator::function);
    }

    public void bindNamingServices(final String currentNaming) {
        LOGGER.debug(LAUNCHER, "Current naming domain is '{}'", currentNaming);
        nameBindings = namingTable.values().stream().
                filter(nameMappingServiceDecorator -> nameMappingServiceDecorator.validTarget(currentNaming)).
                collect(Collectors.toMap(NameMappingServiceDecorator::understands, Function.identity()));
        LOGGER.debug(LAUNCHER, "Identified name mapping providers {}", nameBindings);
    }
}
