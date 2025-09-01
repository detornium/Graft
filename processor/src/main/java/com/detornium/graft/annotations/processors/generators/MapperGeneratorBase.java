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

    protected static CodeBlock generateConvertCode(MemberRefInfo converter, Accessor setter, Accessor getter, ClassName srcType, List<FieldSpec> fields, CodeBlock retrieveValueCode) {
        if (converter != null) {
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

    protected static CodeBlock generateCloneCode(TypeElement src, Mapping mapping, Accessor getter, CodeBlock retrieveValueCode) {
        if (mapping.isCopy()) {
            // check if cloneable
            // get src type
            CodeBlock cloneCode;
            TypeMirror srcValueType = (getter == null)
                    ? src.asType()
                    : getter.getValueType();

            TypeMirror srcValueTypeMirror = (getter == null)
                    ? src.asType()
                    : getter.getValueType();

            if (isCloneable(srcValueType)) {
                // CLoneable
                cloneCode = CodeBlock.of("($T) ($L).clone()", srcValueTypeMirror, retrieveValueCode);
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

    protected static CodeBlock generateValueRetrievalCode(ConstantValue constantSrc, Accessor getter, String getterMethod) {
        CodeBlock retrieveValueCode;
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
        } else if (getter == null) {
            // src.this
            retrieveValueCode = CodeBlock.of("src");
        } else {
            retrieveValueCode = CodeBlock.of("src.$L()", getterMethod);
        }
        return retrieveValueCode;
    }
}
