/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.GeoServerOAuth2LoginAuthenticationProvider.REG_ID_OIDC;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID;

import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.GeoServerOAuth2LoginFilterConfig;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;

/**
 * Adapts {@link OAuth2AuthorizationRequest}s to specific needs. Currently used to handle
 * "response_mode" special case.
 *
 * @author awaterme
 */
public class GeoServerAuthorizationRequestCustomizer
        implements Consumer<OAuth2AuthorizationRequest.Builder> {

    private static final Logger LOGGER =
            Logging.getLogger(GeoServerAuthorizationRequestCustomizer.class);

    private GeoServerOAuth2LoginFilterConfig config;

    /** @param pConfig */
    public GeoServerAuthorizationRequestCustomizer(GeoServerOAuth2LoginFilterConfig pConfig) {
        super();
        config = pConfig;
    }

    @Override
    public void accept(Builder pBuilder) {
        Consumer<Map<String, Object>> lCustomizer =
                attr -> {
                    Object lRegId = attr.get(REGISTRATION_ID);
                    if (!REG_ID_OIDC.equals(lRegId)) {
                        // supposed to be used for OIDC only
                        return;
                    }
                    handleResponseModeParam(pBuilder);
                    handlePKCE(pBuilder);
                };
        pBuilder.attributes(lCustomizer);
    }

    /** @param pBuilder */
    private void handlePKCE(Builder pBuilder) {
        boolean lUsePKCE = config.isOidcUsePKCE();
        if (lUsePKCE) {
            Consumer<Builder> lConsumer = OAuth2AuthorizationRequestCustomizers.withPkce();
            lConsumer.accept(pBuilder);
        }
    }

    private void handleResponseModeParam(Builder pBuilder) {
        String lResponseMode = config.getOidcResponseMode();
        if (lResponseMode == null || lResponseMode.isBlank()) {
            return;
        }
        String lMode = lResponseMode.trim();
        LOGGER.fine("Adding 'response_mode' parameter to authorize request: '" + lMode + "'.");
        pBuilder.additionalParameters(m -> m.put("response_mode", lMode));
    }
}
