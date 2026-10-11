package kr.newgodwar.ability.feedback;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One sampled pose per scene and animation input, independent of viewer/anchor updates. */
final class ObjectFrame {
    private final ObjectModel model;
    private double phase, detail;
    private List<ObjectModel.Part> parts;

    ObjectFrame(ObjectModel model) { this.model = model; }

    List<ObjectModel.Part> sample(double phase, double detail) {
        if (parts == null || this.phase != phase || this.detail != detail) {
            // Models may reuse a mutable list; keep the previously rendered pose intact for comparisons.
            parts = Collections.unmodifiableList(new ArrayList<ObjectModel.Part>(model.parts(phase, detail)));
            this.phase = phase; this.detail = detail;
        }
        return parts;
    }

    static boolean sameTransform(ObjectModel.Part before, ObjectModel.Part after) {
        return before != null && before.item == after.item && before.billboard == after.billboard
            && ((after.item && after.billboard) || sameOffset(before, after))
            && before.sx == after.sx && before.sy == after.sy && before.sz == after.sz
            && before.roll == after.roll && before.turn == after.turn && before.pitch == after.pitch;
    }

    static boolean sameOffset(ObjectModel.Part before, ObjectModel.Part after) {
        return before != null && before.x == after.x && before.y == after.y && before.z == after.z;
    }

    /** Billboard rotation must turn only the mesh, never orbit its authored offset around each viewer. */
    static Location displayLocation(Location anchor, ObjectModel.Part part) {
        if (!part.item || !part.billboard) return anchor;
        double yaw = Math.toRadians(anchor.getYaw()), pitch = Math.toRadians(anchor.getPitch());
        double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch), sinPitch = Math.sin(pitch);
        double y = part.y * cosPitch - part.z * sinPitch;
        double z = part.y * sinPitch + part.z * cosPitch;
        return anchor.clone().add(part.x * cosYaw - z * sinYaw, y, part.x * sinYaw + z * cosYaw);
    }

    static boolean sameItem(ObjectModel.Part before, ObjectModel.Part after) {
        return before != null && before.item == after.item
            && Objects.equals(before.material, after.material) && Objects.equals(before.art, after.art);
    }
}
