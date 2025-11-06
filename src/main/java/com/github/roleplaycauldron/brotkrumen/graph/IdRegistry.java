package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Registry for the different ids.
 */
@SuppressWarnings("PMD.ShortVariable")
public class IdRegistry {

    private final IdPool nodePool;

    private final IdPool edgePool;

    /**
     * The default constructor.
     */
    public IdRegistry() {
        this(1, 1);
    }

    /**
     * Creates a new registry with the given start ids of the nodes and edges.
     *
     * @param nodeStart the start id of the nodes. Ignores the seed of all values before this one
     * @param edgeStart the start id of the edges. Ignores the seed of all values before this one
     */
    public IdRegistry(final int nodeStart, final int edgeStart) {
        this.nodePool = new IdPool(nodeStart);
        this.edgePool = new IdPool(edgeStart);
    }

    /**
     * Get the next node id.
     *
     * @return the next node id
     */
    public int getNextNodeId() {
        return nodePool.acquire();
    }

    /**
     * Get the next edge id.
     *
     * @return the next edge id
     */
    public int getNextEdgeId() {
        return edgePool.acquire();
    }

    /**
     * Release a node id.
     *
     * @param id the id to release
     */
    public void releaseNodeId(final int id) {
        nodePool.release(id);
    }

    /**
     * Release an edge id.
     *
     * @param id the id to release
     */
    public void releaseEdgeId(final int id) {
        edgePool.release(id);
    }

    /**
     * Check if a node id is in use.
     *
     * @param id the id to check
     * @return {@code true} if the id is in use, {@code false} otherwise
     */
    public boolean isNodeInUse(final int id) {
        return nodePool.inUse(id);
    }

    /**
     * Check if an edge id is in use.
     *
     * @param id the id to check
     * @return {@code true} if the id is in use, {@code false} otherwise
     */
    public boolean isEdgeInUse(final int id) {
        return edgePool.inUse(id);
    }

    /**
     * Seed the id pools with existing ids.
     *
     * @param existingIds the existing ids
     */
    public void seedNodeIds(final Collection<Integer> existingIds) {
        nodePool.seed(existingIds);
    }

    /**
     * Seed the id pools with existing ids.
     *
     * @param existingIds the existing ids
     */
    public void seedEdgeIds(final Collection<Integer> existingIds) {
        edgePool.seed(existingIds);
    }

    /**
     * A simple id pool.
     */
    @SuppressWarnings("PMD.ShortVariable")
    private static final class IdPool {

        private final int start;

        private final AtomicInteger next;

        private final Queue<Integer> free;

        private final Set<Integer> allocated;

        private final ReentrantLock lock;

        /**
         * The default constructor.
         *
         * @param start the start id
         */
        public IdPool(final int start) {
            this.start = start;
            this.next = new AtomicInteger(start);
            this.free = new PriorityQueue<>();
            this.allocated = new HashSet<>();
            this.lock = new ReentrantLock();
        }

        /**
         * Acquire a new id.
         *
         * @return the id
         */
        public int acquire() {
            lock.lock();
            try {
                final int id;
                if (free.isEmpty()) {
                    id = next.getAndIncrement();
                } else {
                    id = free.poll();
                }
                allocated.add(id);
                return id;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Release an id back to the pool.
         *
         * @param id the id to release
         */
        public void release(final int id) {
            lock.lock();
            try {
                if (!allocated.remove(id)) {
                    return;
                }
                free.offer(id);
            } finally {
                lock.unlock();
            }
        }

        /**
         * Check if an id is in use.
         *
         * @param id the id to check
         * @return {@code true} if the id is in use, {@code false} otherwise
         */
        public boolean inUse(final int id) {
            lock.lock();
            try {
                return allocated.contains(id);
            } finally {
                lock.unlock();
            }
        }

        /**
         * Seed the pool with existing ids.
         *
         * @param existing the existing ids
         */
        public void seed(final Collection<Integer> existing) {
            lock.lock();
            try {
                resetState(existing);
                updateNextPointer();
                refillFreeSlots();
            } finally {
                lock.unlock();
            }
        }

        private void resetState(final Collection<Integer> existing) {
            allocated.clear();
            free.clear();

            if (existing == null || existing.isEmpty()) {
                next.set(start);
                return;
            }

            for (final Integer id : existing) {
                if (id == null || id < start) {
                    continue;
                }
                allocated.add(id);
            }
        }

        private void updateNextPointer() {
            int max = start - 1;
            for (final int id : allocated) {
                if (id > max) {
                    max = id;
                }
            }
            next.set(Math.max(max + 1, start));
        }

        private void refillFreeSlots() {
            final int nextVal = next.get();
            final int span = nextVal - start;
            if (allocated.size() >= span) {
                return;
            }

            final BitSet used = new BitSet(span);
            for (final int id : allocated) {
                final int index = id - start;
                if (index >= 0 && index < span) {
                    used.set(index);
                }
            }

            for (int i = 0; i < span; i++) {
                if (!used.get(i)) {
                    free.offer(start + i);
                }
            }
        }
    }
}
