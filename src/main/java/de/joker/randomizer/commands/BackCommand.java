package de.joker.randomizer.commands;

import de.cytooxien.realms.api.RealmInformationProvider;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import dev.jorel.commandapi.CommandTree;
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
                        MessageUtils.send(sender, "<red>Dieser Befehl kann nur von Spielern ausgeführt werden!");
                        return false;
                    }
                    RealmInformationProvider informationProvider = serviceManager.getInformationProvider();
                    boolean booster = false;
                    if (informationProvider != null) {
                        var boostsHolder = informationProvider.boostsByPlayer(player.getUniqueId());
                        Integer boosts = null;

                        if (boostsHolder != null) {
                            boosts = boostsHolder.value();
                        }

                        int boostsValue = (boosts != null) ? boosts : 0;

                        booster = boostsValue > 0;
                    }

                    return booster;
                })
                .executesPlayer((player, args) -> {
                    if (!serviceManager.getIslandManager().hasIsland(player)) {
                        MessageUtils.send(player, "<red>Du hast keine Insel, zu der du zurückkehren kannst!");
                        return;
                    }

                    var location = serviceManager.getIslandManager().getOrCreateIsland(player);

                    var world = location.getWorld();
                    int startX = location.getBlockX();
                    int startZ = location.getBlockZ();

                    int highestZWithBlock = Integer.MIN_VALUE;
                    int highestYAtZ = -1;

                    for (int z = startZ; z >= startZ - 20; z--) {
                        for (int y = 140; y >= 40; y--) {
                            var block = world.getBlockAt(startX, y, z);
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

                    var teleportLocation = new org.bukkit.Location(
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
