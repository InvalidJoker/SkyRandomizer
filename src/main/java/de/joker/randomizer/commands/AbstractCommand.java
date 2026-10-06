package de.joker.randomizer.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AbstractCommand {

    protected final ServiceManager serviceManager;

    protected AbstractCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    protected abstract String name();

    protected List<String> aliases() { return List.of(); }

    protected boolean requiresBooster() { return true; }

    protected boolean requiresIsland() { return false; }

    protected abstract void execute(Player player);

    public final LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(name())
                .executes(context -> executeCommand(context.getSource()))
                .build();
    }

    private int executeCommand(CommandSourceStack source) {
        Player player = source.getExecutor() instanceof Player
                ? (Player) source.getExecutor()
                : null;

        if (player == null) {
            source.getSender().sendRichMessage(
                    "<red>Dieser Befehl kann nur von einem Spieler ausgeführt werden!"
            );
            return 0;
        }

        if (requiresBooster() && !serviceManager.isBooster(player)) {
            MessageUtils.send(
                    player,
                    "<color:#C678DD><bold>Booste</bold><red> diesen Realm, um Zugriff auf diesen Befehl zu erhalten!"
            );
            return 0;
        }

        if (requiresIsland()
                && !serviceManager.getIslandManager().hasIsland(player)) {

            MessageUtils.send(
                    player,
                    "<red>Du hast keine Insel, zu der du zurückkehren kannst!"
            );
            return 0;
        }

        execute(player);

        return Command.SINGLE_SUCCESS;
    }

    public final void register(Commands commands) {
        LiteralCommandNode<CommandSourceStack> command = build();

        if (aliases().isEmpty()) {
            commands.register(command);
        } else {
            commands.register(
                    command,
                    null,
                    aliases()
            );
        }
    }
}