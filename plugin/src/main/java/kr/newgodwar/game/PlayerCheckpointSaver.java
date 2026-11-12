package kr.newgodwar.game;

import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Captures native player NBT on the server thread; only detached NBT and paths reach workers. */
final class PlayerCheckpointSaver implements AutoCloseable {
    private static final int IO_WORKERS = 8;
    private final CheckpointIo io = new CheckpointIo(IO_WORKERS);
    private final NativeAccess access;

    PlayerCheckpointSaver(Logger logger) {
        NativeAccess detected = null;
        try {
            detected = new NativeAccess();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            logger.log(Level.FINE, "Native player checkpoint layout unavailable; using Bukkit.savePlayers", ex);
        }
        access = detected;
        logger.info("Player checkpoint I/O: " + (access == null ? "Bukkit synchronous compatibility path" : IO_WORKERS + " workers, same-tick completion barrier"));
    }

    void savePlayers(BiConsumer<String, Long> timing) throws IOException {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Player checkpoints require the server thread");
        if (access == null) {
            Bukkit.savePlayers();
            return;
        }
        try {
            long started = System.nanoTime();
            List<Callable<Void>> writes = new ArrayList<Callable<Void>>();
            List<Object> handles = new ArrayList<Object>();
            boolean disabled = access.disablePlayerDataSaving.getBoolean(null);
            // Capture the entire roster before any writes. No player/world access is scheduled
            // off-thread, and the server cannot process transfers between these snapshots.
            for (Player player : BukkitCompat.onlinePlayers()) {
                if (!Boolean.TRUE.equals(access.isPersistent.invoke(player))) continue;
                Object handle = access.getHandle.invoke(player);
                handles.add(handle);
                if (!disabled) {
                    Object tag;
                    try (AutoCloseable reporter = (AutoCloseable) access.reporter.newInstance(
                            access.problemPath.invoke(handle), access.nativeLogger)) {
                        Object output = access.createOutput.invoke(null, reporter, access.registryAccess.invoke(handle));
                        access.saveWithoutId.invoke(handle, output);
                        tag = access.copyTag.invoke(access.buildResult.invoke(output));
                    }
                    final Object snapshot = tag;
                    final Path target = access.directory.resolve(player.getUniqueId() + ".dat");
                    writes.add(() -> {
                        CheckpointIo.replace(target, temporary -> access.writeCompressed.invoke(null, snapshot, temporary));
                        return null;
                    });
                }
            }
            // Preserve PlayerList.save's statistics/advancements and their disable settings.
            // Native serializers run on the server thread; workers receive only UTF-8 bytes.
            for (Object handle : handles) {
                Object stats = access.getStats.invoke(handle);
                if (stats != null && !access.disableStatSaving.getBoolean(null)) {
                    Object json = access.statsToJson.invoke(stats);
                    addJsonWrite(writes, (Path) access.statsFile.get(stats),
                        (String) access.jsonToString.invoke(access.statsGson, json));
                }
                Object advancements = access.getAdvancements.invoke(handle);
                if (advancements != null && !access.disableAdvancementSaving.getBoolean(null)) {
                    Object result = access.encodeStart.invoke(access.advancementCodec.get(advancements),
                        access.jsonOps, access.advancementsAsData.invoke(advancements));
                    Object json = access.getOrThrow.invoke(result);
                    addJsonWrite(writes, (Path) access.advancementsFile.get(advancements),
                        (String) access.jsonToString.invoke(access.advancementsGson, json));
                }
            }
            timing.accept("players_capture", started);
            started = System.nanoTime();
            io.writeAll(writes);
            timing.accept("players_io", started);
            long tick = access.currentTick.getLong(null);
            for (Object handle : handles) access.lastSave.setLong(handle, tick);
        } catch (Exception ex) {
            // Never fall back while writers might still be active, or commit the session
            // after an incomplete player checkpoint. writeAll has already drained failures.
            throw new IOException("Could not save native player checkpoint", ex);
        }
    }

    private static void addJsonWrite(List<Callable<Void>> writes, Path target, String json) {
        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        writes.add(() -> {
            CheckpointIo.replaceJsonIfChanged(target, bytes);
            return null;
        });
    }

    @Override public void close() { io.close(); }

    /** Capability-gated Mojang-mapped ValueOutput layout; legacy servers use Bukkit unchanged. */
    private static final class NativeAccess {
        final Method getHandle, isPersistent, problemPath, registryAccess, createOutput, saveWithoutId;
        final Method buildResult, copyTag, writeCompressed, getStats, statsToJson, getAdvancements, advancementsAsData;
        final Method encodeStart, getOrThrow, jsonToString;
        final Field disablePlayerDataSaving, disableStatSaving, disableAdvancementSaving, currentTick, lastSave;
        final Field statsFile, advancementsFile, advancementCodec;
        final Constructor<?> reporter;
        final Object nativeLogger, jsonOps, statsGson, advancementsGson;
        final Path directory;

        NativeAccess() throws ReflectiveOperationException {
            Class<?> player = Class.forName("net.minecraft.server.level.ServerPlayer");
            Class<?> tag = Class.forName("net.minecraft.nbt.CompoundTag");
            Class<?> output = Class.forName("net.minecraft.world.level.storage.TagValueOutput");
            Class<?> storage = Class.forName("net.minecraft.world.level.storage.PlayerDataStorage");
            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            Object playerList = Bukkit.getServer().getClass().getMethod("getHandle").invoke(Bukkit.getServer());
            Object playerIo = Class.forName("net.minecraft.server.players.PlayerList").getField("playerIo").get(playerList);
            if (playerIo.getClass() != storage) throw new IllegalStateException("Custom player storage requires native save path");
            Field playerDirectory = storage.getDeclaredField("playerDir");
            playerDirectory.setAccessible(true);
            directory = ((File) playerDirectory.get(playerIo)).toPath();
            Field logger = storage.getDeclaredField("LOGGER");
            logger.setAccessible(true);
            nativeLogger = logger.get(null);
            reporter = Class.forName("net.minecraft.util.ProblemReporter$ScopedCollector").getConstructor(
                Class.forName("net.minecraft.util.ProblemReporter$PathElement"), Class.forName("org.slf4j.Logger"));
            getHandle = craftPlayer.getMethod("getHandle");
            isPersistent = craftPlayer.getMethod("isPersistent");
            problemPath = player.getMethod("problemPath");
            registryAccess = player.getMethod("registryAccess");
            createOutput = output.getMethod("createWithContext", Class.forName("net.minecraft.util.ProblemReporter"),
                Class.forName("net.minecraft.core.HolderLookup$Provider"));
            saveWithoutId = player.getMethod("saveWithoutId", Class.forName("net.minecraft.world.level.storage.ValueOutput"));
            buildResult = output.getMethod("buildResult");
            copyTag = tag.getMethod("copy");
            writeCompressed = Class.forName("net.minecraft.nbt.NbtIo").getMethod("writeCompressed", tag, Path.class);
            getStats = player.getMethod("getStats");
            Class<?> stats = getStats.getReturnType();
            statsToJson = stats.getDeclaredMethod("toJson");
            statsToJson.setAccessible(true);
            statsFile = field(stats, "file");
            statsGson = field(stats, "GSON").get(null);
            getAdvancements = player.getMethod("getAdvancements");
            Class<?> advancements = getAdvancements.getReturnType();
            advancementsAsData = advancements.getDeclaredMethod("asData");
            advancementsAsData.setAccessible(true);
            advancementsFile = field(advancements, "playerSavePath");
            advancementCodec = field(advancements, "codec");
            advancementsGson = field(advancements, "GSON").get(null);
            jsonOps = Class.forName("com.mojang.serialization.JsonOps").getField("INSTANCE").get(null);
            encodeStart = Class.forName("com.mojang.serialization.Encoder").getMethod("encodeStart",
                Class.forName("com.mojang.serialization.DynamicOps"), Object.class);
            getOrThrow = Class.forName("com.mojang.serialization.DataResult").getMethod("getOrThrow");
            jsonToString = Class.forName("com.google.gson.Gson").getMethod("toJson", Class.forName("com.google.gson.JsonElement"));
            disablePlayerDataSaving = Class.forName("org.spigotmc.SpigotConfig").getField("disablePlayerDataSaving");
            disableStatSaving = Class.forName("org.spigotmc.SpigotConfig").getField("disableStatSaving");
            disableAdvancementSaving = Class.forName("org.spigotmc.SpigotConfig").getField("disableAdvancementSaving");
            currentTick = Class.forName("net.minecraft.server.MinecraftServer").getField("currentTick");
            lastSave = player.getField("lastSave");
        }

        private static Field field(Class<?> type, String name) throws NoSuchFieldException {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }
}
