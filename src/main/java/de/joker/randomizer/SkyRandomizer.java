package de.joker.randomizer;

import de.joker.randomizer.commands.*;
import de.joker.randomizer.data.Database;
import de.joker.randomizer.listener.ExtraProtectionListener;
import de.joker.randomizer.listener.PlayerListener;
import de.joker.randomizer.listener.ServerListener;
import de.joker.randomizer.manager.BroadcastManager;
import de.joker.randomizer.manager.ItemSpawner;
import de.joker.randomizer.manager.ScoreboardManager;
import de.joker.randomizer.manager.ServiceManager;
import de.joker.randomizer.utils.VoidGenerator;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

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

        registerCommands();
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> {

                    Commands commands = event.registrar();

                    register(
                            commands,
                            new EcCommand(serviceManager)
                    );

                    register(
                            commands,
                            new WbCommand(serviceManager)
                    );

                    register(
                            commands,
                            new SpawnCommand(serviceManager)
                    );

                    register(
                            commands,
                            new BackCommand(serviceManager)
                    );
                }
        );
    }

    private void register(
            Commands commands,
            AbstractCommand command
    ) {
        command.register(commands);
    }

    @Override
    public void onDisable() {
        serviceManager.shutdown();
    }
}
