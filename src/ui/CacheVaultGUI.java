package ui;

import expiry.ExpiringLRUCache;
import stats.CacheStats;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CacheVaultGUI extends JFrame {

    private final ExpiringLRUCache<String, String> cache;
    private final CacheStats stats;
    private final int capacity;

    private JTextField keyField;
    private JTextField valueField;
    private JTextField ttlField;
    private DefaultListModel<String> listModel;
    private JList<String> cacheList;
    private JLabel statusLabel;
    private JLabel statsLabel;
    private JLabel sizeLabel;

    public CacheVaultGUI(int capacity) {
        this.capacity = capacity;
        this.stats = new CacheStats();
        this.cache = new ExpiringLRUCache<>(capacity, stats);
        buildUI();
        startAutoRefresh();
    }

    private void buildUI() {
        setTitle("CacheVault - LRU Cache with TTL (capacity " + capacity + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 500);
        setLayout(new BorderLayout(10, 10));

        // --- Input panel ---
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        keyField = new JTextField();
        valueField = new JTextField();
        ttlField = new JTextField();

        inputPanel.add(new JLabel("Key:"));
        inputPanel.add(keyField);
        inputPanel.add(new JLabel("Value:"));
        inputPanel.add(valueField);
        inputPanel.add(new JLabel("TTL seconds (optional):"));
        inputPanel.add(ttlField);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        JButton putButton = new JButton("Put");
        JButton getButton = new JButton("Get");
        JButton removeButton = new JButton("Remove");

        putButton.addActionListener(e -> handlePut());
        getButton.addActionListener(e -> handleGet());
        removeButton.addActionListener(e -> handleRemove());

        buttonPanel.add(putButton);
        buttonPanel.add(getButton);
        buttonPanel.add(removeButton);

        inputPanel.add(buttonPanel);

        // --- Cache display (live, most-recently-used at top) ---
        listModel = new DefaultListModel<>();
        cacheList = new JList<>(listModel);
        cacheList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(cacheList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Cache (most-recently-used first)"));

        // --- Status footer ---
        JPanel footerPanel = new JPanel(new GridLayout(3, 1));
        statusLabel = new JLabel(" ");
        statsLabel = new JLabel("Hits: 0, Misses: 0, Hit Ratio: 0.0%");
        sizeLabel = new JLabel("Size: 0 / " + capacity);
        footerPanel.add(statusLabel);
        footerPanel.add(statsLabel);
        footerPanel.add(sizeLabel);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void handlePut() {
        String key = keyField.getText().trim();
        String value = valueField.getText().trim();
        String ttlText = ttlField.getText().trim();

        if (key.isEmpty() || value.isEmpty()) {
            statusLabel.setText("Key and value are both required.");
            return;
        }

        if (!ttlText.isEmpty()) {
            try {
                long ttlMillis = Long.parseLong(ttlText) * 1000L;
                cache.put(key, value, ttlMillis);
                statusLabel.setText("Stored '" + key + "' (expires in " + ttlText + "s)");
            } catch (NumberFormatException ex) {
                statusLabel.setText("TTL must be a number.");
                return;
            }
        } else {
            cache.put(key, value);
            statusLabel.setText("Stored '" + key + "' -> '" + value + "'");
        }

        keyField.setText("");
        valueField.setText("");
        ttlField.setText("");
        refreshView();
    }

    private void handleGet() {
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            statusLabel.setText("Enter a key to get.");
            return;
        }
        String value = cache.get(key);
        if (value == null) {
            statusLabel.setText("MISS: '" + key + "' not found (or expired)");
        } else {
            statusLabel.setText("HIT: '" + key + "' -> '" + value + "'");
        }
        refreshView();
    }

    private void handleRemove() {
        String key = keyField.getText().trim();
        if (key.isEmpty()) {
            statusLabel.setText("Enter a key to remove.");
            return;
        }
        boolean removed = cache.remove(key);
        statusLabel.setText(removed ? "Removed '" + key + "'" : "Key not found: '" + key + "'");
        refreshView();
    }

    private void refreshView() {
        List<String> keys = cache.keysInRecencyOrder();
        listModel.clear();
        for (String key : keys) {
            listModel.addElement(key);
        }
        sizeLabel.setText("Size: " + cache.size() + " / " + capacity);
        statsLabel.setText(stats.toString());
    }

    // Periodically refreshes the view so entries that expire via the
    // background TTL sweeper (rather than a direct get/put here) still
    // visibly disappear from the list without needing user action.
    private void startAutoRefresh() {
        Timer timer = new Timer(1000, e -> refreshView());
        timer.start();
    }
}
