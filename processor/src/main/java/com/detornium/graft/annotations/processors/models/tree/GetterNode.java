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

import com.detornium.graft.annotations.processors.models.Accessor;
import com.detornium.graft.annotations.processors.scanners.NodeVisitor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetterNode extends CompositeNode {
    private Accessor accessor;

    private int usageCount = 0;

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        R in = parent != null ? parent.accept(visitor) : null;
        return visitor.visit(this, in);
    }

    @Override
    protected <R> R doVisit(NodeVisitor<R> visitor, R childResults) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int incrementUsage() {
        return ++usageCount;
    }

    public int decrementUsage() {
        if (usageCount == 0) {
            throw new IllegalStateException("Usage count cannot be less than zero");
        }

        return --usageCount;
    }

    public int getUsage() {
        return usageCount;
    }

    public int getTransientUsage() {
        return usageCount + getChildren().stream()
                .filter(GetterNode.class::isInstance)
                .map(GetterNode.class::cast)
                .mapToInt(GetterNode::getTransientUsage)
                .sum();
    }

}
