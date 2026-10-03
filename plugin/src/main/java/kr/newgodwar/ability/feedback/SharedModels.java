package kr.newgodwar.ability.feedback;

import java.util.ArrayList;
import java.util.List;

import static kr.newgodwar.ability.feedback.ModelParts.*;

/** General-purpose models shared by multiple abilities. Ability-specific models live with the ability. */
public final class SharedModels {
    private SharedModels() { }

    public static final ObjectModel SPEAR = ObjectModel.animated((phase, detail) -> {
        List<ObjectModel.Part> out = new ArrayList<ObjectModel.Part>();
        box(out, "DARK_OAK_PLANKS", 0, 0, -1.0, 0.07, 0.07, 1.8, 0, 0);
        box(out, "GOLD_BLOCK", 0, 0, -0.2, 0.13, 0.13, 0.26, 0, 0);
        box(out, "IRON_BLOCK", -0.07, 0, -0.05, 0.08, 0.07, 0.35, 0, -0.45);
        box(out, "IRON_BLOCK", 0.07, 0, -0.05, 0.08, 0.07, 0.35, 0, 0.45);
        return out;
    });
    public static final ObjectModel SHIELD = ObjectModel.animated((phase, detail) -> {
        List<ObjectModel.Part> out = new ArrayList<ObjectModel.Part>();
        shield(out, 0, 0, 0);
        return out;
    });
    public static final ObjectModel WINGS = WingModels.create(false, false, false);
    public static final ObjectModel FIRE_WINGS = WingModels.create(true, false, false);
}
