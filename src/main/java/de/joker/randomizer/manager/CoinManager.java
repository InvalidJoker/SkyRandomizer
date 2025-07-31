package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.data.PlayerData;
import de.joker.randomizer.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class CoinManager {

    private final SkyRandomizer plugin;
    private final ScoreboardManager scoreboardManager;
    private final List<UUID> giveaway = new ArrayList<>();
    private int taskId = 0;

    public void addToGiveaway(Player player) {
        if (giveaway.contains(player.getUniqueId())) return;
        giveaway.add(player.getUniqueId());
    }

    public void startTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);

            if (hour == 22 && minute == 0 && second == 0) {
                startRaffle();
            }
        }, 20, 20);
    }

    public void startRaffle() {
        Bukkit.broadcast(MessageUtils.parseWithPrefix("<gray>In <white>30 Sekunden <gray>werden <white>1000 Coins <gray>an alle Spieler die heute auf dem Realm waren verlost."));
        AtomicInteger countdown = new AtomicInteger(30);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            countdown.addAndGet(-1);
            switch (countdown.get()) {
                case 25, 20, 15, 10, 5, 3, 2 -> {
                    Bukkit.broadcast(MessageUtils.parseWithPrefix("<gray>Die Ziehung erfolgt in <white>" + countdown.get() + " Sekunden<gray>."));
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f));
                }
                case 1 -> {
                    Bukkit.broadcast(MessageUtils.parseWithPrefix("<gray>Die Ziehung erfolgt in <white>einer Sekunde<gray>."));
                    Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f));
                }
            }

            if (countdown.get() == 0 && taskId != 0) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = 0;

                if (giveaway.isEmpty()) {
                    Bukkit.broadcast(MessageUtils.parseWithPrefix("<red>Niemand hat gewonnen."));
                    return;
                }

                Collections.shuffle(giveaway);
                UUID random = giveaway.getFirst();
                giveaway.clear();
                PlayerData playerData = plugin.getServiceManager().getPlayerCache().getPlayer(random);

                if (playerData == null) {
                    Bukkit.broadcast(MessageUtils.parseWithPrefix("<red>Niemand hat gewonnen."));
                    return;
                }

                plugin.getServiceManager().getPlayerCache().updatePlayerCoins(playerData.getUuid(), playerData.getName(), playerData.getCoins() + 1000);
                Bukkit.broadcast(MessageUtils.parseWithPrefix("<white>" + playerData.getName() + " <gray>hat <white>1000 Coins <gray>gewonnen! <green>Glückwunsch!"));
                Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f));
                scoreboardManager.updateForAllPlayers();
            }
        }, 20, 20);
    }

}
