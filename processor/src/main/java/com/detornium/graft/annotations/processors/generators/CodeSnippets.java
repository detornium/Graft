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

import com.detornium.graft.annotations.processors.models.MemberRefInfo;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import java.util.List;

class CodeSnippets {
    public static CodeBlock methodRefCode(MemberRefInfo info) {
        TypeMirror qual = info.qualifierType();

        // --- Canonical method references (no AST needed) ---
        if (info.constructorRef()) {
            if (qual != null && qual.getKind() == TypeKind.ARRAY) {
                ArrayType at = (ArrayType) qual;
                TypeName comp = TypeName.get(at.getComponentType());
                return CodeBlock.of("$L[]::new", comp);
            }
            if (qual != null && qual.getKind() == TypeKind.DECLARED) {
                ClassName cn = ClassName.get((TypeElement) ((DeclaredType) qual).asElement());
                return CodeBlock.of("$T::new", cn);
            }
        } else if (qual != null && qual.getKind() == TypeKind.DECLARED) {
            ClassName cn = ClassName.get((TypeElement) ((DeclaredType) qual).asElement());
            return CodeBlock.of("$T::$L", cn, info.name());
        }

        // Fallback: try enclosing type of the referenced element
        ExecutableElement el = info.element();
        if (el != null) {
            Element encl = el.getEnclosingElement();
            if (encl instanceof TypeElement te) {
                ClassName cn = ClassName.get(te);
                if (info.constructorRef()) return CodeBlock.of("$T::new", cn);
                return CodeBlock.of("$T::$L", cn, info.name());
            }
        }

        // --- Last resort: build a lambda equivalent ---
        return asLambda(info);
    }

    private static CodeBlock asLambda(MemberRefInfo info) {
        ExecutableElement el = info.element();
        ExecutableType mt = info.resolvedExecType();       // method signature (no receiver)
        if (el == null || mt == null) {
            return CodeBlock.of("(/*unresolved*/) -> { throw new UnsupportedOperationException(); }");
        }

        boolean isStatic = el.getModifiers().contains(Modifier.STATIC);
        List<? extends TypeMirror> mParams = mt.getParameterTypes();

        int samParamCount = -1; // optional: derive from info.samType() if you want to detect bound vs unbound
        // Simple parameter names p0, p1, ...
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < (samParamCount > -1 ? samParamCount : mParams.size() + (isStatic ? 0 : 1)); i++) {
            if (i > 0) params.append(", ");
            params.append("p").append(i);
        }

        // Static: (p0,p1)-> Type.m(p0,p1)
        if (isStatic) {
            ClassName owner = ClassName.get((TypeElement) el.getEnclosingElement());
            StringBuilder call = new StringBuilder();
            call.append("$T.").append(el.getSimpleName()).append("(");
            for (int i = 0; i < mParams.size(); i++) {
                if (i > 0) call.append(", ");
                call.append("p").append(i);
            }
            call.append(")");
            return CodeBlock.of("($L) -> " + call, params, owner);
        }

        // Instance, unbound: (recv,p0,p1)-> recv.m(p0,p1)
        StringBuilder call = new StringBuilder();
        call.append("p0.").append(el.getSimpleName()).append("(");
        for (int i = 0; i < mParams.size(); i++) {
            if (i > 0) call.append(", ");
            call.append("p").append(i + 1); // shift by 1 for receiver
        }
        call.append(")");
        return CodeBlock.of("($L) -> " + call, params);
    }

    public static CodeBlock returnNullIfNullCode(String varName) {
        return CodeBlock.builder()
                .beginControlFlow("if ($L == null)", varName)
                .addStatement("return null")
                .endControlFlow()
                .build();
    }

    public static CodeBlock constructVariableStatement(ClassName varType, String varName) {
        return CodeBlock.of("$T $L = new $T()", varType, varName, varType);
    }
}
