package xyz.critterz.holonpc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.World;

import java.util.*;

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
                world,
                UUID.fromString(map.get("uuid").toString()),
                map.get("name").toString(),
                Double.parseDouble(map.get("x").toString()),
                Double.parseDouble(map.get("y").toString()),
                Double.parseDouble(map.get("z").toString()),
                Float.parseFloat(map.get("yaw").toString()),
                Float.parseFloat(map.get("pitch").toString())
        );

        if (map.containsKey("properties") && map.get("properties") instanceof Iterable<?> properties) {
            GameProfile profile = npc.getProfile();
            for (Object entry : properties) {
                try {
                    if (entry instanceof Map<?, ?> property) {
                        profile.getProperties().put((String) property.get("name"), new Property((String) property.get("name"), (String) property.get("value"), (String) property.get("signature")));
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
        map.put("uuid", npc.getPlayer().getUniqueId().toString());
        map.put("name", npc.getPlayer().getName());
        map.put("x", npc.getPlayer().getLocation().getX());
        map.put("y", npc.getPlayer().getLocation().getY());
        map.put("z", npc.getPlayer().getLocation().getZ());
        map.put("yaw", npc.getPlayer().getLocation().getYaw());
        map.put("pitch", npc.getPlayer().getLocation().getPitch());

        List<Object> properties = new ArrayList<>();
        for (Property property : npc.getProfile().getProperties().values()) {
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
