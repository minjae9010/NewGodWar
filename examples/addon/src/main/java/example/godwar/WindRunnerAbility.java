package example.godwar;

import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.BaseAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

@AbilityInfo(
    id = "example_windrunner",
    name = "바람 주자",
    description = "블레이즈 막대 좌클릭으로 5초 동안 빨라집니다.",
    normalSkill = "5초 동안 신속 II",
    normalStoneCost = 4,
    normalCooldownSeconds = 15,
    author = "ExampleAddon"
)
public final class WindRunnerAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(player, PotionEffectType.SPEED, 5, 1);
        }
    }
}
