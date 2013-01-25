/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.GetLegendGraphic;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.type.FeatureType;

/**
 * Tets the functioning of the abstract legend producer for raster formats, which relies on
 * Geotools' StyledShapePainter.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class AbstractLegendGraphicOutputFormatTest extends WMSTestSupport {

    private static final Logger LOGGER = Logging
            .getLogger(AbstractLegendGraphicOutputFormatTest.class);

    private BufferedImageLegendGraphicBuilder legendProducer;

    GetLegendGraphic service;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new AbstractLegendGraphicOutputFormatTest());
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        dataDirectory.addCoverage(new QName("http://www.geo-solutions.it", "world", "gs"),
                MockData.class.getResource("world.tiff"), "tiff", "raster");
        dataDirectory.addStyle("rainfall", MockData.class.getResource("rainfall.sld"));
        dataDirectory.addStyle("rainfall_ramp", MockData.class.getResource("rainfall_ramp.sld"));
        dataDirectory.addStyle("rainfall_classes",
                MockData.class.getResource("rainfall_classes.sld"));
    }

    @Override
    public void setUpInternal() throws Exception {
        super.setUpInternal();
        this.legendProducer = new BufferedImageLegendGraphicBuilder() {

            public String getContentType() {
                return "image/png";
            }

        };

        service = new GetLegendGraphic(getWMS());
    }

    @Override
    public void tearDownInternal() throws Exception {
        this.legendProducer = null;
        super.tearDownInternal();
    }

    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle
     * has more than one rule, and one of them is requested by the RULE parameter.
     */
    public void testUserSpecifiedRule() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName(
                MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        assertNotNull(multipleRulesStyle);

        Rule rule = multipleRulesStyle.getFeatureTypeStyles()[0].getRules()[0];
        LOGGER.info("testing single rule " + rule.getName() + " from style "
                + multipleRulesStyle.getName());

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(
                MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        req.setLayer(ftInfo.getFeatureType());
        req.setStyle(multipleRulesStyle);
        req.setRule(rule.getName());
        req.setLegendOptions(new HashMap());

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testUserSpecifiedRule", image, LegendUtils.DEFAULT_BG_COLOR);

        // was created only one rule?
        String errMsg = "expected just one legend of height " + HEIGHT_HINT + ", for the rule "
                + rule.getName();
        int resultLegendCount = image.getHeight() / HEIGHT_HINT;
        assertEquals(errMsg, 1, resultLegendCount);
    }

    /**
     * Tests that a legend is produced for the explicitly specified rule, when the FeatureTypeStyle
     * has more than one rule, and one of them is requested by the RULE parameter.
     * 
     */
    public void testRainfall() throws Exception {
        // load a style with 3 rules
        Style multipleRulesStyle = getCatalog().getStyleByName("rainfall").getStyle();

        assertNotNull(multipleRulesStyle);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        CoverageInfo cInfo = getCatalog().getCoverageByName("world");
        assertNotNull(cInfo);

        GridCoverage coverage = cInfo.getGridCoverage(null, null);
        SimpleFeatureCollection feature;
        feature = FeatureUtilities.wrapGridCoverage((GridCoverage2D) coverage);
        req.setLayer(feature.getSchema());
        req.setStyle(multipleRulesStyle);
        req.setLegendOptions(new HashMap());

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

    }

    /**
     * Tests that the legend graphic is still produced when the request's strict parameter is set to
     * false and a layer is not specified
     */
    public void testNoLayerProvidedAndNonStrictRequest() throws Exception {
        Style style = getCatalog().getStyleByName("rainfall").getStyle();
        assertNotNull(style);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setStrict(false);
        req.setLayer(null);
        req.setStyle(style);

        final int HEIGHT_HINT = 30;
        req.setHeight(HEIGHT_HINT);

        // use default values for the rest of parameters
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

        // was the legend painted?
        assertNotBlank("testRainfall", image, LegendUtils.DEFAULT_BG_COLOR);

    }
    
    /**
     * Tests that the legend graphic is produced for multiple layers
     */    
    public void testMultipleLayers() throws Exception {              
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        
        int titleHeight = getTitleHeight(req);
        
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(
                MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers=new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());
        
        req.setLayers(layers);
        
        List<Style> styles=new ArrayList<Style>();
        styles.add(getCatalog().getStyleByName(
                MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());
        req.setStyles(styles);
        
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testMultipleLayers", image, LegendUtils.DEFAULT_BG_COLOR);
        int height=image.getHeight();
        
        layers.add(ftInfo.getFeatureType());
        styles.add(getCatalog().getStyleByName(
                MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());
        this.legendProducer.buildLegendGraphic(req);

        image = this.legendProducer.buildLegendGraphic(req);        
        
        // was the legend painted?
        assertNotBlank("testMultipleLayers", image, LegendUtils.DEFAULT_BG_COLOR);
        // with 2 layers we should have a legend at least 2 times taller (title + 2 layers)
        
        assertEquals(2*(height+titleHeight),image.getHeight());
        
        // first title
        assertPixel(image, 1, titleHeight/2, new Color(0,0,0));
        
        // first layer
        assertPixel(image, 10, 10+titleHeight, new Color(192,160,0));
        
        assertPixel(image, 10, 30+titleHeight, new Color(0,0,0));
        
        assertPixel(image, 10, 50+titleHeight, new Color(224,64,0));
        
        // second title
        assertPixel(image, 1, 60+titleHeight+titleHeight/2, new Color(0,0,0));
        
        // same colors for the second layer
        assertPixel(image, 10, 70+titleHeight*2, new Color(192,160,0));
        
        assertPixel(image, 10, 90+titleHeight*2, new Color(0,0,0));
        
        assertPixel(image, 10, 110+titleHeight*2, new Color(224,64,0));
        
    }
    
    /**
     * Tests that with forceTitles option off no title is rendered
     */
    public void testForceTitlesOff() throws Exception {        
        
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        Map<String,String> options = new HashMap<String,String>();
        options.put("forceTitles", "off");
        req.setLegendOptions(options);
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(
                MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers=new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());
        
        req.setLayers(layers);
        
        List<Style> styles = new ArrayList<Style>();
        styles.add(getCatalog().getStyleByName(
                MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());
        req.setStyles(styles);
        
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        // was the legend painted?
        assertNotBlank("testMultipleLayers", image, LegendUtils.DEFAULT_BG_COLOR);
        int height=image.getHeight();
        
        layers.add(ftInfo.getFeatureType());
        styles.add(getCatalog().getStyleByName(
                MockData.ROAD_SEGMENTS.getLocalPart()).getStyle());
        this.legendProducer.buildLegendGraphic(req);

        image = this.legendProducer.buildLegendGraphic(req);        
        
        // was the legend painted?
        assertNotBlank("testForceTitlesOff", image, LegendUtils.DEFAULT_BG_COLOR);
        
        
        assertEquals(2*height,image.getHeight());
                
        // first layer
        assertPixel(image, 10, 10, new Color(192,160,0));
        
        assertPixel(image, 10, 30, new Color(0,0,0));
        
        assertPixel(image, 10, 50, new Color(224,64,0));
                
        // same colors for the second layer
        assertPixel(image, 10, 70, new Color(192,160,0));
        
        assertPixel(image, 10, 90, new Color(0,0,0));
        
        assertPixel(image, 10, 110, new Color(224,64,0));
        
    }
    
    /**
     * Tests that the legend graphic is produced for multiple layers
     * with different style for each layer.
     */    
    public void testMultipleLayersWithDifferentStyles() throws Exception {        
        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        
        int titleHeight = getTitleHeight(req);
        
        FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(
                MockData.ROAD_SEGMENTS.getNamespaceURI(), MockData.ROAD_SEGMENTS.getLocalPart());
        List<FeatureType> layers=new ArrayList<FeatureType>();
        layers.add(ftInfo.getFeatureType());
        layers.add(ftInfo.getFeatureType());
        req.setLayers(layers);
        
        List<Style> styles=new ArrayList<Style>();
        Style style1= getCatalog().getStyleByName(
                MockData.ROAD_SEGMENTS.getLocalPart()).getStyle();
        styles.add(style1);
        
        Style style2= getCatalog().getStyleByName(
                MockData.LAKES.getLocalPart()).getStyle();
        styles.add(style2);
        req.setStyles(styles);
        
        this.legendProducer.buildLegendGraphic(req);

        BufferedImage image = this.legendProducer.buildLegendGraphic(req);

        
        // first layer
        assertPixel(image, 10, 10+titleHeight, new Color(192,160,0));
        
        assertPixel(image, 10, 30+titleHeight, new Color(0,0,0));
        
        assertPixel(image, 10, 50+titleHeight, new Color(224,64,0));
        
        // different color (style) for the second layer
        assertPixel(image, 10, 70+titleHeight*2, new Color(64,64,192));

    }
    
    private int getTitleHeight(GetLegendGraphicRequest req) {    
        final BufferedImage image = ImageUtils.createImage(req.getWidth(),
                req.getHeight(), (IndexColorModel) null, req.isTransparent());
        return getRenderedLabel(image, "TESTTITLE", req).getHeight();
    }
    
    private BufferedImage getRenderedLabel(BufferedImage image, String label,
            GetLegendGraphicRequest request) {
        Font labelFont = LegendUtils.getLabelFont(request);
        boolean useAA = LegendUtils.isFontAntiAliasing(request);
    
        final Graphics2D graphics = image.createGraphics();
        graphics.setFont(labelFont);
        if (useAA) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        return LegendUtils.renderLabel(label, graphics, request);
    }
}
