/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.core.internal.configuration.tool;

import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import io.nuun.kernel.api.plugin.request.ClasspathScanRequest;
import org.seedstack.coffig.Config;
import org.seedstack.seed.CoreConfig;
import org.seedstack.seed.core.internal.AbstractSeedTool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConfigurationTool extends AbstractSeedTool {
    private final Node root = new Node(CoreConfig.class);

    @Override
    public String toolName() {
        return "config";
    }

    @Override
    public Collection<ClasspathScanRequest> classpathScanRequests() {
        return classpathScanRequestBuilder()
                .annotationType(Config.class)
                .build();
    }

    @Override
    protected InitState initialize(InitContext initContext) {
        List<Node> nodes = new ArrayList<>();
        initContext.scannedClassesByAnnotationClass().get(Config.class).stream().map(Node::new).forEach(nodes::add);
        Collections.sort(nodes);
        nodes.forEach(this::buildTree);
        return InitState.INITIALIZED;
    }

    @Override
    public Integer call() throws Exception {
        new TreePrinter(root).print(System.out);
        return 0;
    }

    private void buildTree(Node node) {
        Node current = root;
        String[] path = node.getPath();
        for (String part : path) {
            if (!part.isEmpty()) {
                Node child = current.getChild(part);
                if (child != null) {
                    current = child;
                } else {
                    current.addChild(node);
                    break;
                }
            }
        }
    }
}
