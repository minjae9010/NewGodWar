package kr.newgodwar.ability.feedback;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/** RGB dust on both 1.12 (directional color offsets) and 1.13+ (DustOptions). */
final class ColoredDust {
    private static final Particle DUST = AbilityTheme.resolve(Particle.class, "DUST", "REDSTONE");
    private static final Constructor<?> OPTIONS = optionsConstructor();
    private static final Map<Integer, Object> COLORS = new HashMap<Integer, Object>();

    private ColoredDust() { }

    private static Constructor<?> optionsConstructor() {
        try {
            return Class.forName("org.bukkit.Particle$DustOptions").getConstructor(Color.class, float.class);
        } catch (ReflectiveOperationException | LinkageError ignored) { return null; }
    }

    static void spawn(Player viewer, Location point, int rgb, Particle fallback) {
        try {
            if (DUST != null && OPTIONS != null) {
                Object data = COLORS.get(rgb);
                if (data == null) {
                    data = OPTIONS.newInstance(Color.fromRGB(rgb), 0.85F);
                    COLORS.put(rgb, data);
                }
                viewer.spawnParticle(DUST, point, 1, 0, 0, 0, 0, data);
                return;
            }
            if (DUST != null && DUST.getDataType() == Void.class) {
                viewer.spawnParticle(DUST, point, 0, Math.max(0.001D, ((rgb >> 16) & 255) / 255D),
                    ((rgb >> 8) & 255) / 255D, (rgb & 255) / 255D, 1D);
                return;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
            // The outline still renders if the server does not understand colored dust.
        }
        if (fallback != null) viewer.spawnParticle(fallback, point, 1, 0, 0, 0, 0);
    }
}
