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
import com.detornium.graft.models.Car;
import com.detornium.graft.models.CarDtoRecord;

@MappingSpec(com.detornium.graft.mappers.CarToCarDtoRecordMapper.class)
public class CarToCarDtoRecordMapperSpec extends MappingDsl<Car, CarDtoRecord> {
    {
        map(Car::getModel).to(CarDtoRecord::carModel);
        map(Car::getVersion).converting(String::valueOf).to(CarDtoRecord::version);
        exclude(CarDtoRecord::owner);
        self().converting(CarToCarDtoRecordMapperSpec::carToDescription).to(CarDtoRecord::description);
        value("N/A").to(CarDtoRecord::notes);
    }

    public static String carToDescription(Car car) {
        return "%s %s".formatted(car.getColor(), car.getModel());
    }
}
