/*
 * Copyright (c) 2023 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.editor.image;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;
import org.weasis.core.api.gui.util.ShortcutManager;
import org.weasis.core.ui.model.GraphicModel;

public class DrawingsKeyListeners implements KeyListener {
  private final Canvas canvas;

  public DrawingsKeyListeners(Canvas canvas) {
    this.canvas = Objects.requireNonNull(canvas);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    ShortcutManager sm = ShortcutManager.getInstance();
    GraphicModel graphicManager = canvas.getGraphicManager();
    if (sm.matches(ShortcutManager.ID_DRAW_DELETE, e)) {
      graphicManager.deleteSelectedGraphics(canvas, true);
    } else if (sm.matches(ShortcutManager.ID_DRAW_DESELECT_ALL, e)) {
      graphicManager.setSelectedGraphic(null);
    } else if (sm.matches(ShortcutManager.ID_DRAW_SELECT_ALL, e)) {
      graphicManager.setSelectedAllGraphics();
    }
    // Arrow keys are reserved for pan operations. Nudge of selected graphics
    // via arrow keys could be implemented here when not in pan mode.
  }

  @Override
  public void keyReleased(KeyEvent e) {
    // Do Nothing
  }

  @Override
  public void keyTyped(KeyEvent e) {
    // DO nothing
  }
}
