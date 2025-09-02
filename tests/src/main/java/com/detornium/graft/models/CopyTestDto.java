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

    public CloneableObject getObject() {
        return object;
    }

    public void setObject(CloneableObject object) {
        this.object = object;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    public Set<Integer> getSet() {
        return set;
    }

    public void setSet(Set<Integer> set) {
        this.set = set;
    }

    public SortedSet<Integer> getSortedSet() {
        return sortedSet;
    }

    public void setSortedSet(SortedSet<Integer> sortedSet) {
        this.sortedSet = sortedSet;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public ConcurrentMap<String, String> getConcurrentMap() {
        return concurrentMap;
    }

    public void setConcurrentMap(ConcurrentMap<String, String> concurrentMap) {
        this.concurrentMap = concurrentMap;
    }

    public SortedMap<String, String> getSortedMap() {
        return sortedMap;
    }

    public void setSortedMap(SortedMap<String, String> sortedMap) {
        this.sortedMap = sortedMap;
    }

    public NavigableMap<String, String> getNavigableMap() {
        return navigableMap;
    }

    public void setNavigableMap(NavigableMap<String, String> navigableMap) {
        this.navigableMap = navigableMap;
    }

    public int[] getArray() {
        return array;
    }

    public void setArray(int[] array) {
        this.array = array;
    }
}
