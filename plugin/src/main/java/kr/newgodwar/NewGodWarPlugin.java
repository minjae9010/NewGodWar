package kr.newgodwar;

import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.command.GodWarCommand;
import kr.newgodwar.command.TeamChatCommand;
import kr.newgodwar.game.BlazeRodRecipes;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.gui.AbilityGui;
import kr.newgodwar.gui.SettingsGui;
import kr.newgodwar.listener.GameListener;
import kr.newgodwar.nms.NmsAdapter;
import kr.newgodwar.nms.NmsAdapters;
import kr.newgodwar.util.Messages;
import kr.newgodwar.util.ServerVersionSupport;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NewGodWarPlugin extends JavaPlugin {

    private Messages messages;
    private NmsAdapter nmsAdapter;
    private AbilityManager abilityManager;
    private GameManager gameManager;
    private ServerVersionSupport versionSupport;
    private SettingsGui settingsGui;
    private AbilityGui abilityGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages(this);
        this.versionSupport = ServerVersionSupport.detect();
        this.nmsAdapter = NmsAdapters.create(this);
        this.abilityManager = new AbilityManager(this);
        this.gameManager = new GameManager(this, abilityManager, nmsAdapter);

        this.settingsGui = new SettingsGui(this, gameManager);
        this.abilityGui = new AbilityGui(this, abilityManager);

        GodWarCommand godWarCommand = new GodWarCommand(this, gameManager, abilityManager, settingsGui, abilityGui);
        getCommand("godwar").setExecutor(godWarCommand);
        getCommand("godwar").setTabCompleter(godWarCommand);
        getCommand("x").setExecutor(godWarCommand);
        getCommand("x").setTabCompleter(godWarCommand);
        getCommand("a").setExecutor(godWarCommand);
        getCommand("a").setTabCompleter(godWarCommand);
        getCommand("teamchat").setExecutor(new TeamChatCommand(this, gameManager));

        Bukkit.getPluginManager().registerEvents(new GameListener(this, gameManager, abilityManager, nmsAdapter), this);
        Bukkit.getPluginManager().registerEvents(settingsGui, this);
        Bukkit.getPluginManager().registerEvents(abilityGui, this);
        BlazeRodRecipes.register(this);

        if (versionSupport.paperDownloadVersion()) {
            getLogger().info("Detected Paper downloadable version target: " + versionSupport.summary());
        } else {
            getLogger().warning("Detected version is not in the Paper official download target list. The plugin will stay enabled, but this version is not a guaranteed support target: " + versionSupport.summary());
        }
        getLogger().info("NewGodWar enabled. NMS adapter: " + nmsAdapter.getServerVersion());
    }

    @Override
    public void onDisable() {
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
}
