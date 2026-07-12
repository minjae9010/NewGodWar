package kr.newgodwar.game;

import org.bukkit.World;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Bridges the legacy String gamerule API and the typed API introduced after
 * Minecraft 1.12. Keeping both paths reflective prevents linkage errors on
 * server forks that remove either API surface.
 */
final class GameRuleAccess {

    private final Logger logger;
    private final Method legacyGetRules;
    private final Method legacyIsRule;
    private final Method legacyGetValue;
    private final Method legacySetValue;
    private final Class<?> gameRuleClass;
    private final Method typedGetByName;
    private final Method typedValues;
    private final Method typedGetName;
    private final Method typedGetType;
    private final Method typedGetValue;
    private final Method typedSetValue;

    GameRuleAccess(Logger logger) {
        this.logger = logger;
        this.legacyGetRules = findMethod(World.class, "getGameRules");
        this.legacyIsRule = findMethod(World.class, "isGameRule", String.class);
        this.legacyGetValue = findMethod(World.class, "getGameRuleValue", String.class);
        this.legacySetValue = findMethod(World.class, "setGameRuleValue", String.class, String.class);

        this.gameRuleClass = findClass("org.bukkit.GameRule");
        this.typedGetByName = findMethod(gameRuleClass, "getByName", String.class);
        this.typedValues = findMethod(gameRuleClass, "values");
        this.typedGetName = findMethod(gameRuleClass, "getName");
        this.typedGetType = findMethod(gameRuleClass, "getType");
        this.typedGetValue = findMethod(World.class, "getGameRuleValue", gameRuleClass);
        this.typedSetValue = findMethod(World.class, "setGameRule", gameRuleClass, Object.class);
    }

    List<String> getGameRules(World world) {
        if (typedValues != null && typedGetName != null) {
            try {
                Object rules = typedValues.invoke(null);
                int length = Array.getLength(rules);
                List<String> result = new ArrayList<String>(length);
                for (int index = 0; index < length; index++) {
                    Object name = typedGetName.invoke(Array.get(rules, index));
                    if (name != null) {
                        result.add(String.valueOf(name));
                    }
                }
                return result;
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                // Try the legacy API below.
            }
        }

        if (legacyGetRules != null) {
            try {
                String[] rules = (String[]) legacyGetRules.invoke(world);
                List<String> result = new ArrayList<String>(rules.length);
                Collections.addAll(result, rules);
                return result;
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException ex) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    boolean isGameRule(World world, String rule) {
        if (findTypedRule(rule) != null) {
            return true;
        }
        if (legacyIsRule != null) {
            try {
                return Boolean.TRUE.equals(legacyIsRule.invoke(world, rule));
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                return false;
            }
        }
        return false;
    }

    String getGameRuleValue(World world, String rule) {
        Object typedRule = findTypedRule(rule);
        if (typedRule != null && typedGetValue != null) {
            try {
                Object value = typedGetValue.invoke(world, typedRule);
                return value == null ? null : String.valueOf(value);
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                // Try the legacy API below.
            }
        }

        if (legacyGetValue != null) {
            try {
                Object value = legacyGetValue.invoke(world, rule);
                return value == null ? null : String.valueOf(value);
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                warn("read", rule, ex);
                return null;
            }
        }
        warn("read", rule, null);
        return null;
    }

    boolean setGameRuleValue(World world, String rule, String value) {
        Object typedRule = findTypedRule(rule);
        if (typedRule != null && typedSetValue != null) {
            try {
                Object typedValue = convertValue(typedRule, value);
                return Boolean.TRUE.equals(typedSetValue.invoke(world, typedRule, typedValue));
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                // Try the legacy API below.
            }
        }

        if (legacySetValue != null) {
            try {
                return Boolean.TRUE.equals(legacySetValue.invoke(world, rule, value));
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                warn("set", rule, ex);
                return false;
            }
        }
        warn("set", rule, null);
        return false;
    }

    private Object findTypedRule(String rule) {
        if (gameRuleClass == null || rule == null) {
            return null;
        }
        String withoutNamespace = stripMinecraftNamespace(rule);
        if (typedGetByName != null) {
            String[] candidates = new String[] {rule, withoutNamespace, "minecraft:" + withoutNamespace};
            for (String candidate : candidates) {
                try {
                    Object result = typedGetByName.invoke(null, candidate);
                    if (result != null) {
                        return result;
                    }
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                    // Continue with the next spelling.
                }
            }
        }
        String normalizedRule = normalizeRuleName(withoutNamespace);
        for (Field field : gameRuleClass.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                || !gameRuleClass.isAssignableFrom(field.getType())
                || !normalizeRuleName(field.getName()).equals(normalizedRule)) {
                continue;
            }
            try {
                return field.get(null);
            } catch (IllegalAccessException | IllegalArgumentException | LinkageError ex) {
                return null;
            }
        }
        return null;
    }

    private Object convertValue(Object typedRule, String value)
        throws IllegalAccessException, InvocationTargetException {
        if (typedGetType == null) {
            return value;
        }
        Object type = typedGetType.invoke(typedRule);
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Expected true or false, got " + value);
            }
            return Boolean.valueOf(value);
        }
        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return Integer.valueOf(value);
        }
        return value;
    }

    private void warn(String operation, String rule, Exception exception) {
        String detail = rootMessage(exception);
        logger.warning("Failed to " + operation + " gamerule " + rule
            + (detail.isEmpty() ? ": no compatible Bukkit gamerule API is available" : ": " + detail));
    }

    private static String rootMessage(Exception exception) {
        if (exception == null) {
            return "";
        }
        Throwable cause = exception instanceof InvocationTargetException
            ? ((InvocationTargetException) exception).getTargetException()
            : exception;
        String message = cause.getMessage();
        return message == null || message.isEmpty() ? cause.getClass().getSimpleName() : message;
    }

    private static String stripMinecraftNamespace(String rule) {
        return rule.startsWith("minecraft:") ? rule.substring("minecraft:".length()) : rule;
    }

    private static String normalizeRuleName(String rule) {
        return rule.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException | LinkageError ex) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null) {
            return null;
        }
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                return null;
            }
        }
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException | LinkageError ex) {
            return null;
        }
    }
}
