/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Structured audit event logger for clinical-grade operations.
 *
 * <p>Provides methods to log DICOM-related events with structured fields suitable for:
 *
 * <ul>
 *   <li>HIPAA audit trail compliance
 *   <li>IHE ATNA (Audit Trail and Node Authentication)
 *   <li>DICOM Part 15 Audit Messages
 * </ul>
 *
 * <p>Each event is logged with MDC (Mapped Diagnostic Context) fields that can be formatted as JSON
 * by the logback appender.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * AuditEventLogger.logStudyAccess("patient-123", "study-456", "user-xyz", "VIEW");
 * }</pre>
 */
public final class AuditEventLogger {
  private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

  private AuditEventLogger() {}

  public enum AuditAction {
    VIEW("study.view"),
    EXPORT("study.export"),
    PRINT("study.print"),
    IMPORT("study.import"),
    DELETE("study.delete"),
    LOGIN("auth.login"),
    LOGOUT("auth.logout"),
    CONFIGURE("app.configure"),
    LAUNCH("app.launch");

    private final String code;

    AuditAction(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }

  public enum AuditResult {
    SUCCESS,
    FAILURE,
    DENIED
  }

  /**
   * Logs a study access event.
   *
   * @param patientId the patient identifier
   * @param studyUid the DICOM Study Instance UID
   * @param userId the user performing the action
   * @param action the audit action
   */
  public static void logStudyAccess(
      String patientId, String studyUid, String userId, AuditAction action) {
    try (MDC.MDCCloseable mdcPatient = MDC.putCloseable("patientId", sanitize(patientId));
        MDC.MDCCloseable mdcStudy = MDC.putCloseable("studyUid", sanitize(studyUid));
        MDC.MDCCloseable mdcUser = MDC.putCloseable("userId", sanitize(userId));
        MDC.MDCCloseable mdcAction = MDC.putCloseable("action", action.getCode());
        MDC.MDCCloseable mdcResult =
            MDC.putCloseable("result", AuditResult.SUCCESS.name().toLowerCase());
        MDC.MDCCloseable mdcTs = MDC.putCloseable("timestamp", Instant.now().toString())) {
      AUDIT_LOG.info("{} patient={} study={}", action.getCode(), sanitize(patientId), sanitize(studyUid));
    }
  }

  /**
   * Logs an audit event with full context.
   *
   * @param patientId the patient identifier
   * @param studyUid the DICOM Study Instance UID
   * @param userId the user performing the action
   * @param action the audit action
   * @param result the audit result
   * @param detail additional detail message (may be null)
   */
  public static void logEvent(
      String patientId,
      String studyUid,
      String userId,
      AuditAction action,
      AuditResult result,
      String detail) {
    try (MDC.MDCCloseable mdcPatient = MDC.putCloseable("patientId", sanitize(patientId));
        MDC.MDCCloseable mdcStudy = MDC.putCloseable("studyUid", sanitize(studyUid));
        MDC.MDCCloseable mdcUser = MDC.putCloseable("userId", sanitize(userId));
        MDC.MDCCloseable mdcAction = MDC.putCloseable("action", action.getCode());
        MDC.MDCCloseable mdcResult = MDC.putCloseable("result", result.name().toLowerCase());
        MDC.MDCCloseable mdcTs = MDC.putCloseable("timestamp", Instant.now().toString())) {
      if (detail != null) {
        AUDIT_LOG.info(
            "{} patient={} study={} {}",
            action.getCode(),
            sanitize(patientId),
            sanitize(studyUid),
            detail);
      } else {
        AUDIT_LOG.info("{} patient={} study={}", action.getCode(), sanitize(patientId), sanitize(studyUid));
      }
    }
  }

  /**
   * Sanitizes a value for logging (prevents log injection).
   *
   * @param value the value to sanitize
   * @return sanitized value, or "?" if null
   */
  private static String sanitize(String value) {
    if (value == null) {
      return "?";
    }
    // Remove control characters and newlines to prevent log injection
    return value.replaceAll("[\\p{Cntrl}]+", "_");
  }
}
