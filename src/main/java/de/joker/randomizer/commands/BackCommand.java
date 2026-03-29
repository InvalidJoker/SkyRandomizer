package de.joker.randomizer.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.joker.randomizer.data.PlayerRank;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.MessageUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackCommand {
    private final ServiceManager serviceManager;
    private final Map<UUID, Instant> cooldownMap = new HashMap<>();

    public BackCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("back")
                .executes(context -> {
                    Player player = CommandUtils.getPlayerSender(context.getSource());
                    if (player == null) {
                        return 0;
                    }

                    if (!serviceManager.isBooster(player)) {
                        MessageUtils.send(player, "command.booster_required");
                        return 0;
                    }
                    if (!serviceManager.getIslandManager().hasIsland(player)) {
                        MessageUtils.send(player, "command.back.no_island");
                        return 0;
                    }

                    Instant lastTeleport = cooldownMap.get(player.getUniqueId());
                    Instant now = Instant.now();
                    if (lastTeleport != null && now.isBefore(lastTeleport.plusSeconds(30)) && !player.hasPermission("realms.bypass")) {
                        MessageUtils.send(player, "command.back.cooldown", MessageUtils.placeholder("seconds", 30));
                        return 0;
                    }

                    Location location = serviceManager.getIslandManager().getOrCreateIsland(player);
                    World world = location.getWorld();
                    int startX = location.getBlockX();
                    int startZ = location.getBlockZ();
                    PlayerRank rank = serviceManager.getRanking().getRankOfPlayer(player.getUniqueId());
                    if (rank == null) {
                        MessageUtils.send(player, "command.back.rank_missing");
                        return 0;
                    }

                    int targetZ = startZ + rank.getDistance();
                    int lastGoodX = Integer.MIN_VALUE;
                    int lastGoodY = -1;

                    for (int x = startX - 3; x <= startX + 3; x++) {
                        for (int y = 140; y >= 40; y--) {
                            Block block = world.getBlockAt(x, y, targetZ);

                            if (!block.isEmpty()
                                    && !block.getType().isAir()
                                    && block.getType().isCollidable()
                                    && !block.getType().name().contains("LAVA")
                                    && !block.getType().name().contains("WATER")) {
                                if (y > lastGoodY) {
                                    lastGoodY = y;
                                    lastGoodX = x;
                                }
                                break;
                            }
                        }
                    }

                    if (lastGoodY == -1) {
                        MessageUtils.send(player, "command.back.no_solid_block");
                        return 0;
                    }

                    Location teleportLocation = new Location(
                            world,
                            lastGoodX + 0.5,
                            lastGoodY + 1,
                            targetZ + 0.5,
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch()
                    );

                    player.teleport(teleportLocation);
                    player.setFallDistance(0f);
                    cooldownMap.put(player.getUniqueId(), now);
                    player.setVelocity(new Vector(0, 0, 0));
                    MessageUtils.send(player, "command.back.teleported");
                    return 1;
                })
                .build();
    }
}
