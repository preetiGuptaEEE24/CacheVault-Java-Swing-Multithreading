import ui.CacheVaultCLI;

public class Main {
    public static void main(String[] args) {
        int capacity = 5; // deliberately small so LRU eviction is easy to see live
        CacheVaultCLI cli = new CacheVaultCLI(capacity);
        cli.start();
    }
}
