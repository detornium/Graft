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
package com.detornium.graft.lombok;

import com.detornium.graft.annotations.processors.spi.ClassReadyCheck;

import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Field;

public class LombokClassReadyCheck implements ClassReadyCheck {

    private static boolean lombokInClassPath;
    private static Field lombokInvokedField;

    static {
        try {
            Class<?> notifierData = Class.forName("lombok.launch.AnnotationProcessorHider$AstModificationNotifierData");
            lombokInClassPath = true;
            lombokInvokedField = notifierData.getField("lombokInvoked");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            lombokInClassPath = false;
        }
    }

    @Override
    public boolean isClassReady(TypeMirror type) {
        return lombokInClassPath && isLombokProcessingDone();
    }

    private boolean isLombokProcessingDone() {
        try {
            return lombokInvokedField.getBoolean(null);
        } catch (Exception e) {
            return true;
        }
    }
}
