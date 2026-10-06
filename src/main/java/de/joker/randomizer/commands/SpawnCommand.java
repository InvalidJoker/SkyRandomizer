package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpawnCommand extends AbstractCommand {

    public SpawnCommand(ServiceManager serviceManager) {
        super(serviceManager);
    }

    @Override
    protected String name() {
        return "spawn";
    }

    @Override
    protected boolean requiresIsland() {
        return true;
    }

    @Override
    protected void execute(Player player) {
        Location location =
                serviceManager.getIslandManager().getOrCreateIsland(player);

        player.teleport(
                location.clone()
                        .add(0.5, 1, 0.5)
                        .setDirection(location.getDirection().setY(0))
        );

        player.setFallDistance(0f);
        player.setVelocity(new Vector(0, 0, 0));

        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }

        player.sendRichMessage(
                "<green>Du wurdest zurück zu deiner Insel teleportiert!"
        );
    }
}