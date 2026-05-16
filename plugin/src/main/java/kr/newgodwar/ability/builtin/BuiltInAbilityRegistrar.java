package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.AbilityRegistry;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityRegistrar;

public final class BuiltInAbilityRegistrar implements AbilityRegistrar {

    @Override
    public void registerAbilities(AbilityRegistry registry) {
        for (AbilityDefinition definition : TheomachyAbilities.definitions()) {
            registry.register(definition);
        }
    }
}
