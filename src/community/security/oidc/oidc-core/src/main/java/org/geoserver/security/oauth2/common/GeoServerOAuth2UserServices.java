/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2.common;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.oauth2.common.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Provides {@link OAuth2UserService} implementation for OAuth2 and OpenID Connect. Allows for
 * integration with the GeoServer supported user role sources.
 *
 * @author awaterme
 */
public class GeoServerOAuth2UserServices {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2UserServices.class);

    private static class GeoServerOAuth2UserService extends GeoServerOAuth2UserServices
            implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

        private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate =
                new DefaultOAuth2UserService();

        public GeoServerOAuth2UserService() {
            super();
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest pUserRequest)
                throws OAuth2AuthenticationException {
            OAuth2User lUser = delegate.loadUser(pUserRequest);
            String lUserName = lUser.getName();
            String lUserNameAttributeName = userNameAttributeName(pUserRequest);

            Collection<GeoServerRole> roles = determineRoles(lUserName, pUserRequest);
            return new DefaultOAuth2User(roles, lUser.getAttributes(), lUserNameAttributeName);
        }
    }

    private static class GeoServerOidcUserService extends GeoServerOAuth2UserServices
            implements OAuth2UserService<OidcUserRequest, OidcUser> {

        private OAuth2UserService<OidcUserRequest, OidcUser> delegate = new OidcUserService();

        public GeoServerOidcUserService() {
            super();
        }

        @Override
        public OidcUser loadUser(OidcUserRequest pUserRequest)
                throws OAuth2AuthenticationException {
            OidcUser lUser = delegate.loadUser(pUserRequest);
            String lUserName = lUser.getName();
            String lUserNameAttributeName = userNameAttributeName(pUserRequest);

            Collection<GeoServerRole> roles = determineRoles(lUserName, pUserRequest);
            return new DefaultOidcUser(
                    roles, lUser.getIdToken(), lUser.getUserInfo(), lUserNameAttributeName);
        }
    }

    public static OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext,
            Supplier<HttpServletRequest> pReqSupplier,
            GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOidcUserService lService = new GeoServerOidcUserService();
        init(lService, pResolverContext, pReqSupplier, pConfig);
        return lService;
    }

    public static OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext,
            Supplier<HttpServletRequest> pReqSupplier,
            GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOAuth2UserService lService = new GeoServerOAuth2UserService();
        init(lService, pResolverContext, pReqSupplier, pConfig);
        return lService;
    }

    private static void init(
            GeoServerOAuth2UserServices pService,
            GeoServerRoleResolvers.ResolverContext pResolverContext,
            Supplier<HttpServletRequest> pReqSupplier,
            GeoServerOAuth2LoginFilterConfig pConfig) {
        pService.setResolverContext(pResolverContext);
        pService.setRequestSupplier(pReqSupplier);
        pService.setConfig(pConfig);
    }

    private GeoServerOAuth2UserServices() {
        super();
    }

    protected String userNameAttributeName(OAuth2UserRequest pUserRequest) {
        // null check performed by delegate already
        String lUserNameAttributeName =
                pUserRequest
                        .getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName();
        return lUserNameAttributeName;
    }

    protected GeoServerRoleResolvers.ResolverContext resolverContext;
    protected Supplier<HttpServletRequest> requestSupplier;
    protected GeoServerOAuth2LoginFilterConfig config;

    protected Collection<GeoServerRole> determineRoles(
            String pUserName, OAuth2UserRequest pRequest) {
        LOGGER.fine("Resolving roles for user '" + pUserName + "'.");
        HttpServletRequest lRequest = requestSupplier.get();
        ResolverParam lParam =
                new OAuth2ResolverParam(pUserName, lRequest, resolverContext, pRequest);
        GeoServerOAuth2RoleResolver lResolver = new GeoServerOAuth2RoleResolver(config);
        Collection<GeoServerRole> roles = lResolver.convert(lParam);
        return roles;
    }

    /** @param pRequestSupplier the requestSupplier to set */
    public void setRequestSupplier(Supplier<HttpServletRequest> pRequestSupplier) {
        requestSupplier = pRequestSupplier;
    }

    /** @param pResolverContext the resolverContext to set */
    public void setResolverContext(GeoServerRoleResolvers.ResolverContext pResolverContext) {
        resolverContext = pResolverContext;
    }

    /** @param pConfig the config to set */
    public void setConfig(GeoServerOAuth2LoginFilterConfig pConfig) {
        config = pConfig;
    }
}
