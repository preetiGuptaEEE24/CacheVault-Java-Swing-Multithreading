package ui;

import expiry.ExpiringLRUCache;
import stats.CacheStats;

import java.util.List;
import java.util.Scanner;

public class CacheVaultCLI {

    private final ExpiringLRUCache<String, String> cache;
    private final CacheStats stats;

    public CacheVaultCLI(int capacity) {
        this.stats = new CacheStats();
        this.cache = new ExpiringLRUCache<>(capacity, stats);
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (true) {
            System.out.print("\ncachevault> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "put":
                    handlePut(parts);
                    break;
                case "get":
                    handleGet(parts);
                    break;
                case "remove":
                    handleRemove(parts);
                    break;
                case "print":
                    printCache();
                    break;
                case "stats":
                    System.out.println(stats);
                    break;
                case "help":
                    printHelp();
                    break;
                case "exit":
                    cache.shutdown();
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Unknown command. Type 'help' for options.");
            }
        }
    }

    private void handlePut(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: put <key> <value> [ttlSeconds]");
            return;
        }
        String key = parts[1];
        String value = parts[2];

        if (parts.length >= 4) {
            long ttlMillis = Long.parseLong(parts[3]) * 1000L;
            cache.put(key, value, ttlMillis);
            System.out.println("Stored '" + key + "' -> '" + value + "' (expires in " + parts[3] + "s)");
        } else {
            cache.put(key, value);
            System.out.println("Stored '" + key + "' -> '" + value + "'");
        }
    }

    private void handleGet(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: get <key>");
            return;
        }
        String value = cache.get(parts[1]);
        if (value == null) {
            System.out.println("MISS: '" + parts[1] + "' not found (or expired)");
        } else {
            System.out.println("HIT: '" + parts[1] + "' -> '" + value + "'");
        }
    }

    private void handleRemove(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: remove <key>");
            return;
        }
        boolean removed = cache.remove(parts[1]);
        System.out.println(removed ? "Removed '" + parts[1] + "'" : "Key not found: '" + parts[1] + "'");
    }

    private void printCache() {
        List<String> keys = cache.keysInRecencyOrder();
        if (keys.isEmpty()) {
            System.out.println("Cache is empty.");
            return;
        }
        System.out.println("Cache (most-recently-used first):");
        for (String key : keys) {
            System.out.println("  " + key);
        }
        System.out.println("Size: " + cache.size());
    }

    private void printHelp() {
        System.out.println("=== CacheVault CLI ===");
        System.out.println("Commands:");
        System.out.println("  put <key> <value> [ttlSeconds]   store a value, optional expiry");
        System.out.println("  get <key>                        retrieve a value");
        System.out.println("  remove <key>                     delete a key");
        System.out.println("  print                             show cache contents (recency order)");
        System.out.println("  stats                             show hit/miss ratio");
        System.out.println("  help                               show this menu");
        System.out.println("  exit                               quit");
    }
}
