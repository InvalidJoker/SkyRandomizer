package de.joker.randomizer.manager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import de.joker.randomizer.cache.PlayerCache;
import de.joker.randomizer.data.PlayerData;
import de.joker.randomizer.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IslandManager {

    private final PlayerCache playerCache;
    private final Map<UUID, Integer> textDisplays;
    private int nextEntityId = 2_000_000;

    public IslandManager(PlayerCache playerCache) {
        this.playerCache = playerCache;
        this.textDisplays = new ConcurrentHashMap<>();
    }

    private World getWorld() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            throw new IllegalStateException("World 'world' not found!");
        }
        return world;
    }

    public Location getOrCreateIsland(org.bukkit.entity.Player player) {
        PlayerData playerData = playerCache.getPlayer(player.getUniqueId());

        if (playerData != null && playerData.getIslandX() != 0) {
            return new Location(getWorld(), playerData.getIslandX(), 64, 0);
        }

        int newIslandX = playerCache.getNextFreeIslandX();

        generateIslandAt(newIslandX);

        playerCache.updatePlayerIsland(player.getUniqueId(), player.getName(), newIslandX);

        return new Location(getWorld(), newIslandX, 64, 0);
    }

    private void generateIslandAt(int x) {
        int centerY = 64;

        Location bedrock = new Location(getWorld(), x, centerY, 0);
        bedrock.getBlock().setType(Material.BEDROCK);

        for (int i = -4; i <= 1; i++) {
            for (int y = centerY - 5; y <= centerY + 5; y++) {
                Location barrierLocation = new Location(getWorld(), x + 4, y, i);
                barrierLocation.getBlock().setType(Material.BARRIER);
            }
        }
    }

    public Location getIslandLocation(org.bukkit.entity.Player player) {
        PlayerData playerData = playerCache.getPlayer(player.getUniqueId());
        if (playerData != null && playerData.getIslandX() != 0) {
            return new Location(getWorld(), playerData.getIslandX(), 64, 0);
        }
        return null;
    }

    public boolean hasIsland(org.bukkit.entity.Player player) {
        PlayerData playerData = playerCache.getPlayer(player.getUniqueId());
        return playerData != null && playerData.getIslandX() != 0;
    }


    public void createBuildDisplay(Player player, int islandX) {
        int entityId = nextEntityId++;

        double y = player.getEyeLocation().getY();
        double z = 6.0;
        Location location = new Location(player.getWorld(), islandX, y, z);

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.TEXT_DISPLAY,
                new Vector3d(location.getX() + 0.5, location.getY(), location.getZ()),
                0.0f,
                180.0f,
                180.0f,
                0,
                Optional.of(new Vector3d(0.0, 0.0, 0.0))
        );

        List<EntityData> metadata = new ArrayList<>();

        metadata.add(new EntityData(15, EntityDataTypes.BYTE, (byte) 0x00));

        Component adventureComponent = MessageUtils.parse("<gradient:#3AC47D:#8cd1bc>Baue in dieser Richtung um Punkte zu sammeln!");

        metadata.add(new EntityData(23, EntityDataTypes.ADV_COMPONENT, adventureComponent));


        WrapperPlayServerEntityMetadata metadataPacket =
                new WrapperPlayServerEntityMetadata(entityId, metadata);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);

        textDisplays.put(player.getUniqueId(), entityId);
    }

    public void removeDisplay(Player player) {
        Integer entityId = textDisplays.remove(player.getUniqueId());
        if (entityId != null) {
            WrapperPlayServerDestroyEntities destroyPacket =
                    new WrapperPlayServerDestroyEntities(entityId);

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
        }
    }
}