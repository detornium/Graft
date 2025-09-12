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
package com.detornium.graft.annotations.processors.phases;

import com.detornium.graft.annotations.processors.ClassNotReadyException;
import com.detornium.graft.annotations.processors.ProcessingException;
import com.detornium.graft.annotations.processors.models.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: refactor
public class DependencyTreeBuildingPhase extends AbstractProcessingPhase {
    private static final String NAME = "Dependency Tree Building Phase";

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException, ClassNotReadyException {
        SetterNode root = new SetterNode();
        Map<String, GetterNode> existingGetters = new HashMap<>();

        context.getMappings().forEach(mapping -> expandMapping(root, mapping, existingGetters));
        context.setDependencyTreeRoot(root);
    }

    private void expandMapping(SetterNode parent, Mapping mapping, Map<String, GetterNode> existingGetters) {
        for (Accessor accessor : mapping.getSetters()) {
            SetterNode child = getChildByAccessorName(parent, accessor.getName());
            if (child == null) {
                SetterNode node = new SetterNode();
                node.setAccessor(accessor);
                parent.addChild(node);
                parent = node;
            } else {
                parent = child;
            }
        }

        GetterNode getter = processGetters(mapping.getGetters(), existingGetters);
        parent.setValueSource(getter);
    }

    private GetterNode processGetters(List<Accessor> getters, Map<String, GetterNode> existingGetters) {
        GetterNode root = null;

        GetterNode result = null;
        for (int idx = getters.size() - 1; idx >= 0; idx--) {
            String path = getGetterPath(getters.subList(0, idx + 1));
            if (existingGetters.containsKey(path)) {
                GetterNode current = existingGetters.get(path);
                if (result == null) {
                    result = current;
                }
                if (root != null) {
                    current.addChild(root);
                }
                break;
            } else {
                GetterNode current = new GetterNode();
                if (result == null) {
                    result = current;
                }
                if (root != null) {
                    current.addChild(root);
                }

                current.setAccessor(getters.get(idx));
                root = current;
                existingGetters.put(path, current);
            }
        }

        return result;
    }

    private String getGetterPath(List<Accessor> getters) {
        return getters.stream()
                .map(Accessor::getName)
                .collect(Collectors.joining("."));
    }

    private SetterNode getChildByAccessorName(SetterNode parent, String accessorName) {
        return parent.getChildren().stream()
                .filter(child -> child.getAccessor().getName().equals(accessorName))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
