package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;
import kr.newgodwar.game.GodTeam;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
@AbilityInfo(
    id = "harry",
    name = "해리포터",
    description = "채팅 주문으로 시간 변경, 폭발, 보호, 무장 해제, 즉사 주문을 사용합니다.",
    normalSkill = "채팅에 루모스(낮), 녹스(밤), 봄바르다(5블록 앞 위력 1 폭발)를 입력합니다.",
    normalStoneCost = 6,
    normalCooldownSeconds = 8,
    advancedSkill = "채팅에 스투페파이(반경 10블록 적마다 50% 확률로 감속 II 10초), 익스펙토 패트로눔(75% 확률로 5초 무적), 엑스펠리아무스(바라보는 적 25% 확률 무장 해제), 아바다 케다브라(바라보는 적 20% 확률 즉사)를 입력합니다. 실패해도 비용과 쿨타임이 적용됩니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 60,
    passiveSkill = "능력을 받으면 주문서를 받습니다. 보호 주문 동안 모든 피해를 무시합니다. 일반 주문끼리, 고급 주문끼리 쿨타임을 공유합니다.",
    grade = AbilityGrade.S
)
final class HarryAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.ARCANE)
        .normal(EffectCue.ARCANE)
        .advanced(EffectCue.ARCANE)
        .hit(EffectCue.HIT)
        .effect(EffectCue.ARCANE, AbilityDesigns.SPELL_PAGES)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private boolean invincible;

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        giveSpellBook(context.player(), true);
    }

    @Override
    public void onChatMessage(AbilityPlayerContext context, String message) {
        castSpell(context, message, true);
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (invincible) {
            event.setCancelled(true);
        }
    }

    private void giveSpellBook(Player player, boolean harry) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("마법 스펠 암기장");
        meta.setAuthor(harry ? "해리 포터" : "헤르미온느 진 그레인저");
        meta.addPage("루모스/Lumos\n녹스/Nox\n봄바르다/Bombarda");
        meta.addPage("스투페파이/Stupefy\n익스펙토 패트로눔/Expecto Patronum");
        meta.addPage("엑스펠리아무스/Expelliarmus\n아바다 케다브라/Avada Kedavra");
        book.setItemMeta(meta);
        give(player, book);
    }

    private void castSpell(AbilityPlayerContext context, String spell, boolean harry) {
        final Player player = context.player();
        spell = normalizeSpell(spell);
        if (spell.equals("루모스") || spell.equalsIgnoreCase("Lumos")) {
            if (useNormal(context, player)) {
                setWorldTime(context, player, 1000);
            }
        } else if (spell.equals("녹스") || spell.equalsIgnoreCase("Nox")) {
            if (useNormal(context, player)) {
                setWorldTime(context, player, 18000);
            }
        } else if (spell.equals("봄바르다") || spell.equalsIgnoreCase("Bombarda")) {
            if (useNormal(context, player)) {
                createExplosion(context, player, targetLocation(player, 5), 1.0F, false, true);
            }
        } else if (spell.equals("스투페파이") || spell.equalsIgnoreCase("Stupefy")) {
            List<Player> targets = nearbyPlayers(context, player, 10, false);
            if (targets.isEmpty()) {
                sendAbilityMessage(context, player, "failure", "능력을 사용할 수 있는 대상이 없습니다.");
                return;
            }
            if (useAdvanced(context, player)) {
                for (Player target : targets) {
                    if (RANDOM.nextBoolean()) {
                        effect(context, target, "SLOWNESS", "SLOW", 10, harry ? 1 : 2);
                    }
                }
            }
        } else if (spell.equals("익스펙토 패트로눔") || spell.equalsIgnoreCase("Expecto Patronum")) {
            if (useAdvanced(context, player) && rollChance(harry ? 3 : 2, 4)) {
                invincible = true;
                feedback.cue(context, player, kr.newgodwar.ability.feedback.EffectCue.GUARD);
                laterCleanup(context, 5, "보호 주문 종료", "보호 주문 종료", () -> invincible = false);
            }
        } else if (spell.equals("엑스펠리아무스") || spell.equalsIgnoreCase("Expelliarmus")) {
            Player target = targetPlayerInSight(context, player, 20, false);
            if (target != null && useAdvanced(context, player) && rollPercent(harry ? 25 : 20)) {
                boolean equipped = hasEquipment(target);
                dropHeldAndArmor(target);
                if (equipped) feedback.cue(context, target, kr.newgodwar.ability.feedback.EffectCue.ITEM);
            }
        } else if (spell.equals("아바다 케다브라") || spell.equalsIgnoreCase("Avada Kedavra")) {
            Player target = targetPlayerInSight(context, player, 20, false);
            if (target != null && useAdvanced(context, player) && rollPercent(harry ? 20 : 15)) {
                lethalDamage(context, target, player);
            }
        }
    }

    private boolean hasEquipment(Player target) {
        ItemStack held=target.getItemInHand();
        if(held!=null && held.getType()!=Material.AIR && held.getAmount()>0) return true;
        for(ItemStack armor:target.getInventory().getArmorContents())
            if(armor!=null && armor.getType()!=Material.AIR && armor.getAmount()>0) return true;
        return false;
    }

    private String normalizeSpell(String spell) {
        if (spell == null) {
            return "";
        }
        String normalized = ChatColor.stripColor(spell).trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }
}
