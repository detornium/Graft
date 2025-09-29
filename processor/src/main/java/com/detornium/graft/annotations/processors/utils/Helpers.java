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

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;

public final class Helpers {

    private Helpers() {
    }

    /**
     * Utility method to safely extract a class value from an annotation.
     * This method handles the MirroredTypeException that occurs when trying to access
     * a class value in an annotation during annotation processing.
     *
     * @param ann           The annotation instance.
     * @param getter        A function to get the class value from the annotation.
     * @param reolvedMapper A function to map the resolved class to the desired return type.
     * @param mirrorMapper  A function to map the TypeMirror to the desired return type if MirroredTypeException occurs.
     * @param <A>           The type of the annotation.
     * @param <R>           The return type.
     * @param <C>           The type of the class being extracted.
     * @return The mapped value of type R.
     */
    public static <A extends Annotation, R, C extends Class<?>> R getAnnotationClassValue(A ann, Function<A, C> getter,
                                                                                          Function<C, R> reolvedMapper,
                                                                                          Function<TypeMirror, R> mirrorMapper) {
        try {
            return reolvedMapper.apply(getter.apply(ann));
        } catch (MirroredTypeException mte) {
            return mirrorMapper.apply(mte.getTypeMirror());
        }
    }


    public static boolean isRecord(TypeElement type) {
        return type.getKind() == ElementKind.RECORD;
    }

    public static boolean isInterface(TypeElement type) {
        return type.getKind() == ElementKind.INTERFACE;
    }

    public static boolean isPublic(Element type) {
        return type.getModifiers().contains(Modifier.PUBLIC);
    }

    public static boolean isProtected(Element type) {
        return type.getModifiers().contains(Modifier.PROTECTED);
    }

    public static boolean hasNoArgs(ExecutableElement element) {
        return element.getParameters().isEmpty();
    }

    public static boolean isConstructor(ExecutableElement element) {
        return element.getKind() == ElementKind.CONSTRUCTOR;
    }

    public static boolean isCloneable(TypeMirror tm) {
        return declaredTypeMirrorToTypeElement(tm)
                .flatMap(te -> findSuperclass(te, Cloneable.class, 0))
                .isPresent();
    }

    public static boolean isMap(TypeMirror tm) {
        return declaredTypeMirrorToTypeElement(tm)
                .flatMap(te -> findSuperclass(te, java.util.Map.class, 2))
                .isPresent();
    }

    public static boolean isCollection(TypeMirror tm) {
        return declaredTypeMirrorToTypeElement(tm)
                .flatMap(te -> findSuperclass(te, java.util.Collection.class, 1))
                .isPresent();
    }

    public static boolean isArray(TypeMirror tm) {
        return tm.getKind() == TypeKind.ARRAY;
    }

    public static Optional<TypeElement> declaredTypeMirrorToTypeElement(TypeMirror tm) {
        if (tm.getKind() == TypeKind.DECLARED
                && tm instanceof DeclaredType dt
                && dt.asElement() instanceof TypeElement te) {
            return Optional.of(te);
        } else {
            return Optional.empty();
        }
    }

    public static Class<?> getClassForType(TypeMirror tm) {
        return declaredTypeMirrorToTypeElement(tm)
                .map(te -> {
                    try {
                        return Class.forName(te.getQualifiedName().toString());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException("Cannot find class for type mirror: %s".formatted(tm), e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("Type mirror is not a declared type: %s".formatted(tm)));
    }

    public static Optional<DeclaredType> findSuperclass(TypeMirror tm, Class<?> superclass, Integer typeArgsCount) {
        return declaredTypeMirrorToTypeElement(tm)
                .flatMap(te -> findSuperclass(te, superclass, typeArgsCount));
    }

    public static Optional<DeclaredType> findSuperclass(TypeElement type, Class<?> classToFind, Integer typeArgsCount) {
        String canonicalName = classToFind.getCanonicalName();

        Queue<TypeMirror> classes = new LinkedList<>(List.of(type.asType()));

        while (!classes.isEmpty()) {
            TypeMirror current = classes.poll();
            if (current.getKind() == TypeKind.DECLARED && current instanceof DeclaredType dt) {
                Element el = dt.asElement();

                if (el instanceof TypeElement te
                        && te.getQualifiedName().contentEquals(canonicalName)
                        && (typeArgsCount == null || dt.getTypeArguments().size() == typeArgsCount)) {
                    return Optional.of(dt);
                }

                if (el instanceof TypeElement te) {
                    classes.addAll(te.getInterfaces());
                    TypeMirror superClass = te.getSuperclass();
                    if (superClass.getKind() != TypeKind.NONE) {
                        classes.add(superClass);
                    }
                }
            }
        }

        return Optional.empty();
    }
}
