package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityDamageContext;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.GodAbility;
import org.bukkit.ChatColor;

import java.util.Random;

@AbilityInfo(
    id = "zeus",
    name = "제우스",
    description = "공격 시 일정 확률로 번개를 내리칩니다.",
    author = "NewGodWar"
)
public final class ZeusAbility implements GodAbility {

    private final Random random = new Random();
    private long lastStrike;

    @Override
    public void onDamage(AbilityDamageContext context) {
        long now = System.currentTimeMillis();
        long cooldownMillis = context.plugin().getConfig().getLong(context.configPath("cooldown-seconds"), 8L) * 1000L;
        if (now - lastStrike < cooldownMillis) {
            return;
        }

        double chance = context.plugin().getConfig().getDouble(context.configPath("lightning-chance"), 0.18D);
        if (random.nextDouble() > chance) {
            return;
        }

        lastStrike = now;
        context.victim().getWorld().strikeLightningEffect(context.victim().getLocation());
        context.damage(context.damage() + 3.0D);
        context.plugin().nms().sendActionBar(context.damager(), ChatColor.YELLOW + "제우스의 번개가 내려쳤습니다.");
    }
}
