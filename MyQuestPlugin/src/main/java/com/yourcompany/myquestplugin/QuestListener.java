package com.yourcompany.myquestplugin.listener;

import com.yourcompany.myquestplugin.MyQuestPlugin;
import com.yourcompany.myquestplugin.gui.QuestGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
// import org.bukkit.event.player.PlayerJoinEvent; // Tidak lagi diperlukan

public class QuestListener implements Listener {

    private final MyQuestPlugin plugin;

    public QuestListener(MyQuestPlugin plugin) {
        this.plugin = plugin;
    }

    // Event PlayerJoinEvent dihapus karena logika hadiah tertunda dipindahkan ke QuestStorageManager
    // @EventHandler
    // public void onPlayerJoin(PlayerJoinEvent event) {
    //     // Cek hadiah tertunda saat pemain bergabung
    //     plugin.getPendingRewardManager().checkAndGivePendingRewards(event.getPlayer());
    // }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Delegasikan penanganan klik inventaris ke QuestGUI
        plugin.getQuestGUI().handleInventoryClick(event);
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // Delegasikan penanganan chat ke QuestGUI untuk input jumlah item
        if (plugin.getQuestGUI().handleChatInput(player, event.getMessage())) {
            event.setCancelled(true); // Batalkan event chat jika ditangani oleh GUI
        }
    }
}
