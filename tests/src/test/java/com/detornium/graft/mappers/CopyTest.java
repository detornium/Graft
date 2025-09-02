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
package com.detornium.graft.mappers;

import com.detornium.graft.models.*;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CopyTest {

    @Test
    void testCopy() {
        CopyTestBean src = createCopyTestBean();
        CopyTestDto dest = new CopyTestMapper().map(src);

        assertNotNull(dest);

        // Ensure clone (different instances)
        assertNotSame(src.getObject(), dest.getObject());
        assertNotSame(src.getList(), dest.getList());
        assertNotSame(src.getSet(), dest.getSet());
        assertNotSame(src.getSortedSet(), dest.getSortedSet());
        assertNotSame(src.getMap(), dest.getMap());
        assertNotSame(src.getConcurrentMap(), dest.getConcurrentMap());
        assertNotSame(src.getSortedMap(), dest.getSortedMap());
        assertNotSame(src.getNavigableMap(), dest.getNavigableMap());
        assertNotSame(src.getArray(), dest.getArray());

        // Ensure fields are copied correctly
        assertNotNull(dest.getObject());
        assertEquals(src.getObject().getValue1(), dest.getObject().getValue1());
        assertEquals(src.getObject().getValue2(), dest.getObject().getValue2());
        assertEquals(src.getList(), dest.getList());
        assertEquals(src.getSet(), dest.getSet());
        assertEquals(src.getSortedSet(), dest.getSortedSet());
        assertEquals(src.getMap(), dest.getMap());
        assertEquals(src.getConcurrentMap(), dest.getConcurrentMap());
        assertEquals(src.getSortedMap(), dest.getSortedMap());
        assertEquals(src.getNavigableMap(), dest.getNavigableMap());
        assertArrayEquals(src.getArray(), dest.getArray());
    }

    @Test
    void testCopyNulls() {
        CopyTestBean src = new CopyTestBean();
        CopyTestDto dest = new CopyTestMapper().map(src);

        assertNotNull(dest);
        assertNull(dest.getObject());
        assertNull(dest.getList());
        assertNull(dest.getSet());
        assertNull(dest.getMap());
        assertNull(dest.getArray());
    }

    @Test
    void testCopyToRecord() {
        CopyTestBean src = createCopyTestBean();

        CopyTestDtoRecord dest = new CopyToRecordTestMapper().map(src);
        assertNotNull(dest);

        // Ensure clone (different instances)
        assertNotSame(src.getObject(), dest.object());
        assertNotSame(src.getList(), dest.list());
        assertNotSame(src.getSet(), dest.set());
        assertNotSame(src.getSortedSet(), dest.sortedSet());
        assertNotSame(src.getMap(), dest.map());
        assertNotSame(src.getConcurrentMap(), dest.concurrentMap());
        assertNotSame(src.getSortedMap(), dest.sortedMap());
        assertNotSame(src.getNavigableMap(), dest.navigableMap());
        assertNotSame(src.getArray(), dest.array());

        // Ensure fields are copied correctly
        assertNotNull(dest.object());
        assertEquals(src.getObject().getValue1(), dest.object().getValue1());
        assertEquals(src.getObject().getValue2(), dest.object().getValue2());
        assertEquals(src.getList(), dest.list());
        assertEquals(src.getSet(), dest.set());
        assertEquals(src.getSortedSet(), dest.sortedSet());
        assertEquals(src.getMap(), dest.map());
        assertEquals(src.getConcurrentMap(), dest.concurrentMap());
        assertEquals(src.getSortedMap(), dest.sortedMap());
        assertEquals(src.getNavigableMap(), dest.navigableMap());
        assertArrayEquals(src.getArray(), dest.array());
    }

    private CopyTestBean createCopyTestBean() {
        CopyTestBean src = new CopyTestBean();
        src.setObject(new CloneableObject("Test1", "Test2"));
        src.setList(List.of("A", "B", "C"));
        src.setSet(Set.of(1, 2, 3));
        src.setSortedSet(new TreeSet<>(Set.of(3, 2, 1)));
        src.setMap(Map.of("key1", "value1", "key2", "value2"));
        src.setConcurrentMap(new ConcurrentHashMap<>(Map.of("ckey1", "cvalue1")));
        src.setSortedMap(new TreeMap<>(Map.of("skey1", "svalue1")));
        src.setNavigableMap(new TreeMap<>(Map.of("nkey1", "nvalue1")));
        src.setArray(new int[]{1, 2, 3, 4, 5});
        return src;
    }

    @Test
    void testCopyToRecordNulls() {
        CopyTestBean src = new CopyTestBean();

        CopyTestDtoRecord dest = new CopyToRecordTestMapper().map(src);

        assertNotNull(dest);
        assertNull(dest.object());
        assertNull(dest.list());
        assertNull(dest.set());
        assertNull(dest.map());
        assertNull(dest.array());
    }

    @Test
    void testCopyRawCollections() {
        RawCollectionsBean src = new RawCollectionsBean();
        src.setList(List.of("A", "B", "C"));
        src.setSet(Set.of(1, 2, 3));
        src.setMap(Map.of("key1", "value1", "key2", "value2"));

        RawCollectionsBean dest = new CopyRawCollectionsMapper().map(src);

        assertNotNull(dest);

        // Ensure clone (different instances)
        assertNotSame(src.getList(), dest.getList());
        assertNotSame(src.getSet(), dest.getSet());
        assertNotSame(src.getMap(), dest.getMap());

        // Ensure fields are copied correctly
        assertEquals(src.getList(), dest.getList());
        assertEquals(src.getSet(), dest.getSet());
        assertEquals(src.getMap(), dest.getMap());
    }

    @Test
    void testCloneSpecificCollections() {
        SpecificCollectionsBean src = new SpecificCollectionsBean();
        src.setArrayList(new ArrayList<>(List.of("A", "B", "C")));
        src.setLinkedList(new LinkedList<>(List.of("D", "E", "F")));
        src.setVector(new Vector<>(List.of("G", "H", "I")));
        src.setCopyOnWriteArrayList(new CopyOnWriteArrayList<>(List.of("J", "K", "L")));
        src.setHashSet(new HashSet<>(Set.of(1, 2, 3)));
        src.setLinkedHashSet(new LinkedHashSet<>(Set.of(4, 5, 6)));
        src.setTreeSet(new TreeSet<>(Set.of(7, 8, 9)));
        src.setEnumSet(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        src.setConcurrentSkipListSet(new ConcurrentSkipListSet<>(Set.of(10, 11, 12)));
        src.setHashMap(new HashMap<>(Map.of("key1", "value1")));
        src.setLinkedHashMap(new LinkedHashMap<>(Map.of("key2", "value2")));
        src.setTreeMap(new TreeMap<>(Map.of("key3", "value3")));
        src.setIdentityHashMap(new IdentityHashMap<>(Map.of("key4", "value4")));
        src.setEnumMap(new EnumMap<>(DayOfWeek.class) {{
            put(DayOfWeek.FRIDAY, "value5");
        }});
        src.setWeakHashMap(new WeakHashMap<>(Map.of("key6", "value6")));
        src.setConcurrentHashMap(new ConcurrentHashMap<>(Map.of("key7", "value7")));
        src.setConcurrentSkipListMap(new ConcurrentSkipListMap<>(Map.of("key8", "value8")));

        SpecificCollectionsBean dest = new SpecificCollectionsBeanMapper().map(src);

        assertNotNull(dest);

        // Ensure clone (different instances)
        assertNotSame(src.getArrayList(), dest.getArrayList());
        assertNotSame(src.getLinkedList(), dest.getLinkedList());
        assertNotSame(src.getVector(), dest.getVector());
        assertNotSame(src.getCopyOnWriteArrayList(), dest.getCopyOnWriteArrayList());
        assertNotSame(src.getHashSet(), dest.getHashSet());
        assertNotSame(src.getLinkedHashSet(), dest.getLinkedHashSet());
        assertNotSame(src.getTreeSet(), dest.getTreeSet());
        assertNotSame(src.getEnumSet(), dest.getEnumSet());
        assertNotSame(src.getConcurrentSkipListSet(), dest.getConcurrentSkipListSet());
        assertNotSame(src.getHashMap(), dest.getHashMap());
        assertNotSame(src.getLinkedHashMap(), dest.getLinkedHashMap());
        assertNotSame(src.getTreeMap(), dest.getTreeMap());
        assertNotSame(src.getIdentityHashMap(), dest.getIdentityHashMap());
        assertNotSame(src.getEnumMap(), dest.getEnumMap());
        assertNotSame(src.getWeakHashMap(), dest.getWeakHashMap());
        assertNotSame(src.getConcurrentHashMap(), dest.getConcurrentHashMap());
        assertNotSame(src.getConcurrentSkipListMap(), dest.getConcurrentSkipListMap());

        // Ensure fields are copied correctly
        assertEquals(src.getArrayList(), dest.getArrayList());
        assertEquals(src.getLinkedList(), dest.getLinkedList());
        assertEquals(src.getVector(), dest.getVector());
        assertEquals(src.getCopyOnWriteArrayList(), dest.getCopyOnWriteArrayList());
        assertEquals(src.getHashSet(), dest.getHashSet());
        assertEquals(src.getLinkedHashSet(), dest.getLinkedHashSet());
        assertEquals(src.getTreeSet(), dest.getTreeSet());
        assertEquals(src.getEnumSet(), dest.getEnumSet());
        assertEquals(src.getConcurrentSkipListSet(), dest.getConcurrentSkipListSet());
        assertEquals(src.getHashMap(), dest.getHashMap());
        assertEquals(src.getLinkedHashMap(), dest.getLinkedHashMap());
        assertEquals(src.getTreeMap(), dest.getTreeMap());
        assertEquals(src.getIdentityHashMap(), dest.getIdentityHashMap());
        assertEquals(src.getEnumMap(), dest.getEnumMap());
        assertEquals(src.getWeakHashMap(), dest.getWeakHashMap());
        assertEquals(src.getConcurrentHashMap(), dest.getConcurrentHashMap());
        assertEquals(src.getConcurrentSkipListMap(), dest.getConcurrentSkipListMap());
    }
}