package com.gogless.PunishReasons;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * A custom InventoryHolder to mark our GUIs. This is the key to preventing
 * the plugin from affecting other inventories like chests or the player's inventory.
 */
public class PunishmentGUIHolder implements InventoryHolder {

    private final PunishmentContext.MenuType menuType;

    public PunishmentGUIHolder(PunishmentContext.MenuType menuType) {
        this.menuType = menuType;
    }

    public PunishmentContext.MenuType getMenuType() {
        return this.menuType;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}

