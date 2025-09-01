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
import com.detornium.graft.models.CopyTestBean;
import com.detornium.graft.models.CopyTestDtoRecord;

@MappingSpec(com.detornium.graft.mappers.CopyToRecordTestMapper.class)
public class CopyToRecordSpec extends MappingDsl<CopyTestBean, CopyTestDtoRecord> {
    {
        map(CopyTestBean::getObject).copy().to(CopyTestDtoRecord::object);
        map(CopyTestBean::getList).copy().to(CopyTestDtoRecord::list);
        map(CopyTestBean::getSet).copy().to(CopyTestDtoRecord::set);
        map(CopyTestBean::getSortedSet).copy().to(CopyTestDtoRecord::sortedSet);
        map(CopyTestBean::getMap).copy().to(CopyTestDtoRecord::map);
        map(CopyTestBean::getConcurrentMap).copy().to(CopyTestDtoRecord::concurrentMap);
        map(CopyTestBean::getSortedMap).copy().to(CopyTestDtoRecord::sortedMap);
        map(CopyTestBean::getNavigableMap).copy().to(CopyTestDtoRecord::navigableMap);
        map(CopyTestBean::getArray).copy().to(CopyTestDtoRecord::array);
    }
}
