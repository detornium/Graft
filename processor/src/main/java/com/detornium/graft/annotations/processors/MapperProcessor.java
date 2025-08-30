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

package com.detornium.graft.annotations.processors;

import com.detornium.graft.MappingDsl;
import com.detornium.graft.annotations.MappingSpec;
import com.detornium.graft.annotations.processors.generators.DestRecordMapperGenerator;
import com.detornium.graft.annotations.processors.generators.GetterSetterMapperGenerator;
import com.detornium.graft.annotations.processors.generators.MapperGenerator;
import com.detornium.graft.annotations.processors.models.*;
import com.detornium.graft.annotations.processors.utils.BeanIntrospector;
import com.detornium.graft.annotations.processors.utils.ProcessingUtils;
import com.sun.source.tree.*;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.detornium.graft.annotations.processors.utils.Helpers.*;
import static com.detornium.graft.annotations.processors.utils.MappingUtils.*;

//@AutoService(Processor.class)
@SupportedAnnotationTypes("com.detornium.graft.annotations.MappingSpec")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MapperProcessor extends AbstractProcessor {

    private static final List<List<String>> ALLOWED_CALL_CHAIN = List.of(
            List.of("map", "to"),
            List.of("map", "converting", "to"),
            List.of("exclude"),
            List.of("self", "to"),
            List.of("self", "converting", "to"),
            List.of("value", "to")
    );

    private Elements elements;
    private Filer filer;
    private Trees trees;

    private BeanIntrospector beanIntrospector;
    private ProcessingUtils processingUtils;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elements = env.getElementUtils();
        filer = env.getFiler();
        trees = Trees.instance(env);

        beanIntrospector = new BeanIntrospector(processingEnv);
        processingUtils = new ProcessingUtils(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> anns, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(MappingSpec.class)) {
            if (!(e instanceof TypeElement spec)) {
                error(e, "@%s can only be applied to classes.".formatted(MappingSpec.class.getSimpleName()));
                continue;
            }

            try {
                MappingSpec meta = e.getAnnotation(MappingSpec.class);

                DeclaredType st = findSuperclass(spec, MappingDsl.class, 2)
                        .orElseThrow(() -> new ProcessingException(spec, "Class must extend MappingDsl<S,D>."));

                TypeElement src = declaredTypeMirrorToTypeElement(st.getTypeArguments().get(0))
                        .orElseThrow(() -> new ProcessingException(spec, "Failed to resolve source type S."));

                TypeElement dst = declaredTypeMirrorToTypeElement(st.getTypeArguments().get(1))
                        .orElseThrow(() -> new ProcessingException(spec, "Failed to resolve destination type D."));

                Fqcn mapperFqcn = getAnnotationClassValue(
                        meta,
                        MappingSpec::value,
                        c -> Optional.<Fqcn>empty(), // error target class already exists
                        tm -> processingUtils.resolveTypeFqcn(tm, spec))
                        .orElseThrow(() -> new ProcessingException(spec, "Failed to resolve mapper class from @MappingSpec."));

                List<Mapping> mappings = processMappings(spec, src, dst);

                MapperGenerator mapperGenerator = isRecord(dst)
                        ? new DestRecordMapperGenerator()
                        : new GetterSetterMapperGenerator();

                mapperGenerator.generate(mapperFqcn, src, dst, mappings)
                        .writeTo(filer);

            } catch (ProcessingException procEx) {
                error(procEx.getElement(), "Processor failure: " + procEx.getMessage());
            } catch (Exception ex) {
                error(e, "Processor failure: " + ex.getMessage());
            }
        }
        return true;
    }

    private List<Mapping> processMappings(TypeElement spec, TypeElement src, TypeElement dst) throws ProcessingException {
        List<Accessor> getters = isRecord(src)
                ? beanIntrospector.getAccessors(src, Accessor.AccessorType.RECORD_FIELD)
                : beanIntrospector.getAccessors(src, Accessor.AccessorType.GETTER);

        List<Accessor> setters = isRecord(dst)
                ? beanIntrospector.getAccessors(dst, Accessor.AccessorType.RECORD_FIELD)
                : beanIntrospector.getAccessors(dst, Accessor.AccessorType.SETTER);

        List<Mapping> mappings = parseMappingsFromInitializers(spec, src, dst);
        List<Mapping> autoMappings = createAutoMappings(getters, setters);
        List<Mapping> allMappings = mergeMappings(mappings, autoMappings);

        List<String> unmapped = findUnmappedFields(allMappings, setters);
        if (!unmapped.isEmpty()) {
            throw new ProcessingException(spec, "Some destination fields are not mapped: " + String.join(", ", unmapped));
        }

        return allMappings;
    }

    private List<Mapping> parseMappingsFromInitializers(TypeElement spec, TypeElement src, TypeElement dst) throws ProcessingException {
        Function<ExpressionStatementTree, Mapping> expressionHandler = est -> {
            try {
                return handleExpression(spec, est.getExpression(), src, dst);
            } catch (ProcessingException e) {
                error(spec, e.getTree(), e.getMessage());
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
                case "map" -> {
                    MemberRefInfo memberRefInfo = processingUtils.resolveMemberRef(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a method reference."));

                    ExecutableElement executableElement = memberRefInfo.element();
                    Accessor getter = resolveGetter(executableElement, src);
                    mapping.setGetter(getter);
                }
                case "value" -> {
                    ConstantValue constValue = processingUtils.resolveConstantValue(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a constant value."));

                    mapping.setConstant(constValue);
                }
                case "self" -> {
                    mapping.setGetter(null); // mark as self
                }
                case "to" -> {
                    MemberRefInfo memberRefInfo = processingUtils.resolveMemberRef(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a method reference."));

                    // TODO: check MemberRefInfo::qualifierType for records
                    ExecutableElement executableElement = memberRefInfo.element();
                    Accessor setter = resolveSetter(executableElement, dst);
                    mapping.setSetter(setter);
                }
                case "converting" -> {
                    MemberRefInfo memberRefInfo = processingUtils.resolveMemberRef(spec, call.argument(0))
                            .orElseThrow(() -> new ProcessingException(call.argument(0), "Should be a method reference."));

                    mapping.setConverter(memberRefInfo); // lambda or method ref
                }
                case "exclude" -> {
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

    private void error(Element e, String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, e);
    }

    private void error(TypeElement spec, Tree t, String msg) {
        CompilationUnitTree compilationUnit = trees.getPath(spec).getCompilationUnit();
        trees.printMessage(Diagnostic.Kind.ERROR, msg, t, compilationUnit);
    }
}