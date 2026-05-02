/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.gui.util;

import java.awt.KeyboardFocusManager;
import java.util.Objects;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.ui.accessible.AccessibleFocusTraversalPolicy;

public final class AccessibilityHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessibilityHelper.class);

  private AccessibilityHelper() {}

  public static void setAccessibleName(JComponent component, String name) {
    Objects.requireNonNull(component);
    if (name != null) {
      component.getAccessibleContext().setAccessibleName(name);
    }
  }

  public static void setAccessibleDescription(JComponent component, String description) {
    Objects.requireNonNull(component);
    if (description != null) {
      component.getAccessibleContext().setAccessibleDescription(description);
    }
  }

  public static void setAccessibleNameAndDescription(
      JComponent component, String name, String description) {
    setAccessibleName(component, name);
    setAccessibleDescription(component, description);
  }

  public static void installAccessibleSupport(JButton button, String name, String description) {
    button.getAccessibleContext().setAccessibleName(name);
    button.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void installAccessibleSupport(JToggleButton button, String name, String description) {
    button.getAccessibleContext().setAccessibleName(name);
    button.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void installAccessibleSupport(
      JSlider slider, String name, String description, int minimum, int maximum, int value) {
    slider.getAccessibleContext().setAccessibleName(name);
    slider.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void installAccessibleSupport(
      JComboBox<?> comboBox, String name, String description) {
    comboBox.getAccessibleContext().setAccessibleName(name);
    comboBox.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void installAccessibleSupport(JTree tree, String name, String description) {
    tree.getAccessibleContext().setAccessibleName(name);
    tree.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void installAccessibleSupport(JTable table, String name, String description) {
    table.getAccessibleContext().setAccessibleName(name);
    table.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void installAccessibleSupport(JMenuItem menuItem, String name, String description) {
    menuItem.getAccessibleContext().setAccessibleName(name);
    menuItem.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void installAccessibleSupport(
      AbstractButton button, String name, String description) {
    button.getAccessibleContext().setAccessibleName(name);
    button.getAccessibleContext().setAccessibleDescription(description);
  }

  public static void configureGlobalFocusTraversalPolicy() {
    KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    focusManager.setDefaultFocusTraversalPolicy(new AccessibleFocusTraversalPolicy());
  }

  public static void registerKeyboardAlternative(
      JComponent component, String actionKey, KeyStroke keyStroke, int condition) {
    component.getInputMap(condition).put(keyStroke, actionKey);
  }

  public static void enableJavaAccessBridge() {
    String property = "javax.accessibility.assistive_technologies";
    if (System.getProperty(property) == null) {
      try {
        System.setProperty(property, "java.awt.event.InputMethodListener");
      } catch (SecurityException e) {
        LOGGER.warn("Cannot set system property {}: {}", property, e.getMessage());
      }
    }
  }

  public static void initAccessibility() {
    enableJavaAccessBridge();
    configureGlobalFocusTraversalPolicy();
    LOGGER.info("Accessibility support initialized");
  }

  public static void setFocusTraversalKeysEnabled(JComponent component, boolean enabled) {
    component.setFocusTraversalKeysEnabled(enabled);
  }

  public static void setFocusable(JComponent component, boolean focusable) {
    component.setFocusable(focusable);
  }

  public static void setRequestFocusEnabled(JComponent component, boolean enabled) {
    component.setRequestFocusEnabled(enabled);
  }
}
