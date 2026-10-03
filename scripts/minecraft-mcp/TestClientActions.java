package kr.newgodwar.visualtest;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** Minecraft 26.3 internal actions; never sends operating-system input. */
public final class TestClientActions {
    private static volatile boolean recording;

    /** Sample real framebuffers on the render thread, with original timing and bounded disk output. */
    private static void record(Object minecraft, String name, int duration) throws Exception {
        if (!name.matches("[A-Za-z0-9_.-]{1,100}") || name.equals(".") || name.equals(".."))
            throw new IllegalArgumentException("Invalid recording name");
        if (duration < 1000 || duration > 6000 || recording) throw new IllegalStateException("Invalid or overlapping recording");
        Path base = Path.of("recordings").toAbsolutePath().normalize();
        Path folder = base.resolve(name).normalize();
        if (!folder.startsWith(base) || Files.exists(folder)) throw new IllegalArgumentException("Recording already exists");
        Files.createDirectories(folder);
        Class<?> type = Class.forName("xyz.langyo.minecraft.mcp.common.ScreenshotHelper");
        Method capture = type.getDeclaredMethod("takeScreenshotOnMainThread", Object.class, int.class, int.class);
        capture.setAccessible(true);
        Method execute = minecraft.getClass().getMethod("execute", Runnable.class);
        recording = true;
        Thread worker = new Thread(() -> {
            long start = System.nanoTime(), next = start;
            StringBuilder frames = new StringBuilder();
            String error = null;
            int count = 0;
            try {
                while ((System.nanoTime() - start) / 1_000_000 < duration && count < 120) {
                    long wait = next - System.nanoTime();
                    if (wait > 0) TimeUnit.NANOSECONDS.sleep(wait);
                    CompletableFuture<byte[]> frame = new CompletableFuture<>();
                    long[] captured = {0};
                    execute.invoke(minecraft, (Runnable) () -> {
                        try {
                            captured[0] = System.nanoTime();
                            frame.complete((byte[]) capture.invoke(null, minecraft, 960, 540));
                        } catch (Throwable ex) { frame.completeExceptionally(ex); }
                    });
                    byte[] png = frame.get(3, TimeUnit.SECONDS);
                    if (png == null || png.length < 24) throw new IllegalStateException("Empty framebuffer");
                    String file = String.format("%03d.png", count++);
                    Files.write(folder.resolve(file), png);
                    if (frames.length() > 0) frames.append(',');
                    frames.append("{\"file\":\"").append(file).append("\",\"ms\":")
                        .append((captured[0] - start) / 1_000_000.0).append('}');
                    next = Math.max(next + 50_000_000, System.nanoTime());
                }
            } catch (Throwable ex) { error = ex.toString().replace("\\", "\\\\").replace("\"", "\\\""); }
            try {
                String json = "{\"durationMs\":" + duration + ",\"targetFps\":20,\"frames\":[" + frames
                    + "],\"error\":" + (error == null ? "null" : "\"" + error + "\"") + "}";
                Files.writeString(folder.resolve("clip.json"), json, StandardCharsets.UTF_8);
            } catch (Exception ex) { ex.printStackTrace(); }
            finally { recording = false; }
        }, "NGW-frame-recording");
        worker.setDaemon(true); worker.start();
    }
    public static String command(Object minecraft, String command) {
        CompletableFuture<String> result = new CompletableFuture<>();
        try {
            minecraft.getClass().getMethod("execute", Runnable.class).invoke(minecraft, (Runnable) () -> {
                try {
                    String value = command.startsWith("/") ? command.substring(1) : command;
                    if (value.startsWith("__ngw_visual ")) {
                        action(minecraft, value.substring(13).split(" "));
                    } else {
                        Object connection = minecraft.getClass().getMethod("getConnection").invoke(minecraft);
                        if (connection == null) throw new IllegalStateException("No test server connection");
                        connection.getClass().getMethod("sendCommand", String.class).invoke(connection, value);
                    }
                    result.complete("{\"sent\":true,\"method\":\"minecraft26.3.internal\"}");
                } catch (Exception error) {
                    result.complete("{\"error\":\"" + error.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
                }
            });
            return result.get(5, TimeUnit.SECONDS);
        } catch (Exception error) {
            return "{\"error\":\"Internal client action failed or timed out\"}";
        }
    }

    private static void action(Object minecraft, String[] args) throws Exception {
        Object options = minecraft.getClass().getField("options").get(minecraft);
        switch (args[0]) {
            case "menu_hover":
            case "menu_click": {
                int index = Integer.parseInt(args[1]);
                if (index < 0 || index >= 54) throw new IllegalArgumentException("Expected a top-menu slot 0-53");
                Object gui = minecraft.getClass().getField("gui").get(minecraft);
                Object screen = gui.getClass().getMethod("screen").invoke(gui);
                Class<?> container = Class.forName("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen");
                if (!container.isInstance(screen)) throw new IllegalStateException("No container screen is open");
                Object menu = container.getMethod("getMenu").invoke(screen);
                java.util.List<?> slots = (java.util.List<?>) Class.forName("net.minecraft.world.inventory.AbstractContainerMenu")
                    .getField("slots").get(menu);
                // The last 36 slots are the player's inventory; never operate on them here.
                if (index >= slots.size() - 36) throw new IllegalArgumentException("Slot is outside the top menu");
                Object slot = slots.get(index);
                Class<?> slotType = Class.forName("net.minecraft.world.inventory.Slot");
                if (args[0].equals("menu_click")) {
                    Class<?> input = Class.forName("net.minecraft.world.inventory.ContainerInput");
                    Method click = container.getDeclaredMethod("slotClicked", slotType, int.class, int.class, input);
                    click.setAccessible(true);
                    click.invoke(screen, slot, index, 0, input.getField("PICKUP").get(null));
                } else {
                    var left = container.getDeclaredField("leftPos"); left.setAccessible(true);
                    var top = container.getDeclaredField("topPos"); top.setAccessible(true);
                    double x = left.getInt(screen) + slotType.getField("x").getInt(slot) + 8;
                    double y = top.getInt(screen) + slotType.getField("y").getInt(slot) + 8;
                    Object window = minecraft.getClass().getMethod("getWindow").invoke(minecraft);
                    double width = ((Number) window.getClass().getMethod("getScreenWidth").invoke(window)).doubleValue();
                    double height = ((Number) window.getClass().getMethod("getScreenHeight").invoke(window)).doubleValue();
                    double scaledWidth = ((Number) window.getClass().getMethod("getGuiScaledWidth").invoke(window)).doubleValue();
                    double scaledHeight = ((Number) window.getClass().getMethod("getGuiScaledHeight").invoke(window)).doubleValue();
                    Object mouse = minecraft.getClass().getField("mouseHandler").get(minecraft);
                    var mouseX = mouse.getClass().getDeclaredField("xpos"); mouseX.setAccessible(true);
                    var mouseY = mouse.getClass().getDeclaredField("ypos"); mouseY.setAccessible(true);
                    // Set only the test client's in-memory pointer; never move the OS cursor.
                    mouseX.setDouble(mouse, x * width / scaledWidth);
                    mouseY.setDouble(mouse, y * height / scaledHeight);
                }
                break;
            }
            case "respawn": {
                Object player = minecraft.getClass().getField("player").get(minecraft);
                player.getClass().getMethod("respawn").invoke(player);
                break;
            }
            case "pack": {
                if (!args[1].equals("accept") && !args[1].equals("reject")) throw new IllegalArgumentException("Invalid pack action");
                Object gui = minecraft.getClass().getField("gui").get(minecraft);
                Object screen = gui.getClass().getMethod("screen").invoke(gui);
                Class<?> confirm = Class.forName("net.minecraft.client.gui.screens.ConfirmScreen");
                if (screen == null || !confirm.isInstance(screen)) throw new IllegalStateException("No resource pack confirmation is open");
                Object narration = Class.forName("net.minecraft.client.gui.screens.Screen").getMethod("getNarrationMessage").invoke(screen);
                String text = (String) Class.forName("net.minecraft.network.chat.Component").getMethod("getString").invoke(narration);
                if (!text.contains("NewGodWar")) throw new IllegalStateException("This confirmation does not identify the NewGodWar pack");
                var callback = confirm.getDeclaredField("callback"); callback.setAccessible(true);
                Class.forName("it.unimi.dsi.fastutil.booleans.BooleanConsumer").getMethod("accept", boolean.class)
                    .invoke(callback.get(screen), args[1].equals("accept"));
                break;
            }
            case "record": {
                if (args.length != 3) throw new IllegalArgumentException("Recording requires name and milliseconds");
                record(minecraft, args[1], Integer.parseInt(args[2]));
                break;
            }
            case "camera": {
                Class<?> type = Class.forName("net.minecraft.client.CameraType");
                String name = switch (args[1]) {
                    case "first" -> "FIRST_PERSON";
                    case "back" -> "THIRD_PERSON_BACK";
                    case "front" -> "THIRD_PERSON_FRONT";
                    default -> throw new IllegalArgumentException("Invalid camera");
                };
                options.getClass().getMethod("setCameraType", type).invoke(options, type.getField(name).get(null));
                break;
            }
            case "attack", "use": {
                Method method = minecraft.getClass().getDeclaredMethod(args[0].equals("attack") ? "startAttack" : "startUseItem");
                method.setAccessible(true);
                method.invoke(minecraft);
                break;
            }
            case "hud": {
                Object gui = minecraft.getClass().getField("gui").get(minecraft);
                Object hud = gui.getClass().getField("hud").get(gui);
                var field = hud.getClass().getDeclaredField("isHidden");
                field.setAccessible(true);
                field.setBoolean(hud, !Boolean.parseBoolean(args[1]));
                break;
            }
            case "particles": {
                Class<?> type = Class.forName("net.minecraft.server.level.ParticleStatus");
                if (!args[1].equals("ALL") && !args[1].equals("MINIMAL")) throw new IllegalArgumentException("Invalid particles");
                Object option = options.getClass().getMethod("particles").invoke(options);
                option.getClass().getMethod("set", Object.class).invoke(option, type.getField(args[1]).get(null));
                break;
            }
            case "fresh_frame": {
                Class<?> type = Class.forName("xyz.langyo.minecraft.mcp.common.ScreenshotHelper");
                var cache = type.getDeclaredField("cachedScreenshot");
                var timestamp = type.getDeclaredField("cachedScreenshotTime");
                cache.setAccessible(true);
                timestamp.setAccessible(true);
                cache.set(null, null);
                timestamp.setLong(null, 0);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown internal visual-test action");
        }
    }
}
