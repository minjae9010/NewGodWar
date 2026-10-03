package kr.newgodwar.ability.feedback;

import org.bukkit.Sound;

/** Audio events are independent of renderers. Names resolve on both legacy and modern servers. */
public final class AbilitySounds {
    private AbilitySounds() { }

    public static final class Voice {
        public final Sound sound;
        public final float volume, pitch;
        Voice(Sound sound,float volume,float pitch) { this.sound=sound;this.volume=volume;this.pitch=pitch; }
    }

    public static Voice cast(AbilityStyle style, boolean advanced) {
        // A successful cast always has an audible acknowledgement, including dedicated/NONE visuals.
        Sound tone=style.theme().sound();
        switch(style.theme()) {
            case LIGHTNING: tone=resolve("BLOCK_RESPAWN_ANCHOR_CHARGE","BLOCK_NOTE_BLOCK_PLING","BLOCK_NOTE_PLING");break;
            case CRAFT: tone=resolve("BLOCK_ANVIL_PLACE");break;
            case HUNT: tone=resolve("ENTITY_ARROW_SHOOT");break;
            case SWARM: tone=resolve("ENTITY_BEE_POLLINATE","ENTITY_BAT_TAKEOFF");break;
            case HEALING: tone=resolve("BLOCK_AMETHYST_BLOCK_CHIME","BLOCK_NOTE_BLOCK_CHIME","BLOCK_NOTE_CHIME");break;
            default: break;
        }
        return new Voice(tone,advanced?.52F:.36F,advanced?.85F:1.15F);
    }

    public static Voice reaction(AbilityStyle style,EffectCue cue) {
        Sound tone;
        switch(cue) {
            case NONE:return null;
            case FORGE:tone=resolve("BLOCK_ANVIL_USE");break;
            case ITEM:tone=resolve("ENTITY_ITEM_PICKUP");break;
            case HEAL:case CLEANSE:tone=resolve("BLOCK_AMETHYST_BLOCK_CHIME","BLOCK_NOTE_BLOCK_CHIME","BLOCK_NOTE_CHIME");break;
            case GUARD:tone=resolve("ITEM_SHIELD_BLOCK");break;
            case WINGS:case WIND:tone=AbilityTheme.WIND.sound();break;
            case HASTE:tone=resolve("BLOCK_NOTE_BLOCK_HAT","BLOCK_NOTE_HAT");break;
            case WEAKNESS:tone=resolve("BLOCK_NOTE_BLOCK_BASS","BLOCK_NOTE_BASS");break;
            case FROST:tone=resolve("BLOCK_GLASS_BREAK");break;
            case WATER:tone=AbilityTheme.WATER.sound();break;
            case ROOT:case BLOOM:tone=resolve("BLOCK_GRASS_PLACE");break;
            case FIRE:tone=AbilityTheme.FIRE.sound();break;
            case HIT:case SLASH:tone=AbilityTheme.COMBAT.sound();break;
            case POISON:case HUNGER:tone=resolve("ENTITY_GENERIC_DRINK");break;
            case SLOW:case SEAL:tone=resolve("BLOCK_CHAIN_PLACE","BLOCK_IRON_DOOR_CLOSE");break;
            case PORTAL:case STEALTH:tone=AbilityTheme.SHADOW.sound();break;
            case BLIND:case SLEEP:tone=resolve("BLOCK_NOTE_BLOCK_BASS","BLOCK_NOTE_BASS");break;
            case MUSIC:tone=AbilityTheme.MUSIC.sound();break;
            default:tone=style.theme().sound();break;
        }
        return new Voice(tone,cue==EffectCue.FORGE?.28F:.25F,cue==EffectCue.SLEEP?.65F:1.25F);
    }

    public static boolean forge(DesignedEffect design) {
        return design == kr.newgodwar.ability.builtin.AbilityDesigns.ANVIL
            || design == kr.newgodwar.ability.builtin.AbilityDesigns.IRON_FORGE
            || design == kr.newgodwar.ability.builtin.AbilityDesigns.GEM_FORGE;
    }

    public static Voice finish(AbilityStyle style) { return new Voice(style.theme().sound(),.18F,.65F); }
    private static Sound resolve(String... names) { return AbilityTheme.sound(names); }
}
