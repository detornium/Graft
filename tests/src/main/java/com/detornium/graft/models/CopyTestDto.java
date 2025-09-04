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

import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

@Data
public class CopyTestDto {
    private CloneableObject object;
    private List<String> list;
    private Set<Integer> set;
    private SortedSet<Integer> sortedSet;
    private Map<String, String> map;
    private ConcurrentMap<String, String> concurrentMap;
    private SortedMap<String, String> sortedMap;
    private NavigableMap<String, String> navigableMap;
    private int[] array;
}
