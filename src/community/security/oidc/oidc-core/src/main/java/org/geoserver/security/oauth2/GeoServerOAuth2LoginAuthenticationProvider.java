/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.geoserver.security.oauth2.GeoServerOAuth2UserServices.oauth2UserService;
import static org.geoserver.security.oauth2.GeoServerOAuth2UserServices.oidcUserService;
import static org.geoserver.security.oauth2.OAuth2LoginButtonEnablementEvent.disableButtonEvent;
import static org.geoserver.security.oauth2.OAuth2LoginButtonEnablementEvent.enableButtonEvent;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.Filter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;

/** */
public class GeoServerOAuth2LoginAuthenticationProvider extends AbstractFilterProvider
        implements ApplicationListener<ApplicationEvent> {

    public static final String REG_ID_GIT_HUB = "gitHub";
    public static final String REG_ID_GOOGLE = "google";
    public static final String REG_ID_OIDC = "oidc";
    public static final String REG_ID_MICROSOFT = "microsoft";

    /** Filter types required for GeoServer */
    private static final List<Class<?>> REQ_FILTER_TYPES =
            asList(
                    OAuth2AuthorizationRequestRedirectFilter.class,
                    OAuth2LoginAuthenticationFilter.class,
                    RequestCacheAwareFilter.class);

    private class FilterBuilder {

        private GeoServerOAuth2LoginFilterConfig config;
        private HttpSecurity http;

        /**
         * @param pConfig
         * @param pHttpSecurity
         */
        public FilterBuilder(GeoServerOAuth2LoginFilterConfig pConfig, HttpSecurity pHttpSecurity) {
            super();
            config = pConfig;
            http = pHttpSecurity;
        }

        private List<Filter> createFilters() {
            try {
                return createFiltersImpl();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create OpenID filter.", e);
            }
        }

        private List<Filter> createFiltersImpl() throws Exception {
            OAuth2UserService<OAuth2UserRequest, OAuth2User> lOAuth2UserService;
            OAuth2UserService<OidcUserRequest, OidcUser> lOidcUserService;
            GeoServerRoleResolvers.ResolverContext roleResolverCtx = createRoleResolverContext();

            lOAuth2UserService = oauth2UserService(roleResolverCtx);
            lOidcUserService = oidcUserService(roleResolverCtx);
            ClientRegistrationRepository lCRR = clientRegistrationRepository();
            OAuth2AuthorizedClientService lACS = authorizedClientService(lCRR);
            OAuth2AuthorizedClientRepository lACR = authorizedClientRepository(lACS);

            if (redirectAuto) {
                http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
            }
            http.oauth2Login(
                    oauthConfig -> {
                        oauthConfig.clientRegistrationRepository(lCRR);
                        oauthConfig.authorizedClientRepository(lACR);
                        oauthConfig.authorizedClientService(lACS);
                        oauthConfig.userInfoEndpoint().userService(lOAuth2UserService);
                        oauthConfig.userInfoEndpoint().oidcUserService(lOidcUserService);
                    });

            SecurityFilterChain lChain = http.build();
            List<Filter> lFilters = lChain.getFilters();
            // Chain consists of the following filters, created for a typical spring app:
            // org.springframework.security.web.session.DisableEncodeUrlFilter
            // org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter
            // org.springframework.security.web.context.SecurityContextPersistenceFilter
            // org.springframework.security.web.header.HeaderWriterFilter
            // org.springframework.security.web.csrf.CsrfFilter
            // org.springframework.security.web.authentication.logout.LogoutFilter
            // org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
            // org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter
            // org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter
            // org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter
            // org.springframework.security.web.savedrequest.RequestCacheAwareFilter
            // org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter
            // org.springframework.security.web.authentication.AnonymousAuthenticationFilter
            // org.springframework.security.web.session.SessionManagementFilter
            // org.springframework.security.web.access.ExceptionTranslationFilter

            lFilters =
                    lFilters.stream()
                            .filter(f -> REQ_FILTER_TYPES.contains(f.getClass()))
                            .collect(toList());
            return lFilters;
        }

        private ResolverContext createRoleResolverContext() {
            GeoServerRoleConverter lConverter = null;
            if (PreAuthenticatedUserNameRoleSource.Header.equals(config.getRoleSource())) {
                String converterName = config.getRoleConverterName();
                lConverter = GeoServerRoleResolvers.loadConverter(converterName);
            }
            return new GeoServerRoleResolvers.DefaultResolverContext(
                    securityManager,
                    config.getRoleServiceName(),
                    config.getUserGroupServiceName(),
                    config.getRolesHeaderAttribute(),
                    lConverter,
                    config.getRoleSource());
        }

        private ClientRegistrationRepository clientRegistrationRepository() {
            List<ClientRegistration> lRegistrations = new ArrayList<>();
            if (config.isGoogleEnabled()) {
                lRegistrations.add(googleClientRegistration());
                context.publishEvent(enableButtonEvent(this, REG_ID_GOOGLE));
            } else {
                context.publishEvent(disableButtonEvent(this, REG_ID_GOOGLE));
            }
            if (config.isGitHubEnabled()) {
                lRegistrations.add(gitHubClientRegistration());
                context.publishEvent(enableButtonEvent(this, REG_ID_GIT_HUB));
            } else {
                context.publishEvent(disableButtonEvent(this, REG_ID_GIT_HUB));
            }
            if (config.isMsEnabled()) {
                lRegistrations.add(microsoftClientRegistration());
                context.publishEvent(enableButtonEvent(this, REG_ID_MICROSOFT));
            } else {
                context.publishEvent(disableButtonEvent(this, REG_ID_MICROSOFT));
            }
            if (config.isOidcEnabled()) {
                lRegistrations.add(customProviderRegistration());
                context.publishEvent(enableButtonEvent(this, REG_ID_OIDC));
            } else {
                context.publishEvent(disableButtonEvent(this, REG_ID_OIDC));
            }
            return new InMemoryClientRegistrationRepository(lRegistrations);
        }

        private OAuth2AuthorizedClientService authorizedClientService(
                ClientRegistrationRepository clientRegistrationRepository) {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }

        private OAuth2AuthorizedClientRepository authorizedClientRepository(
                OAuth2AuthorizedClientService authorizedClientService) {
            return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(
                    authorizedClientService);
        }

        private ClientRegistration googleClientRegistration() {
            return CommonOAuth2Provider.GOOGLE
                    // registrationId is used in paths (login and authorization)
                    .getBuilder(REG_ID_GOOGLE)
                    .clientId(config.getGoogleClientId())
                    .clientSecret(config.getGoogleClientSecret())
                    .userNameAttributeName(config.getGoogleUserNameAttribute())
                    .redirectUri(config.getGoogleRedirectUri())
                    .build();
        }

        private ClientRegistration gitHubClientRegistration() {
            return CommonOAuth2Provider.GITHUB
                    // registrationId is used in paths (login and authorization)
                    .getBuilder(REG_ID_GIT_HUB)
                    .clientId(config.getGitHubClientId())
                    .clientSecret(config.getGitHubClientSecret())
                    .userNameAttributeName(config.getGitHubUserNameAttribute())
                    .redirectUri(config.getGitHubRedirectUri())
                    .build();
        }

        private ClientRegistration microsoftClientRegistration() {
            String lScopeTxt = config.getMsScopes();
            String[] lScopes = ScopeUtils.valueOf(lScopeTxt);
            return ClientRegistration
                    // registrationId is used in paths (login and authorization)
                    .withRegistrationId(REG_ID_MICROSOFT)
                    .clientId(config.getMsClientId())
                    .clientSecret(config.getMsClientSecret())
                    .userNameAttributeName(config.getMsUserNameAttribute())
                    .redirectUri(config.getMsRedirectUri())
                    .clientAuthenticationMethod(CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AUTHORIZATION_CODE)
                    .scope(lScopes)
                    // src:
                    // https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration
                    .authorizationUri(
                            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                    .tokenUri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                    .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                    .jwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys")
                    .clientName(REG_ID_MICROSOFT)
                    .build();
        }

        private ClientRegistration customProviderRegistration() {
            String lScopeTxt = config.getOidcScopes();
            String[] lScopes = ScopeUtils.valueOf(lScopeTxt);
            return ClientRegistration
                    // registrationId is used in paths (login and authorization)
                    .withRegistrationId(REG_ID_OIDC)
                    .clientId(config.getOidcClientId())
                    .clientSecret(config.getOidcClientSecret())
                    .userNameAttributeName(config.getOidcUserNameAttribute())
                    .redirectUri(config.getOidcRedirectUri())
                    .clientAuthenticationMethod(CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AUTHORIZATION_CODE)
                    .scope(lScopes)
                    .authorizationUri(config.getOidcAuthorizationUri())
                    .tokenUri(config.getOidcTokenUri())
                    .userInfoUri(config.getOidcUserInfoUri())
                    .jwkSetUri(config.getOidcJwkSetUri())
                    // TODO AW what about issuer uri?
                    .clientName(REG_ID_OIDC)
                    .build();
        }
    }

    private static final Logger LOGGER =
            Logging.getLogger(GeoServerOAuth2LoginAuthenticationProvider.class);

    private GeoServerSecurityManager securityManager;
    private ApplicationContext context;
    private boolean redirectAuto = false;

    public GeoServerOAuth2LoginAuthenticationProvider(GeoServerSecurityManager pSecurityManager) {
        this.securityManager = pSecurityManager;
        context = pSecurityManager.getApplicationContext();
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream()
                .alias("openIdConnectAuthentication", GeoServerOAuth2LoginFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerOAuth2LoginAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        GeoServerOAuth2LoginFilterConfig lConfig = (GeoServerOAuth2LoginFilterConfig) config;
        HttpSecurity httpSecurity = context.getBean(HttpSecurity.class);

        FilterBuilder lBuilder = new FilterBuilder(lConfig, httpSecurity);
        List<Filter> lFilters = lBuilder.createFilters();

        GeoServerCompositeFilter filter = new GeoServerOAuth2LoginAuthenticationFilter();
        filter.setNestedFilters(lFilters);
        return filter;
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new OpenIdConnectFilterConfigValidator(securityManager);
    }

    /**
     * Provide a helpful OIDC_LOGGING configuration for this extension on context load event.
     *
     * @param event application event, responds ContextLoadEvent
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextLoadedEvent) {
            // provide a helpful logging config for this extension
            GeoServer geoserver = GeoServerExtensions.bean(GeoServer.class, this.context);
            GeoServerResourceLoader loader = geoserver.getCatalog().getResourceLoader();
            LoggingUtils.checkBuiltInLoggingConfiguration(loader, "OIDC_LOGGING");
        }
    }
}
