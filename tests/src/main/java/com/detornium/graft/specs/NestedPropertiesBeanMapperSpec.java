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
import com.detornium.graft.models.NestedPropertiesBean;
import com.detornium.graft.models.SubBean1;
import com.detornium.graft.models.SubBean2;

@MappingSpec(com.detornium.graft.mappers.NestedPropertiesBeanMapper.class)
public class NestedPropertiesBeanMapperSpec extends MappingDsl<NestedPropertiesBean, NestedPropertiesBean> {
    {
        map(NestedPropertiesBean::getSubBean1)
                .nested(SubBean1::getSubBean2)
                .nested(SubBean2::getProp2)
                .to(NestedPropertiesBean::setBeanProp);

//        map(NestedPropertiesBean::getSubBean1)
//                .nested(SubBean1::getSubBean2)
//                .nested(SubBean2::getProp2)
//                .to(bean(NestedPropertiesBean::setSubBean1)
//                        .nested(SubBean1::setSubBean2)
//                        .nested(SubBean2::setProp2));
    }
}
