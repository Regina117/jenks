/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static java.util.Optional.ofNullable;
import static org.geoserver.security.oauth2.GeoServerOAuth2LoginAuthenticationProvider.REG_ID_GIT_HUB;
import static org.geoserver.security.oauth2.GeoServerOAuth2LoginAuthenticationProvider.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.GeoServerOAuth2LoginAuthenticationProvider.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.GeoServerOAuth2LoginAuthenticationProvider.REG_ID_OIDC;

import java.util.Optional;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Filter configuration for OpenId Connect. This is completely freeform, so adding only the basic
 * bits in here.
 */
public class GeoServerOAuth2LoginFilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = -8581346584859849804L;

    /** Supports extraction of roles among the token claims */
    public static enum OpenIdRoleSource implements RoleSource {
        IdToken,
        AccessToken,
        MSGraphAPI,
        UserInfo;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    };

    /**
     * Constant used to setup the proxy base in tests that are running without a GeoServer instance
     * or an actual HTTP request context. The value of the variable is set-up in the pom.xml, as a
     * system property for surefire, in order to avoid hard-coding the value in the code.
     */
    public static final String OPENID_TEST_GS_PROXY_BASE = "OPENID_TEST_GS_PROXY_BASE";

    // Common
    private String baseRedirectUri = baseRedirectUri();

    // Google
    private boolean googleEnabled;
    private String googleClientId;
    private String googleClientSecret;
    private String googleUserNameAttribute = "email";
    private String googleRedirectUri;

    // GitHub
    private boolean gitHubEnabled;
    private String gitHubClientId;
    private String gitHubClientSecret;
    private String gitHubUserNameAttribute = "id";
    private String gitHubRedirectUri;

    // Microsoft
    private boolean msEnabled;
    private String msClientId;
    private String msClientSecret;
    private String msUserNameAttribute = "sub";
    private String msRedirectUri;

    // custom OpenID Connect
    private boolean enabled;
    private String cliendId;
    private String clientSecret;
    private String principalKey = "email";
    private String redirectUri = "http://localhost:8080/geoserver"; // TODO AW

    private String scopes;
    private String tokenRolesClaim;
    private String responseMode;

    private String accessTokenUri;
    private String userAuthorizationUri;
    private String checkTokenEndpointUrl;
    private String jwkURI;
    private String logoutUri;
    private String postLogoutRedirectUri;

    private Boolean enableRedirectAuthenticationEntryPoint;
    private Boolean forceAccessTokenUriHttps;
    private Boolean forceUserAuthorizationUriHttps;
    private boolean sendClientSecret = false;
    private boolean allowBearerTokens = true;
    private boolean usePKCE = false;
    private boolean enforceTokenValidation = true;

    /**
     * Add extra logging. NOTE: this might spill confidential information to the log - do not turn
     * on in normal operation!
     */
    private boolean allowUnSecureLogging = false;

    public GeoServerOAuth2LoginFilterConfig() {
        this.redirectUri = baseRedirectUri();
        this.postLogoutRedirectUri = baseRedirectUri();
        this.scopes = "user";
        this.enableRedirectAuthenticationEntryPoint = false;
        this.forceAccessTokenUriHttps = true;
        this.forceUserAuthorizationUriHttps = true;
        this.calculateredirectUris();
    };

    public void calculateredirectUris() {
        this.googleRedirectUri = redirectUri(REG_ID_GOOGLE);
        this.gitHubRedirectUri = redirectUri(REG_ID_GIT_HUB);
        this.msRedirectUri = redirectUri(REG_ID_MICROSOFT);
        this.redirectUri = redirectUri(REG_ID_OIDC);
    }

    /**
     * @param pRegId
     * @return
     */
    private String redirectUri(String pRegId) {
        String lBase =
                ofNullable(baseRedirectUri).map(s -> s.endsWith("/") ? s : s + "/").orElse("/");
        return lBase + "login/oauth2/code/" + pRegId;
    }

    /**
     * we add "/" at the end since not having it will SOMETIME cause issues. This will either use
     * the proxyBaseURL (if set), or from ServletUriComponentsBuilder.fromCurrentContextPath().
     *
     * @return
     */
    String baseRedirectUri() {
        Optional<String> proxbaseUrl =
                Optional.ofNullable(GeoServerExtensions.bean(GeoServer.class))
                        .map(gs -> gs.getSettings())
                        .map(s -> s.getProxyBaseUrl());
        if (proxbaseUrl.isPresent() && StringUtils.hasText(proxbaseUrl.get())) {
            return proxbaseUrl + "/";
        }
        if (RequestContextHolder.getRequestAttributes() != null)
            return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/";
        // fallback to run tests without a full environment
        return GeoServerExtensions.getProperty(OPENID_TEST_GS_PROXY_BASE);
    }

    public String getPrincipalKey() {
        return principalKey == null ? "email" : principalKey;
    }

    public boolean providesAuthenticationEntryPoint() {
        return true; // TODO AW
    }

    public boolean isAllowUnSecureLogging() {
        return allowUnSecureLogging;
    }

    public void setAllowUnSecureLogging(boolean allowUnSecureLogging) {
        this.allowUnSecureLogging = allowUnSecureLogging;
    }

    /** @return the cliendId */
    public String getCliendId() {
        return cliendId;
    }

    /** @param cliendId the cliendId to set */
    public void setCliendId(String cliendId) {
        this.cliendId = cliendId;
    }

    /** @return the clientSecret */
    public String getClientSecret() {
        return clientSecret;
    }

    /** @param clientSecret the clientSecret to set */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /** @return the accessTokenUri */
    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    /** @param accessTokenUri the accessTokenUri to set */
    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    /** @return the userAuthorizationUri */
    public String getUserAuthorizationUri() {
        return userAuthorizationUri;
    }

    /** @param userAuthorizationUri the userAuthorizationUri to set */
    public void setUserAuthorizationUri(String userAuthorizationUri) {
        this.userAuthorizationUri = userAuthorizationUri;
    }

    /** @return the redirectUri */
    public String getRedirectUri() {
        return redirectUri;
    }

    /** @param redirectUri the redirectUri to set */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /** @return the checkTokenEndpointUrl */
    public String getCheckTokenEndpointUrl() {
        return checkTokenEndpointUrl;
    }

    /** @param checkTokenEndpointUrl the checkTokenEndpointUrl to set */
    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl) {
        this.checkTokenEndpointUrl = checkTokenEndpointUrl;
    }

    /** @return the logoutUri */
    public String getLogoutUri() {
        return logoutUri;
    }

    /** @param logoutUri the logoutUri to set */
    public void setLogoutUri(String logoutUri) {
        this.logoutUri = logoutUri;
    }

    /** @return the scopes */
    public String getScopes() {
        return scopes;
    }

    /** @param scopes the scopes to set */
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    /** @return the enableRedirectAuthenticationEntryPoint */
    public Boolean getEnableRedirectAuthenticationEntryPoint() {
        return enableRedirectAuthenticationEntryPoint;
    }

    /**
     * @param enableRedirectAuthenticationEntryPoint the enableRedirectAuthenticationEntryPoint to
     *     set
     */
    public void setEnableRedirectAuthenticationEntryPoint(
            Boolean enableRedirectAuthenticationEntryPoint) {
        this.enableRedirectAuthenticationEntryPoint = enableRedirectAuthenticationEntryPoint;
    }

    public Boolean getForceAccessTokenUriHttps() {
        return forceAccessTokenUriHttps;
    }

    public void setForceAccessTokenUriHttps(Boolean forceAccessTokenUriHttps) {
        this.forceAccessTokenUriHttps = forceAccessTokenUriHttps;
    }

    public Boolean getForceUserAuthorizationUriHttps() {
        return forceUserAuthorizationUriHttps;
    }

    public void setForceUserAuthorizationUriHttps(Boolean forceUserAuthorizationUriHttps) {
        this.forceUserAuthorizationUriHttps = forceUserAuthorizationUriHttps;
    }

    public void setPrincipalKey(String principalKey) {
        this.principalKey = principalKey;
    }

    public String getJwkURI() {
        return jwkURI;
    }

    public void setJwkURI(String jwkURI) {
        this.jwkURI = jwkURI;
    }

    public String getTokenRolesClaim() {
        return tokenRolesClaim;
    }

    public void setTokenRolesClaim(String tokenRolesClaim) {
        this.tokenRolesClaim = tokenRolesClaim;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public boolean isSendClientSecret() {
        return sendClientSecret;
    }

    public void setSendClientSecret(boolean sendClientSecret) {
        this.sendClientSecret = sendClientSecret;
    }

    public boolean isAllowBearerTokens() {
        return allowBearerTokens;
    }

    public void setAllowBearerTokens(boolean allowBearerTokens) {
        this.allowBearerTokens = allowBearerTokens;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public boolean isUsePKCE() {
        return usePKCE;
    }

    public void setUsePKCE(boolean usePKCE) {
        this.usePKCE = usePKCE;
    }

    public boolean isEnforceTokenValidation() {
        return enforceTokenValidation;
    }

    public void setEnforceTokenValidation(boolean enforceTokenValidation) {
        this.enforceTokenValidation = enforceTokenValidation;
    }

    /** @return the googleEnabled */
    public boolean isGoogleEnabled() {
        return googleEnabled;
    }

    /** @param pGoogleEnabled the googleEnabled to set */
    public void setGoogleEnabled(boolean pGoogleEnabled) {
        googleEnabled = pGoogleEnabled;
    }

    /** @return the googleCliendId */
    public String getGoogleClientId() {
        return googleClientId;
    }

    /** @param pGoogleCliendId the googleCliendId to set */
    public void setGoogleClientId(String pGoogleCliendId) {
        googleClientId = pGoogleCliendId;
    }

    /** @return the googleClientSecret */
    public String getGoogleClientSecret() {
        return googleClientSecret;
    }

    /** @param pGoogleClientSecret the googleClientSecret to set */
    public void setGoogleClientSecret(String pGoogleClientSecret) {
        googleClientSecret = pGoogleClientSecret;
    }

    /** @return the googleUserNameAttribute */
    public String getGoogleUserNameAttribute() {
        return googleUserNameAttribute;
    }

    /** @param pGoogleUserNameAttribute the googleUserNameAttribute to set */
    public void setGoogleUserNameAttribute(String pGoogleUserNameAttribute) {
        googleUserNameAttribute = pGoogleUserNameAttribute;
    }

    /** @return the gitHubEnabled */
    public boolean isGitHubEnabled() {
        return gitHubEnabled;
    }

    /** @param pGitHubEnabled the gitHubEnabled to set */
    public void setGitHubEnabled(boolean pGitHubEnabled) {
        gitHubEnabled = pGitHubEnabled;
    }

    /** @return the gitHubClientId */
    public String getGitHubClientId() {
        return gitHubClientId;
    }

    /** @param pGitHubClientId the gitHubClientId to set */
    public void setGitHubClientId(String pGitHubClientId) {
        gitHubClientId = pGitHubClientId;
    }

    /** @return the gitHubClientSecret */
    public String getGitHubClientSecret() {
        return gitHubClientSecret;
    }

    /** @param pGitHubClientSecret the gitHubClientSecret to set */
    public void setGitHubClientSecret(String pGitHubClientSecret) {
        gitHubClientSecret = pGitHubClientSecret;
    }

    /** @return the gitHubUserNameAttribute */
    public String getGitHubUserNameAttribute() {
        return gitHubUserNameAttribute;
    }

    /** @param pGitHubUserNameAttribute the gitHubUserNameAttribute to set */
    public void setGitHubUserNameAttribute(String pGitHubUserNameAttribute) {
        gitHubUserNameAttribute = pGitHubUserNameAttribute;
    }

    /** @return the enabled */
    public boolean isEnabled() {
        return enabled;
    }

    /** @param pEnabled the enabled to set */
    public void setEnabled(boolean pEnabled) {
        enabled = pEnabled;
    }

    /** @return the msEnabled */
    public boolean isMsEnabled() {
        return msEnabled;
    }

    /** @param pMsEnabled the msEnabled to set */
    public void setMsEnabled(boolean pMsEnabled) {
        msEnabled = pMsEnabled;
    }

    /** @return the msClientId */
    public String getMsClientId() {
        return msClientId;
    }

    /** @param pMsClientId the msClientId to set */
    public void setMsClientId(String pMsClientId) {
        msClientId = pMsClientId;
    }

    /** @return the msClientSecret */
    public String getMsClientSecret() {
        return msClientSecret;
    }

    /** @param pMsClientSecret the msClientSecret to set */
    public void setMsClientSecret(String pMsClientSecret) {
        msClientSecret = pMsClientSecret;
    }

    /** @return the msNameAttribute */
    public String getMsUserNameAttribute() {
        return msUserNameAttribute;
    }

    /** @param pMsNameAttribute the msNameAttribute to set */
    public void setMsUserNameAttribute(String pMsNameAttribute) {
        msUserNameAttribute = pMsNameAttribute;
    }

    /** @return the baseRedirectUri */
    public String getBaseRedirectUri() {
        return baseRedirectUri;
    }

    /** @param pBaseRedirectUri the baseRedirectUri to set */
    public void setBaseRedirectUri(String pBaseRedirectUri) {
        baseRedirectUri = pBaseRedirectUri;
    }

    /** @return the googleRedirectUri */
    public String getGoogleRedirectUri() {
        return googleRedirectUri;
    }

    /** @param pGoogleRedirectUri the googleRedirectUri to set */
    public void setGoogleRedirectUri(String pGoogleRedirectUri) {
        googleRedirectUri = pGoogleRedirectUri;
    }

    /** @return the gitHubRedirectUri */
    public String getGitHubRedirectUri() {
        return gitHubRedirectUri;
    }

    /** @param pGitHubRedirectUri the gitHubRedirectUri to set */
    public void setGitHubRedirectUri(String pGitHubRedirectUri) {
        gitHubRedirectUri = pGitHubRedirectUri;
    }

    /** @return the msRedirectUri */
    public String getMsRedirectUri() {
        return msRedirectUri;
    }

    /** @param pMsRedirectUri the msRedirectUri to set */
    public void setMsRedirectUri(String pMsRedirectUri) {
        msRedirectUri = pMsRedirectUri;
    }
}
