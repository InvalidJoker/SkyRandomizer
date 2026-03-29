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
            MessageUtils.send(inviter, "coop.self_invite");
            return;
        }

        IslandData inviterIsland = serviceManager.getIslandManager().getIslandData(inviter);
        if (inviterIsland == null) {
            serviceManager.getIslandManager().getOrCreateIsland(inviter);
            inviterIsland = serviceManager.getIslandManager().getIslandData(inviter);
        }

        IslandData targetIsland = serviceManager.getIslandManager().getIslandData(target);
        if (targetIsland != null && targetIsland.getId() == inviterIsland.getId()) {
            MessageUtils.send(inviter, "coop.already_same_island");
            return;
        }

        if (inviterIsland.getMemberCount() >= MAX_ISLAND_MEMBERS) {
            MessageUtils.send(inviter, "coop.island_full_self", MessageUtils.placeholder("max", MAX_ISLAND_MEMBERS));
            return;
        }

        invites.computeIfAbsent(target.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(inviter.getUniqueId(), Instant.now().plus(INVITE_TTL));

        MessageUtils.send(inviter, "coop.invite_sent", MessageUtils.placeholder("player", target.getName()));
        MessageUtils.send(target, "coop.invite_received",
                MessageUtils.placeholder("player", inviter.getName()),
                MessageUtils.placeholder("accept_command", "/acceptinvite " + inviter.getName()),
                MessageUtils.placeholder("decline_command", "/declineinvite " + inviter.getName()));
    }

    public void acceptInvite(Player player, Player inviter) {
        if (!hasInvite(player.getUniqueId(), inviter.getUniqueId())) {
            MessageUtils.send(player, "coop.invite_none");
            return;
        }

        IslandData targetIsland = serviceManager.getIslandManager().getIslandData(inviter);
        if (targetIsland == null) {
            removeInvite(player.getUniqueId(), inviter.getUniqueId());
            MessageUtils.send(player, "coop.target_island_missing");
            return;
        }

        IslandData currentIsland = serviceManager.getIslandManager().getIslandData(player);
        if (currentIsland != null && currentIsland.getId() == targetIsland.getId()) {
            removeInvite(player.getUniqueId(), inviter.getUniqueId());
            MessageUtils.send(player, "coop.already_on_island");
            return;
        }

        if (targetIsland.getMemberCount() >= MAX_ISLAND_MEMBERS) {
            MessageUtils.send(player, "coop.island_full_target", MessageUtils.placeholder("max", MAX_ISLAND_MEMBERS));
            return;
        }

        IslandAssignmentResult result = serviceManager.getIslandManager().movePlayerToIsland(player, targetIsland);
        if (result == null) {
            MessageUtils.send(player, "coop.accept_failed");
            return;
        }

        invites.remove(player.getUniqueId());
        teleportToIsland(player, result.assignedIsland());
        syncPlayerState(player, result.assignedIsland());
        serviceManager.getScoreboardManager().updateForAllPlayers();

        MessageUtils.send(player, "coop.accept_success", MessageUtils.placeholder("player", inviter.getName()));
        MessageUtils.send(inviter, "coop.accept_notify", MessageUtils.placeholder("player", player.getName()));
    }

    public void declineInvite(Player player, Player inviter) {
        if (!removeInvite(player.getUniqueId(), inviter.getUniqueId())) {
            MessageUtils.send(player, "coop.invite_none");
            return;
        }

        MessageUtils.send(player, "coop.decline_self", MessageUtils.placeholder("player", inviter.getName()));
        MessageUtils.send(inviter, "coop.decline_notify", MessageUtils.placeholder("player", player.getName()));
    }

    public void leaveCoop(Player player) {
        IslandData currentIsland = serviceManager.getIslandManager().getIslandData(player);
        if (currentIsland == null || currentIsland.getMemberCount() <= 1) {
            MessageUtils.send(player, "coop.leave_not_in_coop");
            return;
        }

        IslandAssignmentResult result = serviceManager.getIslandManager().createFreshIslandForPlayer(player);
        teleportToIsland(player, result.assignedIsland());
        syncPlayerState(player, result.assignedIsland());
        serviceManager.getScoreboardManager().updateForAllPlayers();

        MessageUtils.send(player, "coop.leave_success");
        for (Player onlineMember : serviceManager.getIslandManager().getOnlineMembers(currentIsland)) {
            if (!onlineMember.getUniqueId().equals(player.getUniqueId())) {
                MessageUtils.send(onlineMember, "coop.leave_notify", MessageUtils.placeholder("player", player.getName()));
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
