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

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class SpecificCollectionsBean {
    private ArrayList<String> arrayList;
    private LinkedList<String> linkedList;
    private Vector<String> vector;
    private CopyOnWriteArrayList<String> copyOnWriteArrayList;

    private HashSet<Integer> hashSet;
    private LinkedHashSet<Integer> linkedHashSet;
    private TreeSet<Integer> treeSet;
    private EnumSet<DayOfWeek> enumSet;
    private ConcurrentSkipListSet<Integer> concurrentSkipListSet;

    private HashMap<String, String> hashMap;
    private LinkedHashMap<String, String> linkedHashMap;
    private TreeMap<String, String> treeMap;
    private IdentityHashMap<String, String> identityHashMap;
    private EnumMap<DayOfWeek, String> enumMap;
    private WeakHashMap<String, String> weakHashMap;
    private ConcurrentHashMap<String, String> concurrentHashMap;
    private ConcurrentSkipListMap<String, String> concurrentSkipListMap;
}
