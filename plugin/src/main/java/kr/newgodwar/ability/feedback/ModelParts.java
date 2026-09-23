package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.feedback.ObjectModel.Part;

import java.util.ArrayList;
import java.util.List;

/** Reusable model geometry; positions and dimensions are measured in blocks. */
public final class ModelParts {
    private ModelParts() { }

    public static void shield(List<Part> out, double x, double y, double z) {
        box(out, "LIGHT_BLUE_STAINED_GLASS", x, y + 1.18, z + 0.62, 0.84, 0.8, 0.06, 0, 0);
        box(out, "LIGHT_BLUE_STAINED_GLASS", x, y + 0.69, z + 0.62, 0.4, 0.25, 0.06, 0, 0);
        box(out, "GOLD_BLOCK", x, y + 1.62, z + 0.62, 0.94, 0.06, 0.1, 0, 0);
        for (int side : new int[] {-1, 1}) {
            box(out, "GOLD_BLOCK", x + side * 0.46, y + 1.18, z + 0.62, 0.055, 0.86, 0.1, 0, 0);
            box(out, "GOLD_BLOCK", x + side * 0.22, y + 0.61, z + 0.62, 0.055, 0.55, 0.1, -side * 0.88, 0);
        }
    }

    public static void bar(List<Part> out, String material, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        box(out, material, (ax + bx) / 2, 0.12, (az + bz) / 2, 0.045, 0.035,
            Math.sqrt(dx * dx + dz * dz), 0, Math.atan2(dx, dz));
    }

    public static void box(List<Part> out, String material, double x, double y, double z,
                            double sx, double sy, double sz, double roll, double turn) {
        out.add(new Part(material, false, x, y, z, sx, sy, sz, roll, turn));
    }

    public static List<Part> rotate(List<Part> parts, double angle) {
        List<Part> result = new ArrayList<Part>();
        for (Part p : parts) result.add(new Part(p.material, p.item, p.x * Math.cos(angle) - p.y * Math.sin(angle),
            p.x * Math.sin(angle) + p.y * Math.cos(angle), p.z, p.sx, p.sy, p.sz, p.roll + angle, p.turn));
        return result;
    }
}
