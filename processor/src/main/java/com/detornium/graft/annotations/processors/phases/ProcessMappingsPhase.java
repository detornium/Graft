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
package com.detornium.graft.annotations.processors.phases;

import com.detornium.graft.annotations.DisableAutoMapping;
import com.detornium.graft.annotations.IgnoreUnmapped;
import com.detornium.graft.annotations.processors.ProcessingException;
import com.detornium.graft.annotations.processors.models.*;
import com.detornium.graft.annotations.processors.utils.BeanIntrospector;
import com.detornium.graft.annotations.processors.utils.ProcessingUtils;
import com.sun.source.tree.*;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.detornium.graft.annotations.processors.utils.Helpers.*;
import static com.detornium.graft.annotations.processors.utils.MappingUtils.*;

public class ProcessMappingsPhase extends AbstractProcessingPhase {
    private static final String NAME = "Process Mappings Phase";

    // TODO: use tree for possible combinations hint
    private static final List<List<String>> ALLOWED_CALL_CHAIN = List.of(
            List.of("map", "to"),
            List.of("map", "converting", "to"),
            List.of("exclude"),
            List.of("self", "to"),
            List.of("self", "converting", "to"),
            List.of("value", "to"),
            List.of("self", "copy", "to"),
            List.of("map", "copy", "to")
    );

    private static final String MAP_INSTR = "map";
    private static final String VALUE_INSTR = "value";
    private static final String SELF_INSTR = "self";
    private static final String COPY_INSTR = "copy";
    private static final String CONVERTING_INSTR = "converting";
    private static final String TO_INSTR = "to";
    private static final String EXCLUDE_INSTR = "exclude";

    private final Trees trees;

    private final BeanIntrospector beanIntrospector;
    private final ProcessingUtils processingUtils;

    public ProcessMappingsPhase(ProcessingEnvironment processingEnv) {
        this.trees = Trees.instance(processingEnv);
        this.beanIntrospector = new BeanIntrospector(processingEnv);
        this.processingUtils = new ProcessingUtils(processingEnv);
    }

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException {
        TypeElement spec = context.getSpec();
        TypeElement source = context.getSourceType();
        TypeElement target = context.getTargetType();

        boolean ignoreUnmapped = spec.getAnnotation(IgnoreUnmapped.class) != null;
        boolean disableAutoMapping = spec.getAnnotation(DisableAutoMapping.class) != null;

        List<Accessor> getters = isRecord(source)
                ? beanIntrospector.getAccessors(source, Accessor.AccessorType.RECORD_FIELD)
                : beanIntrospector.getAccessors(source, Accessor.AccessorType.GETTER);

        List<Accessor> setters = isRecord(target)
                ? beanIntrospector.getAccessors(target, Accessor.AccessorType.RECORD_FIELD)
                : beanIntrospector.getAccessors(target, Accessor.AccessorType.SETTER);

        List<Mapping> mappings = parseMappingsFromInitializers(spec, source, target);
        List<Mapping> autoMappings = disableAutoMapping
                ? List.of()
                : createAutoMappings(getters, setters);
        List<Mapping> allMappings = mergeMappings(mappings, autoMappings);

        List<String> unmapped = findUnmappedFields(allMappings, setters);
        if (!ignoreUnmapped && !unmapped.isEmpty()) {
            throw new ProcessingException(spec, "Some target fields are not mapped: " + String.join(", ", unmapped));
        }

        context.setMappings(allMappings);
    }

    private List<Mapping> parseMappingsFromInitializers(TypeElement spec, TypeElement src, TypeElement dst) throws ProcessingException {
        Function<ExpressionStatementTree, Mapping> expressionHandler = est -> {
            try {
                return handleExpression(spec, est.getExpression(), src, dst);
            } catch (ProcessingException e) {
                // TODO refactor error reporting
                CompilationUnitTree compilationUnit = trees.getPath(spec).getCompilationUnit();
                trees.printMessage(Diagnostic.Kind.ERROR, e.getMessage(), e.getTree(), compilationUnit);
                return null;
            }
        };

        List<? extends StatementTree> statements = processingUtils.findInitializerBlocks(spec).stream()
                .map(BlockTree::getStatements)
                .flatMap(List::stream)
                .toList();

        List<Mapping> mappings = statements.stream()
                .filter(ExpressionStatementTree.class::isInstance)
                .map(ExpressionStatementTree.class::cast)
                .map(expressionHandler)
                .filter(Objects::nonNull)
                .toList();

        if (statements.size() != mappings.size()) {
            throw new ProcessingException(spec, "All statements in initializer blocks must be mapping specifications.");
        }

        return mappings;
    }

    private Mapping handleExpression(TypeElement spec, ExpressionTree expr, TypeElement src, TypeElement dst) throws ProcessingException {
        if (!(expr instanceof MethodInvocationTree)) {
            throw new ProcessingException(expr, "Mapping specification must be a method call chain.");
        }

        List<Call> callChain = buildCallChain((MethodInvocationTree) expr);

        if (!isValidCallChain(callChain)) {
            throw new ProcessingException(expr, "Invalid method call chain in mapping specification.");
        }

        Mapping mapping = new Mapping();
        for (Call call : callChain) {
            String callName = call.methodName();
            switch (callName) {
                case MAP_INSTR -> {
                    MemberRefInfo memberRefInfo = processingUtils.resolveMemberRef(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a method reference."));

                    ExecutableElement executableElement = memberRefInfo.element();
                    Accessor getter = resolveGetter(executableElement, src);
                    mapping.setGetter(getter);
                }
                case VALUE_INSTR -> {
                    ConstantValue constValue = processingUtils.resolveConstantValue(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a constant value."));

                    mapping.setConstant(constValue);
                }
                case SELF_INSTR -> {
                    mapping.setGetter(null); // mark as self
                }
                case COPY_INSTR -> {
                    // check if getter return is Cloneable, Map, Collection or array
                    TypeMirror srcPropertyType = mapping.getGetter() == null
                            ? src.asType()
                            : mapping.getGetter().getValueType();

                    if (!isCloneable(srcPropertyType) && !isMap(srcPropertyType)
                            && !isCollection(srcPropertyType) && !isArray(srcPropertyType)) {
                        throw new ProcessingException(expr, "Cloning is only supported for Cloneable, Map, Collection or array types.");
                    }

                    mapping.setCopy(true);
                }
                case CONVERTING_INSTR -> {
                    MemberRefInfo memberRefInfo = processingUtils.resolveMemberRef(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a method reference."));

                    mapping.setConverter(memberRefInfo); // lambda or method ref
                }
                case TO_INSTR -> {
                    MemberRefInfo memberRefInfo = processingUtils.resolveMemberRef(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a method reference."));

                    // TODO: check MemberRefInfo::qualifierType for records
                    ExecutableElement executableElement = memberRefInfo.element();
                    Accessor setter = resolveSetter(executableElement, dst);
                    mapping.setSetter(setter);
                }
                case EXCLUDE_INSTR -> {
                    MemberRefInfo memberRefInfo = processingUtils.resolveMemberRef(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a method reference."));

                    // TODO: check MemberRefInfo::qualifierType for records

                    ExecutableElement executableElement = memberRefInfo.element();

                    Accessor setter = resolveSetter(executableElement, dst);
                    mapping.setSetter(setter);
                    mapping.setExclude(true);
                }
                default -> {
                    throw new ProcessingException(expr, "Unexpected method call '%s' in mapping specification.".formatted(callName));
                }
            }
        }

        return mapping;
    }

    private Accessor resolveGetter(ExecutableElement executableElement, TypeElement type) {
        Accessor.AccessorType accessorType = isRecord(type)
                ? Accessor.AccessorType.RECORD_FIELD
                : Accessor.AccessorType.GETTER;

        return beanIntrospector.getAccessor(executableElement, accessorType);
    }

    private Accessor resolveSetter(ExecutableElement executableElement, TypeElement type) {
        Accessor.AccessorType accessorType = isRecord(type)
                ? Accessor.AccessorType.RECORD_FIELD
                : Accessor.AccessorType.SETTER;

        return beanIntrospector.getAccessor(executableElement, accessorType);
    }

    private List<Call> buildCallChain(MethodInvocationTree expr) {
        // TODO: refactor
        List<Call> result = Stream.iterate(expr,
                        Objects::nonNull,
                        prev -> prev.getMethodSelect() instanceof MemberSelectTree
                                ? (MethodInvocationTree) ((MemberSelectTree) prev.getMethodSelect()).getExpression()
                                : null)
                .map(call -> new Call(
                        call.getMethodSelect() instanceof IdentifierTree
                                ? ((IdentifierTree) call.getMethodSelect()).getName().toString()
                                : ((MemberSelectTree) call.getMethodSelect()).getIdentifier().toString(),
                        call.getArguments()
                ))
                .collect(Collectors.toList());

        Collections.reverse(result);

        return result;
    }

    private record Call(String methodName, List<? extends ExpressionTree> arguments) {
        public ExpressionTree argument(int idx) {
            return arguments.get(idx);
        }
    }

    private boolean isValidCallChain(List<Call> calls) {
        List<String> callNames = calls.stream()
                .map(Call::methodName)
                .toList();

        return ALLOWED_CALL_CHAIN.stream()
                .anyMatch(allowed -> allowed.equals(callNames));
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
