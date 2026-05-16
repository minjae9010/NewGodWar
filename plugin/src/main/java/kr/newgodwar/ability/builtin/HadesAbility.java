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
    id = "hades",
    name = "하데스",
    description = "주변 생물을 나락으로 떨어뜨리고 사망 시 확률로 아이템을 보존합니다.",
    normalSkill = "주변 생물과 자신을 나락으로 떨어뜨립니다.",
    normalStoneCost = 20,
    advancedSkill = "더 넓은 범위의 주변 생물을 나락으로 떨어뜨립니다.",
    advancedStoneCost = 35,
    passiveSkill = "사망 시 확률로 인벤토리와 방어구를 보존합니다."
)
final class HadesAbility extends BaseAbility {
    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 1, COBBLESTONE, 20, 100)) {
            abyss(player, 2, true);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 2, COBBLESTONE, 35, 150)) {
            abyss(player, 4, false);
        }
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (event.getEntity().equals(context.player()) && RANDOM.nextInt(10) <= 6) {
            savedInventory = context.player().getInventory().getContents();
            savedArmor = context.player().getInventory().getArmorContents();
            event.getDrops().clear();
        }
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        Player player = context.player();
        if (savedInventory != null) {
            player.getInventory().setContents(savedInventory);
            savedInventory = null;
        }
        if (savedArmor != null) {
            player.getInventory().setArmorContents(savedArmor);
            savedArmor = null;
        }
    }
}
