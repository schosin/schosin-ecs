package de.schosin.ecs.buildtools.codegen.apt.visitor;

import java.beans.Introspector;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner14;

import com.palantir.javapoet.TypeName;

import de.schosin.ecs.buildtools.codegen.apt.processor.CancelException;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.ComponentData;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.VisitorResult;

public abstract class AbstractVisitor extends ElementScanner14<VisitorResult, Void> {

    protected final TypeElement typeElement;
    protected final VisitorResult data;

    public AbstractVisitor(TypeElement typeElement) {
        super(new VisitorResult());

        this.typeElement = typeElement;
        this.data = DEFAULT_VALUE;

        determinePackage(typeElement);
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

    protected void addComponent(String name, TypeMirror returnType) {
        if (isEntityId(name, returnType) || returnType.getKind() == TypeKind.VOID || !(returnType instanceof DeclaredType declaredType)) {
            return;
        }

        this.data.components.add(new ComponentData(declaredType, TypeName.get(declaredType), name, isOptional(returnType)));
    }

    protected String determineFactoryName(TypeElement e, String name) {
        if (name == null || name.isBlank()) {
            name = e.getSimpleName().toString();
        }

        return Introspector.decapitalize(name);
    }

    private boolean isEntityId(String name, TypeMirror returnType) {
        return "entityId".equals(name) && returnType.getKind() == TypeKind.INT;
    }

    private boolean isOptional(TypeMirror type) {
        for (var annotation : type.getAnnotationMirrors()) {
            var simpleName = annotation.getAnnotationType().asElement().getSimpleName().toString();

            if ("Nullable".equalsIgnoreCase(simpleName)) {
                return true;
            }
        }

        return false;
    }

}
