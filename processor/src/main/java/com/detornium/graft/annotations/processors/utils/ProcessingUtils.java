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

import com.detornium.graft.annotations.processors.models.ConstantValue;
import com.detornium.graft.annotations.processors.models.Fqcn;
import com.detornium.graft.annotations.processors.models.MemberRefInfo;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ProcessingUtils {

    private static final char SEPARATOR = '.';
    private static final String CLASS_IMPORT_PATTERN = ".%s";

    private final ProcessingEnvironment processingEnv;
    private final Trees trees;
    private final Elements elements;
    private final Types types;

    public ProcessingUtils(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.trees = Trees.instance(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    public Optional<Fqcn> resolveTypeFqcn(TypeMirror typeMirror, TypeElement definitionType) {
        if ((typeMirror.getKind() == TypeKind.DECLARED || typeMirror.getKind() == TypeKind.ERROR)
                && typeMirror instanceof DeclaredType dt && dt.asElement() instanceof TypeElement te) {

            String pkg = elements.getPackageOf(te).getQualifiedName().toString();
            String simple = te.getSimpleName().toString();

            if ((pkg == null || pkg.isEmpty()) && simple.contains(".")) {
                int idx = simple.lastIndexOf(SEPARATOR);
                pkg = simple.substring(0, idx);
                simple = simple.substring(idx + 1);
            } else {
                String className = simple;
                findClassImport(definitionType, className)
                        .map(i -> i.substring(0, i.length() - CLASS_IMPORT_PATTERN.formatted(className).length()))
                        .orElseGet(() -> elements.getPackageOf(definitionType).getQualifiedName().toString());
            }

            return Optional.of(new Fqcn(pkg, simple));
        }

        return Optional.empty();
    }

    private Optional<String> findClassImport(TypeElement type, String className) {
        TreePath path = trees.getPath(type);

        String ending = CLASS_IMPORT_PATTERN.formatted(className);

        return path.getCompilationUnit().getImports().stream()
                .map(ImportTree::getQualifiedIdentifier)
                .map(Tree::toString)
                .filter(i -> i.endsWith(ending))
                .findAny(); // only one import per class name
    }

    public List<BlockTree> findInitializerBlocks(TypeElement type) {
        ClassTree ct = trees.getTree(type);
        if (ct == null) {
            return Collections.emptyList();
        }

        return ct.getMembers().stream()
                .filter(member -> member.getKind() == Tree.Kind.BLOCK)
                .map(BlockTree.class::cast)
                .toList();
    }

    public Optional<MemberRefInfo> resolveMemberRef(TypeElement enclosingType, ExpressionTree expressionTree) {
        if (!(expressionTree instanceof MemberReferenceTree)) {
            return Optional.empty();
        }

        MemberReferenceTree mrt = (MemberReferenceTree) expressionTree;

        TreePath enclosingPath = trees.getPath(enclosingType);

        // Path to the method reference node
        TreePath refPath = new TreePath(enclosingPath, mrt);

        // (1) Referenced element (method/ctor). May be null for array ctor refs (int[]::new).
        Element el = trees.getElement(refPath);
        ExecutableElement exec = (el instanceof ExecutableElement ee) ? ee : null;

        // (2) Type of the whole method reference expression (the SAM type, e.g. Function<String,Integer>)
        TypeMirror samType = trees.getTypeMirror(refPath); // can be null if not yet attributed

        // (3) Qualifier (receiver) type: the expression/type before '::'
        TreePath qualPath = new TreePath(enclosingPath, mrt.getQualifierExpression());
        TypeMirror qualifierType = trees.getTypeMirror(qualPath);

        // (4) Method/ctor signature with generics applied to the receiver
        ExecutableType execResolved = null;
        if (exec != null && qualifierType != null && qualifierType.getKind() == TypeKind.DECLARED) {
            execResolved = (ExecutableType) types.asMemberOf((DeclaredType) qualifierType, exec);
        }

        return Optional.of(new MemberRefInfo(
                mrt.getName().toString(),                         // e.g., "setColor" or "new"
                exec,
                execResolved,                                     // params/return from receiver's viewpoint
                samType,                                          // e.g., Setter<CarDto,String>
                qualifierType,                                    // e.g., CarDto
                mrt.getMode() == MemberReferenceTree.ReferenceMode.NEW
        ));
    }

    // resolves literal values (e.g. "string", 1, 1.0, true) and static final constants (e.g. Integer.MAX_VALUE)
    // literal value is stored in ConstantValue.value, static field name in ConstantValue.staticFieldName and reference class in ConstantValue.type
    public Optional<ConstantValue> resolveConstantValue(TypeElement enclosingType, ExpressionTree expressionTree) {
        if (expressionTree == null) {
            return Optional.empty();
        }

        TreePath enclosingPath = trees.getPath(enclosingType);
        TreePath exprPath = new TreePath(enclosingPath, expressionTree);

        // (1) Literal value
        if (expressionTree instanceof LiteralTree lt) {
            // need same representation as in source code

            Object value = lt.getValue();
            if (value instanceof String) {
                value = "\"" + value + "\"";
            } else if (value instanceof Character) {
                value = "'" + value + "'";
            } else if (value == null) {
                value = "null";
            } else if (value instanceof Float) {
                value = value + "f";
            } else if (value instanceof Long) {
                value = value + "L";
            } else if (value instanceof Double) {
                value = value + "d";
            }

            return Optional.of(new ConstantValue(value.toString(), null, null));
        }

        // (2) Static final constant (e.g. Integer.MAX_VALUE)
        if (expressionTree instanceof MemberSelectTree mst) {
            Element el = trees.getElement(exprPath);
            if (el != null && el.getKind().isField() && el.getModifiers().containsAll(List.of(javax.lang.model.element.Modifier.STATIC, javax.lang.model.element.Modifier.FINAL))) {
                String fieldName = mst.getIdentifier().toString();
                TypeMirror type = el.getEnclosingElement().asType();
                Optional<Fqcn> fqcn = resolveTypeFqcn(type, enclosingType);
                if (fqcn.isPresent()) {
                    return Optional.of(new ConstantValue(null, fqcn.get(), fieldName));
                }
            }
        }

        return Optional.empty();
    }


}
