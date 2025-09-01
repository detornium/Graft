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
import com.detornium.graft.annotations.processors.models.*;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.detornium.graft.annotations.processors.generators.CodeSnippets.returnNullIfNullCode;

public class DestRecordMapperGenerator extends MapperGeneratorBase {

    @Override
    public GeneratorResult generate(Fqcn fqcn,
                                    TypeElement src, TypeElement dst,
                                    List<Mapping> mappings) {

        ClassName srcType = ClassName.get(src);
        ClassName dstType = ClassName.get(dst);

        List<FieldSpec> fields = new ArrayList<>();

        Map<String, Mapping> mappingMap = mappings.stream()
                .collect(Collectors.toMap(m -> m.getSetter().getName(), m -> m));

        MethodSpec.Builder mapMethod = MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(Override.class)
                .returns(dstType)
                .addParameter(srcType, "src")
                .addCode(returnNullIfNullCode("src"));

        List<? extends RecordComponentElement> components = dst.getRecordComponents();
        List<CodeBlock> args = new LinkedList<>();

        for (RecordComponentElement component : components) {
            String destName = component.getSimpleName().toString();
            String type = component.asType().toString();

            CodeBlock retrieveValueCode;
            Mapping mapping = mappingMap.get(destName);
            if (mapping != null && !mapping.isExclude()) {
                Accessor setter = mapping.getSetter();
                Accessor getter = mapping.getGetter();
                String getterMethod = getter == null ? null : getter.getMethodName();

                ConstantValue constantSrc = mapping.getConstant();

                retrieveValueCode = generateValueRetrievalCode(constantSrc, getter, getterMethod);
                MemberRefInfo converter = mapping.getConverter();

                // Apply cloning if needed
                retrieveValueCode = generateCloneCode(src, mapping, getter, retrieveValueCode);

                // Apply converter if present
                retrieveValueCode = generateConvertCode(converter, setter, getter, srcType, fields, retrieveValueCode);
            } else if (isPrimitive(type)) {
                retrieveValueCode = CodeBlock.of("$L", getZeroValue(type));
            } else {
                retrieveValueCode = CodeBlock.of("null");
            }

            args.add(retrieveValueCode);
        }

        CodeBlock argsBlock = CodeBlock.join(args, "," + System.lineSeparator());
        mapMethod.addStatement("return new $T($L)", dstType, argsBlock);

        ParameterizedTypeName superInterface = ParameterizedTypeName.get(
                ClassName.get(Mapper.class), srcType, dstType
        );

        TypeSpec type = TypeSpec.classBuilder(fqcn.className())
                .addSuperinterface(superInterface)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fields)
                .addMethod(mapMethod.build())
                .build();

        JavaFile javaFile = JavaFile.builder(fqcn.packageName(), type)
                .indent("  ")
                .build();

        return javaFile::writeTo;
    }

    private boolean isPrimitive(String type) {
        return type.equals("int") || type.equals("long") || type.equals("double") ||
                type.equals("float") || type.equals("boolean") || type.equals("char") ||
                type.equals("byte") || type.equals("short");
    }

    private String getZeroValue(String type) {
        return switch (type) {
            case "int" -> "0";
            case "long" -> "0L";
            case "double" -> "0.0";
            case "float" -> "0.0f";
            case "boolean" -> "false";
            case "char" -> "'\\0'";
            case "byte" -> "(byte)0";
            case "short" -> "(short)0";
            default -> "null";
        };
    }
}