package com.example.camel;

import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedOperation;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.ServiceSupport;

import java.util.LinkedHashMap;

/**
 * Naive memory based implementation of {@link IdempotentRepository}.
 * <p>
 * Keys added to this repository get a timestamp and are removed from the repository as soon
 * as they reached the configured time to live.
 */
@ManagedResource(description = "Memory based idempotent repository with TTL eviction")
public class TTLIdempotentRepository extends ServiceSupport implements IdempotentRepository {

    private final LinkedHashMap<Object, Long> keys = new LinkedHashMap<>();
    private final long ttl;

    /**
     * Create a new memory based repository.
     *
     * @param millisecondsToLive
     */
    public TTLIdempotentRepository(long millisecondsToLive) {
        this.ttl = millisecondsToLive;
    }

    @Override
    @ManagedOperation(description = "Adds the key to the store")
    public boolean add(Object key) {
        synchronized (keys) {
            sweep();
            if (keys.containsKey(key)) {
                return false;
            } else {
                keys.put(key, System.currentTimeMillis());
                return true;
            }
        }
    }

    @Override
    @ManagedOperation(description = "Does the store contain the given key")
    public boolean contains(Object key) {
        synchronized (keys) {
            sweep();
            return keys.containsKey(key);
        }
    }

    @Override
    @ManagedOperation(description = "Remove the key from the store")
    public boolean remove(Object key) {
        synchronized (keys) {
            sweep();
            return keys.remove(key) != null;
        }
    }

    @Override
    public boolean confirm(Object key) {
        // noop
        return true;
    }

    @Override
    @ManagedOperation(description = "Clear the store")
    public void clear() {
        synchronized (keys) {
            keys.clear();
        }
    }

    @ManagedAttribute(description = "The current cache size")
    public int getCacheSize() {
        return keys.size();
    }

    private void sweep() {
        synchronized (keys) {
            long runTimestamp = System.currentTimeMillis();
            keys.entrySet().removeIf(entry -> (entry.getValue() + ttl) <= runTimestamp);
        }
    }

    @Override
    protected void doStart() {
        // noop
    }

    @Override
    protected void doStop() {
        clear();
    }

}
