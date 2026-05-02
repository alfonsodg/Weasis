/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.weasis.dicom.codec.DicomMediaIO;

class View2dFactoryTest {

  private final View2dFactory factory = new View2dFactory();

  @Test
  void testGetUIName() {
    assertNotNull(factory.getUIName());
    assertFalse(factory.getUIName().isEmpty());
  }

  @Test
  void testGetDescription() {
    assertNotNull(factory.getDescription());
    assertFalse(factory.getDescription().isEmpty());
  }

  @Test
  void testCanReadMimeType() {
    assertTrue(factory.canReadMimeType(DicomMediaIO.SERIES_MIMETYPE));
    assertFalse(factory.canReadMimeType("application/unknown"));
    assertFalse(factory.canReadMimeType(null));
  }

  @Test
  void testGetLevel() {
    assertEquals(5, factory.getLevel());
  }

  @Test
  void testCanAddSeries() {
    assertTrue(factory.canAddSeries());
  }

  @Test
  void testCanExternalizeSeries() {
    assertTrue(factory.canExternalizeSeries());
  }

  @Test
  void testCanReadSeries() {
    assertFalse(factory.canReadSeries(null));
  }
}
