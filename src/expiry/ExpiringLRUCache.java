package expiry;

import core.LRUCache;
import stats.CacheStats;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Adds TTL (time-to-live) expiry on top of the plain LRUCache.
 *
 * Two expiry strategies are combined, which is the same trade-off
 * real caches like Redis make:
 *  - LAZY expiry: checked every time get() is called, so a stale read
 *    never slips through even if the sweeper hasn't run yet.
 *  - ACTIVE expiry: a background thread sweeps periodically to remove
 *    expired entries that are never touched again (otherwise they'd
 *    sit in memory forever, never triggering a lazy check).
 */
public class ExpiringLRUCache<K, V> {

    private final LRUCache<K, CacheEntry<V>> cache;
    private final CacheStats stats;
    private final ScheduledExecutorService cleaner;

    public ExpiringLRUCache(int capacity, CacheStats stats) {
        this.cache = new LRUCache<>(capacity);
        this.stats = stats;
        this.cleaner = Executors.newSingleThreadScheduledExecutor();
        this.cleaner.scheduleAtFixedRate(this::sweepExpired, 2, 2, TimeUnit.SECONDS);
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);

        if (entry == null) {
            stats.recordMiss();
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            stats.recordMiss();
            return null;
        }

        stats.recordHit();
        return entry.getValue();
    }

    public void put(K key, V value) {
        put(key, value, -1);
    }

    public void put(K key, V value, long ttlMillis) {
        cache.put(key, new CacheEntry<>(value, ttlMillis));
    }

    public boolean remove(K key) {
        return cache.remove(key);
    }

    public int size() {
        return cache.size();
    }

    public List<K> keysInRecencyOrder() {
        return cache.keysInRecencyOrder();
    }

    private void sweepExpired() {
        for (K key : cache.keysInRecencyOrder()) {
            CacheEntry<V> entry = cache.peek(key); // peek: doesn't disturb recency order
            if (entry != null && entry.isExpired()) {
                cache.remove(key);
            }
        }
    }

    public void shutdown() {
        cleaner.shutdown();
    }
}
