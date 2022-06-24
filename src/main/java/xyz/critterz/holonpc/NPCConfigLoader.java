package xyz.critterz.holonpc;

import org.bukkit.World;

import java.util.UUID;

public class NPCConfigLoader {

    private final HoloNPCPlugin plugin;

    public NPCConfigLoader(HoloNPCPlugin plugin) {
        this.plugin = plugin;

        for (World world : plugin.getServer().getWorlds()) {
            loadWorld(world);
        }
    }

    public void loadWorld(World world) {
        plugin.getLogger().info("Loading NPCs for world " + world.getName());

        for (NPC npc : plugin.getSerializer().deserialize(world, plugin.getConfig().getMapList("npcs." + world.getName()))) {
            plugin.getNPCManager().registerNPC(npc);
        }

        plugin.getNPCManager().registerNPC(new NPC(plugin, world, UUID.randomUUID(), "Fred", 0, 66, 0, 45f, -10f));
    }
}
