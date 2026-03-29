package de.joker.randomizer.manager;

import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.data.IslandData;
import de.joker.randomizer.data.Ranking;
import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
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
        this.scoreboards = new HashMap<>();
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
        Sidebar sidebar = scoreboards.computeIfAbsent(player, ignored -> plugin.getScoreboardLibrary().createSidebar());

        sidebar.title(MessageUtils.parse(MessageUtils.getName()));
        sidebar.clearLines();

        IslandData playerIsland = ranking.getIslandOfPlayer(player.getUniqueId());
        List<IslandData> topIslands = ranking.getTop3();

        if (playerIsland == null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> showScoreboard(player), 40L);
            return;
        }

        sidebar.line(0, MessageUtils.component(player, "scoreboard.distance_label"));
        sidebar.line(1, MessageUtils.component(player, "scoreboard.distance_value", MessageUtils.placeholder("distance", playerIsland.getDistance())));
        sidebar.line(2, MessageUtils.parse(""));
        sidebar.line(3, MessageUtils.component(player, "scoreboard.top_label"));

        for (int i = 0; i < topIslands.size(); i++) {
            IslandData islandData = topIslands.get(i);
            String rankColor = (i == 0) ? "<gold>" : (i == 1) ? "<#A9A9A9>" : (i == 2) ? "<#B08D57>" : "<white>";
            String nameColor = islandData.getId() == playerIsland.getId() ? "<green>" : "<white>";
            sidebar.line(4 + i, MessageUtils.component(player, "scoreboard.entry",
                    MessageUtils.placeholder("rank_color", rankColor),
                    MessageUtils.placeholder("rank", i + 1),
                    MessageUtils.placeholder("name_color", nameColor),
                    MessageUtils.placeholder("name", islandData.getDisplayName()),
                    MessageUtils.placeholder("distance", islandData.getDistance())));
        }

        for (int i = topIslands.size(); i < 3; i++) {
            String rankColor = (i == 0) ? "<gold>" : (i == 1) ? "<#A9A9A9>" : (i == 2) ? "<#B08D57>" : "<white>";
            sidebar.line(4 + i, MessageUtils.component(player, "scoreboard.empty_entry",
                    MessageUtils.placeholder("rank_color", rankColor),
                    MessageUtils.placeholder("rank", i + 1)));
        }

        if (topIslands.stream().noneMatch(island -> island.getId() == playerIsland.getId())) {
            sidebar.line(4 + topIslands.size(), MessageUtils.component(player, "scoreboard.self_entry",
                    MessageUtils.placeholder("rank", rank.getRank()),
                    MessageUtils.placeholder("name", playerIsland.getDisplayName()),
                    MessageUtils.placeholder("distance", rank.getDistance())));
        }

        sidebar.addPlayer(player);
    }
}
