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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an authenticated user session in the Weasis RBAC framework.
 *
 * <p>Tracks the user's identity, assigned roles, authentication state, and session timing. Each
 * session maintains a {@code lastAccessTime} that should be updated on every user action.
 *
 * <p>Custom properties are supported via the {@link #getProperties()} map for extending session
 * data without subclassing.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * UserSession session = new UserSession(
 *     "jsmith",
 *     "John Smith",
 *     Set.of(UserRole.RADIOLOGIST, UserRole.VIEWER));
 *
 * session.updateLastAccess();
 *
 * if (AccessControl.hasPermission(UserRole.RADIOLOGIST, session)) {
 *     // perform privileged operation
 * }
 * }</pre>
 */
public final class UserSession {

  private final String userId;
  private final String displayName;
  private final Set<UserRole> roles;
  private final boolean authenticated;
  private final Instant loginTime;
  private volatile Instant lastAccessTime;
  private final Map<String, String> properties;

  /**
   * Creates a new authenticated user session.
   *
   * @param userId the unique user identifier (must not be null or blank)
   * @param displayName the human-readable display name (must not be null)
   * @param roles the set of roles assigned to this user (must not be null or empty)
   * @throws NullPointerException if any parameter is null
   * @throws IllegalArgumentException if userId is blank or roles is empty
   */
  public UserSession(String userId, String displayName, Set<UserRole> roles) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
    this.roles = Collections.unmodifiableSet(
        Objects.requireNonNull(roles, "roles must not be null"));
    if (userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    if (roles.isEmpty()) {
      throw new IllegalArgumentException("roles must not be empty");
    }
    this.authenticated = true;
    this.loginTime = Instant.now();
    this.lastAccessTime = this.loginTime;
    this.properties = new HashMap<>();
  }

  /**
   * Creates a new authenticated user session with custom properties.
   *
   * @param userId the unique user identifier
   * @param displayName the human-readable display name
   * @param roles the set of roles assigned to this user
   * @param properties initial custom properties (may be null)
   */
  public UserSession(
      String userId, String displayName, Set<UserRole> roles, Map<String, String> properties) {
    this(userId, displayName, roles);
    if (properties != null) {
      this.properties.putAll(properties);
    }
  }

  /**
   * Returns the unique user identifier.
   *
   * @return the userId
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Returns the human-readable display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the unmodifiable set of roles assigned to this user.
   *
   * @return the roles
   */
  public Set<UserRole> getRoles() {
    return roles;
  }

  /**
   * Returns whether this session is authenticated.
   *
   * @return true if the user has been authenticated
   */
  public boolean isAuthenticated() {
    return authenticated;
  }

  /**
   * Returns the time when the user logged in.
   *
   * @return the login timestamp
   */
  public Instant getLoginTime() {
    return loginTime;
  }

  /**
   * Returns the time of the last access or action by this user.
   *
   * @return the last access timestamp
   */
  public Instant getLastAccessTime() {
    return lastAccessTime;
  }

  /**
   * Updates the last access time to the current instant.
   *
   * <p>Should be called on every user action to track session activity.
   */
  public void updateLastAccess() {
    this.lastAccessTime = Instant.now();
  }

  /**
   * Returns the mutable map of custom properties for this session.
   *
   * <p>Can be used to store additional session data such as IP address, authentication source,
   * access tokens, etc.
   *
   * @return the properties map
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserSession that = (UserSession) o;
    return userId.equals(that.userId);
  }

  @Override
  public int hashCode() {
    return userId.hashCode();
  }

  @Override
  public String toString() {
    return "UserSession{"
        + "userId='"
        + userId
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + ", roles="
        + roles
        + ", authenticated="
        + authenticated
        + '}';
  }
}
