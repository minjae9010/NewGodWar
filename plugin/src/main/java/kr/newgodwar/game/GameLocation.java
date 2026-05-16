package kr.newgodwar.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class GameLocation {

    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public GameLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static GameLocation from(Location location) {
        return new GameLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static GameLocation deserialize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length < 4) {
            return null;
        }
        try {
            float yaw = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0.0F;
            float pitch = parts.length >= 6 ? Float.parseFloat(parts[5]) : 0.0F;
            return new GameLocation(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), yaw, pitch);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public String serialize() {
        return world + "," + x + "," + y + "," + z + "," + yaw + "," + pitch;
    }

    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}
