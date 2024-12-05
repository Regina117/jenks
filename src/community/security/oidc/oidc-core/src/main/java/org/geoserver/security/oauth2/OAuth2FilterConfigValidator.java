/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.springframework.util.StringUtils;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *     <p>Validates {@link OAuth2FilterConfig} objects.
 */
public class OAuth2FilterConfigValidator extends FilterConfigValidator {

    public OAuth2FilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
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

    /**
     * Validate {@code client_secret} if required.
     *
     * <p>Default implementation requires {@code client_secret} to be provided. Subclasses can
     * override if working with a public client that cannot keep a secret.
     */
    protected void validateClientSecret(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws FilterConfigException {

        if (filterConfig.isUsePKCE()) {
            return;
        }

        if (!StringUtils.hasLength(filterConfig.getOidcClientSecret())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_CLIENT_SECRET_REQUIRED);
        }
    }

    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {

        if (config instanceof GeoServerOAuth2LoginFilterConfig) {
            validateOAuth2FilterConfig((GeoServerOAuth2LoginFilterConfig) config);
        } else {
            super.validateFilterConfig(config);
        }
    }

    public void validateOAuth2FilterConfig(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws FilterConfigException {
        if (StringUtils.hasLength(filterConfig.getLogoutUri())) {
            try {
                new URL(filterConfig.getLogoutUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_URL_IN_LOGOUT_URI_MALFORMED);
            }
        }
        super.validateFilterConfig((SecurityNamedServiceConfig) filterConfig);

        validateCheckTokenEndpointUrl(filterConfig);

        if (StringUtils.hasLength(filterConfig.getOidcTokenUri())) {
            URL accessTokenUri = null;
            try {
                accessTokenUri = new URL(filterConfig.getOidcTokenUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED);
            }
            if (filterConfig.getOidcForceTokenUriHttps()
                    && "https".equalsIgnoreCase(accessTokenUri.getProtocol()) == false)
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_NOT_HTTPS);
        }

        if (StringUtils.hasLength(filterConfig.getOidcAuthorizationUri())) {
            URL userAuthorizationUri = null;
            try {
                userAuthorizationUri = new URL(filterConfig.getOidcAuthorizationUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED);
            }
            if (filterConfig.getOidcForceAuthorizationUriHttps()
                    && "https".equalsIgnoreCase(userAuthorizationUri.getProtocol()) == false)
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_USERAUTHURI_NOT_HTTPS);
        }

        if (StringUtils.hasLength(filterConfig.getOidcRedirectUri())) {
            try {
                new URL(filterConfig.getOidcRedirectUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_REDIRECT_URI_MALFORMED);
            }
        }

        if (!StringUtils.hasLength(filterConfig.getOidcClientId())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED);
        }

        validateClientSecret(filterConfig);

        if (!StringUtils.hasLength(filterConfig.getOidcScopes())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_SCOPE_REQUIRED);
        }

        if (StringUtils.hasLength(filterConfig.getOidcJwkSetUri()) != false) {
            try {
                new URL(filterConfig.getOidcJwkSetUri());
            } catch (MalformedURLException ex) {
                throw new OpenIdConnectFilterConfigException(
                        OpenIdConnectFilterConfigException.OAUTH2_WKTS_URL_MALFORMED);
            }
        }
    }

    @Override
    protected OAuth2FilterConfigException createFilterException(String errorid, Object... args) {
        return new OAuth2FilterConfigException(errorid, args);
    }
}
