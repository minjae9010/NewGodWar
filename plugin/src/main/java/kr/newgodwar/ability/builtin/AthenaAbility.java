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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
@AbilityInfo(
    id = "athena",
    name = "아테나",
    description = "책과 제한된 인챈트 테이블을 만들고 사망자를 통해 경험을 얻습니다.",
    normalSkill = "책 3권을 생성합니다.",
    normalStoneCost = 6,
    normalCooldownSeconds = 20,
    advancedSkill = "게임 중 최대 2회 인챈트 테이블을 생성합니다.",
    advancedStoneCost = 64,
    advancedCooldownSeconds = -1,
    passiveSkill = "다른 플레이어가 사망하면 경험 레벨 1을 얻습니다.",
    grade = AbilityGrade.A
)
final class AthenaAbility extends BaseAbility {
    private int enchantTables = 2;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.BOOK, 3);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (enchantTables <= 0) {
            player.sendMessage("이 능력은 더이상 사용할 수 없습니다.");
            return;
        }
        if (use(context, player, 2, COBBLESTONE, 64, enchantTables > 1 ? 3 : 0)) {
            enchantTables--;
            give(player, material("ENCHANTING_TABLE", "ENCHANTMENT_TABLE"), 1);
            player.sendMessage("남은 교환 횟수 : " + enchantTables);
        }
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (!event.getEntity().equals(context.player())) {
            context.player().setLevel(context.player().getLevel() + 1);
        }
    }
}
