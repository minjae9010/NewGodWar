package kr.newgodwar.ability.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A named, short action with an opening, readable silhouette, and closing movement. */
public final class DesignedEffect {
    public static final int DURATION = 18;
    private final String description;
    private final ObjectModel animated, still;

    public DesignedEffect(String description, ObjectModel model) {
        this.description = Objects.requireNonNull(description, "description");
        Objects.requireNonNull(model, "model");
        animated = ObjectModel.animated((phase, detail) -> {
            double age = Math.max(0, Math.min(DURATION, phase));
            double opening = Math.min(1, (age + 1) / 5);
            double closing = Math.min(1, (DURATION - age + 1) / 6);
            double scale = Math.max(0.08, opening * closing);
            List<ObjectModel.Part> out = new ArrayList<ObjectModel.Part>();
            for (ObjectModel.Part p : model.parts(age, detail)) {
                // Each component grows in place; roots stay at the feet and crowns above the head.
                out.add(new ObjectModel.Part(p.material, p.item, p.x, p.y, p.z,
                    p.sx * scale, p.sy * scale, p.sz * scale, p.roll, p.turn, p.pitch, p.art, p.billboard));
            }
            return out;
        });
        still = ObjectModel.animated((phase, detail) -> model.parts(8, detail));
    }

    public String description() { return description; }
    public ObjectModel model(boolean animate) { return animate ? animated : still; }
}
