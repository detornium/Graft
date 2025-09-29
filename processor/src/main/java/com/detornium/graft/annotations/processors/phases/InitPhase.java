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
import com.detornium.graft.annotations.processors.models.TargetSuperInfo;
import com.detornium.graft.annotations.processors.utils.ProcessingUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Optional;

import static com.detornium.graft.annotations.processors.utils.Helpers.*;

public class InitPhase extends AbstractProcessingPhase {
    public static final String NAME = "Initialization Phase";

    private final ProcessingUtils processingUtils;
    private final Elements elementUtils;

    public InitPhase(ProcessingEnvironment processingEnv) {
        this.processingUtils = new ProcessingUtils(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
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

        TargetSuperInfo targetSuperInfo = resolveTargetSuperTypeInfo(meta, spec, src.asType(), target.asType());

        context.setTargetSuperInfo(targetSuperInfo);
        context.setSpec(spec);
        context.setSourceType(src);
        context.setTargetType(target);
        context.setMapperType(mapperFqcn);
    }

    private TargetSuperInfo resolveTargetSuperTypeInfo(MappingSpec meta, TypeElement spec, TypeMirror sourceType, TypeMirror targetType) throws ProcessingException {
        TypeMirror superTypeMirror = getAnnotationClassValue(
                meta,
                MappingSpec::targetSuperType,
                c -> elementUtils.getTypeElement(c.getCanonicalName()).asType(),
                tm -> tm);

        TypeElement superTypeElement = declaredTypeMirrorToTypeElement(superTypeMirror)
                .orElseThrow(() -> new ProcessingException(spec, "Failed to resolve target super type."));

        if (!isInterface(superTypeElement)) {
            throw new ProcessingException(spec, "Target super type must be an interface.");
        }

        ExecutableElement mappingMethod = processingUtils.findEffectiveSam(superTypeElement)
                .orElseThrow(() -> new ProcessingException(spec, "Target super type must be a functional interface."));

        // check if method has exactly 1 parameter
        if (mappingMethod.getParameters().size() != 1) {
            throw new ProcessingException(mappingMethod, "Mapping method in target super type must have exactly one parameter.");
        }

        // get generic type parameters
        List<TypeMirror> typeParams = superTypeElement.getTypeParameters().stream()
                .map(Element::asType)
                .toList();

        // check if method parameter type matches one of the generic type parameters
        TypeMirror paramType = mappingMethod.getParameters().get(0).asType();
        int sourceTypeParamIndex = typeParams.indexOf(paramType);
        if (sourceTypeParamIndex == -1 && !paramType.equals(sourceType)) {
            throw new ProcessingException(mappingMethod, "Mapping method parameter type must match source type or one of the generic type parameters of the target super type.");
        }

        // check if return type matches one of the generic type parameters
        TypeMirror returnType = mappingMethod.getReturnType();
        int targetTypeParamIndex = typeParams.indexOf(returnType);
        if (targetTypeParamIndex == -1 && !returnType.equals(targetType)) {
            throw new ProcessingException(mappingMethod, "Mapping method return type must match target type or one of the generic type parameters of the target super type.");
        }

        TargetSuperInfo result = new TargetSuperInfo();
        result.setTargetInterface(superTypeElement);
        result.setMethodName(mappingMethod.getSimpleName().toString());
        result.setSourceParamIndex(sourceTypeParamIndex == -1 ? null : sourceTypeParamIndex);
        result.setTargetParamIndex(targetTypeParamIndex == -1 ? null : targetTypeParamIndex);

        return result;
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
