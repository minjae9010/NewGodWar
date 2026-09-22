package kr.newgodwar.ability.feedback;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** No modern API types appear in descriptors: the plugin still links on Bukkit 1.12. */
final class DisplayBridge {
    static final DisplayBridge INSTANCE = discover();
    private final Class<?> blockDisplay, itemDisplay;
    private final Constructor<?> matrix, brightness, itemStack;
    private final Method spawn, visible, persistent, show, hide, transform, translate, rotateZ, rotateY, scale;
    private final Method block, item, itemTransform, blockData, material;
    private final Method interpolation, delay, teleportDuration, light, width, height, viewRange;
    private final Object fixed;
    private final Map<String, Object> blocks = new HashMap<String, Object>();
    private final Map<String, ItemStack> items = new HashMap<String, ItemStack>();

    private static DisplayBridge discover() {
        try { return new DisplayBridge(); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return null; }
    }

    private DisplayBridge() throws ReflectiveOperationException {
        Class<?> display = Class.forName("org.bukkit.entity.Display");
        blockDisplay = Class.forName("org.bukkit.entity.BlockDisplay");
        itemDisplay = Class.forName("org.bukkit.entity.ItemDisplay");
        Class<?> matrixClass = Class.forName("org.joml.Matrix4f");
        Class<?> brightnessClass = Class.forName("org.bukkit.entity.Display$Brightness");
        Class<?> itemTransformClass = Class.forName("org.bukkit.entity.ItemDisplay$ItemDisplayTransform");
        matrix = matrixClass.getConstructor(); brightness = brightnessClass.getConstructor(int.class, int.class);
        itemStack = ItemStack.class.getConstructor(Material.class);
        spawn = World.class.getMethod("spawn", Location.class, Class.class, Consumer.class);
        visible = Entity.class.getMethod("setVisibleByDefault", boolean.class);
        persistent = Entity.class.getMethod("setPersistent", boolean.class);
        show = Player.class.getMethod("showEntity", Plugin.class, Entity.class);
        hide = Player.class.getMethod("hideEntity", Plugin.class, Entity.class);
        transform = display.getMethod("setTransformationMatrix", matrixClass);
        translate = matrixClass.getMethod("translate", float.class, float.class, float.class);
        rotateZ = matrixClass.getMethod("rotateZ", float.class);
        rotateY = matrixClass.getMethod("rotateY", float.class);
        scale = matrixClass.getMethod("scale", float.class, float.class, float.class);
        block = blockDisplay.getMethod("setBlock", Class.forName("org.bukkit.block.data.BlockData"));
        item = itemDisplay.getMethod("setItemStack", ItemStack.class);
        itemTransform = itemDisplay.getMethod("setItemDisplayTransform", itemTransformClass);
        Object mode = null;
        for (Object value : itemTransformClass.getEnumConstants()) if (value.toString().equals("FIXED")) mode = value;
        if (mode == null) throw new NoSuchFieldException("FIXED item transform");
        fixed = mode;
        blockData = Bukkit.class.getMethod("createBlockData", String.class);
        // Reflection bypasses Paper's legacy Material.valueOf remapping for this 1.12-compatible plugin.
        material = Material.class.getMethod("valueOf", String.class);
        interpolation = display.getMethod("setInterpolationDuration", int.class);
        delay = display.getMethod("setInterpolationDelay", int.class);
        Method teleport;
        try { teleport = display.getMethod("setTeleportDuration", int.class); }
        catch (NoSuchMethodException olderDisplayApi) { teleport = null; }
        teleportDuration = teleport;
        light = display.getMethod("setBrightness", brightnessClass);
        width = display.getMethod("setDisplayWidth", float.class);
        height = display.getMethod("setDisplayHeight", float.class);
        viewRange = display.getMethod("setViewRange", float.class);
    }

    Entity spawn(Plugin plugin, Location location, ObjectModel.Part part, List<Entity> created) throws ReflectiveOperationException {
        Consumer<Entity> initializer = entity -> {
            created.add(entity);
            try {
                // This callback runs before adding to the world, so no public one-frame flash can occur.
                visible.invoke(entity, false); persistent.invoke(entity, false);
                entity.setGravity(false); entity.setInvulnerable(true); entity.setSilent(true);
                entity.addScoreboardTag("newgodwar_cosmetic");
                interpolation.invoke(entity, 2); delay.invoke(entity, 0);
                if (teleportDuration != null) teleportDuration.invoke(entity, 2);
                light.invoke(entity, brightness.newInstance(12, 12));
                width.invoke(entity, 12F); height.invoke(entity, 6F); viewRange.invoke(entity, 0.6F);
                if (part.item) {
                    ItemStack stack = items.get(part.material);
                    if (stack == null) {
                        stack = (ItemStack) itemStack.newInstance(material.invoke(null, part.material));
                        items.put(part.material, stack);
                    }
                    item.invoke(entity, stack.clone()); itemTransform.invoke(entity, fixed);
                } else {
                    Object data = blocks.get(part.material);
                    if (data == null) {
                        data = blockData.invoke(null, "minecraft:" + part.material.toLowerCase(java.util.Locale.ROOT));
                        blocks.put(part.material, data);
                    }
                    block.invoke(entity, data);
                }
                transform(entity, part);
            } catch (ReflectiveOperationException ex) { throw new IllegalStateException("Cannot initialize cosmetic display", ex); }
        };
        Entity entity = (Entity) spawn.invoke(location.getWorld(), location, part.item ? itemDisplay : blockDisplay, initializer);
        if (entity == null || !entity.isValid()) throw new IllegalStateException("Cosmetic display spawn was rejected");
        return entity;
    }

    void transform(Entity entity, ObjectModel.Part p) throws ReflectiveOperationException {
        Object value = matrix.newInstance();
        translate.invoke(value, (float) p.x, (float) p.y, (float) p.z);
        rotateZ.invoke(value, (float) p.roll); rotateY.invoke(value, (float) p.turn);
        scale.invoke(value, (float) p.sx, (float) p.sy, (float) p.sz);
        if (!p.item) translate.invoke(value, -0.5F, -0.5F, -0.5F);
        delay.invoke(entity, 0); transform.invoke(entity, value);
    }

    void visibility(Plugin plugin, Player viewer, Entity entity, boolean value) throws ReflectiveOperationException {
        (value ? show : hide).invoke(viewer, plugin, entity);
    }
}
