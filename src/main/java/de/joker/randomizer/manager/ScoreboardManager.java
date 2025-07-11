package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.data.PlayerData;
import de.joker.randomizer.data.PlayerRank;
import de.joker.randomizer.data.Ranking;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.Map;

@Slf4j
public class ScoreboardManager {

    private final SkyRandomizer plugin;
    private final Ranking ranking;
    private final Map<Player, Sidebar> scoreboards;

    public ScoreboardManager(SkyRandomizer plugin, Ranking ranking) {
        this.plugin = plugin;
        this.ranking = ranking;
        this.scoreboards = new java.util.HashMap<>();
    }

    public void updateForAllPlayers() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            showScoreboard(player);
            log.debug("Updated scoreboard for player: {}", player.getName());
        }
    }

    public void removeScoreboard(Player player) {
        Sidebar sidebar = scoreboards.remove(player);
        if (sidebar != null) {
            sidebar.removePlayer(player);
            log.debug("Removed scoreboard for player: {}", player.getName());
        }
    }

    public void showScoreboard(Player player) {
        Sidebar sidebar = scoreboards.get(player);
        if (sidebar == null) {
            sidebar = plugin.getScoreboardLibrary().createSidebar();
            scoreboards.put(player, sidebar);
        }

        sidebar.title(MessageUtils.parse(MessageUtils.getName()));

        sidebar.clearLines();

        PlayerRank rank = ranking.getRankOfPlayer(player.getUniqueId());
        List<PlayerData> topPlayers = ranking.getTop3();

        if (rank == null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> showScoreboard(player), 40L);
            return;
        }

        sidebar.line(0,
                MessageUtils.parse("<gray>Distanz: <white>")
        );
        sidebar.line(1,
                MessageUtils.parse(
                        "<white>" + rank.getDistance() + " Blöcke"
                )
        );
        sidebar.line(2,
                MessageUtils.parse("")
        );
        sidebar.line(3,
                MessageUtils.parse("<gray>Rangliste: <white>")
        );
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerData playerData = topPlayers.get(i);
            String rankColor = (i == 0) ? "<gold>" : (i == 1) ? "<#A9A9A9>" : (i == 2) ? "<#B08D57>" : "<white>";
            String playerColor = playerData.getUuid().equals(player.getUniqueId()) ? "<green>" : "<white>";
            sidebar.line(4 + i,
                    MessageUtils.parse(
                            rankColor + (i + 1) + ". " + playerColor + playerData.getName() + " <gray>(" + playerData.getDistance() + " Blöcke)"
                    )
            );
        }

        for (int i = topPlayers.size(); i < 3; i++) {
            String rankColor = (i == 0) ? "<gold>" : (i == 1) ? "<#A9A9A9>" : (i == 2) ? "<#B08D57>" : "<white>";
            sidebar.line(4 + i,
                    MessageUtils.parse(
                            rankColor + (i + 1) + ". <white> - <gray>(0 Blöcke)"
                    )
            );
        }

        sidebar.addPlayer(player);




    }
}