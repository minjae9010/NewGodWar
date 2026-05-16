package kr.newgodwar.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class TempleLocation {

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public TempleLocation(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static TempleLocation fromBlock(Block block) {
        Location location = block.getLocation();
        return new TempleLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static TempleLocation deserialize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new TempleLocation(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public String serialize() {
        return world + "," + x + "," + y + "," + z;
    }

    public boolean matches(Block block) {
        Location location = block.getLocation();
        return world.equals(location.getWorld().getName())
            && x == location.getBlockX()
            && y == location.getBlockY()
            && z == location.getBlockZ();
    }

    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x + 0.5D, y + 0.5D, z + 0.5D);
    }
}
