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
import com.detornium.graft.models.RawCollectionsBean;

@MappingSpec(com.detornium.graft.mappers.CopyRawCollectionsMapper.class)
public class CopyRawCollectionsSpec extends MappingDsl<RawCollectionsBean, RawCollectionsBean> {
    {
        map(RawCollectionsBean::getList).copy().to(RawCollectionsBean::setList);
        map(RawCollectionsBean::getSet).copy().to(RawCollectionsBean::setSet);
        map(RawCollectionsBean::getMap).copy().to(RawCollectionsBean::setMap);
    }
}
