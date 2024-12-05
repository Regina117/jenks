/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.validation.FilterConfigException;
import org.springframework.util.StringUtils;

// TODO AW: review & complete for new fields
public class OpenIdConnectFilterConfigValidator extends OAuth2FilterConfigValidator {

    public OpenIdConnectFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    public void validateOAuth2FilterConfig(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws FilterConfigException {
        super.validateOAuth2FilterConfig(filterConfig);
        GeoServerOAuth2LoginFilterConfig oidcFilterConfig =
                (GeoServerOAuth2LoginFilterConfig) filterConfig;

        if (StringUtils.hasLength(oidcFilterConfig.getOidcJwkSetUri()) != false) {
            try {
                new URL(oidcFilterConfig.getOidcJwkSetUri());
            } catch (MalformedURLException ex) {
                throw new OpenIdConnectFilterConfigException(
                        OpenIdConnectFilterConfigException.OAUTH2_WKTS_URL_MALFORMED);
            }
        }
    }
    /** Only require checkTokenEndpointUrl if JSON Web Key set URI is empty. */
    protected void validateCheckTokenEndpointUrl(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws FilterConfigException {
        var oidcFilterConfig = (GeoServerOAuth2LoginFilterConfig) filterConfig;
        if (StringUtils.hasLength(filterConfig.getOidcUserInfoUri()) == false
                && StringUtils.hasLength(oidcFilterConfig.getOidcJwkSetUri()) == false) {
            // One of checkTokenEndpointUrl or jwkURI is required
            throw new OpenIdConnectFilterConfigException(
                    OpenIdConnectFilterConfigException
                            .OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED);
        }
        if (StringUtils.hasLength(filterConfig.getOidcUserInfoUri()) != false) {
            try {
                new URL(filterConfig.getOidcUserInfoUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED);
            }
        }
    }

    /** Only require {@code client_secret} when not using PKCE. */
    protected void validateClientSecret(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws FilterConfigException {
        var oidcFilterConfig = (GeoServerOAuth2LoginFilterConfig) filterConfig;
        if (!oidcFilterConfig.isUsePKCE()) {
            super.validateClientSecret(filterConfig);
        }
    }
}
