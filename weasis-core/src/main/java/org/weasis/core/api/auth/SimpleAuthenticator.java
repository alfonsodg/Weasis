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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple file-based authenticator for the Weasis RBAC framework.
 *
 * <p>Users are configured in a standard Java {@link Properties} file. The file format is:
 *
 * <pre>{@code
 * username=SHA-256(password):role[,role2,...]
 *
 * # Example:
 * admin=a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3:ADMIN
 * radiologist=...:RADIOLOGIST,TECHNOLOGIST
 * viewer=...:VIEWER
 * }</pre>
 *
 * <p>Passwords are stored as SHA-256 hexadecimal hashes. A default admin user (admin/admin with
 * ADMIN role) is preconfigured when loading the built-in {@code config/users.properties} resource.
 *
 * <p>This class is thread-safe after construction. The user map is populated once at construction
 * time and served as an unmodifiable snapshot.
 */
public class SimpleAuthenticator implements Authenticator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAuthenticator.class);

  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String PROPERTIES_RESOURCE = "config/users.properties";
  private static final String DEFAULT_ADMIN_USER = "admin";
  private static final String DEFAULT_ADMIN_PASSWORD = "admin";
  private static final UserRole DEFAULT_ADMIN_ROLE = UserRole.ADMIN;

  private final java.util.Map<String, UserEntry> users;

  /**
   * Creates a SimpleAuthenticator loading users from the built-in {@code config/users.properties}
   * resource on the classpath.
   *
   * <p>If the resource is not found, a default admin user (admin/admin as ADMIN) is created.
   */
  public SimpleAuthenticator() {
    this.users = loadUsers();
  }

  /**
   * Creates a SimpleAuthenticator loading users from the specified properties file path.
   *
   * @param usersPath the path to the users properties file
   * @throws IOException if the file cannot be read
   */
  public SimpleAuthenticator(Path usersPath) throws IOException {
    this.users = loadUsers(usersPath);
  }

  /**
   * Creates a SimpleAuthenticator with a preconfigured user map.
   *
   * @param users the user entries (must not be null)
   */
  SimpleAuthenticator(java.util.Map<String, UserEntry> users) {
    this.users = Collections.unmodifiableMap(
        Objects.requireNonNull(users, "users must not be null"));
  }

  @Override
  public UserSession authenticate(String userId, String password) throws AuthenticationException {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(password, "password must not be null");

    if (userId.isBlank()) {
      throw new AuthenticationException("User ID must not be blank");
    }

    UserEntry entry = users.get(userId.trim());
    if (entry == null) {
      throw new AuthenticationException("User not found: " + userId);
    }

    String passwordHash = hashPassword(password);
    if (!entry.passwordHash.equals(passwordHash)) {
      throw new AuthenticationException("Invalid password for user: " + userId);
    }

    return new UserSession(userId.trim(), entry.displayName, entry.roles);
  }

  @Override
  public void logout(UserSession session) {
    Objects.requireNonNull(session, "session must not be null");
    LOGGER.info("User logged out: {}", session.getUserId());
  }

  @Override
  public boolean supportsPasswordChange() {
    return false;
  }

  /**
   * Hashes a password using SHA-256 and returns the hexadecimal representation.
   *
   * @param password the plain-text password
   * @return the SHA-256 hex hash
   */
  public static String hashPassword(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /**
   * Loads user entries from the built-in classpath resource.
   *
   * @return an unmodifiable map of userId to UserEntry
   */
  private static java.util.Map<String, UserEntry> loadUsers() {
    try (InputStream in = SimpleAuthenticator.class
        .getClassLoader()
        .getResourceAsStream(PROPERTIES_RESOURCE)) {
      if (in != null) {
        return loadUsers(in);
      }
    } catch (IOException e) {
      LOGGER.warn("Cannot load users from resource {}", PROPERTIES_RESOURCE, e);
    }
    LOGGER.warn("Users resource {} not found, creating default admin user", PROPERTIES_RESOURCE);
    return createDefaultUserMap();
  }

  /**
   * Loads user entries from a properties file path.
   *
   * @param path the path to the properties file
   * @return an unmodifiable map of userId to UserEntry
   * @throws IOException if the file cannot be read
   */
  private static java.util.Map<String, UserEntry> loadUsers(Path path) throws IOException {
    try (InputStream in = Files.newInputStream(path)) {
      return loadUsers(in);
    }
  }

  /**
   * Parses a properties input stream into a user map.
   *
   * <p>Format per property: {@code passwordHash:displayName:role[,role2]}
   *
   * @param in the input stream containing the properties
   * @return an unmodifiable map of userId to UserEntry
   */
  private static java.util.Map<String, UserEntry> loadUsers(InputStream in) throws IOException {
    Properties props = new Properties();
    props.load(in);
    java.util.Map<String, UserEntry> map = new java.util.LinkedHashMap<>();
    for (String key : props.stringPropertyNames()) {
      String value = props.getProperty(key);
      if (value == null || value.isBlank()) {
        LOGGER.warn("Skipping user '{}': empty value", key);
        continue;
      }
      try {
        UserEntry entry = parseUserEntry(value.trim());
        map.put(key.trim(), entry);
      } catch (IllegalArgumentException e) {
        LOGGER.warn("Skipping user '{}': {}", key, e.getMessage());
      }
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * Parses a single user entry from the value string.
   *
   * <p>Format: {@code passwordHash:displayName:role[,role2]}
   *
   * @param value the property value
   * @return the parsed UserEntry
   */
  static UserEntry parseUserEntry(String value) {
    String[] parts = value.split(":", 3);
    if (parts.length < 3) {
      throw new IllegalArgumentException(
          "Invalid format, expected passwordHash:displayName:role[,role2]");
    }
    String passwordHash = parts[0].trim();
    String displayName = parts[1].trim();
    String rolesStr = parts[2].trim();

    if (passwordHash.isEmpty()) {
      throw new IllegalArgumentException("Password hash must not be empty");
    }
    if (displayName.isEmpty()) {
      displayName = parts[0]; // fallback to userId-like display
    }
    if (rolesStr.isEmpty()) {
      throw new IllegalArgumentException("At least one role must be specified");
    }

    Set<UserRole> roles = Arrays.stream(rolesStr.split(","))
        .map(String::trim)
        .map(UserRole::fromString)
        .filter(r -> r != UserRole.NONE)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));

    if (roles.isEmpty()) {
      throw new IllegalArgumentException("No valid roles found in: " + rolesStr);
    }

    return new UserEntry(passwordHash, displayName, roles);
  }

  /**
   * Creates the default map with a single admin user.
   *
   * @return an unmodifiable map containing the default admin user
   */
  private static java.util.Map<String, UserEntry> createDefaultUserMap() {
    String adminHash = hashPassword(DEFAULT_ADMIN_PASSWORD);
    UserEntry adminEntry = new UserEntry(
        adminHash,
        DEFAULT_ADMIN_USER,
        EnumSet.of(DEFAULT_ADMIN_ROLE));
    return Collections.singletonMap(DEFAULT_ADMIN_USER, adminEntry);
  }

  /**
   * Internal record representing a configured user entry.
   */
  static final class UserEntry {
    final String passwordHash;
    final String displayName;
    final Set<UserRole> roles;

    UserEntry(String passwordHash, String displayName, Set<UserRole> roles) {
      this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
      this.displayName = Objects.requireNonNull(displayName, "displayName");
      this.roles = Collections.unmodifiableSet(
          Objects.requireNonNull(roles, "roles"));
    }
  }
}
