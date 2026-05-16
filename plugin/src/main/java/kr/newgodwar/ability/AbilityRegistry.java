package kr.newgodwar.ability;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityFactory;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.GodAbility;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AbilityRegistry {

    private final Map<String, AbilityDefinition> abilities = new LinkedHashMap<String, AbilityDefinition>();

    public void register(Class<? extends GodAbility> abilityClass) {
        AbilityInfo info = abilityClass.getAnnotation(AbilityInfo.class);
        if (info == null) {
            throw new IllegalArgumentException(abilityClass.getName() + " must have @AbilityInfo.");
        }
        register(new AbilityDefinition(
            normalize(info.id()),
            info.name(),
            info.description(),
            info.author(),
            info.enabledByDefault(),
            reflectionFactory(abilityClass)
        ));
    }

    public void register(AbilityDefinition definition) {
        String id = normalize(definition.id());
        if (abilities.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ability id: " + id);
        }
        abilities.put(id, definition);
    }

    public AbilityDefinition get(String id) {
        return abilities.get(normalize(id));
    }

    public Collection<AbilityDefinition> all() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    public Collection<String> ids() {
        return Collections.unmodifiableCollection(new ArrayList<String>(abilities.keySet()));
    }

    private AbilityFactory reflectionFactory(final Class<? extends GodAbility> abilityClass) {
        return new AbilityFactory() {
            @Override
            public GodAbility create() {
                try {
                    Constructor<? extends GodAbility> constructor = abilityClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                } catch (Exception ex) {
                    throw new IllegalStateException("Cannot create ability: " + abilityClass.getName(), ex);
                }
            }
        };
    }

    private String normalize(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase(Locale.ROOT).trim();
    }
}
