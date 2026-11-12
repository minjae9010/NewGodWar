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
    id = "pokego",
    name = "포켓몬고",
    description = "빠르게 전장을 탐험하고 새로운 지역을 발견하면 다른 능력을 얻습니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "탐험 중 신속 I을 받습니다. 서로 다른 4×4블록 구역 16곳을 방문하면 무작위 능력으로 바뀝니다. 같은 곳을 왕복하거나 순간이동·비행하는 것은 탐험에 포함되지 않습니다. 탐험 기록은 사망해도 유지됩니다.",
    grade = AbilityGrade.C
)
final class PokegoAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .passive(EffectCue.WIND)
        .effect(EffectCue.WIND, AbilityDesigns.COMPASS)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private final java.util.Set<String> visited = new java.util.LinkedHashSet<String>();

    @Override
    public void saveSession(org.bukkit.configuration.ConfigurationSection data) {
        super.saveSession(data);
        data.set("explored-regions", new java.util.ArrayList<String>(visited));
    }

    @Override
    public void loadSession(org.bukkit.configuration.ConfigurationSection data) {
        super.loadSession(data);
        visited.clear();
        for (String region : data.getStringList("explored-regions")) {
            if (region.matches("[0-9a-fA-F-]{36}:-?\\d+:-?\\d+") && visited.size() < 16) visited.add(region);
        }
    }

    @Override
    public void onAssign(AbilityPlayerContext context) { onTick(context); }

    @Override
    public void onTick(AbilityPlayerContext context) {
        effect(context, context.player(), PotionEffectType.SPEED, 3, 0);
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        respawnEffect(context, PotionEffectType.SPEED, 3, 0);
    }

    @Override
    public void onMove(AbilityPlayerContext context, PlayerMoveEvent event) {
        if (event.isCancelled() || event instanceof PlayerTeleportEvent || event.getTo() == null
            || context.player().isFlying() || context.player().isGliding() || context.player().isInsideVehicle()
            || !event.getFrom().getWorld().equals(event.getTo().getWorld())) return;
        double distance = event.getFrom().distanceSquared(event.getTo());
        if (distance <= 0.01D || distance > 16.0D) return;
        boolean discovered = visit(event.getFrom());
        discovered |= visit(event.getTo());
        if (discovered) feedback.progress(context, "새 지역 발견 · 탐험 " + visited.size() + "/16");
        if (visited.size() >= 16) {
            context.plugin().abilities().assignRandom(context.player());
            context.player().sendMessage(ChatColor.AQUA + "탐험을 마치고 새 능력을 잡았습니다!");
        }
    }

    private boolean visit(Location location) {
        return visited.size() < 16 && visited.add(location.getWorld().getUID() + ":"
            + Math.floorDiv(location.getBlockX(), 4) + ":" + Math.floorDiv(location.getBlockZ(), 4));
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = new java.util.ArrayList<String>(super.activeTimerLines());
        lines.add(ChatColor.AQUA + "탐험 " + visited.size() + "/16 구역");
        return lines;
    }
}
