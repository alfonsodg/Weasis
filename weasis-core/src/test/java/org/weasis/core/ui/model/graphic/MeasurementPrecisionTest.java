/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.model.graphic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.weasis.core.api.image.measure.MeasurementsAdapter;
import org.weasis.core.api.image.util.MeasurableLayer;
import org.weasis.core.api.image.util.Unit;
import org.weasis.core.ui.model.graphic.imp.angle.AngleToolGraphic;
import org.weasis.core.ui.model.graphic.imp.area.EllipseGraphic;
import org.weasis.core.ui.model.graphic.imp.area.PolygonGraphic;
import org.weasis.core.ui.model.graphic.imp.line.LineGraphic;
import org.weasis.core.ui.model.utils.bean.MeasureItem;

class MeasurementPrecisionTest {

    private MeasurableLayer mockLayer;
    private MeasurementsAdapter pixelAdapter;
    private MeasurementsAdapter mmAdapter;
    private MeasurementsAdapter halfMmAdapter;

    @BeforeEach
    void setUp() {
        mockLayer = mock(MeasurableLayer.class);
        pixelAdapter = new MeasurementsAdapter(1.0, 0, 0, false, 512, "px");
        mmAdapter = new MeasurementsAdapter(0.5, 0, 0, false, 512, "mm");
        halfMmAdapter = new MeasurementsAdapter(0.25, 0, 0, false, 512, "mm");
        when(mockLayer.hasContent()).thenReturn(true);
    }

    // ========================================================================
    // LineGraphic Precision Tests
    // ========================================================================

    static Stream<Arguments> lineDistanceSource() {
        return Stream.of(
            Arguments.of(0.0, 0.0, 3.0, 4.0, 5.0),
            Arguments.of(0.0, 0.0, 1.0, 1.0, Math.sqrt(2)),
            Arguments.of(0.0, 0.0, 0.0, 5.0, 5.0),
            Arguments.of(0.0, 0.0, 5.0, 0.0, 5.0),
            Arguments.of(-3.0, -4.0, 0.0, 0.0, 5.0),
            Arguments.of(1.0, 2.0, 4.0, 6.0, 5.0),
            Arguments.of(0.0, 0.0, 100.0, 0.0, 100.0),
            Arguments.of(0.5, 0.5, 1.5, 1.5, Math.sqrt(2)),
            Arguments.of(-5.0, -5.0, 5.0, 5.0, Math.sqrt(200)),
            Arguments.of(1e6, 0.0, 1e6, 100.0, 100.0)
        );
    }

    @ParameterizedTest
    @MethodSource("lineDistanceSource")
    void testLineDistancePrecision(
        double x1, double y1, double x2, double y2, double expectedDist) {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        LineGraphic line = createLine(x1, y1, x2, y2);

        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem lengthItem = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.LINE_LENGTH)
            .findFirst()
            .orElseThrow(() -> new AssertionError("LINE_LENGTH not found"));

        assertEquals(expectedDist, (Double) lengthItem.getValue(), 0.1);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 3, 4, 2.5",
        "0, 0, 0, 10, 5.0",
        "0, 0, 4, 0, 2.0",
        "0, 0, 1, 1, 0.707106781"
    })
    void testLineDistanceCalibrated(
        double x1, double y1, double x2, double y2, double expectedDist) {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(mmAdapter);
        LineGraphic line = createLine(x1, y1, x2, y2);

        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.MILLIMETER);

        MeasureItem lengthItem = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.LINE_LENGTH)
            .findFirst()
            .orElseThrow(() -> new AssertionError("LINE_LENGTH not found"));

        assertEquals(expectedDist, (Double) lengthItem.getValue(), 0.1);
    }

    @Test
    void testLineDistanceWithOffset() {
        MeasurementsAdapter offsetAdapter =
            new MeasurementsAdapter(1.0, 10, 20, false, 512, "px");
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(offsetAdapter);
        LineGraphic line = createLine(0, 0, 3, 4);

        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem lengthItem = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.LINE_LENGTH)
            .findFirst()
            .orElseThrow(() -> new AssertionError("LINE_LENGTH not found"));

        assertEquals(5.0, (Double) lengthItem.getValue(), 0.1,
            "Distance should be independent of offset");
    }

    @Test
    void testLineCoordinateValues() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        LineGraphic line = createLine(10.0, 20.0, 30.0, 40.0);

        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem firstX = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.FIRST_POINT_X)
            .findFirst()
            .orElseThrow(() -> new AssertionError("FIRST_POINT_X not found"));
        MeasureItem firstY = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.FIRST_POINT_Y)
            .findFirst()
            .orElseThrow(() -> new AssertionError("FIRST_POINT_Y not found"));
        MeasureItem lastX = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.LAST_POINT_X)
            .findFirst()
            .orElseThrow(() -> new AssertionError("LAST_POINT_X not found"));
        MeasureItem lastY = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.LAST_POINT_Y)
            .findFirst()
            .orElseThrow(() -> new AssertionError("LAST_POINT_Y not found"));

        assertEquals(10.0, (Double) firstX.getValue(), 0.001);
        assertEquals(20.0, (Double) firstY.getValue(), 0.001);
        assertEquals(30.0, (Double) lastX.getValue(), 0.001);
        assertEquals(40.0, (Double) lastY.getValue(), 0.001);
    }

    @Test
    void testLineCoordinateValuesWithOffset() {
        MeasurementsAdapter offsetAdapter =
            new MeasurementsAdapter(1.0, 5, 10, false, 512, "px");
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(offsetAdapter);
        LineGraphic line = createLine(10.0, 20.0, 30.0, 40.0);

        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem firstX = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.FIRST_POINT_X)
            .findFirst()
            .orElseThrow(() -> new AssertionError("FIRST_POINT_X not found"));

        assertEquals(15.0, (Double) firstX.getValue(), 0.001,
            "X coordinate should include offset");
    }

    @Test
    void testLineCoordinateValuesWithCalibration() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(mmAdapter);
        LineGraphic line = createLine(10.0, 20.0, 30.0, 40.0);

        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.MILLIMETER);

        MeasureItem firstX = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.FIRST_POINT_X)
            .findFirst()
            .orElseThrow(() -> new AssertionError("FIRST_POINT_X not found"));

        assertEquals(5.0, (Double) firstX.getValue(), 0.001,
            "Calibrated X coordinate should be pixel * ratio");
    }

    @Test
    void testLineOrientation() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        // Horizontal line: orientation should be 0 or 180
        LineGraphic horizontal = createLine(0, 0, 5, 0);
        List<MeasureItem> items = horizontal.computeMeasurements(mockLayer, true, Unit.PIXEL);
        MeasureItem orientation = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.ORIENTATION)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ORIENTATION not found"));
        assertEquals(0.0, (Double) orientation.getValue(), 0.01);
    }

    @Test
    void testLineAzimuth() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        // Vertical line going up: azimuth should be 0
        LineGraphic vertical = createLine(0, 5, 0, 0);
        List<MeasureItem> items = vertical.computeMeasurements(mockLayer, true, Unit.PIXEL);
        MeasureItem azimuth = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.AZIMUTH)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AZIMUTH not found"));
        assertEquals(0.0, (Double) azimuth.getValue(), 0.01);
    }

    @Test
    void testLineZeroLengthReturnsEmpty() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        LineGraphic line = new LineGraphic();
        line.setPts(Arrays.asList(new Point2D.Double(0, 0), new Point2D.Double(0, 0)));
        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.PIXEL);
        assertTrue(items.isEmpty(), "Zero-length line should return no measurements");
    }

    @Test
    void testLineNegativeCoordinates() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        LineGraphic line = createLine(-10.0, -10.0, -4.0, -2.0);

        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem lengthItem = items.stream()
            .filter(i -> i.getMeasurement() == LineGraphic.LINE_LENGTH)
            .findFirst()
            .orElseThrow(() -> new AssertionError("LINE_LENGTH not found"));

        double expected = Math.sqrt(6.0 * 6.0 + 8.0 * 8.0);
        assertEquals(expected, (Double) lengthItem.getValue(), 0.1);
    }

    @Test
    void testLineWithNullLayerReturnsEmpty() {
        LineGraphic line = createLine(0, 0, 3, 4);
        List<MeasureItem> items = line.computeMeasurements(null, true, Unit.PIXEL);
        assertTrue(items.isEmpty(), "Null layer should return no measurements");
    }

    @Test
    void testLineSinglePointReturnsEmpty() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        LineGraphic line = new LineGraphic();
        line.setPts(Collections.singletonList(new Point2D.Double(0, 0)));
        List<MeasureItem> items =
            line.computeMeasurements(mockLayer, true, Unit.PIXEL);
        assertTrue(items.isEmpty(), "Incomplete line should return no measurements");
    }

    // ========================================================================
    // AngleToolGraphic Precision Tests
    // ========================================================================

    static Stream<Arguments> angleSource() {
        return Stream.of(
            Arguments.of(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 90.0),
            Arguments.of(1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 45.0),
            Arguments.of(1.0, 0.0, 0.0, 0.0, 0.5, Math.sqrt(3) / 2.0, 60.0),
            Arguments.of(1.0, 0.0, 0.0, 0.0, Math.sqrt(3) / 2.0, 0.5, 30.0),
            Arguments.of(1.0, 0.0, 0.0, 0.0, -1.0, 0.0, 180.0),
            Arguments.of(0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 90.0),
            Arguments.of(1.0, 1.0, 0.0, 0.0, -1.0, 1.0, 90.0),
            Arguments.of(1.0, 0.0, 0.0, 0.0, -0.5, Math.sqrt(3) / 2.0, 120.0),
            Arguments.of(2.0, 0.0, 0.0, 0.0, 0.0, 3.0, 90.0)
        );
    }

    @ParameterizedTest
    @MethodSource("angleSource")
    void testAngleValue(double ax, double ay, double ox, double oy,
                        double bx, double by, double expectedAngle) {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        AngleToolGraphic angle = createAngle(ax, ay, ox, oy, bx, by);

        List<MeasureItem> items =
            angle.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem angleItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ANGLE not found"));

        assertEquals(expectedAngle, (Double) angleItem.getValue(), 0.01);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 0, 0, 0, 0, 1, 90.0, 90.0, 270.0",
        "1, 0, 0, 0, 1, 1, 45.0, 135.0, 315.0",
        "1, 0, 0, 0, -1, 0, 180.0, 0.0, 180.0",
        "1, 0, 0, 0, -0.5, 0.866, 120.0, 60.0, 240.0"
    })
    void testAllAngleVariants(double ax, double ay, double ox, double oy,
                              double bx, double by,
                              double expectedAngle, double expectedCompl,
                              double expectedReflex) {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        AngleToolGraphic angle = createAngle(ax, ay, ox, oy, bx, by);

        List<MeasureItem> items =
            angle.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem angleItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ANGLE not found"));
        MeasureItem complItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.COMPLEMENTARY_ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("COMPLEMENTARY_ANGLE not found"));
        MeasureItem reflexItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.REFLEX_ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("REFLEX_ANGLE not found"));

        assertEquals(expectedAngle, (Double) angleItem.getValue(), 0.01);
        assertEquals(expectedCompl, (Double) complItem.getValue(), 0.01);
        assertEquals(expectedReflex, (Double) reflexItem.getValue(), 0.01);
    }

    @Test
    void testAngleUnchangedByCalibration() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(mmAdapter);
        AngleToolGraphic angle = createAngle(1.0, 0.0, 0.0, 0.0, 0.0, 1.0);

        List<MeasureItem> items =
            angle.computeMeasurements(mockLayer, true, Unit.MILLIMETER);

        MeasureItem angleItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ANGLE not found"));

        assertEquals(90.0, (Double) angleItem.getValue(), 0.01,
            "Angle should be unaffected by calibration ratio");
    }

    @Test
    void testAngleUpYAxis() {
        MeasurementsAdapter upAdapter =
            new MeasurementsAdapter(1.0, 0, 0, true, 512, "px");
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(upAdapter);
        AngleToolGraphic angle = createAngle(1.0, 0.0, 0.0, 0.0, 0.0, 1.0);

        List<MeasureItem> items =
            angle.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem angleItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ANGLE not found"));

        assertEquals(90.0, (Double) angleItem.getValue(), 0.01,
            "Angle should be unaffected by Y-axis orientation");
    }

    @Test
    void testAngleZeroDegrees() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        // ptA=(2,0), ptO=(0,0), ptB=(1,0) creates two colinear vectors
        // both pointing along the positive x-axis, angle should be 0
        AngleToolGraphic angle = createAngle(2.0, 0.0, 0.0, 0.0, 1.0, 0.0);

        List<MeasureItem> items =
            angle.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem angleItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ANGLE not found"));

        assertEquals(0.0, (Double) angleItem.getValue(), 0.01);
    }

    @Test
    void testAngleColinearOpposite() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        // ptA=(1,0), ptO=(0,0), ptB=(-1,0) creates colinear vectors
        // in opposite directions, angle should be 180
        AngleToolGraphic angle = createAngle(1.0, 0.0, 0.0, 0.0, -1.0, 0.0);

        List<MeasureItem> items =
            angle.computeMeasurements(mockLayer, true, Unit.PIXEL);

        MeasureItem angleItem = items.stream()
            .filter(i -> i.getMeasurement() == AngleToolGraphic.ANGLE)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ANGLE not found"));

        assertEquals(180.0, (Double) angleItem.getValue(), 0.01);
    }

    @Test
    void testAngleIncompletePointsReturnsEmpty() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        AngleToolGraphic angle = new AngleToolGraphic();
        angle.setPts(Arrays.asList(
            new Point2D.Double(1, 0),
            new Point2D.Double(0, 0)
        ));
        List<MeasureItem> items =
            angle.computeMeasurements(mockLayer, true, Unit.PIXEL);
        assertTrue(items.isEmpty(), "Incomplete angle should return no measurements");
    }

    // ========================================================================
    // EllipseGraphic Precision Tests
    // ========================================================================

    static Stream<Arguments> ellipseAreaSource() {
        return Stream.of(
            // majorAxis, minorAxis, expectedArea (uncalibrated = pi * a*b / 4)
            Arguments.of(10.0, 6.0, Math.PI * 10.0 * 6.0 / 4.0),
            Arguments.of(20.0, 10.0, Math.PI * 20.0 * 10.0 / 4.0),
            Arguments.of(8.0, 8.0, Math.PI * 8.0 * 8.0 / 4.0),
            Arguments.of(100.0, 50.0, Math.PI * 100.0 * 50.0 / 4.0),
            Arguments.of(4.0, 2.0, Math.PI * 4.0 * 2.0 / 4.0)
        );
    }

    @ParameterizedTest
    @MethodSource("ellipseAreaSource")
    void testEllipseAreaPrecision(double majorAxis, double minorAxis, double expectedArea) {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        EllipseGraphic ellipse = createEllipse(majorAxis, minorAxis);

        List<MeasureItem> items =
            ellipse.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        String unit = areaItem.getUnit();
        assertEquals("px", unit);

        double area = (Double) areaItem.getValue();
        assertEquals(expectedArea, area, expectedArea * 0.01,
            "Ellipse area should be within 1% tolerance");
    }

    @Test
    void testEllipseAreaCalibrated() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(halfMmAdapter);
        EllipseGraphic ellipse = createEllipse(10.0, 6.0);

        List<MeasureItem> items =
            ellipse.computeMeasurements(mockLayer, false, Unit.MILLIMETER);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        String unit = areaItem.getUnit();
        assertEquals("mm2", unit);

        double expectedPixels = Math.PI * 10.0 * 6.0 / 4.0;
        double ratio = halfMmAdapter.calibrationRatio();
        double expectedArea = expectedPixels * ratio * ratio;
        double area = (Double) areaItem.getValue();
        assertEquals(expectedArea, area, expectedArea * 0.01,
            "Calibrated ellipse area should scale with ratio squared");
    }

    @ParameterizedTest
    @CsvSource({
        "10, 6, 15.0",
        "20, 10, 30.0",
        "8, 8, 16.0"
    })
    void testEllipseWidthAndHeight(double majorAxis, double minorAxis,
                                   double expectedWidthHeight) {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(mmAdapter);
        EllipseGraphic ellipse = createEllipse(majorAxis, minorAxis);

        List<MeasureItem> items =
            ellipse.computeMeasurements(mockLayer, false, Unit.MILLIMETER);

        MeasureItem widthItem = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.WIDTH)
            .findFirst()
            .orElseThrow(() -> new AssertionError("WIDTH not found"));

        MeasureItem heightItem = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.HEIGHT)
            .findFirst()
            .orElseThrow(() -> new AssertionError("HEIGHT not found"));

        double expectedCalibrated = expectedWidthHeight * mmAdapter.calibrationRatio();
        assertEquals(expectedCalibrated, (Double) widthItem.getValue(), 0.001);
        assertEquals(expectedCalibrated, (Double) heightItem.getValue(), 0.001);
    }

    @Test
    void testEllipsePerimeter() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        EllipseGraphic ellipse = createEllipse(10.0, 6.0);

        List<MeasureItem> items =
            ellipse.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem perimeterItem = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.PERIMETER)
            .findFirst()
            .orElseThrow(() -> new AssertionError("PERIMETER not found"));

        double a = 10.0 / 2.0;
        double b = 6.0 / 2.0;
        double expectedPerimeter = 2.0 * Math.PI * Math.sqrt((a * a + b * b) / 2.0);
        assertEquals(expectedPerimeter, (Double) perimeterItem.getValue(), 0.01);
    }

    @Test
    void testEllipseCircleSpecialCase() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        // Circle is an ellipse with equal major and minor axes
        EllipseGraphic circle = createEllipse(10.0, 10.0);

        List<MeasureItem> items =
            circle.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        double expectedArea = Math.PI * 10.0 * 10.0 / 4.0;
        assertEquals(expectedArea, (Double) areaItem.getValue(), expectedArea * 0.01,
            "Circle area should match pi * r^2");
    }

    @Test
    void testEllipseIncompleteReturnsEmpty() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        EllipseGraphic ellipse = new EllipseGraphic();
        ellipse.setPts(Arrays.asList(
            new Point2D.Double(0, 0),
            new Point2D.Double(10, 0)
        ));
        List<MeasureItem> items =
            ellipse.computeMeasurements(mockLayer, false, Unit.PIXEL);
        assertTrue(items.isEmpty(), "Incomplete ellipse should return no measurements");
    }

    @Test
    void testEllipseCenterCoordinates() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        // Ellipse with center at (10, 15), major axis horizontal, minor vertical
        double cx = 10.0, cy = 15.0;
        double major = 20.0, minor = 10.0;
        EllipseGraphic ellipse = createEllipseAt(cx, cy, major, minor);

        List<MeasureItem> items =
            ellipse.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem centerX = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.CENTER_X)
            .findFirst()
            .orElseThrow(() -> new AssertionError("CENTER_X not found"));
        MeasureItem centerY = items.stream()
            .filter(i -> i.getMeasurement() == EllipseGraphic.CENTER_Y)
            .findFirst()
            .orElseThrow(() -> new AssertionError("CENTER_Y not found"));

        assertEquals(cx, (Double) centerX.getValue(), 0.001);
        assertEquals(cy, (Double) centerY.getValue(), 0.001);
    }

    // ========================================================================
    // PolygonGraphic Precision Tests
    // ========================================================================

    @Test
    void testTriangleArea() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic triangle = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(0, 3)
        );

        List<MeasureItem> items =
            triangle.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        double expectedArea = 6.0;
        assertEquals(expectedArea, (Double) areaItem.getValue(), expectedArea * 0.01);
    }

    @Test
    void testTrianglePerimeter() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic triangle = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(0, 3)
        );

        List<MeasureItem> items =
            triangle.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem perimeterItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.PERIMETER)
            .findFirst()
            .orElseThrow(() -> new AssertionError("PERIMETER not found"));

        double expectedPerimeter = 4.0 + 3.0 + 5.0;
        assertEquals(expectedPerimeter, (Double) perimeterItem.getValue(), 0.001);
    }

    @Test
    void testRectangleArea() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic rect = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(4, 3),
            new Point2D.Double(0, 3)
        );

        List<MeasureItem> items =
            rect.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        assertEquals(12.0, (Double) areaItem.getValue(), 0.001);
    }

    @Test
    void testPolygonAreaCalibrated() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(mmAdapter);
        PolygonGraphic rect = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(4, 3),
            new Point2D.Double(0, 3)
        );

        List<MeasureItem> items =
            rect.computeMeasurements(mockLayer, false, Unit.MILLIMETER);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        double ratio = mmAdapter.calibrationRatio();
        double expectedArea = 12.0 * ratio * ratio;
        assertEquals(expectedArea, (Double) areaItem.getValue(), expectedArea * 0.001);
    }

    @Test
    void testPolygonAreaUnit() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(mmAdapter);
        PolygonGraphic rect = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(4, 3),
            new Point2D.Double(0, 3)
        );

        List<MeasureItem> items =
            rect.computeMeasurements(mockLayer, false, Unit.MILLIMETER);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        assertEquals("mm2", areaItem.getUnit(),
            "Calibrated area unit should be unit^2");
    }

    @Test
    void testPolygonAreaUnitPixel() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic rect = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(4, 3),
            new Point2D.Double(0, 3)
        );

        List<MeasureItem> items =
            rect.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        assertEquals("pix", areaItem.getUnit(),
            "Pixel area unit should remain 'pix'");
    }

    @Test
    void testPolygonCentroid() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic triangle = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(0, 3)
        );

        List<MeasureItem> items =
            triangle.computeMeasurements(mockLayer, false, Unit.PIXEL);

        MeasureItem centroidX = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.CENTROID_X)
            .findFirst()
            .orElseThrow(() -> new AssertionError("CENTROID_X not found"));
        MeasureItem centroidY = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.CENTROID_Y)
            .findFirst()
            .orElseThrow(() -> new AssertionError("CENTROID_Y not found"));

        // Centroid of right triangle (0,0),(4,0),(0,3): average of vertices = (4/3, 1)
        assertEquals(4.0 / 3.0, (Double) centroidX.getValue(), 0.001);
        assertEquals(1.0, (Double) centroidY.getValue(), 0.001);
    }

    @Test
    void testPolygonInvalidThrows() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic invalid = new PolygonGraphic();
        invalid.setPts(Arrays.asList(
            new Point2D.Double(0, 0),
            new Point2D.Double(1, 1)
        ));
        invalid.setPointNumber(2);

        List<MeasureItem> items =
            invalid.computeMeasurements(mockLayer, false, Unit.PIXEL);
        assertTrue(items.isEmpty(), "Invalid polygon with < 3 points should return empty");
    }

    @Test
    void testPolygonNegativeCoordinates() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic triangle = createPolygon(
            new Point2D.Double(-3, -2),
            new Point2D.Double(1, -2),
            new Point2D.Double(-3, 2)
        );

        List<MeasureItem> items =
            triangle.computeMeasurements(mockLayer, false, Unit.PIXEL);

        // Right triangle: base=4, height=4, area=8
        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        assertEquals(8.0, (Double) areaItem.getValue(), 0.001);
    }

    @Test
    void testPolygonLargeCoordinates() {
        when(mockLayer.getMeasurementAdapter(any())).thenReturn(pixelAdapter);
        PolygonGraphic triangle = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(10000, 0),
            new Point2D.Double(0, 10000)
        );

        List<MeasureItem> items =
            triangle.computeMeasurements(mockLayer, false, Unit.PIXEL);

        double expectedArea = 10000.0 * 10000.0 / 2.0;
        MeasureItem areaItem = items.stream()
            .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AREA not found"));

        assertEquals(expectedArea, (Double) areaItem.getValue(), expectedArea * 0.01);
    }

    // ========================================================================
    // Calibration Ratio Precision Tests
    // ========================================================================

    @Test
    void testLineLengthScalesLinearlyWithCalibration() {
        double[] ratios = {0.1, 0.5, 1.0, 2.0, 10.0};
        double pixelDist = 10.0;
        Point2D p1 = new Point2D.Double(0, 0);
        Point2D p2 = new Point2D.Double(10, 0);

        for (double ratio : ratios) {
            MeasurementsAdapter adapter =
                new MeasurementsAdapter(ratio, 0, 0, false, 512, "cm");
            when(mockLayer.getMeasurementAdapter(any())).thenReturn(adapter);
            LineGraphic line = createLine(0, 0, 10, 0);

            List<MeasureItem> items =
                line.computeMeasurements(mockLayer, true, Unit.CENTIMETER);

            MeasureItem lengthItem = items.stream()
                .filter(i -> i.getMeasurement() == LineGraphic.LINE_LENGTH)
                .findFirst()
                .orElseThrow(() -> new AssertionError("LINE_LENGTH not found"));

            assertEquals(pixelDist * ratio, (Double) lengthItem.getValue(), 0.001,
                "Line length should scale linearly with calibration ratio");
        }
    }

    @Test
    void testAreaScalesQuadraticallyWithCalibration() {
        double[] ratios = {0.1, 0.5, 1.0, 2.0};
        double pixelArea = 12.0;
        PolygonGraphic rect = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(4, 3),
            new Point2D.Double(0, 3)
        );

        for (double ratio : ratios) {
            MeasurementsAdapter adapter =
                new MeasurementsAdapter(ratio, 0, 0, false, 512, "cm");
            when(mockLayer.getMeasurementAdapter(any())).thenReturn(adapter);
            rect.buildShape(null);

            List<MeasureItem> items =
                rect.computeMeasurements(mockLayer, false, Unit.CENTIMETER);

            MeasureItem areaItem = items.stream()
                .filter(i -> i.getMeasurement() == PolygonGraphic.AREA)
                .findFirst()
                .orElseThrow(() -> new AssertionError("AREA not found"));

            assertEquals(pixelArea * ratio * ratio,
                (Double) areaItem.getValue(), pixelArea * ratio * ratio * 0.001,
                "Area should scale quadratically with calibration ratio");
        }
    }

    @Test
    void testAngleUnaffectedByCalibrationRatio() {
        double[] ratios = {0.1, 0.5, 1.0, 2.0, 10.0};

        for (double ratio : ratios) {
            MeasurementsAdapter adapter =
                new MeasurementsAdapter(ratio, 0, 0, false, 512, "deg");
            when(mockLayer.getMeasurementAdapter(any())).thenReturn(adapter);
            AngleToolGraphic angle = createAngle(1.0, 0.0, 0.0, 0.0, 0.0, 1.0);

            List<MeasureItem> items =
                angle.computeMeasurements(mockLayer, true, Unit.DEGREE);

            MeasureItem angleItem = items.stream()
                .filter(i -> i.getMeasurement() == AngleToolGraphic.ANGLE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ANGLE not found"));

            assertEquals(90.0, (Double) angleItem.getValue(), 0.01,
                "Angle should be invariant under calibration ratio changes");
        }
    }

    @Test
    void testPerimeterScalesLinearlyWithCalibration() {
        double[] ratios = {0.5, 1.0, 2.0};
        double pixelPerimeter = 12.0;
        PolygonGraphic triangle = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(0, 3)
        );

        for (double ratio : ratios) {
            MeasurementsAdapter adapter =
                new MeasurementsAdapter(ratio, 0, 0, false, 512, "cm");
            when(mockLayer.getMeasurementAdapter(any())).thenReturn(adapter);
            triangle.buildShape(null);

            List<MeasureItem> items =
                triangle.computeMeasurements(mockLayer, false, Unit.CENTIMETER);

            MeasureItem perimeterItem = items.stream()
                .filter(i -> i.getMeasurement() == PolygonGraphic.PERIMETER)
                .findFirst()
                .orElseThrow(() -> new AssertionError("PERIMETER not found"));

            assertEquals(pixelPerimeter * ratio,
                (Double) perimeterItem.getValue(), 0.001,
                "Perimeter should scale linearly with calibration ratio");
        }
    }

    // ========================================================================
    // Null / Edge Case Tests
    // ========================================================================

    @Test
    void testAllGraphicsReturnEmptyForNullLayer() {
        LineGraphic line = createLine(0, 0, 3, 4);
        assertTrue(line.computeMeasurements(null, true, Unit.PIXEL).isEmpty());

        AngleToolGraphic angle = createAngle(1, 0, 0, 0, 0, 1);
        assertTrue(angle.computeMeasurements(null, true, Unit.PIXEL).isEmpty());

        EllipseGraphic ellipse = createEllipse(10, 6);
        assertTrue(ellipse.computeMeasurements(null, false, Unit.PIXEL).isEmpty());

        PolygonGraphic polygon = createPolygon(
            new Point2D.Double(0, 0),
            new Point2D.Double(4, 0),
            new Point2D.Double(0, 3)
        );
        assertTrue(polygon.computeMeasurements(null, false, Unit.PIXEL).isEmpty());
    }

    @Test
    void testAllGraphicsReturnEmptyForNoContentLayer() {
        when(mockLayer.hasContent()).thenReturn(false);
        LineGraphic line = createLine(0, 0, 3, 4);
        assertTrue(line.computeMeasurements(mockLayer, true, Unit.PIXEL).isEmpty());
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private LineGraphic createLine(double x1, double y1, double x2, double y2) {
        LineGraphic line = new LineGraphic();
        line.setPts(Arrays.asList(
            new Point2D.Double(x1, y1),
            new Point2D.Double(x2, y2)
        ));
        line.buildShape(null);
        return line;
    }

    private AngleToolGraphic createAngle(
        double ax, double ay, double ox, double oy, double bx, double by) {
        AngleToolGraphic angle = new AngleToolGraphic();
        angle.setPts(Arrays.asList(
            new Point2D.Double(ax, ay),
            new Point2D.Double(ox, oy),
            new Point2D.Double(bx, by)
        ));
        angle.buildShape(null);
        return angle;
    }

    private EllipseGraphic createEllipse(double majorAxis, double minorAxis) {
        return createEllipseAt(0.0, 0.0, majorAxis, minorAxis);
    }

    private EllipseGraphic createEllipseAt(
        double cx, double cy, double majorAxis, double minorAxis) {
        EllipseGraphic ellipse = new EllipseGraphic();
        double halfMajor = majorAxis / 2.0;
        double halfMinor = minorAxis / 2.0;
        ellipse.setPts(Arrays.asList(
            new Point2D.Double(cx - halfMajor, cy),
            new Point2D.Double(cx + halfMajor, cy),
            new Point2D.Double(cx, cy - halfMinor),
            new Point2D.Double(cx, cy + halfMinor)
        ));
        ellipse.buildShape(null);
        return ellipse;
    }

    private PolygonGraphic createPolygon(Point2D... points) {
        PolygonGraphic polygon = new PolygonGraphic();
        polygon.setPts(Arrays.asList(points));
        polygon.setPointNumber(points.length);
        polygon.buildShape(null);
        return polygon;
    }
}
