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
        public final double x, y, z, sx, sy, sz, roll, turn, pitch;
        public final String art;
        /** Pack art that turns around the vertical axis to face each viewer. Fixed for a part's lifetime. */
        public final boolean billboard;
        public Part(String material, boolean item, double x, double y, double z, double sx, double sy, double sz, double roll, double turn) {
            this(material, item, x, y, z, sx, sy, sz, roll, turn, 0, null);
        }
        public Part(String material, boolean item, double x, double y, double z, double sx, double sy, double sz,
                    double roll, double turn, double pitch, String art) {
            this(material, item, x, y, z, sx, sy, sz, roll, turn, pitch, art, false);
        }
        public Part(String material, boolean item, double x, double y, double z, double sx, double sy, double sz,
                    double roll, double turn, double pitch, String art, boolean billboard) {
            this.billboard = billboard;
            this.material = material; this.item = item; this.x = x; this.y = y; this.z = z;
            this.sx = sx; this.sy = sy; this.sz = sz; this.roll = roll; this.turn = turn;
            this.pitch = pitch; this.art = art;
        }
        public int color() {
            if (material.contains("LIME")) return 0xA8D65D;
            if (material.contains("GREEN") || material.equals("PRISMARINE")) return 0x398F78;
            if (material.contains("PINK")) return 0xF2A8C6;
            if (material.contains("ORANGE") || material.equals("BLAZE_ROD")) return 0xFF923D;
            if (material.contains("RED")) return 0xCD434E;
            if (material.contains("YELLOW")) return 0xF2D468;
            if (material.contains("LIGHT_BLUE") || material.contains("DIAMOND")) return 0x82DDF0;
            if (material.contains("BLUE")) return 0x507DE0;
            if (material.contains("CYAN")) return 0x35B9C2;
            if (material.contains("PURPLE")) return 0xA279CB;
            if (material.contains("OBSIDIAN")) return 0x514665;
            if (material.contains("GOLD") || material.contains("GLOWSTONE")) return 0xF2CA73;
            if (material.contains("OAK")) return 0x79523B;
            if (material.contains("ICE") || material.contains("LANTERN")) return 0xA4E7F1;
            if (material.contains("AMETHYST")) return 0xA67BDA;
            if (material.contains("GLASS")) return 0x78BFD5;
            return 0xE1E8EF;
        }
    }

}
