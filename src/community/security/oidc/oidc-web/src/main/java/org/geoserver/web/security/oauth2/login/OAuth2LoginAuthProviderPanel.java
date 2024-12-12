/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 *
 */

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 *
 */
package org.geoserver.web.security.oauth2.login;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.AccessToken;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.IdToken;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.MSGraphAPI;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.UserInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2LoginAuthProviderPanel
        extends PreAuthenticatedUserNameFilterPanel<GeoServerOAuth2LoginFilterConfig> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3025321797363970333L;

    /** Prefix of Microsoft specific attributes */
    private static final String PREFIX_MS = "ms";

    /** Prefix of GitHub specific attributes */
    private static final String PREFIX_GIT_HUB = "gitHub";

    /** Prefix of Google specific attributes */
    private static final String PREFIX_GOOGLE = "google";

    /** Prefix of custom OIDC specific attributes */
    private static final String PREFIX_OIDC = "oidc";

    /** Must be serializable in order for Wicket to work */
    @FunctionalInterface
    private interface VisibleSupplier extends Supplier<Boolean>, Serializable {}

    /**
     * If they have chosen MSGraphAPI as the RoleProvider, we need to make sure that the userinfo
     * endpoint is also an MS Graph URL. If not, they've probably made a misconfiguration - the
     * bearer token is from another IDP and this will cause issues access the MS graph endpoint.
     * Let's fail early.
     */
    class MSGraphRoleProviderOnlyWithMSGraphSystem extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return null;
        }

        @Override
        public void validate(Form<?> form) {
            DropDownChoice roleSource = (DropDownChoice) form.get("panel").get("roleSource");
            if (roleSource == null) {
                return;
            }
            if (!MSGraphAPI.equals(roleSource.getConvertedInput())) {
                return;
            }

            // TODO AW
            return;
            //            TextField userInfoTextField = (TextField)
            // form.get("panel").get("oidcUserInfoUri");
            //
            //            String userInfoEndpointUrl = (String)
            // userInfoTextField.getConvertedInput();
            //
            //            if (!userInfoEndpointUrl.startsWith("https://graph.microsoft.com/")) {
            //
            // form.error(form.getString("OAuth2LoginAuthProviderPanel.invalidMSGraphURL"));
            //            }
        }
    }

    private class DiscoveryPanel extends Panel {

        public DiscoveryPanel(String panelId) {
            super(panelId);

            TextField<String> url =
                    new TextField<>(
                            "oidcDiscoveryUri",
                            new PropertyModel<>(configModel.getObject(), "oidcDiscoveryUri"));
            add(url);
            add(
                    new AjaxButton("discover") {

                        @Override
                        protected void onError(AjaxRequestTarget target, Form<?> form) {
                            onSubmit(target, form);
                        }

                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            url.processInput();
                            discover(url.getInput(), target);
                        }
                    });
            add(new HelpLink("oidcDiscoveryUriKeyHelp", this).setDialog(dialog));
        }

        private void discover(String discoveryURL, AjaxRequestTarget target) {
            GeoServerOAuth2LoginFilterConfig model =
                    (GeoServerOAuth2LoginFilterConfig)
                            OAuth2LoginAuthProviderPanel.this.getForm().getModelObject();
            try {
                new DiscoveryClient(discoveryURL).autofill(model);
                target.add(OAuth2LoginAuthProviderPanel.this);
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            } catch (Exception e) {
                error(new ParamResourceModel("discoveryError", this, e.getMessage()).getString());
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            }
        }
    }

    static class TokenClaimPanel extends Panel {
        public TokenClaimPanel(String id) {
            super(id, new Model<>());
            add(new TextField<String>("tokenRolesClaim").setRequired(true));
        }
    }

    private static final class AjaxCheckboxWithTarget extends AjaxCheckBox {
        /** serialVersionUID */
        private static final long serialVersionUID = -545275711746328975L;

        private Component target;

        /** @param pId */
        public AjaxCheckboxWithTarget(String pId, IModel<Boolean> pModel, Component pTarget) {
            super(pId, pModel);
            target = pTarget;
        }

        @Override
        protected void onUpdate(AjaxRequestTarget pTarget) {
            pTarget.add(target);
        }
    }

    static class ShowHideWebMarkupContainer extends WebMarkupContainer {
        private static final long serialVersionUID = 1L;

        /** @param pId */
        public ShowHideWebMarkupContainer(String pId, Supplier<Boolean> pVisibitySupplier) {
            super(pId);
            visibleSupplier = pVisibitySupplier;
        }

        private Supplier<Boolean> visibleSupplier;

        @Override
        public boolean isVisible() {
            return visibleSupplier.get();
        }
    }

    private static Logger LOGGER = Logging.getLogger(OAuth2LoginAuthProviderPanel.class);

    private GeoServerDialog dialog;

    @SuppressWarnings("serial")
    public OAuth2LoginAuthProviderPanel(String id, IModel<GeoServerOAuth2LoginFilterConfig> model) {
        super(id, model);
        GeoServerOAuth2LoginFilterConfig config = model.getObject();
        this.dialog = (GeoServerDialog) get("dialog");

        add(new HelpLink("userNameAttributeHelp", this).setDialog(dialog));

        add(new HelpLink("geoserverParametersHelp", this).setDialog(dialog));
        TextField<String> tf = new TextField<String>("baseRedirectUri");
        tf.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget pTarget) {
                        configModel.getObject().calculateRedirectUris();
                        pTarget.add(OAuth2LoginAuthProviderPanel.this);
                    }
                });

        add(tf);
        add(new HelpLink("baseRedirectUriHelp", this).setDialog(dialog));

        RepeatingView prefixView = new RepeatingView("prefixView");
        add(prefixView);

        VisibleSupplier isGoogleVisible = () -> config.isGoogleEnabled();
        VisibleSupplier isGitHubVisible = () -> config.isGitHubEnabled();
        VisibleSupplier isMsVisible = () -> config.isMsEnabled();
        VisibleSupplier isOidcVisible = () -> config.isOidcEnabled();
        addProviderComponents(prefixView, PREFIX_GOOGLE, "Google", isGoogleVisible);
        addProviderComponents(prefixView, PREFIX_GIT_HUB, "GitHub", isGitHubVisible);
        addProviderComponents(prefixView, PREFIX_MS, "Microsoft Azure", isMsVisible);
        addProviderComponents(prefixView, PREFIX_OIDC, "OpenID Connect Provider", isOidcVisible);

        add(new HelpLink("enableRedirectAuthenticationEntryPointHelp", this).setDialog(dialog));
        add(new CheckBox("enableRedirectAuthenticationEntryPoint"));

        add(new HelpLink("connectionParametersHelp", this).setDialog(dialog));

        add(new HelpLink("postLogoutRedirectUriHelp", this).setDialog(dialog));
        add(new TextField<String>("postLogoutRedirectUri"));
    }

    private void addProviderComponents(
            RepeatingView pView,
            String pProviderKey,
            String pProviderLabel,
            Supplier<Boolean> pShowConfig) {
        OAuth2LoginAuthProviderPanel lMainPanel = OAuth2LoginAuthProviderPanel.this;
        WebMarkupContainer lContainer = new WebMarkupContainer(pView.newChildId());
        pView.add(lContainer);

        lContainer.add(createLabelResourceWithParams("providerHeadline", pProviderLabel));

        IModel<Boolean> lModel =
                new PropertyModel<>(configModel.getObject(), pProviderKey + "Enabled");
        AjaxCheckBox cb = new AjaxCheckboxWithTarget("enabled", lModel, lMainPanel);
        lContainer.add(cb);

        WebMarkupContainer lSHContainer = new ShowHideWebMarkupContainer("settings", pShowConfig);
        lContainer.add(lSHContainer);

        lSHContainer.add(createLabelResourceWithParams("infoFromProvider", pProviderLabel));
        lSHContainer.add(createLabelResourceWithParams("infoForProvider", pProviderLabel));
        lSHContainer.add(new HelpLink("connectionFromParametersHelp", this).setDialog(dialog));
        lSHContainer.add(createTextField("clientId", pProviderKey));
        lSHContainer.add(new HelpLink("clientIdHelp", this).setDialog(dialog));
        lSHContainer.add(createTextField("clientSecret", pProviderKey));
        lSHContainer.add(new HelpLink("clientSecretHelp", this).setDialog(dialog));
        lSHContainer.add(createTextField("userNameAttribute", pProviderKey));
        lSHContainer.add(new HelpLink("userNameAttributeHelp", this).setDialog(dialog));
        lSHContainer.add(createTextField("redirectUri", pProviderKey, false));

        lSHContainer.add(new HelpLink("connectionForParametersHelp", this).setDialog(dialog));
        lSHContainer.add(new HelpLink("redirectUriHelp", this).setDialog(dialog));

        // -- Provider specifics below --

        boolean lSupportsScope = pProviderKey.equals(PREFIX_MS) || pProviderKey.equals(PREFIX_OIDC);
        WebMarkupContainer lScopeContainer = new WebMarkupContainer("displayOnScopeSupport");
        lSHContainer.add(lScopeContainer);
        if (lSupportsScope) {
            lScopeContainer.add(createTextField("scopes", pProviderKey));
            lScopeContainer.add(new HelpLink("scopesHelp", this).setDialog(dialog));
        } else {
            lScopeContainer.setVisible(false);
        }

        boolean lOidc = pProviderKey.equals(PREFIX_OIDC);
        WebMarkupContainer lOidcContainer = new WebMarkupContainer("displayOnOidc");
        lSHContainer.add(lOidcContainer);
        if (lOidc) {
            lOidcContainer.add(new DiscoveryPanel("topPanel"));
            lOidcContainer.add(new HelpLink("oidcTokenUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new HelpLink("oidcAuthorizationUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new HelpLink("oidcUserInfoUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcForceAuthorizationUriHttps"));
            lOidcContainer.add(new CheckBox("oidcForceTokenUriHttps"));
            lOidcContainer.add(new TextField<String>("oidcTokenUri"));
            lOidcContainer.add(new TextField<String>("oidcAuthorizationUri"));
            lOidcContainer.add(new TextField<String>("oidcUserInfoUri"));
            lOidcContainer.add(new HelpLink("oidcJwkSetUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new TextField<String>("oidcJwkSetUri"));
            lOidcContainer.add(new HelpLink("oidcResponseModeHelp", this).setDialog(dialog));
            lOidcContainer.add(new TextField<String>("oidcResponseMode"));
            lOidcContainer.add(
                    new HelpLink("oidcEnforceTokenValidationHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcEnforceTokenValidation"));

            lOidcContainer.add(
                    new HelpLink("oidcAuthenticationMethodPostSecretHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcAuthenticationMethodPostSecret"));

            lOidcContainer.add(new HelpLink("oidcUsePKCEHelp", this).setDialog(dialog));
            lOidcContainer.add(new CheckBox("oidcUsePKCE"));

            lOidcContainer.add(new HelpLink("oidcLogoutUriHelp", this).setDialog(dialog));
            lOidcContainer.add(new TextField<String>("oidcLogoutUri"));

            lOidcContainer.add(new HelpLink("oidcAdvancedSettingsHelp", this).setDialog(dialog));
            lOidcContainer.add(new HelpLink("oidcProviderSettingsHelp", this).setDialog(dialog));

        } else {
            lOidcContainer.setVisible(false);
        }
    }

    /**
     * @param pKey
     * @param pParams
     * @return a {@link Label} with {@link StringResourceModel} and parameters set
     */
    private Label createLabelResourceWithParams(String pKey, Object... pParams) {
        StringResourceModel lModel = new StringResourceModel(pKey);
        lModel.setParameters(pParams);
        Label lLabel = new Label(pKey, lModel);
        return lLabel;
    }

    private TextField<String> createTextField(String pFieldName, String pProviderName) {
        return createTextField(pFieldName, pProviderName, true);
    }

    private TextField<String> createTextField(String pAttr, String pProvider, boolean pEnabled) {
        String lModelField = pProvider + StringUtils.capitalize(pAttr);
        IModel<String> lModel = new PropertyModel<>(configModel.getObject(), lModelField);
        TextField<String> lTextField = new TextField<>(pAttr, lModel);
        lTextField.setEnabled(pEnabled);
        return lTextField;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getForm().add(new MSGraphRoleProviderOnlyWithMSGraphSystem());
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        if (IdToken.equals(model) || AccessToken.equals(model) || UserInfo.equals(model)) {
            return new TokenClaimPanel("panel");
        }
        return super.getRoleSourcePanel(model);
    }

    @Override
    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        List<RoleSource> sources = new ArrayList<>(Arrays.asList(OpenIdRoleSource.values()));
        sources.addAll(Arrays.asList(PreAuthenticatedUserNameRoleSource.values()));
        return new DropDownChoice<>("roleSource", sources, new RoleSourceChoiceRenderer());
    }
}
