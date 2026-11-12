package kr.newgodwar.ability.builtin;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.*;

public final class SnowAbilityTest {
    @Test public void deathsDoNotFarmOrEraseCombatProgress() {
        Player owner = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Player.class},
            (p, m, a) -> m.getName().equals("equals") ? p == a[0] : null);
        AbilityPlayerContext context = new AbilityPlayerContext(null, owner, null);
        YamlConfiguration saved = new YamlConfiguration();
        saved.set("snow-attack", 3);
        saved.set("snow-growth-hits", 2);
        SnowAbility ability = new SnowAbility(); ability.loadSession(saved);
        for (int i = 0; i < 8; i++) ability.onDeath(context, new PlayerDeathEvent(owner, new ArrayList<ItemStack>(), 0, ""));
        YamlConfiguration after = new YamlConfiguration(); ability.saveSession(after);
        assertEquals(3, after.getInt("snow-attack"));
        assertEquals(2, after.getInt("snow-growth-hits"));
    }

    @Test public void recoveryPreservesPartialCombatProgressAndCooldowns() throws Exception {
        YamlConfiguration saved = new YamlConfiguration();
        saved.set("snow-attack", 4);
        saved.set("snow-growth-hits", 3);
        saved.set("cooldowns.1", 3000L);
        SnowAbility original = new SnowAbility(); original.loadSession(saved);
        YamlConfiguration encoded = new YamlConfiguration(); original.saveSession(encoded);
        YamlConfiguration decoded = new YamlConfiguration(); decoded.loadFromString(encoded.saveToString());
        SnowAbility restored = new SnowAbility(); restored.loadSession(decoded);
        YamlConfiguration after = new YamlConfiguration(); restored.saveSession(after);
        assertEquals(4, after.getInt("snow-attack"));
        assertEquals(3, after.getInt("snow-growth-hits"));
        assertTrue(restored.cooldownRemainingMillis(1) > 0);
    }

    @Test public void legacyAndInvalidProgressCannotStartBelowOneOrExceedTheCap() {
        for (Integer savedValue : new Integer[] {null, -3, 0, 1, 5, 99}) {
            YamlConfiguration saved = new YamlConfiguration();
            saved.set("snow-attack", savedValue); saved.set("snow-growth-hits", 99);
            SnowAbility ability = new SnowAbility(); ability.loadSession(saved);
            YamlConfiguration after = new YamlConfiguration(); ability.saveSession(after);
            int expected = savedValue == null ? 1 : Math.max(1, Math.min(5, savedValue));
            assertEquals(expected, after.getInt("snow-attack"));
            assertEquals(expected == 5 ? 0 : 3, after.getInt("snow-growth-hits"));
        }
    }
}
