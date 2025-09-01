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

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

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

    public ArrayList<String> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<String> arrayList) {
        this.arrayList = arrayList;
    }

    public LinkedList<String> getLinkedList() {
        return linkedList;
    }

    public void setLinkedList(LinkedList<String> linkedList) {
        this.linkedList = linkedList;
    }

    public Vector<String> getVector() {
        return vector;
    }

    public void setVector(Vector<String> vector) {
        this.vector = vector;
    }

    public CopyOnWriteArrayList<String> getCopyOnWriteArrayList() {
        return copyOnWriteArrayList;
    }

    public void setCopyOnWriteArrayList(CopyOnWriteArrayList<String> copyOnWriteArrayList) {
        this.copyOnWriteArrayList = copyOnWriteArrayList;
    }

    public HashSet<Integer> getHashSet() {
        return hashSet;
    }

    public void setHashSet(HashSet<Integer> hashSet) {
        this.hashSet = hashSet;
    }

    public LinkedHashSet<Integer> getLinkedHashSet() {
        return linkedHashSet;
    }

    public void setLinkedHashSet(LinkedHashSet<Integer> linkedHashSet) {
        this.linkedHashSet = linkedHashSet;
    }

    public TreeSet<Integer> getTreeSet() {
        return treeSet;
    }

    public void setTreeSet(TreeSet<Integer> treeSet) {
        this.treeSet = treeSet;
    }

    public EnumSet<DayOfWeek> getEnumSet() {
        return enumSet;
    }

    public void setEnumSet(EnumSet<DayOfWeek> enumSet) {
        this.enumSet = enumSet;
    }

    public ConcurrentSkipListSet<Integer> getConcurrentSkipListSet() {
        return concurrentSkipListSet;
    }

    public void setConcurrentSkipListSet(ConcurrentSkipListSet<Integer> concurrentSkipListSet) {
        this.concurrentSkipListSet = concurrentSkipListSet;
    }

    public HashMap<String, String> getHashMap() {
        return hashMap;
    }

    public void setHashMap(HashMap<String, String> hashMap) {
        this.hashMap = hashMap;
    }

    public LinkedHashMap<String, String> getLinkedHashMap() {
        return linkedHashMap;
    }

    public void setLinkedHashMap(LinkedHashMap<String, String> linkedHashMap) {
        this.linkedHashMap = linkedHashMap;
    }

    public TreeMap<String, String> getTreeMap() {
        return treeMap;
    }

    public void setTreeMap(TreeMap<String, String> treeMap) {
        this.treeMap = treeMap;
    }

    public IdentityHashMap<String, String> getIdentityHashMap() {
        return identityHashMap;
    }

    public void setIdentityHashMap(IdentityHashMap<String, String> identityHashMap) {
        this.identityHashMap = identityHashMap;
    }

    public EnumMap<DayOfWeek, String> getEnumMap() {
        return enumMap;
    }

    public void setEnumMap(EnumMap<DayOfWeek, String> enumMap) {
        this.enumMap = enumMap;
    }

    public WeakHashMap<String, String> getWeakHashMap() {
        return weakHashMap;
    }

    public void setWeakHashMap(WeakHashMap<String, String> weakHashMap) {
        this.weakHashMap = weakHashMap;
    }

    public ConcurrentHashMap<String, String> getConcurrentHashMap() {
        return concurrentHashMap;
    }

    public void setConcurrentHashMap(ConcurrentHashMap<String, String> concurrentHashMap) {
        this.concurrentHashMap = concurrentHashMap;
    }

    public ConcurrentSkipListMap<String, String> getConcurrentSkipListMap() {
        return concurrentSkipListMap;
    }

    public void setConcurrentSkipListMap(ConcurrentSkipListMap<String, String> concurrentSkipListMap) {
        this.concurrentSkipListMap = concurrentSkipListMap;
    }
}
