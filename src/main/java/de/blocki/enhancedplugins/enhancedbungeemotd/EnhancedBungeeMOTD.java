package de.blocki.enhancedplugins.enhancedbungeemotd;

import de.blocki.enhancedplugins.enhancedbungeemotd.listener.PingListener;
import de.blocki.enhancedplugins.enhancedbungeemotd.utils.models.ConfigManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

public final class EnhancedBungeeMOTD extends Plugin {

    private static Plugin plugin;
    private static boolean isDebug;

    @Override
    public void onEnable() {
        plugin = this;

        initConfig();

        PluginManager pm = ProxyServer.getInstance().getPluginManager();
        pm.registerListener(this, new PingListener());
    }

    private static void initConfig(){
        ConfigManager.setDef("config.showRealPlayers", true);
        isDebug = (boolean) ConfigManager.setDef("config.isDebug", true);
        ConfigManager.set("messages.motd_server_offline", "&cThe Server is currently offline.");
    }

    public static void debug(String msg){
        if(isDebug){
            System.out.println("[EB-MOTD Debug] " + msg);
        }
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    public static boolean isDebug() {
        return isDebug;
    }
}
