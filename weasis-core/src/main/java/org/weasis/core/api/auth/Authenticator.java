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
 * Defines the authentication contract for the Weasis RBAC framework.
 *
 * <p>Implementations provide user authentication against various backends (file-based, LDAP, OAuth,
 * etc.). The primary method is {@link #authenticate(String, String)}, which returns a fully
 * populated {@link UserSession} on success or throws an {@link AuthenticationException} on failure.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * Authenticator authenticator = new SimpleAuthenticator(usersFile);
 * try {
 *     UserSession session = authenticator.authenticate("jsmith", "password");
 *     // session is now ready for use in AccessControl checks
 * } catch (AuthenticationException e) {
 *     // handle failed login
 * }
 * }</pre>
 *
 * @see SimpleAuthenticator
 * @see UserSession
 * @see AuthenticationException
 */
public interface Authenticator {

  /**
   * Authenticates a user with the given credentials.
   *
   * <p>Returns a fully initialized {@link UserSession} on successful authentication. The session
   * includes the user's assigned roles and a login timestamp.
   *
   * @param userId the user identifier (must not be null or blank)
   * @param password the user's password (must not be null)
   * @return a fully initialized authenticated UserSession
   * @throws AuthenticationException if authentication fails (invalid credentials, user not found,
   *         account locked, etc.)
   * @throws NullPointerException if either parameter is null
   */
  UserSession authenticate(String userId, String password) throws AuthenticationException;

  /**
   * Logs out the given session, invalidating it.
   *
   * <p>Implementations should perform any necessary cleanup such as invalidating tokens, updating
   * audit logs, or releasing resources associated with the session.
   *
   * @param session the session to log out
   * @throws NullPointerException if session is null
   */
  void logout(UserSession session);

  /**
   * Returns whether this authenticator supports password changes.
   *
   * @return true if password changes are supported, false otherwise
   */
  boolean supportsPasswordChange();
}
