package com.yourcompany.myquestplugin.gui;

import com.yourcompany.myquestplugin.MyQuestPlugin;
import com.yourcompany.myquestplugin.Quest;
import com.yourcompany.myquestplugin.command.QuestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class QuestGUI {

    private final MyQuestPlugin plugin;
    // Map untuk melacak status pembuatan misi per pemain
    private final Map<UUID, QuestCreationState> playerCreationStates;
    // Map untuk melacak pemain yang sedang menunggu input chat untuk jumlah item
    private final Map<UUID, ItemInputState> playerItemInputStates;
    // Map untuk melacak item yang dipilih pemain di GUI hasil
    private final Map<UUID, Map<Material, Integer>> playerSelectedClaimItems; // Pemain UUID -> Material -> Amount selected
    // Map untuk melacak halaman saat ini di GUI hasil
    private final Map<UUID, Integer> storedItemsPage;


    // List of common materials to display in the item selection GUI
    // This list is now dynamically generated to include all obtainable items
    private static final List<Material> ALL_OBTAINABLE_MATERIALS;
    static {
        ALL_OBTAINABLE_MATERIALS = Arrays.stream(Material.values())
                .filter(Material::isItem) // Filter untuk item yang sebenarnya
                .filter(material -> !material.isLegacy()) // Kecualikan material lama
                .filter(material -> material != Material.AIR) // Kecualikan udara
                // Penambahan filter baru berdasarkan permintaan pengguna:
                .filter(material -> material != Material.BEDROCK) // Hapus bedrock
                .filter(material -> material != Material.SPAWNER) // Hapus spawner
                .filter(material -> material != Material.REINFORCED_DEEPSLATE) // Hapus reinforced deepslate
                .filter(material -> material != Material.STRUCTURE_VOID) // Hapus STRUCTURE_VOID
                .filter(material -> material != Material.TRIAL_SPAWNER) // Hapus TRIAL_SPAWNER
                .filter(material -> {
                    // Kecualikan semua material dasar ramuan
                    if (material == Material.POTION ||
                        material == Material.SPLASH_POTION ||
                        material == Material.LINGERING_POTION) {
                        return false; // Jangan sertakan ramuan
                    }
                    // Kemudian terapkan filter umum lainnya untuk material non-ramuan
                    return !material.name().endsWith("_SPAWN_EGG") &&
                           !material.name().endsWith("_BANNER") &&
                           !material.name().endsWith("_HEAD") &&
                           !material.name().contains("COMMAND_BLOCK") &&
                           !material.name().contains("DEBUG_STICK") &&
                           !material.name().contains("JIGSAW") &&
                           !material.name().contains("STRUCTURE_BLOCK") &&
                           !material.name().contains("BARRIER") &&
                           !material.name().contains("LIGHT") &&
                           !material.name().contains("KNOWLEDGE_BOOK");
                })
                .sorted(Comparator.comparing(Enum::name)) // Urutkan secara alfabetis untuk tampilan yang konsisten
                .collect(Collectors.toList());
    }

    private static final int ITEMS_PER_PAGE = 45; // 5 baris x 9 slot

    public QuestGUI(MyQuestPlugin plugin) {
        this.plugin = plugin;
        this.playerCreationStates = new HashMap<>();
        this.playerItemInputStates = new HashMap<>();
        this.playerSelectedClaimItems = new HashMap<>(); // Inisialisasi map baru
        this.storedItemsPage = new HashMap<>(); // Inisialisasi map halaman
    }

    /**
     * Membuka GUI utama pembuatan misi untuk pemain.
     * @param player Pemain yang akan membuka GUI.
     */
    public void openQuestCreationGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Buat Misi Baru"));

        // Inisialisasi state pembuatan misi jika belum ada
        playerCreationStates.computeIfAbsent(player.getUniqueId(), k -> new QuestCreationState());
        QuestCreationState state = playerCreationStates.get(player.getUniqueId());

        // Item untuk input Judul
        ItemStack titleItem = createGuiItem(Material.NAME_TAG, "§aJudul Misi", "§7Klik untuk mengatur judul.");
        gui.setItem(10, titleItem);

        // Item untuk input Deskripsi
        ItemStack descriptionItem = createGuiItem(Material.PAPER, "§aDeskripsi Misi", "§7Klik untuk mengatur deskripsi.");
        gui.setItem(12, descriptionItem);

        // Item untuk menambah Item yang Dibutuhkan
        ItemStack requiredItemsItem = createGuiItem(Material.CHEST, "§aItem yang Dibutuhkan", "§7Klik untuk menambah item yang dibutuhkan.");
        gui.setItem(14, requiredItemsItem);

        // Item untuk menambah Item Hadiah
        ItemStack rewardItemsItem = createGuiItem(Material.DIAMOND, "§aItem Hadiah (Opsional)", "§7Klik untuk menambah item hadiah.");
        gui.setItem(16, rewardItemsItem);

        // Item untuk membuat Misi
        ItemStack createQuestItem = createGuiItem(Material.LIME_WOOL, "§aBuat Misi", "§7Klik untuk membuat misi ini.");
        gui.setItem(22, createQuestItem);

        // Tampilkan ringkasan saat ini
        updateQuestSummary(player, gui);

        player.openInventory(gui);
    }

    /**
     * Memperbarui ringkasan misi di GUI.
     * @param player Pemain yang sedang melihat GUI.
     * @param gui Inventaris GUI.
     */
    private void updateQuestSummary(Player player, Inventory gui) {
        QuestCreationState state = playerCreationStates.get(player.getUniqueId());
        if (state == null) return;

        // Update Judul
        ItemStack titleItem = gui.getItem(10);
        if (titleItem != null) {
            ItemMeta meta = titleItem.getItemMeta();
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("§7Klik untuk mengatur judul."));
            loreComponents.add(Component.text("§fSaat Ini: §e" + (state.title != null ? state.title : "Belum diatur")));
            meta.setLore(loreComponents.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList()));
            titleItem.setItemMeta(meta);
        }

        // Update Deskripsi
        ItemStack descriptionItem = gui.getItem(12);
        if (descriptionItem != null) {
            ItemMeta meta = descriptionItem.getItemMeta();
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("§7Klik untuk mengatur deskripsi."));
            loreComponents.add(Component.text("§fSaat Ini: §e" + (state.description != null ? state.description : "Belum diatur")));
            meta.setLore(loreComponents.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList()));
            descriptionItem.setItemMeta(meta);
        }

        // Update Item yang Dibutuhkan
        ItemStack requiredItemsItem = gui.getItem(14);
        if (requiredItemsItem != null) {
            ItemMeta meta = requiredItemsItem.getItemMeta();
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("§7Klik untuk menambah item yang dibutuhkan."));
            if (state.requiredItems.isEmpty()) {
                loreComponents.add(Component.text("§fSaat Ini: §eTidak ada item."));
            } else {
                state.requiredItems.forEach((mat, amt) -> loreComponents.add(Component.text("§f- §e" + amt + "x " + mat.name().replace("_", " "))));
            }
            meta.setLore(loreComponents.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList()));
            requiredItemsItem.setItemMeta(meta);
        }

        // Update Item Hadiah
        ItemStack rewardItemsItem = gui.getItem(16);
        if (rewardItemsItem != null) {
            ItemMeta meta = rewardItemsItem.getItemMeta();
            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("§7Klik untuk menambah item hadiah."));
            if (state.rewardItems.isEmpty()) {
                loreComponents.add(Component.text("§fSaat Ini: §eTidak ada item."));
            } else {
                // Tampilkan nama item dari ItemStack, bukan hanya Material
                state.rewardItems.forEach(itemStack -> {
                    String itemName = itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() ?
                            LegacyComponentSerializer.legacySection().serialize(itemStack.getItemMeta().displayName()) :
                            itemStack.getType().name().replace("_", " ");
                    loreComponents.add(Component.text("§f- §e" + itemStack.getAmount() + "x " + itemName));
                });
            }
            meta.setLore(loreComponents.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList()));
            rewardItemsItem.setItemMeta(meta);
        }
    }

    /**
     * Membuka GUI pemilihan item untuk item yang dibutuhkan (dengan paginasi).
     * @param player Pemain yang akan membuka GUI.
     */
    public void openRequiredItemSelectionGUI(Player player) {
        QuestCreationState state = playerCreationStates.get(player.getUniqueId());
        if (state == null) {
            player.sendMessage("§cTerjadi kesalahan dengan sesi pembuatan misi Anda. Silakan coba lagi.");
            player.closeInventory();
            return;
        }

        int totalPages = (int) Math.ceil((double) ALL_OBTAINABLE_MATERIALS.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1; // Ensure at least one page if no materials
        if (state.requiredItemsPage < 0) state.requiredItemsPage = 0;
        if (state.requiredItemsPage >= totalPages) state.requiredItemsPage = totalPages - 1;

        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Pilih Item Dibutuhkan (Hal " + (state.requiredItemsPage + 1) + "/" + totalPages + ")"));

        int startIndex = state.requiredItemsPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, ALL_OBTAINABLE_MATERIALS.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Material material = ALL_OBTAINABLE_MATERIALS.get(i);
            // Ensure the material is a valid item and not a placeholder or special block
            if (material.isItem() && material != Material.AIR && material != Material.GRAY_STAINED_GLASS_PANE) {
                gui.setItem(slot, createGuiItem(material, "§f" + material.name().replace("_", " "), "§7Klik untuk memilih item ini."));
                slot++;
            }
        }

        // Navigation buttons
        // Only show arrow if there are more pages
        if (state.requiredItemsPage > 0) {
            gui.setItem(45, createGuiItem(Material.ARROW, "§aHalaman Sebelumnya", "§7Klik untuk melihat halaman sebelumnya."));
        } else {
            gui.setItem(45, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", " ")); // Placeholder
        }

        if (state.requiredItemsPage < totalPages - 1) {
            gui.setItem(53, createGuiItem(Material.ARROW, "§aHalaman Berikutnya", "§7Klik untuk melihat halaman berikutnya."));
        } else {
            gui.setItem(53, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", " ")); // Placeholder
        }
        gui.setItem(49, createGuiItem(Material.BARRIER, "§cKembali", "§7Kembali ke GUI pembuatan misi."));

        player.openInventory(gui);
    }

    /**
     * Membuka GUI untuk menambahkan item hadiah (dengan drag-and-drop).
     * @param player Pemain yang akan membuka GUI.
     */
    public void openRewardDropGUI(Player player) {
        QuestCreationState state = playerCreationStates.get(player.getUniqueId());
        if (state == null) {
            player.sendMessage("§cTerjadi kesalahan dengan sesi pembuatan misi Anda. Silakan coba lagi.");
            player.closeInventory();
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Tambahkan Hadiah Misi")); // 3 baris

        // Isi GUI dengan item hadiah yang sudah ada
        int slot = 0;
        for (ItemStack itemStack : state.rewardItems) { // Iterasi melalui List<ItemStack>
            if (slot >= 18) break; // Hanya 18 slot untuk item hadiah (slot 0-17)
            gui.setItem(slot, itemStack.clone()); // Gunakan .clone() untuk mencegah modifikasi item asli
            slot++;
        }

        // Tombol Kembali dan Selesai
        gui.setItem(18, createGuiItem(Material.BARRIER, "§cKembali", "§7Kembali ke GUI pembuatan misi.")); // Slot kiri bawah
        gui.setItem(26, createGuiItem(Material.LIME_WOOL, "§aSelesai", "§7Klik untuk mengonfirmasi hadiah.")); // Slot kanan bawah

        player.openInventory(gui);
    }

    /**
     * Membuka GUI untuk mengklaim item yang disimpan oleh pembuat misi.
     * @param player Pemain yang akan membuka GUI.
     */
    public void openStoredItemsGUI(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Material, Integer> storedItems = plugin.getQuestStorageManager().getStoredItems(playerId);
        Map<Material, Integer> selectedItems = playerSelectedClaimItems.computeIfAbsent(playerId, k -> new HashMap<>());
        int currentPage = storedItemsPage.computeIfAbsent(playerId, k -> 0);

        List<Map.Entry<Material, Integer>> itemsList = new ArrayList<>(storedItems.entrySet());
        int totalPages = (int) Math.ceil((double) itemsList.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage < 0) currentPage = 0;
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        storedItemsPage.put(playerId, currentPage); // Update current page in state

        Inventory gui = Bukkit.createInventory(null, 54, Component.text("Klaim Hasil Misi (Hal " + (currentPage + 1) + "/" + totalPages + ")"));

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, itemsList.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<Material, Integer> entry = itemsList.get(i);
            Material material = entry.getKey();
            int amount = entry.getValue();

            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Jumlah Tersedia: §f" + amount));

            // Cek apakah item ini sudah dipilih
            if (selectedItems.containsKey(material)) {
                lore.add(Component.text("§a[Dipilih untuk Klaim]", NamedTextColor.GREEN));
                // Menggunakan NamespacedKey untuk mendapatkan Enchantment Unbreaking
                Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
                if (unbreaking != null) {
                    meta.addEnchant(unbreaking, 1, true); // Tambahkan glow
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            } else {
                lore.add(Component.text("§7Klik untuk memilih item ini."));
            }
            meta.setLore(lore.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList()));
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }

        // Navigation buttons
        if (currentPage > 0) {
            gui.setItem(45, createGuiItem(Material.ARROW, "§aHalaman Sebelumnya", "§7Klik untuk melihat halaman sebelumnya."));
        } else {
            gui.setItem(45, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", " "));
        }

        gui.setItem(48, createGuiItem(Material.LIME_WOOL, "§aKlaim Item Terpilih", "§7Klik untuk mengambil item yang Anda pilih."));
        gui.setItem(49, createGuiItem(Material.BARRIER, "§cKembali", "§7Kembali ke menu utama.")); // Kembali ke menu utama, bukan pembuatan misi

        if (currentPage < totalPages - 1) {
            gui.setItem(53, createGuiItem(Material.ARROW, "§aHalaman Berikutnya", "§7Klik untuk melihat halaman berikutnya."));
        } else {
            gui.setItem(53, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", " "));
        }

        player.openInventory(gui);
    }


    /**
     * Menangani klik di inventaris GUI.
     * @param event Event klik inventaris.
     */
    public void handleInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();
        Inventory clickedInventory = event.getClickedInventory(); // Inventaris yang diklik (GUI atau pemain)
        Inventory topInventory = event.getView().getTopInventory(); // Inventaris GUI yang terbuka

        // Check if it's the main quest creation GUI
        if (topInventory.equals(player.getOpenInventory().getTopInventory()) && event.getView().title().equals(Component.text("Buat Misi Baru"))) {
            event.setCancelled(true); // Batalkan semua klik di GUI ini

            QuestCreationState state = playerCreationStates.get(player.getUniqueId());
            if (state == null) {
                player.sendMessage("§cTerjadi kesalahan dengan sesi pembuatan misi Anda. Silakan coba lagi.");
                player.closeInventory();
                return;
            }

            switch (slot) {
                case 10: // Judul Misi
                    player.sendMessage("§aKetik judul misi di chat sekarang:");
                    state.awaitingTitleInput = true;
                    player.closeInventory();
                    break;
                case 12: // Deskripsi Misi
                    player.sendMessage("§aKetik deskripsi misi di chat sekarang:");
                    state.awaitingDescriptionInput = true;
                    player.closeInventory();
                    break;
                case 14: // Item yang Dibutuhkan
                    openRequiredItemSelectionGUI(player); // Buka GUI pemilihan item untuk item yang dibutuhkan (paginasi)
                    break;
                case 16: // Item Hadiah
                    openRewardDropGUI(player); // Buka GUI drop item untuk hadiah
                    break;
                case 22: // Buat Misi
                    createQuest(player, state);
                    break;
            }
        }
        // Check if it's the paginated required item selection GUI
        // Use LegacyComponentSerializer to reliably compare titles
        else if (topInventory.equals(player.getOpenInventory().getTopInventory()) && LegacyComponentSerializer.legacySection().serialize(event.getView().title()).startsWith("Pilih Item Dibutuhkan")) {
            event.setCancelled(true); // Batalkan semua klik di GUI ini

            QuestCreationState state = playerCreationStates.get(player.getUniqueId());
            if (state == null) {
                player.sendMessage("§cTerjadi kesalahan dengan sesi pembuatan misi Anda. Silakan coba lagi.");
                player.closeInventory();
                return;
            }

            // Handle navigation buttons
            // Ensure the click is on the top inventory (the GUI itself)
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                if (slot == 45) { // Previous Page button slot
                    if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                        if (state.requiredItemsPage > 0) {
                            state.requiredItemsPage--;
                            openRequiredItemSelectionGUI(player);
                        }
                    }
                    return; // Always return after checking a navigation slot
                }
                if (slot == 53) { // Next Page button slot
                    if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                        int totalPages = (int) Math.ceil((double) ALL_OBTAINABLE_MATERIALS.size() / ITEMS_PER_PAGE);
                        if (state.requiredItemsPage < totalPages - 1) {
                            state.requiredItemsPage++;
                            openRequiredItemSelectionGUI(player);
                        }
                    }
                    return; // Always return after checking a navigation slot
                }
                if (slot == 49) { // Back Button slot
                    if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                        openQuestCreationGUI(player);
                    }
                    return; // Always return after checking a navigation slot
                }

                // If a material item is clicked (not a navigation or placeholder button)
                if (clickedItem != null && clickedItem.getType() != Material.AIR && clickedItem.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    // Check if the click is within the item display area (slots 0-44)
                    if (slot >= 0 && slot < ITEMS_PER_PAGE) {
                        state.selectedMaterial = clickedItem.getType();
                        state.addingToRequired = true; // Selalu true untuk GUI ini
                        player.sendMessage("§aAnda memilih §e" + state.selectedMaterial.name().replace("_", " ") + "§a. Sekarang, ketik jumlah yang diinginkan di chat.");
                        player.closeInventory(); // Tutup GUI pemilihan item
                        playerItemInputStates.put(player.getUniqueId(), new ItemInputState(state.selectedMaterial, state.addingToRequired));
                        return; // Important: Return after selecting an item
                    }
                }
            }
            // If the click is in the player's inventory while this GUI is open, cancel it.
            else if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                event.setCancelled(true); // Prevent interaction with player's inventory
                return;
            }
        }
        // Check if it's the reward item drop GUI
        else if (topInventory.equals(player.getOpenInventory().getTopInventory()) && event.getView().title().equals(Component.text("Tambahkan Hadiah Misi"))) {
            // Izinkan pemain untuk mengambil item dari inventaris mereka dan meletakkannya di GUI hadiah
            // Atau mengambil item dari GUI hadiah kembali ke inventaris mereka.

            QuestCreationState state = playerCreationStates.get(player.getUniqueId());
            if (state == null) {
                player.sendMessage("§cTerjadi kesalahan dengan sesi pembuatan misi Anda. Silakan coba lagi.");
                player.closeInventory();
                return;
            }

            // Handle Back and Done buttons
            if (slot == 18 && clickedItem != null && clickedItem.getType() == Material.BARRIER) { // Tombol Kembali
                event.setCancelled(true);
                // Konsolidasi item dari GUI ke state.rewardItems saat tombol kembali ditekan
                state.rewardItems.clear(); // Hapus semua entri lama
                for (int i = 0; i < 18; i++) { // Hanya slot item hadiah (0-17)
                    ItemStack item = topInventory.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        state.rewardItems.add(item.clone()); // Tambahkan ItemStack lengkap
                    }
                }
                player.sendMessage("§aHadiah misi telah dikonfirmasi."); // Tambahkan pesan konfirmasi saat kembali
                openQuestCreationGUI(player);
                return;
            }
            if (slot == 26 && clickedItem != null && clickedItem.getType() == Material.LIME_WOOL) { // Tombol Selesai
                event.setCancelled(true);
                // Konsolidasi item dari GUI ke state.rewardItems
                state.rewardItems.clear(); // Hapus semua entri lama
                for (int i = 0; i < 18; i++) { // Hanya slot item hadiah (0-17)
                    ItemStack item = topInventory.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        state.rewardItems.add(item.clone()); // Tambahkan ItemStack lengkap
                    }
                }
                player.sendMessage("§aHadiah misi telah dikonfirmasi.");
                openQuestCreationGUI(player);
                return;
            }

            // Allow placing items into the reward slots (0-17) from player inventory
            // Allow taking items from reward slots (0-17) to player inventory
            // Allow moving items within reward slots (0-17)
            if (clickedInventory != null && clickedInventory.equals(topInventory) && slot >= 0 && slot < 18) {
                // If player is putting item from cursor to GUI or moving within GUI
                event.setCancelled(false);
            } else if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                // If player is taking item from their inventory to GUI (e.g., shift-click)
                event.setCancelled(false);
            } else {
                // Cancel clicks on other slots (e.g., outside reward slots, or on border items)
                event.setCancelled(true);
            }
        }
        // Check if it's the stored items claim GUI
        else if (topInventory.equals(player.getOpenInventory().getTopInventory()) && LegacyComponentSerializer.legacySection().serialize(event.getView().title()).startsWith("Klaim Hasil Misi")) {
            event.setCancelled(true); // Batalkan semua klik di GUI ini

            UUID playerId = player.getUniqueId();
            Map<Material, Integer> storedItems = plugin.getQuestStorageManager().getStoredItems(playerId);
            Map<Material, Integer> selectedItems = playerSelectedClaimItems.computeIfAbsent(playerId, k -> new HashMap<>());
            int currentPage = storedItemsPage.computeIfAbsent(playerId, k -> 0);

            // Handle navigation and action buttons
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                if (slot == 45) { // Previous Page
                    if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                        if (currentPage > 0) {
                            storedItemsPage.put(playerId, currentPage - 1);
                            openStoredItemsGUI(player);
                        }
                    }
                    return;
                }
                if (slot == 53) { // Next Page
                    if (clickedItem != null && clickedItem.getType() == Material.ARROW) {
                        List<Map.Entry<Material, Integer>> itemsList = new ArrayList<>(storedItems.entrySet());
                        int totalPages = (int) Math.ceil((double) itemsList.size() / ITEMS_PER_PAGE);
                        if (currentPage < totalPages - 1) {
                            storedItemsPage.put(playerId, currentPage + 1);
                            openStoredItemsGUI(player);
                        }
                    }
                    return;
                }
                if (slot == 49) { // Back to main menu
                    if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                        player.closeInventory(); // Close the GUI
                        playerSelectedClaimItems.remove(playerId); // Clear selection state
                        storedItemsPage.remove(playerId); // Clear page state
                    }
                    return;
                }
                if (slot == 48) { // Claim Selected Items
                    if (clickedItem != null && clickedItem.getType() == Material.LIME_WOOL) {
                        if (selectedItems.isEmpty()) {
                            player.sendMessage("§cAnda belum memilih item untuk diklaim.");
                            return;
                        }

                        player.sendMessage("§aMencoba memberikan item yang Anda pilih...");
                        int givenCount = 0;
                        for (Map.Entry<Material, Integer> entry : new HashMap<>(selectedItems).entrySet()) { // Iterate over a copy
                            Material materialToClaim = entry.getKey();
                            int amountToClaim = entry.getValue();

                            // Dapatkan jumlah sebenarnya yang tersedia di penyimpanan
                            int actualStoredAmount = storedItems.getOrDefault(materialToClaim, 0);
                            int amountToGive = Math.min(amountToClaim, actualStoredAmount); // Jangan berikan lebih dari yang tersedia

                            if (amountToGive > 0) {
                                ItemStack itemToGive = new ItemStack(materialToClaim, amountToGive);
                                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(itemToGive);
                                if (!remaining.isEmpty()) {
                                    for (ItemStack remainingItem : remaining.values()) {
                                        player.getWorld().dropItemNaturally(player.getLocation(), remainingItem);
                                        player.sendMessage("§eBeberapa item Anda dijatuhkan di tanah karena inventaris penuh.");
                                    }
                                }
                                // Hapus jumlah yang diberikan dari penyimpanan
                                plugin.getQuestStorageManager().removeStoredItems(playerId, materialToClaim, amountToGive);
                                givenCount++;
                            }
                        }

                        if (givenCount > 0) {
                            player.sendMessage("§aItem yang Anda pilih telah diberikan!");
                            playerSelectedClaimItems.remove(playerId); // Clear selection after claiming
                            openStoredItemsGUI(player); // Refresh GUI
                        } else {
                            player.sendMessage("§cGagal memberikan item yang Anda pilih. Inventaris mungkin penuh atau item tidak lagi tersedia.");
                        }
                    }
                    return;
                }

                // Item selection logic
                if (clickedItem != null && clickedItem.getType() != Material.AIR && clickedItem.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    if (slot >= 0 && slot < ITEMS_PER_PAGE) { // Clicked an item slot
                        Material clickedMaterial = clickedItem.getType();
                        int storedAmount = storedItems.getOrDefault(clickedMaterial, 0);

                        if (storedAmount > 0) { // Only allow selecting if there's actually something to claim
                            if (selectedItems.containsKey(clickedMaterial)) {
                                // Item sudah dipilih, batalkan pilihan (hapus dari selectedItems)
                                selectedItems.remove(clickedMaterial);
                                player.sendMessage("§ePilihan item " + clickedMaterial.name().replace("_", " ") + " dibatalkan.");
                            } else {
                                // Item belum dipilih, pilih (tambahkan ke selectedItems dengan jumlah penuh yang tersedia)
                                selectedItems.put(clickedMaterial, storedAmount);
                                player.sendMessage("§aItem " + clickedMaterial.name().replace("_", " ") + " dipilih untuk diklaim.");
                            }
                            openStoredItemsGUI(player); // Refresh GUI untuk menampilkan status pilihan
                        } else {
                            player.sendMessage("§cTidak ada item ini yang tersedia untuk diklaim.");
                        }
                    }
                }
            }
            // Prevent interaction with player's inventory while this GUI is open
            else if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Menangani input chat dari pemain untuk judul, deskripsi, atau jumlah item.
     * @param player Pemain yang mengirim pesan chat.
     * @param message Pesan chat.
     * @return True jika pesan chat ditangani oleh GUI, false jika tidak.
     */
    public boolean handleChatInput(Player player, String message) {
        UUID playerId = player.getUniqueId();
        QuestCreationState state = playerCreationStates.get(playerId);
        ItemInputState itemInputState = playerItemInputStates.get(playerId);

        if (state == null) {
            return false; // Pemain tidak dalam sesi pembuatan misi
        }

        if (state.awaitingTitleInput) {
            state.title = message;
            state.awaitingTitleInput = false;
            player.sendMessage("§aJudul misi diatur menjadi: §e" + message);
            // Buka kembali GUI setelah input
            new BukkitRunnable() {
                @Override
                public void run() {
                    openQuestCreationGUI(player);
                }
            }.runTaskLater(plugin, 1L);
            return true;
        } else if (state.awaitingDescriptionInput) {
            state.description = message;
            state.awaitingDescriptionInput = false;
            player.sendMessage("§aDeskripsi misi diatur menjadi: §e" + message);
            // Buka kembali GUI setelah input
            new BukkitRunnable() {
                @Override
                public void run() {
                    openQuestCreationGUI(player);
                }
            }.runTaskLater(plugin, 1L);
            return true;
        } else if (itemInputState != null) {
            try {
                int amount = Integer.parseInt(message);
                if (amount <= 0) {
                    player.sendMessage("§cJumlah harus lebih besar dari nol. Silakan ketik ulang jumlahnya.");
                    return true; // Tetap menunggu input yang valid
                }

                if (itemInputState.addingToRequired) {
                    state.requiredItems.merge(itemInputState.material, amount, Integer::sum);
                    player.sendMessage("§aDitambahkan §e" + amount + "x " + itemInputState.material.name().replace("_", " ") + "§a ke item yang dibutuhkan.");
                } else {
                    // Logika ini seharusnya tidak terpanggil lagi karena hadiah sekarang menggunakan GUI drag-and-drop.
                    // Namun, untuk jaga-jaga, kita biarkan saja.
                    // state.rewardItems.merge(itemInputState.material, amount, Integer::sum); // Ini tidak lagi digunakan
                    player.sendMessage("§aDitambahkan §e" + amount + "x " + itemInputState.material.name().replace("_", " ") + "§a ke item hadiah.");
                }

                playerItemInputStates.remove(playerId); // Hapus status input chat
                // Buka kembali GUI setelah input
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openQuestCreationGUI(player);
                    }
                }.runTaskLater(plugin, 1L);
                return true;
            } catch (NumberFormatException e) {
                player.sendMessage("§cJumlah tidak valid. Silakan masukkan angka.");
                return true;
            }
        }
        return false;
    }

    /**
     * Membuat misi baru berdasarkan state saat ini.
     * @param player Pemain yang membuat misi.
     * @param state State pembuatan misi pemain.
     */
    private void createQuest(Player player, QuestCreationState state) {
        if (state.title == null || state.title.isEmpty()) {
            player.sendMessage("§cJudul misi belum diatur.");
            return;
        }
        if (state.description == null || state.description.isEmpty()) {
            player.sendMessage("§cDeskripsi misi belum diatur.");
            return;
        }
        if (state.requiredItems.isEmpty()) {
            player.sendMessage("§cAnda harus menambahkan setidaknya satu item yang dibutuhkan.");
            return;
        }

        UUID questId = UUID.randomUUID();
        Quest newQuest = new Quest(questId, player.getUniqueId(), state.title, state.description,
                state.requiredItems, state.rewardItems); // Mengirim List<ItemStack>

        plugin.getQuestManager().addQuest(newQuest);
        playerCreationStates.remove(player.getUniqueId()); // Hapus state setelah misi dibuat
        player.closeInventory();
        player.sendMessage("§aMisi Anda '" + newQuest.getTitle() + "' berhasil dibuat!");

        // Berikan buku misi kepada pembuat
        ItemStack questBook = new QuestCommand(plugin).createQuestBook(newQuest);
        if (player.getInventory().addItem(questBook).size() > 0) {
            player.sendMessage("§eInventaris Anda penuh, buku misi dijatuhkan di tanah.");
            player.getWorld().dropItemNaturally(player.getLocation(), questBook);
        } else {
            player.sendMessage("§aBuku misi Anda telah diberikan.");
        }
    }

    /**
     * Helper method untuk membuat ItemStack untuk GUI.
     */
    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name));

        List<String> serializedLore = Arrays.stream(lore)
                .map(s -> LegacyComponentSerializer.legacySection().serialize(Component.text(s)))
                .collect(Collectors.toList());
        meta.setLore(serializedLore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Kelas internal untuk melacak state pembuatan misi per pemain.
     */
    private static class QuestCreationState {
        String title;
        String description;
        Map<Material, Integer> requiredItems;
        List<ItemStack> rewardItems; // Diubah menjadi List<ItemStack>
        boolean awaitingTitleInput;
        boolean awaitingDescriptionInput;
        boolean awaitingItemSelection; // Tidak terlalu digunakan lagi, tapi biarkan saja
        boolean addingToRequired; // True jika menambahkan ke required, false jika ke reward
        Material selectedMaterial; // Material yang dipilih saat ini
        int requiredItemsPage; // Halaman saat ini untuk pemilihan item yang dibutuhkan

        public QuestCreationState() {
            this.requiredItems = new HashMap<>();
            this.rewardItems = new ArrayList<>(); // Inisialisasi sebagai ArrayList
            this.awaitingTitleInput = false;
            this.awaitingDescriptionInput = false;
            this.awaitingItemSelection = false;
            this.addingToRequired = false;
            this.selectedMaterial = null;
            this.requiredItemsPage = 0;
        }
    }

    /**
     * Kelas internal untuk melacak status input jumlah item melalui chat.
     */
    private static class ItemInputState {
        Material material;
        boolean addingToRequired;

        public ItemInputState(Material material, boolean addingToRequired) {
            this.material = material;
            this.addingToRequired = addingToRequired;
        }
    }
}
