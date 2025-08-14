package com.gogless.PunishReasons;

import com.gogless.GUITweaks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PunishmentManager {

    private final GUITweaks plugin;
    private final Map<UUID, PunishmentContext> activePunishments = new HashMap<>();
    private final Map<UUID, PunishmentContext.InputType> awaitingInput = new HashMap<>();

    public PunishmentManager(GUITweaks plugin) {
        this.plugin = plugin;
    }

    public void startPunishmentProcess(Player punisher, String targetName) {
        PunishmentContext context = new PunishmentContext(punisher.getUniqueId(), targetName);
        // This is the only place where the first menu is added to the history.
        context.pushHistory(PunishmentContext.MenuType.MAIN);
        activePunishments.put(punisher.getUniqueId(), context);
        plugin.getGuiManager().openMainMenu(punisher, targetName);
    }

    public PunishmentContext getContext(Player player) {
        return activePunishments.get(player.getUniqueId());
    }

    public void requestChatInput(Player player, PunishmentContext.InputType type) {
        awaitingInput.put(player.getUniqueId(), type);
        player.closeInventory();
        String prompt = plugin.getConfig().getString("chat-prompts." + type.toString().toLowerCase(), "&cInvalid prompt type.");
        player.sendMessage(plugin.colorize(prompt));
    }

    public boolean isAwaitingInput(UUID uuid) {
        return awaitingInput.containsKey(uuid);
    }

    public void handleChatInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        PunishmentContext.InputType inputType = awaitingInput.remove(uuid);
        PunishmentContext context = getContext(player);

        if (context == null || inputType == null) return;

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.colorize("&cInput cancelled. Punishment process aborted."));
            activePunishments.remove(uuid);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inputType == PunishmentContext.InputType.DURATION) {
                context.setDuration(message);
                context.setPunishmentCommandFromType();
                // We now push the next menu to the history here, before opening it.
                context.pushHistory(PunishmentContext.MenuType.REASON);
                plugin.getGuiManager().openReasonMenu(player, context);
            } else if (inputType == PunishmentContext.InputType.REASON) {
                context.setReason(message);
                context.pushHistory(PunishmentContext.MenuType.APPEAL);
                plugin.getGuiManager().openAppealMenu(player, context);
            }
        });
    }

    public void executePunishment(Player punisher) {
        UUID uuid = punisher.getUniqueId();
        PunishmentContext context = activePunishments.remove(uuid);
        if (context == null) return;

        String command = context.buildCommand();
        if (command == null) {
            punisher.sendMessage(plugin.colorize("&cSomething went wrong building the command. Process aborted."));
            return;
        }

        punisher.closeInventory();
        punisher.sendMessage(plugin.colorize("&aExecuting command: &f" + command));
        Bukkit.dispatchCommand(punisher, command);
    }

    public void handleGUI_Close(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isAwaitingInput(uuid)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activePunishments.containsKey(uuid) && player.getOpenInventory().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CRAFTING) {
                activePunishments.remove(uuid);
                player.sendMessage(plugin.colorize("&c[GUITweaks] Punishment process cancelled."));
            }
        }, 1L);
    }
}
