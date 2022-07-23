package xyz.critterz.holonpc;

import com.github.puregero.multilib.MultiLib;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HoloNPCPlugin extends JavaPlugin {

    private NPCSerializer serializer;
    private NPCConfigLoader configLoader;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        npcManager = new NPCManager();
        serializer = new NPCSerializer(this);
        configLoader = new NPCConfigLoader(this);
        new NPCListener(this);
        new NPCCommand(this);
    }

    @Override
    public void onDisable() {
        for (Player player : MultiLib.getLocalOnlinePlayers()) {
            getNPCManager().hideAllVisibleNPCs(player, player.getLocation());
        }
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    public NPCSerializer getSerializer() {
        return serializer;
    }

    public NPCConfigLoader getConfigLoader() {
        return configLoader;
    }
}
