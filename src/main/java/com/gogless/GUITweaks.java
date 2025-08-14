package com.gogless;

import com.gogless.PunishReasons.GUIManager;
import com.gogless.PunishReasons.PunishmentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GUITweaks extends JavaPlugin implements Listener, CommandExecutor {

    private PunishmentManager punishmentManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        this.punishmentManager = new PunishmentManager(this);
        this.guiManager = new GUIManager(this);

        saveDefaultConfig();

        getCommand("punish").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("GUITweaks has been enabled successfully!");
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
    public GUIManager getGuiManager() {
        return guiManager;
    }

    public String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command must be run by a player.");
            return true;
        }
        if (!sender.hasPermission("GUITweaks.punish")) {
            sender.sendMessage(colorize("&cYou do not have permission to use this command."));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(colorize("&cUsage: /punish <player>"));
            return true;
        }

        Player punisher = (Player) sender;
        String targetName = args[0];

        if (punisher.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(colorize("&cYou cannot punish yourself."));
            return true;
        }

        // This now correctly passes the target's name (a String) to the method.
        punishmentManager.startPunishmentProcess(punisher, targetName);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            guiManager.handleGUI_Click(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            punishmentManager.handleGUI_Close(event);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (punishmentManager.isAwaitingInput(playerUUID)) {
            event.setCancelled(true);
            punishmentManager.handleChatInput(player, event.getMessage());
        }
    }
}
