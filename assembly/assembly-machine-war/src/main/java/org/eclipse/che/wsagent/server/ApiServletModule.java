/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.wsagent.server;

import com.google.common.collect.ImmutableMap;
import com.google.inject.servlet.ServletModule;

import org.apache.catalina.filters.CorsFilter;
import org.eclipse.che.env.local.server.SingleEnvironmentFilter;
import org.eclipse.che.inject.DynaModule;
import org.everrest.guice.servlet.GuiceEverrestServlet;
import org.everrest.websockets.WSConnectionTracker;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static org.apache.catalina.filters.CorsFilter.PARAM_CORS_ALLOWED_HEADERS;
import static org.apache.catalina.filters.CorsFilter.PARAM_CORS_ALLOWED_METHODS;
import static org.apache.catalina.filters.CorsFilter.PARAM_CORS_ALLOWED_ORIGINS;
import static org.apache.catalina.filters.CorsFilter.PARAM_CORS_PREFLIGHT_MAXAGE;
import static org.apache.catalina.filters.CorsFilter.PARAM_CORS_SUPPORT_CREDENTIALS;

/** @author andrew00x */
@DynaModule
public class ApiServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
        getServletContext().addListener(new WSConnectionTracker());

        final Map<String, String> corsFilterParams = new HashMap<>();
        corsFilterParams.put(PARAM_CORS_ALLOWED_ORIGINS, "http://localhost:8080");
        corsFilterParams.put(PARAM_CORS_ALLOWED_METHODS, "GET," +
                                                         "POST," +
                                                         "HEAD," +
                                                         "OPTIONS," +
                                                         "PUT," +
                                                         "DELETE");
        corsFilterParams.put(PARAM_CORS_ALLOWED_HEADERS, "Content-Type," +
                                                         "X-Requested-With," +
                                                         "accept," +
                                                         "Origin," +
                                                         "Access-Control-Request-Method," +
                                                         "Access-Control-Request-Headers");
        corsFilterParams.put(PARAM_CORS_SUPPORT_CREDENTIALS, "true");
        // preflight cache is available for 10 minutes
        corsFilterParams.put(PARAM_CORS_PREFLIGHT_MAXAGE, "10");

        bind(CorsFilter.class).in(Singleton.class);
        filter("/*").through(CorsFilter.class, corsFilterParams);

        filter("/ext/*").through(SingleEnvironmentFilter.class);
//        serve("/ext/*").with(GuiceEverrestServlet.class);
        serveRegex("^/ext((?!(/(ws|eventbus)($|/.*)))/.*)").with(GuiceEverrestServlet.class);

        bind(io.swagger.jaxrs.config.DefaultJaxrsConfig.class).asEagerSingleton();
        serve("/swaggerinit").with(io.swagger.jaxrs.config.DefaultJaxrsConfig.class, ImmutableMap
                .of("api.version", "1.0",
                    "swagger.api.title", "Eclipse Che",
                    "swagger.api.basepath", "/ide/ext"
                   ));
    }
}
