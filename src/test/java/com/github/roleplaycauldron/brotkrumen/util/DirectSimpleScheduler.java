package com.github.roleplaycauldron.brotkrumen.util;

import com.github.roleplaycauldron.spellbook.core.scheduler.SimpleScheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Synchronous scheduler for async adapter tests.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class DirectSimpleScheduler implements SimpleScheduler {

    /**
     * Empty constructor.
     */
    public DirectSimpleScheduler() {
        // Empty constructor
    }

    @Override
    public <T> CompletableFuture<T> runTaskAsync(final Callable<T> callable) {
        try {
            return CompletableFuture.completedFuture(callable.call());
        } catch (final Exception exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public CompletableFuture<Void> runTaskAsync(final Runnable runnable) {
        try {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        } catch (final RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
