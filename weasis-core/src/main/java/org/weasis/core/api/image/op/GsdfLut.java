/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.image.op;

/**
 * Implements the DICOM Part 14 Grayscale Standard Display Function (GSDF) using the formula from
 * NEMA PS3.14.
 *
 * <p>The GSDF defines a relationship between perceptual JND (Just Noticeable Difference) indices
 * (P-Values) and luminance values (cd/m2). This implementation provides methods for converting
 * between P-Values and luminance, generating lookup tables (LUTs), and calculating JND values.
 *
 * <p>Formula: log10(L) = (a + c*log10(J) + e*log10(J)^2 + g*log10(J)^3 + i*log10(J)^4) / (1 +
 * b*log10(J) + d*log10(J)^2 + f*log10(J)^3 + h*log10(J)^4 + j*log10(J)^5)
 *
 * <p>where J = JND index (P-Value), L = Luminance in cd/m2
 *
 * @see <a href="https://dicom.nema.org/medical/dicom/current/output/chtml/part14/chapter_B.html">
 *     DICOM PS3.14 - Grayscale Standard Display Function</a>
 */
public final class GsdfLut {

  private GsdfLut() {}

  // DICOM PS3.14 constants for the GSDF formula
  private static final double A = -1.3011877;
  private static final double B = -2.5840191E-2;
  private static final double C = 8.0242636E-2;
  private static final double D = -1.0320229E-1;
  private static final double E = 1.3646699E-1;
  private static final double F = 2.8745620E-2;
  private static final double G = -2.5468404E-2;
  private static final double H = -3.1978977E-3;
  private static final double I = 1.2992634E-2;
  private static final double J = 1.3635334E-3;

  // Typical luminance range for medical displays
  private static final double MIN_LUMINANCE = 0.05;
  private static final double MAX_LUMINANCE = 4000.0;

  // P-Value range covering 0.05 to 4000 cd/m2
  private static final int MIN_P_VALUE = 1;
  private static final int MAX_P_VALUE = 4095;

  // Newton's method convergence
  private static final int NEWTON_MAX_ITERATIONS = 50;
  private static final double NEWTON_TOLERANCE = 1e-12;

  /**
   * Converts a P-Value (JND index) to luminance in cd/m2 using the DICOM PS3.14 formula.
   *
   * @param pValue the JND index (P-Value), must be &gt;= 1
   * @return the luminance in cd/m2
   * @throws IllegalArgumentException if pValue is less than 1
   */
  public static double getLuminanceFromPValue(int pValue) {
    if (pValue < 1) {
      throw new IllegalArgumentException("P-Value must be >= 1, got: " + pValue);
    }
    double logJ = Math.log10(pValue);
    double logJ2 = logJ * logJ;
    double logJ3 = logJ2 * logJ;
    double logJ4 = logJ3 * logJ;
    double logJ5 = logJ4 * logJ;

    double numerator = A + C * logJ + E * logJ2 + G * logJ3 + I * logJ4;
    double denominator = 1.0 + B * logJ + D * logJ2 + F * logJ3 + H * logJ4 + J * logJ5;

    return Math.pow(10.0, numerator / denominator);
  }

  /**
   * Converts a luminance value in cd/m2 to the nearest P-Value (JND index) using Newton's method.
   *
   * @param luminance the luminance in cd/m2, must be &gt;= 0
   * @return the closest P-Value (JND index) for the given luminance
   * @throws IllegalArgumentException if luminance is negative
   */
  public static int getPValueFromLuminance(double luminance) {
    if (luminance < 0.0) {
      throw new IllegalArgumentException("Luminance cannot be negative, got: " + luminance);
    }
    if (luminance == 0.0) {
      return MIN_P_VALUE;
    }

    return findPValueByNewton(luminance);
  }

  /**
   * Generates a GSDF lookup table (LUT) mapping step indices to P-Values for the given range.
   *
   * <p>The LUT contains {@code steps} entries, each being a P-Value linearly distributed between
   * {@code minPValue} and {@code maxPValue}.
   *
   * @param minPValue the minimum P-Value (JND index), must be &gt;= 1
   * @param maxPValue the maximum P-Value (JND index), must be &gt;= minPValue
   * @param steps the number of entries in the LUT, must be &gt;= 2
   * @return an array of P-Values forming the GSDF LUT
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static int[] getGsdfLut(int minPValue, int maxPValue, int steps) {
    if (minPValue < 1) {
      throw new IllegalArgumentException("minPValue must be >= 1, got: " + minPValue);
    }
    if (maxPValue < minPValue) {
      throw new IllegalArgumentException(
          "maxPValue must be >= minPValue, got min=" + minPValue + " max=" + maxPValue);
    }
    if (steps < 2) {
      throw new IllegalArgumentException("steps must be >= 2, got: " + steps);
    }

    int[] lut = new int[steps];
    double range = (double) maxPValue - minPValue;
    double stepSize = range / (steps - 1.0);

    for (int i = 0; i < steps; i++) {
      lut[i] = (int) Math.round(minPValue + stepSize * i);
    }

    return lut;
  }

  /**
   * Calculates the Just Noticeable Difference (JND) in luminance at a given P-Value. The JND is the
   * luminance difference between the given P-Value and P-Value+1.
   *
   * @param pValue the JND index (P-Value), must be &gt;= 1
   * @return the luminance difference (cd/m2) representing one JND at this P-Value
   * @throws IllegalArgumentException if pValue is less than 1
   */
  public static double getJustNoticeableDifference(int pValue) {
    if (pValue < 1) {
      throw new IllegalArgumentException("P-Value must be >= 1, got: " + pValue);
    }
    double l1 = getLuminanceFromPValue(pValue);
    double l2 = getLuminanceFromPValue(pValue + 1);
    return l2 - l1;
  }

  /**
   * Returns the minimum luminance of the typical medical display range (0.05 cd/m2).
   *
   * @return minimum luminance in cd/m2
   */
  public static double getMinimumLuminance() {
    return MIN_LUMINANCE;
  }

  /**
   * Returns the maximum luminance of the typical medical display range (4000 cd/m2).
   *
   * @return maximum luminance in cd/m2
   */
  public static double getMaximumLuminance() {
    return MAX_LUMINANCE;
  }

  /**
   * Finds the P-Value for a given luminance using Newton's method on the GSDF function.
   *
   * <p>Uses binary search to get close to the solution, then refines with Newton's method for
   * higher precision.
   *
   * @param luminance the target luminance in cd/m2
   * @return the P-Value (JND index) corresponding to the given luminance
   */
  private static int findPValueByNewton(double luminance) {
    double j = binarySearchPValue(luminance);
    if (j < 1.0) {
      return MIN_P_VALUE;
    }

    for (int iter = 0; iter < NEWTON_MAX_ITERATIONS; iter++) {
      double logJ = Math.log10(j);
      double logJ2 = logJ * logJ;
      double logJ3 = logJ2 * logJ;
      double logJ4 = logJ3 * logJ;
      double logJ5 = logJ4 * logJ;

      double numerator = A + C * logJ + E * logJ2 + G * logJ3 + I * logJ4;
      double denominator = 1.0 + B * logJ + D * logJ2 + F * logJ3 + H * logJ4 + J * logJ5;

      double luminance_j = Math.pow(10.0, numerator / denominator);
      double diff = luminance_j - luminance;

      if (Math.abs(diff) < NEWTON_TOLERANCE) {
        break;
      }

      double nPrimeLog = C + 2 * E * logJ + 3 * G * logJ2 + 4 * I * logJ3;
      double dPrimeLog = B + 2 * D * logJ + 3 * F * logJ2 + 4 * H * logJ3 + 5 * J * logJ4;

      double dLdJ =
          luminance_j * (nPrimeLog * denominator - numerator * dPrimeLog)
              / (denominator * denominator * j);

      if (Math.abs(dLdJ) < 1e-20) {
        break;
      }

      j = j - diff / dLdJ;
    }

    return (int) Math.round(j);
  }

  /**
   * Performs a binary search to find an approximate P-Value for the given luminance.
   *
   * @param luminance the target luminance
   * @return approximate P-Value
   */
  private static double binarySearchPValue(double luminance) {
    double lo = 1.0;
    double hi = 5000.0;

    while (hi - lo > 0.5) {
      double mid = (lo + hi) / 2.0;
      double l = getLuminanceFromPValue((int) Math.round(mid));
      if (l < luminance) {
        lo = mid;
      } else {
        hi = mid;
      }
    }

    return (lo + hi) / 2.0;
  }
}
