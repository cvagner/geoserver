/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIEXTInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.JAIInfo.PngEncoderType;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.image.ImageWorker;

import com.sun.media.imageioimpl.common.PackageUtil;

/**
 * Edits the JAI configuration parameters
 */
public class JAIPage extends ServerAdminPage {
    private static final long serialVersionUID = -1184717232184497578L;

    public JAIPage(){
        final IModel geoServerModel = getGeoServerModel();
        
        // this invokation will trigger a clone of the JAIInfo
        // which will allow the modification proxy seeing changes on the
        // Jai page with respect to the original JAIInfo object
        final IModel jaiModel = getJAIModel();

        // form and submit
        Form form = new Form("form", new CompoundPropertyModel(jaiModel));
        add( form );

        // All the fields
        // ... memory capacity and threshold are percentages
        RangeValidator<Integer> percentageValidator = RangeValidator.range(0, 1);
        TextField memoryCapacity = new TextField("memoryCapacity");
        memoryCapacity.add(percentageValidator);
        form.add(memoryCapacity);
        TextField memoryThreshold = new TextField("memoryThreshold");
        memoryThreshold.add(percentageValidator);
        form.add(memoryThreshold);
        TextField tileThreads = new TextField("tileThreads");
        tileThreads.add(RangeValidator.minimum(0));
        form.add(tileThreads);
        TextField tilePriority = new TextField("tilePriority");
        tilePriority.add(RangeValidator.minimum(0));
        form.add(tilePriority);
        form.add(new CheckBox("recycling"));
        form.add(new CheckBox("jpegAcceleration"));
        addPngEncoderEditor(form);
        CheckBox checkBoxMosaic = new CheckBox("allowNativeMosaic");
        CheckBox checkBoxWarp = new CheckBox("allowNativeWarp");
        JAIInfo info = (JAIInfo)jaiModel.getObject();
        JAIEXTInfo je = null;
        boolean isJAIExtEnabled = ImageWorker.isJaiExtEnabled(); 
        if (isJAIExtEnabled) {
            je = info.getJAIEXTInfo();
        }
        boolean mosaicEnabled = je != null && !je.getJAIEXTOperations().contains("Mosaic");
        boolean warpEnabled = je != null && !je.getJAIEXTOperations().contains("Warp");
        checkBoxMosaic.setEnabled(mosaicEnabled);
        checkBoxWarp.setEnabled(warpEnabled);
        form.add(checkBoxMosaic);
        form.add(checkBoxWarp);
        JAIEXTPanel jaiExtPanel = new JAIEXTPanel("jaiext", jaiModel);
        if (!isJAIExtEnabled) {
            jaiExtPanel.setVisible(false);
        }
        form.add(jaiExtPanel);

        Button submit = new Button("submit", new StringResourceModel("submit", this, null)) {
            @Override
            public void onSubmit() {
                GeoServer gs = (GeoServer) geoServerModel.getObject();
                GeoServerInfo global = gs.getGlobal();
                global.setJAI( (JAIInfo)jaiModel.getObject());
                gs.save( global );
                doReturn();
            }
        };
        form.add(submit);
        
        Button cancel = new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
    }

    private void addPngEncoderEditor(Form form) {
        // get the list of available encoders
        List<PngEncoderType> encoders = new ArrayList(Arrays.asList(JAIInfo.PngEncoderType.values()));
        if(!PackageUtil.isCodecLibAvailable()) {
            encoders.remove(PngEncoderType.NATIVE);
        }
        // create the editor, eventually set a default value
        DropDownChoice<JAIInfo.PngEncoderType> editor = new DropDownChoice<JAIInfo.PngEncoderType>(
                "pngEncoderType", encoders, new IChoiceRenderer<JAIInfo.PngEncoderType>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(PngEncoderType type) {
                        return new ParamResourceModel("pngEncoder." + type.name(), JAIPage.this)
                                .getString();
                    }

                    @Override
                    public String getIdValue(PngEncoderType type, int index) {
                        return type.name();
                    }

                    @Override
                    public PngEncoderType getObject(String id,
                            IModel<? extends List<? extends PngEncoderType>> choices) {
                        return PngEncoderType.valueOf(id);
                    }
                });
        form.add(editor);
        if(!encoders.contains(editor.getModelObject())) {
            editor.setModelObject(PngEncoderType.PNGJ);
        }
    }

}
