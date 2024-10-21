/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geotools.util.logging.Logging;

/**
 * {@link Filter} supports OpenID Connect and OAuth2 based logins by delegating to the nested Spring
 * filter implementations.
 */
public class GeoServerOAuth2ResourceServerAuthenticationFilter extends GeoServerCompositeFilter
        implements GeoServerAuthenticationFilter {

    private static final Logger LOGGER =
            Logging.getLogger(GeoServerOAuth2ResourceServerAuthenticationFilter.class);

    /** @param filterConfig */
    public GeoServerOAuth2ResourceServerAuthenticationFilter() {
        super();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LOGGER.log(Level.FINER, "Running filter.");
        super.doFilter(request, response, chain);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig pConfig) throws IOException {
        LOGGER.log(Level.FINE, "Initializing filter.");
        super.initializeFromConfig(pConfig);
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }
}
