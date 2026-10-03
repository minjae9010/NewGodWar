package kr.newgodwar.ability.feedback;

import java.util.Objects;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable visual choices declared in an ability file; the renderer only executes them. */
public final class AbilityStyle {
    public static final AbilityStyle DEFAULT = builder(AbilityTheme.ARCANE).hit(EffectCue.HIT).build();

    private final AbilityTheme theme, trailTheme;
    private final EffectCue normal, advanced, hit, benefit, passive;
    private final boolean dedicated, privateCast;
    private final ObjectModel flightModel;
    private final Map<EffectCue, DesignedEffect> effects;
    private final Map<EffectCue, DesignedEffect> receivedEffects;

    private AbilityStyle(Builder builder) {
        theme = builder.theme;
        trailTheme = builder.trailTheme == null ? theme : builder.trailTheme;
        normal = builder.normal; advanced = builder.advanced; hit = builder.hit;
        benefit = builder.benefit; passive = builder.passive;
        dedicated = builder.dedicated; privateCast = builder.privateCast;
        flightModel = builder.flightModel;
        effects = Collections.unmodifiableMap(new EnumMap<EffectCue, DesignedEffect>(builder.effects));
        receivedEffects = Collections.unmodifiableMap(new EnumMap<EffectCue, DesignedEffect>(builder.receivedEffects));
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
    public DesignedEffect effect(EffectCue cue) { return effects.get(cue); }
    /** Recipients never inherit a caster's weapon, aiming reticle, crafting table or spellbook. */
    public DesignedEffect receivedEffect(EffectCue cue) {
        DesignedEffect effect=receivedEffects.get(cue);
        return effect!=null?effect:kr.newgodwar.ability.builtin.AbilityDesigns.status(cue);
    }
    public Map<EffectCue, DesignedEffect> effects() { return effects; }

    public static final class Builder {
        private final AbilityTheme theme;
        private AbilityTheme trailTheme;
        private EffectCue normal = EffectCue.NONE, advanced = EffectCue.NONE, hit = EffectCue.NONE;
        private EffectCue benefit = EffectCue.NONE, passive = EffectCue.NONE;
        private boolean dedicated, privateCast;
        private ObjectModel flightModel;
        private final Map<EffectCue, DesignedEffect> effects = new EnumMap<EffectCue, DesignedEffect>(EffectCue.class);
        private final Map<EffectCue, DesignedEffect> receivedEffects = new EnumMap<EffectCue, DesignedEffect>(EffectCue.class);

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
        public Builder effect(EffectCue cue, DesignedEffect effect) {
            effects.put(Objects.requireNonNull(cue, "cue"), Objects.requireNonNull(effect, "effect")); return this;
        }
        public Builder received(EffectCue cue, DesignedEffect effect) {
            receivedEffects.put(Objects.requireNonNull(cue,"cue"),Objects.requireNonNull(effect,"effect"));return this;
        }
        public AbilityStyle build() { return new AbilityStyle(this); }
    }

    public EffectCue status(String potion) {
        if (dedicated || potion == null) return EffectCue.NONE;
        if (potion.equals("INVISIBILITY")) return EffectCue.STEALTH;
        if (hit == EffectCue.SLEEP || hit == EffectCue.MUSIC) return hit;
        switch (potion) {
            case "POISON": case "WITHER": case "CONFUSION": case "NAUSEA": return EffectCue.POISON;
            case "BLINDNESS": return EffectCue.BLIND;
            case "SLOW": case "SLOWNESS": return hit == EffectCue.ROOT ? hit : EffectCue.SLOW;
            case "REGENERATION": case "HEAL": case "INSTANT_HEALTH": return EffectCue.HEAL;
            case "HASTE": case "FAST_DIGGING": return EffectCue.HASTE;
            case "WEAKNESS": return EffectCue.WEAKNESS;
            case "ABSORPTION": case "DAMAGE_RESISTANCE": case "RESISTANCE": return EffectCue.GUARD;
            case "SPEED": case "JUMP": case "JUMP_BOOST": case "LEVITATION": return EffectCue.WIND;
            case "INCREASE_DAMAGE": case "STRENGTH": return EffectCue.CHARGE;
            // Routine haste/night-vision refreshes and minor debuffs need no extra body decoration.
            default: return EffectCue.NONE;
        }
    }
}
