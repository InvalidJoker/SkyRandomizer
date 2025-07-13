package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import dev.jorel.commandapi.CommandTree;
import org.bukkit.entity.Player;

public class EcCommand {
    private final ServiceManager serviceManager;

    public EcCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("ec")
                .withRequirement(sender -> {
                    if (!(sender instanceof Player player)) {
                        return false;
                    }

                    return serviceManager.isBooster(player);
                })
                .withAliases("enderchest", "e-chest", "enderchest", "e-chest")
                .executesPlayer((player, args) -> {

                    player.openInventory(player.getEnderChest());
                });
    }
}
