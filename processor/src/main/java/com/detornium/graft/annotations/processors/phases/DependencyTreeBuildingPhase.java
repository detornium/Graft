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
import com.detornium.graft.annotations.processors.models.Accessor;
import com.detornium.graft.annotations.processors.models.Mapping;
import com.detornium.graft.annotations.processors.models.MappingContext;
import com.detornium.graft.annotations.processors.models.tree.*;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.detornium.graft.annotations.processors.Constants.TARGET_VAR_NAME;
import static com.detornium.graft.annotations.processors.utils.Helpers.declaredTypeMirrorToTypeElement;
import static com.detornium.graft.annotations.processors.utils.Helpers.isRecord;
import static java.util.function.Predicate.not;

// TODO: refactor
public class DependencyTreeBuildingPhase extends AbstractProcessingPhase {
    private static final String NAME = "Dependency Tree Building Phase";

    @Override
    protected void doProcess(MappingContext context) throws ProcessingException, ClassNotReadyException {
        TypeMirror sourceType = context.getSourceType().asType();
        TypeMirror targetType = context.getTargetType().asType();

        CompositeNode root = createConstructNode(TARGET_VAR_NAME, targetType);

        Map<String, GetterNode> existingGetters = new HashMap<>();

        context.getMappings().stream()
                .filter(not(Mapping::isExclude))
                .forEach(mapping -> expandMapping(root, sourceType, mapping, existingGetters));

        context.setDependencyTreeRoot(root);
    }

    private CompositeNode createConstructNode(String name, TypeMirror type) {
        TypeElement typeElement = declaredTypeMirrorToTypeElement(type).orElseThrow(
                () -> new IllegalStateException("Type element not found for type: " + type));

        return isRecord(typeElement)
                ? new ConstructRecordNode(name, type)
                : new ConstructNode(name, type);
    }

    private void expandMapping(CompositeNode root, TypeMirror sourceType, Mapping mapping, Map<String, GetterNode> existingGetters) {
        AtomicReference<CompositeNode> parentRef = new AtomicReference<>(root);

        List<Accessor> setters = mapping.getSetters();
        for (int idx = 0; idx < setters.size(); idx++) {
            Accessor accessor = setters.get(idx);
            boolean isLast = idx == setters.size() - 1;

            if (isLast) {
                createSetterNode(parentRef.get(), sourceType, mapping, existingGetters);
            } else {
                CompositeNode node = findConstructNode(parentRef.get(), accessor.getName())
                        .orElseGet(() -> createTransientSetterNode(parentRef.get(), accessor));
                parentRef.set(node);
            }
        }
    }

    private CompositeNode createTransientSetterNode(CompositeNode parent, Accessor accessor) {
        // TODO: name collision check
        CompositeNode node = createConstructNode(accessor.getName(), accessor.getValueType());

        // Setter node always creates a new instance of the object
        SetterNode setterNode = new SetterNode();
        setterNode.setAccessor(accessor);
        setterNode.setParent(parent);
        setterNode.setChild(node);

        parent.addChild(setterNode);

        return node;
    }

    private void createSetterNode(CompositeNode parent, TypeMirror srcType, Mapping mapping, Map<String, GetterNode> existingGetters) {
        Accessor lastSetter = mapping.getLastSetter();

        // Can be constant, chain of getters or self(src)
        Node childNode;

        if (mapping.getConstant() != null) {
            childNode = new ConstantValueNode(mapping.getConstant());
        } else if (mapping.getGetters().isEmpty()) {
            childNode = new SelfNode();
        } else {
            GetterNode getterNode = processGetters(mapping.getGetters(), existingGetters);
            getterNode.incrementUsage();
            srcType = getterNode.getAccessor().getValueType();
            childNode = getterNode;
        }

        // copy(), converter() and constant() are mutually exclusive
        if (mapping.isCopy()) {
            // can be used for getter chain only
            CopyValueNode copyValueNode = new CopyValueNode();
            copyValueNode.setChild(childNode);
            copyValueNode.setSourceType(srcType);
            childNode = copyValueNode;
        } else if (mapping.getConverter() != null) {
            // can be used for self(src) or getters chain
            ConverterNode converterNode = new ConverterNode();
            converterNode.setConverterRef(mapping.getConverter());
            converterNode.setSourceType(srcType);
            converterNode.setTargetType(lastSetter.getValueType());
            converterNode.setChild(childNode);
            childNode = converterNode;
        }

        SetterNode node = new SetterNode();
        node.setAccessor(lastSetter);
        node.setParent(parent);
        node.setChild(childNode);

        parent.addChild(node);
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

    private Optional<CompositeNode> findConstructNode(Node parent, String accessorName) {
        return parent.getChildren().stream()
                .filter(isSetterNodeWithAccessor(accessorName))
                .map(SetterNode.class::cast)
                .map(SetterNode::getChild)
                .filter(CompositeNode.class::isInstance)
                .map(CompositeNode.class::cast)
                .findFirst();
    }

    private Predicate<Node> isSetterNodeWithAccessor(String accessorName) {
        return node -> node instanceof SetterNode
                && ((SetterNode) node).getAccessor().getName().equals(accessorName);
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
