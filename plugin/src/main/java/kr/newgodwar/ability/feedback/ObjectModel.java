package kr.newgodwar.ability.feedback;

import java.util.ArrayList;
import java.util.List;

/** Small resource-pack-free models. Dimensions are in blocks, relative to the scene anchor. */
public enum ObjectModel {
    HAMMER, SPEAR, SHIELD, PHALANX, WINGS, FIRE_WINGS, FIRE_RUNE, FROST_RUNE, SCALES, GRAVITY;

    public static final class Part {
        public final String material;
        public final boolean item;
        public final double x, y, z, sx, sy, sz, roll, turn;
        Part(String material, boolean item, double x, double y, double z, double sx, double sy, double sz, double roll, double turn) {
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

    public List<Part> parts(double phase, double detail) {
        if (!Double.isFinite(phase)) phase = 0;
        if (!Double.isFinite(detail)) detail = 1;
        detail = Math.max(0, Math.min(6, detail));
        List<Part> out = new ArrayList<Part>();
        switch (this) {
            case HAMMER:
                box(out, "IRON_BLOCK", 0, 0.12, 0, 0.78, 0.34, 0.34, 0, 0);
                box(out, "POLISHED_ANDESITE", -0.43, 0.12, 0, 0.09, 0.28, 0.28, 0, 0);
                box(out, "POLISHED_ANDESITE", 0.43, 0.12, 0, 0.09, 0.28, 0.28, 0, 0);
                box(out, "DARK_OAK_PLANKS", 0, -0.4, 0, 0.11, 0.7, 0.11, 0, 0);
                box(out, "GOLD_BLOCK", 0, -0.1, 0, 0.18, 0.09, 0.18, 0, 0);
                box(out, "GOLD_BLOCK", 0, -0.76, 0, 0.18, 0.12, 0.18, 0, 0);
                return rotate(out, Math.sin(phase * 0.6) * 0.4);
            case SPEAR:
                box(out, "DARK_OAK_PLANKS", 0, 0, -1.0, 0.07, 0.07, 1.8, 0, 0);
                box(out, "GOLD_BLOCK", 0, 0, -0.2, 0.13, 0.13, 0.26, 0, 0);
                box(out, "IRON_BLOCK", -0.07, 0, -0.05, 0.08, 0.07, 0.35, 0, -0.45);
                box(out, "IRON_BLOCK", 0.07, 0, -0.05, 0.08, 0.07, 0.35, 0, 0.45);
                break;
            case SHIELD: shield(out, 0, 0, 0); break;
            case PHALANX:
                for (int i = -2; i <= 2; i++) {
                    double angle = i * Math.PI / 5;
                    shield(out, Math.sin(angle) * 4.5, 0, Math.cos(angle) * 4.5);
                }
                break;
            case WINGS: case FIRE_WINGS:
                for (int side : new int[] {-1, 1}) for (int feather = 0; feather < 5; feather++) {
                    double flap = Math.sin(phase * 0.22) * 0.12;
                    out.add(new Part(this == FIRE_WINGS ? "BLAZE_ROD" : "FEATHER", true,
                        side * (0.35 + feather * 0.22), 1.4 + feather * 0.12 + flap,
                        -0.3 - feather * 0.045, 0.7, 0.9, 0.7, side * (0.45 + feather * 0.08), side * flap));
                }
                break;
            case FIRE_RUNE: case FROST_RUNE:
                double radius = Math.max(0.5, Math.min(3, detail));
                int arms = this == FROST_RUNE ? 6 : 3;
                String material = this == FROST_RUNE ? "SEA_LANTERN" : "GLOWSTONE";
                for (int i = 0; i < arms; i++) {
                    double a = Math.PI * 2 * i / arms, b = Math.PI * 2 * (i + 1) / arms;
                    bar(out, material, Math.cos(a) * radius, Math.sin(a) * radius,
                        Math.cos(b) * radius, Math.sin(b) * radius);
                    bar(out, material, 0, 0, Math.cos(a) * radius * 0.75, Math.sin(a) * radius * 0.75);
                }
                out.add(new Part(this == FROST_RUNE ? "PRISMARINE_CRYSTALS" : "BLAZE_POWDER", true,
                    0, 0.35 + Math.sin(phase * 0.15) * 0.06, 0, 0.5, 0.5, 0.5, 0, phase * 0.03));
                break;
            case SCALES:
                double tilt = Math.min(4, detail) * 0.08;
                box(out, "GOLD_BLOCK", 0, -0.12, 0, 0.055, 0.9, 0.055, 0, 0);
                box(out, "GOLD_BLOCK", 0, 0.1, 0, 1.25, 0.055, 0.055, -tilt, 0);
                for (int side : new int[] {-1, 1}) {
                    double y = 0.1 - side * Math.sin(tilt) * 0.6;
                    box(out, "IRON_BLOCK", side * 0.6, y - 0.2, 0, 0.025, 0.4, 0.025, 0, 0);
                    box(out, "GOLD_BLOCK", side * 0.6, y - 0.42, 0, 0.32, 0.06, 0.28, 0, 0);
                }
                break;
            case GRAVITY:
                box(out, "CRYING_OBSIDIAN", 0, 0.65, 0, 0.42, 0.42, 0.42, phase * 0.08, phase * 0.06);
                for (int i = 0; i < 3; i++) {
                    double a = phase * 0.1 + i * Math.PI * 2 / 3;
                    box(out, "AMETHYST_BLOCK", Math.cos(a) * 0.85, 0.65 + Math.sin(a * 2) * 0.25,
                        Math.sin(a) * 0.85, 0.16, 0.16, 0.16, a, a);
                }
                break;
            default: throw new AssertionError(this);
        }
        return out;
    }

    private static void shield(List<Part> out, double x, double y, double z) {
        box(out, "LIGHT_BLUE_STAINED_GLASS", x, y + 1.18, z + 0.62, 0.84, 0.8, 0.06, 0, 0);
        box(out, "LIGHT_BLUE_STAINED_GLASS", x, y + 0.69, z + 0.62, 0.4, 0.25, 0.06, 0, 0);
        box(out, "GOLD_BLOCK", x, y + 1.62, z + 0.62, 0.94, 0.06, 0.1, 0, 0);
        for (int side : new int[] {-1, 1}) {
            box(out, "GOLD_BLOCK", x + side * 0.46, y + 1.18, z + 0.62, 0.055, 0.86, 0.1, 0, 0);
            box(out, "GOLD_BLOCK", x + side * 0.22, y + 0.61, z + 0.62, 0.055, 0.55, 0.1, -side * 0.88, 0);
        }
    }

    private static void bar(List<Part> out, String material, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        box(out, material, (ax + bx) / 2, 0.12, (az + bz) / 2, 0.045, 0.035,
            Math.sqrt(dx * dx + dz * dz), 0, Math.atan2(dx, dz));
    }

    private static void box(List<Part> out, String material, double x, double y, double z,
                            double sx, double sy, double sz, double roll, double turn) {
        out.add(new Part(material, false, x, y, z, sx, sy, sz, roll, turn));
    }

    private static List<Part> rotate(List<Part> parts, double angle) {
        List<Part> result = new ArrayList<Part>();
        for (Part p : parts) result.add(new Part(p.material, p.item, p.x * Math.cos(angle) - p.y * Math.sin(angle),
            p.x * Math.sin(angle) + p.y * Math.cos(angle), p.z, p.sx, p.sy, p.sz, p.roll + angle, p.turn));
        return result;
    }
}
