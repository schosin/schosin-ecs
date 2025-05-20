package de.schosin.ecs.buildtools.codegen.apt.visitor;

import javax.lang.model.element.TypeElement;

public class ClassVisitor extends AbstractVisitor {

    public ClassVisitor(TypeElement typeElement) {
        super(typeElement);
    }

}
