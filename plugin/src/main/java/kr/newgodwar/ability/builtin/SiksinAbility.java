package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@AbilityInfo(
    id = "siksin",
    name = "식신",
    description = "신성한 음식을 만들어 자신과 팀원을 강화합니다.",
    normalSkill = "식신만 버프를 얻는 음식을 생성합니다. 실패하면 기본 빵을 생성합니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 45,
    advancedSkill = "팀원도 먹을 수 있는 음식을 생성합니다. 팀원이 먹으면 식신도 같은 버프를 얻습니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 90,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class SiksinAbility extends BaseAbility {
    private static final String MARKER = "NGW_SIKSIN_FOOD";
    private static final int SUCCESS_PERCENT = 75;

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
        FoodData data = foodData(event.getItem());
        if (data == null || !context.player().getUniqueId().equals(data.ownerId)) {
            return;
        }

        Player owner = context.player();
        Player eater = event.getPlayer();
        if (data.teamFood) {
            if (!owner.equals(eater) && !sameTeam(context, owner, eater)) {
                return;
            }
            applyBuff(eater, data.buff);
            if (!owner.equals(eater)) {
                applyBuff(owner, data.buff);
                owner.sendMessage(ChatColor.GOLD + eater.getName() + " 님이 식신의 음식을 먹어 같은 버프를 얻었습니다.");
            }
            return;
        }

        if (owner.equals(eater)) {
            applyBuff(owner, data.buff);
        }
    }

    private void giveCreatedFood(Player player, boolean teamFood) {
        if (!rollPercent(SUCCESS_PERCENT)) {
            give(player, Material.BREAD, 1);
            player.sendMessage(ChatColor.YELLOW + "음식 생성에 실패해 기본 빵이 만들어졌습니다.");
            return;
        }
        player.getInventory().addItem(createFood(player, teamFood, randomBuff()));
    }

    private ItemStack createFood(Player owner, boolean teamFood, BuffKind buff) {
        ItemStack stack = new ItemStack(randomFoodMaterial(), 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((teamFood ? ChatColor.AQUA : ChatColor.GOLD)
                + (teamFood ? "나눔의 성찬" : "식신의 성찬"));
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + buff.label + " 버프를 품고 있습니다.",
                ChatColor.DARK_GRAY + MARKER + ":" + owner.getUniqueId().toString() + ":" + (teamFood ? "TEAM" : "SOLO") + ":" + buff.id
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private FoodData foodData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            return null;
        }
        for (String line : lore) {
            String stripped = ChatColor.stripColor(line);
            if (stripped == null || !stripped.startsWith(MARKER + ":")) {
                continue;
            }
            String[] parts = stripped.split(":");
            if (parts.length != 4) {
                return null;
            }
            try {
                UUID ownerId = UUID.fromString(parts[1]);
                BuffKind buff = BuffKind.byId(parts[3]);
                if (buff == null) {
                    return null;
                }
                return new FoodData(ownerId, "TEAM".equals(parts[2]), buff);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }

    private Material randomFoodMaterial() {
        Material[] foods = new Material[] {
            Material.BREAD,
            material("COOKED_BEEF", "COOKED_BEEF"),
            material("COOKED_PORKCHOP", "GRILLED_PORK"),
            material("COOKED_CHICKEN", "COOKED_CHICKEN"),
            material("BAKED_POTATO", "BAKED_POTATO"),
            material("COOKED_COD", "COOKED_FISH")
        };
        Material food = foods[RANDOM.nextInt(foods.length)];
        return food == null || food == Material.AIR ? Material.BREAD : food;
    }

    private BuffKind randomBuff() {
        BuffKind[] buffs = BuffKind.values();
        return buffs[RANDOM.nextInt(buffs.length)];
    }

    private void applyBuff(Player player, BuffKind buff) {
        if ("STRENGTH".equals(buff.id)) {
            effect(player, "STRENGTH", "INCREASE_DAMAGE", buff.seconds, buff.amplifier);
        } else if ("RESISTANCE".equals(buff.id)) {
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", buff.seconds, buff.amplifier);
        } else if ("HASTE".equals(buff.id)) {
            effect(player, "HASTE", "FAST_DIGGING", buff.seconds, buff.amplifier);
        } else {
            effect(player, buff.type, buff.seconds, buff.amplifier);
        }
    }

    private enum BuffKind {
        SPEED("SPEED", "신속", PotionEffectType.SPEED, 18, 0),
        REGENERATION("REGENERATION", "재생", PotionEffectType.REGENERATION, 12, 0),
        STRENGTH("STRENGTH", "공격력 증가", null, 12, 0),
        RESISTANCE("RESISTANCE", "저항", null, 12, 0),
        HASTE("HASTE", "성급함", null, 18, 0),
        JUMP("JUMP", "점프 강화", PotionEffectType.JUMP, 18, 0);

        private final String id;
        private final String label;
        private final PotionEffectType type;
        private final int seconds;
        private final int amplifier;

        BuffKind(String id, String label, PotionEffectType type, int seconds, int amplifier) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.seconds = seconds;
            this.amplifier = amplifier;
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

    private static final class FoodData {
        private final UUID ownerId;
        private final boolean teamFood;
        private final BuffKind buff;

        private FoodData(UUID ownerId, boolean teamFood, BuffKind buff) {
            this.ownerId = ownerId;
            this.teamFood = teamFood;
            this.buff = buff;
        }
    }
}
