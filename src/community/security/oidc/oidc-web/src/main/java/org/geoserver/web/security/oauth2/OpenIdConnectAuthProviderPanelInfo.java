/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.geoserver.security.oauth2.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/** Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}. */
public class OpenIdConnectAuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<
                GeoServerOAuth2LoginFilterConfig, OpenIdConnectAuthProviderPanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3891569684560944819L;

    public OpenIdConnectAuthProviderPanelInfo() {
        setComponentClass(OpenIdConnectAuthProviderPanel.class);
        setServiceClass(GeoServerOAuth2LoginAuthenticationFilter.class);
        setServiceConfigClass(GeoServerOAuth2LoginFilterConfig.class);
    }
}
