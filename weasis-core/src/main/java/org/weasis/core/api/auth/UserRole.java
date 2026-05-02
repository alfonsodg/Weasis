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

/**
 * Defines the role hierarchy for the Weasis RBAC framework.
 *
 * <p>Each role has an associated hierarchy level that determines permission precedence. Higher levels
 * grant all permissions of lower levels.
 *
 * <p>Hierarchy levels:
 *
 * <ul>
 *   <li>{@link #ADMIN} (100) - Full system access, user management, configuration
 *   <li>{@link #RADIOLOGIST} (80) - Read/write access to studies, reporting
 *   <li>{@link #TECHNOLOGIST} (60) - Study acquisition, basic operations
 *   <li>{@link #VIEWER} (40) - Read-only access to studies
 *   <li>{@link #NONE} (0) - No access
 * </ul>
 */
public enum UserRole {

  ADMIN(100),
  RADIOLOGIST(80),
  TECHNOLOGIST(60),
  VIEWER(40),
  NONE(0);

  private final int hierarchyLevel;

  UserRole(int hierarchyLevel) {
    this.hierarchyLevel = hierarchyLevel;
  }

  /**
   * Returns the hierarchy level of this role.
   *
   * @return the numeric hierarchy level
   */
  public int getHierarchyLevel() {
    return hierarchyLevel;
  }

  /**
   * Returns whether this role is at least as permissive as the specified role.
   *
   * @param other the role to compare against
   * @return true if this role's hierarchy level is greater than or equal to the other's
   */
  public boolean implies(UserRole other) {
    if (other == null) {
      return true;
    }
    return this.hierarchyLevel >= other.hierarchyLevel;
  }

  /**
   * Parses a role name to its enum value, case-insensitively.
   *
   * @param name the role name (e.g., "admin", "RADIOLOGIST")
   * @return the matching UserRole, or {@link #NONE} if no match is found
   */
  public static UserRole fromString(String name) {
    if (name == null || name.isBlank()) {
      return NONE;
    }
    try {
      return valueOf(name.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      return NONE;
    }
  }
}
