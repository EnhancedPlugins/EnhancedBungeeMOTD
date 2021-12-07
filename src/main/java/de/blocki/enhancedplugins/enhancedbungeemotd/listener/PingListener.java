package de.blocki.enhancedplugins.enhancedbungeemotd.listener;

import de.blocki.enhancedplugins.enhancedbungeemotd.utils.models.ConfigManager;
import de.blocki.enhancedplugins.enhancedbungeemotd.utils.pinger.MCServerPing;
import de.blocki.enhancedplugins.enhancedbungeemotd.utils.pinger.MCServerPingResponse;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class PingListener implements Listener  {

    @EventHandler
    public void pingEvent(final ProxyPingEvent e) {
        if (e.getConnection() == null || e.getConnection().getVirtualHost() == null || e.getConnection().getVirtualHost().getHostName() == null) {return;}

        ServerPing ping = e.getResponse();
        ServerPing.Players pingPlayers = ping.getPlayers();
        ServerPing.Protocol pingVersion = ping.getVersion();

        ListenerInfo l = e.getConnection().getListener();

        //hostname über die die anfrage kommt (eingabe im launcher)
        String host = e.getConnection().getVirtualHost().getHostName();

        ////domain//name///
        Map<String, String> proxyForcedHosts = l.getForcedHosts();

        proxyForcedHosts.forEach((domain, name) -> {

            if(domain.equals(host)) {

                ServerInfo sInfo = ProxyServer.getInstance().getServerInfo(name);
                InetSocketAddress address = (InetSocketAddress) sInfo.getSocketAddress();

                try {
                    MCServerPingResponse resp = MCServerPing.getPing(address.getHostName(), address.getPort());

                    int onlinePlayers;
                    int maxOnlinePlayers;

                    if(ConfigManager.getBool("config.showRealPlayers")){
                        onlinePlayers = resp.getPlayerOnline();
                        maxOnlinePlayers = resp.getPlayerMax();
                    }else {
                        onlinePlayers = ProxyServer.getInstance().getOnlineCount();
                        maxOnlinePlayers = l.getMaxPlayers();
                    }

                    ping.setDescription(resp.getMotd());

                    pingPlayers.setMax(maxOnlinePlayers);
                    pingPlayers.setOnline(onlinePlayers);

                    e.setResponse(ping);

                } catch (IOException | TimeoutException ignored) {
                    System.out.println("Server Offline");

                    ping.setDescription(ConfigManager.getString("messages.motd_server_offline").replace("&", "§"));
                    pingPlayers.setMax(0);
                    pingPlayers.setOnline(0);

                    e.setResponse(ping);
                }

            }
        });
    }
}
