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

package com.detornium.graft.annotations.processors.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Mapping {
    private ConstantValue constant; // expr for value(...)
    private List<Accessor> getters = new ArrayList<>();
    private Accessor setter;    // e.g. setNumberOfSeats
    private MemberRefInfo converter; // raw expr for converting(...) or null
    private boolean exclude;  // when exclude(setter)
    private boolean copy;     // when copy()

    public Accessor getFirstGetter() {
        return getters != null && !getters.isEmpty() ? getters.get(0) : null;
    }

    public Accessor getLastGetter() {
        return getters != null && !getters.isEmpty() ? getters.get(getters.size() - 1) : null;
    }

    public void addGetter(Accessor getter) {
        if (this.getters == null) {
            this.getters = new ArrayList<>();
        }

        this.getters.add(getter);
    }
}
