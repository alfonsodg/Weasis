/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.qr;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.media.data.TagUtil;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.param.DicomNode;

/**
 * Integration tests for DICOM Web services (QIDO-RS, WADO-RS, STOW-RS).
 *
 * <p>These tests require a running dcm4chee Arc server. The server URL is configured via system
 * property {@code dcm4chee.base.url} or defaults to {@code http://gdevel:8080/dcm4chee-arc}.
 *
 * <p>To run: {@code mvn test -Ddcm4chee.base.url=http://server:port/dcm4chee-arc -Pintegration}
 */
@Tag("integration")
class DicomWebIntegrationTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DicomWebIntegrationTest.class);

  private static String baseUrl;
  private static String aet;
  private static boolean serverAvailable;

  @BeforeAll
  static void setup() {
    String host = System.getProperty("dcm4chee.host", "gdevel");
    String port = System.getProperty("dcm4chee.port", "8080");
    String webContext = System.getProperty("dcm4chee.web.context", "dcm4chee-arc");
    baseUrl = System.getProperty("dcm4chee.base.url", "http://" + host + ":" + port + "/" + webContext);
    aet = System.getProperty("dcm4chee.aet", "DCM4CHEE");

    // Check if server is reachable
    try {
      URL url = new URL(baseUrl + "/aets/" + aet + "/rs");
      java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(3000);
      conn.setReadTimeout(3000);
      int responseCode = conn.getResponseCode();
      serverAvailable = (responseCode == 200 || responseCode == 204);
      conn.disconnect();
    } catch (Exception e) {
      serverAvailable = false;
      LOGGER.warn("dcm4chee server not available at {}: {}", baseUrl, e.getMessage());
    }

    if (serverAvailable) {
      LOGGER.info("dcm4chee server available at {} (AET: {})", baseUrl, aet);
    } else {
      LOGGER.warn("dcm4chee server NOT available. Integration tests will be skipped.");
    }
  }

  @Test
  void testQidoStudiesQuery() {
    Assumptions.assumeTrue(serverAvailable, "dcm4chee server required");

    // Build query parameters to search for recent studies
    String studyUrl = baseUrl + "/aets/" + aet + "/rs";
    RsQuery.RsQueryParams params = new RsQuery.RsQueryParams(studyUrl, null, null);

    // Use DicomQueryParams to search by modality
    String modality = System.getProperty("test.modality", "CT");
    int limit = Integer.parseInt(System.getProperty("test.limit", "5"));

    // Execute QIDO-RS query for studies
    List<MediaSeriesGroup> results = RsQuery.queryStudies(params, modality, null, null, limit);

    // Verify results
    assertNotNull(results);
    LOGGER.info("QIDO-RS returned {} studies for modality {}", results.size(), modality);

    if (!results.isEmpty()) {
      MediaSeriesGroup first = results.get(0);
      assertNotNull(first);
      TagW studyUidTag = TagD.get(Tag.StudyInstanceUID);
      assertNotNull(first.getTagValue(studyUidTag));
      LOGGER.info("First study UID: {}", first.getTagValue(studyUidTag));
    }
  }

  @Test
  void testQidoSeriesQuery() {
    Assumptions.assumeTrue(serverAvailable, "dcm4chee server required");

    String studyUrl = baseUrl + "/aets/" + aet + "/rs";
    String modality = System.getProperty("test.modality", "CT");
    int limit = Integer.parseInt(System.getProperty("test.limit", "5"));

    // First get studies
    List<MediaSeriesGroup> studies = RsQuery.queryStudies(
        new RsQuery.RsQueryParams(studyUrl, null, null), modality, null, null, limit);

    Assumptions.assumeFalse(studies.isEmpty(), "Need at least one study for series query");

    // Get study UID
    MediaSeriesGroup firstStudy = studies.get(0);
    TagW studyUidTag = TagD.get(Tag.StudyInstanceUID);
    String studyUid = (String) firstStudy.getTagValue(studyUidTag);

    // Query series for that study
    List<MediaSeriesGroup> series = RsQuery.querySeries(
        new RsQuery.RsQueryParams(studyUrl, null, null), studyUid, null, null, limit);

    assertNotNull(series);
    LOGGER.info("QIDO-RS returned {} series for study {}", series.size(), studyUid);
  }

  @Test
  void testQidoStudiesQueryByPatientId() {
    Assumptions.assumeTrue(serverAvailable, "dcm4chee server required");

    String patientId = System.getProperty("test.patientId", "*");
    String studyUrl = baseUrl + "/aets/" + aet + "/rs";
    int limit = Integer.parseInt(System.getProperty("test.limit", "5"));

    List<MediaSeriesGroup> results = RsQuery.queryStudies(
        new RsQuery.RsQueryParams(studyUrl, null, null), null, patientId, null, limit);

    assertNotNull(results);
    LOGGER.info("QIDO-RS returned {} studies for patient {}", results.size(), patientId);
  }

  @Test
  void testServerConnection() {
    // Basic connectivity test
    try {
      URL url = new URL(baseUrl + "/aets/" + aet + "/rs/studies");
      java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", "application/dicom+json");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      int responseCode = conn.getResponseCode();
      conn.disconnect();
      // 200 or 204 (no content) or 404 (no studies) are all acceptable
      assertTrue(responseCode < 500, "Server error: " + responseCode);
      LOGGER.info("Server connection OK, response: {}", responseCode);
    } catch (Exception e) {
      fail("Cannot connect to DICOMWeb server: " + e.getMessage());
    }
  }
}
