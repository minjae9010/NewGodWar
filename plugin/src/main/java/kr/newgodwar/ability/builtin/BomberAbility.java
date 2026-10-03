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
    id = "bomber",
    name = "봄버",
    description = "짧은 거리의 위치에 폭탄을 숨겨 설치하고 원격으로 터뜨립니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 5블록 안의 바라보는 위치 위에 폭탄을 설치합니다. 다시 설치하면 위치만 바뀌고 비용은 들지 않습니다.",
    normalStoneCost = 0,
    advancedSkill = "블레이즈 막대기 우클릭: 설치한 폭탄을 위력 2로 터뜨립니다.",
    advancedStoneCost = 30,
    advancedCooldownSeconds = 50,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class BomberAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.FIRE)
        .dedicated()
        .privateCast()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private Location bombLocation;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        bombLocation = targetLocation(player, 5).add(0, 1, 0);
        if (feedback.allow("bomb-placement", 700L)) {
            feedback.activated(context, player, false);
            if (!feedback.object(context, "hidden-bomb", AbilityDesigns.BOMB, bombLocation, 18, 1))
                feedback.modelOutline(context, bombLocation, AbilityDesigns.BOMB, 0, 1, feedback.effectViewers(context, bombLocation));
        }
        player.sendMessage("바라보는 위치에 폭탄을 설치했습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (bombLocation == null) {
            player.sendMessage("설치된 폭탄이 없습니다. 좌클릭으로 먼저 설치하세요.");
            return;
        }
        if (useAdvanced(context, player)) {
            createExplosion(context, player, bombLocation, 2.0F, true, true);
            bombLocation = null;
            player.sendMessage("폭탄이 폭발했습니다!");
        }
    }
}
