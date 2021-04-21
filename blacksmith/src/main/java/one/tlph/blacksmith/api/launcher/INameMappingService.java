package one.tlph.blacksmith.api.launcher;

import java.util.Map;
import java.util.function.BiFunction;

public interface INameMappingService {
    /**
     * The name of this namemapping.
     *
     * E.G. srgtomcp
     *
     * @return a unique name for this mapping
     */
    String mappingName();

    /**
     * A version number for this namemapping.
     *
     * @return a version number for this mapping
     */
    String mappingVersion();

    /**
     * The source and target you support. If your target is not the active naming, you will be ignored.
     *
     * @return A key (source naming) value (target naming) pair representing your source to target translation.
     */
    Map.Entry<String,String> understanding();

    /**
     * A function mapping a name to another name, for the given domain.
     *
     * The input string will be the name in the source naming, you should return the name in the target naming.
     *
     * @return A function mapping names
     */
    BiFunction<Domain, String, String> namingFunction();

    enum Domain { CLASS, METHOD, FIELD }
}
