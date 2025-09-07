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

import com.sun.source.tree.ExpressionTree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodInvocation {
    private String methodName;
    private Arguments arguments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Arguments {
        private List<? extends ExpressionTree> arguments;

        public ExpressionTree argument(int idx) {
            return arguments.get(idx);
        }

        public int argumentsCount() {
            return arguments.size();
        }

        public boolean isEmpty() {
            return arguments.isEmpty();
        }
    }
}
