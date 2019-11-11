/*
 * Copyright 2019 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.internal.consul;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * {@code CachedConsulClient} caches a object that queried from consul agent by polling.
 *
 * @param <T> cached object type
 */
final class CachedConsulClient<T> implements AutoCloseable {
    public static final long DEFAULT_DELAY_MILLIS = 10_000;

    private long delayMillis;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private T cachedObject;
    private boolean stop;
    private boolean started;
    private boolean checkOnce;
    private final ConsulQuery<T> query;
    private final List<Listener<T>> listeners = new ArrayList<>();

    /**
     * Creates a {@link CachedConsulClient} with a query and default delay time.
     *
     * @param query a {@link ConsulQuery} that a function for executing query to consul agent
     */
    CachedConsulClient(ConsulQuery<T> query) {
        this.query = query;
    }

    public CachedConsulClient<T> delayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
        return this;
    }

    /**
     * Start caching.
     */
    public void start() {
        synchronized (executor) {
            if (!started) {
                started = true;
                checkOnce = true;
            }
        }
        if (checkOnce) {
            checkOnce = false;
            check();
        }
    }

    /**
     * Stop caching.
     */
    public void stop() {
        stop = true;
        executor.shutdownNow();
    }

    /**
     * Gets a object from cache.
     *
     * @return the result of executing query
     */
    @Nullable
    public T get() {
        synchronized (executor) {
            if (!started) {
                throw new IllegalStateException("Not yet started the cache.");
            }
            return cachedObject;
        }
    }

    /**
     * Adds a listener to check whether endpoints changed.
     *
     * @param listener a listener to add
     */
    public void addListener(Listener<T> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener a listener to remove
     */
    public void removeListener(Listener<T> listener) {
        listeners.remove(listener);
    }

    private void check() {
        if (stop) {
            return;
        }
        final T newObject = query.query();
        synchronized (executor) {
            // If need to this operation changed to asynchronous way, have to copy cachedObject.
            listeners.forEach(listener -> listener.update(cachedObject, newObject));
            cachedObject = newObject;
        }
        executor.schedule(this::check, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Implements a close method of {@link AutoCloseable}.
     */
    @Override
    public void close() {
        stop();
        executor.shutdownNow();
    }

    /**
     * Functional interface for checking on changed what cached objects.
     *
     * @param <T> a type of object cached
     */
    @FunctionalInterface
    public interface Listener<T> {
        void update(T oldObject, T newObject);
    }
}
