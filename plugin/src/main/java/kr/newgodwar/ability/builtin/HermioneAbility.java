package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
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
    id = "hermione",
    name = "헤르미온느",
    description = "채팅 주문으로 시간 변경, 폭발, 보호, 무장 해제, 즉사 주문을 사용합니다.",
    normalSkill = "채팅으로 루모스, 녹스, 봄바르다를 시전합니다.",
    normalStoneCost = 6,
    normalCooldownSeconds = 8,
    advancedSkill = "채팅으로 고급 주문을 시전합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 50,
    passiveSkill = "배정 시 주문서를 받고 보호 주문 중 모든 피해를 무시합니다.",
    grade = AbilityGrade.S
)
final class HermioneAbility extends BaseAbility {
    private boolean invincible;

    @Override
    public void onAssign(AbilityPlayerContext context) {
        giveSpellBook(context.player(), false);
    }

    @Override
    public void onChatMessage(AbilityPlayerContext context, String message) {
        castSpell(context, message, false);
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
        player.getInventory().addItem(book);
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
                player.getWorld().createExplosion(targetLocation(player, 5), 1.0F);
            }
        } else if (spell.equals("스투페파이") || spell.equalsIgnoreCase("Stupefy")) {
            if (useAdvanced(context, player)) {
                for (Player target : nearbyPlayers(context, player, 10, false)) {
                    if (RANDOM.nextBoolean()) {
                        effect(target, "SLOWNESS", "SLOW", 10, harry ? 1 : 2);
                    }
                }
            }
        } else if (spell.equals("익스펙토 패트로눔") || spell.equalsIgnoreCase("Expecto Patronum")) {
            if (useAdvanced(context, player) && rollChance(harry ? 3 : 2, 4)) {
                invincible = true;
                later(context, 5, "보호 주문 종료", "보호 주문 종료", () -> invincible = false);
            }
        } else if (spell.equals("엑스펠리아무스") || spell.equalsIgnoreCase("Expelliarmus")) {
            Player target = targetPlayerInSight(context, player, 20, false);
            if (target != null && useAdvanced(context, player) && rollPercent(harry ? 25 : 20)) {
                dropHeldAndArmor(target);
            }
        } else if (spell.equals("아바다 케다브라") || spell.equalsIgnoreCase("Avada Kedavra")) {
            Player target = targetPlayerInSight(context, player, 20, false);
            if (target != null && useAdvanced(context, player) && rollPercent(harry ? 20 : 15)) {
                target.setHealth(0.0D);
            }
        }
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
