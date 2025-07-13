package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
public class BroadcastManager {

    private final SkyRandomizer plugin;
    private final Random random = new Random();

    private static final List<String> MESSAGES = List.of(
            "<gradient:#FFAA00:#FFDD55>Vergiss nicht, deine Insel regelmäßig auszubauen!",
            "<gradient:#55FF55:#AAFFAA><color:#C678DD><bold>Booste</bold></color> den Realm, um coole Vorteile wie <gold>/back</gold> oder eine <italic>kürzere Wartezeit</italic> zu erhalten!</gradient>",
            "<gradient:#55FFFF:#AAFFFF>Spiele fair und habe Spaß!"
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
        String message = MESSAGES.get(random.nextInt(MESSAGES.size()));
        Bukkit.broadcast(MessageUtils.parse(message));
    }
}
