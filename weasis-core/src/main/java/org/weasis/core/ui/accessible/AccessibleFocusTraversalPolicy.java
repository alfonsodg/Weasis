/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.accessible;

/**
 * A focus traversal policy that follows a logical left-to-right, top-to-bottom order based on
 * component position within the container.
 *
 * <p>This policy enables keyboard-only navigation (Tab/Shift-Tab) in a clinically meaningful order
 * across the Weasis interface. Components are ordered by their rectangular bounds, prioritizing
 * vertical position (top row first) then horizontal position (left to right).
 *
 * <p>Replaces the default Swing FocusTraversalPolicy which may not follow the clinical workflow
 * order expected by radiologists and technicians.
 *
 * @see javax.swing.FocusTraversalPolicy
 */
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AccessibleFocusTraversalPolicy extends FocusTraversalPolicy {

  private final List<Component> focusableOrder;

  public AccessibleFocusTraversalPolicy() {
    this.focusableOrder = new ArrayList<>();
  }

  public AccessibleFocusTraversalPolicy(Container container) {
    this.focusableOrder = new ArrayList<>();
    rebuildOrder(container);
  }

  public void rebuildOrder(Container container) {
    focusableOrder.clear();
    collectFocusableComponents(container, focusableOrder);
    focusableOrder.sort(
        Comparator.comparingInt(
                (Component c) -> {
                  Rectangle b = c.getBounds();
                  return b.y + b.height;
                })
            .thenComparingInt(
                c -> {
                  Rectangle b = c.getBounds();
                  return b.x;
                }));
  }

  private static void collectFocusableComponents(
      Container container, List<Component> result) {
    for (Component component : container.getComponents()) {
      if (component instanceof Container childContainer) {
        if (childContainer.isFocusCycleRoot()) {
          continue;
        }
        collectFocusableComponents(childContainer, result);
      }
      if (component.isFocusable() && component.isVisible() && component.isEnabled()) {
        result.add(component);
      }
    }
  }

  @Override
  public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
    if (focusableOrder.isEmpty()) {
      rebuildOrder(focusCycleRoot);
    }
    int index = focusableOrder.indexOf(aComponent);
    if (index < 0) {
      return getDefaultComponent(focusCycleRoot);
    }
    int next = (index + 1) % focusableOrder.size();
    return focusableOrder.get(next);
  }

  @Override
  public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
    if (focusableOrder.isEmpty()) {
      rebuildOrder(focusCycleRoot);
    }
    int index = focusableOrder.indexOf(aComponent);
    if (index < 0) {
      return getDefaultComponent(focusCycleRoot);
    }
    int prev = (index - 1 + focusableOrder.size()) % focusableOrder.size();
    return focusableOrder.get(prev);
  }

  @Override
  public Component getFirstComponent(Container focusCycleRoot) {
    if (focusableOrder.isEmpty()) {
      rebuildOrder(focusCycleRoot);
    }
    return focusableOrder.isEmpty() ? null : focusableOrder.get(0);
  }

  @Override
  public Component getLastComponent(Container focusCycleRoot) {
    if (focusableOrder.isEmpty()) {
      rebuildOrder(focusCycleRoot);
    }
    return focusableOrder.isEmpty() ? null : focusableOrder.get(focusableOrder.size() - 1);
  }

  @Override
  public Component getDefaultComponent(Container focusCycleRoot) {
    return getFirstComponent(focusCycleRoot);
  }
}
