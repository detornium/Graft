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
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.detornium.graft.annotations.processors.generators.CodeSnippets.*;

public class GetterSetterMapperGenerator implements MapperGenerator {

    @Override
    public GeneratorResult generate(Fqcn fqcn,
                                    TypeElement src, TypeElement dst,
                                    List<Mapping> mappings) {

        ClassName srcType = ClassName.get(src);
        ClassName dstType = ClassName.get(dst);

        // Fields for converters would be added here if needed.
        List<FieldSpec> fields = new ArrayList<>();

        MethodSpec.Builder mapMethod = MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(Override.class)
                .returns(dstType)
                .addParameter(srcType, "src")
                .addCode(returnNullIfNullCode("src"))
                .addStatement(constructVariableStatement(dstType, "dst"));

        for (Mapping mapping : mappings) {
            if (mapping.isExclude()) continue;
            if (mapping.getSetter() == null) continue;

            Accessor setter = mapping.getSetter();
            String setterMethod = setter.getMethodName();

            Accessor getter = mapping.getGetter();
            String getterMethod = getter == null ? null : getter.getMethodName();

            MemberRefInfo converter = mapping.getConverter();
            ConstantValue constantSrc = mapping.getConstant();

            if (mapping.getConverter() == null) {
                if (constantSrc != null) {
                    String value = constantSrc.getValue();
                    if (value != null) {
                        mapMethod.addStatement("dst.$L($L)", setterMethod, value);
                    } else {
                        Fqcn constantType = constantSrc.getType();
                        String constName = constantSrc.getStaticFieldName();
                        if (constantType != null && constName != null) {
                            mapMethod.addStatement("dst.$L($T.$L)", setterMethod,
                                    ClassName.get(constantType.packageName(), constantType.className()),
                                    constName);
                        }
                    }

                } else if (getter == null) {
                    // src.this
                    mapMethod.addStatement("dst.$L(src)", setterMethod);
                } else {
                    mapMethod.addStatement("dst.$L(src.$L())", setterMethod, getterMethod);
                }
            } else {
                String converterDefinitionName = setter.getName() + "Converter";
                TypeName converterType = ParameterizedTypeName.get(
                        ClassName.get(Function.class),
                        getter == null ? srcType : ClassName.get(getter.getValueType()),
                        ClassName.get(setter.getValueType())
                );

                FieldSpec converterField = FieldSpec.builder(converterType, converterDefinitionName)
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("$L", methodRefCode(converter))
                        .build();

                fields.add(converterField);

                if (getter == null) {
                    // src.this with converter
                    mapMethod.addStatement("dst.$L($L.apply(src))", setterMethod, converterDefinitionName);
                } else {
                    mapMethod.addStatement("dst.$L($L.apply(src.$L()))", setterMethod, converterDefinitionName, getterMethod);
                }
            }
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
                .build();

        JavaFile javaFile = JavaFile.builder(fqcn.packageName(), type)
                .indent("  ")
                .build();

        return javaFile::writeTo;
    }
}
