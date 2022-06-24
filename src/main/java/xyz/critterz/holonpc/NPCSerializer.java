package xyz.critterz.holonpc;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCSerializer {

    private final HoloNPCPlugin plugin;

    public NPCSerializer(HoloNPCPlugin plugin) {
        this.plugin = plugin;
    }

    public Iterable<NPC> deserialize(World world, List<Map<?,?>> list) {
        List<NPC> npcs = new ArrayList<>();

        for (Map<?,?> map : list) {
            try {
                npcs.add(deserialize(world, map));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load npc " + map);
                e.printStackTrace();
            }
        }

        return npcs;
    }

    private NPC deserialize(World world, Map<?,?> map) {
        NPC npc = new NPC(
                plugin,
                world,
                UUID.fromString(map.get("uuid").toString()),
                map.get("name").toString(),
                Double.parseDouble(map.get("x").toString()),
                Double.parseDouble(map.get("y").toString()),
                Double.parseDouble(map.get("z").toString()),
                Float.parseFloat(map.get("yaw").toString()),
                Float.parseFloat(map.get("pitch").toString())
        );
        return npc;
    }
}
