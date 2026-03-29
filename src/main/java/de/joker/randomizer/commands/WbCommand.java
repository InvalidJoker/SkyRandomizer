package de.joker.randomizer.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class WbCommand {
    private final ServiceManager serviceManager;

    public WbCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("wb")
                .executes(context -> {
                    Player player = CommandUtils.getPlayerSender(context.getSource());
                    if (player == null) {
                        return 0;
                    }

                    if (!serviceManager.isBooster(player)) {
                        MessageUtils.send(player, "<color:#C678DD><bold>Booste</bold><red> diesen Realm, um Zugriff auf diesen Befehl zu erhalten!");
                        return 0;
                    }
                    player.openWorkbench(null, true);
                    return 1;
                })
                .build();
    }
}
