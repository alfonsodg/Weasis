/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.image;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.weasis.core.api.image.op.GsdfLut;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;

/**
 * Image operation that applies the DICOM Part 14 Grayscale Standard Display Function (GSDF) to an
 * image.
 *
 * <p>The GSDF ensures that the perceived contrast is uniform across the entire luminance range of
 * the display, providing consistent image appearance across different display devices.
 *
 * <p>This operation should typically be placed after the WindowOp in the processing pipeline. It
 * applies a non-linear lookup table derived from the DICOM PS3.14 formula to map pixel values
 * through the GSDF curve.
 *
 * <p>Configuration parameters:
 *
 * <ul>
 *   <li>{@link #P_MIN_LUMINANCE} - minimum display luminance in cd/m2 (default: 0.05)
 *   <li>{@link #P_MAX_LUMINANCE} - maximum display luminance in cd/m2 (default: 4000.0)
 *   <li>{@link #P_AMBIENT_LIGHT} - ambient light level in lux (default: 0.0)
 * </ul>
 */
public class GsdfOp extends AbstractOp {

  public static final String OP_NAME = "gsdf";

  /** Minimum luminance of the target display in cd/m2 (Double, default: 0.05). */
  public static final String P_MIN_LUMINANCE = "gsdf.min.luminance";

  /** Maximum luminance of the target display in cd/m2 (Double, default: 4000.0). */
  public static final String P_MAX_LUMINANCE = "gsdf.max.luminance";

  /** Ambient light level in lux (Double, default: 0.0). */
  public static final String P_AMBIENT_LIGHT = "gsdf.ambient.light";

  private static final double DEFAULT_MIN_LUMINANCE = 0.05;
  private static final double DEFAULT_MAX_LUMINANCE = 4000.0;
  private static final double DEFAULT_AMBIENT_LIGHT = 0.0;

  // LUT sizes for 8-bit and 16-bit images
  private static final int LUT_8BIT_SIZE = 256;
  private static final int LUT_16BIT_SIZE = 65536;

  public GsdfOp() {
    setName(OP_NAME);
  }

  public GsdfOp(GsdfOp op) {
    super(op);
  }

  @Override
  public GsdfOp copy() {
    return new GsdfOp(this);
  }

  @Override
  public void process() throws Exception {
    PlanarImage source = getSourceImage();
    if (source == null) {
      return;
    }

    double minLuminance = getParam(P_MIN_LUMINANCE, Double.class, DEFAULT_MIN_LUMINANCE);
    double maxLuminance = getParam(P_MAX_LUMINANCE, Double.class, DEFAULT_MAX_LUMINANCE);
    double ambientLight = getParam(P_AMBIENT_LIGHT, Double.class, DEFAULT_AMBIENT_LIGHT);

    PlanarImage result = applyGsdf(source, minLuminance, maxLuminance, ambientLight);
    params.put(Param.OUTPUT_IMG, result);
  }

  /**
   * Applies the GSDF lookup table to the source image.
   *
   * @param source the input image
   * @param minLuminance minimum display luminance in cd/m2
   * @param maxLuminance maximum display luminance in cd/m2
   * @param ambientLight ambient light level in lux
   * @return the GSDF-calibrated image
   */
  private PlanarImage applyGsdf(
      PlanarImage source, double minLuminance, double maxLuminance, double ambientLight) {
    int depth = CvType.depth(source.type());

    Mat lut = createGsdfLut(depth, minLuminance, maxLuminance, ambientLight);
    if (lut == null) {
      return source;
    }

    Mat srcMat = source.toMat();
    ImageCV result = new ImageCV();
    Core.LUT(srcMat, lut, result);
    return result;
  }

  /**
   * Creates a GSDF lookup table for the given image depth and luminance range.
   *
   * @param depth the OpenCV depth type (e.g., CV_8U, CV_16U)
   * @param minLuminance minimum display luminance
   * @param maxLuminance maximum display luminance
   * @param ambientLight ambient light level
   * @return the LUT matrix, or null if the depth is not supported
   */
  private Mat createGsdfLut(
      int depth, double minLuminance, double maxLuminance, double ambientLight) {
    int lutSize = getLutSize(depth);
    if (lutSize <= 0) {
      return null;
    }

    int minPValue = Math.max(1, GsdfLut.getPValueFromLuminance(minLuminance));
    int maxPValue = GsdfLut.getPValueFromLuminance(maxLuminance);
    if (maxPValue <= minPValue) {
      maxPValue = minPValue + 1;
    }

    int[] pValues = GsdfLut.getGsdfLut(minPValue, maxPValue, lutSize);

    double compensatedMin = computeCompensatedLuminance(minLuminance, ambientLight);
    double compensatedMax = computeCompensatedLuminance(maxLuminance, ambientLight);
    double lumRange = compensatedMax - compensatedMin;

    int cvType = (depth == CvType.CV_8U) ? CvType.CV_8UC1 : CvType.CV_16UC1;
    Mat lut = new Mat(1, lutSize, cvType);

    byte[] lutData8;
    short[] lutData16;
    if (depth == CvType.CV_8U) {
      lutData8 = new byte[lutSize];
    } else {
      lutData16 = new short[lutSize];
    }

    for (int i = 0; i < lutSize; i++) {
      double luminance = GsdfLut.getLuminanceFromPValue(pValues[i]);
      double compensated = computeCompensatedLuminance(luminance, ambientLight);
      double normalized = (compensated - compensatedMin) / lumRange;
      normalized = Math.clamp(normalized, 0.0, 1.0);

      if (depth == CvType.CV_8U) {
        lutData8[i] = (byte) (normalized * 255.0);
      } else {
        lutData16[i] = (short) (normalized * 65535.0);
      }
    }

    if (depth == CvType.CV_8U) {
      lut.put(0, 0, lutData8);
    } else {
      lut.put(0, 0, lutData16);
    }

    return lut;
  }

  /**
   * Computes the effective luminance including ambient light contribution. The ambient light adds a
   * reflection component to the display luminance.
   *
   * @param displayLuminance the display luminance in cd/m2
   * @param ambientLight the ambient light level in lux
   * @return the total effective luminance
   */
  private static double computeCompensatedLuminance(double displayLuminance, double ambientLight) {
    return displayLuminance + ambientLight * 0.01;
  }

  /**
   * Returns the LUT size appropriate for the given image depth.
   *
   * @param depth the OpenCV depth type
   * @return the LUT size, or -1 if unsupported
   */
  private static int getLutSize(int depth) {
    return switch (depth) {
      case CvType.CV_8U -> LUT_8BIT_SIZE;
      case CvType.CV_16U -> LUT_16BIT_SIZE;
      default -> -1;
    };
  }
}
