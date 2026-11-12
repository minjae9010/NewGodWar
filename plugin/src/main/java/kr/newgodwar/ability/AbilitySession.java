package kr.newgodwar.ability;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.GodAbility;

public final class AbilitySession {

    private final AbilityDefinition definition;
    private final GodAbility ability;
    private boolean failureReported;
    private long lastFailureReportNanos;

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

    // A broken addon must not flood the log every tick. State expires with this session.
    boolean shouldReportFailure(long nowNanos) {
        if (failureReported && nowNanos - lastFailureReportNanos < 30000000000L) return false;
        failureReported = true;
        lastFailureReportNanos = nowNanos;
        return true;
    }
}
