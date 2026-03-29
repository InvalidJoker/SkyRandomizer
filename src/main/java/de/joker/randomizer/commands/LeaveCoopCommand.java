package de.joker.randomizer.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.manager.ServiceManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class LeaveCoopCommand {

    private final ServiceManager serviceManager;

    public LeaveCoopCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("leavecoop")
                .executes(context -> {
                    Player source = CommandUtils.getPlayerSender(context.getSource());
                    if (source == null) {
                        return 0;
                    }

                    serviceManager.getCoopManager().leaveCoop(source);
                    return 1;
                })
                .build();
    }
}
