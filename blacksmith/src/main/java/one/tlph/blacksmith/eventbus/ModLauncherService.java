package one.tlph.blacksmith.eventbus;

import one.tlph.blacksmith.launcher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;
import java.util.Objects;

public class ModLauncherService implements ILaunchPluginService {
    @Override
    public String name() {
        return "eventbus";
    }

    @Override
    public int processClassWithFlags(final Phase phase, final ClassNode classNode, final Type classType, String reason) {
        return Objects.equals(reason, "classloading") ? EventBusEngine.INSTANCE.processClass(classNode, classType) : ComputeFlags.NO_REWRITE;
    }

    private static final EnumSet<Phase> YAY = EnumSet.of(Phase.AFTER);
    private static final EnumSet<Phase> NAY = EnumSet.noneOf(Phase.class);

    @Override
    public EnumSet<Phase> handlesClass(final Type classType, final boolean isEmpty) {
        // we never handle empty classes
        return !isEmpty && EventBusEngine.INSTANCE.handlesClass(classType) ? YAY : NAY;
    }
}
