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

import de.schosin.ecs.codegen.generators.plugins.archetype.ArchetypeGenerator;
import de.schosin.ecs.codegen.generators.plugins.archetype.ArchetypeManagerGenerator;
import de.schosin.ecs.codegen.generators.plugins.archetype.ArchetypeManagerTestGenerator;
import de.schosin.ecs.codegen.generators.plugins.composition.CompositionGenerator;
import de.schosin.ecs.codegen.generators.plugins.composition.CompositionManagerGenerator;
import de.schosin.ecs.codegen.generators.plugins.datatypes.BaseDataTypeGenerator;
import de.schosin.ecs.codegen.generators.plugins.datatypes.DataTypeMapperGenerator;
import de.schosin.ecs.codegen.generators.plugins.transmuter.TransmutationManagerGenerator;
import de.schosin.ecs.codegen.generators.plugins.transmuter.TransmutationManagerTestGenerator;
import de.schosin.ecs.codegen.generators.plugins.transmuter.TransmuterGenerator;

@AutoService(Processor.class)
@SupportedOptions({ EcsCodegenProcessor.COMPOSITION_PARAMS, EcsCodegenProcessor.ARCHETYPE_PARAMS, EcsCodegenProcessor.TRANSMUTER_PARAMS })
@SupportedAnnotationTypes("de.schosin.ecs.codegen.EcsCodegen")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class EcsCodegenProcessor extends AbstractProcessor {

    static final String COMPOSITION_PARAMS = "compositionParams";
    static final String ARCHETYPE_PARAMS = "archetypeParams";
    static final String TRANSMUTER_PARAMS = "transmuterParams";

    private static final String DEFAULT_PARAMS = "8";

    private static final String ARCHETYPE_PLUGIN = "de.schosin.ecs.plugins.archetype.BaseArchetype";
    private static final String ARCHETYPE_PLUGIN_MANAGER = "de.schosin.ecs.plugins.archetype.ArchetypeManager";
    private static final String ARCHETYPE_PLUGIN_MANAGER_TEST = "de.schosin.ecs.plugins.archetype.ArchetypeManagerTest";

    private static final String TRANSMUTER_PLUGIN = "de.schosin.ecs.plugins.transmuter.BaseTransmuter";
    private static final String TRANSMUTER_PLUGIN_MANAGER = "de.schosin.ecs.plugins.transmuter.TransmuterManager";
    private static final String TRANSMUTER_PLUGIN_MANAGER_TEST = "de.schosin.ecs.plugins.transmuter.TransmuterManagerTest";

    private static final String COMPOSIITON_PLUGIN = "de.schosin.ecs.plugins.composition.BaseComposition";
    private static final String COMPOSIITON_PLUGIN_MANAGER = "de.schosin.ecs.plugins.composition.manager.CompositionManager";

    private static final String BASE_DATA_TYPE = "de.schosin.ecs.plugins.data.types.BaseDataType";
    private static final String DATA_TYPE_MAPPER = "de.schosin.ecs.plugins.data.mappers.DataTypeMapper";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var compositionParams = Integer.parseUnsignedInt(processingEnv.getOptions().getOrDefault(COMPOSITION_PARAMS, DEFAULT_PARAMS));
        var archetypeParams = Integer.parseUnsignedInt(processingEnv.getOptions().getOrDefault(ARCHETYPE_PARAMS, DEFAULT_PARAMS));
        var transmuterParams = Integer.parseUnsignedInt(processingEnv.getOptions().getOrDefault(TRANSMUTER_PARAMS, DEFAULT_PARAMS));
        var maxParams = Math.max(compositionParams, Math.max(archetypeParams, transmuterParams));

        for (var element : roundEnv.getElementsAnnotatedWith(EcsCodegen.class)) {
            if (element instanceof TypeElement type) {
                var name = type.getQualifiedName().toString();

                switch (name) {
                    // plugins
                    case ARCHETYPE_PLUGIN -> writeFile(ArchetypeGenerator.generateFile(type, archetypeParams));
                    case ARCHETYPE_PLUGIN_MANAGER -> writeFile(ArchetypeManagerGenerator.generateFile(type, archetypeParams));
                    case ARCHETYPE_PLUGIN_MANAGER_TEST -> writeFile(ArchetypeManagerTestGenerator.generateFile(type, archetypeParams));

                    case TRANSMUTER_PLUGIN -> writeFile(TransmuterGenerator.generateFile(type, transmuterParams));
                    case TRANSMUTER_PLUGIN_MANAGER -> writeFile(TransmutationManagerGenerator.generateFile(type, transmuterParams));
                    case TRANSMUTER_PLUGIN_MANAGER_TEST -> writeFile(TransmutationManagerTestGenerator.generateFile(type, transmuterParams));

                    case COMPOSIITON_PLUGIN -> writeFile(CompositionGenerator.generateFile(type, compositionParams));
                    case COMPOSIITON_PLUGIN_MANAGER -> writeFile(CompositionManagerGenerator.generateFile(type, compositionParams));

                    case BASE_DATA_TYPE -> writeFiles(BaseDataTypeGenerator.generateFiles(type, maxParams));
                    case DATA_TYPE_MAPPER -> writeFile(DataTypeMapperGenerator.generateFile(type, maxParams));

                    default -> throw new IllegalArgumentException("@%s used on %s: not supported".formatted(EcsCodegen.class.getSimpleName(), name));
                }
            }
        }

        return true;
    }

    private void writeFiles(Iterable<JavaFile> javaFiles) {
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
