/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Collection;
import java.util.logging.Logger;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.filter.GeoServerRoleResolvers.RoleResolver;
import org.geoserver.security.impl.GeoServerRole;
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

/** */
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

            Collection<GeoServerRole> roles = determineRoles(lUserName);

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

            Collection<GeoServerRole> roles = determineRoles(lUserName);

            return new DefaultOidcUser(
                    roles, lUser.getIdToken(), lUser.getUserInfo(), lUserNameAttributeName);
        }
    }

    public static OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext) {
        GeoServerOidcUserService lService = new GeoServerOidcUserService();
        lService.setResolverContext(pResolverContext);
        return lService;
    }

    public static OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext) {
        GeoServerOAuth2UserService lService = new GeoServerOAuth2UserService();
        lService.setResolverContext(pResolverContext);
        return lService;
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

    protected Collection<GeoServerRole> determineRoles(String pUserName) {
        ResolverParam lParam = new ResolverParam(pUserName, null, resolverContext);
        RoleResolver lResolver = GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
        Collection<GeoServerRole> roles = lResolver.convert(lParam);
        return roles;
    }

    /** @param pResolverContext the resolverContext to set */
    public void setResolverContext(GeoServerRoleResolvers.ResolverContext pResolverContext) {
        resolverContext = pResolverContext;
    }
}
