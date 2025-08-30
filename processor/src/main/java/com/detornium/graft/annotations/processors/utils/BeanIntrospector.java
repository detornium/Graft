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

package com.detornium.graft.annotations.processors.utils;

import com.detornium.graft.annotations.processors.models.Accessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.List;

public class BeanIntrospector {

    private static final String SETTER_PREFIX = "set";
    private static final int SETTER_PREFIX_LENGTH = SETTER_PREFIX.length();
    private static final int SETTER_PARAMS_COUNT = 1;

    private static final String GETTER_PREFIX = "get";
    private static final int GETTER_PREFIX_LENGTH = GETTER_PREFIX.length();
    private static final String BOOLEAN_GETTER_PREFIX = "is";
    private static final int BOOLEAN_GETTER_PREFIX_LENGTH = BOOLEAN_GETTER_PREFIX.length();

    private final Elements elements;

    public BeanIntrospector(ProcessingEnvironment env) {
        this.elements = env.getElementUtils();
    }

    public List<Accessor> getAccessors(TypeElement beanType, Accessor.AccessorType type) {
        return switch (type) {
            case GETTER -> getGetters(beanType);
            case SETTER -> getSetters(beanType);
            case FIELD -> getFieldAccessors(beanType); // TODO check if needed
            case RECORD_FIELD -> getRecordAccessors(beanType);
        };
    }

    public Accessor getAccessor(ExecutableElement executableElement, Accessor.AccessorType type) {
        List<String> fieldNames = getAllFieldNames((TypeElement) executableElement.getEnclosingElement());

        return switch (type) {
            case GETTER -> getterMethodToAccessor(executableElement, fieldNames);
            case SETTER -> setterMethodToAccessor(executableElement, fieldNames);
            case FIELD -> throw new UnsupportedOperationException("Not implemented yet");
            case RECORD_FIELD -> recordGetterToAccessor(executableElement, fieldNames);
        };
    }

    private List<Accessor> getGetters(TypeElement beanType) {
        List<String> fieldNames = getAllFieldNames(beanType);

        return elements.getAllMembers(beanType).stream()
                .filter(this::isPublicNonStaticMethod)
                .map(ExecutableElement.class::cast)
                .filter(this::isGetter)
                .map(e -> getterMethodToAccessor(e, fieldNames))
                .toList();
    }

    private boolean isGetter(ExecutableElement element) {
        return !isObjectClassMethod(element)
                && (isRegularGetter(element) || isBooleanGetter(element));
    }

    private boolean isRegularGetter(ExecutableElement element) {
        return element.getSimpleName().toString().startsWith(GETTER_PREFIX)
                && element.getSimpleName().toString().length() > GETTER_PREFIX_LENGTH
                && element.getParameters().isEmpty()
                && element.getReturnType().getKind() != TypeKind.VOID;
    }

    private boolean isBooleanGetter(ExecutableElement element) {
        return element.getSimpleName().toString().startsWith(BOOLEAN_GETTER_PREFIX)
                && element.getSimpleName().toString().length() > BOOLEAN_GETTER_PREFIX_LENGTH
                && element.getParameters().isEmpty()
                && element.getReturnType().getKind() == TypeKind.BOOLEAN;
    }

    // Exclude methods from java.lang.Object e.g. getClass(), toString(), hashCode(), etc.
    private boolean isObjectClassMethod(ExecutableElement element) {
        TypeElement objectElement = elements.getTypeElement(Object.class.getCanonicalName());
        return element.getEnclosingElement().equals(objectElement);
    }

    private Accessor getterMethodToAccessor(ExecutableElement element, List<String> fieldNames) {
        String methodName = element.getSimpleName().toString();

        String name = methodName.startsWith(GETTER_PREFIX)
                ? methodName.substring(GETTER_PREFIX_LENGTH)
                : methodName.substring(BOOLEAN_GETTER_PREFIX_LENGTH);

        name = findBestMatchingFieldName(decapitalize(name), fieldNames);

        TypeMirror returnType = element.getReturnType();

        return new Accessor(name, methodName, returnType, Accessor.AccessorType.GETTER);
    }

    private List<Accessor> getSetters(TypeElement beanType) {
        List<String> fieldNames = getAllFieldNames(beanType);

        return elements.getAllMembers(beanType).stream()
                .filter(this::isPublicNonStaticMethod)
                .map(ExecutableElement.class::cast)
                .filter(this::isSetter)
                .map(e -> setterMethodToAccessor(e, fieldNames))
                .toList();
    }

    private boolean isSetter(ExecutableElement element) {
        return element.getSimpleName().toString().startsWith(SETTER_PREFIX)
                && element.getSimpleName().toString().length() > SETTER_PREFIX_LENGTH
                && element.getParameters().size() == SETTER_PARAMS_COUNT
                && element.getReturnType().getKind() == TypeKind.VOID
                && element.getModifiers().contains(Modifier.PUBLIC)
                && !element.getModifiers().contains(Modifier.STATIC);
    }

    private Accessor setterMethodToAccessor(ExecutableElement element, List<String> fieldNames) {
        String methodName = element.getSimpleName().toString();

        String name = findBestMatchingFieldName(decapitalize(methodName.substring(SETTER_PREFIX_LENGTH)), fieldNames);
        TypeMirror valueType = element.getParameters().get(0).asType();

        return new Accessor(name, methodName, valueType, Accessor.AccessorType.SETTER);
    }

    private boolean isPublicNonStaticMethod(Element element) {
        return element.getKind() == ElementKind.METHOD
                && element.getModifiers().contains(Modifier.PUBLIC)
                && !element.getModifiers().contains(Modifier.STATIC);
    }

    private List<String> getAllFieldNames(TypeElement beanType) {
        return elements.getAllMembers(beanType).stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> e.getSimpleName().toString())
                .toList();
    }


    private String findBestMatchingFieldName(String name, List<String> fieldNames) {
        return fieldNames.stream()
                .filter(f -> f.equalsIgnoreCase(name))
                .findFirst()
                .orElse(name);
    }

    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.length() > 1 && Character.isUpperCase(str.charAt(1)) && Character.isUpperCase(str.charAt(0))) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private List<Accessor> getFieldAccessors(TypeElement beanType) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private List<Accessor> getRecordAccessors(TypeElement beanType) {
        if (beanType.getKind() == ElementKind.RECORD) {
            return beanType.getRecordComponents().stream()
                    .map(this::recordComponentToAccessor)
                    .toList();
        }

        return List.of();
    }

    private Accessor recordComponentToAccessor(RecordComponentElement rc) {
        String name = rc.getSimpleName().toString();
        return new Accessor(
                name,
                name,
                rc.asType(),
                Accessor.AccessorType.RECORD_FIELD
        );
    }

    // TODO: generated, review
    private Accessor recordGetterToAccessor(ExecutableElement element, List<String> fieldNames) {
        String methodName = element.getSimpleName().toString();

        String name = findBestMatchingFieldName(methodName, fieldNames);

        TypeMirror returnType = element.getReturnType();

        return new Accessor(name, methodName, returnType, Accessor.AccessorType.RECORD_FIELD);
    }
}
