package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import org.bukkit.entity.Player;

public class InviteCommand {

    private final ServiceManager serviceManager;

    public InviteCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("invite")
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executesPlayer((PlayerCommandExecutor) (player, args) ->
                                serviceManager.getCoopManager().invite(player, (Player) args.get("player"))));
    }
}
