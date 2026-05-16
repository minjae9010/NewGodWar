package kr.newgodwar.game;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public final class GameRuleController {

    private final NewGodWarPlugin plugin;
    private final Map<String, Map<String, String>> previousValues = new HashMap<String, Map<String, String>>();

    public GameRuleController(NewGodWarPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyConfiguredRules() {
        if (!plugin.getConfig().getBoolean("gamerules.enabled", true)) {
            return;
        }
        ConfigurationSection rules = plugin.getConfig().getConfigurationSection("gamerules.rules");
        if (rules == null) {
            return;
        }

        previousValues.clear();
        for (World world : Bukkit.getWorlds()) {
            Map<String, String> worldValues = new HashMap<String, String>();
            for (String rule : rules.getKeys(false)) {
                String value = String.valueOf(rules.get(rule));
                String previous = world.getGameRuleValue(rule);
                if (previous != null) {
                    worldValues.put(rule, previous);
                }
                if (!world.setGameRuleValue(rule, value)) {
                    plugin.getLogger().warning("Unknown or unsupported gamerule: " + rule);
                }
            }
            previousValues.put(world.getName(), worldValues);
        }
    }

    public void restorePreviousRules() {
        if (!plugin.getConfig().getBoolean("gamerules.restore-on-stop", true)) {
            previousValues.clear();
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            Map<String, String> worldValues = previousValues.get(world.getName());
            if (worldValues == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : worldValues.entrySet()) {
                world.setGameRuleValue(entry.getKey(), entry.getValue());
            }
        }
        previousValues.clear();
    }
}
