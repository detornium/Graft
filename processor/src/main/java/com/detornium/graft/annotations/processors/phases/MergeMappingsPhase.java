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

import com.detornium.graft.annotations.IgnoreUnmapped;
import com.detornium.graft.annotations.processors.ProcessingException;
import com.detornium.graft.annotations.processors.models.Accessor;
import com.detornium.graft.annotations.processors.models.Mapping;
import com.detornium.graft.annotations.processors.models.MappingContext;
import com.detornium.graft.annotations.processors.utils.BeanIntrospector;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

import static com.detornium.graft.annotations.processors.utils.Helpers.isRecord;

public class MergeMappingsPhase extends AbstractProcessingPhase {
    private static final String NAME = "Merge Mappings Phase";

    private final BeanIntrospector beanIntrospector;

    public MergeMappingsPhase(ProcessingEnvironment processingEnv) {
        this.beanIntrospector = new BeanIntrospector(processingEnv);
    }

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException {
        TypeElement spec = context.getSpec();
        TypeElement target = context.getTargetType();

        List<Mapping> explicit = context.getExplicitMappings();
        List<Mapping> auto = context.getAutoMappings();

        List<Mapping> mappings = mergeMappings(explicit, auto);

        boolean ignoreUnmapped = spec.getAnnotation(IgnoreUnmapped.class) != null;

        // TODO: move introspector to context?
        List<Accessor> setters = isRecord(target)
                ? beanIntrospector.getAccessors(target, Accessor.AccessorType.RECORD_FIELD)
                : beanIntrospector.getAccessors(target, Accessor.AccessorType.SETTER);

        if (!ignoreUnmapped) {
            List<String> unmapped = findUnmappedFields(mappings, setters);
            if (!unmapped.isEmpty()) {
                throw new ProcessingException(spec, "Some target fields are not mapped: " + String.join(", ", unmapped));
            }
        }

        context.setMappings(mappings);
    }

    private List<Mapping> mergeMappings(List<Mapping> explicit, List<Mapping> auto) {
        List<Mapping> result = new ArrayList<>(explicit);
        for (Mapping am : auto) {
            boolean found = false;
            for (Mapping em : explicit) {
                if (am.getFirstSetter() != null && em.getFirstSetter() != null &&
                        am.getFirstSetter().getName().equals(em.getFirstSetter().getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) result.add(am);
        }
        return result;
    }

    public static List<String> findUnmappedFields(List<Mapping> mappings, List<Accessor> accessors) {
        List<String> unmapped = new ArrayList<>();
        for (Accessor acc : accessors) {
            boolean found = false;
            for (Mapping m : mappings) {
                if (!m.getSetters().isEmpty() && acc.getName().equals(m.getFirstSetter().getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) unmapped.add(acc.getName());
        }

        return unmapped;
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
