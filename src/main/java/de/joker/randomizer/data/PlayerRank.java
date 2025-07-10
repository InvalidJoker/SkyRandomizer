package de.joker.randomizer.data;

import lombok.Getter;

@Getter
public class PlayerRank {
    private final int rank;
    private final int distance;

    public PlayerRank(int rank, int distance) {
        this.rank = rank;
        this.distance = distance;
    }

}
