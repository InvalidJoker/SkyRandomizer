package de.joker.randomizer.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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
                            String playerName = context.getArgument("player", String.class);
                            Player player = Bukkit.getPlayer(playerName);
                            if (player == null) {
                                MessageUtils.send(context.getSource().getSender(), "<red>Der Spieler " + playerName + " ist nicht online!");
                                return 0;
                            }
                            CommandSender sender = context.getSource().getSender();
                            if (!(sender instanceof Player source)) return 0;

                            serviceManager.getCoopManager().acceptInvite(source, player);
                            return 1;
                        })).build();
    }
}
