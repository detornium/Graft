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

package com.detornium.graft.annotations.processors;

import com.sun.source.tree.Tree;
import lombok.Getter;

import javax.lang.model.element.Element;

public class ProcessingException extends Exception {
    @Getter
    private final Element element;
    @Getter
    private final Tree tree;

    public ProcessingException(Element element, String message) {
        super(message);
        this.element = element;
        this.tree = null;
    }

    public ProcessingException(Tree tree, String message) {
        super(message);
        this.element = null;
        this.tree = tree;
    }
}
