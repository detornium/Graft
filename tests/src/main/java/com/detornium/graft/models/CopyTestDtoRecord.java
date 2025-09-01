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
package com.detornium.graft.models;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public record CopyTestDtoRecord(
        CloneableObject object,
        List<String> list,
        Set<Integer> set,
        SortedSet<Integer> sortedSet,
        Map<String, String> map,
        ConcurrentMap<String, String> concurrentMap,
        SortedMap<String, String> sortedMap,
        NavigableMap<String, String> navigableMap,
        int[] array
) {
}
