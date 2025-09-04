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

package com.detornium.graft.models;

import lombok.Data;

import java.util.Collection;

@Data
public class CarDto {
    private String color;
    private String carModel;
    private String version;

    private String owner;

    private String description;
    private String notes;

    private Collection<String> previousOwners;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Collection<String> getPreviousOwners() {
        return previousOwners;
    }

    public void setPreviousOwners(Collection<String> previousOwners) {
        this.previousOwners = previousOwners;
    }
}
