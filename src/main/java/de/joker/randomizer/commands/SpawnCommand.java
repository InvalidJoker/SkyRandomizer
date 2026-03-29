package de.joker.randomizer.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpawnCommand {
    private final ServiceManager serviceManager;

    public SpawnCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("spawn")
                .executes(context -> {
                    Player player = CommandUtils.getPlayerSender(context.getSource());
                    if (player == null) {
                        return 0;
                    }

                    if (!serviceManager.isBooster(player)) {
                        MessageUtils.send(player, "command.booster_required");
                        return 0;
                    }
                    if (!serviceManager.getIslandManager().hasIsland(player)) {
                        MessageUtils.send(player, "command.spawn.no_island");
                        return 0;
                    }

                    Location location = serviceManager.getIslandManager().getOrCreateIsland(player);
                    player.teleport(location.clone().add(0.5, 1, 0.5).setDirection(location.getDirection().setY(0)));
                    player.setFallDistance(0f);
                    player.setVelocity(new Vector(0, 0, 0));
                    if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                    }

                    MessageUtils.send(player, "command.spawn.teleported");
                    return 1;
                })
                .build();
    }
}
