package kr.newgodwar.util;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BukkitCompat {

    private BukkitCompat() {
    }

    @SuppressWarnings("deprecation")
    public static Block getTargetBlock(Player player, int maxDistance) {
        try {
            Method method = player.getClass().getMethod("getTargetBlock", Set.class, int.class);
            return (Block) method.invoke(player, null, maxDistance);
        } catch (Throwable ignored) {
            try {
                Method method = player.getClass().getMethod("getTargetBlock", HashSet.class, int.class);
                return (Block) method.invoke(player, null, maxDistance);
            } catch (Throwable ignoredToo) {
                return player.getTargetBlock((Set<Material>) null, maxDistance);
            }
        }
    }

    public static boolean isMainHandInteract(PlayerInteractEvent event) {
        try {
            Method method = event.getClass().getMethod("getHand");
            Object hand = method.invoke(event);
            return hand == null || !"OFF_HAND".equals(String.valueOf(hand));
        } catch (Throwable ignored) {
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    public static ItemStack mainHandItem(Player player) {
        if (player == null) {
            return null;
        }
        try {
            Method getInventory = player.getClass().getMethod("getInventory");
            Object inventory = getInventory.invoke(player);
            Method getItemInMainHand = inventory.getClass().getMethod("getItemInMainHand");
            Object item = getItemInMainHand.invoke(inventory);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable ignored) {
            return player.getItemInHand();
        }
    }

    public static boolean isEmptyItem(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    public static boolean hasOpenContainer(Player player) {
        try {
            InventoryView view = player.getOpenInventory();
            if (view == null) {
                return false;
            }
            Inventory top = view.getTopInventory();
            if (top == null) {
                return false;
            }
            InventoryType type = top.getType();
            return type != InventoryType.CRAFTING && type != InventoryType.PLAYER;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<? extends Player> onlinePlayers() {
        try {
            Method method = Bukkit.class.getMethod("getOnlinePlayers");
            Object value = method.invoke(null);
            if (value instanceof Collection) {
                return (Collection<? extends Player>) value;
            }
            if (value instanceof Player[]) {
                List<Player> players = new ArrayList<Player>();
                Player[] array = (Player[]) value;
                for (Player player : array) {
                    players.add(player);
                }
                return players;
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<Player>();
    }

    public static void setSurvival(Player player) {
        setGameMode(player, "SURVIVAL", GameMode.SURVIVAL);
    }

    public static void setSpectatorOrAdventure(Player player) {
        setGameMode(player, "SPECTATOR", GameMode.ADVENTURE);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setGameMode(Player player, String name, GameMode fallback) {
        try {
            Object mode = Enum.valueOf((Class<? extends Enum>) GameMode.class.asSubclass(Enum.class), name);
            player.setGameMode((GameMode) mode);
        } catch (Throwable ignored) {
            player.setGameMode(fallback);
            player.setAllowFlight(true);
            player.setFlying(true);
        }
    }

    public static void addPotionEffect(Player player, PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles) {
        PotionEffect effect = createPotionEffect(type, duration, amplifier, ambient, particles);
        if (effect != null) {
            player.addPotionEffect(effect, true);
        }
    }

    public static void clearPotionEffects(Player player) {
        if (player == null) {
            return;
        }
        List<PotionEffectType> types = new ArrayList<PotionEffectType>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            types.add(effect.getType());
        }
        for (PotionEffectType type : types) {
            player.removePotionEffect(type);
        }
    }

    public static void playLevelUp(Player player) {
        Sound sound = sound("ENTITY_PLAYER_LEVELUP");
        if (sound == null) {
            sound = sound("LEVEL_UP");
        }
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        }
    }

    public static void setTeamColor(Team team, ChatColor color) {
        try {
            Method method = team.getClass().getMethod("setColor", ChatColor.class);
            method.invoke(team, color);
        } catch (NoSuchMethodException ignored) {
            team.setPrefix(color.toString());
        } catch (IllegalAccessException ignored) {
            team.setPrefix(color.toString());
        } catch (InvocationTargetException ignored) {
            team.setPrefix(color.toString());
        }
    }

    private static Sound sound(String name) {
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static PotionEffect createPotionEffect(PotionEffectType type, int duration, int amplifier, boolean ambient, boolean particles) {
        try {
            Constructor<PotionEffect> constructor = PotionEffect.class.getConstructor(PotionEffectType.class, int.class, int.class, boolean.class, boolean.class);
            return constructor.newInstance(type, duration, amplifier, ambient, particles);
        } catch (Throwable ignored) {
            try {
                Constructor<PotionEffect> constructor = PotionEffect.class.getConstructor(PotionEffectType.class, int.class, int.class, boolean.class);
                return constructor.newInstance(type, duration, amplifier, ambient);
            } catch (Throwable ignoredToo) {
                try {
                    Constructor<PotionEffect> constructor = PotionEffect.class.getConstructor(PotionEffectType.class, int.class, int.class);
                    return constructor.newInstance(type, duration, amplifier);
                } catch (Throwable ignoredThree) {
                    return null;
                }
            }
        }
    }
}
