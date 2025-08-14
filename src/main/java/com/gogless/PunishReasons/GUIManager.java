
package com.gogless.PunishReasons;

import com.gogless.GUITweaks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class GUIManager {

    private final GUITweaks plugin;

    public GUIManager(GUITweaks plugin) {
        this.plugin = plugin;
    }

    // --- GUI Openers (They no longer manage history) ---

    public void openMainMenu(Player player, String targetName) {
        String title = plugin.colorize(plugin.getConfig().getString("titles.main-menu", "Punish: %player%").replace("%player%", targetName));
        Inventory inv = Bukkit.createInventory(new PunishmentGUIHolder(PunishmentContext.MenuType.MAIN), 27, title);

        inv.setItem(11, createItemFromConfig("main-menu-items.mute"));
        inv.setItem(13, createItemFromConfig("main-menu-items.warn"));
        inv.setItem(15, createItemFromConfig("main-menu-items.ban"));
        inv.setItem(26, createBackButton());

        player.openInventory(inv);
    }

    public void openTypeMenu(Player player, PunishmentContext context) {
        String title = plugin.colorize(plugin.getConfig().getString("titles.type-select"));
        Inventory inv = Bukkit.createInventory(new PunishmentGUIHolder(PunishmentContext.MenuType.TYPE), 27, title);

        String permPath = context.getPunishmentType() == PunishmentContext.PunishmentType.BAN ? "duration-type-items.permanent-ip" : "duration-type-items.permanent";

        inv.setItem(11, createItemFromConfig(permPath));
        inv.setItem(15, createItemFromConfig("duration-type-items.temporary"));
        inv.setItem(26, createBackButton());

        player.openInventory(inv);
    }

    public void openDurationMenu(Player player, PunishmentContext context) {
        String path = "titles.duration-select-" + context.getPunishmentType().toString().toLowerCase();
        String title = plugin.colorize(plugin.getConfig().getString(path));
        Inventory inv = Bukkit.createInventory(new PunishmentGUIHolder(PunishmentContext.MenuType.DURATION), 54, title);

        String durationPath = "temporary-durations." + context.getPunishmentType().toString().toLowerCase();
        ConfigurationSection durationSection = plugin.getConfig().getConfigurationSection(durationPath);
        if (durationSection != null) {
            for(String key : durationSection.getKeys(false)){
                ConfigurationSection sec = durationSection.getConfigurationSection(key);
                if (sec == null) continue;
                ItemStack item = new ItemStack(Material.valueOf(sec.getString("material", "STONE")));
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.colorize(sec.getString("name")));
                    item.setItemMeta(meta);
                }
                inv.addItem(item);
            }
        }

        inv.setItem(49, createItemFromConfig("custom-input-items.duration"));
        inv.setItem(53, createBackButton());
        player.openInventory(inv);
    }

    public void openReasonMenu(Player player, PunishmentContext context) {
        String title = plugin.colorize(plugin.getConfig().getString("titles.reason-select"));
        Inventory inv = Bukkit.createInventory(new PunishmentGUIHolder(PunishmentContext.MenuType.REASON), 54, title);

        List<String> reasons = plugin.getConfig().getStringList("reasons");
        for (String reason : reasons) {
            ItemStack book = new ItemStack(Material.BOOK);
            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.colorize(reason));
                book.setItemMeta(meta);
            }
            inv.addItem(book);
        }

        inv.setItem(49, createItemFromConfig("custom-input-items.reason"));
        inv.setItem(53, createBackButton());
        player.openInventory(inv);
    }

    public void openAppealMenu(Player player, PunishmentContext context) {
        String title = plugin.colorize(plugin.getConfig().getString("titles.appeal-select"));
        Inventory inv = Bukkit.createInventory(new PunishmentGUIHolder(PunishmentContext.MenuType.APPEAL), 27, title);

        inv.setItem(11, createItemFromConfig("appeal-items.can-appeal"));
        inv.setItem(15, createItemFromConfig("appeal-items.cannot-appeal"));
        inv.setItem(26, createBackButton());

        player.openInventory(inv);
    }

    private void goBack(Player player, PunishmentContext context) {
        PunishmentContext.MenuType previousMenu = context.goBack();
        if (previousMenu == null) {
            player.closeInventory();
            return;
        }

        // Re-open the previous menu. This no longer adds to the history.
        switch(previousMenu) {
            case MAIN:
                openMainMenu(player, context.getTargetName());
                break;
            case TYPE:
                openTypeMenu(player, context);
                break;
            case DURATION:
                openDurationMenu(player, context);
                break;
            case REASON:
                openReasonMenu(player, context);
                break;
            default:
                player.closeInventory();
                break;
        }
    }

    public void handleGUI_Click(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PunishmentGUIHolder)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        PunishmentContext context = plugin.getPunishmentManager().getContext(player);
        if (context == null) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            goBack(player, context);
            return;
        }

        PunishmentGUIHolder guiHolder = (PunishmentGUIHolder) holder;
        PunishmentContext.MenuType menuType = guiHolder.getMenuType();

        switch(menuType) {
            case MAIN:
                handleMainMenuClick(clicked, context, player);
                break;
            case TYPE:
                handleTypeMenuClick(clicked, context, player);
                break;
            case DURATION:
                handleDurationMenuClick(clicked, context, player);
                break;
            case REASON:
                handleReasonMenuClick(clicked, context, player);
                break;
            case APPEAL:
                handleAppealMenuClick(clicked, context, player);
                break;
        }
    }

    // --- Click Handlers (They now manage history) ---

    private void handleMainMenuClick(ItemStack clicked, PunishmentContext context, Player player) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String muteName = plugin.colorize(plugin.getConfig().getString("main-menu-items.mute.name"));
        String warnName = plugin.colorize(plugin.getConfig().getString("main-menu-items.warn.name"));
        String banName = plugin.colorize(plugin.getConfig().getString("main-menu-items.ban.name"));

        if (meta.getDisplayName().equals(muteName)) context.setPunishmentType(PunishmentContext.PunishmentType.MUTE);
        else if (meta.getDisplayName().equals(warnName)) context.setPunishmentType(PunishmentContext.PunishmentType.WARN);
        else if (meta.getDisplayName().equals(banName)) context.setPunishmentType(PunishmentContext.PunishmentType.BAN);

        context.pushHistory(PunishmentContext.MenuType.TYPE);
        openTypeMenu(player, context);
    }

    private void handleTypeMenuClick(ItemStack clicked, PunishmentContext context, Player player) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String tempName = plugin.colorize(plugin.getConfig().getString("duration-type-items.temporary.name"));
        if (meta.getDisplayName().equals(tempName)) {
            context.setPermanent(false);
            context.pushHistory(PunishmentContext.MenuType.DURATION);
            openDurationMenu(player, context);
        } else {
            context.setPermanent(true);
            context.setPunishmentCommandFromType();
            context.pushHistory(PunishmentContext.MenuType.REASON);
            openReasonMenu(player, context);
        }
    }

    private void handleDurationMenuClick(ItemStack clicked, PunishmentContext context, Player player) {
        if (clicked.getType() == Material.OAK_SIGN) {
            plugin.getPunishmentManager().requestChatInput(player, PunishmentContext.InputType.DURATION);
        } else {
            String durationPath = "temporary-durations." + context.getPunishmentType().toString().toLowerCase();
            ConfigurationSection durationSection = plugin.getConfig().getConfigurationSection(durationPath);
            if (durationSection != null) {
                for(String key : durationSection.getKeys(false)){
                    ConfigurationSection sec = durationSection.getConfigurationSection(key);
                    if (sec != null && plugin.colorize(sec.getString("name")).equals(clicked.getItemMeta().getDisplayName())){
                        context.setDuration(sec.getString("duration"));
                        break;
                    }
                }
            }
            context.setPunishmentCommandFromType();
            context.pushHistory(PunishmentContext.MenuType.REASON);
            openReasonMenu(player, context);
        }
    }

    private void handleReasonMenuClick(ItemStack clicked, PunishmentContext context, Player player) {
        if (clicked.getType() == Material.OAK_SIGN) {
            plugin.getPunishmentManager().requestChatInput(player, PunishmentContext.InputType.REASON);
        } else if (clicked.getType() == Material.BOOK) {
            context.setReason(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
            context.pushHistory(PunishmentContext.MenuType.APPEAL);
            openAppealMenu(player, context);
        }
    }

    private void handleAppealMenuClick(ItemStack clicked, PunishmentContext context, Player player) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String canAppealName = plugin.colorize(plugin.getConfig().getString("appeal-items.can-appeal.name"));
        if (meta.getDisplayName().equals(canAppealName)) {
            context.setAppealMessage(plugin.getConfig().getString("appeal-items.can-appeal.message"));
        } else {
            context.setAppealMessage(plugin.getConfig().getString("appeal-items.cannot-appeal.message"));
        }
        plugin.getPunishmentManager().executePunishment(player);
    }

    private ItemStack createItemFromConfig(String path) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) return new ItemStack(Material.STONE);

        String materialName = section.getString("material", "STONE");
        String name = plugin.colorize(section.getString("name", "&cError"));
        List<String> lore = section.getStringList("lore").stream()
                .map(plugin::colorize)
                .collect(Collectors.toList());

        ItemStack item = new ItemStack(Material.valueOf(materialName.toUpperCase()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.colorize("&c&lBack"));
            item.setItemMeta(meta);
        }
        return item;
    }
}

