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

import com.detornium.graft.Mapper;
import com.detornium.graft.models.Car;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DisableAutoMappingTest {

    @Test
    void test() {
        Mapper<Car, Car> mapper = new DisableAutoMappingMapper();

        Car source = new Car();
        source.setModel("Camry");
        source.setColor("Blue");
        source.setVersion(1);
        source.setPrevOwners(List.of("John", "Alice"));

        Car target = mapper.map(source);

        assertNotNull(target);
        assertEquals(source.getVersion(), target.getVersion());
        // Other fields should remain null or default since auto-mapping is disabled
        assertNull(target.getModel());
        assertNull(target.getColor());
        assertNull(target.getPrevOwners());
    }
}
