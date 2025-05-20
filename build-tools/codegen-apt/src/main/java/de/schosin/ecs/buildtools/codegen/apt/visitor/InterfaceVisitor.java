package de.schosin.ecs.buildtools.codegen.apt.visitor;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import com.palantir.javapoet.ClassName;

import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.VisitorKind;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.VisitorResult;

public class InterfaceVisitor extends AbstractVisitor {

    public InterfaceVisitor(TypeElement typeElement) {
        super(typeElement);
    }

    @Override
    public VisitorResult visitType(TypeElement e, Void p) {
        if (e.getKind() != ElementKind.INTERFACE) {
            return super.visitType(e, p);
        }

        this.data.kind = VisitorKind.INTERFACE;

        this.data.source = e.getQualifiedName().toString();
        this.data.factoryName = determineFactoryName(e, null);

        this.data.interfaceName = ClassName.bestGuess(e.getQualifiedName().toString());
        this.data.interfaceType = (DeclaredType) e.asType();

        this.data.implementationName = ClassName.get(this.data.packageName, this.data.interfaceName.simpleName() + "Impl");

        if (e.getModifiers().contains(Modifier.SEALED)) {
            this.data.kind = VisitorKind.MANUAL;

            var implementationType = (DeclaredType) e.getPermittedSubclasses().get(0);

            this.data.implementationType = implementationType;
            this.data.implementationName = ClassName.bestGuess(implementationType.asElement().toString());
        }

        return super.visitType(e, p);
    }

    @Override
    public VisitorResult visitExecutable(ExecutableElement e, Void p) {
        if (e.isDefault() || e.getKind() != ElementKind.METHOD || e.getEnclosingElement().getKind() != ElementKind.INTERFACE || e.getModifiers().contains(Modifier.STATIC)) {
            return null;
        }

        var name = e.getSimpleName().toString();
        var returnType = e.getReturnType();

        addComponent(name, returnType);

        return super.visitExecutable(e, p);
    }

}
