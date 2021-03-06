package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.ITransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static one.tlph.blacksmith.launcher.TransformTargetLabel.LabelType.*;


public final class TransformTargetLabel {

    private final Type className;
    private final String elementName;
    private final Type elementDescriptor;
    private final LabelType labelType;
    TransformTargetLabel(ITransformer.Target target) {
        this(target.getClassName(), target.getElementName(), target.getElementDescriptor(), LabelType.valueOf(target.getTargetType().name()));
    }
    private TransformTargetLabel(String className, String elementName, String elementDescriptor, LabelType labelType) {
        this.className = Type.getObjectType(className.replace('.', '/'));
        this.elementName = elementName;
        this.elementDescriptor = elementDescriptor.length() > 0 ? Type.getMethodType(elementDescriptor) : Type.VOID_TYPE;
        this.labelType = labelType;
    }
    public TransformTargetLabel(String className, String fieldName) {
        this(className, fieldName, "", FIELD);
    }

    TransformTargetLabel(String className, String methodName, String methodDesc) {
        this(className, methodName, methodDesc, METHOD);
    }

    @Deprecated
    public TransformTargetLabel(String className) {
        this(className, "", "", CLASS);
    }

    public TransformTargetLabel(String className, LabelType type) {
        this(className, "", "", type);
        if (type.nodeType != ClassNode.class)
            throw new IllegalArgumentException("Invalid type " + type + ", must be for class!");
    }

    final Type getClassName() {
        return this.className;
    }

    public final String getElementName() {
        return this.elementName;
    }

    public final Type getElementDescriptor() {
        return this.elementDescriptor;
    }

    final LabelType getLabelType() {
        return this.labelType;
    }

    public int hashCode() {
        return Objects.hash(this.className, this.elementName, this.elementDescriptor);
    }

    @Override
    public boolean equals(Object obj) {
        try {
            TransformTargetLabel tl = (TransformTargetLabel) obj;
            return Objects.equals(this.className, tl.className)
                    && Objects.equals(this.elementName, tl.elementName)
                    && Objects.equals(this.elementDescriptor, tl.elementDescriptor);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Target : " + Objects.toString(labelType) + " {" + Objects.toString(className) + "} {" + Objects.toString(elementName) + "} {" + Objects.toString(elementDescriptor) + "}";
    }

    public enum LabelType {
        FIELD(FieldNode.class), METHOD(MethodNode.class), CLASS(ClassNode.class), PRE_CLASS(ClassNode.class);

        private final Class<?> nodeType;

        LabelType(Class<?> nodeType) {
            this.nodeType = nodeType;
        }

        private static final Map<String, List<LabelType>> TYPE_LOOKUP;
        static {
            final Map<String, List<LabelType>> tmpTypes = new HashMap<>();
            for (LabelType type : values()) {
                tmpTypes.computeIfAbsent(type.nodeType.getName(), s -> new ArrayList<>()).add(type);
            }
            final Map<String, List<LabelType>> unmodifiableTypes = tmpTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, pair -> Collections.unmodifiableList(pair.getValue())));
            TYPE_LOOKUP = Collections.unmodifiableMap(unmodifiableTypes);
        }

        public static List<LabelType> getTypeFor(java.lang.reflect.Type type) {
            return TYPE_LOOKUP.getOrDefault(type.getTypeName(), Collections.emptyList());
        }

        public Class<?> getNodeType() {
            return nodeType;
        }

        @SuppressWarnings("unchecked")
        public <V> TransformList<V> getFromMap(EnumMap<LabelType, TransformList<?>> transformers) {
            return get(transformers, (Class<V>) this.nodeType);
        }

        @SuppressWarnings("unchecked")
        private <V> TransformList<V> get(EnumMap<LabelType, TransformList<?>> transformers, Class<V> type) {
            return (TransformList<V>) transformers.get(this);
        }

        @SuppressWarnings("unchecked")
        public <T> Supplier<TransformList<T>> mapSupplier(EnumMap<LabelType, TransformList<?>> transformers) {
            return () -> (TransformList<T>) transformers.get(this);
        }

    }
}
