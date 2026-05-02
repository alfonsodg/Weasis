/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.model.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages version history for {@link org.weasis.core.ui.model.GraphicModel} instances. Each
 * revision captures a snapshot of metadata (revision number, timestamp, user, description) whenever
 * the model is modified or saved. The revision history is bounded by a configurable maximum number
 * of entries, with the oldest revisions being evicted first.
 *
 * <p>All public methods are thread-safe, using a {@link ReadWriteLock} to allow concurrent reads
 * while serializing writes.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * RevisionManager rm = new RevisionManager();
 * int rev = rm.createRevision("user1", "Added measurement");
 * rm.restoreRevision(rev);
 * }</pre>
 */
public class RevisionManager {

  private final List<Revision> revisions;
  private final int maxRevisions;
  private final ReadWriteLock lock;
  private int currentRevisionNumber;

  /**
   * Creates a revision manager with the default maximum of 100 revisions.
   */
  public RevisionManager() {
    this(100);
  }

  /**
   * Creates a revision manager with the specified maximum number of revisions.
   *
   * @param maxRevisions the maximum number of revisions to retain (must be at least 1)
   */
  public RevisionManager(int maxRevisions) {
    this.maxRevisions = Math.max(1, maxRevisions);
    this.revisions = new ArrayList<>();
    this.lock = new ReentrantReadWriteLock();
    this.currentRevisionNumber = 0;
  }

  /**
   * Creates a new revision with the given user identifier and description, increments the current
   * revision number, and evicts the oldest revision if the history exceeds the maximum size.
   *
   * @param userId      the identifier of the user who triggered the revision (can be empty)
   * @param description a human-readable description of the change (can be empty)
   * @return the new revision number
   */
  public int createRevision(String userId, String description) {
    lock.writeLock().lock();
    try {
      currentRevisionNumber++;
      revisions.add(
          new Revision(currentRevisionNumber, Instant.now(), userId, description));
      while (revisions.size() > maxRevisions) {
        revisions.remove(0);
      }
      return currentRevisionNumber;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Creates a new revision with empty user identifier and description.
   *
   * @return the new revision number
   */
  public int createRevision() {
    return createRevision("", "");
  }

  /**
   * Retrieves the revision with the given revision number, or {@code null} if no such revision
   * exists in the current history.
   *
   * @param revisionNumber the revision number to look up
   * @return the matching revision, or {@code null}
   */
  public Revision getRevision(int revisionNumber) {
    lock.readLock().lock();
    try {
      for (Revision r : revisions) {
        if (r.revisionNumber == revisionNumber) {
          return r;
        }
      }
      return null;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns an unmodifiable snapshot of the full revision history.
   *
   * @return a list of all stored revisions in chronological order
   */
  public List<Revision> getRevisionHistory() {
    lock.readLock().lock();
    try {
      return Collections.unmodifiableList(new ArrayList<>(revisions));
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns the current revision number. This is the highest revision number assigned so far.
   *
   * @return the current revision number
   */
  public int getCurrentRevisionNumber() {
    lock.readLock().lock();
    try {
      return currentRevisionNumber;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Sets the current revision number to the specified value without validation. Intended for use
   * during deserialization to restore the version from a previously saved model.
   *
   * @param revisionNumber the revision number to set
   */
  public void setCurrentRevisionNumber(int revisionNumber) {
    lock.writeLock().lock();
    try {
      this.currentRevisionNumber = revisionNumber;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Restores the revision pointer to the specified revision number, effectively marking that
   * revision as the current one. This does not revert the underlying model data; it only updates
   * the version tracking metadata.
   *
   * @param revisionNumber the revision number to restore to
   * @return {@code true} if the revision number is valid and was restored, {@code false} otherwise
   */
  public boolean restoreRevision(int revisionNumber) {
    lock.writeLock().lock();
    try {
      if (revisionNumber < 0 || revisionNumber > currentRevisionNumber) {
        return false;
      }
      currentRevisionNumber = revisionNumber;
      return true;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Returns the maximum number of revisions that can be stored.
   *
   * @return the maximum number of revisions
   */
  public int getMaxRevisions() {
    return maxRevisions;
  }

  /**
   * Clears all revision history and resets the current revision number to zero.
   */
  public void clear() {
    lock.writeLock().lock();
    try {
      revisions.clear();
      currentRevisionNumber = 0;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Represents a single revision in the version history. Each revision captures the metadata
   * associated with a change to the model: a sequential revision number, the timestamp of the
   * change, the identifier of the user who made the change, and a description.
   */
  public static class Revision {

    private final int revisionNumber;
    private final Instant timestamp;
    private final String userId;
    private final String description;

    /**
     * Creates a new revision with the specified metadata.
     *
     * @param revisionNumber the sequential revision number
     * @param timestamp      the instant at which the revision was created
     * @param userId         the identifier of the user who made the change (can be {@code null})
     * @param description    a description of the change (can be {@code null})
     */
    public Revision(
        int revisionNumber, Instant timestamp, String userId, String description) {
      this.revisionNumber = revisionNumber;
      this.timestamp = Objects.requireNonNull(timestamp);
      this.userId = userId;
      this.description = description;
    }

    /**
     * Returns the sequential revision number.
     *
     * @return the revision number
     */
    public int getRevisionNumber() {
      return revisionNumber;
    }

    /**
     * Returns the timestamp when this revision was created.
     *
     * @return the creation timestamp
     */
    public Instant getTimestamp() {
      return timestamp;
    }

    /**
     * Returns the identifier of the user who created this revision.
     *
     * @return the user identifier, or {@code null}
     */
    public String getUserId() {
      return userId;
    }

    /**
     * Returns the description of this revision.
     *
     * @return the description, or {@code null}
     */
    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return String.format(
          "Revision #%d [%s] by %s: %s",
          revisionNumber, timestamp, userId, description);
    }
  }
}
