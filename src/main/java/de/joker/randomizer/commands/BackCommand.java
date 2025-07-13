package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import dev.jorel.commandapi.CommandTree;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackCommand {
    private final ServiceManager serviceManager;
    private final Map<UUID, Instant> cooldownMap = new HashMap<>();

    public BackCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("back")
                .withRequirement(sender -> {
                    if (!(sender instanceof Player player)) {
                        return false;
                    }

                    return serviceManager.isBooster(player);
                })
                .withAliases("front", "return", "zurück")
                .executesPlayer((player, args) -> {
                    if (!serviceManager.getIslandManager().hasIsland(player)) {
                        MessageUtils.send(player, "<red>Du hast keine Insel, zu der du zurückkehren kannst!");
                        return;
                    }

                    Instant lastTeleport = cooldownMap.get(player.getUniqueId());
                    Instant now = Instant.now();
                    if (lastTeleport != null && now.isBefore(lastTeleport.plusSeconds(30)) && !player.hasPermission("realms.bypass")) {
                        MessageUtils.send(player, "<red>Du kannst erst in 30 Sekunden wieder zurück teleportieren!");
                        return;
                    }

                    Location location = serviceManager.getIslandManager().getOrCreateIsland(player);

                    World world = location.getWorld();
                    int startX = location.getBlockX();
                    int startZ = location.getBlockZ();
                    int lastGoodZ = Integer.MIN_VALUE;
                    int lastGoodY = -1;
                    int lastGoodX = Integer.MIN_VALUE;

                    int z = startZ;
                    int attempts = 0;
                    int maxAttempts = 100000;

                    while (attempts < maxAttempts) {
                        attempts++;
                        boolean foundSolidBlockInRow = false;
                        int highestYInRow = -1;
                        int bestXInRow = startX;

                        for (int x = startX - 3; x <= startX + 3; x++) {
                            for (int y = 140; y >= 40; y--) {
                                Block block = world.getBlockAt(x, y, z);

                                if (!block.isEmpty()
                                        && !block.getType().isAir()
                                        && block.getType().isCollidable()
                                        && !block.getType().name().contains("LAVA")
                                        && !block.getType().name().contains("WATER")) {
                                    foundSolidBlockInRow = true;
                                    if (y > highestYInRow) {
                                        highestYInRow = y;
                                        bestXInRow = x;
                                    }
                                }
                            }
                        }

                        if (foundSolidBlockInRow) {
                            lastGoodZ = z;
                            lastGoodY = highestYInRow;
                            lastGoodX = bestXInRow;
                            z++;
                        } else {
                            break;
                        }
                    }

                    if (lastGoodZ == Integer.MIN_VALUE) {
                        MessageUtils.send(player, "<red>Es wurde kein solider Block gefunden, zu dem du teleportiert werden kannst!");
                        return;
                    }

                    Location teleportLocation = new Location(
                            world,
                            lastGoodX + 0.5,
                            lastGoodY + 1,
                            lastGoodZ + 0.5,
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch()
                    );

                    player.teleport(teleportLocation);
                    player.setFallDistance(0f);
                    cooldownMap.put(player.getUniqueId(), now);
                    player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    MessageUtils.send(player, "<green>Du wurdest zurück zu deiner Insel teleportiert!");
                });
    }
}
