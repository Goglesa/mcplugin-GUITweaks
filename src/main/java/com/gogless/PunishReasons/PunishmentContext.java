package com.gogless.PunishReasons;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class PunishmentContext {

    private final UUID punisherUUID;
    private final String targetName;

    private PunishmentType punishmentType;
    private String punishmentCommand;
    private boolean isPermanent;
    private String duration;
    private String reason;
    private String appealMessage;

    // This stack will keep track of the player's path through the menus
    private final Deque<MenuType> menuHistory = new ArrayDeque<>();

    public enum PunishmentType { BAN, MUTE, WARN }
    public enum InputType { DURATION, REASON }
    public enum MenuType { MAIN, TYPE, DURATION, REASON, APPEAL }

    public PunishmentContext(UUID punisherUUID, String targetName) {
        this.punisherUUID = punisherUUID;
        this.targetName = targetName;
    }

    // --- History Management for "Back" button ---
    public void pushHistory(MenuType type) {
        this.menuHistory.push(type);
    }

    public MenuType goBack() {
        if (menuHistory.size() > 1) {
            menuHistory.pop(); // Remove current menu
            return menuHistory.peek(); // Return the previous menu
        }
        return null; // Can't go back from the first menu
    }

    // --- Setters ---
    public void setPunishmentType(PunishmentType type) { this.punishmentType = type; }
    public void setPermanent(boolean permanent) { isPermanent = permanent; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setReason(String reason) { this.reason = reason; }
    public void setAppealMessage(String appealMessage) { this.appealMessage = appealMessage; }

    public void setPunishmentCommandFromType() {
        if (punishmentType == null) return;
        switch (punishmentType) {
            case BAN:
                this.punishmentCommand = isPermanent ? "ipban" : "tempban";
                break;
            case MUTE:
                this.punishmentCommand = isPermanent ? "mute" : "tempmute";
                break;
            case WARN:
                this.punishmentCommand = "warn";
                this.isPermanent = false;
                break;
        }
    }


    public String getTargetName() { return targetName; }
    public PunishmentType getPunishmentType() { return punishmentType; }


    public String buildCommand() {
        if (reason == null || appealMessage == null) {
            return null;
        }

        if (punishmentType == PunishmentType.WARN) {
            return String.format("warn %s %s %s -s", targetName, reason, appealMessage);
        }

        if (punishmentCommand == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(punishmentCommand).append(" ");
        sb.append(targetName).append(" ");

        if (!isPermanent) {
            if (duration == null) return null;
            sb.append(duration).append(" ");
        }

        sb.append(reason).append(" ");
        sb.append(appealMessage);
        sb.append(" -s");

        return sb.toString();
    }
}