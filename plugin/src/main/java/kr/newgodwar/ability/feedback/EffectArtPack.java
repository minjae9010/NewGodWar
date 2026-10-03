package kr.newgodwar.ability.feedback;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Only this pack's SUCCESSFULLY_LOADED acknowledgement permits custom item models. */
public final class EffectArtPack implements Listener {
    private final NewGodWarPlugin plugin;
    private final Set<UUID> loaded = new HashSet<UUID>(), offered = new HashSet<UUID>();
    private Method send, eventId;
    private String url;
    private byte[] hash;
    private UUID id;

    public EffectArtPack(NewGodWarPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getConfig().getBoolean("abilities.effects.resource-pack.enabled", true)) return;
        url = plugin.getConfig().getString("abilities.effects.resource-pack.url", "auto").trim();
        String sha = plugin.getConfig().getString("abilities.effects.resource-pack.sha1", "").trim();
        if (url.isEmpty() || url.equalsIgnoreCase("auto")) {
            String[] pack = PackModels.currentPack();
            if (pack == null) return;
            url = PackModels.downloadUrl(pack);
            sha = pack[1];
        }
        try {
            // These releases cannot address individual custom items without replacing vanilla assets.
            if (Bukkit.getBukkitVersion().matches("1\\.(12|13)(\\..*)?(-.*)?")) {
                plugin.getLogger().info("Minecraft 1.12/1.13 retains vanilla menu items and particle effects.");
                return;
            }
            URI uri = URI.create(url);
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null)
                throw new IllegalArgumentException("Resource pack URL must be HTTP(S)");
            if (!sha.matches("[0-9a-fA-F]{40}")) throw new IllegalArgumentException("Resource pack SHA-1 must contain 40 hexadecimal characters");
            hash = new byte[20];for(int i=0;i<20;i++)hash[i]=(byte)Integer.parseInt(sha.substring(i*2,i*2+2),16);
            id = UUID.nameUUIDFromBytes(("newgodwar-art:"+sha.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            try {
                send = Player.class.getMethod("addResourcePack", UUID.class, String.class, byte[].class, String.class, boolean.class);
                eventId = PlayerResourcePackStatusEvent.class.getMethod("getID");
            } catch (NoSuchMethodException legacy) {
                send = Player.class.getMethod("setResourcePack", String.class, byte[].class);
                eventId = null;
            }
            plugin.getLogger().info("Ability art pack configured: " + id);
            Bukkit.getPluginManager().registerEvents(this, plugin);
            for (Player player : Bukkit.getOnlinePlayers()) offer(player);
        } catch (Exception | LinkageError error) {
            send = null;plugin.getLogger().warning("Ability art pack unavailable: "+error.getMessage());
        }
    }

    public boolean ready(Player player) { return loaded.contains(player.getUniqueId()); }
    public void offer(Player player) {
        if (send == null || !player.isOnline()) return;
        loaded.remove(player.getUniqueId());offered.add(player.getUniqueId());
        try {
            if (eventId != null) send.invoke(player,id,url,hash,"NewGodWar 메뉴 아이콘 · 능력 빛·문양·잔상",false);
            else send.invoke(player,url,hash);
        }
        catch (Exception error) { offered.remove(player.getUniqueId());plugin.getLogger().warning("Could not offer ability art pack: "+error.getMessage()); }
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> offer(event.getPlayer()), 20L);
    }
    @EventHandler public void quit(PlayerQuitEvent event) {loaded.remove(event.getPlayer().getUniqueId());offered.remove(event.getPlayer().getUniqueId());}
    @EventHandler public void status(PlayerResourcePackStatusEvent event) {
        UUID player=event.getPlayer().getUniqueId();
        try {
            if (!offered.contains(player) || (eventId != null && !id.equals(eventId.invoke(event)))) return;
            if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
                loaded.add(player);
                plugin.getLogger().info("Ability art pack loaded by " + event.getPlayer().getName());
            }
            else loaded.remove(player);
        } catch (Exception ignored) { loaded.remove(player); }
    }
}
