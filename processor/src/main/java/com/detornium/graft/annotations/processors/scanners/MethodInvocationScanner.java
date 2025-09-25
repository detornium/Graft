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

import com.detornium.graft.annotations.processors.models.MethodInvocation;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;

import java.util.Deque;
import java.util.List;

public class MethodInvocationScanner extends TreeScanner<Deque<MethodInvocation>, Deque<MethodInvocation>> {

    @Override
    public Deque<MethodInvocation> visitMethodInvocation(MethodInvocationTree node, Deque<MethodInvocation> methodInvocations) {
        String methodName = node.getMethodSelect() instanceof IdentifierTree
                ? ((IdentifierTree) node.getMethodSelect()).getName().toString()
                : ((MemberSelectTree) node.getMethodSelect()).getIdentifier().toString();

        List<? extends ExpressionTree> arguments = node.getArguments();

        MethodInvocation.Arguments invocationArguments = new MethodInvocation.Arguments(arguments);
        MethodInvocation invocation = new MethodInvocation(methodName, invocationArguments);

        methodInvocations.addFirst(invocation);

        scan(node.getTypeArguments(), methodInvocations);
        scan(node.getMethodSelect(), methodInvocations);

        return methodInvocations;
    }
}
