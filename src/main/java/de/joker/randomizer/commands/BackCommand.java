package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import dev.jorel.commandapi.CommandTree;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BackCommand {
    private final ServiceManager serviceManager;

    public BackCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("back")
                .withRequirement(sender -> {
                    if (!(sender instanceof Player player)) {
                        return false;
                    }

                    return serviceManager.isBooster(player.getUniqueId());
                })
                .executesPlayer((player, args) -> {
                    if (!serviceManager.getIslandManager().hasIsland(player)) {
                        MessageUtils.send(player, "<red>Du hast keine Insel, zu der du zurückkehren kannst!");
                        return;
                    }

                    Location location = serviceManager.getIslandManager().getOrCreateIsland(player);

                    World world = location.getWorld();
                    int startX = location.getBlockX();
                    int startZ = location.getBlockZ();

                    int highestZWithBlock = Integer.MIN_VALUE;
                    int highestYAtZ = -1;

                    for (int z = startZ; z >= startZ - 20; z--) {
                        for (int y = 140; y >= 40; y--) {
                            Block block = world.getBlockAt(startX, y, z);
                            if (!block.isEmpty() && !block.getType().isAir() && block.getType().isCollidable()) {
                                if (z > highestZWithBlock) {
                                    highestZWithBlock = z;
                                    highestYAtZ = y;
                                }
                                break;
                            }
                        }
                    }

                    if (highestZWithBlock == Integer.MIN_VALUE) {
                        MessageUtils.send(player, "<red>Es wurde kein solider Block gefunden, zu dem du teleportiert werden kannst!");
                        return;
                    }

                    Location teleportLocation = new Location(
                            world,
                            startX + 0.5,
                            highestYAtZ + 1,
                            highestZWithBlock + 0.5,
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch()
                    );

                    player.teleport(teleportLocation);
                    MessageUtils.send(player, "<green>Du wurdest zurück zu deiner Insel teleportiert!");
                });
    }
}
