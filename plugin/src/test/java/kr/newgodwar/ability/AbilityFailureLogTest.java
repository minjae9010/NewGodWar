package kr.newgodwar.ability;

import kr.newgodwar.ability.api.GodAbility;
import org.junit.Test;
import static org.junit.Assert.*;

public class AbilityFailureLogTest {
    @Test public void limitsRepeatedFailuresWithoutSuppressingFutureReports() {
        AbilitySession session = new AbilitySession(null, new GodAbility() { });
        assertTrue(session.shouldReportFailure(0));
        assertFalse(session.shouldReportFailure(1));
        assertFalse(session.shouldReportFailure(29999999999L));
        assertTrue(session.shouldReportFailure(30000000000L));
        assertFalse(session.shouldReportFailure(30000000001L));
        assertTrue(new AbilitySession(null, new GodAbility() { }).shouldReportFailure(1));
    }
}
