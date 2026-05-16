package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.AbilityRegistry;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityRegistrar;

public final class DefaultAbilityRegistrar implements AbilityRegistrar {

    @Override
    public void registerAbilities(AbilityRegistry registry) {
        for (AbilityDefinition definition : AbilityDefinitions.definitions()) {
            registry.register(definition);
        }
    }
}
