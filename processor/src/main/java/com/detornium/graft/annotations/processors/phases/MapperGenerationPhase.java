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

import com.detornium.graft.annotations.processors.ProcessingException;
import com.detornium.graft.annotations.processors.generators.DestRecordMapperGenerator;
import com.detornium.graft.annotations.processors.generators.GetterSetterMapperGenerator;
import com.detornium.graft.annotations.processors.generators.MapperGenerator;
import com.detornium.graft.annotations.processors.models.MappingContext;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;

import static com.detornium.graft.annotations.processors.utils.Helpers.isRecord;

public class MapperGenerationPhase extends AbstractProcessingPhase {
    private static final String NAME = "Mapper Generation Phase";

    private final Filer filer;

    public MapperGenerationPhase(ProcessingEnvironment processingEnvironment) {
        this.filer = processingEnvironment.getFiler();
    }

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException {
        try {
            MapperGenerator mapperGenerator = isRecord(context.getTargetType())
                    ? new DestRecordMapperGenerator()
                    : new GetterSetterMapperGenerator();

            mapperGenerator.generate(context.getMapperType(),
                    context.getSourceType(),
                    context.getTargetType(),
                    context.getMappings()).writeTo(filer);
        } catch (IOException e) {
            // TODO log the exception
            throw new ProcessingException(context.getSpec(), "Failed to generate mapper class: %s".formatted(e.getMessage()));
        }
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
