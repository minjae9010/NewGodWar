package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.ability.feedback.AbilityFeedback;
import kr.newgodwar.ability.feedback.ObjectModel;
import kr.newgodwar.ability.feedback.SharedModels;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Test-only access to each ability's real visual methods; production has no visual registry. */
final class VisualProbe {
    private final NewGodWarPlugin core;
    private final Map<String, GodAbility> abilities = new LinkedHashMap<String, GodAbility>();

    VisualProbe(NewGodWarPlugin core) { this.core = core; }

    GodAbility ability(String id) {
        return abilities.computeIfAbsent(id, key -> core.abilities().registry().get(key).create());
    }

    AbilityFeedback feedback(String id) {
        try {
            Field field = BaseAbility.class.getDeclaredField("feedback");
            field.setAccessible(true);
            return (AbilityFeedback) field.get(ability(id));
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }

    ObjectModel model(String id, String name) {
        try {
            Field field = ability(id).getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (ObjectModel) field.get(null);
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }

    List<ObjectModel> models() {
        List<ObjectModel> result = new ArrayList<ObjectModel>();
        result.add(SharedModels.SPEAR); result.add(SharedModels.SHIELD);
        result.add(SharedModels.WINGS); result.add(SharedModels.FIRE_WINGS);
        for (String id : core.abilities().registry().ids()) {
            // Resolve method signature classes while Paper still has its real CraftServer.
            ability(id).getClass().getDeclaredMethods();
            for (Field field : ability(id).getClass().getDeclaredFields()) {
                if (field.getType() == ObjectModel.class) result.add(model(id, field.getName()));
            }
        }
        return result;
    }

    void effect(String name, AbilityPlayerContext context, Object... arguments) {
        GodAbility owner = ability(context.ability().id());
        for (Method method : owner.getClass().getDeclaredMethods()) {
            if (!method.getName().equals(name)) continue;
            Object[] all = new Object[arguments.length + 1];
            all[0] = context; System.arraycopy(arguments, 0, all, 1, arguments.length);
            try {
                method.setAccessible(true); method.invoke(owner, all); return;
            } catch (InvocationTargetException ex) { throw new AssertionError(ex.getCause()); }
            catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        }
        throw new AssertionError("Missing ability-owned visual: " + name);
    }

    void clear() { for (String id : abilities.keySet()) feedback(id).clear(); }
}
