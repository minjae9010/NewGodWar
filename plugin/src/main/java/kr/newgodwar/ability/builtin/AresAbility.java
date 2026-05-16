package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityDamageContext;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.GodAbility;

@AbilityInfo(
    id = "ares",
    name = "아레스",
    description = "근접 공격 피해량이 증가합니다.",
    author = "NewGodWar"
)
public final class AresAbility implements GodAbility {

    @Override
    public void onDamage(AbilityDamageContext context) {
        double bonus = context.plugin().getConfig().getDouble(context.configPath("damage-bonus"), 1.25D);
        context.damage(context.damage() * bonus);
    }
}
