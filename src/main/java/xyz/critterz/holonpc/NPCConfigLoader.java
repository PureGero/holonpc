package xyz.critterz.holonpc;

import com.github.puregero.multilib.MultiLib;
import org.bukkit.World;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCConfigLoader {

    private final HoloNPCPlugin plugin;

    public NPCConfigLoader(HoloNPCPlugin plugin) {
        this.plugin = plugin;

        for (World world : plugin.getServer().getWorlds()) {
            loadWorld(world);
        }

        MultiLib.on(plugin, "holonpc:addnpc", bytes -> {
            Map<?, ?> serializedNPC = (Map<?, ?>) deserializeObject(bytes);
            String world = (String) serializedNPC.remove("world");
            List<Map<?, ?>> mapList = plugin.getConfig().getMapList("npcs." + world);
            mapList.add(serializedNPC);
            plugin.getConfig().set("npcs." + world, mapList);
            plugin.saveConfig();

            World bukkitWorld = plugin.getServer().getWorld(world);
            if (bukkitWorld != null) {
                NPC npc = plugin.getSerializer().deserialize(bukkitWorld, serializedNPC);
                plugin.getNPCManager().registerNPC(npc);
                npc.showToAllNearbyPlayers();
            }
        });

        MultiLib.onString(plugin, "holonpc:removenpc", uuidStr -> {
            UUID uuid = UUID.fromString(uuidStr);
            NPC npc = plugin.getNPCManager().getNPC(uuid);
            if (npc == null) {
                new Exception("Could not find npc to remove " + uuid).printStackTrace();
                return;
            }

            npc.hideFromAllNearbyPlayers();
            plugin.getNPCManager().unregisterNPC(npc);
            removeNPC(npc, false);
        });
    }

    private byte[] serializeObject(Object object) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buffer.toByteArray();
    }

    private Object deserializeObject(byte[] bytes) {
        ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
        try (ObjectInputStream in = new ObjectInputStream(buffer)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadWorld(World world) {
        plugin.getLogger().info("Loading NPCs for world " + world.getName());

        for (NPC npc : plugin.getSerializer().deserialize(world, plugin.getConfig().getMapList("npcs." + world.getName()))) {
            plugin.getNPCManager().registerNPC(npc);
        }
    }

    public void addNPC(NPC npc) {
        Map<?, ?> serializedNPC = plugin.getSerializer().serialize(npc);
        List<Map<?, ?>> mapList = plugin.getConfig().getMapList("npcs." + npc.getPlayer().getWorld().getName());
        mapList.add(serializedNPC);
        plugin.getConfig().set("npcs." + npc.getPlayer().getWorld().getName(), mapList);
        plugin.saveConfig();

        Map<Object, Object> toSend = new HashMap<>(serializedNPC);
        toSend.put("world", npc.getPlayer().getWorld().getName());
        MultiLib.notify("holonpc:addnpc", serializeObject(toSend));
    }

    public void removeNPC(NPC npc) {
        removeNPC(npc, true);
    }

    private void removeNPC(NPC npc, boolean broadcastChanges) {
        List<Map<?, ?>> mapList = plugin.getConfig().getMapList("npcs." + npc.getPlayer().getWorld().getName());
        for (Map<?, ?> map : mapList) {
            if (npc.getPlayer().getUniqueId().toString().equals(map.get("uuid"))) {
                mapList.remove(map);
                break;
            }
        }
        plugin.getConfig().set("npcs." + npc.getPlayer().getWorld().getName(), mapList);
        plugin.saveConfig();

        if (broadcastChanges) {
            MultiLib.notify("holonpc:removenpc", npc.getPlayer().getUniqueId().toString());
        }
    }
}
