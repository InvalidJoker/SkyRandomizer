package de.joker.randomizer.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.manager.ServiceManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class AcceptInviteCommand {

    private final ServiceManager serviceManager;

    public AcceptInviteCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("acceptinvite")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtils::suggestOnlinePlayers)
                        .executes(context -> {
                            Player target = CommandUtils.getOnlinePlayer(context, "player");
                            if (target == null) {
                                return 0;
                            }

                            Player source = CommandUtils.getPlayerSender(context.getSource());
                            if (source == null) {
                                return 0;
                            }

                            serviceManager.getCoopManager().acceptInvite(source, target);
                            return 1;
                        }))
                .build();
    }
}
