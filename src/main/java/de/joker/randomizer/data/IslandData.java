package de.joker.randomizer.data;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class IslandData {
    private final long id;
    private final int islandX;

    @Setter
    private int distance;

    private final Map<UUID, IslandMemberData> members = new LinkedHashMap<>();

    public IslandData(long id, int islandX, int distance) {
        this.id = id;
        this.islandX = islandX;
        this.distance = distance;
    }

    public IslandData(IslandData other) {
        this.id = other.id;
        this.islandX = other.islandX;
        this.distance = other.distance;

        for (IslandMemberData member : other.members.values()) {
            addMember(new IslandMemberData(member.getUuid(), member.getName(), member.getIslandId()));
        }
    }

    public void addMember(IslandMemberData member) {
        members.put(member.getUuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public int getMemberCount() {
        return members.size();
    }

    public Collection<IslandMemberData> getMembersView() {
        return Collections.unmodifiableCollection(members.values());
    }

    public String getDisplayName() {
        List<String> names = members.values().stream()
                .map(IslandMemberData::getName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        if (names.isEmpty()) {
            return "Unbekannt";
        }

        if (names.size() == 1) {
            return names.getFirst();
        }

        return names.getFirst() + " +" + (names.size() - 1);
    }
}
