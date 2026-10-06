package de.joker.randomizer.commands;

import com.mojang.brigadier.Command;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BackCommand extends AbstractCommand {

    private final Map<UUID, Instant> cooldownMap = new HashMap<>();

    public BackCommand(ServiceManager serviceManager) {
        super(serviceManager);
    }

    @Override
    protected String name() {
        return "back";
    }

    @Override
    protected List<String> aliases() {
        return List.of(
                "front",
                "return",
                "zurück"
        );
    }

    @Override
    protected boolean requiresIsland() {
        return true;
    }

    @Override
    protected void execute(Player player) {
        Instant now = Instant.now();
        Instant lastTeleport = cooldownMap.get(player.getUniqueId());

        if (lastTeleport != null
                && now.isBefore(lastTeleport.plusSeconds(30))
                && !player.hasPermission("realms.bypass")) {

            MessageUtils.send(
                    player,
                    "<red>Du kannst erst in 30 Sekunden wieder zurück teleportieren!"
            );
            return;
        }

        Location location =
                serviceManager.getIslandManager().getOrCreateIsland(player);

        World world = location.getWorld();

        if (world == null) {
            MessageUtils.send(
                    player,
                    "<red>Die Welt deiner Insel konnte nicht gefunden werden!"
            );
            return;
        }

        int startX = location.getBlockX();
        int startZ = location.getBlockZ();

        PlayerRank rank =
                serviceManager.getRanking()
                        .getRankOfPlayer(player.getUniqueId());

        if (rank == null) {
            MessageUtils.send(
                    player,
                    "<red>Dein Rang konnte nicht ermittelt werden!"
            );
            return;
        }

        int maxDistance = rank.getDistance();
        int targetZ = startZ + maxDistance;

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
            MessageUtils.send(
                    player,
                    "<red>Es wurde kein solider Block gefunden, zu dem du teleportiert werden kannst!"
            );
            return;
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
        player.setVelocity(new Vector(0, 0, 0));

        cooldownMap.put(player.getUniqueId(), now);

        MessageUtils.send(
                player,
                "<green>Du wurdest zu deinem letzten Standort auf deiner Insel teleportiert!"
        );
    }
}