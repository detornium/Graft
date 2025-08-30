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

import com.detornium.graft.models.Car;
import com.detornium.graft.models.CarDtoRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarToCarDtoRecordMapperTest {

    @Test
    void testMapCarToCarDtoRecord() {
        Car car = new Car();
        car.setModel("Tesla");
        car.setVersion(3);
        car.setPrevOwners(List.of("Alice", "Bob"));
        car.setColor("Red");

        CarToCarDtoRecordMapper mapper = new CarToCarDtoRecordMapper();
        CarDtoRecord dto = mapper.map(car);

        assertNotNull(dto);
        assertEquals("Red", dto.color());
        assertEquals("Tesla", dto.carModel());
        assertEquals("3", dto.version()); // conversion
        assertNull(dto.owner()); // excluded
        assertEquals("Red Tesla", dto.description()); // custom converter
        assertEquals("N/A", dto.notes()); // constant value
    }

    @Test
    void testMapNullCar() {
        CarToCarDtoRecordMapper mapper = new CarToCarDtoRecordMapper();
        assertNull(mapper.map(null));
    }
}