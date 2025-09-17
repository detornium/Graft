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
import lombok.Getter;
import lombok.Setter;

import javax.lang.model.type.TypeMirror;

@Getter
@Setter
public class CopyValueNode extends UnaryNode {

    private TypeMirror sourceType;

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        R in = child != null ? child.accept(visitor) : null;
        return visitor.visit(this, in);
    }
}
