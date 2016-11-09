/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.web.internal.security;

import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.apache.shiro.util.StringUtils;
import org.apache.shiro.web.filter.PathMatchingFilter;
import org.seedstack.seed.security.internal.SecurityGuiceConfigurer;
import org.seedstack.seed.web.SecurityFilter;
import org.seedstack.seed.web.WebConfig;
import org.seedstack.seed.web.internal.security.shiro.ShiroWebModule;
import org.seedstack.seed.web.spi.AntiXsrfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WebSecurityModule extends ShiroWebModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiroWebModule.class);
    private static final String PROPERTIES_PREFIX = "org.seedstack.seed.security.urls";
    private static final Map<String, Key<? extends Filter>> DEFAULT_FILTERS = new HashMap<>();

    static {
        // Shiro filters
        DEFAULT_FILTERS.put("anon", ANON);
        DEFAULT_FILTERS.put("authc", AUTHC);
        DEFAULT_FILTERS.put("authcBasic", AUTHC_BASIC);
        DEFAULT_FILTERS.put("logout", LOGOUT);
        DEFAULT_FILTERS.put("noSessionCreation", NO_SESSION_CREATION);
        DEFAULT_FILTERS.put("perms", PERMS);
        DEFAULT_FILTERS.put("port", PORT);
        DEFAULT_FILTERS.put("rest", REST);
        DEFAULT_FILTERS.put("roles", ROLES);
        DEFAULT_FILTERS.put("ssl", SSL);
        DEFAULT_FILTERS.put("user", USER);

        // Seed filters
        DEFAULT_FILTERS.put("xsrf", Key.get(AntiXsrfFilter.class));
        DEFAULT_FILTERS.put("cert", Key.get(X509CertificateFilter.class));
    }

    private final String applicationName;
    private final WebConfig.SecurityConfig securityConfig;
    private final Collection<Class<? extends Filter>> customFilters;
    private final SecurityGuiceConfigurer securityGuiceConfigurer;

    WebSecurityModule(ServletContext servletContext, WebConfig.SecurityConfig securityConfig, Collection<Class<? extends Filter>> customFilters, String applicationName, SecurityGuiceConfigurer securityGuiceConfigurer) {
        super(servletContext);
        this.securityConfig = securityConfig;
        this.customFilters = customFilters;
        this.applicationName = applicationName;
        this.securityGuiceConfigurer = securityGuiceConfigurer;
    }

    @Override
    protected void configureShiroWeb() {
        for (WebConfig.SecurityConfig.UrlConfig urlConfig : securityConfig.getUrls()) {
            String pattern = urlConfig.getPattern();
            List<String> filters = urlConfig.getFilters();
            LOGGER.trace("Binding {} to security filter chain {}", pattern, filters);
            addFilterChain(pattern, getFilterKeys(filters));
        }
        LOGGER.debug("{} URL(s) bound to security filters", securityConfig.getUrls().size());

        bind(WebConfig.SecurityConfig.class);

        // Bind filters which are not PatchMatchingFilters
        bind(AntiXsrfFilter.class);

        // Bind custom filters not extending PathMatchingFilter as Shiro doesn't do it
        for (Class<? extends Filter> customFilter : customFilters) {
            if (!PathMatchingFilter.class.isAssignableFrom(customFilter)) {
                bind(customFilter);
            }
        }

        // Additional web security bindings
        bind(AntiXsrfService.class).to(StatelessAntiXsrfService.class);
        bindConstant().annotatedWith(Names.named("shiro.applicationName")).to(applicationName);

        // Shiro global configuration
        securityGuiceConfigurer.configure(binder());

        // Shiro filter
        bind(GuiceShiroFilter.class).in(Scopes.SINGLETON);

        // Exposed binding
        expose(AntiXsrfService.class);
    }

    @SuppressWarnings("unchecked")
    private FilterKey[] getFilterKeys(List<String> filters) {
        FilterKey[] keys = new FilterKey[filters.size()];
        int index = 0;
        for (String filter : filters) {
            String[] nameConfig = toNameConfigPair(filter);
            Key<? extends Filter> key = findKey(nameConfig[0]);
            if (key != null) {
                keys[index] = new FilterKey(key, nameConfig[1] == null ? "" : nameConfig[1]);
            } else {
                addError("The filter [" + nameConfig[0] + "] could not be found as a default filter or as a class annotated with SecurityFilter");
            }
            index++;
        }
        return keys;
    }

    private Key<? extends Filter> findKey(String filterName) {
        Key<? extends Filter> currentKey = null;
        if (DEFAULT_FILTERS.containsKey(filterName)) {
            currentKey = DEFAULT_FILTERS.get(filterName);
        } else {
            for (Class<? extends Filter> filterClass : customFilters) {
                String name = filterClass.getAnnotation(SecurityFilter.class).value();
                if (filterName.equals(name)) {
                    currentKey = Key.get(filterClass);
                }
            }
        }
        return currentKey;
    }

    /**
     * This method is copied from the same method in Shiro in class DefaultFilterChainManager.
     */
    private String[] toNameConfigPair(String token) throws ConfigurationException {

        String[] pair = token.split("\\[", 2);
        String name = StringUtils.clean(pair[0]);

        if (name == null) {
            throw new IllegalArgumentException("Filter name not found for filter chain definition token: " + token);
        }
        String config = null;

        if (pair.length == 2) {
            config = StringUtils.clean(pair[1]);
            //if there was an open bracket, it assumed there is a closing bracket, so strip it too:
            config = config.substring(0, config.length() - 1);
            config = StringUtils.clean(config);

            //backwards compatibility prior to implementing SHIRO-205:
            //prior to SHIRO-205 being implemented, it was common for end-users to quote the config inside brackets
            //if that config required commas.  We need to strip those quotes to get to the interior quoted definition
            //to ensure any existing quoted definitions still function for end users:
            if (config != null && config.startsWith("\"") && config.endsWith("\"")) {
                String stripped = config.substring(1, config.length() - 1);
                stripped = StringUtils.clean(stripped);

                //if the stripped value does not have any internal quotes, we can assume that the entire config was
                //quoted and we can use the stripped value.
                if (stripped != null && stripped.indexOf('"') == -1) {
                    config = stripped;
                }
                //else:
                //the remaining config does have internal quotes, so we need to assume that each comma delimited
                //pair might be quoted, in which case we need the leading and trailing quotes that we stripped
                //So we ignore the stripped value.
            }
        }

        return new String[]{name, config};

    }
}
