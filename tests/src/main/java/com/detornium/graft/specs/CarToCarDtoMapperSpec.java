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
import com.detornium.graft.models.CarDto;

@MappingSpec(com.detornium.graft.mappers.CarToCarDtoMapper.class)
public class CarToCarDtoMapperSpec extends MappingDsl<Car, CarDto> {
    {
        map(Car::getModel).to(CarDto::setCarModel);
        map(Car::getVersion).converting(String::valueOf).to(CarDto::setVersion);
        exclude(CarDto::setOwner);
        map(Car::getPrevOwners).to(CarDto::setPreviousOwners);
        self().converting(CarToCarDtoMapperSpec::carToDescription).to(CarDto::setDescription);
        value("N/A").to(CarDto::setNotes);
    }

    public static String carToDescription(Car car) {
        return "%s %s".formatted(car.getColor(), car.getModel());
    }
}