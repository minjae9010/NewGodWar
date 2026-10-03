package kr.newgodwar.ability.builtin;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.*;

public final class SnowAbilityTest {
    private final List<String> messages = new ArrayList<String>();
    private final Player owner = player(messages);
    private final AbilityPlayerContext context = new AbilityPlayerContext(null, owner, null);

    @Test public void firstDeathIncreasesAttackFromZeroToOneAndReportsIt() {
        SnowAbility ability = new SnowAbility();
        ability.onStaffRight(context, owner, null);
        assertEquals("공격 지수 : 0", messages.get(0));
        ability.onDeath(context, death(owner));
        assertEquals("공격 지수가 1(으)로 증가했습니다. (최대 5)", messages.get(1));
        ability.onStaffRight(context, owner, null);
        assertEquals("공격 지수 : 1", messages.get(2));
    }

    @Test public void onlyOwnDeathsIncreaseAttackAndGrowthStopsAtFive() {
        SnowAbility ability = new SnowAbility();
        ability.onDeath(context, death(player(new ArrayList<String>())));
        assertTrue(messages.isEmpty());
        ability.onStaffRight(context, owner, null);
        assertEquals("공격 지수 : 0", messages.get(0));
        messages.clear();
        for (int i = 1; i <= 8; i++) {
            ability.onDeath(context, death(owner));
            ability.onStaffRight(context, owner, null);
            assertEquals("공격 지수 : " + Math.min(i, 5), messages.get(messages.size() - 1));
        }
        assertEquals(13, messages.size()); // Five growth messages and eight queries.
    }

    @Test public void growthSurvivesSessionRecoveryAndContinuesAfterwards() throws Exception {
        SnowAbility original = new SnowAbility();
        original.onDeath(context, death(owner));
        original.onDeath(context, death(owner));
        YamlConfiguration saved = new YamlConfiguration();
        original.saveSession(saved);
        YamlConfiguration decoded = new YamlConfiguration();
        decoded.loadFromString(saved.saveToString());
        SnowAbility restored = new SnowAbility();
        restored.loadSession(decoded);
        restored.onStaffRight(context, owner, null);
        assertEquals("공격 지수 : 2", messages.get(messages.size() - 1));
        restored.onDeath(context, death(owner));
        restored.onStaffRight(context, owner, null);
        assertEquals("공격 지수 : 3", messages.get(messages.size() - 1));
    }

    @Test public void legacyOrInvalidSavedAttackStaysWithinZeroToFive() {
        for (Integer savedValue : new Integer[] {null, -3, 0, 1, 5, 99}) {
            YamlConfiguration saved = new YamlConfiguration();
            saved.set("snow-attack", savedValue);
            SnowAbility ability = new SnowAbility();
            ability.loadSession(saved);
            ability.onStaffRight(context, owner, null);
            int expected = savedValue == null ? 0 : Math.max(0, Math.min(5, savedValue));
            assertEquals("공격 지수 : " + expected, messages.get(messages.size() - 1));
        }
    }

    private static PlayerDeathEvent death(Player player) {
        return new PlayerDeathEvent(player, new ArrayList<ItemStack>(), 0, "");
    }

    private static Player player(List<String> messages) {
        return (Player) Proxy.newProxyInstance(SnowAbilityTest.class.getClassLoader(),
            new Class<?>[] {Player.class}, (proxy, method, args) -> {
                if (method.getName().equals("equals")) return proxy == args[0];
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("sendMessage")) messages.add((String) args[0]);
                return null;
            });
    }
}
