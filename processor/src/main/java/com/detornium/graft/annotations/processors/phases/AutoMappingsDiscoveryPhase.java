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
import com.detornium.graft.annotations.processors.ClassNotReadyException;
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

public class AutoMappingsDiscoveryPhase extends AbstractProcessingPhase {
    public static final String NAME = "Auto Mappings Discovery Phase";

    private final BeanIntrospector beanIntrospector;

    public AutoMappingsDiscoveryPhase(ProcessingEnvironment processingEnv) {
        this.beanIntrospector = new BeanIntrospector(processingEnv);
    }

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException, ClassNotReadyException {
        boolean disableAutoMapping = context.getSpec().getAnnotation(DisableAutoMapping.class) != null;

        TypeElement source = context.getSourceType();
        TypeElement target = context.getTargetType();

        List<Accessor> getters = isRecord(source)
                ? beanIntrospector.getAccessors(source, Accessor.AccessorType.RECORD_FIELD)
                : beanIntrospector.getAccessors(source, Accessor.AccessorType.GETTER);

        List<Accessor> setters = isRecord(target)
                ? beanIntrospector.getAccessors(target, Accessor.AccessorType.RECORD_FIELD)
                : beanIntrospector.getAccessors(target, Accessor.AccessorType.SETTER);

        List<Mapping> autoMappings = disableAutoMapping
                ? List.of()
                : createAutoMappings(getters, setters);

        context.setAutoMappings(autoMappings);
    }

    private List<Mapping> createAutoMappings(List<Accessor> getters, List<Accessor> setters) {
        List<Mapping> mappings = new ArrayList<>();
        for (Accessor setter : setters) {
            Accessor getter = getterForSetter(setter, getters);
            if (getter == null) {
                continue;
            }

            Mapping mapping = new Mapping();
            mapping.setSetters(List.of(setter));
            mapping.setGetters(List.of(getter));
            mappings.add(mapping);
        }
        return mappings;
    }

    private Accessor getterForSetter(Accessor setter, List<Accessor> getters) {
        return getters.stream()
                .filter(g -> g.getValueType().equals(setter.getValueType()))
                .filter(g -> g.getName().equals(setter.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
