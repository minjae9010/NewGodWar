package kr.newgodwar.ability;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.GodAbility;

public final class AbilitySession {

    private final AbilityDefinition definition;
    private final GodAbility ability;

    public AbilitySession(AbilityDefinition definition, GodAbility ability) {
        this.definition = definition;
        this.ability = ability;
    }

    public AbilityDefinition definition() {
        return definition;
    }

    public GodAbility ability() {
        return ability;
    }
}
