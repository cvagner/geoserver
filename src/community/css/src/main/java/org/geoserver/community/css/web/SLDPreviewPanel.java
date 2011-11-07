package org.geoserver.community.css.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class SLDPreviewPanel extends Panel {
    private Label _label;

    public SLDPreviewPanel(String id, IModel<String> model) {
        super(id, model);
        _label = new Label("sld-preview", model);
        _label.setOutputMarkupId(true);
        add(_label);
    }

    public Label getLabel() {
        return _label;
    }
}
