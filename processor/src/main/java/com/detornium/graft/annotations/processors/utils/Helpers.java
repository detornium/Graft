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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;

public final class Helpers {

    private Helpers() {
    }

    /**
     * Entry for single Class<?> members: get(ann, MyAnn::value).ifResolved(...).orElse(...);
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


    public static Optional<TypeElement> declaredTypeMirrorToTypeElement(TypeMirror tm) {
        if (tm.getKind() == TypeKind.DECLARED
                && tm instanceof DeclaredType dt
                && dt.asElement() instanceof TypeElement te) {
            return Optional.of(te);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<DeclaredType> findSuperclass(TypeElement spec, Class<?> superclass, Integer typeArgsCount) {
        TypeMirror cur = spec.getSuperclass();
        String canonicalName = superclass.getCanonicalName();

        while (cur.getKind() != TypeKind.NONE) {
            DeclaredType dt = (DeclaredType) cur;
            Element el = dt.asElement();
            if (el instanceof TypeElement te
                    && te.getQualifiedName().contentEquals(canonicalName)
                    && (typeArgsCount == null || dt.getTypeArguments().size() == typeArgsCount)) {
                return Optional.of(dt);
            }

            cur = ((TypeElement) el).getSuperclass();
        }

        return Optional.empty();
    }

}
