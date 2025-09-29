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
package com.detornium.graft.annotations.processors.utils;

import javax.lang.model.element.*;
import java.util.Collection;
import java.util.function.Predicate;

public class Predicates {

    public static Predicate<Element> isPublic() {
        return Helpers::isPublic;
    }

    public static Predicate<Element> isProtected() {
        return Helpers::isProtected;
    }

    public static Predicate<ExecutableElement> hasNoArgs() {
        return Helpers::hasNoArgs;
    }

    public static Predicate<ExecutableElement> isConstructor() {
        return Helpers::isConstructor;
    }

    public static Predicate<ExecutableElement> isDefaultMethod() {
        return ExecutableElement::isDefault;
    }

    public static Predicate<ExecutableElement> isStatic() {
        return (el) -> el.getModifiers().contains(Modifier.STATIC);
    }

    public static Predicate<ExecutableElement> isPrivate() {
        return (el) -> el.getModifiers().contains(Modifier.PRIVATE);
    }

    public static Predicate<ExecutableElement> isObjectMethod() {
        return (el) -> {
            Collection<? extends VariableElement> params = el.getParameters();
            Name name = el.getSimpleName();
            return (name.contentEquals("toString") && params.isEmpty())
                    || (name.contentEquals("hashCode") && params.isEmpty())
                    || (name.contentEquals("equals") && params.size() == 1)
                    || (name.contentEquals("getClass") && params.isEmpty())
                    || (name.contentEquals("notify") && params.isEmpty())
                    || (name.contentEquals("notifyAll") && params.isEmpty())
                    || (name.contentEquals("wait") && (params.isEmpty() || params.size() == 1 || params.size() == 2))
                    || (name.contentEquals("finalize") && params.isEmpty());
        };
    }

}
