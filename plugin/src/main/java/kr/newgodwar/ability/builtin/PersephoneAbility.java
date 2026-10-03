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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

@AbilityInfo(
    id = "persephone",
    name = "페르세포네",
    description = "봄의 회복과 저승의 뿌리로 전장을 보조합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 18블록 안에서 바라보는 적에게 8초 동안 감속 IV와 약화를 줍니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 55,
    advancedSkill = "블레이즈 막대기 우클릭: 자신과 반경 7블록 아군의 체력을 4 회복하고 8초 재생을 줍니다.",
    advancedStoneCost = 22,
    advancedCooldownSeconds = 115,
    passiveSkill = "피해를 받아 체력이 8(하트 4칸) 이하가 되면 30% 확률로 8초 재생을 얻습니다. 발동 후 60초 동안은 다시 발동하지 않습니다.",
    grade = AbilityGrade.A
)
final class PersephoneAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .hit(EffectCue.ROOT)
        .benefit(EffectCue.HEAL)
        .effect(EffectCue.ROOT, AbilityDesigns.UNDERWORLD_ROOTS)
        .effect(EffectCue.HEAL, AbilityDesigns.POMEGRANATE)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private long bloomReadyAt;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 18, false);
        if (target == null) {
            return;
        }
        if (useNormal(context, player)) {
            effect(context, target, "SLOWNESS", "SLOW", 8, 3);
            effect(context, target, PotionEffectType.WEAKNESS, 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 7, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() + 4.0D));
                effect(context, target, PotionEffectType.REGENERATION, 8, 0);
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        Player player = context.player();
        double remaining = player.getHealth() - event.getFinalDamage();
        // Lethal hits cannot be saved by regeneration, so only react to surviving at low health.
        if (now < bloomReadyAt || event.isCancelled() || remaining <= 0.0D || remaining > 8.0D) {
            return;
        }
        if (rollChance(3, 10)) {
            bloomReadyAt = now + context.plugin().abilities().scaleCooldownMillis(60 * 1000L);
            effect(context, player, PotionEffectType.REGENERATION, 8, 0);
            player.sendMessage(ChatColor.GREEN + "페르세포네의 봄기운이 잠시 피어납니다.");
        }
    }
}
