package com.yourcompany.myquestplugin;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Quest {

    private final UUID questId;
    private final UUID creatorId;
    private String title;
    private String description;
    private final Map<Material, Integer> requiredItems;
    private List<ItemStack> rewardItems; // Diubah dari Map<Material, Integer>
    private boolean completed;

    // Konstruktor baru untuk deserialisasi dari YAML (menggunakan Map<String, Integer> untuk requiredItems dan rewardItems)
    public Quest(UUID questId, UUID creatorId, String title, String description,
                 Map<String, Integer> requiredItemsSerialized, Map<String, Integer> rewardItemsSerialized, boolean completed) {
        this.questId = questId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.completed = completed;

        // Deserialisasi requiredItems
        this.requiredItems = requiredItemsSerialized.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> Material.valueOf(entry.getKey()),
                        Map.Entry::getValue
                ));

        // Deserialisasi rewardItems (ini akan diubah nanti di QuestManager)
        // Untuk saat ini, kita akan mengasumsikan ini masih Material,Integer untuk kompatibilitas sementara
        // Ini akan diubah menjadi List<ItemStack> setelah QuestManager diperbarui
        this.rewardItems = rewardItemsSerialized.entrySet().stream()
                .map(entry -> new ItemStack(Material.valueOf(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    // Konstruktor untuk membuat misi baru (dari GUI)
    public Quest(UUID questId, UUID creatorId, String title, String description,
                 Map<Material, Integer> requiredItems, List<ItemStack> rewardItems) { // Diubah untuk menerima List<ItemStack>
        this.questId = questId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.requiredItems = requiredItems;
        this.rewardItems = rewardItems;
        this.completed = false;
    }

    public UUID getQuestId() {
        return questId;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<Material, Integer> getRequiredItems() {
        return requiredItems;
    }

    public List<ItemStack> getRewardItems() { // Getter diubah
        return rewardItems;
    }

    public void setRewardItems(List<ItemStack> rewardItems) { // Setter diubah
        this.rewardItems = rewardItems;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
