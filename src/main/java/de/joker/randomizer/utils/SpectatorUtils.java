package de.joker.randomizer.utils;

import org.bukkit.entity.Player;

public class SpectatorUtils {
    public static boolean isSpectatorMode(Player player) {
        return player.getGameMode().equals(org.bukkit.GameMode.SPECTATOR) || player.getGameMode().equals(org.bukkit.GameMode.CREATIVE);
    }
}