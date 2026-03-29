package de.joker.randomizer.manager;

import de.cytooxien.realms.api.RealmPermissionProvider;
import de.joker.randomizer.data.IslandAssignmentResult;
import de.joker.randomizer.data.IslandData;
import de.joker.randomizer.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class CoopManager {

    private static final int MAX_ISLAND_MEMBERS = 4;
    private static final Duration INVITE_TTL = Duration.ofMinutes(10);

    private final ServiceManager serviceManager;
    private final Map<UUID, Map<UUID, Instant>> invites = new ConcurrentHashMap<>();

    public void invite(Player inviter, Player target) {
        cleanupExpiredInvites(target.getUniqueId());

        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            MessageUtils.send(inviter, "<red>Du kannst dich nicht selbst einladen.");
            return;
        }

        IslandData inviterIsland = serviceManager.getIslandManager().getIslandData(inviter);
        if (inviterIsland == null) {
            serviceManager.getIslandManager().getOrCreateIsland(inviter);
            inviterIsland = serviceManager.getIslandManager().getIslandData(inviter);
        }

        IslandData targetIsland = serviceManager.getIslandManager().getIslandData(target);
        if (targetIsland != null && targetIsland.getId() == inviterIsland.getId()) {
            MessageUtils.send(inviter, "<red>Dieser Spieler ist bereits auf deiner Insel.");
            return;
        }

        if (inviterIsland.getMemberCount() >= MAX_ISLAND_MEMBERS) {
            MessageUtils.send(inviter, "<red>Deine Insel ist bereits voll. Maximal 4 Spieler sind erlaubt.");
            return;
        }

        invites.computeIfAbsent(target.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(inviter.getUniqueId(), Instant.now().plus(INVITE_TTL));

        MessageUtils.send(inviter, "<green>Einladung an <white>" + target.getName() + "<green> gesendet.");
        MessageUtils.send(target, "<green>" + inviter.getName() + " <gray>hat dich auf seine Insel eingeladen.");
        MessageUtils.send(target, "<gray>Nutze <white>/acceptinvite " + inviter.getName() + " <gray>oder <white>/declineinvite " + inviter.getName());
    }

    public void acceptInvite(Player player, Player inviter) {
        if (!hasInvite(player.getUniqueId(), inviter.getUniqueId())) {
            MessageUtils.send(player, "<red>Von diesem Spieler liegt keine offene Einladung vor.");
            return;
        }

        IslandData targetIsland = serviceManager.getIslandManager().getIslandData(inviter);
        if (targetIsland == null) {
            removeInvite(player.getUniqueId(), inviter.getUniqueId());
            MessageUtils.send(player, "<red>Die Ziel-Insel konnte nicht gefunden werden.");
            return;
        }

        IslandData currentIsland = serviceManager.getIslandManager().getIslandData(player);
        if (currentIsland != null && currentIsland.getId() == targetIsland.getId()) {
            removeInvite(player.getUniqueId(), inviter.getUniqueId());
            MessageUtils.send(player, "<red>Du bist bereits auf dieser Insel.");
            return;
        }

        if (targetIsland.getMemberCount() >= MAX_ISLAND_MEMBERS) {
            MessageUtils.send(player, "<red>Diese Insel ist bereits voll.");
            return;
        }

        IslandAssignmentResult result = serviceManager.getIslandManager().movePlayerToIsland(player, targetIsland);
        if (result == null) {
            MessageUtils.send(player, "<red>Die Einladung konnte nicht angenommen werden.");
            return;
        }

        invites.remove(player.getUniqueId());
        teleportToIsland(player, result.assignedIsland());
        syncPlayerState(player, result.assignedIsland());
        serviceManager.getScoreboardManager().updateForAllPlayers();

        MessageUtils.send(player, "<green>Du bist der Insel von <white>" + inviter.getName() + "<green> beigetreten.");
        MessageUtils.send(inviter, "<green>" + player.getName() + " <gray>ist deiner Insel beigetreten.");
    }

    public void declineInvite(Player player, Player inviter) {
        if (!removeInvite(player.getUniqueId(), inviter.getUniqueId())) {
            MessageUtils.send(player, "<red>Von diesem Spieler liegt keine offene Einladung vor.");
            return;
        }

        MessageUtils.send(player, "<yellow>Du hast die Einladung von <white>" + inviter.getName() + "<yellow> abgelehnt.");
        MessageUtils.send(inviter, "<yellow>" + player.getName() + " <gray>hat deine Einladung abgelehnt.");
    }

    public void leaveCoop(Player player) {
        IslandData currentIsland = serviceManager.getIslandManager().getIslandData(player);
        if (currentIsland == null || currentIsland.getMemberCount() <= 1) {
            MessageUtils.send(player, "<red>Du bist aktuell nicht in einer Koop-Insel.");
            return;
        }

        IslandAssignmentResult result = serviceManager.getIslandManager().createFreshIslandForPlayer(player);
        teleportToIsland(player, result.assignedIsland());
        syncPlayerState(player, result.assignedIsland());
        serviceManager.getScoreboardManager().updateForAllPlayers();

        MessageUtils.send(player, "<green>Du hast die Koop-Insel verlassen und eine neue eigene Insel erhalten.");
        for (Player onlineMember : serviceManager.getIslandManager().getOnlineMembers(currentIsland)) {
            if (!onlineMember.getUniqueId().equals(player.getUniqueId())) {
                MessageUtils.send(onlineMember, "<yellow>" + player.getName() + " <gray>hat die Insel verlassen.");
            }
        }
    }

    private void syncPlayerState(Player player, IslandData island) {
        serviceManager.getIslandManager().removeDisplay(player);
        if (island.getDistance() < 4) {
            serviceManager.getIslandManager().createBuildDisplay(player, island.getIslandX());
        }

        RealmPermissionProvider permissionProvider = serviceManager.getPermissionProvider();
        if (permissionProvider != null) {
            serviceManager.getIslandManager().modifyPlayerGroup(player, permissionProvider, island.getDistance());
        }
    }

    private void teleportToIsland(Player player, IslandData island) {
        Location location = serviceManager.getIslandManager().getIslandLocation(island);
        player.teleport(location.clone().add(0.5, 1, 0.5).setDirection(location.getDirection().setY(0)));
        player.setFallDistance(0f);
        player.setVelocity(new Vector(0, 0, 0));
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
    }

    private boolean hasInvite(UUID targetUuid, UUID inviterUuid) {
        cleanupExpiredInvites(targetUuid);
        return invites.getOrDefault(targetUuid, Map.of()).containsKey(inviterUuid);
    }

    private boolean removeInvite(UUID targetUuid, UUID inviterUuid) {
        Map<UUID, Instant> invitations = invites.get(targetUuid);
        if (invitations == null) {
            return false;
        }

        boolean removed = invitations.remove(inviterUuid) != null;
        if (invitations.isEmpty()) {
            invites.remove(targetUuid);
        }
        return removed;
    }

    private void cleanupExpiredInvites(UUID targetUuid) {
        Map<UUID, Instant> invitations = invites.get(targetUuid);
        if (invitations == null) {
            return;
        }

        Instant now = Instant.now();
        invitations.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        if (invitations.isEmpty()) {
            invites.remove(targetUuid);
        }
    }
}
