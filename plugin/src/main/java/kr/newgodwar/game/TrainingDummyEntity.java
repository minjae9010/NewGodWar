package kr.newgodwar.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** A server-side player, registered for target lookup but never added to the login/player list. */
public final class TrainingDummyEntity {
    private final Object handle;
    private final Object level;
    private final Object channel;
    private final Method runPendingTasks;
    private final Method readOutbound;
    private final Map<Object, Object> byId;
    private final Map<Object, Object> byName;
    private final Player player;
    private final String legacy;
    private boolean removed;

    private TrainingDummyEntity(Object handle, Object level, Object channel, Map<Object, Object> byId,
                                Map<Object, Object> byName, String legacy) throws ReflectiveOperationException {
        this.handle = handle;
        this.level = level;
        this.channel = channel;
        this.runPendingTasks = channel.getClass().getMethod("runPendingTasks");
        this.readOutbound = channel.getClass().getMethod("readOutbound");
        this.byId = byId;
        this.byName = byName;
        this.legacy = legacy;
        this.player = (Player) call(handle, new String[] {"getBukkitEntity"});
    }

    public static TrainingDummyEntity spawn(Location location, String name) throws ReflectiveOperationException {
        if (Bukkit.getPlayerExact(name) != null) throw new IllegalStateException("이미 사용 중인 더미 이름입니다.");
        String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
        String suffix = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
        String legacy = "net.minecraft.server." + suffix + ".";
        Object server = call(Bukkit.getServer(), new String[] {"getServer"});
        Object level = call(location.getWorld(), new String[] {"getHandle"});
        Object profile = Class.forName("com.mojang.authlib.GameProfile").getConstructor(UUID.class, String.class)
            .newInstance(UUID.randomUUID(), name);
        Class<?> playerType = type("net.minecraft.server.level.ServerPlayer", "net.minecraft.server.level.EntityPlayer", legacy + "EntityPlayer");
        Object handle = null;
        for (Constructor<?> constructor : playerType.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length < 3 || types.length > 4 || !types[0].isInstance(server)
                || !types[1].isInstance(level) || !types[2].isInstance(profile)) continue;
            if (types.length == 3) handle = constructor.newInstance(server, level, profile);
            else {
                Object extra;
                try { extra = types[3].getMethod("createDefault").invoke(null); }
                catch (NoSuchMethodException ignored) { extra = construct(types[3], level); }
                handle = constructor.newInstance(server, level, profile, extra);
            }
            break;
        }
        if (handle == null) throw new NoSuchMethodException("Server player constructor");
        Class<?> flowType = type("net.minecraft.network.protocol.PacketFlow", "net.minecraft.network.protocol.EnumProtocolDirection", legacy + "EnumProtocolDirection");
        Object flow = enumValue(flowType, "SERVERBOUND");
        Object connection = construct(type("net.minecraft.network.Connection", "net.minecraft.network.NetworkManager", legacy + "NetworkManager"), flow);
        Object channel = Class.forName("io.netty.channel.embedded.EmbeddedChannel").getConstructor().newInstance();
        TrainingDummyEntity result = null;
        try {
            field(connection.getClass(), "channel").set(connection, channel);
            Field address = optionalField(connection.getClass(), "address", "socketAddress");
            if (address != null) address.set(connection, new InetSocketAddress("127.0.0.1", 0));
            Class<?> listenerType = type("net.minecraft.server.network.ServerGamePacketListenerImpl", "net.minecraft.server.network.PlayerConnection", legacy + "PlayerConnection");
            Object listener;
            try {
                Class<?> cookieType = Class.forName("net.minecraft.server.network.CommonListenerCookie");
                Object cookie = callStatic(cookieType, "createInitial", profile, false);
                listener = construct(listenerType, server, connection, handle, cookie);
            } catch (ClassNotFoundException ignored) {
                listener = construct(listenerType, server, connection, handle);
            }
            field(handle.getClass(), "connection", "playerConnection").set(handle, listener);
            // A dummy has no client to acknowledge loading or wait out join protection.
            Field loadedTimer = optionalField(listener.getClass(), "clientLoadedTimeoutTimer");
            if (loadedTimer != null) loadedTimer.setInt(listener, 0);
            try { call(handle, new String[] {"setClientLoaded"}, true); }
            catch (NoSuchMethodException ignored) { }
            Field joinProtection = optionalField(handle.getClass(), "spawnInvulnerableTime", "invulnerableTicks");
            if (joinProtection != null) joinProtection.setInt(handle, 0);
            call(handle, new String[] {"snapTo", "moveTo", "setLocation"}, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            // New Paper tracks fake players without allocating a real client chunk loader.
            try { call(handle, new String[] {"moonrise$setRealPlayer"}, false); }
            catch (NoSuchMethodException ignored) { }
            Object playerList = call(server, new String[] {"getPlayerList"});
            result = new TrainingDummyEntity(handle, level, channel, map(playerList, "playersByUUID", "j"), map(playerList, "playersByName"), legacy);
            result.byId.put(result.player.getUniqueId(), handle);
            result.byName.put(name.toLowerCase(Locale.ROOT), handle);
            for (Player viewer : Bukkit.getOnlinePlayers()) result.show(viewer);
            try { call(level, new String[] {"addNewPlayer", "addPlayer"}, handle); }
            catch (NoSuchMethodException ignored) {
                call(level, new String[] {"addEntity"}, handle, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
            if (!result.player.isValid()) throw new IllegalStateException("서버에서 더미 소환을 허용하지 않았습니다.");
            result.player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            result.player.setNoDamageTicks(0);
            result.player.setCustomName("§e테스트 더미 §7(" + name + ")");
            result.player.setCustomNameVisible(true);
            result.player.setRemoveWhenFarAway(false);
            result.player.setCanPickupItems(false);
            return result;
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (result != null) result.remove();
            else call(channel, new String[] {"finishAndReleaseAll"});
            throw error;
        }
    }

    public Player player() { return player; }

    public void show(Player viewer) throws ReflectiveOperationException {
        if (removed || viewer.equals(player)) return;
        Object packet;
        try {
            Class<?> info = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            packet = callStatic(info, "createPlayerInitializing", Collections.singletonList(handle));
        } catch (ClassNotFoundException ignored) {
            Class<?> info = type("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo", legacy + "PacketPlayOutPlayerInfo");
            Class<?> action = type(info.getName() + "$EnumPlayerInfoAction");
            packet = construct(info, enumValue(action, "ADD_PLAYER"), playerArray());
        }
        send(viewer, packet);
    }

    public void tick() throws ReflectiveOperationException {
        // There is no socket. Drain packets generated by effects/inventory updates each tick.
        runPendingTasks.invoke(channel);
        while (readOutbound.invoke(channel) != null) { }
    }

    public void remove() throws ReflectiveOperationException {
        if (removed) return;
        removed = true;
        try {
            try { call(handle, new String[] {"discard"}); }
            catch (NoSuchMethodException ignored) { call(level, new String[] {"removeEntity"}, handle); }
        } finally {
            byId.remove(player.getUniqueId(), handle);
            byName.remove(player.getName().toLowerCase(Locale.ROOT), handle);
            try {
                Object packet;
                try {
                    packet = construct(Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket"), Collections.singletonList(player.getUniqueId()));
                } catch (ClassNotFoundException ignored) {
                    Class<?> info = type("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo", legacy + "PacketPlayOutPlayerInfo");
                    packet = construct(info, enumValue(type(info.getName() + "$EnumPlayerInfoAction"), "REMOVE_PLAYER"), playerArray());
                }
                for (Player viewer : Bukkit.getOnlinePlayers()) send(viewer, packet);
            } finally { call(channel, new String[] {"finishAndReleaseAll"}); }
        }
    }

    private Object playerArray() {
        Object array = Array.newInstance(handle.getClass(), 1);
        Array.set(array, 0, handle);
        return array;
    }

    private static void send(Player player, Object packet) throws ReflectiveOperationException {
        Object handle = call(player, new String[] {"getHandle"});
        Object connection = field(handle.getClass(), "connection", "playerConnection").get(handle);
        call(connection, new String[] {"send", "sendPacket"}, packet);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> map(Object instance, String... names) throws ReflectiveOperationException {
        return (Map<Object, Object>) field(instance.getClass(), names).get(instance);
    }

    private static Class<?> type(String... names) throws ClassNotFoundException {
        for (String name : names) try { return Class.forName(name); } catch (ClassNotFoundException ignored) { }
        throw new ClassNotFoundException(String.join(" / ", names));
    }

    private static Object enumValue(Class<?> type, String name) throws NoSuchFieldException, IllegalAccessException {
        return type.getField(name).get(null);
    }

    private static Field optionalField(Class<?> type, String... names) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : names) try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { }
        }
        return null;
    }

    private static Field field(Class<?> type, String... names) throws NoSuchFieldException {
        Field result = optionalField(type, names);
        if (result == null) throw new NoSuchFieldException(type.getName() + ": " + String.join(", ", names));
        return result;
    }

    private static Object construct(Class<?> type, Object... args) throws ReflectiveOperationException {
        for (Constructor<?> constructor : type.getConstructors()) {
            if (matches(constructor.getParameterTypes(), args)) return constructor.newInstance(args);
        }
        throw new NoSuchMethodException(type.getName() + " constructor");
    }

    private static Object callStatic(Class<?> type, String name, Object... args) throws ReflectiveOperationException {
        return invoke(type, null, new String[] {name}, args);
    }

    private static Object call(Object receiver, String[] names, Object... args) throws ReflectiveOperationException {
        return invoke(receiver.getClass(), receiver, names, args);
    }

    private static Object invoke(Class<?> type, Object receiver, String[] names, Object[] args) throws ReflectiveOperationException {
        for (String name : names) for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && matches(method.getParameterTypes(), args)) return method.invoke(receiver, args);
        }
        throw new NoSuchMethodException(type.getName() + ": " + String.join(", ", names));
    }

    private static boolean matches(Class<?>[] types, Object[] args) {
        if (types.length != args.length) return false;
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type == double.class) type = Double.class;
            else if (type == float.class) type = Float.class;
            else if (type == boolean.class) type = Boolean.class;
            if (args[i] == null ? type.isPrimitive() : !type.isInstance(args[i])) return false;
        }
        return true;
    }
}
