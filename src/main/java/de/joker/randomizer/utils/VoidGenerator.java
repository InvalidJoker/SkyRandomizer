package de.joker.randomizer.utils;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class VoidGenerator extends ChunkGenerator {
    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) { return List.of(); }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
                              @NotNull ChunkData chunkData) {
        // No need to generate noise, we want an empty world
    }
    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
                                @NotNull ChunkData chunkData) {
        // No need to generate surface, we want an empty world
    }
    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
                                @NotNull ChunkData chunkData) {
        // No need to generate bedrock, we want an empty world
    }
    @Override
    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
                              @NotNull ChunkData chunkData) {
        // No need to generate caves, we want an empty world
    }

    @Override
    public boolean canSpawn(World world, int x, int z) { return true; }
}
