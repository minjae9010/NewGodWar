package kr.newgodwar.ability.feedback;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Each action has one visual owner: cast cue, target reaction, or its existing dedicated effect. */
public final class AbilityStyle {
    private static final Map<String, AbilityStyle> STYLES = new LinkedHashMap<String, AbilityStyle>();
    private static final AbilityStyle FALLBACK = new AbilityStyle(AbilityTheme.ARCANE, EffectCue.NONE,
        EffectCue.NONE, EffectCue.HIT, EffectCue.NONE, EffectCue.NONE, false);
    static {
        add("acidarcher", AbilityTheme.HUNT, EffectCue.ITEM, EffectCue.ITEM, EffectCue.POISON, EffectCue.NONE, EffectCue.NONE, false);
        add("aeolus", AbilityTheme.WIND, EffectCue.NONE, EffectCue.NONE, EffectCue.WIND, EffectCue.HEAL, EffectCue.NONE, false);
        add("anjunggeun", AbilityTheme.COMBAT, EffectCue.NONE, EffectCue.NONE, EffectCue.HIT, EffectCue.NONE, EffectCue.CHARGE, false);
        add("akasha", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.POISON, EffectCue.HEAL, EffectCue.NONE, false);
        add("amaterasu", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.FIRE, EffectCue.NONE, EffectCue.NONE, false);
        add("anorexia", AbilityTheme.NATURE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("anubis", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("apollon", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.FIRE, EffectCue.NONE, EffectCue.NONE, false);
        add("aprodite", AbilityTheme.HEALING, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.NONE, EffectCue.NONE, false);
        add("archer", AbilityTheme.HUNT, EffectCue.ITEM, EffectCue.ITEM, EffectCue.HIT, EffectCue.NONE, EffectCue.NONE, false);
        add("ares", AbilityTheme.COMBAT, EffectCue.NONE, EffectCue.NONE, EffectCue.SLASH, EffectCue.NONE, EffectCue.WIND, false);
        add("artemis", AbilityTheme.HUNT, EffectCue.ITEM, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("asclepius", AbilityTheme.HEALING, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.HEAL, EffectCue.NONE, false);
        add("assasin", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.NONE, EffectCue.NONE, false);
        add("athena", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("blacksmith", AbilityTheme.CRAFT, EffectCue.FORGE, EffectCue.FORGE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("blinder", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.BLIND, EffectCue.NONE, EffectCue.NONE, false);
        add("bomber", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("bulter", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.GUARD, EffectCue.NONE, EffectCue.GUARD, false);
        add("chronos", AbilityTheme.TIME, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("clocking", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.SLASH, EffectCue.NONE, EffectCue.NONE, false);
        add("counter", AbilityTheme.RUNE, EffectCue.NONE, EffectCue.NONE, EffectCue.SEAL, EffectCue.NONE, EffectCue.NONE, false);
        add("creeper", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.CHARGE, true);
        add("darkness", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.GUARD, false);
        add("demeter", AbilityTheme.NATURE, EffectCue.ITEM, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("dionysus", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.POISON, EffectCue.NONE, EffectCue.NONE, false);
        add("echo", AbilityTheme.ECHO, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("eris", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.NONE, EffectCue.NONE, false);
        add("examinee", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("fisher", AbilityTheme.WATER, EffectCue.NONE, EffectCue.NONE, EffectCue.WATER, EffectCue.NONE, EffectCue.WATER, false);
        add("frost", AbilityTheme.FROST, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("gaia", AbilityTheme.NATURE, EffectCue.NONE, EffectCue.NONE, EffectCue.ROOT, EffectCue.HEAL, EffectCue.NONE, false);
        add("gardener", AbilityTheme.NATURE, EffectCue.NONE, EffectCue.NONE, EffectCue.BLOOM, EffectCue.NONE, EffectCue.NONE, false);
        add("gigachad", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.HIT, EffectCue.GUARD, EffectCue.GUARD, false);
        add("girl", AbilityTheme.HEALING, EffectCue.NONE, EffectCue.NONE, EffectCue.SLOW, EffectCue.NONE, EffectCue.NONE, false);
        add("goldspoon", AbilityTheme.CRAFT, EffectCue.NONE, EffectCue.NONE, EffectCue.ITEM, EffectCue.NONE, EffectCue.NONE, false);
        add("graviton", AbilityTheme.GRAVITY, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("hades", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("harry", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.POISON, EffectCue.NONE, EffectCue.NONE, false);
        add("hecate", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.STEALTH, EffectCue.NONE, EffectCue.NONE, false);
        add("heojun", AbilityTheme.HEALING, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.HEAL, EffectCue.NONE, false);
        add("hephaestus", AbilityTheme.CRAFT, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("hera", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("hermes", AbilityTheme.WIND, EffectCue.WINGS, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("hermione", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("honggildong", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.STEALTH, EffectCue.NONE, EffectCue.NONE, false);
        add("horeundal", AbilityTheme.TIME, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.NONE, EffectCue.NONE, false);
        add("invincibility", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.GUARD, EffectCue.HEAL, EffectCue.NONE, false);
        add("iris", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.HEAL, EffectCue.NONE, false);
        add("jangyeongsil", AbilityTheme.CRAFT, EffectCue.FORGE, EffectCue.NONE, EffectCue.HIT, EffectCue.GUARD, EffectCue.NONE, false);
        add("jujak", AbilityTheme.FIRE, EffectCue.WINGS, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("loki", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.NONE, EffectCue.NONE, false);
        add("megumin", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("midoriya", AbilityTheme.LIGHTNING, EffectCue.NONE, EffectCue.NONE, EffectCue.HIT, EffectCue.NONE, EffectCue.CHARGE, false);
        add("miner", AbilityTheme.CRAFT, EffectCue.NONE, EffectCue.NONE, EffectCue.HIT, EffectCue.NONE, EffectCue.NONE, false);
        add("morpious", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.SLEEP, EffectCue.NONE, EffectCue.NONE, false);
        add("naro", AbilityTheme.FIRE, EffectCue.WIND, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("nasdaq", AbilityTheme.CRAFT, EffectCue.NONE, EffectCue.NONE, EffectCue.ITEM, EffectCue.NONE, EffectCue.NONE, false);
        add("nature", AbilityTheme.NATURE, EffectCue.ITEM, EffectCue.NONE, EffectCue.NONE, EffectCue.HEAL, EffectCue.NONE, false);
        add("nike", AbilityTheme.WIND, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("odin", AbilityTheme.RUNE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("onepunch", AbilityTheme.COMBAT, EffectCue.CHARGE, EffectCue.NONE, EffectCue.HIT, EffectCue.NONE, EffectCue.NONE, false);
        add("pan", AbilityTheme.MUSIC, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("persephone", AbilityTheme.NATURE, EffectCue.NONE, EffectCue.NONE, EffectCue.ROOT, EffectCue.HEAL, EffectCue.NONE, false);
        add("pokego", AbilityTheme.NATURE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("poseidon", AbilityTheme.WATER, EffectCue.NONE, EffectCue.NONE, EffectCue.WATER, EffectCue.NONE, EffectCue.NONE, true);
        add("priest", AbilityTheme.HEALING, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("queenbee", AbilityTheme.SWARM, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("quetzalcoatl", AbilityTheme.WIND, EffectCue.WINGS, EffectCue.NONE, EffectCue.WIND, EffectCue.NONE, EffectCue.NONE, false);
        add("ra", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.FIRE, EffectCue.NONE, EffectCue.NONE, false);
        add("reflection", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.HIT, EffectCue.NONE, EffectCue.GUARD, false);
        add("rickroll", AbilityTheme.MUSIC, EffectCue.NONE, EffectCue.NONE, EffectCue.MUSIC, EffectCue.NONE, EffectCue.NONE, false);
        add("runesmith", AbilityTheme.RUNE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("scrooge", AbilityTheme.CRAFT, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("sejong", AbilityTheme.RUNE, EffectCue.NONE, EffectCue.NONE, EffectCue.SEAL, EffectCue.HEAL, EffectCue.NONE, false);
        add("selene", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.STEALTH, EffectCue.NONE, EffectCue.NONE, false);
        add("shinsaimdang", AbilityTheme.NATURE, EffectCue.ITEM, EffectCue.NONE, EffectCue.NONE, EffectCue.HEAL, EffectCue.NONE, false);
        add("siksin", AbilityTheme.NATURE, EffectCue.ITEM, EffectCue.ITEM, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, false);
        add("sniper", AbilityTheme.HUNT, EffectCue.NONE, EffectCue.NONE, EffectCue.HIT, EffectCue.NONE, EffectCue.ITEM, false);
        add("snow", AbilityTheme.FROST, EffectCue.ITEM, EffectCue.NONE, EffectCue.FROST, EffectCue.NONE, EffectCue.NONE, false);
        add("stance", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.GUARD, false);
        add("sus", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.NONE, EffectCue.NONE, false);
        add("tajja", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.SLASH, EffectCue.NONE, EffectCue.NONE, false);
        add("teleporter", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.PORTAL, EffectCue.NONE, EffectCue.NONE, true);
        add("thisisfine", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.FIRE, EffectCue.HEAL, EffectCue.NONE, false);
        add("thor", AbilityTheme.LIGHTNING, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
        add("voodoo", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.POISON, EffectCue.NONE, EffectCue.NONE, false);
        add("witch", AbilityTheme.SHADOW, EffectCue.NONE, EffectCue.NONE, EffectCue.POISON, EffectCue.NONE, EffectCue.NONE, false);
        add("wizard", AbilityTheme.ARCANE, EffectCue.NONE, EffectCue.NONE, EffectCue.WIND, EffectCue.NONE, EffectCue.NONE, false);
        add("yisunsin", AbilityTheme.WATER, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.GUARD, EffectCue.NONE, false);
        add("yugwansun", AbilityTheme.GUARD, EffectCue.NONE, EffectCue.NONE, EffectCue.FIRE, EffectCue.GUARD, EffectCue.NONE, false);
        add("zet", AbilityTheme.FIRE, EffectCue.NONE, EffectCue.NONE, EffectCue.FIRE, EffectCue.NONE, EffectCue.WIND, false);
        add("zeus", AbilityTheme.LIGHTNING, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, EffectCue.NONE, true);
    }
    private final AbilityTheme theme;
    private final EffectCue normal, advanced, hit, benefit, passive;
    private final boolean dedicated;
    private AbilityStyle(AbilityTheme theme, EffectCue normal, EffectCue advanced, EffectCue hit,
                         EffectCue benefit, EffectCue passive, boolean dedicated) {
        this.theme = theme; this.normal = normal; this.advanced = advanced; this.hit = hit;
        this.benefit = benefit; this.passive = passive; this.dedicated = dedicated;
    }
    private static void add(String id, AbilityTheme theme, EffectCue normal, EffectCue advanced,
                            EffectCue hit, EffectCue benefit, EffectCue passive, boolean dedicated) {
        if (STYLES.put(id, new AbilityStyle(theme, normal, advanced, hit, benefit, passive, dedicated)) != null)
            throw new IllegalStateException("Duplicate ability style: " + id);
    }
    public static AbilityStyle of(String id) { AbilityStyle style = STYLES.get(id); return style == null ? FALLBACK : style; }
    public static Set<String> ids() { return Collections.unmodifiableSet(STYLES.keySet()); }
    public AbilityTheme theme() { return theme; }
    public EffectCue cast(boolean advanced) { return advanced ? this.advanced : normal; }
    public EffectCue hit() { return hit; }
    public EffectCue benefit() { return benefit; }
    public EffectCue passive() { return passive; }
    public boolean dedicated() { return dedicated; }

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
