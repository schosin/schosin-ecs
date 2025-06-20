package de.schosin.ecs.buildtools.codegen.apt.visitor;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner14;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.TypeName;

import de.schosin.ecs.buildtools.codegen.apt.processor.CancelException;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.ComponentData;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.ComponentType;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.FieldData;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.VisitorResult;

public abstract class AbstractVisitor extends ElementScanner14<VisitorResult, Void> {

    public static final String COMPONENT_RELATION = "de.schosin.ecs.api.components.Relation.ComponentRelation";
    public static final String ENTITY_RELATION = "de.schosin.ecs.api.components.Relation.EntityRelation";
    public static final String COMPONENT_RELATIONS = "de.schosin.ecs.api.components.Relations.ComponentRelations";
    public static final String ENTITY_RELATIONS = "de.schosin.ecs.api.components.Relations.EntityRelations";
    public static final String COMPONENT_SET = "de.schosin.ecs.api.components.ComponentSet";
    public static final String BASE_DATA = "de.schosin.ecs.plugins.data.types.BaseData";

    private static final Pattern BEAN_SETTER = Pattern.compile("^set([A-Z].*)$");

    protected final Element element;
    protected final TypeElement typeElement;
    protected final VisitorResult data;

    public AbstractVisitor(TypeElement typeElement) {
        super(new VisitorResult());

        this.element = typeElement;
        this.typeElement = typeElement;
        this.data = DEFAULT_VALUE;

        determinePackage(typeElement);
    }

    public AbstractVisitor(ExecutableElement executable) {
        super(new VisitorResult());

        this.element = executable;
        this.typeElement = determineType(executable);
        this.data = DEFAULT_VALUE;

        determinePackage(typeElement);
    }

    private TypeElement determineType(ExecutableElement e) {
        var enclosingElement = e.getEnclosingElement();
        do {
            if (enclosingElement instanceof TypeElement typeElement) {
                return typeElement;
            }

            enclosingElement = enclosingElement.getEnclosingElement();
        } while (enclosingElement != null);

        throw new IllegalStateException("Failed to determine type element of executable: " + e);
    }

    private void determinePackage(TypeElement e) {
        var packageType = e.getEnclosingElement();
        while (packageType != null && packageType.getKind() != ElementKind.PACKAGE) {
            packageType = packageType.getEnclosingElement();
            if (packageType == null) {
                throw new CancelException("Failed to determine package of type '%s'".formatted(typeElement));
            }
        }

        if (packageType instanceof PackageElement pkg) {
            this.data.packageName = pkg.getQualifiedName().toString();
            return;
        }

        throw new CancelException("Failed to determine package of type '%s'".formatted(typeElement));
    }

    protected void addComponent(String name, TypeMirror type) {
        if (isEntityId(name, type) || type.getKind() == TypeKind.VOID || !(type instanceof DeclaredType declaredType)) {
            return;
        }

        var componentType = detectComponentType(declaredType);
        this.data.components.add(new ComponentData(this.data.components.size(), declaredType, TypeName.get(declaredType), name, componentType, resolveFields(declaredType)));
    }

    private ComponentType detectComponentType(DeclaredType type) {
        if (ComponentSetsGenerator.doesImplement(type, COMPONENT_RELATION)) {
            return ComponentType.COMPONENT_RELATION;
        }
        if (ComponentSetsGenerator.doesImplement(type, ENTITY_RELATION)) {
            return ComponentType.ENTITY_RELATION;
        }
        if (ComponentSetsGenerator.doesImplement(type, COMPONENT_RELATIONS)) {
            return ComponentType.COMPONENT_RELATIONS;
        }
        if (ComponentSetsGenerator.doesImplement(type, ENTITY_RELATIONS)) {
            return ComponentType.ENTITY_RELATIONS;
        }
        if (!isRegularComponent(type)) {
            return ComponentType.NON_REGULAR;
        }
        if (ComponentSetsGenerator.doesImplement(type, ComponentSetsGenerator.POOLED.canonicalName())) {
            return ComponentType.POOLED;
        }

        return ComponentType.CLASS;
    }

    private boolean isRegularComponent(DeclaredType type) {
        if (ComponentSetsGenerator.doesImplement(type, COMPONENT_SET)) {
            return false;
        }
        if (ComponentSetsGenerator.doesImplement(type, BASE_DATA)) {
            return false;
        }

        // this will cause issues with custom component types, probably needs an "ignore" annotation or the likes
        return true;
    }

    private List<FieldData> resolveFields(DeclaredType type) {
        var element = (TypeElement) type.asElement();
        if (element.getKind() == ElementKind.RECORD) {
            return resolveRecordFields(element);
        }

        var result = new ArrayList<FieldData>();
        for (var enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD && enclosed instanceof ExecutableElement method) {
                var modifiers = method.getModifiers();
                if (modifiers.contains(Modifier.STATIC) || !modifiers.contains(Modifier.PUBLIC)) {
                    continue;
                }

                var parameters = method.getParameters();
                if (parameters.size() != 1) {
                    continue;
                }

                var parameter = parameters.get(0);
                var name = parameter.getSimpleName().toString();

                var methodName = method.getSimpleName().toString();

                var beanStyleSetter = BEAN_SETTER.matcher(methodName);
                if (!beanStyleSetter.matches()) {
                    continue;
                }

                switch (parameter.asType()) {
                    case PrimitiveType t -> result.add(new FieldData(t, TypeName.get(t), name, methodName));
                    case DeclaredType t -> result.add(new FieldData(t, TypeName.get(t), name, methodName));
                    case ArrayType t -> result.add(new FieldData(t, ArrayTypeName.get(t), name, methodName));
                    default -> {
                    }
                }
            }
        }

        return result;
    }

    private List<FieldData> resolveRecordFields(TypeElement element) {
        element.getRecordComponents().stream()
                .map(component -> switch (component.asType()) {
                    case PrimitiveType type -> FieldData.recordComponent(type, TypeName.get(type), component.getSimpleName().toString());
                    case DeclaredType type -> FieldData.recordComponent(type, TypeName.get(type), component.getSimpleName().toString());
                    case ArrayType type -> FieldData.recordComponent(type, ArrayTypeName.get(type), component.getSimpleName().toString());
                    default -> null;
                })
                .filter(Objects::nonNull);

        return List.of();
    }

    protected String determineFactoryName(String name) {
        return Introspector.decapitalize(name);
    }

    private boolean isEntityId(String name, TypeMirror returnType) {
        return "entityId".equals(name) && returnType.getKind() == TypeKind.INT;
    }

}
