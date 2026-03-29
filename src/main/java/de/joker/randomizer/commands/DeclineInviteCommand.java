package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import org.bukkit.entity.Player;

public class DeclineInviteCommand {

    private final ServiceManager serviceManager;

    public DeclineInviteCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("declineinvite")
                .then(new EntitySelectorArgument.OnePlayer("player")
                        .executesPlayer((PlayerCommandExecutor) (player, args) ->
                                serviceManager.getCoopManager().declineInvite(player, (Player) args.get("player"))));
    }
}
