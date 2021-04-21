package one.tlph.blacksmith.api.launcher;

import java.util.function.Predicate;

public interface ITransformingClassLoader {
    default ClassLoader getInstance() {
        return (ClassLoader) this;
    }

    void addTargetPackageFilter(Predicate<String> filter);
}
