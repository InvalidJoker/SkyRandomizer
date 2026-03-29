package de.joker.randomizer.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class IslandMemberData {
    private final UUID uuid;

    @Setter
    private String name;

    @Setter
    private long islandId;

    public IslandMemberData(UUID uuid, String name, long islandId) {
        this.uuid = uuid;
        this.name = name;
        this.islandId = islandId;
    }
}
