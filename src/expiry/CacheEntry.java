package expiry;

public class CacheEntry<V> {

    private final V value;
    private final long expiryTimeMillis; // -1 means "never expires"

    public CacheEntry(V value, long ttlMillis) {
        this.value = value;
        this.expiryTimeMillis = ttlMillis > 0
                ? System.currentTimeMillis() + ttlMillis
                : -1;
    }

    public V getValue() {
        return value;
    }

    public boolean isExpired() {
        return expiryTimeMillis > 0 && System.currentTimeMillis() > expiryTimeMillis;
    }
}
