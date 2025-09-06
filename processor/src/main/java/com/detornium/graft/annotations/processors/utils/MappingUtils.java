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

package com.detornium.graft.annotations.processors.utils;

import com.detornium.graft.annotations.processors.models.Accessor;
import com.detornium.graft.annotations.processors.models.Mapping;

import java.util.ArrayList;
import java.util.List;

public final class MappingUtils {

    private MappingUtils() {
    }

    public static List<Mapping> createAutoMappings(List<Accessor> getters, List<Accessor> setters) {
        List<Mapping> mappings = new ArrayList<>();
        for (Accessor setter : setters) {
            Accessor getter = getterForSetter(setter, getters);
            if (getter == null) {
                continue;
            }

            Mapping mapping = new Mapping();
            mapping.setSetter(setter);
            mapping.setGetters(List.of(getter));
            mappings.add(mapping);
        }
        return mappings;
    }

    private static Accessor getterForSetter(Accessor setter, List<Accessor> getters) {
        return getters.stream()
                .filter(g -> g.getValueType().equals(setter.getValueType()))
                .filter(g -> g.getName().equals(setter.getName()))
                .findFirst()
                .orElse(null);
    }

    public static List<Mapping> mergeMappings(List<Mapping> explicit, List<Mapping> auto) {
        List<Mapping> result = new ArrayList<>(explicit);
        for (Mapping am : auto) {
            boolean found = false;
            for (Mapping em : explicit) {
                if (am.getSetter() != null && em.getSetter() != null &&
                        am.getSetter().getName().equals(em.getSetter().getName())) {
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
                if (m.getSetter() != null && acc.getName().equals(m.getSetter().getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) unmapped.add(acc.getName());
        }

        return unmapped;
    }

}
