package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AbilityInfo(
    id = "siksin",
    name = "식신",
    description = "신성한 음식을 만들어 자신과 팀원을 강화합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 75% 확률로 자신 전용 음식을 만듭니다. 음식마다 다른 버프를 얻고 실패하면 일반 빵이 나옵니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 45,
    advancedSkill = "블레이즈 막대기 우클릭: 75% 확률로 나눔 음식을 만듭니다. 같은 월드의 살아 있는 팀원이 먹으면 먹은 사람과 식신만 같은 버프를 얻습니다. 거리 제한은 없습니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 90,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class SiksinAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .normal(EffectCue.ITEM)
        .advanced(EffectCue.ITEM)
        .effect(EffectCue.ITEM, AbilityDesigns.FEAST)
        .received(EffectCue.CHARGE, AbilityDesigns.MUSCLE)
        .received(EffectCue.WIND, AbilityDesigns.CLOUD_STEP)
        .received(EffectCue.GUARD, AbilityDesigns.AEGIS_PLATES)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private static final String MARKER = "NGW_SIKSIN_FOOD";
    private static final int SUCCESS_PERCENT = 75;
    private UUID foodSession = UUID.randomUUID();

    @Override
    public void saveSession(org.bukkit.configuration.ConfigurationSection data) {
        super.saveSession(data);
        data.set("food-session", foodSession.toString());
    }

    @Override
    public void loadSession(org.bukkit.configuration.ConfigurationSection data) {
        super.loadSession(data);
        try {
            foodSession = UUID.fromString(data.getString("food-session", ""));
        } catch (IllegalArgumentException invalid) {
            foodSession = UUID.randomUUID();
        }
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        giveCreatedFood(player, false);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useAdvanced(context, player)) {
            return;
        }
        giveCreatedFood(player, true);
    }

    @Override
    public void onItemConsume(AbilityPlayerContext context, PlayerItemConsumeEvent event) {
        final FoodData data = foodData(event.getItem());
        if (event.isCancelled() || data == null || !context.player().getUniqueId().equals(data.ownerId)
            || !foodSession.equals(data.sessionId)) {
            return;
        }
        final Player owner = context.player();
        final Player eater = event.getPlayer();
        if (!canEat(context, owner, eater, data.teamFood)) return;
        // Consume listeners can still cancel after this callback; only confirm the meal next tick.
        scheduleLater(context, () -> {
            if (event.isCancelled() || !canEat(context, owner, eater, data.teamFood)) return;
            FoodData consumed = foodData(event.getItem());
            if (consumed == null || !data.ownerId.equals(consumed.ownerId) || !data.sessionId.equals(consumed.sessionId)
                || data.buff != consumed.buff || data.teamFood != consumed.teamFood) return;
            kr.newgodwar.ability.AbilitySession active = context.plugin().abilities().session(owner);
            if (active == null || active.ability() != this || context.plugin().abilities().isAbilitySuppressed(owner)) {
                return;
            }
            applyBuff(context, eater, data.buff);
            if (data.teamFood && !owner.getUniqueId().equals(eater.getUniqueId())) {
                applyBuff(context, owner, data.buff);
            }
        }, 1L);
    }

    private boolean canEat(AbilityPlayerContext context, Player owner, Player eater, boolean teamFood) {
        if (!eligible(context, owner) || !eligible(context, eater) || !owner.getWorld().equals(eater.getWorld())) return false;
        return owner.getUniqueId().equals(eater.getUniqueId()) || (teamFood && sameTeam(context, owner, eater));
    }

    private boolean eligible(AbilityPlayerContext context, Player player) {
        return player != null && player.isOnline() && !player.isDead() && context.plugin().game().canUseAbility(player);
    }

    private void giveCreatedFood(Player player, boolean teamFood) {
        if (!rollPercent(SUCCESS_PERCENT)) {
            give(player, Material.BREAD, 1);
            player.sendMessage(ChatColor.YELLOW + "음식 생성에 실패해 기본 빵이 만들어졌습니다.");
            return;
        }
        BuffKind buff = randomBuff();
        give(player, createFood(player, teamFood, buff));
        player.sendMessage(ChatColor.GOLD + "✦ " + buff.foodName + ChatColor.WHITE + " 완성! "
            + ChatColor.GRAY + buff.label + " I · " + buff.seconds + "초 · " + (teamFood ? "자신 또는 같은 월드 팀원용" : "자신 전용"));
    }

    private ItemStack createFood(Player owner, boolean teamFood, BuffKind buff) {
        ItemStack stack = new ItemStack(material(buff.material, buff.legacyMaterial), 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((teamFood ? ChatColor.AQUA : ChatColor.GOLD)
                + buff.foodName + (teamFood ? ChatColor.AQUA + " · 나눔" : ""));
            List<String> lore = new ArrayList<String>(Arrays.asList(
                ChatColor.WHITE + buff.label + " I " + ChatColor.GRAY + "· " + buff.seconds + "초",
                ChatColor.GRAY + "요리사: " + ChatColor.WHITE + owner.getName(),
                ChatColor.GRAY + (teamFood ? "대상: 요리사 또는 같은 월드의 살아 있는 팀원" : "대상: 요리사 자신만"),
                ChatColor.GRAY + (teamFood ? "팀원이 먹으면 두 사람에게 적용 · 거리 제한 없음" : "다른 사람이 먹으면 일반 음식으로만 소비돼요."),
                ChatColor.DARK_GRAY + "더 강하거나 오래 남은 같은 효과는 유지돼요.",
                ChatColor.DARK_GRAY + "요리사의 현재 능력에 연결된 음식이에요."
            ));
            String marker = marker(owner.getUniqueId(), teamFood, buff);
            if (!storeMarker(meta, marker)) lore.add(ChatColor.DARK_GRAY + marker);
            meta.setLore(lore);
            applyFoodModel(meta, buff.modelId);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private FoodData foodData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String stored = storedMarker(meta);
        if (stored != null) return validatedFood(item, stored);
        if (!meta.hasLore()) return null;
        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }
        for (String line : lore) {
            String stripped = ChatColor.stripColor(line);
            if (stripped == null || !stripped.startsWith(MARKER + ":")) {
                continue;
            }
            return validatedFood(item, stripped);
        }
        return null;
    }

    private FoodData validatedFood(ItemStack item, String marker) {
        FoodData data = parseMarker(marker);
        return data == null || item.getType() != material(data.buff.material, data.buff.legacyMaterial) ? null : data;
    }

    private boolean storeMarker(ItemMeta meta, String marker) {
        try {
            Class<?> keyType = Class.forName("org.bukkit.NamespacedKey");
            Class<?> valueType = Class.forName("org.bukkit.persistence.PersistentDataType");
            Class<?> containerType = Class.forName("org.bukkit.persistence.PersistentDataContainer");
            Object key = keyType.getConstructor(String.class, String.class).newInstance("newgodwar", "siksin_food");
            Object container = ItemMeta.class.getMethod("getPersistentDataContainer").invoke(meta);
            containerType.getMethod("set", keyType, valueType, Object.class)
                .invoke(container, key, valueType.getField("STRING").get(null), marker);
            return true;
        } catch (ReflectiveOperationException legacy) {
            return false;
        }
    }

    private String storedMarker(ItemMeta meta) {
        try {
            Class<?> keyType = Class.forName("org.bukkit.NamespacedKey");
            Class<?> valueType = Class.forName("org.bukkit.persistence.PersistentDataType");
            Class<?> containerType = Class.forName("org.bukkit.persistence.PersistentDataContainer");
            Object key = keyType.getConstructor(String.class, String.class).newInstance("newgodwar", "siksin_food");
            Object container = ItemMeta.class.getMethod("getPersistentDataContainer").invoke(meta);
            return (String) containerType.getMethod("get", keyType, valueType)
                .invoke(container, key, valueType.getField("STRING").get(null));
        } catch (ReflectiveOperationException legacy) {
            return null;
        }
    }

    String marker(UUID ownerId, boolean teamFood, BuffKind buff) {
        return MARKER + ":" + ownerId + ":" + (teamFood ? "TEAM" : "SOLO") + ":" + buff.id + ":" + foodSession;
    }

    static FoodData parseMarker(String marker) {
        if (marker == null) return null;
        String[] parts = marker.split(":", -1);
        if (parts.length != 5 || !MARKER.equals(parts[0]) || !("TEAM".equals(parts[2]) || "SOLO".equals(parts[2]))) return null;
        try {
            BuffKind buff = BuffKind.byId(parts[3]);
            if (buff == null) return null;
            UUID ownerId = UUID.fromString(parts[1]);
            UUID sessionId = UUID.fromString(parts[4]);
            if (!ownerId.toString().equals(parts[1]) || !sessionId.toString().equals(parts[4])) return null;
            return new FoodData(ownerId, "TEAM".equals(parts[2]), buff, sessionId);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private void applyFoodModel(ItemMeta meta, int modelId) {
        try {
            // Legacy custom model data keeps ordinary food rendering when the optional pack is absent.
            ItemMeta.class.getMethod("setCustomModelData", Integer.class).invoke(meta, Integer.valueOf(modelId));
        } catch (ReflectiveOperationException unsupported) {
            // 1.12 has no custom model metadata; the normal edible material remains intact.
        }
    }

    private BuffKind randomBuff() {
        BuffKind[] buffs = BuffKind.values();
        return buffs[RANDOM.nextInt(buffs.length)];
    }

    private void applyBuff(AbilityPlayerContext context, Player player, BuffKind buff) {
        PotionEffectType type = buff.type != null ? buff.type : effectType(buff.id, buff.legacyEffect);
        if (type == null) return;
        PotionEffect before = player.getPotionEffect(type);
        effect(context, player, type, buff.seconds, buff.amplifier);
        PotionEffect after = player.getPotionEffect(type);
        boolean applied = after != null && (before == null || after.getAmplifier() > before.getAmplifier()
            || (after.getAmplifier() == before.getAmplifier() && after.getDuration() > before.getDuration()));
        if (applied && buff == BuffKind.HASTE) feedback.cue(context, player, EffectCue.HASTE);
        player.sendMessage((applied ? ChatColor.GOLD + "✦ " : ChatColor.GRAY + "• ") + buff.foodName + ChatColor.WHITE
            + (applied ? " · " + buff.label + " I " + buff.seconds + "초"
                : after != null ? " · 기존 " + buff.label + " 효과 유지" : " · " + buff.label + " 효과가 적용되지 않았어요."));
    }

    enum BuffKind {
        SPEED("SPEED", "신속", PotionEffectType.SPEED, "SPEED", 18, "바람결 크루아상", "BREAD", "BREAD", 73101),
        REGENERATION("REGENERATION", "재생", PotionEffectType.REGENERATION, "REGENERATION", 12, "생명의 허브구이", "COOKED_CHICKEN", "COOKED_CHICKEN", 73102),
        STRENGTH("STRENGTH", "공격력 증가", null, "INCREASE_DAMAGE", 12, "용사의 불꽃 스테이크", "COOKED_BEEF", "COOKED_BEEF", 73103),
        RESISTANCE("RESISTANCE", "저항", null, "DAMAGE_RESISTANCE", 12, "수호의 황금 감자", "BAKED_POTATO", "BAKED_POTATO", 73104),
        HASTE("HASTE", "성급함", null, "FAST_DIGGING", 18, "장인의 꿀 바비큐", "COOKED_PORKCHOP", "GRILLED_PORK", 73105),
        JUMP("JUMP", "점프 강화", PotionEffectType.JUMP, "JUMP", 18, "구름 도약 생선구이", "COOKED_COD", "COOKED_FISH", 73106);

        private final String id;
        private final String label;
        private final PotionEffectType type;
        private final String legacyEffect;
        final int seconds;
        private final int amplifier = 0;
        final String foodName;
        final String material;
        final String legacyMaterial;
        final int modelId;

        BuffKind(String id, String label, PotionEffectType type, String legacyEffect, int seconds,
                 String foodName, String material, String legacyMaterial, int modelId) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.seconds = seconds;
            this.legacyEffect = legacyEffect;
            this.foodName = foodName;
            this.material = material;
            this.legacyMaterial = legacyMaterial;
            this.modelId = modelId;
        }

        private static BuffKind byId(String id) {
            for (BuffKind buff : values()) {
                if (buff.id.equals(id)) {
                    return buff;
                }
            }
            return null;
        }
    }

    static final class FoodData {
        final UUID ownerId;
        final boolean teamFood;
        final BuffKind buff;
        final UUID sessionId;

        private FoodData(UUID ownerId, boolean teamFood, BuffKind buff, UUID sessionId) {
            this.ownerId = ownerId;
            this.teamFood = teamFood;
            this.buff = buff;
            this.sessionId = sessionId;
        }
    }
}
