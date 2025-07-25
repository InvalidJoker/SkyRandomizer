package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import dev.jorel.commandapi.CommandTree;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.util.Vector;

public class SpawnCommand {
    private final ServiceManager serviceManager;

    public SpawnCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("spawn")
                .executesPlayer((player, args) -> {
                    if (!serviceManager.isBooster(player)) {
                        MessageUtils.send(player, "<color:#C678DD><bold>Booste</bold><red> diesen Realm, um Zugriff auf diesen Befehl zu erhalten!");
                        return;
                    }
                    if (!serviceManager.getIslandManager().hasIsland(player)) {
                        MessageUtils.send(player, "<red>Du hast keine Insel, zu der du zurückkehren kannst!");
                        return;
                    }

                    Location location = serviceManager.getIslandManager().getOrCreateIsland(player);
                    player.teleport(location.clone().add(0.5, 1, 0.5).setDirection(location.getDirection().setY(0)));
                    player.setFallDistance(0f);
                    player.setVelocity(new Vector(0, 0, 0));
                    player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());

                    MessageUtils.send(player, "<green>Du wurdest zurück zu deiner Insel teleportiert!");
                });
    }
}
