package kr.newgodwar.game;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public final class VoidWorldGenerator extends ChunkGenerator {
    public boolean shouldGenerateNoise() {
        return false;
    }

    public boolean shouldGenerateSurface() {
        return false;
    }

    public boolean shouldGenerateBedrock() {
        return false;
    }

    public boolean shouldGenerateCaves() {
        return false;
    }

    public boolean shouldGenerateDecorations() {
        return false;
    }

    public boolean shouldGenerateMobs() {
        return false;
    }

    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public ChunkData generateChunkData(World world, java.util.Random random, int x, int z, BiomeGrid biome) {
        return createChunkData(world);
    }

    @Override
    public byte[] generate(World world, java.util.Random random, int x, int z) {
        return new byte[32768];
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }
}
