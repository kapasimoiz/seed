/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.web.internal.diagnostic;

import com.google.common.collect.Lists;
import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import org.seedstack.seed.core.internal.AbstractSeedPlugin;
import org.seedstack.seed.web.WebConfig;
import org.seedstack.seed.web.spi.FilterDefinition;
import org.seedstack.seed.web.spi.ListenerDefinition;
import org.seedstack.seed.web.spi.ServletDefinition;
import org.seedstack.seed.web.spi.WebProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.List;

import static org.seedstack.seed.web.internal.WebPlugin.WEB_PLUGIN_PREFIX;

public class WebDiagnosticPlugin extends AbstractSeedPlugin implements WebProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebDiagnosticPlugin.class);

    private WebConfig webConfig;

    @Override
    public String name() {
        return "web-diagnostic";
    }

    @Override
    public InitState initialize(InitContext initContext) {
        webConfig = getConfiguration(WebConfig.class);

        ServletContext servletContext = getSeedRuntime().contextAs(ServletContext.class);
        if (servletContext != null) {
            getSeedRuntime().getDiagnosticManager().registerDiagnosticInfoCollector(
                    WEB_PLUGIN_PREFIX,
                    new WebDiagnosticCollector(servletContext)
            );
        }

        return InitState.INITIALIZED;
    }

    @Override
    public Object nativeUnitModule() {
        if (webConfig.isRequestDiagnosticEnabled()) {
            return new WebDiagnosticModule();
        } else {
            return null;
        }
    }

    @Override
    public List<ServletDefinition> servlets() {
        return null;
    }

    @Override
    public List<FilterDefinition> filters() {
        if (webConfig.isRequestDiagnosticEnabled()) {
            LOGGER.info("Per-request diagnostic enabled, a diagnostic file will be dumped for each request exception");

            FilterDefinition filterDefinition = new FilterDefinition("web-diagnostic", WebDiagnosticFilter.class);
            filterDefinition.setPriority(10000);
            filterDefinition.setAsyncSupported(true);
            filterDefinition.addMappings(new FilterDefinition.Mapping("/*"));
            return Lists.newArrayList(filterDefinition);
        } else {
            return null;
        }
    }

    @Override
    public List<ListenerDefinition> listeners() {
        return null;
    }
}
