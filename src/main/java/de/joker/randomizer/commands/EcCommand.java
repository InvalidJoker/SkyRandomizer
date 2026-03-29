package de.joker.randomizer.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public class EcCommand {
    private final ServiceManager serviceManager;

    public EcCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("ec")
                .executes(context -> {
                    Player player = CommandUtils.getPlayerSender(context.getSource());
                    if (player == null) {
                        return 0;
                    }

                    if (!serviceManager.isBooster(player)) {
                        MessageUtils.send(player, "command.booster_required");
                        return 0;
                    }
                    player.openInventory(player.getEnderChest());
                    return 1;
                })
                .build();
    }
}
