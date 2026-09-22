package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@AbilityInfo(
    id = "hermione", name = "헤르미온느",
    description = "정교한 주문으로 적을 띄우고 장비를 수리하며 보호막으로 아군을 지킵니다.",
    normalSkill = "채팅에 윙가르디움 레비오사(16블록 적 부양 2초), 레파로(자신과 6블록 아군의 주 손 장비·착용 방어구 내구도 40 수리), 피니테(같은 범위의 독·위더·감속·실명 해제)를 입력합니다.",
    normalStoneCost = 10, normalCooldownSeconds = 22,
    advancedSkill = "채팅에 프로테고를 입력합니다. 현재 위치에 6초 동안 반경 5블록 보호 마법진을 만들고 안에 있는 아군에게 저항 II를 갱신합니다.",
    advancedStoneCost = 26, advancedCooldownSeconds = 90,
    passiveSkill = "지원 주문서를 지급받습니다. 일반 주문은 하나의 쿨타임을 공유합니다.",
    grade = AbilityGrade.A
)
final class HermioneAbility extends TransientAbility {
    private boolean shieldActive;

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("헤르미온느의 지원 주문");
        meta.setAuthor("헤르미온느");
        meta.addPage("일반: 돌 10 / 22초 공유\n윙가르디움 레비오사\nWingardium Leviosa\n16블록 적 부양 2초\n\n레파로 / Reparo\n자신과 6블록 아군의\n주 손 장비·방어구 40 수리");
        meta.addPage("피니테 / Finite\n자신과 6블록 아군의\n독, 위더, 감속, 실명 해제\n\n프로테고 / Protego\n돌 26 / 90초\n5블록 보호진 6초\n진 안의 아군에게 저항 II");
        book.setItemMeta(meta);
        give(context.player(), book);
    }

    @Override
    public void onChatMessage(AbilityPlayerContext context, String message) {
        if (message == null || !active(context)) return;
        String spell = ChatColor.stripColor(message).trim().replaceFirst("^/", "")
            .replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        Player player = context.player();
        if ("윙가르디움레비오사".equals(spell) || "wingardiumleviosa".equals(spell)) {
            Player target = targetPlayerInSight(context, player, 16, false);
            if (target == null || !useNormal(context, player)) return;
            effect(context, target, "LEVITATION", "LEVITATION", 2, 0);
            feedback.spiral(context, target.getLocation(), 0.8D);
            feedback.affected(context, target, "부양 주문 · 2초", true);
        } else if ("레파로".equals(spell) || "reparo".equals(spell)) {
            List<Player> recipients = allies(context, player.getLocation(), 6);
            recipients.removeIf(ally -> !hasDamagedGear(ally));
            if (recipients.isEmpty()) {
                sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "주변에 수리할 주 손 장비나 착용 방어구가 없습니다.");
                return;
            }
            if (!useNormal(context, player)) return;
            for (Player ally : recipients) {
                repairGear(ally);
                feedback.cue(context, ally, kr.newgodwar.ability.feedback.EffectCue.FORGE);
                feedback.affected(context, ally, "레파로 · 장비 내구도 40 수리", false);
            }
        } else if ("피니테".equals(spell) || "finite".equals(spell)) {
            if (!useNormal(context, player)) return;
            for (Player ally : allies(context, player.getLocation(), 6)) {
                ally.removePotionEffect(PotionEffectType.POISON);
                ally.removePotionEffect(PotionEffectType.WITHER);
                ally.removePotionEffect(PotionEffectType.BLINDNESS);
                removeEffect(ally, "SLOWNESS", "SLOW");
                feedback.cue(context, ally, kr.newgodwar.ability.feedback.EffectCue.CLEANSE);
                feedback.affected(context, ally, "피니테 · 상태 이상 해제", false);
            }
        } else if ("프로테고".equals(spell) || "protego".equals(spell)) {
            if (shieldActive) {
                sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "이미 보호 마법진이 유지 중입니다.");
                return;
            }
            if (!useAdvanced(context, player)) return;
            shieldActive = true;
            final Location center = player.getLocation();
            for (int i = 0; i < 6; i++) {
                scheduleLater(context, () -> {
                    if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
                    feedback.shield(context, center, 5);
                    for (Player ally : allies(context, center, 5)) {
                        effectTicks(ally, "RESISTANCE", "DAMAGE_RESISTANCE", 20, 1);
                    }
                }, i * 20L);
            }
            scheduleLater(context, () -> shieldActive = false, 120L);
        }
    }

    @Override
    protected void clearTransientState() { shieldActive = false; }

    private boolean damaged(ItemStack item) {
        return item != null && item.getType().getMaxDurability() > 0 && item.getDurability() > 0;
    }

    private boolean hasDamagedGear(Player player) {
        if (damaged(player.getItemInHand())) return true;
        for (ItemStack armor : player.getInventory().getArmorContents()) if (damaged(armor)) return true;
        return false;
    }

    private ItemStack repaired(ItemStack item) {
        ItemStack result = item.clone();
        result.setDurability((short) Math.max(0, result.getDurability() - 40));
        return result;
    }

    private void repairGear(Player player) {
        if (damaged(player.getItemInHand())) player.setItemInHand(repaired(player.getItemInHand()));
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            if (damaged(armor[i])) { armor[i] = repaired(armor[i]); changed = true; }
        }
        if (changed) player.getInventory().setArmorContents(armor);
    }

    private List<Player> allies(AbilityPlayerContext context, Location center, double radius) {
        List<Player> result = new ArrayList<Player>();
        for (Player ally : alliedPlayers(context, context.player(), true)) {
            if (!ally.isDead() && center.getWorld().equals(ally.getWorld())
                && ally.getLocation().distanceSquared(center) <= radius * radius) result.add(ally);
        }
        return result;
    }
}
