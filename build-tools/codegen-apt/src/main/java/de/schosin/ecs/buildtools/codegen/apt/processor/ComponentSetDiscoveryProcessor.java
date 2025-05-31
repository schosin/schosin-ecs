package de.schosin.ecs.buildtools.codegen.apt.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.JavaFile;

import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.TypeData;

@AutoService(Processor.class)
@SupportedAnnotationTypes(ComponentSetDiscoveryProcessor.CONFIG)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ComponentSetDiscoveryProcessor extends AbstractProcessor {

    public static final String CONFIG = "de.schosin.ecs.api.components.ComponentSetConfig";

    private ComponentSetsGenerator generator;

    private boolean generated;

    private final List<TypeData> implementations = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.errorRaised()) {
            return false;
        }

        if (this.generator == null) {
            this.generator = new ComponentSetsGenerator();
        }

        var componentSetConfig = processingEnv.getElementUtils().getTypeElement(CONFIG);

        try {
            var implementations = generator.generate(roundEnv.getElementsAnnotatedWith(componentSetConfig));
            if (implementations.isEmpty()) {
                if (generated) {
                    return true;
                }

                var componentSets = generator.generateComponentSets(this.implementations);
                var componentSetsFile = JavaFile.builder(ComponentSetsGenerator.COMPONENT_SET.packageName(), componentSets)
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build();

                writeFile(componentSetsFile);
                generated = true;

                processingEnv.getMessager().printNote("Generated ComponentSets for %d types".formatted(this.implementations.size()));

                return true;
            }

            if (generated) {
                processingEnv.getMessager().printWarning("Unexpected round, some implementations might not be available via ComponentSets.");
            }

            this.implementations.addAll(implementations);

            var implementationFiles = implementations.stream()
                    .flatMap(data -> data.types().stream()
                            .map(type -> JavaFile.builder(data.result().packageName, type)
                                    .skipJavaLangImports(true)
                                    .indent("    ")
                                    .build()))
                    .toList();

            writeFiles(implementationFiles);
        } catch (CancelException ex) {
            processingEnv.getMessager().printError(ex.getMessage());
            return true;
        }

        return true;
    }

    private void writeFiles(List<JavaFile> javaFiles) {
        for (var javaFile : javaFiles) {
            writeFile(javaFile);
        }
    }

    private void writeFile(JavaFile javaFile) {
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write generated file: " + e.getMessage());
        }
    }

}
