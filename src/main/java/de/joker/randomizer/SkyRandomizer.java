package de.joker.randomizer;

import de.joker.randomizer.commands.BackCommand;
import de.joker.randomizer.commands.EcCommand;
import de.joker.randomizer.commands.SpawnCommand;
import de.joker.randomizer.commands.WbCommand;
import de.joker.randomizer.data.Database;
import de.joker.randomizer.listener.ExtraProtectionListener;
import de.joker.randomizer.listener.PlayerListener;
import de.joker.randomizer.listener.ServerListener;
import de.joker.randomizer.manager.BroadcastManager;
import de.joker.randomizer.manager.ItemSpawner;
import de.joker.randomizer.manager.ScoreboardManager;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.VoidGenerator;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;

@Slf4j
@Getter
public class SkyRandomizer extends JavaPlugin {
    private ServiceManager serviceManager;
    private ScoreboardLibrary scoreboardLibrary;

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        getLogger().info("Using VoidGenerator for world: " + worldName);
        return new VoidGenerator();
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        CommandAPI.onEnable();

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            log.error("No packet adapter available for ScoreboardLibrary!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Database database = new Database(this);
        try {
            database.init();
        } catch (SQLException e) {
            log.error("Could not initialize database!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        serviceManager = new ServiceManager(database, this);

        ItemSpawner itemSpawner = new ItemSpawner(this, serviceManager.getIslandManager());
        ScoreboardManager scoreboardManager = new ScoreboardManager(this, serviceManager.getRanking());
        BroadcastManager broadcastManager = new BroadcastManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(serviceManager, scoreboardManager), this);
        Bukkit.getPluginManager().registerEvents(new ExtraProtectionListener(serviceManager), this);
        Bukkit.getPluginManager().registerEvents(new ServerListener(), this);

        itemSpawner.start();
        broadcastManager.start();

        new BackCommand(serviceManager).build().register();
        new SpawnCommand(serviceManager).build().register();
        new EcCommand(serviceManager).build().register();
        new WbCommand(serviceManager).build().register();
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();

        serviceManager.shutdown();
    }
}
