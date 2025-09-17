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

import com.detornium.graft.annotations.processors.models.tree.GetterNode;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import static com.detornium.graft.annotations.processors.Constants.*;

@Getter
@Setter
public class GenerationContext {
    private Map<GetterNode, CodeBlock> resolvedGetters = new HashMap<>();

    private Set<String> varNames = new HashSet<>();

    private List<FieldSpec> fields = new ArrayList<>();
    private List<MethodSpec> methods = new ArrayList<>();
    private List<CodeBlock> statements = new ArrayList<>();

    public GenerationContext() {
        // Initialize varNames with reserved names
        varNames.add(SOURCE_VAR_NAME);
        varNames.add(TARGET_VAR_NAME);
    }

    public String generateVarNameFromAccessor(String prefix, Accessor accessor) {
        String baseName = accessor.getName();

        if (prefix != null && !prefix.isEmpty()) {
            baseName = prefix + capitalize(baseName);
        }

        if (baseName == null || baseName.isEmpty()) {
            baseName = DEFAULT_VAR_NAME;
        }

        return generateVarName(baseName);
    }

    private String capitalize(String str) {
        return (str == null || str.isEmpty())
                ? str
                : str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String generateVarName(String name) {
        String uniqueName = name;
        for (int counter = 0; !varNames.add(uniqueName); counter++) {
            uniqueName = name + (counter == 0 ? "" : counter);
        }

        return uniqueName;
    }
}
