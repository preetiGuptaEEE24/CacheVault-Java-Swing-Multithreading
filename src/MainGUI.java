import ui.CacheVaultGUI;

import javax.swing.SwingUtilities;

public class MainGUI {
    public static void main(String[] args) {
        int capacity = 5; // deliberately small so LRU eviction is easy to see live

        SwingUtilities.invokeLater(() -> {
            CacheVaultGUI gui = new CacheVaultGUI(capacity);
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
}
