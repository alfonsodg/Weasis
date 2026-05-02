/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides AES-256-GCM encryption for data at rest.
 *
 * <p>This class enables encrypting DICOM data before writing to disk and decrypting when reading.
 * The encryption key can be derived from application configuration or provided externally.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Encrypt output stream
 * try (OutputStream encrypted = CipherProvider.encryptStream(outputStream, key)) {
 *     // write plaintext data
 * }
 *
 * // Decrypt input stream
 * try (InputStream decrypted = CipherProvider.decryptStream(inputStream, key)) {
 *     // read plaintext data
 * }
 * }</pre>
 */
public final class CipherProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(CipherProvider.class);

  private static final String ALGORITHM = "AES";
  private static final String CIPHER_MODE = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int GCM_IV_LENGTH = 12;
  private static final int KEY_SIZE = 256;

  private CipherProvider() {}

  /**
   * Generates a new random AES-256 key.
   *
   * @return a new SecretKey
   */
  public static SecretKey generateKey() {
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
      keyGen.init(KEY_SIZE, new SecureRandom());
      return keyGen.generateKey();
    } catch (Exception e) {
      LOGGER.error("Cannot generate AES key", e);
      throw new IllegalStateException("Encryption not available", e);
    }
  }

  /**
   * Creates a SecretKey from a Base64-encoded string.
   *
   * @param encoded Base64-encoded key
   * @return the SecretKey
   */
  public static SecretKey fromBase64(String encoded) {
    byte[] decoded = Base64.getDecoder().decode(encoded);
    return new SecretKeySpec(decoded, ALGORITHM);
  }

  /**
   * Encodes a SecretKey to a Base64 string for storage.
   *
   * @param key the key to encode
   * @return Base64-encoded key string
   */
  public static String toBase64(SecretKey key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

  /**
   * Wraps an OutputStream with AES-256-GCM encryption.
   *
   * <p>The IV (initialization vector) is prepended to the output stream so that decryption can
   * recover it.
   *
   * @param outputStream the underlying output stream
   * @param key the secret key
   * @return an encrypting CipherOutputStream
   */
  public static OutputStream encryptStream(OutputStream outputStream, SecretKey key) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      SecureRandom.getInstanceStrong().nextBytes(iv);
      // Prepend IV to the output
      outputStream.write(iv);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      Cipher cipher = Cipher.getInstance(CIPHER_MODE);
      cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
      return new CipherOutputStream(outputStream, cipher);
    } catch (Exception e) {
      LOGGER.error("Cannot create encryption stream", e);
      throw new IllegalStateException("Encryption not available", e);
    }
  }

  /**
   * Wraps an InputStream with AES-256-GCM decryption.
   *
   * <p>The IV is expected to be the first 12 bytes of the input stream (as written by {@link
   * #encryptStream}).
   *
   * @param inputStream the underlying input stream
   * @param key the secret key
   * @return a decrypting CipherInputStream
   */
  public static InputStream decryptStream(InputStream inputStream, SecretKey key) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      int read = inputStream.read(iv);
      if (read != GCM_IV_LENGTH) {
        throw new IllegalStateException("Invalid encrypted stream: missing IV");
      }
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      Cipher cipher = Cipher.getInstance(CIPHER_MODE);
      cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
      return new CipherInputStream(inputStream, cipher);
    } catch (Exception e) {
      LOGGER.error("Cannot create decryption stream", e);
      throw new IllegalStateException("Decryption not available", e);
    }
  }
}
