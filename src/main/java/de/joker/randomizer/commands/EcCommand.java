package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import dev.jorel.commandapi.CommandTree;

public class EcCommand {
    private final ServiceManager serviceManager;

    public EcCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("ec")
                .withAliases("enderchest", "echest")
                .executesPlayer((player, args) -> {
                    if (!serviceManager.isBooster(player)) {
                        MessageUtils.send(player, "<color:#C678DD><bold>Booste</bold><red> diesen Realm, um Zugriff auf diesen Befehl zu erhalten!");
                        return;
                    }
                    player.openInventory(player.getEnderChest());
                });
    }
}
