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
import com.detornium.graft.models.CarDto;
import com.detornium.graft.models.CarDtoRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IgnoreUnmappedTest {

    @Test
    void test() {
        Mapper<Car, CarDto> mapper = new IgnoreUnmappedMapper();

        Car car = new Car();
        car.setColor("Red");
        car.setModel("Camry");
        CarDto carDto = mapper.map(car);

        assertNotNull(carDto);

        assertEquals(car.getModel(), carDto.getCarModel()); // manually mapped
        assertEquals(car.getColor(), carDto.getColor()); // mapped automatically
    }

    @Test
    void testRecord() {
        Mapper<Car, CarDtoRecord> mapper = new IgnoreUnmappedRecordMapper();

        Car car = new Car();
        car.setColor("Red");
        car.setModel("Camry");
        CarDtoRecord carDto = mapper.map(car);

        assertNotNull(carDto);

        assertEquals(car.getModel(), carDto.carModel()); // manually mapped
        assertEquals(car.getColor(), carDto.color()); // mapped automatically
    }
}
