package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ITransformationService;
import one.tlph.blacksmith.api.launcher.ITransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static one.tlph.blacksmith.launcher.Launcher.LAUNCHER;

public class TransformStore {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<String> classNeedsTransforming = new HashSet<>();
    private final EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers;

    public TransformStore() {
        transformers = new EnumMap<>(TransformTargetLabel.LabelType.class);
        for (TransformTargetLabel.LabelType type : TransformTargetLabel.LabelType.values())
            transformers.put(type, new TransformList<>(type.getNodeType()));
    }

    List<ITransformer<FieldNode>> getTransformersFor(String className, FieldNode field) {
        TransformTargetLabel tl = new TransformTargetLabel(className, field.name);
        TransformList<FieldNode> transformerlist = TransformTargetLabel.LabelType.FIELD.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    List<ITransformer<MethodNode>> getTransformersFor(String className, MethodNode method) {
        TransformTargetLabel tl = new TransformTargetLabel(className, method.name, method.desc);
        TransformList<MethodNode> transformerlist = TransformTargetLabel.LabelType.METHOD.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    List<ITransformer<ClassNode>> getTransformersFor(String className, TransformTargetLabel.LabelType classType) {
        TransformTargetLabel tl = new TransformTargetLabel(className, classType);
        TransformList<ClassNode> transformerlist = classType.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    @SuppressWarnings("unchecked")
    <T> void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer, ITransformationService service) {
        LOGGER.debug(LAUNCHER,"Adding transformer {} to {}", () -> transformer, () -> targetLabel);
        classNeedsTransforming.add(targetLabel.getClassName().getInternalName());
        final TransformList<T> transformList = (TransformList<T>) this.transformers.get(targetLabel.getLabelType());
        transformList.addTransformer(targetLabel, new TransformerHolder<>(transformer, service));
    }

    /**
     * Requires internal class name (using '/' instead of '.')
     */
    boolean needsTransforming(String internalClassName) {
        return classNeedsTransforming.contains(internalClassName);
    }
}
