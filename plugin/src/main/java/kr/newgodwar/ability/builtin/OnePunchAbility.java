package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@AbilityInfo(
    id = "onepunch",
    name = "원펀치",
    description = "맨손 한 방을 크게 강화하고 짧은 돌진으로 거리를 좁힙니다.",
    normalSkill = "8초 안의 다음 맨손 공격에 추가 피해와 큰 밀침을 부여합니다.",
    normalStoneCost = 24,
    normalCooldownSeconds = 95,
    advancedSkill = "앞으로 도약하고 5초 동안 공격력 증가를 얻습니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 55,
    passiveSkill = "맨손 공격 시 적을 추가로 밀쳐냅니다.",
    grade = AbilityGrade.A
)
final class OnePunchAbility extends BaseAbility {
    private boolean punchReady;

    @Override
    protected void onStaffLeft(final AbilityPlayerContext context, final Player player, PlayerInteractEvent event) {
        if (punchReady) {
            player.sendMessage(ChatColor.YELLOW + "이미 한 방이 준비되어 있습니다.");
            return;
        }
        if (useNormal(context, player)) {
            punchReady = true;
            player.sendMessage(ChatColor.RED + "다음 맨손 공격이 한 방으로 강화됩니다.");
            later(context, 8, "원펀치 준비 해제", "원펀치 준비 해제", () -> punchReady = false);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            dash(player);
            effect(player, "STRENGTH", "INCREASE_DAMAGE", 5, 0);
        }
    }

    private void dash(Player player) {
        Vector vector = player.getEyeLocation().getDirection();
        vector.setY(0.5D);
        player.setVelocity(vector);
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 1);
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        Player player = context.player();
        if (!attacker || player.getItemInHand().getType() != Material.AIR) {
            return;
        }
        Vector vector = player.getEyeLocation().getDirection().setY(0.35D);
        if (punchReady) {
            punchReady = false;
            event.setDamage(event.getDamage() + 14.0D);
            opponent.setVelocity(vector.normalize().multiply(1.9D));
            effect(player, PotionEffectType.WEAKNESS, 6, 0);
            player.sendMessage(ChatColor.RED + "원펀치!");
        } else {
            opponent.setVelocity(vector.normalize().multiply(0.7D));
        }
    }
}
