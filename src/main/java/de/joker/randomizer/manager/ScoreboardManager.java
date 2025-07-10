package de.joker.randomizer.manager;

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

    private final JavaPlugin plugin;
    private final Ranking ranking;
    private final ScoreboardLibrary scoreboardLibrary;
    private final Map<Player, Sidebar> scoreboards;

    public ScoreboardManager(JavaPlugin plugin, Ranking ranking, ScoreboardLibrary scoreboardLibrary) {
        this.plugin = plugin;
        this.ranking = ranking;
        this.scoreboards = new java.util.HashMap<>();
        this.scoreboardLibrary = scoreboardLibrary;
    }

    public void updateForAllPlayers() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            showScoreboard(player);
            log.debug("Updated scoreboard for player: {}", player.getName());
        }
    }

    public void showScoreboard(Player player) {
        Sidebar sidebar = scoreboards.get(player);
        if (sidebar == null) {
            sidebar = scoreboardLibrary.createSidebar();
            scoreboards.put(player, sidebar);
        }

        sidebar.title(MessageUtils.parse(MessageUtils.getName()));

        sidebar.clearLines();

        PlayerRank rank = ranking.getRankOfPlayer(player.getUniqueId());
        List<PlayerData> topPlayers = ranking.getTop3();

        if (rank == null) {
            // run again 2 seconds later if rank is null
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
        sidebar.line(1,
                MessageUtils.parse("")
        );
        sidebar.line(2,
                MessageUtils.parse("<gray>Rangliste: <white>")
        );
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerData playerData = topPlayers.get(i);
            String rankColor = (i == 0) ? "<gold>" : (i == 1) ? "<silver>" : (i == 2) ? "<bronze>" : "<white>";
            // arrow left
            String arrow = (playerData.getUuid().equals(player.getUniqueId())) ? " <green>←" : "";
            sidebar.line(3 + i,
                    MessageUtils.parse(
                            rankColor + (i + 1) + ". <white>" + playerData.getName() + " <gray>(" + playerData.getDistance() + " Blöcke)" + arrow
                    )
            );
        }

        sidebar.addPlayer(player);




    }
}