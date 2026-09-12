package kr.newgodwar.addon;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;

/** Uses Bukkit's plugin lifecycle and class loader for jars in NewGodWar/addon. */
public final class AddonLoader {
    private final JavaPlugin host;
    private final List<Plugin> loaded = new ArrayList<Plugin>();

    public AddonLoader(JavaPlugin host) { this.host = host; }

    public void load() {
        File directory = new File(host.getDataFolder(), "addon");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create addon directory: " + directory);
        }
        File[] jars = directory.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars == null) return;
        Arrays.sort(jars, Comparator.comparing(File::getName));
        List<File> pending = new ArrayList<File>(Arrays.asList(jars));
        boolean progress;
        do {
            progress = false;
            Iterator<File> iterator = pending.iterator();
            while (iterator.hasNext()) {
                File jar = iterator.next();
                try {
                    PluginDescriptionFile description;
                    try (JarFile archive = new JarFile(jar)) {
                        if (archive.getJarEntry("plugin.yml") == null) {
                            throw new IllegalArgumentException("Bukkit plugin.yml is required");
                        }
                        description = new PluginDescriptionFile(archive.getInputStream(archive.getJarEntry("plugin.yml")));
                    }
                    if (!description.getDepend().contains(host.getName())) {
                        throw new IllegalArgumentException("plugin.yml must declare depend: [" + host.getName() + "]");
                    }
                    if (host.getServer().getPluginManager().getPlugin(description.getName()) != null) {
                        throw new IllegalArgumentException("Plugin name already loaded: " + description.getName());
                    }
                    for (String dependency : description.getDepend()) {
                        Plugin required = host.getServer().getPluginManager().getPlugin(dependency);
                        if (required == null || !required.isEnabled()) {
                            throw new UnknownDependencyException(dependency);
                        }
                    }
                    Plugin addon = host.getServer().getPluginManager().loadPlugin(jar);
                    if (addon == null) throw new IllegalStateException("No Bukkit loader accepted the addon");
                    loaded.add(addon);
                    // Modern Paper's runtime provider invokes onLoad inside loadPlugin.
                    // Legacy Bukkit/Spigot loaders leave it to the caller.
                    if (!loadsLifecycleAutomatically()) addon.onLoad();
                    host.getServer().getPluginManager().enablePlugin(addon);
                    if (!addon.isEnabled()) throw new IllegalStateException("Addon failed to enable");
                    host.getLogger().info("Loaded addon: " + addon.getName());
                    iterator.remove();
                    progress = true;
                } catch (UnknownDependencyException ex) {
                    // Retry after dependencies from this directory have been enabled.
                } catch (Exception | LinkageError ex) {
                    host.getLogger().log(Level.SEVERE, "Cannot load addon " + jar.getName(), ex);
                    iterator.remove();
                }
            }
        } while (progress && !pending.isEmpty());
        for (File jar : pending) {
            host.getLogger().severe("Skipped addon with missing, disabled or cyclic dependencies: " + jar.getName());
        }
    }

    public void shutdown() {
        List<Plugin> reverse = new ArrayList<Plugin>(loaded);
        Collections.reverse(reverse);
        for (Plugin addon : reverse) {
            if (addon.isEnabled()) host.getServer().getPluginManager().disablePlugin(addon);
        }
        loaded.clear();
    }

    private boolean loadsLifecycleAutomatically() {
        try {
            Class.forName("io.papermc.paper.plugin.manager.PaperPluginManagerImpl", false,
                host.getServer().getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
