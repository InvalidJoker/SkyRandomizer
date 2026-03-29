package de.joker.randomizer.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class CommandUtils {
    public static CompletableFuture<Suggestions> suggestOnlinePlayers(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.startsWith(builder.getRemaining()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static Player getOnlinePlayer(CommandContext<CommandSourceStack> context, String argumentName) {
        String playerName = context.getArgument(argumentName, String.class);
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            MessageUtils.send(context.getSource().getSender(), "command.player_not_online", MessageUtils.placeholder("player", playerName));
        }
        return player;
    }

    public static Player getPlayerSender(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        if (sender instanceof Player player) {
            return player;
        }

        MessageUtils.send(sender, "command.players_only");
        return null;
    }
}
