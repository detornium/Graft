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

package com.detornium.graft.annotations;

import com.detornium.graft.Mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
/**
 * Specifies that a mapper should be generated based on the annotated mapping spec class.
 * The annotated class should extend MappingDsl<S, D> where S is the source type and D is the destination type.
 * The generated mapper will implement Mapper<S, D>.
 *
 * Example usage:
 * <pre>
 * {@code
 * @MappingSpec(com.example.MyMapper.class)
 * class MyMapperSpec extends MappingDsl<SourceType, DestinationType> {
 *     {
 *         map(SourceType::getField).to(DestinationType::setField);
 *         // additional mappings...
 *     }
 * }
 * }
 * </pre>
 */
public @interface MappingSpec {

    /**
     * Specifies the mapper class to be generated.
     * Can be provided as a fully qualified class name (e.g. com.example.MyMapper.class)
     * or as an imported simple class name (e.g. MyMapper.class).
     * If the package is not specified, the package of the annotated class is used.
     */
    Class<? extends Mapper<?, ?>> value();

}
