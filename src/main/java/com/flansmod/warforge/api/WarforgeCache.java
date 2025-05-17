package com.flansmod.warforge.api;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * A Cache datatype optimized for quick retrieval and purging of data.
 * It purges data inside either based on age or size limit as long as
 * that constraint isn't 0.
 *
 *
 * @param <K> Key
 * @param <V> Value
 * @author MrNorwood
 */
public class WarforgeCache<K, V> {

    private final Map<K, Entry> cache = new HashMap<>();
    private final Deque<Entry> queue = new ArrayDeque<>();
    private final long maxAgeMillis; //0 to disable
    private final int maxSize; //0 for infinite

    public WarforgeCache(long maxAgeMillis, int maxSize) {
        this.maxAgeMillis = maxAgeMillis;
        this.maxSize = maxSize;
    }

    public synchronized void put(K key, V value) {
        purgeExpired();

        if (cache.containsKey(key)) {
            queue.remove(cache.get(key));
        } else if (maxSize > 0 && cache.size() >= maxSize) {
            evictOldest();
        }

        Entry entry = new Entry(key, value);
        cache.put(key, entry);
        queue.addLast(entry);
    }

    public synchronized V get(K key) {
        purgeExpired();
        Entry entry = cache.get(key);
        return entry != null ? entry.value : null;
    }

    private void evictOldest() {
        Entry oldest = queue.pollFirst();
        if (oldest != null) {
            cache.remove(oldest.key);
        }
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty()) {
            Entry oldest = queue.peekFirst();
            if (now - oldest.timestamp > maxAgeMillis) {
                queue.pollFirst();
                cache.remove(oldest.key);
            } else {
                break;
            }
        }
    }

    public synchronized int size() {
        purgeExpired();
        return cache.size();
    }

    public synchronized void clear() {
        cache.clear();
        queue.clear();
    }
    public synchronized void remove(K key){
        purgeExpired();
        Entry entry = cache.remove(key);
        if (entry != null) {
            queue.remove(entry);
        }
    }

    private class Entry {
        final K key;
        final V value;
        final long timestamp;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }
}

