package de.joker.randomizer.manager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.joker.randomizer.cache.PlayerCache;
import de.joker.randomizer.data.PlayerData;
import de.joker.randomizer.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class IslandManager {

    private final PlayerCache playerCache;
    private final Map<UUID, Integer> textDisplays;

    public IslandManager(PlayerCache playerCache) {
        this.playerCache = playerCache;
        this.textDisplays = new java.util.HashMap<>();
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

        for (int y = centerY - 5; y <= centerY + 5; y++) {
            Location barrierLocation = new Location(getWorld(), x + 4, y, 0);
            barrierLocation.getBlock().setType(Material.BARRIER);
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


    public void createBuildDisplay(Player p, int islandX) {
        Random random = new Random();

        double playerLoc = p.getEyeHeight();

        Location spawnLoc = new Location(
                getWorld(),
                islandX,
                playerLoc,
                4
        );

        int entityID = random.nextInt();

        PacketContainer textDisplay = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        textDisplay.getIntegers().write(0, entityID);
        textDisplay.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
        textDisplay.getUUIDs().write(0, UUID.randomUUID());
        textDisplay.getDoubles().write(0, spawnLoc.getX());
        textDisplay.getDoubles().write(1, spawnLoc.getY());
        textDisplay.getDoubles().write(2, spawnLoc.getZ());
        ProtocolLibrary.getProtocolManager().sendServerPacket(p, textDisplay);

        PacketContainer textDisplayData = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        textDisplayData.getIntegers().write(0, entityID);

        Component msg = MessageUtils.parse("<gradient:#3AC47D:#8cd1bc>Baue in dieser Richtung um Punkte zu sammeln!");

        GsonComponentSerializer.gson().serialize(msg);

        List<WrappedDataValue> dataValues = List.of(
                new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x03),
                new WrappedDataValue(23, WrappedDataWatcher.Registry.getChatComponentSerializer(),
                        WrappedChatComponent.fromJson(GsonComponentSerializer.gson().serialize(msg)))
        );
        textDisplayData.getDataValueCollectionModifier().write(0, dataValues);
        ProtocolLibrary.getProtocolManager().sendServerPacket(p, textDisplayData);

        textDisplays.put(p.getUniqueId(), entityID);
    }

    public void removeDisplay(Player player) {
        Integer entityId = textDisplays.remove(player.getUniqueId());
        if (entityId != null) {
            PacketContainer destroyDisplay = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyDisplay.getIntLists().write(0, List.of(entityId));
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroyDisplay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}