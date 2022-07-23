package xyz.critterz.holonpc;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
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

    public NPC deserialize(World world, Map<?,?> map) {
        NPC npc = new NPC(
                plugin,
                new Location(
                        world,
                        Double.parseDouble(map.get("x").toString()),
                        Double.parseDouble(map.get("y").toString()),
                        Double.parseDouble(map.get("z").toString()),
                        Float.parseFloat(map.get("yaw").toString()),
                        Float.parseFloat(map.get("pitch").toString())
                ),
                UUID.fromString(map.get("uuid").toString()),
                map.get("name").toString()
        );

        if (map.containsKey("properties") && map.get("properties") instanceof Iterable<?> properties) {
            for (Object entry : properties) {
                try {
                    if (entry instanceof Map<?, ?> property) {
                        npc.setTexture(new TextureProperty((String) property.get("name"), (String) property.get("value"), (String) property.get("signature")));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return npc;
    }

    public Map<?, ?> serialize(NPC npc) {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", npc.getUniqueId().toString());
        map.put("name", npc.getName());
        map.put("x", npc.getLocation().getX());
        map.put("y", npc.getLocation().getY());
        map.put("z", npc.getLocation().getZ());
        map.put("yaw", npc.getLocation().getYaw());
        map.put("pitch", npc.getLocation().getPitch());

        List<Object> properties = new ArrayList<>();
        for (TextureProperty property : npc.getTextures().values()) {
            Map<String, Object> propertyMap = new HashMap<>();
            propertyMap.put("name", property.getName());
            propertyMap.put("value", property.getValue());
            propertyMap.put("signature", property.getSignature());
            properties.add(propertyMap);
        }
        if (!properties.isEmpty()) {
            map.put("properties", properties);
        }

        return map;
    }
}
