package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.GodAbility;
import org.bukkit.Material;
import org.bukkit.block.Block;

@AbilityInfo(
    id = "poseidon",
    name = "포세이돈",
    description = "물 안에서 주기적으로 회복합니다.",
    author = "NewGodWar"
)
public final class PoseidonAbility implements GodAbility {

    private long lastHeal;

    @Override
    public void onTick(AbilityPlayerContext context) {
        if (context.player().isDead()) {
            return;
        }
        Block block = context.player().getLocation().getBlock();
        if (block.getType() != Material.WATER && !block.getType().name().equals("STATIONARY_WATER")) {
            return;
        }
        long intervalMillis = context.plugin().abilities().scaleCooldownMillis(
            Math.max(1L, context.plugin().getConfig().getLong(context.configPath("water-heal-interval-seconds"), 5L)) * 1000L);
        long now = System.currentTimeMillis();
        if (now - lastHeal < intervalMillis) {
            return;
        }

        double amount = context.plugin().getConfig().getDouble(context.configPath("water-heal-amount"), 1.0D);
        if (amount <= 0.0D) {
            return;
        }
        lastHeal = now;
        context.player().setHealth(Math.min(context.player().getMaxHealth(), context.player().getHealth() + amount));
    }
}
