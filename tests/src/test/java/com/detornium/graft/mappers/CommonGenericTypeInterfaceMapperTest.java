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

import com.detornium.graft.models.SimpleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonGenericTypeInterfaceMapperTest {

    @Test
    void testConvertSimpleModelToDto() {
        SimpleModel model = new SimpleModel();
        model.setStringField("testValue");

        CommonGenericTypeInterfaceMapper mapper = new CommonGenericTypeInterfaceMapper();
        SimpleModel copy = mapper.copy(model);

        assertNotNull(copy);
        assertEquals("testValue", copy.getStringField());
    }

    @Test
    void testConvertNullModel() {
        CommonGenericTypeInterfaceMapper mapper = new CommonGenericTypeInterfaceMapper();
        assertNull(mapper.copy(null));
    }
}