/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.wicket.SimpleChoiceRenderer;

public class OutputTypesFormComponent extends FormComponentPanel<String> {

    /** the palette */
    protected Palette<String> palette;

    protected AjaxCheckBox allOutputTypesCheckBox;

    /** list of behaviors to add, staged before the palette recorder component is created */
    List<Behavior> toAdd = new ArrayList<>();

    public OutputTypesFormComponent(
            String id,
            IModel<List<String>> model,
            IModel<Collection<String>> choicesModel,
            final boolean isOutputTypeCheckingEnabled) {
        super(id, new Model<>());

        add(
                new AjaxCheckBox(
                        "outputTypeCheckingEnabled", new Model<>(isOutputTypeCheckingEnabled)) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        toggleVisibility(getModelObject());
                        target.add(palette);
                    }
                });

        add(
                palette =
                        new Palette<>(
                                "palette",
                                model,
                                choicesModel,
                                new SimpleChoiceRenderer<>(),
                                10,
                                false) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected Recorder<String> newRecorderComponent() {
                                Recorder<String> rec = super.newRecorderComponent();

                                // add any behaviors that need to be added
                                rec.add(toAdd.toArray(new Behavior[toAdd.size()]));
                                toAdd.clear();
                                return rec;
                            }

                            // Override otherwise the header is not i18n'ized
                            @Override
                            public Component newSelectedHeader(final String componentId) {
                                return new Label(
                                        componentId,
                                        new ResourceModel(getSelectedHeaderPropertyKey()));
                            }

                            // Override otherwise the header is not i18n'ized
                            @Override
                            public Component newAvailableHeader(final String componentId) {
                                return new Label(
                                        componentId,
                                        new ResourceModel(getAvaliableHeaderPropertyKey()));
                            }
                        });
        palette.add(new DefaultTheme());
        palette.setOutputMarkupPlaceholderTag(true);
        toggleVisibility(isOutputTypeCheckingEnabled);
    }

    void toggleVisibility(boolean visible) {
        palette.setVisible(visible);
        if (visible == false) palette.getModelCollection().clear();
    }

    public void setOutputTypeCheckingEnabled(boolean enabled) {
        get("outputTypeCheckingEnabled").setDefaultModelObject(enabled);
        toggleVisibility(enabled);
    }

    public boolean isOutputTypeCheckingEnabled() {
        return (Boolean) get("outputTypeCheckingEnabled").getDefaultModelObject();
    }

    /**
     * @return the default key, subclasses may override, if "Selected" is not illustrative enough
     */
    protected String getSelectedHeaderPropertyKey() {
        return "OutputTypesFormComponent.selectedHeader";
    }

    /**
     * @return the default key, subclasses may override, if "Available" is not illustrative enough
     */
    protected String getAvaliableHeaderPropertyKey() {
        return "OutputTypesFormComponent.availableHeader";
    }

    @Override
    public Component add(Behavior... behaviors) {
        if (palette.getRecorderComponent() == null) {
            // stage for them for later
            toAdd.addAll(Arrays.asList(behaviors));
        } else {
            // add them now
            palette.getRecorderComponent().add(behaviors);
        }
        return this;
    }

    public Palette<String> getPalette() {
        return palette;
    }

    public IModel<Collection<String>> getPaletteModel() {
        return palette.getModel();
    }

    @Override
    public void updateModel() {
        super.updateModel();
        if (palette.getRecorderComponent() != null) palette.getRecorderComponent().updateModel();
    }
}
