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
package com.detornium.graft.annotations.processors.scanners;

import com.detornium.graft.annotations.processors.models.tree.*;

import java.util.List;

public interface NodeVisitor<R> {
    R visit(GetterNode node, R in);

    R visit(SelfNode node);

    R visit(ConstantValueNode node);

    R visit(ConverterNode node, R in);

    R visit(CopyValueNode node, R in);

    R visit(SetterNode node, R in);

    R visit(ConstructNode node, R in);

    R visit(ConstructRecordNode node, R in);

    R reduce(List<R> results);
}
