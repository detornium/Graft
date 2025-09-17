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
package com.detornium.graft.annotations.processors.models.tree;

import com.detornium.graft.annotations.processors.scanners.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeNode extends Node {
    protected final List<Node> children = new ArrayList<>();

    public void addChild(Node child) {
        child.setParent(this);
        children.add(child);
    }

    @Override
    public List<Node> getChildren() {
        return List.copyOf(children);
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        List<R> childResults = new ArrayList<>(children.size());
        for (Node child : children) {
            childResults.add(child.accept(visitor));
        }

        R visitorResult = visitor.reduce(childResults);

        return doVisit(visitor, visitorResult);
    }

    protected abstract <R> R doVisit(NodeVisitor<R> visitor, R childResults);
}
