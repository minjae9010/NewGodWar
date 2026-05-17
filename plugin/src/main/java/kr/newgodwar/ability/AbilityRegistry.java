package kr.newgodwar.ability;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityFactory;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.GodAbility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AbilityRegistry {

    private final Map<String, AbilityDefinition> abilities = new LinkedHashMap<String, AbilityDefinition>();
    private final Map<String, AbilityDefinition> abilitiesByName = new LinkedHashMap<String, AbilityDefinition>();
    private final Map<Class<? extends GodAbility>, AbilityDefinition> abilitiesByClass = new LinkedHashMap<Class<? extends GodAbility>, AbilityDefinition>();

    public void register(Class<? extends GodAbility> abilityClass) {
        if (abilityClass == null) {
            throw new IllegalArgumentException("Ability class cannot be null.");
        }
        int modifiers = abilityClass.getModifiers();
        if (abilityClass.isInterface() || Modifier.isAbstract(modifiers)) {
            throw new IllegalArgumentException(abilityClass.getName() + " must be a concrete ability class.");
        }
        if (abilitiesByClass.containsKey(abilityClass)) {
            return;
        }
        final Constructor<? extends GodAbility> constructor = constructor(abilityClass);
        AbilityInfo info = abilityClass.getAnnotation(AbilityInfo.class);
        if (info == null) {
            throw new IllegalArgumentException(abilityClass.getName() + " must have @AbilityInfo.");
        }
        validate(info, abilityClass);
        register(new AbilityDefinition(
            normalize(info.id()),
            info.name(),
            info.description(),
            info.normalSkill(),
            info.normalStoneCost(),
            info.normalCooldownSeconds(),
            info.advancedSkill(),
            info.advancedStoneCost(),
            info.advancedCooldownSeconds(),
            info.passiveSkill(),
            info.grade(),
            info.author(),
            info.enabledByDefault(),
            reflectionFactory(abilityClass, constructor)
        ), abilityClass);
    }

    public void register(AbilityDefinition definition) {
        register(definition, null);
    }

    private void register(AbilityDefinition definition, Class<? extends GodAbility> abilityClass) {
        if (definition == null) {
            throw new IllegalArgumentException("Ability definition cannot be null.");
        }
        String id = normalize(definition.id());
        String name = normalizeName(definition.name());
        if (abilities.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ability id: " + id);
        }
        if (abilitiesByName.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate ability name: " + definition.name());
        }
        abilities.put(id, definition);
        abilitiesByName.put(name, definition);
        if (abilityClass != null) {
            abilitiesByClass.put(abilityClass, definition);
        }
    }

    public AbilityDefinition get(String id) {
        return abilities.get(normalize(id));
    }

    public AbilityDefinition getByName(String name) {
        return abilitiesByName.get(normalizeName(name));
    }

    public boolean isRegistered(Class<? extends GodAbility> abilityClass) {
        return abilitiesByClass.containsKey(abilityClass);
    }

    public boolean isRegistered(String id) {
        return abilities.containsKey(normalize(id));
    }

    public Collection<AbilityDefinition> all() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    public Collection<String> ids() {
        return Collections.unmodifiableCollection(new ArrayList<String>(abilities.keySet()));
    }

    private Constructor<? extends GodAbility> constructor(Class<? extends GodAbility> abilityClass) {
        try {
            Constructor<? extends GodAbility> constructor = abilityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(abilityClass.getName() + " must have a no-argument constructor.", ex);
        }
    }

    private AbilityFactory reflectionFactory(final Class<? extends GodAbility> abilityClass, final Constructor<? extends GodAbility> constructor) {
        return () -> {
            try {
                return constructor.newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create ability: " + abilityClass.getName(), ex);
            }
        };
    }

    private void validate(AbilityInfo info, Class<? extends GodAbility> abilityClass) {
        if (blank(info.id())) {
            throw new IllegalArgumentException(abilityClass.getName() + " has an empty ability id.");
        }
        if (blank(info.name())) {
            throw new IllegalArgumentException(abilityClass.getName() + " has an empty ability name.");
        }
        if (blank(info.description())) {
            throw new IllegalArgumentException(abilityClass.getName() + " has an empty ability description.");
        }
        if (info.normalStoneCost() < 0 || info.advancedStoneCost() < 0) {
            throw new IllegalArgumentException(abilityClass.getName() + " has a negative stone cost.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String normalize(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase(Locale.ROOT).trim();
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).trim();
    }
}
