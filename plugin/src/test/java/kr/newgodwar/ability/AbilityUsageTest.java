package kr.newgodwar.ability;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.builtin.DefaultAbilityRegistrar;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AbilityUsageTest {
    @Test
    public void everyBuiltinActiveSkillExplainsHowToTriggerIt() {
        AbilityRegistry registry = new AbilityRegistry();
        new DefaultAbilityRegistrar().registerAbilities(registry);
        assertTrue(registry.all().size() > 80);
        for (AbilityDefinition ability : registry.all()) {
            checkUsage(ability.id(), ability.normalSkill());
            checkUsage(ability.id(), ability.advancedSkill());
        }
        AbilityDefinition voodoo = registry.get("voodoo");
        assertTrue(voodoo.normalSkill().contains("팻말을 좌클릭"));
        assertTrue(voodoo.passiveSkill().contains("7초"));
        assertTrue(registry.get("counter").normalSkill().contains("/x <플레이어>"));
        assertTrue(registry.get("hecate").advancedSkill().contains("/x <플레이어>"));
    }

    private void checkUsage(String id, String skill) {
        if (skill.isEmpty() || "없음".equals(skill)) return;
        assertTrue(id + ": missing input instructions: " + skill,
            skill.contains("클릭") || skill.contains("채팅"));
    }
}
