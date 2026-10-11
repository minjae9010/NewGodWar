package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ObjectEffectsTest {
    @Test public void baseAndPackScenesCanExpireInTheSameUpdate() throws Exception {
        ObjectEffects effects = new ObjectEffects();
        Player player = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Player.class},
            (proxy, method, args) -> null);
        AbilityPlayerContext context = new AbilityPlayerContext(null, player, null);
        ObjectModel model = (phase, detail) -> Collections.emptyList();
        Location location = new Location(null, 0, 0, 0);
        Supplier<Location> anchor = () -> location;
        Supplier<List<Player>> audience = Collections::emptyList;
        Class<?> sceneClass = Class.forName(ObjectEffects.class.getName() + "$Scene");
        Constructor<?> constructor = sceneClass.getDeclaredConstructor(AbilityPlayerContext.class, ObjectModel.class,
            ObjectFrame.class, Supplier.class, Supplier.class, Location.class, int.class, int.class, double.class);
        constructor.setAccessible(true);
        Field scenesField = ObjectEffects.class.getDeclaredField("scenes");
        scenesField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String, Object> scenes = (Map<String, Object>) scenesField.get(effects);
        for (String key : new String[] {"cast", "cast:art"}) scenes.put(key,
            constructor.newInstance(context, model, new ObjectFrame(model), anchor, audience, location, 0, 2, 1D));
        Method update = ObjectEffects.class.getDeclaredMethod("update");
        update.setAccessible(true);
        update.invoke(effects);
        assertTrue(scenes.isEmpty());
        assertFalse(effects.contains("cast"));
    }
}
