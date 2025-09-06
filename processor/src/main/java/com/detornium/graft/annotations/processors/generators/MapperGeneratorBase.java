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

import com.detornium.graft.annotations.processors.models.*;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static com.detornium.graft.annotations.processors.generators.CodeSnippets.methodRefCode;
import static com.detornium.graft.annotations.processors.utils.Helpers.*;

abstract class MapperGeneratorBase implements MapperGenerator {

    private static final Map<Class<?>, Class<?>> DEFAULT_COLLECTION_IMPLEMENTATIONS = Map.ofEntries(
            // List
            Map.entry(List.class, ArrayList.class),

            // Set
            Map.entry(Set.class, HashSet.class),
            Map.entry(SortedSet.class, TreeSet.class),

            // Map
            Map.entry(WeakHashMap.class, WeakHashMap.class),
            Map.entry(Map.class, HashMap.class),
            Map.entry(ConcurrentHashMap.class, ConcurrentHashMap.class),
            Map.entry(ConcurrentMap.class, ConcurrentHashMap.class),
            Map.entry(SortedMap.class, TreeMap.class),
            Map.entry(NavigableMap.class, TreeMap.class)
    );

    protected CodeBlock generateConvertCode(Mapping mapping, ClassName srcType, List<FieldSpec> fields, CodeBlock retrieveValueCode) {
        MemberRefInfo converter = mapping.getConverter();
        if (converter != null) {
            Accessor setter = mapping.getSetter();
            Accessor getter = mapping.getLastGetter();

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

            retrieveValueCode = CodeBlock.of("$L.apply($L)", converterDefinitionName, retrieveValueCode);
        }
        return retrieveValueCode;
    }

    protected CodeBlock generateCloneCode(TypeElement src, Mapping mapping, CodeBlock retrieveValueCode) {
        if (mapping.isCopy()) {
            // check if cloneable
            // get src type
            CodeBlock cloneCode;

            Accessor getter = mapping.getLastGetter();
            TypeMirror srcValueType = (getter == null)
                    ? src.asType()
                    : getter.getValueType();

            if (isCloneable(srcValueType)) {
                // CLoneable
                cloneCode = CodeBlock.of("($T) ($L).clone()", srcValueType, retrieveValueCode);
            } else if (isCollection(srcValueType) || isMap(srcValueType)) {
                // Collection or Map with known clone strategy
                Class<?> implementation = DEFAULT_COLLECTION_IMPLEMENTATIONS.get(getClassForType(srcValueType));
                if (implementation == null) {
                    throw new IllegalStateException("Type %s is not Cloneable and no default implementation found".formatted(srcValueType));
                }
                cloneCode = CodeBlock.of("new $T<>($L)", implementation, retrieveValueCode);
            } else if (isArray(srcValueType)) {
                // Array
                cloneCode = CodeBlock.of("$L.clone()", retrieveValueCode);
            } else {
                throw new IllegalStateException("Type %s is not Cloneable".formatted(srcValueType));
            }

            // wrap with null check
            retrieveValueCode = CodeBlock.of("($L != null) ? $L : null", retrieveValueCode, cloneCode);
        }
        return retrieveValueCode;
    }

    protected CodeBlock generateValueRetrievalCode(MethodSpec.Builder methodBuilder, List<MethodSpec> methods, TypeElement src, Mapping mapping) {
        CodeBlock retrieveValueCode;

        List<Accessor> getters = mapping.getGetters();
        ConstantValue constantSrc = mapping.getConstant();

        if (constantSrc != null) {
            String value = constantSrc.getValue();
            if (value != null) {
                retrieveValueCode = CodeBlock.of("$L", value);
            } else {
                Fqcn constantType = constantSrc.getType();
                String constName = constantSrc.getStaticFieldName();
                if (constantType != null && constName != null) {
                    retrieveValueCode = CodeBlock.of("$T.$L",
                            ClassName.get(constantType.packageName(), constantType.className()),
                            constName);
                } else {
                    // should not happen
                    retrieveValueCode = CodeBlock.of("null");
                }
            }
        } else if (getters == null || getters.isEmpty()) {
            // src.this
            retrieveValueCode = CodeBlock.of("src");
        } else if (getters.size() == 1) {
            retrieveValueCode = CodeBlock.of("src.$L()", getters.get(0).getMethodName());
        } else {
            String getterMethodName = "get" + mapping.getSetter().getName() + "Value";
            String varName = mapping.getSetter().getName();
            methodBuilder.addStatement("$T $L = $L(src)",
                    TypeName.get(mapping.getLastGetter().getValueType()),
                    varName,
                    getterMethodName);
            methods.add(generateNestedGetterMethod(getterMethodName, src, getters));
            retrieveValueCode = CodeBlock.of("$L", varName);
        }

        return retrieveValueCode;
    }

    private MethodSpec generateNestedGetterMethod(String methodName, TypeElement srcType, List<Accessor> getters) {
        TypeName returnType = TypeName.get(getters.get(getters.size() - 1).getValueType());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .returns(returnType)
                .addParameter(ClassName.get(srcType), "src");

        methodBuilder.beginControlFlow("if (src == null)")
                .addStatement("return null")
                .endControlFlow();

        String currentVar = "src";
        for (int i = 0; i < getters.size() - 1; i++) {
            Accessor getter = getters.get(i);
            String nextVar = "var" + (i + 1);
            TypeName nextType = TypeName.get(getter.getValueType());
            methodBuilder.addStatement("$T $L = $L.$L()", nextType, nextVar, currentVar, getter.getMethodName());
            methodBuilder.beginControlFlow("if ($L == null)", nextVar)
                    .addStatement("return null")
                    .endControlFlow();
            currentVar = nextVar;
        }

        Accessor lastGetter = getters.get(getters.size() - 1);
        String retrieveValueCode = currentVar + "." + lastGetter.getMethodName() + "()";

        methodBuilder.addStatement("return $L", retrieveValueCode);

        return methodBuilder.build();
    }
}
