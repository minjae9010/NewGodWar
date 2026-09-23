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
    MUSIC(ChatColor.AQUA, "NOTE", "NOTE", "BLOCK_NOTE_BLOCK_HARP", "BLOCK_NOTE_HARP"),
    GRAVITY(ChatColor.DARK_PURPLE, "PORTAL", "PORTAL", "BLOCK_BEACON_ACTIVATE", "BLOCK_END_PORTAL_SPAWN"),
    ECHO(ChatColor.AQUA, "SWEEP_ATTACK", "SWEEP_ATTACK", "ENTITY_PLAYER_ATTACK_SWEEP", "ENTITY_PLAYER_ATTACK_SWEEP"),
    RUNE(ChatColor.GOLD, "ENCHANT", "ENCHANTMENT_TABLE", "BLOCK_ENCHANTMENT_TABLE_USE", "BLOCK_ENCHANTMENT_TABLE_USE"),
    HUNT(ChatColor.GREEN, "CRIT", "CRIT", "ENTITY_ARROW_HIT_PLAYER", "ENTITY_ARROW_HIT_PLAYER"),
    TIME(ChatColor.AQUA, "END_ROD", "END_ROD", "BLOCK_NOTE_BLOCK_HAT", "BLOCK_NOTE_HAT"),
    SWARM(ChatColor.GOLD, "CRIT", "CRIT", "ENTITY_BEE_LOOP", "ENTITY_BAT_TAKEOFF");

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

    public static <T extends Enum<T>> T resolve(Class<T> type, String... names) {
        for (String name : names) {
            try {
                return Enum.valueOf(type, name);
            } catch (IllegalArgumentException ignored) {
                // Cosmetic features must also work on the oldest supported server.
            }
        }
        return null;
    }

    public static Sound sound(String... names) {
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
