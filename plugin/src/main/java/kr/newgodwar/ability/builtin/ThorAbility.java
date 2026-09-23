package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.ObjectModel;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static kr.newgodwar.ability.feedback.ModelParts.*;

@AbilityInfo(
    id = "thor", name = "토르",
    description = "천둥신의 망치 묠니르를 던지고 회수하며 모은 전하로 낙뢰를 내립니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 20블록 안의 적 위치로 묠니르를 던집니다. 0.6초 뒤 적이 충돌 지점 1.5블록 안에 있으면 피해 5와 감속 2초를 주고, 1.2초 뒤 회수하며 전하 1을 얻습니다.",
    normalStoneCost = 12, normalCooldownSeconds = 18,
    advancedSkill = "블레이즈 막대기 우클릭: 전하를 모두 소모해 반경 5블록 적에게 천둥 강타를 내립니다. 낙뢰 피해 3 + 전하당 2를 주고 밀쳐내며 전하가 1 이상 필요합니다.",
    advancedStoneCost = 24, advancedCooldownSeconds = 65,
    passiveSkill = "묠니르(철 도끼)를 지급받고 도끼 직접 공격의 피해가 15% 증가합니다. 1초마다 적중 시 전하 1을 얻으며 최대 3까지 저장합니다. 번개 피해를 무시합니다.",
    grade = AbilityGrade.A
)
final class ThorAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.LIGHTNING)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private int hammerFrame;
    private int charges;
    private long nextCharge;
    private boolean hammerDamage;
    private boolean hammerInFlight;

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        ItemStack hammer = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = hammer.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "묠니르");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "천둥신 토르의 망치", ChatColor.GRAY + "블레이즈 막대기로 투척과 천둥 강타를 사용합니다."));
        hammer.setItemMeta(meta);
        give(context.player(), hammer);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (hammerInFlight) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "묠니르가 돌아오는 중입니다.");
            return;
        }
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null || !useNormal(context, player)) return;
        hammerInFlight = true;
        Location origin = player.getEyeLocation();
        Location impact = target.getEyeLocation();
        for (int i = 1; i <= 12; i++) {
            final int frame = i;
            scheduleLater(context, () -> {
                if (frame == 12) hammerInFlight = false;
                if (!active(context) || !player.getWorld().equals(impact.getWorld())) return;
                hammer(context, frame <= 6 ? between(origin, impact, frame / 6.0D)
                    : between(impact, player.getEyeLocation(), (frame - 6) / 6.0D));
                if (frame == 6 && validEnemy(context, target, 24) && player.hasLineOfSight(target)
                    && target.getEyeLocation().distanceSquared(impact) <= 2.25D) {
                    thunderbolt(context, impact);
                    hammerHit(context, target, 5);
                    if (validEnemy(context, target, 24)) effect(context, target, "SLOWNESS", "SLOW", 2, 0);
                }
                if (frame == 12) gainCharge(context);
            }, i * 2L);
        }
    }

    private Location between(Location from, Location to, double fraction) {
        return from.clone().add(to.toVector().subtract(from.toVector()).multiply(fraction));
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (charges == 0) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "망치를 회수하거나 도끼로 적을 공격해 전하를 모으세요.");
            return;
        }
        List<Player> targets = enemies(context, player.getLocation(), 5);
        if (targets.isEmpty()) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "반경 5블록 안에 적이 없습니다.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        int spent = charges;
        charges = 0;
        Location center = player.getLocation();
        for (Player target : targets) {
            thunderbolt(context, target.getLocation());
            hammerHit(context, target, 3 + spent * 2);
            if (validEnemy(context, target, 5)) repel(target, center, 0.5D + spent * 0.15D);
        }
    }

    private void hammerHit(AbilityPlayerContext context, Player target, double amount) {
        hammerDamage = true;
        try { damage(context, target, amount, context.player()); }
        finally { hammerDamage = false; }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker || hammerDamage || event.isCancelled() || event.getDamage() <= 0
            || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
            || !event.getDamager().equals(context.player())
            || !context.player().getItemInHand().getType().name().endsWith("_AXE")) return;
        if (!validEnemy(context, opponent, 5)) return;
        event.setDamage(event.getDamage() * 1.15D);
        if (System.currentTimeMillis() >= nextCharge) {
            nextCharge = System.currentTimeMillis() + 1000L;
            gainCharge(context);
        }
    }

    private void gainCharge(AbilityPlayerContext context) {
        charges = Math.min(3, charges + 1);
        feedback.passive(context, "망치 전하 " + charges + "/3");
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        }
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        lines.add(ChatColor.YELLOW + "망치 전하 " + charges + "/3");
        return lines;
    }

    @Override
    protected void clearTransientState() { charges = 0; nextCharge = 0; hammerInFlight = false; }

    static final ObjectModel HAMMER = ObjectModel.animated((phase, detail) -> {
        List<ObjectModel.Part> out = new ArrayList<ObjectModel.Part>();
        box(out, "IRON_BLOCK", 0, 0.12, 0, 0.78, 0.34, 0.34, 0, 0);
        box(out, "POLISHED_ANDESITE", -0.43, 0.12, 0, 0.09, 0.28, 0.28, 0, 0);
        box(out, "POLISHED_ANDESITE", 0.43, 0.12, 0, 0.09, 0.28, 0.28, 0, 0);
        box(out, "DARK_OAK_PLANKS", 0, -0.4, 0, 0.11, 0.7, 0.11, 0, 0);
        box(out, "GOLD_BLOCK", 0, -0.1, 0, 0.18, 0.09, 0.18, 0, 0);
        box(out, "GOLD_BLOCK", 0, -0.76, 0, 0.18, 0.12, 0.18, 0, 0);
        return rotate(out, Math.sin(phase * 0.6) * 0.4);
    });

    /** A small hammer silhouette, used along Mjolnir's actual outbound/return path. */
    private void hammer(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null || !feedback.visuals(context)) return;
        if (feedback.object(context, "hammer", HAMMER, center, 5, 1)) return;
        feedback.modelOutline(context, center, HAMMER, ++hammerFrame, 1, feedback.effectViewers(context, center));
    }

    /** Cosmetic lightning never ignites blocks or damages teammates. */
    private void thunderbolt(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = feedback.effectViewers(context, center);
        for (int i = 0; i <= 24; i++) {
            double offset = i == 24 ? 0 : Math.sin(i * 1.9D) * 0.35D;
            feedback.particle(context, center.clone().add(offset, 6.0D - i * 0.25D, -offset),
                AbilityTheme.LIGHTNING.particle(), audience, 1, 0);
        }
        if (feedback.allow("thunder-sound", 150L)) feedback.sound(context, center, audience, AbilityTheme.LIGHTNING.sound(), 0.6F, 1.1F);
    }
}
