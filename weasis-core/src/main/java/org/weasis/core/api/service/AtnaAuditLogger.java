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

import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Formats DICOM ATNA (Audit Trail and Node Authentication) audit messages as RFC 3881 XML.
 *
 * <p>Generates structured audit messages conforming to:
 *
 * <ul>
 *   <li>RFC 3881 - Security Audit and Access Accountability Message XML Data Definitions for
 *       Healthcare
 *   <li>DICOM Part 15 (PS3.15) - Security and System Management Profiles
 *   <li>IHE ITI Audit Trail and Node Authentication (ATNA) Profile
 * </ul>
 *
 * <p>All messages include the required ATNA fields:
 *
 * <ul>
 *   <li>{@code EventIdentification} - EventID, EventTypeCode, EventDateTime,
 *       EventOutcomeIndicator
 *   <li>{@code ActiveParticipant} - User ID, role ID codes, network access point
 *   <li>{@code AuditSourceIdentification} - Process ID, enterprise site ID
 *   <li>{@code ParticipantObjectIdentification} - Study UID, Patient ID
 * </ul>
 *
 * <p>This class uses {@link AuditEventLogger} as the event source model. The {@link
 * #format(AuditEventLogger.AuditAction, AuditEventLogger.AuditResult, String, String, String)}
 * method maps {@link AuditEventLogger.AuditAction} codes to the corresponding DICOM EventID and
 * EventTypeCode values.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Format a study access event
 * String xml = AtnaAuditLogger.studyAccessed("patient-123", "1.2.3.4.5.6.7.8", "jsmith");
 *
 * // General mapping from AuditEventLogger
 * String xml = AtnaAuditLogger.format(
 *     AuditAction.VIEW, AuditResult.SUCCESS,
 *     "patient-123", "1.2.3.4.5.6.7.8", "jsmith");
 * }</pre>
 *
 * <p>XML is generated using only {@link javax.xml.parsers.DocumentBuilderFactory} and standard JDK
 * APIs, requiring no external dependencies.
 */
public final class AtnaAuditLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(AtnaAuditLogger.class);

  // -- DICOM Coding Scheme --
  private static final String CODE_SYSTEM_DCM = "DCM";
  private static final String CODE_SYSTEM_EVENT_ID = "DCM";

  // -- Event Outcome Indicators (RFC 3881 Section 5.2) --
  /** Event completed successfully. */
  public static final int OUTCOME_SUCCESS = 0;

  /** Minor failure (e.g., access denied by policy). */
  public static final int OUTCOME_MINOR_FAILURE = 4;

  /** Serious failure. */
  public static final int OUTCOME_SERIOUS_FAILURE = 8;

  /** Major failure (e.g., system error). */
  public static final int OUTCOME_MAJOR_FAILURE = 12;

  // -- DICOM EventID Codes (DICOM PS3.15 Table A.1-1) --
  /** EventID: Patient record has been accessed. */
  public static final String EVENT_STUDY_ACCESSED = "110100";

  /** EventID: Study has been exported. */
  public static final String EVENT_STUDY_EXPORTED = "110101";

  /** EventID: Study has been printed. */
  public static final String EVENT_STUDY_PRINTED = "110103";

  /** EventID: Study has been deleted. */
  public static final String EVENT_STUDY_DELETED = "110104";

  /** EventID: Data has been exported (general). */
  public static final String EVENT_EXPORT = "110105";

  /** EventID: Data has been imported. */
  public static final String EVENT_IMPORT = "110106";

  /** EventID: User authentication event. */
  public static final String EVENT_USER_AUTHENTICATION = "110114";

  /** EventID: Application has started. */
  public static final String EVENT_APPLICATION_START = "110130";

  /** EventID: Application has stopped. */
  public static final String EVENT_APPLICATION_STOP = "110131";

  // -- DICOM EventTypeCode Codes (DICOM PS3.15 Table A.1-2) --
  /** EventTypeCode: User has been authenticated. */
  public static final String EVENT_TYPE_USER_AUTHENTICATED = "110120";

  /** EventTypeCode: User authentication has failed. */
  public static final String EVENT_TYPE_USER_AUTH_FAILED = "110121";

  /** EventTypeCode: User has logged out. */
  public static final String EVENT_TYPE_USER_LOGOUT = "110122";

  // -- Role ID Codes (DICOM PS3.15 Table A.1-3) --
  /** RoleIDCode: Application role. */
  public static final String ROLE_APPLICATION = "110150";

  /** RoleIDCode: Application launcher role. */
  public static final String ROLE_APPLICATION_LAUNCHER = "110151";

  /** RoleIDCode: Destination role (for export). */
  public static final String ROLE_DESTINATION = "110152";

  /** RoleIDCode: Source role (for import). */
  public static final String ROLE_SOURCE = "110154";

  /** RoleIDCode: Human user role. */
  public static final String ROLE_USER = "110156";

  // -- Network Access Point Type Codes --
  /** NetworkAccessPointTypeCode: Machine (DNS name). */
  public static final String NET_TYPE_DNS_NAME = "1";

  /** NetworkAccessPointTypeCode: IP address. */
  public static final String NET_TYPE_IP_ADDRESS = "2";

  /** NetworkAccessPointTypeCode: Telephone number. */
  public static final String NET_TYPE_TELEPHONE = "3";

  // -- Participant Object Type Codes --
  /** ParticipantObjectTypeCode: Person. */
  public static final String OBJ_TYPE_PERSON = "1";

  /** ParticipantObjectTypeCode: System object. */
  public static final String OBJ_TYPE_SYSTEM = "2";

  /** ParticipantObjectTypeCode: Organization. */
  public static final String OBJ_TYPE_ORGANIZATION = "3";

  /** ParticipantObjectTypeCode: Other. */
  public static final String OBJ_TYPE_OTHER = "4";

  // -- Participant Object Type Code Roles --
  /** ParticipantObjectTypeCodeRole: Patient. */
  public static final String OBJ_ROLE_PATIENT = "1";

  /** ParticipantObjectTypeCodeRole: Report. */
  public static final String OBJ_ROLE_REPORT = "3";

  /** ParticipantObjectTypeCodeRole: User. */
  public static final String OBJ_ROLE_USER = "6";

  /** ParticipantObjectTypeCodeRole: Study. */
  public static final String OBJ_ROLE_STUDY = "15";

  // -- Participant Object Data Life Cycle --
  /** ParticipantObjectDataLifeCycle: Access (used when study is accessed). */
  public static final String OBJ_LIFE_CYCLE_ACCESS = "6";

  /** ParticipantObjectDataLifeCycle: Export (used when study is exported). */
  public static final String OBJ_LIFE_CYCLE_EXPORT = "8";

  /** ParticipantObjectDataLifeCycle: Print (used when study is printed). */
  public static final String OBJ_LIFE_CYCLE_PRINT = "9";

  /** ParticipantObjectDataLifeCycle: Deletion (used when study is deleted). */
  public static final String OBJ_LIFE_CYCLE_DELETION = "11";

  /** ISO 8601 date-time formatter for ATNA timestamps. */
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneOffset.UTC);

  private static String localHostName;
  private static String localHostAddress;
  private static String processId;
  private static String auditSourceId;
  private static String auditEnterpriseSiteId;

  static {
    resolveLocalHost();
    processId = resolveProcessId();
    auditSourceId = "Weasis";
    auditEnterpriseSiteId = null;
  }

  private AtnaAuditLogger() {}

  /**
   * Sets the audit source identifier (default: "Weasis").
   *
   * @param sourceId the audit source ID; if null, reverts to default
   */
  public static void setAuditSourceId(String sourceId) {
    auditSourceId = sourceId != null ? sourceId : "Weasis";
  }

  /**
   * Sets the enterprise site identifier for the audit source.
   *
   * <p>This is typically the healthcare facility name or code (e.g., "GeneralHospital").
   *
   * @param siteId the enterprise site ID; may be null to omit
   */
  public static void setAuditEnterpriseSiteId(String siteId) {
    auditEnterpriseSiteId = siteId;
  }

  /**
   * Returns the audit source identifier currently configured.
   *
   * @return the audit source ID
   */
  public static String getAuditSourceId() {
    return auditSourceId;
  }

  /**
   * Returns the enterprise site identifier currently configured.
   *
   * @return the enterprise site ID, may be null
   */
  public static String getAuditEnterpriseSiteId() {
    return auditEnterpriseSiteId;
  }

  // ========================================================================
  // Public API: Convenience methods for each supported event type
  // ========================================================================

  /**
   * Formats a Study Accessed audit event.
   *
   * @param patientId the patient identifier
   * @param studyUid the DICOM Study Instance UID
   * @param userId the user who accessed the study
   * @return RFC 3881 XML audit message, or null on error
   * @throws NullPointerException if any parameter is null
   */
  public static String studyAccessed(String patientId, String studyUid, String userId) {
    Objects.requireNonNull(patientId, "patientId");
    Objects.requireNonNull(studyUid, "studyUid");
    Objects.requireNonNull(userId, "userId");
    return buildStudyEvent(
        EVENT_STUDY_ACCESSED,
        "Study Accessed",
        patientId,
        studyUid,
        userId,
        OUTCOME_SUCCESS,
        OBJ_LIFE_CYCLE_ACCESS);
  }

  /**
   * Formats a Study Exported audit event.
   *
   * @param patientId the patient identifier
   * @param studyUid the DICOM Study Instance UID
   * @param userId the user who exported the study
   * @return RFC 3881 XML audit message, or null on error
   * @throws NullPointerException if any parameter is null
   */
  public static String studyExported(String patientId, String studyUid, String userId) {
    Objects.requireNonNull(patientId, "patientId");
    Objects.requireNonNull(studyUid, "studyUid");
    Objects.requireNonNull(userId, "userId");
    return buildStudyEvent(
        EVENT_STUDY_EXPORTED,
        "Study Exported",
        patientId,
        studyUid,
        userId,
        OUTCOME_SUCCESS,
        OBJ_LIFE_CYCLE_EXPORT);
  }

  /**
   * Formats a Study Printed audit event.
   *
   * @param patientId the patient identifier
   * @param studyUid the DICOM Study Instance UID
   * @param userId the user who printed the study
   * @return RFC 3881 XML audit message, or null on error
   * @throws NullPointerException if any parameter is null
   */
  public static String studyPrinted(String patientId, String studyUid, String userId) {
    Objects.requireNonNull(patientId, "patientId");
    Objects.requireNonNull(studyUid, "studyUid");
    Objects.requireNonNull(userId, "userId");
    return buildStudyEvent(
        EVENT_STUDY_PRINTED,
        "Study Printed",
        patientId,
        studyUid,
        userId,
        OUTCOME_SUCCESS,
        OBJ_LIFE_CYCLE_PRINT);
  }

  /**
   * Formats a Study Deleted audit event.
   *
   * @param patientId the patient identifier
   * @param studyUid the DICOM Study Instance UID
   * @param userId the user who deleted the study
   * @return RFC 3881 XML audit message, or null on error
   * @throws NullPointerException if any parameter is null
   */
  public static String studyDeleted(String patientId, String studyUid, String userId) {
    Objects.requireNonNull(patientId, "patientId");
    Objects.requireNonNull(studyUid, "studyUid");
    Objects.requireNonNull(userId, "userId");
    return buildStudyEvent(
        EVENT_STUDY_DELETED,
        "Study Deleted",
        patientId,
        studyUid,
        userId,
        OUTCOME_SUCCESS,
        OBJ_LIFE_CYCLE_DELETION);
  }

  /**
   * Formats a User Authenticated audit event.
   *
   * <p>Sets EventTypeCode to "110120" (User Authenticated) on success, or "110121" (User
   * Authentication Failed) on failure.
   *
   * @param userId the user who authenticated
   * @param result the authentication result
   * @return RFC 3881 XML audit message, or null on error
   * @throws NullPointerException if any parameter is null
   */
  public static String userAuthenticated(String userId, AuditEventLogger.AuditResult result) {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(result, "result");
    int outcome = toOutcome(result);
    String eventTypeCode;
    String eventTypeDisplayName;
    if (result == AuditEventLogger.AuditResult.SUCCESS) {
      eventTypeCode = EVENT_TYPE_USER_AUTHENTICATED;
      eventTypeDisplayName = "User Authenticated";
    } else {
      // Both FAILURE and DENIED produce failure event type
      eventTypeCode = EVENT_TYPE_USER_AUTH_FAILED;
      eventTypeDisplayName = "User Authentication Failed";
    }
    try {
      Document doc = createDocument();
      Element root = createRoot(doc);
      appendEventIdentification(doc, root, EVENT_USER_AUTHENTICATION, "User Authentication",
          eventTypeCode, eventTypeDisplayName, outcome);
      appendActiveParticipant(doc, root, userId, ROLE_USER, true);
      appendActiveParticipant(doc, root, auditSourceId, ROLE_APPLICATION, false);
      appendAuditSourceIdentification(doc, root);
      return documentToString(doc);
    } catch (ParserConfigurationException e) {
      LOGGER.error("Failed to build ATNA audit document", e);
      return null;
    }
  }

  /**
   * Formats an Application Start audit event.
   *
   * @param userId the user who launched the application
   * @return RFC 3881 XML audit message, or null on error
   * @throws NullPointerException if userId is null
   */
  public static String applicationStart(String userId) {
    Objects.requireNonNull(userId, "userId");
    try {
      Document doc = createDocument();
      Element root = createRoot(doc);
      appendEventIdentification(doc, root, EVENT_APPLICATION_START, "Application Start",
          null, null, OUTCOME_SUCCESS);
      appendActiveParticipant(doc, root, auditSourceId, ROLE_APPLICATION, false);
      appendAuditSourceIdentification(doc, root);
      return documentToString(doc);
    } catch (ParserConfigurationException e) {
      LOGGER.error("Failed to build ATNA audit document", e);
      return null;
    }
  }

  // ========================================================================
  // General-purpose format method
  // ========================================================================

  /**
   * Formats an audit event from {@link AuditEventLogger} parameters.
   *
   * <p>Maps the {@link AuditEventLogger.AuditAction} to the corresponding DICOM EventID code:
   *
   * <table border="1">
   *   <caption>Action-to-EventID mapping</caption>
   *   <tr><th>AuditAction</th><th>EventID</th><th>Display Name</th></tr>
   *   <tr><td>VIEW</td><td>110100</td><td>Study Accessed</td></tr>
   *   <tr><td>EXPORT</td><td>110101</td><td>Study Exported</td></tr>
   *   <tr><td>PRINT</td><td>110103</td><td>Study Printed</td></tr>
   *   <tr><td>IMPORT</td><td>110106</td><td>Import</td></tr>
   *   <tr><td>DELETE</td><td>110104</td><td>Study Deleted</td></tr>
   *   <tr><td>LOGIN</td><td>110114</td><td>User Authentication</td></tr>
   *   <tr><td>LOGOUT</td><td>110114</td><td>User Authentication</td></tr>
   *   <tr><td>CONFIGURE</td><td>110130</td><td>Application Start</td></tr>
   *   <tr><td>LAUNCH</td><td>110130</td><td>Application Start</td></tr>
   * </table>
   *
   * <p>For LOGIN events, the EventTypeCode is set to "110120" (User Authenticated) when the result
   * is SUCCESS, or "110121" (User Authentication Failed) otherwise. For LOGOUT events, the
   * EventTypeCode is set to "110122" (User Logged out).
   *
   * @param action the audit action (maps to DICOM EventID)
   * @param result the audit result (maps to EventOutcomeIndicator)
   * @param patientId the patient identifier (may be null for non-study events)
   * @param studyUid the DICOM Study Instance UID (may be null for non-study events)
   * @param userId the user performing the action
   * @return RFC 3881 XML audit message, or null on error
   * @throws NullPointerException if action, result, or userId is null
   */
  public static String format(
      AuditEventLogger.AuditAction action,
      AuditEventLogger.AuditResult result,
      String patientId,
      String studyUid,
      String userId) {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(userId, "userId");
    try {
      return buildFromAction(action, result, patientId, studyUid, userId);
    } catch (ParserConfigurationException e) {
      LOGGER.error("Failed to build ATNA audit document from action {}", action, e);
      return null;
    }
  }

  // ========================================================================
  // Internal builders
  // ========================================================================

  /**
   * Builds an audit event from an {@link AuditEventLogger.AuditAction}.
   *
   * @param action the action to map
   * @param result the audit result
   * @param patientId the patient ID (may be null)
   * @param studyUid the study UID (may be null)
   * @param userId the user ID
   * @return the XML document as string
   */
  private static String buildFromAction(
      AuditEventLogger.AuditAction action,
      AuditEventLogger.AuditResult result,
      String patientId,
      String studyUid,
      String userId)
      throws ParserConfigurationException {
    int outcome = toOutcome(result);
    String eventId;
    String eventDisplayName;
    String eventTypeCode = null;
    String eventTypeDisplayName = null;

    switch (action) {
      case VIEW -> {
        eventId = EVENT_STUDY_ACCESSED;
        eventDisplayName = "Study Accessed";
      }
      case EXPORT -> {
        eventId = EVENT_STUDY_EXPORTED;
        eventDisplayName = "Study Exported";
      }
      case PRINT -> {
        eventId = EVENT_STUDY_PRINTED;
        eventDisplayName = "Study Printed";
      }
      case DELETE -> {
        eventId = EVENT_STUDY_DELETED;
        eventDisplayName = "Study Deleted";
      }
      case IMPORT -> {
        eventId = EVENT_IMPORT;
        eventDisplayName = "Import";
      }
      case LOGIN -> {
        eventId = EVENT_USER_AUTHENTICATION;
        eventDisplayName = "User Authentication";
        if (result == AuditEventLogger.AuditResult.SUCCESS) {
          eventTypeCode = EVENT_TYPE_USER_AUTHENTICATED;
          eventTypeDisplayName = "User Authenticated";
        } else {
          eventTypeCode = EVENT_TYPE_USER_AUTH_FAILED;
          eventTypeDisplayName = "User Authentication Failed";
        }
      }
      case LOGOUT -> {
        eventId = EVENT_USER_AUTHENTICATION;
        eventDisplayName = "User Authentication";
        eventTypeCode = EVENT_TYPE_USER_LOGOUT;
        eventTypeDisplayName = "User Logged out";
      }
      case CONFIGURE, LAUNCH -> {
        eventId = EVENT_APPLICATION_START;
        eventDisplayName = "Application Start";
      }
      default -> {
        eventId = EVENT_STUDY_ACCESSED;
        eventDisplayName = "Study Accessed";
      }
    }

    Document doc = createDocument();
    Element root = createRoot(doc);
    appendEventIdentification(doc, root, eventId, eventDisplayName, eventTypeCode,
        eventTypeDisplayName, outcome);

    // Add participants depending on event type
    boolean isAuthEvent = action == AuditEventLogger.AuditAction.LOGIN
        || action == AuditEventLogger.AuditAction.LOGOUT;
    boolean isAppEvent = action == AuditEventLogger.AuditAction.CONFIGURE
        || action == AuditEventLogger.AuditAction.LAUNCH;

    if (isAppEvent) {
      appendActiveParticipant(doc, root, auditSourceId, ROLE_APPLICATION, false);
    } else if (isAuthEvent) {
      appendActiveParticipant(doc, root, userId, ROLE_USER, true);
      appendActiveParticipant(doc, root, auditSourceId, ROLE_APPLICATION, false);
    } else {
      // Study-related events
      appendActiveParticipant(doc, root, userId, ROLE_USER, true);
      appendActiveParticipant(doc, root, auditSourceId, ROLE_APPLICATION, false);
    }

    appendAuditSourceIdentification(doc, root);

    // Study-related Participant Objects
    if (!isAuthEvent && !isAppEvent) {
      if (patientId != null) {
        appendPatientObject(doc, root, patientId);
      }
      if (studyUid != null) {
        appendStudyObject(doc, root, studyUid);
      }
    }

    return documentToString(doc);
  }

  /**
   * Builds a study-related audit event.
   *
   * @param eventId the DICOM EventID code
   * @param eventDisplayName the human-readable event display name
   * @param patientId the patient identifier
   * @param studyUid the DICOM Study Instance UID
   * @param userId the user performing the action
   * @param outcome the event outcome indicator
   * @param dataLifeCycle the participant object data lifecycle value
   * @return the XML document as string
   */
  private static String buildStudyEvent(
      String eventId,
      String eventDisplayName,
      String patientId,
      String studyUid,
      String userId,
      int outcome,
      String dataLifeCycle) {
    try {
      Document doc = createDocument();
      Element root = createRoot(doc);
      appendEventIdentification(doc, root, eventId, eventDisplayName, null, null, outcome);
      appendActiveParticipant(doc, root, userId, ROLE_USER, true);
      appendActiveParticipant(doc, root, auditSourceId, ROLE_APPLICATION, false);
      appendAuditSourceIdentification(doc, root);
      appendPatientObject(doc, root, patientId);
      appendStudyObject(doc, root, studyUid);
      return documentToString(doc);
    } catch (ParserConfigurationException e) {
      LOGGER.error("Failed to build ATNA audit document for event {}", eventId, e);
      return null;
    }
  }

  // ========================================================================
  // XML element construction
  // ========================================================================

  /**
   * Creates a new XML document using {@link DocumentBuilderFactory}.
   *
   * @return a new empty Document
   */
  private static Document createDocument() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    return factory.newDocumentBuilder().newDocument();
  }

  /**
   * Creates the root {@code AuditMessage} element.
   *
   * @param doc the document
   * @return the root element
   */
  private static Element createRoot(Document doc) {
    Element root = doc.createElement("AuditMessage");
    doc.appendChild(root);
    return root;
  }

  /**
   * Appends the {@code EventIdentification} element.
   *
   * @param doc the document
   * @param parent the parent element
   * @param eventId the EventID code
   * @param eventDisplayName the EventID display name
   * @param eventTypeCode optional EventTypeCode (may be null)
   * @param eventTypeDisplayName optional EventTypeCode display name (may be null)
   * @param outcome the EventOutcomeIndicator value
   */
  private static void appendEventIdentification(
      Document doc, Element parent, String eventId, String eventDisplayName,
      String eventTypeCode, String eventTypeDisplayName, int outcome) {
    Element elem = doc.createElement("EventIdentification");
    appendCode(doc, elem, "EventID", eventId, eventDisplayName, CODE_SYSTEM_EVENT_ID);
    if (eventTypeCode != null) {
      appendCode(doc, elem, "EventTypeCode", eventTypeCode,
          eventTypeDisplayName, CODE_SYSTEM_EVENT_ID);
    }
    appendTextElement(doc, elem, "EventDateTime", DATE_TIME_FORMATTER.format(Instant.now()));
    appendTextElement(doc, elem, "EventOutcomeIndicator", String.valueOf(outcome));
    parent.appendChild(elem);
  }

  /**
   * Appends an {@code ActiveParticipant} element.
   *
   * @param doc the document
   * @param parent the parent element
   * @param userId the UserID value
   * @param roleCode the RoleIDCode (e.g., ROLE_USER, ROLE_APPLICATION)
   * @param isRequestor whether this participant is the requestor
   */
  private static void appendActiveParticipant(
      Document doc, Element parent, String userId, String roleCode, boolean isRequestor) {
    Element elem = doc.createElement("ActiveParticipant");
    elem.setAttribute("UserID", userId);
    elem.setAttribute("AlternativeUserID", processId);
    elem.setAttribute("UserIsRequestor", String.valueOf(isRequestor));
    String roleDisplayName = ROLE_USER.equals(roleCode) ? "User" : "Application";
    appendCode(doc, elem, "RoleIDCode", roleCode, roleDisplayName, CODE_SYSTEM_DCM);
    appendTextElement(doc, elem, "NetworkAccessPointID", localHostName);
    appendTextElement(doc, elem, "NetworkAccessPointTypeCode", NET_TYPE_DNS_NAME);
    parent.appendChild(elem);
  }

  /**
   * Appends the {@code AuditSourceIdentification} element.
   *
   * @param doc the document
   * @param parent the parent element
   */
  private static void appendAuditSourceIdentification(Document doc, Element parent) {
    Element elem = doc.createElement("AuditSourceIdentification");
    appendTextElement(doc, elem, "AuditSourceID", auditSourceId);
    if (auditEnterpriseSiteId != null) {
      appendTextElement(doc, elem, "AuditEnterpriseSiteID", auditEnterpriseSiteId);
    }
    parent.appendChild(elem);
  }

  /**
   * Appends a patient {@code ParticipantObjectIdentification} element.
   *
   * @param doc the document
   * @param parent the parent element
   * @param patientId the patient identifier
   */
  private static void appendPatientObject(Document doc, Element parent, String patientId) {
    Element elem = doc.createElement("ParticipantObjectIdentification");
    appendTextElement(doc, elem, "ParticipantObjectID", patientId);
    appendTextElement(doc, elem, "ParticipantObjectTypeCode", OBJ_TYPE_PERSON);
    appendTextElement(doc, elem, "ParticipantObjectTypeCodeRole", OBJ_ROLE_PATIENT);
    parent.appendChild(elem);
  }

  /**
   * Appends a study {@code ParticipantObjectIdentification} element.
   *
   * @param doc the document
   * @param parent the parent element
   * @param studyUid the DICOM Study Instance UID
   */
  private static void appendStudyObject(Document doc, Element parent, String studyUid) {
    Element elem = doc.createElement("ParticipantObjectIdentification");
    appendTextElement(doc, elem, "ParticipantObjectID", studyUid);
    appendTextElement(doc, elem, "ParticipantObjectTypeCode", OBJ_TYPE_SYSTEM);
    appendTextElement(doc, elem, "ParticipantObjectTypeCodeRole", OBJ_ROLE_STUDY);
    // ParticipantObjectIDTypeCode: Study Instance UID
    appendCode(doc, elem, "ParticipantObjectIDTypeCode", "110180",
        "Study Instance UID", "DCM");
    parent.appendChild(elem);
  }

  /**
   * Appends a code element with {@code code}, {@code displayName}, and {@code codeSystemName}
   * attributes.
   *
   * @param doc the document
   * @param parent the parent element
   * @param tagName the element name
   * @param code the code value
   * @param displayName the human-readable display name
   * @param codeSystemName the coding scheme name
   */
  private static void appendCode(
      Document doc, Element parent, String tagName,
      String code, String displayName, String codeSystemName) {
    Element elem = doc.createElement(tagName);
    elem.setAttribute("code", code);
    elem.setAttribute("displayName", displayName);
    elem.setAttribute("codeSystemName", codeSystemName);
    parent.appendChild(elem);
  }

  /**
   * Appends a simple text element.
   *
   * @param doc the document
   * @param parent the parent element
   * @param tagName the element name
   * @param textValue the text content
   */
  private static void appendTextElement(Document doc, Element parent, String tagName,
      String textValue) {
    Element elem = doc.createElement(tagName);
    elem.setTextContent(textValue);
    parent.appendChild(elem);
  }

  /**
   * Serializes the XML document to a formatted string.
   *
   * @param doc the document to serialize
   * @return the XML string, or null on error
   */
  private static String documentToString(Document doc) {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(doc), new StreamResult(writer));
      return writer.toString();
    } catch (TransformerException e) {
      LOGGER.error("Failed to serialize ATNA XML document", e);
      return null;
    }
  }

  // ========================================================================
  // Mapping helpers
  // ========================================================================

  /**
   * Maps {@link AuditEventLogger.AuditResult} to RFC 3881 EventOutcomeIndicator values.
   *
   * <ul>
   *   <li>{@code SUCCESS} → {@link #OUTCOME_SUCCESS} (0)
   *   <li>{@code DENIED} → {@link #OUTCOME_MINOR_FAILURE} (4)
   *   <li>{@code FAILURE} → {@link #OUTCOME_MAJOR_FAILURE} (12)
   * </ul>
   *
   * @param result the audit result
   * @return the numeric outcome indicator
   */
  public static int toOutcome(AuditEventLogger.AuditResult result) {
    return switch (result) {
      case SUCCESS -> OUTCOME_SUCCESS;
      case DENIED -> OUTCOME_MINOR_FAILURE;
      case FAILURE -> OUTCOME_MAJOR_FAILURE;
    };
  }

  /**
   * Resolves local host name and IP address.
   *
   * <p>Falls back to "unknown" and "127.0.0.1" on failure.
   */
  private static void resolveLocalHost() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      localHostName = localHost.getHostName();
      localHostAddress = localHost.getHostAddress();
    } catch (UnknownHostException e) {
      localHostName = "unknown";
      localHostAddress = "127.0.0.1";
      LOGGER.warn("Cannot resolve local host name for ATNA audit messages", e);
    }
  }

  /**
   * Resolves the JVM process identifier.
   *
   * @return the process ID (e.g., "12345@hostname")
   */
  private static String resolveProcessId() {
    String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    return runtimeName != null ? runtimeName : "unknown";
  }
}
