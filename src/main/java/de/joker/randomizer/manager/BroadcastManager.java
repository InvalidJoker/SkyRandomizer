package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
public class BroadcastManager {

    private final SkyRandomizer plugin;
    private final Random random = new Random();

    private static final List<String> MESSAGE_KEYS = List.of(
            "broadcast.reminder_expand_island",
            "broadcast.booster_benefits",
            "broadcast.play_fair"
    );

    public void start() {
        scheduleNext();
    }

    private void scheduleNext() {
        int delaySeconds = random.nextInt(300) + 600;

        new BukkitRunnable() {
            @Override
            public void run() {
                broadcastRandomMessage();
                scheduleNext();
            }
        }.runTaskLater(plugin, delaySeconds * 20L);
    }

    private void broadcastRandomMessage() {
        String message = MESSAGE_KEYS.get(random.nextInt(MESSAGE_KEYS.size()));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendRaw(onlinePlayer, message);
        }
    }
}
