package kr.newgodwar.nms;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class ReflectionNmsAdapter implements NmsAdapter {

    private final Plugin plugin;
    private final String serverVersion;

    public ReflectionNmsAdapter(Plugin plugin) {
        this.plugin = plugin;
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        this.serverVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    @Override
    public String getServerVersion() {
        return serverVersion;
    }

    @Override
    public void sendActionBar(Player player, String message) {
        if (trySpigotActionBar(player, message)) {
            return;
        }
        if (tryModernActionBar(player, message)) {
            return;
        }
        tryLegacyActionBar(player, message);
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (tryBukkitTitle(player, title, subtitle, fadeIn, stay, fadeOut)) {
            return;
        }
        if (tryModernTitle(player, title, subtitle, fadeIn, stay, fadeOut)) {
            return;
        }
        tryLegacyTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    private boolean trySpigotActionBar(Player player, String message) {
        try {
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            Class<?> chatMessageTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBar = enumValue(chatMessageTypeClass, "ACTION_BAR");
            Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Object components = textComponentClass.getMethod("fromLegacyText", String.class).invoke(null, message);
            Method sendMessage = spigot.getClass().getMethod("sendMessage", chatMessageTypeClass, Array.newInstance(baseComponentClass, 0).getClass());
            sendMessage.invoke(spigot, new Object[]{actionBar, components});
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean tryBukkitTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Method method = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            method.invoke(player, title, subtitle, fadeIn, stay, fadeOut);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean tryModernActionBar(Player player, String message) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket");
            Object component = modernComponent(message);
            Object packet = packetClass.getConstructor(componentInterface()).newInstance(component);
            sendPacket(player, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean tryModernTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Object titleComponent = modernComponent(title);
            Object subtitleComponent = modernComponent(subtitle);
            Object titlePacket = constructFirstAvailable(
                new String[]{"net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket"},
                new Class<?>[]{componentInterface()},
                new Object[]{titleComponent}
            );
            Object subtitlePacket = constructFirstAvailable(
                new String[]{"net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket"},
                new Class<?>[]{componentInterface()},
                new Object[]{subtitleComponent}
            );
            Object timesPacket = constructFirstAvailable(
                new String[]{"net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket"},
                new Class<?>[]{int.class, int.class, int.class},
                new Object[]{fadeIn, stay, fadeOut}
            );
            sendPacket(player, timesPacket);
            sendPacket(player, titlePacket);
            sendPacket(player, subtitlePacket);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void tryLegacyActionBar(Player player, String message) {
        try {
            Object component = legacyComponent(message);
            Class<?> packetClass = nmsClass("PacketPlayOutChat");
            Object packet;
            try {
                Class<?> chatMessageType = nmsClass("ChatMessageType");
                packet = packetClass.getConstructor(componentInterface(), chatMessageType, UUID.class)
                    .newInstance(component, enumValue(chatMessageType, "GAME_INFO"), player.getUniqueId());
            } catch (Throwable ignored) {
                packet = packetClass.getConstructor(componentInterface(), byte.class).newInstance(component, (byte) 2);
            }
            sendPacket(player, packet);
        } catch (Throwable ex) {
            plugin.getLogger().fine("ActionBar packet failed: " + ex.getMessage());
        }
    }

    private void tryLegacyTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Class<?> packetClass = nmsClass("PacketPlayOutTitle");
            Class<?> enumClass = nmsClass("PacketPlayOutTitle$EnumTitleAction");
            Constructor<?> textConstructor = packetClass.getConstructor(enumClass, componentInterface());
            Constructor<?> timesConstructor = packetClass.getConstructor(enumClass, componentInterface(), int.class, int.class, int.class);

            Object empty = legacyComponent("");
            Object titlePacket = textConstructor.newInstance(enumValue(enumClass, "TITLE"), legacyComponent(title));
            Object subtitlePacket = textConstructor.newInstance(enumValue(enumClass, "SUBTITLE"), legacyComponent(subtitle));
            Object timesPacket = timesConstructor.newInstance(enumValue(enumClass, "TIMES"), empty, fadeIn, stay, fadeOut);
            sendPacket(player, timesPacket);
            sendPacket(player, titlePacket);
            sendPacket(player, subtitlePacket);
        } catch (Throwable ex) {
            plugin.getLogger().fine("Title packet failed: " + ex.getMessage());
        }
    }

    private Object modernComponent(String legacyText) throws Exception {
        String plain = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', legacyText == null ? "" : legacyText));
        try {
            Class<?> component = Class.forName("net.minecraft.network.chat.Component");
            return component.getMethod("literal", String.class).invoke(null, plain);
        } catch (Throwable ignored) {
            return jsonComponent(plain, "net.minecraft.network.chat.IChatBaseComponent$ChatSerializer");
        }
    }

    private Object legacyComponent(String legacyText) throws Exception {
        String plain = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', legacyText == null ? "" : legacyText));
        return jsonComponent(plain, "net.minecraft.server." + serverVersion + ".IChatBaseComponent$ChatSerializer");
    }

    private Object jsonComponent(String plainText, String serializerClassName) throws Exception {
        Class<?> serializer = Class.forName(serializerClassName);
        Method parse = null;
        for (Method method : serializer.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == String.class) {
                parse = method;
                break;
            }
        }
        if (parse == null) {
            throw new NoSuchMethodException(serializerClassName + "#parse");
        }
        parse.setAccessible(true);
        return parse.invoke(null, "{\"text\":\"" + escapeJson(plainText) + "\"}");
    }

    private Class<?> componentInterface() throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.network.chat.Component");
        } catch (ClassNotFoundException ignored) {
            try {
                return Class.forName("net.minecraft.network.chat.IChatBaseComponent");
            } catch (ClassNotFoundException ignoredToo) {
                return nmsClass("IChatBaseComponent");
            }
        }
    }

    private Object constructFirstAvailable(String[] classNames, Class<?>[] parameterTypes, Object[] arguments) throws Exception {
        Exception last = null;
        for (String className : classNames) {
            try {
                Class<?> packetClass = Class.forName(className);
                return packetClass.getConstructor(parameterTypes).newInstance(arguments);
            } catch (Exception ex) {
                last = ex;
            }
        }
        throw last == null ? new ClassNotFoundException() : last;
    }

    private void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = findConnection(handle);
        Method send = findPacketSendMethod(connection, packet);
        send.invoke(connection, packet);
    }

    private Object findConnection(Object handle) throws Exception {
        String[] names = {"playerConnection", "connection", "b", "c"};
        for (String name : names) {
            try {
                Field field = findField(handle.getClass(), name);
                Object value = field.get(handle);
                if (value != null) {
                    return value;
                }
            } catch (Throwable ignored) {
            }
        }
        Class<?> type = handle.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(handle);
                    if (value != null && value.getClass().getName().toLowerCase().contains("connection")) {
                        return value;
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        throw new NoSuchFieldException("player connection");
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Method findPacketSendMethod(Object connection, Object packet) throws Exception {
        for (Method method : connection.getClass().getMethods()) {
            if (method.getParameterTypes().length != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            if (parameter.isAssignableFrom(packet.getClass())) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("packet send");
    }

    private Class<?> nmsClass(String simpleName) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + serverVersion + "." + simpleName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
