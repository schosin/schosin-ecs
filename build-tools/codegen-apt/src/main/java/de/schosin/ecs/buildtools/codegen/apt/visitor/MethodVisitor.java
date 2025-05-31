package de.schosin.ecs.buildtools.codegen.apt.visitor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

import com.palantir.javapoet.ClassName;

import de.schosin.ecs.buildtools.codegen.ComponentSetConfig;
import de.schosin.ecs.buildtools.codegen.apt.processor.CancelException;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.VisitorKind;

public class MethodVisitor extends AbstractVisitor {

    public MethodVisitor(ExecutableElement executable) {
        super(executable);

        var annotation = executable.getAnnotation(ComponentSetConfig.class);
        if (annotation == null) {
            throw new CancelException("Method %s defined passed to APT, but @ComponentSetConfig is not available.".formatted(executable));
        }

        var name = annotation.value();

        this.data.kind = VisitorKind.RECORD;
        this.data.source = executable.toString();
        this.data.factoryName = determineFactoryName(name);

        this.data.interfaceName = ClassName.get(this.data.packageName, name);
        this.data.implementationName = ClassName.get(this.data.packageName, this.data.interfaceName.simpleName() + "Impl");

        var parameters = executable.getParameters();
        for (int i = 0, s = parameters.size(); i < s; i++) {
            var parameter = parameters.get(i);

            var parameterName = parameter.getSimpleName().toString();
            var type = parameter.asType();

            if (i == 0 && type.getKind() != TypeKind.INT) {
                throw new CancelException("Method %s does not define int (entityId) as first argument.".formatted(executable, i + 1));
            }

            addComponent(parameterName, type);
        }
    }

}
