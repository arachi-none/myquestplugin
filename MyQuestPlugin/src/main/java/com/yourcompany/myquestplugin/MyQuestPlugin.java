package com.yourcompany.myquestplugin;

import com.yourcompany.myquestplugin.command.QuestCommand;
import com.yourcompany.myquestplugin.gui.QuestGUI;
import com.yourcompany.myquestplugin.listener.QuestListener;
import com.yourcompany.myquestplugin.manager.QuestManager;
import com.yourcompany.myquestplugin.manager.QuestStorageManager; // Import baru
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public final class MyQuestPlugin extends JavaPlugin {

    private static MyQuestPlugin instance;
    private QuestManager questManager;
    private QuestStorageManager questStorageManager; // Diganti dari pendingRewardManager
    private QuestGUI questGUI;

    @Override
    public void onEnable() {
        // Mengatur instance plugin
        instance = this;
        getLogger().log(Level.INFO, "MyQuestPlugin is starting up...");

        // Inisialisasi manajer data
        this.questManager = new QuestManager(this);
        this.questStorageManager = new QuestStorageManager(this); // Inisialisasi baru

        // Memuat data dari file YAML
        questManager.loadQuests();
        questStorageManager.loadStoredItems(); // Memuat item yang disimpan

        // Inisialisasi GUI
        this.questGUI = new QuestGUI(this);

        // Mendaftarkan perintah
        Objects.requireNonNull(getCommand("myquest")).setExecutor(new QuestCommand(this));

        // Mendaftarkan event listener
        Bukkit.getPluginManager().registerEvents(new QuestListener(this), this);

        getLogger().log(Level.INFO, "MyQuestPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "MyQuestPlugin is shutting down...");

        // Menyimpan data ke file YAML sebelum plugin mati
        questManager.saveQuests();
        questStorageManager.saveStoredItems(); // Menyimpan item yang disimpan

        getLogger().log(Level.INFO, "MyQuestPlugin has been disabled!");
    }

    /**
     * Mengembalikan instance tunggal dari plugin.
     * @return Instance MyQuestPlugin.
     */
    public static MyQuestPlugin getInstance() {
        return instance;
    }

    /**
     * Mengembalikan QuestManager untuk mengelola misi.
     * @return QuestManager.
     */
    public QuestManager getQuestManager() {
        return questManager;
    }

    /**
     * Mengembalikan QuestStorageManager untuk mengelola item yang disimpan.
     * @return QuestStorageManager.
     */
    public QuestStorageManager getQuestStorageManager() { // Getter baru
        return questStorageManager;
    }

    /**
     * Mengembalikan QuestGUI untuk mengelola antarmuka pengguna misi.
     * @return QuestGUI.
     */
    public QuestGUI getQuestGUI() {
        return questGUI;
    }
}
