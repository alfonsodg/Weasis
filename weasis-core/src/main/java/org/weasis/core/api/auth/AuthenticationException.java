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
 * Thrown when user authentication fails in the Weasis RBAC framework.
 *
 * <p>This exception covers all authentication failures including:
 *
 * <ul>
 *   <li>Invalid user ID or password
 *   <li>User not found
 *   <li>Account locked or disabled
 *   <li>Authentication provider unavailable
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * try {
 *     UserSession session = authenticator.authenticate(userId, password);
 * } catch (AuthenticationException e) {
 *     LOGGER.warn("Login failed: {}", e.getMessage());
 *     // show error to user
 * }
 * }</pre>
 */
public class AuthenticationException extends Exception {

  /**
   * Constructs a new AuthenticationException with the specified detail message.
   *
   * @param message the detail message
   */
  public AuthenticationException(String message) {
    super(message);
  }

  /**
   * Constructs a new AuthenticationException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the root cause
   */
  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
