/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.auth;

import java.util.Objects;

/**
 * Utility class that enforces RBAC permission checks in the Weasis framework.
 *
 * <p>Provides static methods for checking user permissions against required roles using the role
 * hierarchy defined in {@link UserRole}. A user with a higher-level role implicitly has all
 * permissions of lower-level roles.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Check permission, throwing an exception if not granted
 * AccessControl.checkPermission(UserRole.RADIOLOGIST, session);
 *
 * // Non-throwing check
 * if (AccessControl.hasPermission(UserRole.VIEWER, session)) {
 *     // allow read access
 * }
 *
 * // Get the user's highest role
 * UserRole effective = AccessControl.getEffectiveRole(session);
 * }</pre>
 *
 * @see UserRole
 * @see UserSession
 */
public final class AccessControl {

  private AccessControl() {}

  /**
   * Checks whether the session has at least the required role.
   *
   * <p>A user is granted access if any of their roles has a hierarchy level greater than or equal to
   * the required role's level.
   *
   * @param required the minimum role required
   * @param session the authenticated user session
   * @throws AccessDeniedException if the user does not have the required permission
   * @throws NullPointerException if either parameter is null
   */
  public static void checkPermission(UserRole required, UserSession session) {
    Objects.requireNonNull(required, "required must not be null");
    Objects.requireNonNull(session, "session must not be null");
    if (!hasPermission(required, session)) {
      throw new AccessDeniedException(
          session.getUserId(),
          required,
          getEffectiveRole(session));
    }
  }

  /**
   * Returns whether the session has at least the required role.
   *
   * <p>A user is granted access if any of their roles has a hierarchy level greater than or equal to
   * the required role's level.
   *
   * @param required the minimum role required
   * @param session the authenticated user session
   * @return true if the user has the required permission, false otherwise
   * @throws NullPointerException if either parameter is null
   */
  public static boolean hasPermission(UserRole required, UserSession session) {
    Objects.requireNonNull(required, "required must not be null");
    Objects.requireNonNull(session, "session must not be null");
    if (!session.isAuthenticated()) {
      return false;
    }
    for (UserRole role : session.getRoles()) {
      if (role.implies(required)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the highest role in the session's role set based on hierarchy level.
   *
   * @param session the authenticated user session
   * @return the highest role, or {@link UserRole#NONE} if the session has no roles
   * @throws NullPointerException if session is null
   */
  public static UserRole getEffectiveRole(UserSession session) {
    Objects.requireNonNull(session, "session must not be null");
    UserRole highest = UserRole.NONE;
    for (UserRole role : session.getRoles()) {
      if (role.getHierarchyLevel() > highest.getHierarchyLevel()) {
        highest = role;
      }
    }
    return highest;
  }

  /**
   * Exception thrown when a user attempts an operation they are not authorized to perform.
   */
  public static final class AccessDeniedException extends SecurityException {

    private final String userId;
    private final UserRole requiredRole;
    private final UserRole effectiveRole;

    /**
     * Creates an AccessDeniedException.
     *
     * @param userId the user who was denied access
     * @param requiredRole the minimum role required
     * @param effectiveRole the user's highest role
     */
    public AccessDeniedException(String userId, UserRole requiredRole, UserRole effectiveRole) {
      super(String.format(
          "Access denied for user '%s': required role %s (level %d), but effective role is %s (level %d)",
          userId,
          requiredRole,
          requiredRole.getHierarchyLevel(),
          effectiveRole,
          effectiveRole.getHierarchyLevel()));
      this.userId = userId;
      this.requiredRole = requiredRole;
      this.effectiveRole = effectiveRole;
    }

    /**
     * Returns the user who was denied access.
     *
     * @return the userId
     */
    public String getUserId() {
      return userId;
    }

    /**
     * Returns the minimum role that was required.
     *
     * @return the required role
     */
    public UserRole getRequiredRole() {
      return requiredRole;
    }

    /**
     * Returns the user's effective (highest) role.
     *
     * @return the effective role
     */
    public UserRole getEffectiveRole() {
      return effectiveRole;
    }
  }
}
