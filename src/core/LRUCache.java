package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LRUCache<K, V> {

    private final int capacity;
    private final Map<K, Node<K, V>> map;

    // Dummy sentinel nodes remove all the "is this the first/last node?"
    // edge cases from the linking logic below.
    private final Node<K, V> head;
    private final Node<K, V> tail;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    public synchronized V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;
        moveToFront(node);
        return node.value;
    }

    /** Look up a value WITHOUT affecting recency order. Used internally
     *  by things like the TTL sweeper that shouldn't skew LRU order. */
    public synchronized V peek(K key) {
        Node<K, V> node = map.get(key);
        return node == null ? null : node.value;
    }

    public synchronized void put(K key, V value) {
        Node<K, V> existing = map.get(key);
        if (existing != null) {
            existing.value = value;
            moveToFront(existing);
            return;
        }

        Node<K, V> node = new Node<>(key, value);
        map.put(key, node);
        addToFront(node);

        if (map.size() > capacity) {
            evictLeastRecentlyUsed();
        }
    }

    public synchronized boolean remove(K key) {
        Node<K, V> node = map.remove(key);
        if (node == null) return false;
        unlink(node);
        return true;
    }

    public synchronized boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public synchronized int size() {
        return map.size();
    }

    /** Returns keys ordered from most-recently-used to least-recently-used. */
    public synchronized List<K> keysInRecencyOrder() {
        List<K> keys = new ArrayList<>();
        Node<K, V> current = head.next;
        while (current != tail) {
            keys.add(current.key);
            current = current.next;
        }
        return keys;
    }

    private void moveToFront(Node<K, V> node) {
        unlink(node);
        addToFront(node);
    }

    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void unlink(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void evictLeastRecentlyUsed() {
        Node<K, V> lru = tail.prev;
        if (lru == head) return; // cache empty
        unlink(lru);
        map.remove(lru.key);
    }
}
