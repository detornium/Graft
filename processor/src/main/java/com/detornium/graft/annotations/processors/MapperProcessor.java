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

import com.detornium.graft.annotations.MappingSpec;
import com.detornium.graft.annotations.processors.models.MappingContext;
import com.detornium.graft.annotations.processors.phases.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.function.Predicate.not;

@SupportedAnnotationTypes("com.detornium.graft.annotations.MappingSpec")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MapperProcessor extends AbstractProcessor {

    private ProcessingPhase processingChain;
    private final List<MappingContext> processingContexts = new ArrayList<>();


    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        processingChain = new InitPhase(processingEnv)
                .andThen(new AnalyzeDependenciesPhase())
                .andThen(new ProcessMappingsPhase(processingEnv))
                .andThen(new MapperGenerationPhase(processingEnv));
    }

    @Override
    public boolean process(Set<? extends TypeElement> anns, RoundEnvironment roundEnv) {
        if (processingContexts.isEmpty()) {
            processingContexts.addAll(initContexts(roundEnv));
        }

        for (MappingContext mappingContext : processingContexts) {
            if (mappingContext.isProcessed()) {
                continue;
            }

            try {
                processingChain.process(mappingContext);
                mappingContext.setProcessed(true);
            } catch (ClassNotReadyException ignored) {
            } catch (ProcessingException processingException) {
                mappingContext.setProcessed(true);
                error(processingException.getElement(), "Processing failure: " + processingException.getMessage());
            } catch (Exception ex) {
                mappingContext.setProcessed(true);
                error(mappingContext.getSpec(), "Processor failure: " + ex.getMessage());
            }
        }

        // Final round, check for unprocessed items
        if (roundEnv.processingOver()) {
            processingContexts.stream()
                    .filter(not(MappingContext::isProcessed))
                    .map(MappingContext::getSpec)
                    .forEach(spec -> error(spec, "Could not process mapping specification due to unresolved dependencies."));
        }

        // Remove processed items
        processingContexts.removeIf(MappingContext::isProcessed);

        return true;
    }

    private List<MappingContext> initContexts(RoundEnvironment roundEnv) {
        return roundEnv.getElementsAnnotatedWith(MappingSpec.class).stream()
                .map(MappingContext::new)
                .toList();
    }

    private void error(Element e, String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, e);
    }
}