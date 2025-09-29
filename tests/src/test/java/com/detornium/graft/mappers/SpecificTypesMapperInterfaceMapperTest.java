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

import com.detornium.graft.models.SimpleDto;
import com.detornium.graft.models.SimpleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecificTypesMapperInterfaceMapperTest {

    @Test
    void testConvertSimpleModelToDto() {
        SimpleModel model = new SimpleModel();
        model.setStringField("testValue");

        SpecificTypesMapperInterfaceMapper mapper = new SpecificTypesMapperInterfaceMapper();
        SimpleDto dto = mapper.convert(model);

        assertNotNull(dto);
        assertEquals("testValue", dto.getStringField());
    }

    @Test
    void testConvertNullModel() {
        SpecificTypesMapperInterfaceMapper mapper = new SpecificTypesMapperInterfaceMapper();
        assertNull(mapper.convert(null));
    }
}