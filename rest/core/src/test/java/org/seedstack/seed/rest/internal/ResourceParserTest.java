/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.rest.internal;

import com.google.common.collect.Lists;
import io.nuun.kernel.api.annotations.Ignore;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.seedstack.seed.rest.Rel;
import org.seedstack.seed.rest.RestConfig;
import org.seedstack.seed.rest.hal.Link;
import org.seedstack.seed.rest.internal.jsonhome.Resource;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author pierre.thirouin@ext.mpsa.com (Pierre Thirouin)
 */
@Ignore // Tells nuun to not scan the test class
@RunWith(JMockit.class)
public class ResourceParserTest {

    private static final String SERVLET_CONTEXT_PATH = "/app/";
    private static final String REST_PATH = "/rest/";
    private static final String BASE_REL = "http://example.org/rel/";
    private static final String BASE_PARAM = "http://example.org/param/";

    private Map<String, Resource> resourceMap;
    private Map<String, Link> links;
    @Mocked
    private RestConfig restConfig;
    @Mocked
    private ServletContext servletContext;

    @Ignore
    @Rel(value = "widget", home = true)
    @Path("widgets/{widgetName: [a-zA-Z][a-zA-Z_0-9]}")
    private static class MyLinkTemplateResource {

        @GET
        public Response get(@PathParam("widgetName") String widgetId, @QueryParam("lang") String lang) {
            return null;
        }

        @PUT
        public Response put(@PathParam("widgetName") String widgetId) {
            return null;
        }
    }

    @Before
    public void before() {
        new Expectations() {
            {
                restConfig.getPath();
                result = REST_PATH;
                restConfig.getBaseRel();
                result = BASE_REL;
                restConfig.getBaseParam();
                result = BASE_PARAM;
                servletContext.getContextPath();
                result = SERVLET_CONTEXT_PATH;
            }
        };

        ResourceScanner resourceScanner = new ResourceScanner(restConfig, servletContext);
        resourceScanner.scan(Lists.<Class<?>>newArrayList(MyLinkTemplateResource.class));
        resourceMap = resourceScanner.jsonHomeResources();
        links = resourceScanner.halLinks();
        Assertions.assertThat(resourceMap).isNotNull();
    }

    @Test
    public void testCreateLinkTemplateResource() {
        // Path on method
        Resource underTest = resourceMap.get(UriBuilder.uri(BASE_REL, "widget"));
        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(underTest.rel()).isEqualTo(UriBuilder.uri(BASE_REL, "widget"));

        Assertions.assertThat(underTest.hrefTemplate()).isEqualTo("/app/rest/widgets/{widgetName}{?lang}");
        Assertions.assertThat(underTest.hrefVars()).hasSize(2);
        Assertions.assertThat(underTest.hrefVars().get("widgetName")).isEqualTo(UriBuilder.uri(BASE_PARAM, "widgetName"));
        Assertions.assertThat(underTest.hrefVars().get("lang")).isEqualTo(UriBuilder.uri(BASE_PARAM, "lang"));
    }

    @Test
    public void testCreateResource() {
        Resource widgetResource = resourceMap.get(UriBuilder.uri(BASE_REL, "widget"));
        Assertions.assertThat(widgetResource).isNotNull();
        Map<String, Object> underTest = widgetResource.toRepresentation();

        Assertions.assertThat(underTest).hasSize(3);

        String hrefTemplate = (String) underTest.get("href-template");
        Assertions.assertThat(hrefTemplate).isEqualTo("/app/rest/widgets/{widgetName}");
    }

    @Test
    public void testHalLinks() {
        Assertions.assertThat(new Link(links.get("widget"))).isNotNull();
        Assertions.assertThat(new Link(links.get("widget")).templated().getHref()).isEqualTo("/app/rest/widgets/{widgetName}{?lang}");
        Assertions.assertThat(new Link(links.get("widget")).set("lang", "EN").set("widgetName", 1).getHref()).isEqualTo("/app/rest/widgets/1?lang=EN");
    }
}
