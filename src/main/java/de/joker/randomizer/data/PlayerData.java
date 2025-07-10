package de.joker.randomizer.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class PlayerData {
    private final UUID uuid;

    @Setter
    private String name;

    @Setter
    private int distance;

    @Setter
    private int islandX;

    public PlayerData(UUID uuid, String name, int distance, int islandX) {
        this.uuid = uuid;
        this.name = name;
        this.distance = distance;
        this.islandX = islandX;
    }
}
