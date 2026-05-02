/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.send;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.web.ContentType;

/**
 * Integration tests for STOW-RS (DICOM Web Send).
 *
 * <p>Requires a running dcm4chee Arc server. Configure via system properties:
 *
 * <ul>
 *   <li>{@code dcm4chee.base.url} - base URL (default: http://gdevel:8080/dcm4chee-arc)
 *   <li>{@code dcm4chee.aet} - AE Title (default: DCM4CHEE)
 *   <li>{@code test.dicom.dir} - directory with DICOM files to upload (default: empty/skip)
 * </ul>
 */
@Tag("integration")
class StowRsIntegrationTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(StowRsIntegrationTest.class);

  private static String stowUrl;
  private static String aet;
  private static boolean serverAvailable;

  @BeforeAll
  static void setup() {
    String host = System.getProperty("dcm4chee.host", "gdevel");
    String port = System.getProperty("dcm4chee.port", "8080");
    String webContext = System.getProperty("dcm4chee.web.context", "dcm4chee-arc");
    String baseUrl =
        System.getProperty("dcm4chee.base.url", "http://" + host + ":" + port + "/" + webContext);
    aet = System.getProperty("dcm4chee.aet", "DCM4CHEE");
    stowUrl = baseUrl + "/aets/" + aet + "/rs";

    try {
      URL url = new URL(baseUrl + "/aets/" + aet + "/rs");
      java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(3000);
      conn.setReadTimeout(3000);
      serverAvailable = (conn.getResponseCode() < 500);
      conn.disconnect();
    } catch (Exception e) {
      serverAvailable = false;
      LOGGER.warn("dcm4chee server not available: {}", e.getMessage());
    }
  }

  @Test
  void testStowConnection() {
    Assumptions.assumeTrue(serverAvailable, "dcm4chee server required");

    // Verify the STOW-RS endpoint is accessible
    try {
      URL url = new URL(stowUrl + "/studies");
      java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      // Without content, we expect 411 (Length Required) or 400 (Bad Request)
      // Both indicate the endpoint exists
      int code = conn.getResponseCode();
      assertTrue(code == 411 || code == 400 || code == 200,
          "Expected 411/400/200 but got " + code);
      LOGGER.info("STOW-RS endpoint accessible, response: {}", code);
    } catch (Exception e) {
      fail("STOW-RS endpoint not accessible: " + e.getMessage());
    }
  }

  @Test
  void testStowInvalidData() {
    Assumptions.assumeTrue(serverAvailable, "dcm4chee server required");

    HashMap<String, String> headers = new HashMap<>();
    StowRS stowRs = new StowRS(stowUrl, ContentType.APPLICATION_DICOM, "WeasisTest", headers);
    DicomState state = stowRs.uploadDicom(new java.io.File("/nonexistent.dcm"), null);

    assertNotNull(state);
    assertTrue(
        state.getStatus() == DicomState.Status.FAILURE
            || state.getStatus() == DicomState.Status.CANCELED,
        "Expected failure for nonexistent file");
    LOGGER.info("STOW-RS correctly rejected nonexistent file");
  }
}
