package kr.newgodwar.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlazeRodRecipes {

    private BlazeRodRecipes() {
    }

    public static void register(JavaPlugin plugin) {
        add(plugin, "blaze_rod_sticks_vertical", "S", "S", "S");
        add(plugin, "blaze_rod_sticks_horizontal", "SSS");
        add(plugin, "blaze_rod_sticks_diagonal_right", "S  ", " S ", "  S");
        add(plugin, "blaze_rod_sticks_diagonal_left", "  S", " S ", "S  ");
    }

    private static void add(JavaPlugin plugin, String key, String... shape) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key), new ItemStack(Material.BLAZE_ROD));
        recipe.shape(shape);
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }
}
