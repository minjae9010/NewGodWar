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
    id = "snow",
    name = "사이코스노우",
    description = "눈덩이로 적에게 피해를 주며 공격 지수를 성장시킵니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 눈덩이를 1개 만듭니다.",
    normalStoneCost = 2,
    normalCooldownSeconds = 3,
    advancedSkill = "블레이즈 막대기 우클릭: 현재 공격 지수를 확인합니다.",
    advancedStoneCost = 0,
    passiveSkill = "능력을 받으면 눈덩이 8개를 받습니다. 공격 지수는 1부터 시작하며 눈덩이로 적의 체력을 4회 깎을 때마다 1씩 올라 최대 5가 됩니다. 성장 적중은 1초에 한 번 집계하고 사망해도 성장은 유지됩니다. 화염 피해를 2배로 받습니다.",
    grade = AbilityGrade.A
)
final class SnowAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.FROST)
        .normal(EffectCue.ITEM)
        .hit(EffectCue.FROST)
        .effect(EffectCue.ITEM, AbilityDesigns.SNOWFLAKE)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private int snowAttack = 1;
    private int growthHits;
    private long nextGrowthAt;

    @Override
    public void saveSession(org.bukkit.configuration.ConfigurationSection data) {
        super.saveSession(data);
        data.set("snow-attack", snowAttack);
        data.set("snow-growth-hits", growthHits);
    }

    @Override
    public void loadSession(org.bukkit.configuration.ConfigurationSection data) {
        super.loadSession(data);
        snowAttack = Math.max(1, Math.min(5, data.getInt("snow-attack", 1)));
        growthHits = snowAttack == 5 ? 0 : Math.max(0, Math.min(3, data.getInt("snow-growth-hits", 0)));
        nextGrowthAt = 0;
    }

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        give(context.player(), material("SNOWBALL", "SNOW_BALL"), 8);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, material("SNOWBALL", "SNOW_BALL"), 1);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        player.sendMessage("공격 지수 : " + snowAttack);
        if (snowAttack < 5) player.sendMessage("성장 적중 : " + growthHits + "/4");
    }

    @Override
    public void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {
        if (event.isCancelled()) return;
        Projectile projectile = (Projectile) event.getDamager();
        if (projectile.getType().name().contains("SNOW")) {
            event.setCancelled(true);
            double before = victim.getHealth();
            damage(context, victim, snowAttack, context.player());
            long now = System.currentTimeMillis();
            if (victim.getHealth() < before && snowAttack < 5 && now >= nextGrowthAt) {
                nextGrowthAt = now + 1000L;
                growthHits++;
                if (growthHits == 4) {
                    growthHits = 0;
                    snowAttack++;
                    context.player().sendMessage("공격 지수가 " + snowAttack + "(으)로 증가했습니다. (최대 5)");
                }
                feedback.progress(context, "눈덩이 공격 " + snowAttack + (snowAttack < 5 ? " · 성장 " + growthHits + "/4" : " · 최대 성장"));
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setDamage(event.getDamage() * 2.0D);
        }
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = new java.util.ArrayList<String>(super.activeTimerLines());
        lines.add(ChatColor.AQUA + "눈덩이 공격 " + snowAttack + (snowAttack < 5 ? " · 성장 " + growthHits + "/4" : " · 최대 성장"));
        return lines;
    }
}
