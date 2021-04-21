package one.tlph.blacksmith.api.launcher;

import java.util.List;

public interface ITransformerAuditTrail {
    /**
     * Retrieve the list of activities for the specified class
     * @param className Class name
     * @return a read only list of activities
     */
    List<ITransformerActivity> getActivityFor(String className);

    /**
     * Retrieve a formatted string summarizing actions for the supplied class
     * @param clazz The class
     * @return A formatted string
     */
    String getAuditString(String clazz);
}
