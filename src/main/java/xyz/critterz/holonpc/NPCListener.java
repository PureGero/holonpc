package xyz.critterz.holonpc;

import com.github.puregero.multilib.MultiLib;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class NPCListener implements Listener {

    private final HoloNPCPlugin plugin;

    public NPCListener(HoloNPCPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (Player player : MultiLib.getLocalOnlinePlayers()) {
            plugin.getNPCManager().showAllVisibleNPCs(player, player.getLocation());
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getConfigLoader().loadWorld(event.getWorld());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getNPCManager().showAllVisibleNPCs(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (MultiLib.isLocalPlayer(event.getPlayer())) {
            if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
                plugin.getNPCManager().hideAllVisibleNPCs(event.getPlayer(), event.getFrom());
                plugin.getNPCManager().showAllVisibleNPCs(event.getPlayer(), event.getTo());
            } else if (!event.getFrom().getBlock().equals(event.getTo().getBlock())) {
                plugin.getNPCManager().updateVisibleNPCs(event.getPlayer(), event.getFrom(), event.getTo());
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (MultiLib.isLocalPlayer(event.getPlayer())) {
            if (!event.getFrom().getBlock().equals(event.getTo().getBlock())) {
                plugin.getNPCManager().updateVisibleNPCs(event.getPlayer(), event.getFrom(), event.getTo());
            }

            plugin.getNPCManager().updateNPCRotations(event.getPlayer(), event.getFrom(), event.getTo());
        }
    }

}
