package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.potion.PotionEffectType;

@AbilityInfo(
    id = "hermes",
    name = "헤르메스",
    description = "항상 빠른 이동 효과를 받습니다.",
    author = "NewGodWar"
)
public final class HermesAbility implements GodAbility {

    @Override
    public void onAssign(AbilityPlayerContext context) {
        int amplifier = context.plugin().getConfig().getInt(context.configPath("speed-amplifier"), 1);
        BukkitCompat.addPotionEffect(context.player(), PotionEffectType.SPEED, 20 * 60 * 60, amplifier, true, false);
    }
}
