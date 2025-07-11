package com.yourcompany.myquestplugin.manager;

import com.yourcompany.myquestplugin.MyQuestPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

// Kelas ini sekarang mengelola item yang disimpan untuk pembuat misi
public class QuestStorageManager {

    private final MyQuestPlugin plugin;
    private final File questStorageFile;
    private FileConfiguration questStorageConfig;
    // Map: Creator UUID -> Map: Material -> Amount
    private final Map<UUID, Map<Material, Integer>> storedItems;

    public QuestStorageManager(MyQuestPlugin plugin) {
        this.plugin = plugin;
        this.questStorageFile = new File(plugin.getDataFolder(), "quest_storage.yml"); // Nama file baru
        this.storedItems = new HashMap<>();
        createQuestStorageFile();
    }

    /**
     * Membuat file quest_storage.yml jika belum ada.
     */
    private void createQuestStorageFile() {
        if (!questStorageFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                questStorageFile.createNewFile();
                plugin.getLogger().log(Level.INFO, "File quest_storage.yml berhasil dibuat.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Gagal membuat quest_storage.yml: " + e.getMessage());
            }
        }
        this.questStorageConfig = YamlConfiguration.loadConfiguration(questStorageFile);
    }

    /**
     * Memuat semua item yang disimpan dari quest_storage.yml.
     */
    public void loadStoredItems() {
        storedItems.clear();
        createQuestStorageFile(); // Memuat ulang konfigurasi jika file baru dibuat atau ada perubahan
        ConfigurationSection storageSection = questStorageConfig.getConfigurationSection("stored_items");
        if (storageSection == null) {
            plugin.getLogger().log(Level.INFO, "Tidak ada item tersimpan ditemukan di quest_storage.yml.");
            return;
        }

        for (String creatorIdStr : storageSection.getKeys(false)) {
            try {
                UUID creatorId = UUID.fromString(creatorIdStr);
                ConfigurationSection itemsSection = storageSection.getConfigurationSection(creatorIdStr);
                if (itemsSection != null) {
                    Map<Material, Integer> creatorStoredItems = new HashMap<>();
                    for (String materialName : itemsSection.getKeys(false)) {
                        try {
                            Material material = Material.valueOf(materialName);
                            creatorStoredItems.put(material, itemsSection.getInt(materialName));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().log(Level.WARNING, "Bahan tidak valid ditemukan untuk item tersimpan " + materialName + ": " + e.getMessage());
                        }
                    }
                    if (!creatorStoredItems.isEmpty()) {
                        storedItems.put(creatorId, creatorStoredItems);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Gagal memuat item tersimpan untuk UUID " + creatorIdStr + ": " + e.getMessage());
            }
        }
        plugin.getLogger().log(Level.INFO, storedItems.size() + " entri item tersimpan berhasil dimuat.");
    }

    /**
     * Menyimpan semua item yang disimpan ke quest_storage.yml.
     */
    public void saveStoredItems() {
        // Hapus semua entri lama untuk memastikan data yang disimpan adalah yang terbaru
        questStorageConfig.set("stored_items", null);

        for (Map.Entry<UUID, Map<Material, Integer>> creatorEntry : storedItems.entrySet()) {
            String creatorIdStr = creatorEntry.getKey().toString();
            Map<Material, Integer> items = creatorEntry.getValue();

            ConfigurationSection creatorSection = questStorageConfig.createSection("stored_items." + creatorIdStr);
            for (Map.Entry<Material, Integer> itemEntry : items.entrySet()) {
                creatorSection.set(itemEntry.getKey().name(), itemEntry.getValue());
            }
        }

        try {
            questStorageConfig.save(questStorageFile);
            plugin.getLogger().log(Level.INFO, "Semua item tersimpan berhasil disimpan.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal menyimpan quest_storage.yml: " + e.getMessage());
        }
    }

    /**
     * Menambahkan item ke daftar item yang disimpan untuk pembuat misi.
     * @param creatorId UUID dari pembuat misi.
     * @param items Map Material -> Amount dari item yang akan ditambahkan.
     */
    public void addStoredItems(UUID creatorId, Map<Material, Integer> items) {
        Map<Material, Integer> creatorStoredItems = storedItems.computeIfAbsent(creatorId, k -> new HashMap<>());
        items.forEach((material, amount) ->
                creatorStoredItems.merge(material, amount, Integer::sum));
        saveStoredItems();
    }

    /**
     * Menghapus jumlah item tertentu dari item yang disimpan untuk pembuat misi.
     * @param creatorId UUID dari pembuat misi.
     * @param material Material item yang akan dihapus.
     * @param amount Jumlah item yang akan dihapus.
     */
    public void removeStoredItems(UUID creatorId, Material material, int amount) {
        Map<Material, Integer> creatorStoredItems = storedItems.get(creatorId);
        if (creatorStoredItems != null) {
            int currentAmount = creatorStoredItems.getOrDefault(material, 0);
            if (currentAmount <= amount) {
                creatorStoredItems.remove(material); // Hapus sepenuhnya jika jumlahnya kurang atau sama
            } else {
                creatorStoredItems.put(material, currentAmount - amount); // Kurangi jumlahnya
            }
            if (creatorStoredItems.isEmpty()) {
                storedItems.remove(creatorId); // Hapus entri pembuat jika tidak ada lagi item
            }
            saveStoredItems();
        }
    }

    /**
     * Mendapatkan item yang disimpan untuk pembuat misi tertentu.
     * @param creatorId UUID dari pembuat misi.
     * @return Map Material -> Amount dari item yang disimpan, atau map kosong jika tidak ada.
     */
    public Map<Material, Integer> getStoredItems(UUID creatorId) {
        return storedItems.getOrDefault(creatorId, new HashMap<>());
    }

    /**
     * Menghapus semua item yang disimpan untuk pembuat misi tertentu.
     * @param creatorId UUID dari pembuat misi.
     */
    public void clearStoredItems(UUID creatorId) {
        storedItems.remove(creatorId);
        saveStoredItems();
    }
}
