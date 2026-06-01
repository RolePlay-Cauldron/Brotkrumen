package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.visual.VisualizerRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks per-player resolve guidance sessions and generation tokens.
 */
public final class ResolveGuidanceSessionManager {

    private final VisualizerRegistry visualizerRegistry;

    private final Map<UUID, Session> sessionsByPlayer = new HashMap<>();

    private final Map<UUID, Long> generationCounters = new HashMap<>();

    private final ReentrantLock sessionLock = new ReentrantLock();

    /**
     * Creates a session manager.
     *
     * @param visualizerRegistry visualizer registry
     */
    public ResolveGuidanceSessionManager(final VisualizerRegistry visualizerRegistry) {
        this.visualizerRegistry = visualizerRegistry;
    }

    /**
     * Replaces the player's existing resolve guidance session with a new pending token.
     *
     * @param playerId player id
     * @return new session token
     */
    public long replaceWithPending(final UUID playerId) {
        sessionLock.lock();
        try {
            final long token = generationCounters.merge(playerId, 1L, Long::sum);
            final Session previous = sessionsByPlayer.put(playerId, Session.pending(token));
            unregisterIfActive(playerId, previous);
            return token;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Cancels a player's resolve guidance session.
     *
     * @param playerId player id
     * @return true when a session existed
     */
    public boolean cancel(final UUID playerId) {
        sessionLock.lock();
        try {
            final Session previous = sessionsByPlayer.remove(playerId);
            unregisterIfActive(playerId, previous);
            return previous != null;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Checks if a token still belongs to the current player session.
     *
     * @param playerId player id
     * @param token    session token
     * @return true when token is current
     */
    public boolean isCurrent(final UUID playerId, final long token) {
        sessionLock.lock();
        try {
            final Session session = sessionsByPlayer.get(playerId);
            return session != null && session.token() == token;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Marks the matching session as active after its visualizer was built.
     *
     * @param playerId player id
     * @param token    session token
     * @return true when the matching session exists
     */
    public boolean activate(final UUID playerId, final long token) {
        sessionLock.lock();
        try {
            final Session session = sessionsByPlayer.get(playerId);
            if (session == null || session.token() != token) {
                return false;
            }
            sessionsByPlayer.put(playerId, session.activate());
            return true;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Marks a matching session as completed once.
     *
     * @param playerId player id
     * @param token    session token
     * @return true when completion transitioned from incomplete to complete
     */
    public boolean markCompleted(final UUID playerId, final long token) {
        sessionLock.lock();
        try {
            final Session session = sessionsByPlayer.get(playerId);
            if (session == null || session.token() != token || session.completed()) {
                return false;
            }
            sessionsByPlayer.put(playerId, session.complete());
            return true;
        } finally {
            sessionLock.unlock();
        }
    }

    /**
     * Removes the matching session and unregisters its visualizer when still active.
     *
     * @param playerId player id
     * @param token    session token
     * @return true when the matching session existed and was removed
     */
    public boolean clearIfCurrent(final UUID playerId, final long token) {
        sessionLock.lock();
        try {
            final Session session = sessionsByPlayer.get(playerId);
            if (session == null || session.token() != token) {
                return false;
            }
            sessionsByPlayer.remove(playerId);
            unregisterIfActive(playerId, session);
            return true;
        } finally {
            sessionLock.unlock();
        }
    }

    private void unregisterIfActive(final UUID playerId, final Session session) {
        if (session != null && session.active()) {
            visualizerRegistry.unregister(playerId);
        }
    }

    private record Session(long token, boolean active, boolean completed) {

        private static Session pending(final long token) {
            return new Session(token, false, false);
        }

        private Session activate() {
            return new Session(token, true, completed);
        }

        private Session complete() {
            return new Session(token, active, true);
        }
    }
}
