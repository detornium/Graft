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

import com.detornium.graft.annotations.processors.ClassNotReadyException;
import com.detornium.graft.annotations.processors.models.MappingContext;
import com.detornium.graft.annotations.processors.spi.ClassReadyCheck;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.ServiceLoader;

public class AnalyzeDependenciesPhase extends AbstractProcessingPhase {
    private static final String NAME = "Analyze Dependencies Phase";

    private final ServiceLoader<ClassReadyCheck> serviceLoader;

    public AnalyzeDependenciesPhase() {
        serviceLoader = ServiceLoader.load(ClassReadyCheck.class, getClass().getClassLoader());
    }

    @Override
    protected void doProcess(MappingContext context) throws ClassNotReadyException {
        for (TypeElement type : List.of(context.getSourceType(), context.getTargetType())) {
            for (ClassReadyCheck check : serviceLoader) {
                if (!check.isClassReady(type.asType())) {
                    throw new ClassNotReadyException(type);
                }
            }
        }
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
