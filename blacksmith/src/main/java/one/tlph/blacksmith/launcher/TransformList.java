package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ITransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransformList<T> {
    private final Map<TransformTargetLabel, List<ITransformer<T>>> transformers = new ConcurrentHashMap<>();
    private final Class<T> nodeType;

    TransformList(Class<T> nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Testing only
     * @return a map
     */
    private Map<TransformTargetLabel, List<ITransformer<T>>> getTransformers() {
        return transformers;
    }

    void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer) {
        // thread safety - compute if absent to insert the list
        transformers.computeIfAbsent(targetLabel, v -> new ArrayList<>());
        // thread safety - compute if present to mutate the list under the protection of the CHM
        transformers.computeIfPresent(targetLabel, (k,l)-> { l.add(transformer); return l;});
    }

    List<ITransformer<T>> getTransformersForLabel(TransformTargetLabel label) {
        // thread safety - compute if absent to insert the list
        return transformers.computeIfAbsent(label, v-> new ArrayList<>());
    }
}
