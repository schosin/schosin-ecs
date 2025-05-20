package de.schosin.ecs.buildtools.codegen.apt.visitor;

import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.ClassName;

import de.schosin.ecs.buildtools.codegen.ComponentSetConfig;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.VisitorKind;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.VisitorResult;

public class RecordVisitor extends AbstractVisitor {

    public RecordVisitor(TypeElement typeElement) {
        super(typeElement);
    }

    @Override
    public VisitorResult visitType(TypeElement e, Void p) {
        var name = e.getSimpleName().toString() + "Set";

        var annotation = e.getAnnotation(ComponentSetConfig.class);
        if (annotation != null) {
            if (!annotation.value().isBlank()) {
                name = annotation.value();
            }
        }

        this.data.kind = VisitorKind.RECORD;
        this.data.source = e.getQualifiedName().toString();
        this.data.factoryName = determineFactoryName(e, name);

        this.data.interfaceName = ClassName.get(this.data.packageName, name);
        this.data.implementationName = ClassName.get(this.data.packageName, this.data.interfaceName.simpleName() + "Impl");

        return super.visitType(e, p);
    }

    @Override
    public VisitorResult visitRecordComponent(RecordComponentElement e, Void p) {
        var name = e.getSimpleName().toString();
        var returnType = e.getAccessor().getReturnType();

        addComponent(name, returnType);

        return super.visitRecordComponent(e, p);
    }

}
