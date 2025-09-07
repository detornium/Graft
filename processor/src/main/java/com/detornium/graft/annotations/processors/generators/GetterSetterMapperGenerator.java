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

package com.detornium.graft.annotations.processors.generators;

import com.detornium.graft.Mapper;
import com.detornium.graft.annotations.processors.models.Accessor;
import com.detornium.graft.annotations.processors.models.Fqcn;
import com.detornium.graft.annotations.processors.models.Mapping;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

import static com.detornium.graft.annotations.processors.generators.CodeSnippets.constructVariableStatement;
import static com.detornium.graft.annotations.processors.generators.CodeSnippets.returnNullIfNullCode;

public class GetterSetterMapperGenerator extends MapperGeneratorBase {

    @Override
    public GeneratorResult generate(Fqcn fqcn,
                                    TypeElement src, TypeElement dst,
                                    List<Mapping> mappings) {

        ClassName srcType = ClassName.get(src);
        ClassName dstType = ClassName.get(dst);

        // Fields for converters would be added here if needed.
        List<FieldSpec> fields = new ArrayList<>();
        List<MethodSpec> methods = new ArrayList<>();

        MethodSpec.Builder mapMethod = MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(Override.class)
                .returns(dstType)
                .addParameter(srcType, "src")
                .addCode(returnNullIfNullCode("src"))
                .addStatement(constructVariableStatement(dstType, "dst"));


        for (Mapping mapping : mappings) {
            if (mapping.isExclude() || mapping.getSetters().isEmpty()) {
                continue;
            }

            // Retrieve value code block
            CodeBlock retrieveValueCode = generateValueRetrievalCode(mapMethod, methods, src, mapping);

            // Apply cloning if needed
            retrieveValueCode = generateCloneCode(src, mapping, retrieveValueCode);

            // Apply converter if present
            retrieveValueCode = generateConvertCode(mapping, srcType, fields, retrieveValueCode);

            // Set property statement
            CodeBlock setPropertyStatement = generateSetCode(mapping, retrieveValueCode);

            mapMethod.addStatement(setPropertyStatement);
        }

        mapMethod.addStatement("return dst");

        ParameterizedTypeName superInterface = ParameterizedTypeName.get(
                ClassName.get(Mapper.class), srcType, dstType
        );

        TypeSpec type = TypeSpec.classBuilder(fqcn.className())
                .addSuperinterface(superInterface)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fields)
                .addMethod(mapMethod.build())
                .addMethods(methods)
                .build();

        JavaFile javaFile = JavaFile.builder(fqcn.packageName(), type)
                .indent("  ")
                .build();

        return javaFile::writeTo;
    }

    private static CodeBlock generateSetCode(Mapping mapping, CodeBlock retrieveValueCode) {
        List<Accessor> setters = mapping.getSetters();
//        if (setters.size() == 1) {
        return CodeBlock.of("dst.$L($L)", setters.get(0).getMethodName(), retrieveValueCode);
//        } else {
//            String setterMethodName = "set" + mapping.getSetter().getName() + "Value";
//            String varName = mapping.getSetter().getName();
//            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(setterMethodName)
//                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
//                    .addParameter(TypeName.get(mapping.getLastSetter().getValueType()), "dst")
//                    .addParameter(TypeName.get(mapping.getLastGetter().getValueType()), varName);
//            methodBuilder.beginControlFlow("if (dst == null)")
//                    .addStatement("return")
//                    .endControlFlow();
//
//            for (int i = 0; i < setters.size(); i++) {
//                Accessor setter = setters.get(i);
//                if (i == setters.size() - 1) {
//                    methodBuilder.addStatement("dst.$L($L)", setter.getMethodName(), varName);
//                } else {
//                    String nextVar = "var" + (i + 1);
//                    TypeName nextType = TypeName.get(setter.getValueType());
//                    methodBuilder.addStatement("$T $L = dst.$L()", nextType, nextVar, setter.getMethodName());
//                    methodBuilder.beginControlFlow("if ($L == null)", nextVar)
//                            .addStatement("$L = new $T()", nextVar, nextType.box())
//                            .addStatement("dst.$L($L)", setter.getMethodName(), nextVar)
//                            .endControlFlow();
//                    dst = nextVar;
//                }
//            }
//            methods.add(methodBuilder.build());
//            return CodeBlock.of("$L(dst, $L)", setterMethodName, retrieveValueCode);
//        }
    }

}
