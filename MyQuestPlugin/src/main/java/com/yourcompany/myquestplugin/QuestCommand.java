package com.yourcompany.myquestplugin.command;

import com.yourcompany.myquestplugin.MyQuestPlugin;
import com.yourcompany.myquestplugin.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestCommand implements CommandExecutor {

    private final MyQuestPlugin plugin;

    public QuestCommand(MyQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPerintah ini hanya bisa digunakan oleh pemain.");
            return true;
        }

        if (args.length == 0) {
            // Jika tidak ada argumen, buka GUI pembuatan misi
            plugin.getQuestGUI().openQuestCreationGUI(player);
            return true;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("complete")) {
            // Perintah untuk menyelesaikan misi dari buku
            try {
                UUID questId = UUID.fromString(args[1]);
                Quest quest = plugin.getQuestManager().getQuest(questId);

                if (quest == null) {
                    player.sendMessage("§cMisi tidak ditemukan.");
                    return true;
                }
                if (quest.isCompleted()) {
                    player.sendMessage("§cMisi ini sudah selesai.");
                    return true;
                }

                // Cek dan kurangi item yang dibutuhkan
                if (!plugin.getQuestManager().checkAndRemoveRequiredItems(player, quest)) {
                    // Pesan kesalahan sudah diberikan di dalam metode checkAndRemoveRequiredItems
                    return true;
                }

                // Berikan hadiah kepada penyelesai
                plugin.getQuestManager().giveRewardItems(player, quest);
                player.sendMessage("§aSelamat! Anda telah menyelesaikan misi '" + quest.getTitle() + "'!");

                // Tandai misi sebagai selesai
                quest.setCompleted(true);
                plugin.getQuestManager().saveQuests(); // Simpan perubahan status misi

                // Tambahkan item yang dikumpulkan ke storage pembuat misi
                plugin.getQuestStorageManager().addStoredItems(quest.getCreatorId(), quest.getRequiredItems());
                player.sendMessage("§aItem yang Anda kumpulkan telah disimpan untuk pembuat misi.");

                // Hapus buku misi dari inventaris pemain
                ItemStack bookToRemove = null;
                // Serialisasi judul misi untuk perbandingan yang konsisten
                String questTitleSerialized = LegacyComponentSerializer.legacySection().serialize(Component.text(quest.getTitle()));

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                        BookMeta bookMeta = (BookMeta) item.getItemMeta();
                        if (bookMeta != null && bookMeta.hasTitle()) {
                            // Serialisasi judul buku di inventaris untuk perbandingan yang konsisten
                            String bookTitleInInventory = LegacyComponentSerializer.legacySection().serialize(bookMeta.title());
                            if (bookTitleInInventory.equals(questTitleSerialized)) {
                                bookToRemove = item;
                                break; // Buku ditemukan, hentikan pencarian
                            }
                        }
                    }
                }

                if (bookToRemove != null) {
                    player.getInventory().removeItem(bookToRemove);
                    player.sendMessage("§aBuku misi telah dihapus dari inventaris Anda.");
                } else {
                    player.sendMessage("§eBuku misi tidak ditemukan di inventaris Anda.");
                }

                return true;

            } catch (IllegalArgumentException e) {
                player.sendMessage("§cID misi tidak valid.");
                return true;
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            // Menampilkan daftar semua misi (aktif dan selesai) dengan status
            player.sendMessage("§b--- Daftar Misi ---");
            List<Quest> allQuests = plugin.getQuestManager().getAllQuests(); // Gunakan metode baru
            if (allQuests.isEmpty()) {
                player.sendMessage("§7Tidak ada misi saat ini.");
            } else {
                for (Quest quest : allQuests) {
                    String status = quest.isCompleted() ? "§a[Selesai]" : "§e[Tertunda]"; // Tampilkan status
                    Component questLine = Component.text("§f- ")
                            .append(Component.text(quest.getTitle(), NamedTextColor.AQUA))
                            .append(Component.text(" oleh ", NamedTextColor.GRAY))
                            .append(Component.text(Bukkit.getOfflinePlayer(quest.getCreatorId()).getName() != null ? Bukkit.getOfflinePlayer(quest.getCreatorId()).getName() : quest.getCreatorId().toString().substring(0, 8), NamedTextColor.YELLOW))
                            .append(Component.text(" " + status)) // Tambahkan status
                            .append(Component.text(" [Lihat Buku]", NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/myquest getbook " + quest.getQuestId().toString())));
                    player.sendMessage(questLine);
                }
            }
            return true;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("getbook")) {
            // Memberikan buku misi kepada pemain
            try {
                UUID questId = UUID.fromString(args[1]);
                Quest quest = plugin.getQuestManager().getQuest(questId);

                if (quest == null) {
                    player.sendMessage("§cMisi tidak ditemukan.");
                    return true;
                }

                // Buku misi dapat diambil meskipun misi sudah selesai, tetapi tombol "Selesaikan Misi" akan dinonaktifkan
                ItemStack questBook = createQuestBook(quest);
                if (player.getInventory().addItem(questBook).size() > 0) {
                    player.sendMessage("§eInventaris Anda penuh, buku misi dijatuhkan di tanah.");
                    player.getWorld().dropItemNaturally(player.getLocation(), questBook);
                } else {
                    player.sendMessage("§aBuku misi '" + quest.getTitle() + "' telah diberikan kepada Anda.");
                }
                return true;
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cID misi tidak valid.");
                return true;
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("hasil")) {
            // Perintah untuk pembuat misi mengklaim item yang tersimpan
            if (!player.hasPermission("myquestplugin.claim_storage")) { // Izin baru
                player.sendMessage("§cAnda tidak memiliki izin untuk mengklaim hasil misi.");
                return true;
            }

            // Buka GUI untuk mengklaim item
            plugin.getQuestGUI().openStoredItemsGUI(player);
            return true;
        }

        // Pesan penggunaan yang diperbarui
        player.sendMessage("§b--- Panduan Penggunaan MyQuest ---");
        player.sendMessage("§f/myquest §7- Buka GUI untuk membuat misi baru.");
        player.sendMessage("§f/myquest list §7- Lihat daftar semua misi yang tersedia.");
        player.sendMessage("§f/myquest hasil §7- Klaim item yang dikumpulkan dari misi yang Anda buat.");
        return true;
    }

    /**
     * Membuat buku misi interaktif.
     * @param quest Objek misi.
     * @return ItemStack buku misi.
     */
    public ItemStack createQuestBook(Quest quest) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(meta -> {
            // Pastikan meta adalah BookMeta untuk mengakses metode spesifik buku
            if (!(meta instanceof BookMeta bookMeta)) {
                plugin.getLogger().warning("Gagal mendapatkan BookMeta untuk buku misi.");
                return; // Seharusnya tidak terjadi untuk WRITTEN_BOOK
            }

            // Menggunakan Adventure API untuk komponen teks, lalu serialisasi untuk setAuthor/setTitle
            bookMeta.setAuthor(LegacyComponentSerializer.legacySection().serialize(
                    Component.text(Bukkit.getOfflinePlayer(quest.getCreatorId()).getName() != null ? Bukkit.getOfflinePlayer(quest.getCreatorId()).getName() : "Unknown Player")
            ));
            bookMeta.setTitle(LegacyComponentSerializer.legacySection().serialize(
                    Component.text(quest.getTitle())
            ));

            // Halaman pertama: Judul dan Deskripsi
            Component page1 = Component.empty()
                    .append(Component.text("Judul Misi: ", NamedTextColor.GOLD))
                    .append(Component.text(quest.getTitle(), NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text("Deskripsi: ", NamedTextColor.GOLD))
                    .append(Component.text(quest.getDescription(), NamedTextColor.BLACK)) // Ubah ke BLACK
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Dibuat oleh: ", NamedTextColor.GRAY))
                    .append(Component.text(Bukkit.getOfflinePlayer(quest.getCreatorId()).getName() != null ? Bukkit.getOfflinePlayer(quest.getCreatorId()).getName() : "Unknown Player", NamedTextColor.BLACK)); // Ubah ke BLACK
            bookMeta.addPages(page1);

            // Halaman kedua: Item yang Dibutuhkan
            Component page2 = Component.empty()
                    .append(Component.text("Item yang Dibutuhkan:", NamedTextColor.BLACK)) // Ubah ke BLACK
                    .append(Component.newline());
            if (quest.getRequiredItems().isEmpty()) {
                page2 = page2.append(Component.text("Tidak ada item yang dibutuhkan.", NamedTextColor.BLACK)); // Ubah ke BLACK
            } else {
                for (Map.Entry<Material, Integer> entry : quest.getRequiredItems().entrySet()) {
                    page2 = page2.append(Component.text("- " + entry.getValue() + "x " + entry.getKey().name().replace("_", " "), NamedTextColor.BLACK)) // Ubah ke BLACK
                            .append(Component.newline());
                }
            }
            bookMeta.addPages(page2);

            // Halaman ketiga: Hadiah dan Tombol Selesai
            Component page3 = Component.empty()
                    .append(Component.text("Hadiah Misi:", NamedTextColor.BLACK)) // Ubah ke BLACK
                    .append(Component.newline());
            if (quest.getRewardItems().isEmpty()) {
                page3 = page3.append(Component.text("Tidak ada hadiah.", NamedTextColor.BLACK)); // Ubah ke BLACK
            } else {
                // Iterasi melalui List<ItemStack>
                for (ItemStack rewardItem : quest.getRewardItems()) {
                    String itemName = rewardItem.hasItemMeta() && rewardItem.getItemMeta().hasDisplayName() ?
                            LegacyComponentSerializer.legacySection().serialize(rewardItem.getItemMeta().displayName()) :
                            rewardItem.getType().name().replace("_", " ");
                    page3 = page3.append(Component.text("- " + rewardItem.getAmount() + "x " + itemName, NamedTextColor.BLACK))
                            .append(Component.newline());
                }
            }
            page3 = page3.append(Component.newline());

            // Tampilkan tombol "Selesaikan Misi" hanya jika misi belum selesai
            if (!quest.isCompleted()) {
                page3 = page3.append(Component.text("Klik tombol di bawah untuk menyelesaikan misi ini:", NamedTextColor.GRAY))
                        .append(Component.newline())
                        .append(Component.text("[Selesaikan Misi]", NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/myquest complete " + quest.getQuestId().toString()))); // Perintah untuk menyelesaikan misi
            } else {
                page3 = page3.append(Component.text("Misi ini sudah selesai.", NamedTextColor.DARK_GRAY));
            }

            bookMeta.addPages(page3);
        });
        return book;
    }
}
