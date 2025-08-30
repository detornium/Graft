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

package com.detornium.graft;

public class MappingDsl<S, D> {

    /**
     * This class is not meant to be instantiated.
     * It serves as a container for methods that can be used in a DSL (Domain-Specific Language) context.
     */
    protected MappingDsl() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    protected <V> MapChain<S, D, V> map(Getter<S, V> getter) {
        return null;
    }

    protected <V> MapChainTo<D, V> value(V value) {
        return null;
    }

    protected MapChain<S, D, S> self() {
        return null;
    }

    protected <V> void exclude(Setter<D, V> setter) {
    }

    protected <V, RD extends Record> void exclude(Getter<RD, V> recordField) {
    }

    public interface MapChain<S, D, V> {
        void to(Setter<D, V> setter);

        <RD extends Record> void to(Getter<RD, V> recordField);

        <R> MapChainTo<D, R> converting(Converter<V, R> conv);
    }

    public interface MapChainTo<D, V> {
        void to(Setter<D, V> setter);

        <RD extends Record> void to(Getter<RD, V> recordField);
    }
}
