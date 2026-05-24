package kr.newgodwar;

import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.command.GodWarCommand;
import kr.newgodwar.command.TeamChatCommand;
import kr.newgodwar.game.BlazeRodRecipes;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.gui.AbilityGui;
import kr.newgodwar.gui.GamblingGui;
import kr.newgodwar.gui.SettingsGui;
import kr.newgodwar.gui.StarterItemsGui;
import kr.newgodwar.listener.AthenaEnchantListener;
import kr.newgodwar.listener.GameListener;
import kr.newgodwar.nms.NmsAdapter;
import kr.newgodwar.nms.NmsAdapters;
import kr.newgodwar.util.GameTips;
import kr.newgodwar.util.Messages;
import kr.newgodwar.util.PluginUpdater;
import kr.newgodwar.util.ServerVersionSupport;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NewGodWarPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 31354;

    private Messages messages;
    private NmsAdapter nmsAdapter;
    private AbilityManager abilityManager;
    private GameManager gameManager;
    private ServerVersionSupport versionSupport;
    private PluginUpdater updater;
    private SettingsGui settingsGui;
    private StarterItemsGui starterItemsGui;
    private AbilityGui abilityGui;
    private GamblingGui gamblingGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (GameTips.repairLegacyConfiguredTips(this)) {
            getLogger().info("Updated legacy blaze rod recipe tip in config.yml.");
        }
        if (removeLegacyTajjaGamblingRewards()) {
            getLogger().info("Removed legacy tajja gambling rewards from config.yml.");
        }

        this.messages = new Messages(this);
        this.versionSupport = ServerVersionSupport.detect();
        this.updater = new PluginUpdater(this);
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("paper_download_target", () -> versionSupport.paperDownloadVersion() ? "supported" : "unsupported"));

        this.nmsAdapter = NmsAdapters.create(this);
        this.abilityManager = new AbilityManager(this);
        this.gameManager = new GameManager(this, abilityManager, nmsAdapter);

        this.starterItemsGui = new StarterItemsGui(this);
        this.settingsGui = new SettingsGui(this, gameManager, starterItemsGui);
        this.abilityGui = new AbilityGui(this, abilityManager);
        this.gamblingGui = new GamblingGui(this);

        GodWarCommand godWarCommand = new GodWarCommand(this, gameManager, abilityManager, settingsGui, starterItemsGui, abilityGui);
        getCommand("godwar").setExecutor(godWarCommand);
        getCommand("godwar").setTabCompleter(godWarCommand);
        getCommand("t").setExecutor(godWarCommand);
        getCommand("t").setTabCompleter(godWarCommand);
        getCommand("x").setExecutor(godWarCommand);
        getCommand("x").setTabCompleter(godWarCommand);
        getCommand("a").setExecutor(godWarCommand);
        getCommand("a").setTabCompleter(godWarCommand);
        getCommand("gamble").setExecutor(gamblingGui);
        getCommand("teamchat").setExecutor(new TeamChatCommand(this, gameManager));

        Bukkit.getPluginManager().registerEvents(new GameListener(this, gameManager, abilityManager, nmsAdapter), this);
        Bukkit.getPluginManager().registerEvents(new AthenaEnchantListener(this, abilityManager), this);
        Bukkit.getPluginManager().registerEvents(settingsGui, this);
        Bukkit.getPluginManager().registerEvents(starterItemsGui, this);
        Bukkit.getPluginManager().registerEvents(abilityGui, this);
        Bukkit.getPluginManager().registerEvents(gamblingGui, this);
        BlazeRodRecipes.register(this);
        updater.start();

        if (versionSupport.paperDownloadVersion()) {
            getLogger().info("Detected Paper downloadable version target: " + versionSupport.summary());
        } else {
            getLogger().warning("Detected version is not in the Paper official download target list. The plugin will stay enabled, but this version is not a guaranteed support target: " + versionSupport.summary());
        }
        getLogger().info("NewGodWar enabled. NMS adapter: " + nmsAdapter.getServerVersion());
    }

    @Override
    public void onDisable() {
        if (updater != null) {
            updater.shutdown();
        }
        if (gameManager != null) {
            gameManager.shutdown();
        }
    }

    public Messages messages() {
        return messages;
    }

    public NmsAdapter nms() {
        return nmsAdapter;
    }

    public AbilityManager abilities() {
        return abilityManager;
    }

    public GameManager game() {
        return gameManager;
    }

    public ServerVersionSupport versionSupport() {
        return versionSupport;
    }

    public PluginUpdater updater() {
        return updater;
    }

    public boolean removeLegacyTajjaGamblingRewards() {
        if (!getConfig().isSet("gambling.rewards.tajja")) {
            return false;
        }
        getConfig().set("gambling.rewards.tajja", null);
        saveConfig();
        return true;
    }
}
