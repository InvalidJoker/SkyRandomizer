package de.joker.randomizer;

import de.joker.randomizer.commands.AcceptInviteCommand;
import de.joker.randomizer.commands.BackCommand;
import de.joker.randomizer.commands.DeclineInviteCommand;
import de.joker.randomizer.commands.EcCommand;
import de.joker.randomizer.commands.InviteCommand;
import de.joker.randomizer.commands.LeaveCoopCommand;
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
import java.util.List;

@Slf4j
@Getter
public class SkyRandomizer extends JavaPlugin {
    public static final String SEASON_WORLD_NAME = "season2";
    public static final String SEASON_DATABASE_NAME = "season2.db";

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
        serviceManager.setScoreboardManager(scoreboardManager);
        BroadcastManager broadcastManager = new BroadcastManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(serviceManager, scoreboardManager), this);
        Bukkit.getPluginManager().registerEvents(new ExtraProtectionListener(serviceManager), this);
        Bukkit.getPluginManager().registerEvents(new ServerListener(), this);

        itemSpawner.start();
        broadcastManager.start();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new BackCommand(serviceManager).command(), "Teleportiert dich zum letzten erreichten Punkt deiner Insel.", List.of("front", "return", "zurueck"));
            commands.registrar().register(new SpawnCommand(serviceManager).command(), "Teleportiert dich zum Spawn deiner Insel.");
            commands.registrar().register(new EcCommand(serviceManager).command(), "Oeffnet deine Enderchest.", List.of("enderchest", "echest"));
            commands.registrar().register(new WbCommand(serviceManager).command(), "Oeffnet eine Werkbank.", List.of("workbench", "werkbank", "work-bench"));
            commands.registrar().register(new InviteCommand(serviceManager).command(), "Laedt einen Spieler auf deine Coop-Insel ein.");
            commands.registrar().register(new AcceptInviteCommand(serviceManager).command(), "Nimmt eine Coop-Einladung an.");
            commands.registrar().register(new DeclineInviteCommand(serviceManager).command(), "Lehnt eine Coop-Einladung ab.");
            commands.registrar().register(new LeaveCoopCommand(serviceManager).command(), "Verlaesst deine aktuelle Coop-Insel.");
        });
    }

    @Override
    public void onDisable() {
        serviceManager.shutdown();
    }
}
