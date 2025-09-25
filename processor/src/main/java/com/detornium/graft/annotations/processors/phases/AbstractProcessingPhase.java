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
import com.detornium.graft.annotations.processors.ProcessingException;
import com.detornium.graft.annotations.processors.models.MappingContext;

public abstract class AbstractProcessingPhase implements ProcessingPhase {

    @Override
    public final void process(MappingContext context) throws ProcessingException, ClassNotReadyException {
        if (!context.getProcessedPhaseNames().contains(getName())) {
            doProcess(context);
            context.addProcessedPhaseName(getName());
        }
    }

    protected abstract void doProcess(MappingContext context) throws ProcessingException, ClassNotReadyException;

    protected abstract String getName();
}
