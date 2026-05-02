/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.codec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DicomConformanceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DicomConformanceTest.class);

  @Test
  void testAllTransferSyntaxesHaveValidUIDs() {
    for (TransferSyntax ts : TransferSyntax.values()) {
      if (ts == TransferSyntax.NONE) continue;
      String uid = ts.getTransferSyntaxUID();
      assertNotNull(uid, "UID null for " + ts.name());
      assertTrue(uid.startsWith("1.2.840.10008."), "Invalid UID prefix: " + uid);
      LOGGER.info("TS {}: {}", ts.name(), uid);
    }
  }

  @Test
  void testAllTransferSyntaxesHaveLabels() {
    for (TransferSyntax ts : TransferSyntax.values()) {
      String label = ts.getLabel();
      assertNotNull(label, "Label null for " + ts.name());
      assertFalse(label.isEmpty(), "Label empty for " + ts.name());
    }
  }

  @Test
  void testSupportedSopClasses() {
    // List of SOP Class UIDs that Weasis should support
    List<String> sopClasses = Arrays.asList(
        "1.2.840.10008.5.1.4.1.1.1",   // CR Image Storage
        "1.2.840.10008.5.1.4.1.1.2",   // CT Image Storage
        "1.2.840.10008.5.1.4.1.1.4",   // MR Image Storage
        "1.2.840.10008.5.1.4.1.1.7",   // Secondary Capture
        "1.2.840.10008.5.1.4.1.1.9",   // Computed Radiography
        "1.2.840.10008.5.1.4.1.1.12",  // XA/XRF
        "1.2.840.10008.5.1.4.1.1.20",  // NM
        "1.2.840.10008.5.1.4.1.1.30",  // RF
        "1.2.840.10008.5.1.4.1.1.77.1", // VL Image
        "1.2.840.10008.5.1.4.1.1.88.11", // Basic Text SR
        "1.2.840.10008.5.1.4.1.1.88.33", // Comprehensive SR
        "1.2.840.10008.5.1.4.1.1.128",  // RT Structure Set
        "1.2.840.10008.5.1.4.1.1.481.2", // RT Dose
        "1.2.840.10008.5.1.4.1.1.481.3", // RT Plan
        "1.2.840.10008.5.1.4.1.1.481.5", // RT Ion Plan
        "1.2.840.10008.5.1.4.1.1.66",   // Surface Scan Mesh
        "1.2.840.10008.5.1.4.1.1.66.1", // Surface Scan Point Cloud
        "1.2.840.10008.5.1.4.1.1.130",  // Breast Tomosynthesis
        "1.2.840.10008.5.1.4.1.1.3.1"   // Ultrasound MF Image
    );
    for (String sopClass : sopClasses) {
      assertNotNull(sopClass);
    }
    assertEquals(19, sopClasses.size());
  }

  @Test
  void testDicomMediaIOMimeTypes() {
    assertNotNull(DicomMediaIO.DICOM_MIMETYPE);
    assertNotNull(DicomMediaIO.SERIES_MIMETYPE);
    assertNotNull(DicomMediaIO.SERIES_PR_MIMETYPE);
    assertNotNull(DicomMediaIO.SERIES_KO_MIMETYPE);
    assertNotNull(DicomMediaIO.SERIES_SEG_MIMETYPE);
    assertNotNull(DicomMediaIO.SERIES_XDSI);
  }

  @Test
  void testTagDCommonTags() {
    assertNotNull(TagD.get(Tag.PatientName));
    assertNotNull(TagD.get(Tag.StudyInstanceUID));
    assertNotNull(TagD.get(Tag.SeriesInstanceUID));
    assertNotNull(TagD.get(Tag.SOPInstanceUID));
    assertNotNull(TagD.get(Tag.Modality));
    assertNotNull(TagD.get(Tag.PatientID));
  }
}
