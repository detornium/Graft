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

import com.detornium.graft.models.NestedPropertiesBean;
import com.detornium.graft.models.SubBean1;
import com.detornium.graft.models.SubBean2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NestedPropertiesBeanMapperTest {

    @Test
    void testMapNestedProperties() {
        SubBean2 subBean2 = new SubBean2();
        subBean2.setProp2("value2");

        SubBean1 subBean1 = new SubBean1();
        subBean1.setSubBean2(subBean2);

        NestedPropertiesBean src = new NestedPropertiesBean();
        src.setSubBean1(subBean1);

        NestedPropertiesBeanMapper mapper = new NestedPropertiesBeanMapper();
        NestedPropertiesBean dst = mapper.map(src);

        assertNotNull(dst);
        assertNotNull(dst.getSubBean1());
        assertNotNull(dst.getSubBean1().getSubBean2());
        assertEquals("value2", dst.getBeanProp());
        assertEquals("value2", dst.getSubBean1().getSubBean2().getProp2());
    }

    @Test
    void testMapNullSource() {
        NestedPropertiesBeanMapper mapper = new NestedPropertiesBeanMapper();
        assertNull(mapper.map(null));
    }

    @Test
    void testMapWithNullNestedBeans() {
        NestedPropertiesBean src = new NestedPropertiesBean();
        src.setSubBean1(null);

        NestedPropertiesBeanMapper mapper = new NestedPropertiesBeanMapper();
        NestedPropertiesBean dst = mapper.map(src);

        assertNotNull(dst);
        assertNull(dst.getBeanProp());

        assertNotNull(dst.getSubBean1());
        assertNotNull(dst.getSubBean1().getSubBean2());
        assertNull(dst.getSubBean1().getSubBean2().getProp2());
    }
}
