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
package com.detornium.graft.annotations.processors.scanners;

import java.util.List;
import java.util.Objects;

public class ProcessingResult {
    private final List<Object> objects;
    private final boolean composite;

    public ProcessingResult(List<ProcessingResult> results) {
        Objects.requireNonNull(results);
        this.objects = results.stream()
                .filter(Objects::nonNull)
                .map(r -> r.objects)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .toList();

        this.composite = true;
    }

    public ProcessingResult(Object object) {
        this.objects = List.of(object);
        this.composite = false;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> unwrapComposite() {
        if (!composite) {
            throw new IllegalStateException("Cannot unwrap composite from single result");
        }

        return (List<T>) objects;
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrapSingle() {
        if (composite) {
            throw new IllegalStateException("Cannot unwrap single from composite result");
        }

        return (T) objects.get(0);
    }
}
