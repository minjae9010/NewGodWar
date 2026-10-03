package kr.newgodwar.ability.builtin;

import java.lang.reflect.Proxy;
import org.bukkit.World;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class HadesAbilityTest {
    public interface HeightWorld extends World { int getMinHeight(); }

    @Test public void abyssIsBelowTheFloorOfModernAndCustomWorlds() {
        for (int floor : new int[] {-64, -128, 0, 32}) {
            World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HeightWorld.class}, (proxy, method, args) -> floor);
            assertEquals(floor - 2.0D, HadesAbility.abyssY(world), 0);
        }
    }

    @Test public void legacyWorldWithoutHeightApiKeepsItsOriginalDestination() {
        World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {World.class}, (proxy, method, args) -> null);
        assertEquals(-2.0D, HadesAbility.abyssY(world), 0);
    }
}
