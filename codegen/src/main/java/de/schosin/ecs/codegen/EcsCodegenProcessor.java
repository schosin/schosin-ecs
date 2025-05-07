package de.schosin.ecs.codegen;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.JavaFile;

import de.schosin.ecs.codegen.generators.ArchetypeGenerator;
import de.schosin.ecs.codegen.generators.ArchetypeManagerGenerator;
import de.schosin.ecs.codegen.generators.CompositionGenerator;
import de.schosin.ecs.codegen.generators.CompositionManagerGenerator;
import de.schosin.ecs.codegen.generators.EngineWorldGenerator;
import de.schosin.ecs.codegen.generators.TransmutationManagerGenerator;
import de.schosin.ecs.codegen.generators.TransmuterGenerator;
import de.schosin.ecs.codegen.tests.ArchetypeManagerTestGenerator;
import de.schosin.ecs.codegen.tests.TransmutationManagerTestGenerator;

@AutoService(Processor.class)
@SupportedOptions({ EcsCodegenProcessor.COMPOSITION_PARAMS, EcsCodegenProcessor.ARCHETYPE_PARAMS, EcsCodegenProcessor.TRANSMUTER_PARAMS })
@SupportedAnnotationTypes("de.schosin.ecs.codegen.EcsCodegen")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class EcsCodegenProcessor extends AbstractProcessor {

    static final String COMPOSITION_PARAMS = "compositionParams";
    static final String ARCHETYPE_PARAMS = "archetypeParams";
    static final String TRANSMUTER_PARAMS = "transmuterParams";

    private static final String DEFAULT_PARAMS = "8";

    private static final String BASE_COMPOSITION = "de.schosin.ecs.api.components.BaseComposition";
    private static final String BASE_ARCHETYPE = "de.schosin.ecs.api.archetype.BaseArchetype";
    private static final String BASE_TRANSMUTER = "de.schosin.ecs.api.archetype.BaseTransmuter";
    private static final String COMPOSITION_MANAGER = "de.schosin.ecs.engine.compositions.CompositionManager";
    private static final String ARCHETYPE_MANAGER = "de.schosin.ecs.engine.entities.ArchetypeManager";
    private static final String TRANSMUTATION_MANAGER = "de.schosin.ecs.engine.components.TransmutationManager";
    private static final String ENGINE_WORLD = "de.schosin.ecs.engine.EngineWorld";

    private static final String TRANSMUTATION_MANAGER_TEST = "de.schosin.ecs.engine.components.TransmutationManagerTest";
    private static final String ARCHETYPE_MANAGER_TEST = "de.schosin.ecs.engine.entities.ArchetypeManagerTest";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var compositionParams = Integer.parseUnsignedInt(processingEnv.getOptions().getOrDefault(COMPOSITION_PARAMS, DEFAULT_PARAMS));
        var archetypeParams = Integer.parseUnsignedInt(processingEnv.getOptions().getOrDefault(ARCHETYPE_PARAMS, DEFAULT_PARAMS));
        var transmuterParams = Integer.parseUnsignedInt(processingEnv.getOptions().getOrDefault(TRANSMUTER_PARAMS, DEFAULT_PARAMS));

        for (var element : roundEnv.getElementsAnnotatedWith(EcsCodegen.class)) {
            if (element instanceof TypeElement type) {
                var name = type.getQualifiedName().toString();

                switch (name) {
                    // api
                    case BASE_COMPOSITION -> writeFile(CompositionGenerator.generateFile(type, compositionParams));
                    case BASE_ARCHETYPE -> writeFile(ArchetypeGenerator.generateFile(type, archetypeParams));
                    case BASE_TRANSMUTER -> writeFile(TransmuterGenerator.generateFile(type, transmuterParams));

                    // core
                    case COMPOSITION_MANAGER -> writeFile(CompositionManagerGenerator.generateFile(type, compositionParams));
                    case ARCHETYPE_MANAGER -> writeFile(ArchetypeManagerGenerator.generateFile(type, archetypeParams));
                    case TRANSMUTATION_MANAGER -> writeFile(TransmutationManagerGenerator.generateFile(type, transmuterParams));
                    case ENGINE_WORLD -> writeFile(EngineWorldGenerator.generateFile(type, archetypeParams, transmuterParams));

                    // core tests
                    case TRANSMUTATION_MANAGER_TEST -> writeFile(TransmutationManagerTestGenerator.generateFile(type, transmuterParams));
                    case ARCHETYPE_MANAGER_TEST -> writeFile(ArchetypeManagerTestGenerator.generateFile(type, archetypeParams));

                    default -> throw new IllegalArgumentException("@%s used on %s: not supported".formatted(EcsCodegen.class.getSimpleName(), name));
                }
            }
        }

        return true;
    }

    private void writeFile(JavaFile javaFile) {
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write generated file: " + e.getMessage());
        }
    }

}
