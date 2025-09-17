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
package com.detornium.graft.annotations.processors.scanners;

import com.detornium.graft.annotations.processors.models.*;
import com.detornium.graft.annotations.processors.models.tree.*;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.detornium.graft.annotations.processors.Constants.*;
import static com.detornium.graft.annotations.processors.utils.CodeSnippets.methodRefCode;
import static com.detornium.graft.annotations.processors.utils.Helpers.*;

public class MapperGenerationVisitor implements NodeVisitor<ProcessingResult> {

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

    private final GenerationContext context;

    public MapperGenerationVisitor(GenerationContext context) {
        this.context = context;
    }

    @Override
    public ProcessingResult visit(GetterNode node, ProcessingResult in) {
        // ignore input, as we do not need to combine results from children
        CodeBlock result = context.getResolvedGetters()
                .computeIfAbsent(node, this::generateGetter);

        return new ProcessingResult(result);
    }

    @Override
    public ProcessingResult visit(SelfNode node) {
        return new ProcessingResult(CodeBlock.of(SOURCE_VAR_NAME));
    }

    private CodeBlock generateGetter(GetterNode node) {
        if (!(node.getParent() instanceof GetterNode parentNode)) {
            return CodeBlock.of("src.$L()", node.getAccessor().getMethodName());
        } else if (node.getTransientUsage() > 1) {
            // parent getter should be already resolved
            CodeBlock parentGetter = context.getResolvedGetters().get(parentNode);

            // if used more than once, store in a variable
            String varName = context.generateVarNameFromAccessor(SOURCE_VAR_NAME, node.getAccessor());
            TypeName typeName = ClassName.get(node.getAccessor().getValueType());
            CodeBlock varDeclaration = CodeBlock.builder()
                    .addStatement("$T $L = ($L != null) ? $L.$L() : null",
                            typeName,
                            varName,
                            parentGetter,
                            parentGetter,
                            node.getAccessor().getMethodName())
                    .add(System.lineSeparator())
                    .build();

            context.getStatements().add(varDeclaration);

            return CodeBlock.of(varName);
        } else {
            // parent getter should be already resolved
            CodeBlock parentGetter = context.getResolvedGetters().get(parentNode);

            return CodeBlock.of("($L != null ? $L.$L() : null)",
                    parentGetter,
                    parentGetter,
                    node.getAccessor().getMethodName());
        }
    }

    @Override
    public ProcessingResult visit(SetterNode node, ProcessingResult in) {
        CodeBlock valueRetrievalCode = in.unwrapSingle();
        Accessor accessor = node.getAccessor();
        return new ProcessingResult(new AccessorCode(accessor, valueRetrievalCode));
    }

    @Override
    public ProcessingResult visit(ConstructNode node, ProcessingResult in) {
        TypeName typeName = ClassName.get(node.getType());
        String variableName = node.getName();

        List<AccessorCode> accessorCodes = in.unwrapComposite();

        // construct
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement(CodeBlock.of("$T $L = new $T()", typeName, variableName, typeName));
        accessorCodes.forEach((accessorCode) -> {
            Accessor acc = accessorCode.accessor();
            CodeBlock resolveValueCode = accessorCode.code();
            builder.addStatement(CodeBlock.of("$L.$L($L)", variableName, acc.getMethodName(), resolveValueCode));
        });

        builder.add(System.lineSeparator());

        context.getStatements().add(builder.build());

        return new ProcessingResult(CodeBlock.of(variableName));
    }

    @Override
    public ProcessingResult visit(ConstructRecordNode node, ProcessingResult in) {
        TypeElement typeElement = declaredTypeMirrorToTypeElement(node.getType())
                .orElseThrow(() -> new IllegalStateException("Type element not found for type: " + node.getType()));

        TypeName typeName = ClassName.get(node.getType());
        String variableName = Objects.equals(node.getName(), TARGET_VAR_NAME)
                ? TARGET_VAR_NAME
                : context.generateVarName(node.getName());

        List<AccessorCode> accessorCodes = in.unwrapComposite();
        Map<String, CodeBlock> paramValues = accessorCodes.stream()
                .collect(Collectors.toMap(e -> e.accessor().getName(), AccessorCode::code));

        List<? extends RecordComponentElement> components = typeElement.getRecordComponents();
        List<CodeBlock> args = new ArrayList<>();
        for (RecordComponentElement component : components) {
            String argName = component.getSimpleName().toString();
            String type = component.asType().toString();
            CodeBlock retrieveValueCode = paramValues.containsKey(argName)
                    ? paramValues.get(argName)
                    : defaultValue(type);
            args.add(retrieveValueCode);
        }

        // construct
        CodeBlock argsBlock = CodeBlock.join(args, "," + System.lineSeparator());
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement("$T $L = new $T($L)", typeName, variableName, typeName, argsBlock);
        builder.add(System.lineSeparator());

        context.getStatements().add(builder.build());
        return new ProcessingResult(CodeBlock.of(variableName));
    }

    @Override
    public ProcessingResult visit(ConstantValueNode node) {
        return new ProcessingResult(generateConstantValueCode(node.getConstantValue()));
    }

    private CodeBlock generateConstantValueCode(ConstantValue constantSrc) {
        CodeBlock retrieveValueCode;
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
        return retrieveValueCode;
    }

    @Override
    public ProcessingResult visit(ConverterNode node, ProcessingResult in) {
        CodeBlock valueRetrievalCode = in.unwrapSingle();
        CodeBlock resultCode = generateConvertCode(
                node.getConverterRef(),
                node.getSourceType(),
                node.getTargetType(),
                valueRetrievalCode
        );

        return new ProcessingResult(resultCode);
    }

    private CodeBlock generateConvertCode(MemberRefInfo converter, TypeMirror sourceType, TypeMirror targetType, CodeBlock retrieveValueCode) {
        // TODO: reuse converter fields if already defined
        String converterDefinitionName = context.generateVarName(CONVERTER_VAR_NAME);
        TypeName converterType = ParameterizedTypeName.get(
                ClassName.get(Function.class),
                ClassName.get(sourceType),
                ClassName.get(targetType)
        );

        FieldSpec converterField = FieldSpec.builder(converterType, converterDefinitionName)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$L", methodRefCode(converter))
                .build();

        context.getFields().add(converterField);

        return CodeBlock.of("this.$L.apply($L)", converterDefinitionName, retrieveValueCode);
    }

    @Override
    public ProcessingResult visit(CopyValueNode node, ProcessingResult in) {
        CodeBlock valueRetrievalCode = in.unwrapSingle();
        CodeBlock resultCode = generateCloneCode(
                node.getSourceType(),
                valueRetrievalCode);

        return new ProcessingResult(resultCode);
    }

    private CodeBlock generateCloneCode(TypeMirror sourceType, CodeBlock retrieveValueCode) {
        CodeBlock cloneCode;

        if (isCloneable(sourceType)) {
            // CLoneable
            cloneCode = CodeBlock.of("($T) ($L).clone()", sourceType, retrieveValueCode);
        } else if (isCollection(sourceType) || isMap(sourceType)) {
            // Collection or Map with known clone strategy
            Class<?> implementation = DEFAULT_COLLECTION_IMPLEMENTATIONS.get(getClassForType(sourceType));
            if (implementation == null) {
                throw new IllegalStateException("Type %s is not Cloneable and no default implementation found".formatted(sourceType));
            }
            cloneCode = CodeBlock.of("new $T<>($L)", implementation, retrieveValueCode);
        } else if (isArray(sourceType)) {
            // Array
            cloneCode = CodeBlock.of("$L.clone()", retrieveValueCode);
        } else {
            throw new IllegalStateException("Type %s is not Cloneable".formatted(sourceType));
        }

        // wrap with null check
        return CodeBlock.of("($L != null) ? $L : null", retrieveValueCode, cloneCode);
    }

    private static CodeBlock defaultValue(String type) {
        return isPrimitive(type) ? CodeBlock.of(getZeroValue(type)) : CodeBlock.of("null");
    }

    private static boolean isPrimitive(String type) {
        return type.equals("int") || type.equals("long") || type.equals("double") ||
                type.equals("float") || type.equals("boolean") || type.equals("char") ||
                type.equals("byte") || type.equals("short");
    }

    private static String getZeroValue(String type) {
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

    @Override
    public ProcessingResult reduce(List<ProcessingResult> results) {
        return new ProcessingResult(results);
    }

    private record AccessorCode(Accessor accessor, CodeBlock code) {
    }

}
