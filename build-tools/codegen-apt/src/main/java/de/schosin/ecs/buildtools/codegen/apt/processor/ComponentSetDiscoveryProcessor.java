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

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.buildtools.codegen.apt.processor.ComponentSetsGenerator.TypeData;

@AutoService(Processor.class)
@SupportedAnnotationTypes(ComponentSetDiscoveryProcessor.DISCOVER)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ComponentSetDiscoveryProcessor extends AbstractProcessor {

    static final String DISCOVER = "de.schosin.ecs.buildtools.codegen.DiscoverComponentSets";

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

        try {
            var implementations = generator.generate(roundEnv.getRootElements());
            if (implementations.isEmpty()) {
                if (generated) {
                    return true;
                }

                var componentSets = generator.generateComponentSets(this.implementations);
                var componentSetsFile = JavaFile.builder(ComponentSet.class.getPackageName(), componentSets)
                        .addStaticImport(ComponentType.class, "*")
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
