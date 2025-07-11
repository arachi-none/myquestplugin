package com.yourcompany.myquestplugin.manager;

import com.yourcompany.myquestplugin.MyQuestPlugin;
import com.yourcompany.myquestplugin.Quest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class QuestManager {

    private final MyQuestPlugin plugin;
    private final File questFile;
    private FileConfiguration questConfig;
    private final Map<UUID, Quest> quests; // Map: Quest ID -> Quest Object

    public QuestManager(MyQuestPlugin plugin) {
        this.plugin = plugin;
        this.questFile = new File(plugin.getDataFolder(), "quests.yml");
        this.quests = new HashMap<>();
        createQuestFile();
    }

    /**
     * Membuat file quests.yml jika belum ada.
     */
    private void createQuestFile() {
        if (!questFile.exists()) {
            try {
                // Pastikan direktori plugin ada
                plugin.getDataFolder().mkdirs();
                questFile.createNewFile();
                plugin.getLogger().log(Level.INFO, "File quests.yml berhasil dibuat.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Gagal membuat quests.yml: " + e.getMessage());
            }
        }
        this.questConfig = YamlConfiguration.loadConfiguration(questFile);
    }

    /**
     * Memuat semua misi dari quests.yml.
     */
    public void loadQuests() {
        quests.clear();
        createQuestFile(); // Memuat ulang konfigurasi jika file baru dibuat atau ada perubahan
        ConfigurationSection questsSection = questConfig.getConfigurationSection("quests");
        if (questsSection == null) {
            plugin.getLogger().log(Level.INFO, "Tidak ada misi ditemukan di quests.yml.");
            return;
        }

        for (String questIdStr : questsSection.getKeys(false)) {
            try {
                UUID questId = UUID.fromString(questIdStr);
                ConfigurationSection questSection = Objects.requireNonNull(questsSection.getConfigurationSection(questIdStr));

                UUID creatorId = UUID.fromString(Objects.requireNonNull(questSection.getString("creatorId")));
                String title = Objects.requireNonNull(questSection.getString("title"));
                String description = Objects.requireNonNull(questSection.getString("description"));

                // Deserialisasi requiredItems
                Map<String, Integer> requiredItemsSerialized = new HashMap<>();
                ConfigurationSection reqItemsSection = questSection.getConfigurationSection("requiredItems");
                if (reqItemsSection != null) {
                    for (String materialName : reqItemsSection.getKeys(false)) {
                        requiredItemsSerialized.put(materialName, reqItemsSection.getInt(materialName));
                    }
                }

                // Konversi requiredItemsSerialized (Map<String, Integer>) ke Map<Material, Integer>
                Map<Material, Integer> requiredItems = requiredItemsSerialized.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> Material.valueOf(entry.getKey()),
                                Map.Entry::getValue
                        ));

                // Deserialisasi rewardItems (sekarang sebagai List<ItemStack>)
                List<ItemStack> rewardItems = new ArrayList<>();
                List<?> rewardList = questSection.getList("rewardItems");
                if (rewardList != null) {
                    for (Object obj : rewardList) {
                        if (obj instanceof ItemStack) {
                            rewardItems.add((ItemStack) obj);
                        } else {
                            plugin.getLogger().log(Level.WARNING, "Item hadiah tidak valid ditemukan untuk misi " + questIdStr + ": " + obj.getClass().getName());
                        }
                    }
                }

                boolean completed = questSection.getBoolean("completed", false);

                // Menggunakan konstruktor yang sesuai
                Quest quest = new Quest(questId, creatorId, title, description,
                        requiredItems, rewardItems); // Menggunakan Map<Material, Integer> dan List<ItemStack> yang sudah dikonversi
                quest.setCompleted(completed); // Set completed status
                quests.put(questId, quest);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Gagal memuat misi dengan ID " + questIdStr + ": " + e.getMessage());
            }
        }
        plugin.getLogger().log(Level.INFO, quests.size() + " misi berhasil dimuat.");
    }

    /**
     * Menyimpan semua misi ke quests.yml.
     */
    public void saveQuests() {
        // Hapus semua entri lama untuk memastikan data yang disimpan adalah yang terbaru
        questConfig.set("quests", null);

        for (Quest quest : quests.values()) {
            String path = "quests." + quest.getQuestId().toString();
            questConfig.set(path + ".creatorId", quest.getCreatorId().toString());
            questConfig.set(path + ".title", quest.getTitle());
            questConfig.set(path + ".description", quest.getDescription());
            questConfig.set(path + ".completed", quest.isCompleted());

            // Serialisasi requiredItems
            ConfigurationSection reqItemsSection = questConfig.createSection(path + ".requiredItems");
            for (Map.Entry<Material, Integer> entry : quest.getRequiredItems().entrySet()) {
                reqItemsSection.set(entry.getKey().name(), entry.getValue());
            }

            // Serialisasi rewardItems (sekarang sebagai List<ItemStack>)
            questConfig.set(path + ".rewardItems", quest.getRewardItems());
        }

        try {
            questConfig.save(questFile);
            plugin.getLogger().log(Level.INFO, "Semua misi berhasil disimpan.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal menyimpan quests.yml: " + e.getMessage());
        }
    }

    /**
     * Menambahkan misi baru.
     * @param quest Objek misi yang akan ditambahkan.
     */
    public void addQuest(Quest quest) {
        quests.put(quest.getQuestId(), quest);
        saveQuests();
    }

    /**
     * Mendapatkan misi berdasarkan ID.
     * @param questId ID misi.
     * @return Objek Quest, atau null jika tidak ditemukan.
     */
    public Quest getQuest(UUID questId) {
        return quests.get(questId);
    }

    /**
     * Mendapatkan semua misi yang belum selesai.
     * @return Daftar misi yang belum selesai.
     */
    public List<Quest> getActiveQuests() {
        List<Quest> active = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (!quest.isCompleted()) {
                active.add(quest);
            }
        }
        return active;
    }

    /**
     * Mendapatkan semua misi (aktif dan selesai).
     * @return Daftar semua misi.
     */
    public List<Quest> getAllQuests() {
        return new ArrayList<>(quests.values());
    }

    /**
     * Memeriksa dan mengurangi item yang dibutuhkan dari inventaris pemain.
     * @param player Pemain yang mencoba menyelesaikan misi.
     * @param quest Misi yang akan diselesaikan.
     * @return True jika pemain memiliki semua item yang dibutuhkan dan berhasil dikurangi, false jika tidak.
     */
    public boolean checkAndRemoveRequiredItems(Player player, Quest quest) {
        // Periksa apakah pemain memiliki semua item yang dibutuhkan
        for (Map.Entry<Material, Integer> requiredEntry : quest.getRequiredItems().entrySet()) {
            Material requiredMaterial = requiredEntry.getKey();
            int requiredAmount = requiredEntry.getValue();
            int currentAmount = 0;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == requiredMaterial) {
                    currentAmount += item.getAmount();
                }
            }

            if (currentAmount < requiredAmount) {
                player.sendMessage("§cAnda tidak memiliki cukup " + requiredMaterial.name().replace("_", " ") + " untuk menyelesaikan misi ini.");
                return false; // Pemain tidak memiliki cukup item
            }
        }

        // Jika semua item ada, kurangi dari inventaris pemain
        for (Map.Entry<Material, Integer> requiredEntry : quest.getRequiredItems().entrySet()) {
            Material requiredMaterial = requiredEntry.getKey();
            int requiredAmount = requiredEntry.getValue();

            // Gunakan removeItem method dari Inventory untuk pengurangan yang aman
            player.getInventory().removeItem(new ItemStack(requiredMaterial, requiredAmount));
        }
        return true;
    }

    /**
     * Memberikan hadiah misi kepada pemain.
     * @param player Pemain yang menyelesaikan misi.
     * @param quest Misi yang diselesaikan.
     */
    public void giveRewardItems(Player player, Quest quest) {
        for (ItemStack rewardItem : quest.getRewardItems()) { // Iterasi melalui List<ItemStack>
            // Tambahkan item ke inventaris pemain, jatuhkan ke tanah jika inventaris penuh
            if (player.getInventory().addItem(rewardItem.clone()).size() > 0) { // Gunakan .clone() untuk mencegah modifikasi item asli
                player.getWorld().dropItemNaturally(player.getLocation(), rewardItem.clone());
                player.sendMessage("§eBeberapa hadiah Anda dijatuhkan di tanah karena inventaris penuh.");
            }
        }
    }
}
