package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
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
    id = "megumin",
    name = "메구밍",
    description = "강력한 폭렬 마법을 쓰고, 마력을 충전해 다음 폭발을 준비합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 25블록 안의 바라보는 위치에 3초 후 위력 5 폭발을 일으키고 사망합니다. 바깥 경고선은 최대 10블록 피해 거리이며, 실제 피해는 거리와 장애물에 따라 달라집니다.",
    normalStoneCost = 32,
    normalCooldownSeconds = 90,
    advancedSkill = "블레이즈 막대기 우클릭: 마력을 충전해 폭렬 마법의 남은 쿨타임을 30초 줄입니다. 우르프에서는 단축량에도 쿨타임 배율이 적용됩니다. 폭렬 준비 중이거나 이미 사용 가능하면 소모하지 않습니다.",
    advancedStoneCost = 12,
    advancedCooldownSeconds = 20,
    passiveSkill = "사망해도 폭렬 마법의 쿨타임은 유지되며, 충전과 재사용을 반복할 수 있습니다.",
    grade = AbilityGrade.C
)
final class MeguminAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.FIRE)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private boolean charging;

    @Override
    public void loadSession(org.bukkit.configuration.ConfigurationSection data) {
        // Old saves marked explosion as spent forever. Retain all other session data.
        org.bukkit.configuration.MemoryConfiguration migrated = new org.bukkit.configuration.MemoryConfiguration();
        for (java.util.Map.Entry<String, Object> entry : data.getValues(true).entrySet())
            if (!(entry.getValue() instanceof org.bukkit.configuration.ConfigurationSection))
                migrated.set(entry.getKey(), entry.getValue());
        java.util.List<Integer> consumed = new java.util.ArrayList<Integer>(data.getIntegerList("consumed-skills"));
        if (consumed.remove(Integer.valueOf(1))) migrated.set("cooldowns.1", 90000L);
        migrated.set("consumed-skills", consumed);
        super.loadSession(migrated);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, final Player player, PlayerInteractEvent event) {
        if (charging || !useNormal(context, player)) {
            return;
        }
        charging = true;
        final Location location = targetLocation(player, 25);
        charge(context, location, 2.0D);
        scheduleLater(context, () -> charge(context, location, 3.0D), 20L);
        scheduleLater(context, () -> charge(context, location, 4.0D), 40L);
        player.sendMessage(ChatColor.RED + "익스플로전!");
        later(context, 3, "폭렬 발동", "폭렬 마법 발동", () -> {
            charging = false;
            if (!active(context) || !player.getWorld().equals(location.getWorld())) return;
            createExplosion(context, player, location, 5.0F, false, true);
            player.setHealth(0.0D);
        });
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        long remaining = cooldownRemainingMillis(1);
        if (charging || remaining == 0) {
            sendAbilityMessage(context, player, "failure", charging ? "폭렬 준비 중에는 충전할 수 없습니다." : "폭렬 마법이 이미 준비되어 있습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            long reduction = context.plugin().abilities().scaleCooldownMillis(30000L);
            setRawCooldown(context, 1, Math.max(0L, remaining - reduction));
            feedback.castCue(context, player, kr.newgodwar.ability.feedback.EffectCue.CHARGE);
            feedback.progress(context, "마력 충전 · 폭렬까지 " + ((cooldownRemainingMillis(1) + 999) / 1000) + "초");
        }
    }

    @Override
    protected void clearTransientState() { charging = false; }
    private void charge(AbilityPlayerContext context, Location center, double radius) {
        // Vanilla entity damage reaches up to power * 2; the growing core is charge progress, not the hit boundary.
        feedback.pulse(context, center, 10.0D);
        Location anchor = center.clone(); anchor.setPitch(0); anchor.setYaw(0);
        if (!feedback.object(context, "explosion-charge", AbilityDesigns.EXPLOSION_CHARGE, anchor, 22, radius))
            feedback.modelOutline(context, anchor, AbilityDesigns.EXPLOSION_CHARGE, radius * 5, radius, feedback.effectViewers(context, anchor));
    }

}
