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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that unmapped fields in the destination type should be ignored.
 * When applied to a mapping spec class, the generated mapper will not raise errors or warnings
 * for any fields in the destination type that are not explicitly mapped from the source type.
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @IgnoreUnmapped
 * @MappingSpec(com.example.MyMapper.class)
 * class MyMapperSpec extends MappingDsl<SourceType, DestinationType> {
 *     {
 *         map(SourceType::getField).to(DestinationType::setField);
 *         // additional mappings...
 *     }
 * }
 * </pre>
 * <p>
 * * The annotation can be applied at the class level only.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface IgnoreUnmapped {
}
