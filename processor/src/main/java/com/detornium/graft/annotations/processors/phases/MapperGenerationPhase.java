/*
 *     Copyright 2025 Taras Semaniv
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.detornium.graft.annotations.processors.phases;

import com.detornium.graft.annotations.processors.ProcessingException;
import com.detornium.graft.annotations.processors.models.Fqcn;
import com.detornium.graft.annotations.processors.models.GenerationContext;
import com.detornium.graft.annotations.processors.models.MappingContext;
import com.detornium.graft.annotations.processors.models.TargetSuperInfo;
import com.detornium.graft.annotations.processors.models.tree.Node;
import com.detornium.graft.annotations.processors.scanners.MapperGenerationVisitor;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import static com.detornium.graft.annotations.processors.Constants.*;
import static com.detornium.graft.annotations.processors.utils.CodeSnippets.returnNullIfNullCode;

public class MapperGenerationPhase extends AbstractProcessingPhase {
    private static final String NAME = "Mapper Generation Phase";

    private final Filer filer;

    public MapperGenerationPhase(ProcessingEnvironment processingEnvironment) {
        this.filer = processingEnvironment.getFiler();
    }

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException {
        ClassName srcType = ClassName.get(context.getSourceType());
        ClassName targetType = ClassName.get(context.getTargetType());

        Node tree = context.getDependencyTreeRoot();

        GenerationContext generationContext = new GenerationContext();
        tree.accept(new MapperGenerationVisitor(generationContext));

        TargetSuperInfo targetSuperInfo = context.getTargetSuperInfo();

        MethodSpec.Builder mapMethod = MethodSpec.methodBuilder(targetSuperInfo.getMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(Override.class)
                .returns(targetType)
                .addParameter(srcType, SOURCE_VAR_NAME)
                .addCode(returnNullIfNullCode(SOURCE_VAR_NAME));

        generationContext.getStatements().forEach(mapMethod::addCode);
        mapMethod.addStatement("return $L", TARGET_VAR_NAME);


        // TODO: refactor
        Integer sourceParamIndex = targetSuperInfo.getSourceParamIndex();
        Integer targetParamIndex = targetSuperInfo.getTargetParamIndex();

        long argsCount = Stream.of(sourceParamIndex, targetParamIndex)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        TypeName[] typeArguments = new TypeName[(int) argsCount];

        if (sourceParamIndex != null) {
            typeArguments[sourceParamIndex] = srcType;
        }
        if (targetParamIndex != null) {
            typeArguments[targetParamIndex] = targetType;
        }

        TypeName superInterface = (argsCount == 0)
                ? ClassName.get(targetSuperInfo.getTargetInterface())
                : ParameterizedTypeName.get(ClassName.get(targetSuperInfo.getTargetInterface()), typeArguments);

        Fqcn fqcn = context.getMapperType();

        TypeSpec type = TypeSpec.classBuilder(fqcn.className())
                .addSuperinterface(superInterface)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(generationContext.getFields())
                .addMethod(mapMethod.build())
                .build();

        JavaFile javaFile = JavaFile.builder(fqcn.packageName(), type)
                .indent(INDENT)
                .build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            // TODO log the exception
            throw new ProcessingException(context.getSpec(), "Failed to generate mapper class: %s".formatted(e.getMessage()));
        }
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
