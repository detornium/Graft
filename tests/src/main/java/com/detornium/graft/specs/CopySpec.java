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
import com.detornium.graft.models.CopyTestDto;

@MappingSpec(com.detornium.graft.mappers.CopyTestMapper.class)
public class CopySpec extends MappingDsl<CopyTestBean, CopyTestDto> {
    {
        map(CopyTestBean::getObject).copy().to(CopyTestDto::setObject);
        map(CopyTestBean::getList).copy().to(CopyTestDto::setList);
        map(CopyTestBean::getSet).copy().to(CopyTestDto::setSet);
        map(CopyTestBean::getSortedSet).copy().to(CopyTestDto::setSortedSet);
        map(CopyTestBean::getMap).copy().to(CopyTestDto::setMap);
        map(CopyTestBean::getConcurrentMap).copy().to(CopyTestDto::setConcurrentMap);
        map(CopyTestBean::getSortedMap).copy().to(CopyTestDto::setSortedMap);
        map(CopyTestBean::getNavigableMap).copy().to(CopyTestDto::setNavigableMap);
        map(CopyTestBean::getArray).copy().to(CopyTestDto::setArray);
    }
}
