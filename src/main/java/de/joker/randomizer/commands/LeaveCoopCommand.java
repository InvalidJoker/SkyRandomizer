package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;

public class LeaveCoopCommand {

    private final ServiceManager serviceManager;

    public LeaveCoopCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public CommandTree build() {
        return new CommandTree("leavecoop")
                .executesPlayer((PlayerCommandExecutor) (player, args) -> serviceManager.getCoopManager().leaveCoop(player));
    }
}
