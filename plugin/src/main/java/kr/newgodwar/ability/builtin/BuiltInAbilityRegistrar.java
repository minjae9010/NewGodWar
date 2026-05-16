package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.AbilityRegistry;
import kr.newgodwar.ability.api.AbilityRegistrar;

public final class BuiltInAbilityRegistrar implements AbilityRegistrar {

    @Override
    public void registerAbilities(AbilityRegistry registry) {
        registry.register(ZeusAbility.class);
        registry.register(AresAbility.class);
        registry.register(HermesAbility.class);
        registry.register(PoseidonAbility.class);
    }
}
