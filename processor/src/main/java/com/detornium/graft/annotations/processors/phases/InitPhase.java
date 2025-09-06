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

import com.detornium.graft.MappingDsl;
import com.detornium.graft.annotations.MappingSpec;
import com.detornium.graft.annotations.processors.ProcessingException;
import com.detornium.graft.annotations.processors.models.Fqcn;
import com.detornium.graft.annotations.processors.models.MappingContext;
import com.detornium.graft.annotations.processors.utils.ProcessingUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Optional;

import static com.detornium.graft.annotations.processors.utils.Helpers.*;

public class InitPhase extends AbstractProcessingPhase {
    public static final String NAME = "Initialization Phase";

    private final ProcessingUtils processingUtils;

    public InitPhase(ProcessingEnvironment processingEnv) {
        this.processingUtils = new ProcessingUtils(processingEnv);
    }

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException {
        Element elementToProcess = context.getElementToProcess();
        if (!(context.getElementToProcess() instanceof TypeElement spec)) {
            throw new ProcessingException(elementToProcess, "@%s can only be applied to classes.".formatted(MappingSpec.class.getSimpleName()));
        }

        MappingSpec meta = spec.getAnnotation(MappingSpec.class);

        DeclaredType st = findSuperclass(spec, MappingDsl.class, 2)
                .orElseThrow(() -> new ProcessingException(spec, "Class must extend MappingDsl<S,D>."));

        TypeElement src = declaredTypeMirrorToTypeElement(st.getTypeArguments().get(0))
                .orElseThrow(() -> new ProcessingException(spec, "Failed to resolve source type S."));

        TypeElement target = declaredTypeMirrorToTypeElement(st.getTypeArguments().get(1))
                .orElseThrow(() -> new ProcessingException(spec, "Failed to resolve target type D."));

        Fqcn mapperFqcn = getAnnotationClassValue(
                meta,
                MappingSpec::value,
                c -> Optional.<Fqcn>empty(), // error target class already exists
                tm -> processingUtils.resolveTypeFqcn(tm, spec))
                .orElseThrow(() -> new ProcessingException(spec, "Failed to resolve mapper class from @MappingSpec."));

        context.setSpec(spec);
        context.setSourceType(src);
        context.setTargetType(target);
        context.setMapperType(mapperFqcn);
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
