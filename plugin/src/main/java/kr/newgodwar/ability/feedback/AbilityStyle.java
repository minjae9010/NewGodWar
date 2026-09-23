package kr.newgodwar.ability.feedback;

import java.util.Objects;

/** Immutable visual choices declared in an ability file; the renderer only executes them. */
public final class AbilityStyle {
    public static final AbilityStyle DEFAULT = builder(AbilityTheme.ARCANE).hit(EffectCue.HIT).build();

    private final AbilityTheme theme, trailTheme;
    private final EffectCue normal, advanced, hit, benefit, passive;
    private final boolean dedicated, privateCast;
    private final ObjectModel flightModel;

    private AbilityStyle(Builder builder) {
        theme = builder.theme;
        trailTheme = builder.trailTheme == null ? theme : builder.trailTheme;
        normal = builder.normal; advanced = builder.advanced; hit = builder.hit;
        benefit = builder.benefit; passive = builder.passive;
        dedicated = builder.dedicated; privateCast = builder.privateCast;
        flightModel = builder.flightModel;
    }

    public static Builder builder(AbilityTheme theme) { return new Builder(theme); }
    public AbilityTheme theme() { return theme; }
    public AbilityTheme trailTheme() { return trailTheme; }
    public EffectCue cast(boolean advanced) { return advanced ? this.advanced : normal; }
    public EffectCue hit() { return hit; }
    public EffectCue benefit() { return benefit; }
    public EffectCue passive() { return passive; }
    public boolean dedicated() { return dedicated; }
    public boolean privateCast() { return privateCast; }
    public ObjectModel flightModel() { return flightModel; }

    public static final class Builder {
        private final AbilityTheme theme;
        private AbilityTheme trailTheme;
        private EffectCue normal = EffectCue.NONE, advanced = EffectCue.NONE, hit = EffectCue.NONE;
        private EffectCue benefit = EffectCue.NONE, passive = EffectCue.NONE;
        private boolean dedicated, privateCast;
        private ObjectModel flightModel;

        private Builder(AbilityTheme theme) { this.theme = Objects.requireNonNull(theme, "theme"); }
        public Builder normal(EffectCue cue) { normal = Objects.requireNonNull(cue, "cue"); return this; }
        public Builder advanced(EffectCue cue) { advanced = Objects.requireNonNull(cue, "cue"); return this; }
        public Builder hit(EffectCue cue) { hit = Objects.requireNonNull(cue, "cue"); return this; }
        public Builder benefit(EffectCue cue) { benefit = Objects.requireNonNull(cue, "cue"); return this; }
        public Builder passive(EffectCue cue) { passive = Objects.requireNonNull(cue, "cue"); return this; }
        public Builder dedicated() { dedicated = true; return this; }
        public Builder privateCast() { privateCast = true; return this; }
        public Builder trail(AbilityTheme theme) { trailTheme = Objects.requireNonNull(theme, "theme"); return this; }
        public Builder flight(ObjectModel model) { flightModel = Objects.requireNonNull(model, "model"); return this; }
        public AbilityStyle build() { return new AbilityStyle(this); }
    }

    public EffectCue status(String potion) {
        if (dedicated || potion == null) return EffectCue.NONE;
        if (potion.equals("INVISIBILITY")) return EffectCue.STEALTH;
        if (hit == EffectCue.SLEEP || hit == EffectCue.MUSIC) return hit;
        switch (potion) {
            case "POISON": case "WITHER": case "CONFUSION": case "NAUSEA": return EffectCue.POISON;
            case "BLINDNESS": return hit == EffectCue.SEAL || hit == EffectCue.FIRE ? hit : EffectCue.BLIND;
            case "SLOW": case "SLOWNESS": return hit == EffectCue.ROOT || hit == EffectCue.SEAL ? hit : EffectCue.SLOW;
            case "REGENERATION": case "HEAL": case "INSTANT_HEALTH": return EffectCue.HEAL;
            case "ABSORPTION": case "DAMAGE_RESISTANCE": case "RESISTANCE": return EffectCue.GUARD;
            case "SPEED": case "JUMP": case "JUMP_BOOST": case "LEVITATION": return EffectCue.WIND;
            case "INCREASE_DAMAGE": case "STRENGTH": return EffectCue.CHARGE;
            // Routine haste/night-vision refreshes and minor debuffs need no extra body decoration.
            default: return EffectCue.NONE;
        }
    }
}
