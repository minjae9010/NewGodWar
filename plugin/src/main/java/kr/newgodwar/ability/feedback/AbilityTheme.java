package kr.newgodwar.ability.feedback;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;

/** Data-free particles only, with names for both legacy and modern Bukkit. */
public enum AbilityTheme {
    LIGHTNING(ChatColor.YELLOW, "END_ROD", "END_ROD", "ENTITY_LIGHTNING_BOLT_IMPACT", "ENTITY_LIGHTNING_IMPACT"),
    FIRE(ChatColor.GOLD, "FLAME", "FLAME", "ITEM_FIRECHARGE_USE", "ITEM_FIRECHARGE_USE"),
    WATER(ChatColor.AQUA, "SPLASH", "WATER_SPLASH", "ENTITY_PLAYER_SPLASH", "ENTITY_PLAYER_SPLASH"),
    FROST(ChatColor.WHITE, "SNOWFLAKE", "SNOW_SHOVEL", "BLOCK_GLASS_BREAK", "BLOCK_GLASS_BREAK"),
    WIND(ChatColor.WHITE, "CLOUD", "CLOUD", "ENTITY_ENDER_DRAGON_FLAP", "ENTITY_ENDERDRAGON_FLAP"),
    NATURE(ChatColor.GREEN, "HAPPY_VILLAGER", "VILLAGER_HAPPY", "BLOCK_GRASS_BREAK", "BLOCK_GRASS_BREAK"),
    HEALING(ChatColor.LIGHT_PURPLE, "HEART", "HEART", "ENTITY_EXPERIENCE_ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP"),
    SHADOW(ChatColor.DARK_PURPLE, "WITCH", "SPELL_WITCH", "ENTITY_ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT"),
    ARCANE(ChatColor.LIGHT_PURPLE, "ENCHANT", "ENCHANTMENT_TABLE", "BLOCK_ENCHANTMENT_TABLE_USE", "BLOCK_ENCHANTMENT_TABLE_USE"),
    GUARD(ChatColor.YELLOW, "END_ROD", "END_ROD", "ITEM_SHIELD_BLOCK", "ITEM_SHIELD_BLOCK"),
    COMBAT(ChatColor.RED, "CRIT", "CRIT", "ENTITY_PLAYER_ATTACK_SWEEP", "ENTITY_PLAYER_ATTACK_SWEEP"),
    CRAFT(ChatColor.GOLD, "FIREWORK", "FIREWORKS_SPARK", "BLOCK_ANVIL_USE", "BLOCK_ANVIL_USE"),
    MUSIC(ChatColor.AQUA, "NOTE", "NOTE", "BLOCK_NOTE_BLOCK_HARP", "BLOCK_NOTE_HARP");

    private final ChatColor color;
    private final Particle particle;
    private final Sound sound;

    AbilityTheme(ChatColor color, String particle, String legacyParticle, String sound, String legacySound) {
        this.color = color;
        this.particle = resolve(Particle.class, particle, legacyParticle);
        this.sound = sound(sound, legacySound);
    }

    public ChatColor color() { return color; }
    public Particle particle() { return particle; }
    public Sound sound() { return sound; }

    public static AbilityTheme of(String id) {
        if (id == null) return ARCANE;
        switch (id) {
            case "zeus": case "thor": return LIGHTNING;
            case "apollon": case "amaterasu": case "ra": case "hephaestus":
            case "jujak": case "thisisfine": case "bomber": case "creeper": case "megumin": return FIRE;
            case "poseidon": case "fisher": case "yisunsin": return WATER;
            case "frost": case "snow": return FROST;
            case "aeolus": case "hermes": case "naro": case "nike": case "quetzalcoatl": return WIND;
            case "gaia": case "demeter": case "nature": case "persephone": case "gardener":
            case "shinsaimdang": case "siksin": return NATURE;
            case "asclepius": case "heojun": case "priest": case "aprodite": case "girl": return HEALING;
            case "hades": case "anubis": case "witch": case "voodoo": case "blinder":
            case "clocking": case "hecate": case "loki": case "sus": case "honggildong":
            case "selene": case "morpious": case "assasin": return SHADOW;
            case "hera": case "invincibility": case "darkness": case "reflection":
            case "stance": case "odin": case "yugwansun": return GUARD;
            case "ares": case "archer": case "artemis": case "acidarcher": case "sniper":
            case "onepunch": case "midoriya": case "gigachad": case "anjunggeun": return COMBAT;
            case "blacksmith": case "jangyeongsil": case "athena": case "nasdaq":
            case "miner": case "goldspoon": case "scrooge": case "tajja": return CRAFT;
            case "rickroll": case "pan": return MUSIC;
            default: return ARCANE;
        }
    }

    public static boolean privateCast(String id) {
        return "clocking".equals(id) || "hecate".equals(id) || "loki".equals(id)
            || "sus".equals(id) || "honggildong".equals(id) || "selene".equals(id)
            || "assasin".equals(id) || "tajja".equals(id);
    }

    static <T extends Enum<T>> T resolve(Class<T> type, String... names) {
        for (String name : names) {
            try {
                return Enum.valueOf(type, name);
            } catch (IllegalArgumentException ignored) {
                // Cosmetic features must also work on the oldest supported server.
            }
        }
        return null;
    }

    static Sound sound(String... names) {
        for (String name : names) {
            try {
                // Sound became an interface in modern Bukkit; do not use Enum.valueOf here.
                return Sound.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
