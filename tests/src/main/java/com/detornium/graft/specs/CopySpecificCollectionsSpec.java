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
package com.detornium.graft.specs;

import com.detornium.graft.MappingDsl;
import com.detornium.graft.annotations.MappingSpec;
import com.detornium.graft.models.SpecificCollectionsBean;

@MappingSpec(com.detornium.graft.mappers.SpecificCollectionsBeanMapper.class)
public class CopySpecificCollectionsSpec extends MappingDsl<SpecificCollectionsBean, SpecificCollectionsBean> {
    {
        map(SpecificCollectionsBean::getArrayList).copy().to(SpecificCollectionsBean::setArrayList);
        map(SpecificCollectionsBean::getLinkedList).copy().to(SpecificCollectionsBean::setLinkedList);
        map(SpecificCollectionsBean::getVector).copy().to(SpecificCollectionsBean::setVector);
        map(SpecificCollectionsBean::getCopyOnWriteArrayList).copy().to(SpecificCollectionsBean::setCopyOnWriteArrayList);

        map(SpecificCollectionsBean::getHashSet).copy().to(SpecificCollectionsBean::setHashSet);
        map(SpecificCollectionsBean::getLinkedHashSet).copy().to(SpecificCollectionsBean::setLinkedHashSet);
        map(SpecificCollectionsBean::getTreeSet).copy().to(SpecificCollectionsBean::setTreeSet);
        map(SpecificCollectionsBean::getEnumSet).copy().to(SpecificCollectionsBean::setEnumSet);
        map(SpecificCollectionsBean::getConcurrentSkipListSet).copy().to(SpecificCollectionsBean::setConcurrentSkipListSet);

        map(SpecificCollectionsBean::getHashMap).copy().to(SpecificCollectionsBean::setHashMap);
        map(SpecificCollectionsBean::getLinkedHashMap).copy().to(SpecificCollectionsBean::setLinkedHashMap);
        map(SpecificCollectionsBean::getTreeMap).copy().to(SpecificCollectionsBean::setTreeMap);
        map(SpecificCollectionsBean::getIdentityHashMap).copy().to(SpecificCollectionsBean::setIdentityHashMap);
        map(SpecificCollectionsBean::getEnumMap).copy().to(SpecificCollectionsBean::setEnumMap);
        map(SpecificCollectionsBean::getWeakHashMap).copy().to(SpecificCollectionsBean::setWeakHashMap);
        map(SpecificCollectionsBean::getConcurrentHashMap).copy().to(SpecificCollectionsBean::setConcurrentHashMap);
        map(SpecificCollectionsBean::getConcurrentSkipListMap).copy().to(SpecificCollectionsBean::setConcurrentSkipListMap);
    }
}