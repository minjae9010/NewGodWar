package kr.newgodwar.ability.feedback;

import java.util.List;
import java.util.Objects;

/** Resource-pack-free model. Keep part count, material and item/block kind stable across frames. */
@FunctionalInterface
public interface ObjectModel {
    List<Part> parts(double phase, double detail);

    /** Normalize animation input consistently for displays and particle outlines. */
    static ObjectModel animated(ObjectModel model) {
        Objects.requireNonNull(model, "model");
        return (phase, detail) -> model.parts(Double.isFinite(phase) ? phase : 0,
            Double.isFinite(detail) ? Math.max(0, Math.min(6, detail)) : 1);
    }

    public static final class Part {
        public final String material;
        public final boolean item;
        public final double x, y, z, sx, sy, sz, roll, turn;
        public Part(String material, boolean item, double x, double y, double z, double sx, double sy, double sz, double roll, double turn) {
            this.material = material; this.item = item; this.x = x; this.y = y; this.z = z;
            this.sx = sx; this.sy = sy; this.sz = sz; this.roll = roll; this.turn = turn;
        }
        public int color() {
            if (material.contains("GOLD") || material.contains("GLOWSTONE")) return 0xF2CA73;
            if (material.contains("OAK")) return 0x79523B;
            if (material.contains("ICE") || material.contains("LANTERN")) return 0xA4E7F1;
            if (material.contains("OBSIDIAN") || material.contains("AMETHYST")) return 0xA67BDA;
            if (material.contains("GLASS")) return 0x78BFD5;
            return 0xE1E8EF;
        }
    }

}
