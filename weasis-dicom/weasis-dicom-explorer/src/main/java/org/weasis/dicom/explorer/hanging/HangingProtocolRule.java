/*
 * Copyright (c) 2022 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.hanging;

import java.util.Objects;
import org.w3c.dom.Element;
import org.weasis.core.util.StringUtil;

public class HangingProtocolRule implements Comparable<HangingProtocolRule> {

  private final String modality;
  private final String bodyPart;
  private final int minSeries;
  private final int maxSeries;
  private final String layoutName;
  private final int priority;

  public HangingProtocolRule(
      String modality,
      String bodyPart,
      int minSeries,
      int maxSeries,
      String layoutName,
      int priority) {
    this.modality = Objects.requireNonNull(modality, "modality cannot be null");
    this.bodyPart = bodyPart != null ? bodyPart : "";
    this.minSeries = Math.max(0, minSeries);
    this.maxSeries = maxSeries > 0 ? maxSeries : Integer.MAX_VALUE;
    this.layoutName = Objects.requireNonNull(layoutName, "layoutName cannot be null");
    this.priority = priority;
  }

  public HangingProtocolRule(
      String modality, String bodyPart, int minSeries, String layoutName, int priority) {
    this(modality, bodyPart, minSeries, 0, layoutName, priority);
  }

  public HangingProtocolRule(String modality, String bodyPart, String layoutName, int priority) {
    this(modality, bodyPart, 0, 0, layoutName, priority);
  }

  public boolean match(String seriesModality, String seriesBodyPart, int seriesCount) {
    if (!matchesModality(seriesModality)) {
      return false;
    }
    if (StringUtil.hasText(bodyPart) && !bodyPart.equalsIgnoreCase(seriesBodyPart)) {
      return false;
    }
    return seriesCount >= minSeries && seriesCount <= maxSeries;
  }

  private boolean matchesModality(String seriesModality) {
    if (seriesModality == null) {
      return false;
    }
    for (String m : modality.split(",")) {
      if (m.trim().equalsIgnoreCase(seriesModality.trim())) {
        return true;
      }
    }
    return false;
  }

  public static HangingProtocolRule fromXmlElement(Element element) {
    String modality = element.getAttribute("modality");
    String bodyPart = element.getAttribute("bodyPart");
    int minSeries = parseIntAttribute(element, "minSeries", 0);
    int maxSeries = parseIntAttribute(element, "maxSeries", 0);
    String layoutName = element.getAttribute("layoutName");
    int priority = parseIntAttribute(element, "priority", 0);
    return new HangingProtocolRule(modality, bodyPart, minSeries, maxSeries, layoutName, priority);
  }

  private static int parseIntAttribute(Element element, String name, int defaultValue) {
    String value = element.getAttribute(name);
    if (!StringUtil.hasText(value)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public String getModality() {
    return modality;
  }

  public String getBodyPart() {
    return bodyPart;
  }

  public int getMinSeries() {
    return minSeries;
  }

  public int getMaxSeries() {
    return maxSeries;
  }

  public String getLayoutName() {
    return layoutName;
  }

  public int getPriority() {
    return priority;
  }

  @Override
  public int compareTo(HangingProtocolRule o) {
    return Integer.compare(priority, o.priority);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HangingProtocolRule that)) return false;
    return priority == that.priority
        && minSeries == that.minSeries
        && maxSeries == that.maxSeries
        && modality.equals(that.modality)
        && bodyPart.equals(that.bodyPart)
        && layoutName.equals(that.layoutName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modality, bodyPart, minSeries, maxSeries, layoutName, priority);
  }

  @Override
  public String toString() {
    return String.format(
        "HangingProtocolRule{modality='%s', bodyPart='%s', minSeries=%d, maxSeries=%d, layoutName='%s', priority=%d}",
        modality, bodyPart, minSeries, maxSeries, layoutName, priority);
  }
}
