package xyz.critterz.holonpc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.critterz.holonpc.util.AreaMap;
import xyz.critterz.holonpc.util.ChunkBasedAreaMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class NPCManager {
    private final HashMap<UUID, NPC> npcs = new HashMap<>();
    private final HashMap<Integer, NPC> npcsByEntityId = new HashMap<>();
    private final HashMap<String, ChunkBasedAreaMap<NPC>> worldToNpcs = new HashMap<>();
    private final HashMap<String, AreaMap<NPC>> worldToNearNpcs = new HashMap<>();

    public Set<NPC> getVisibleNPCs(Location location) {
        AreaMap<NPC> areaMap = worldToNpcs.get(location.getWorld().getName());

        if (areaMap == null) {
            return Collections.emptySet();
        }

        return areaMap.getObjects(location.getBlockX(), location.getBlockZ());
    }

    public Set<NPC> getNearNPCs(Location location) {
        AreaMap<NPC> areaMap = worldToNearNpcs.get(location.getWorld().getName());

        if (areaMap == null) {
            return Collections.emptySet();
        }

        return areaMap.getObjects(location.getBlockX(), location.getBlockZ());
    }

    public void registerNPC(NPC npc) {
        npcs.put(npc.getUniqueId(), npc);
        npcsByEntityId.put(npc.getEntityId(), npc);
        worldToNpcs
                .computeIfAbsent(npc.getLocation().getWorld().getName(), world -> new ChunkBasedAreaMap<>(64))
                .addObject(npc, npc.getLocation().getBlockX(), npc.getLocation().getBlockZ());
        worldToNearNpcs
                .computeIfAbsent(npc.getLocation().getWorld().getName(), world -> new AreaMap<>(3))
                .addObject(npc, npc.getLocation().getBlockX(), npc.getLocation().getBlockZ());
    }

    public void unregisterNPC(NPC npc) {
        npcs.remove(npc.getUniqueId());
        npcsByEntityId.remove(npc.getEntityId());
        worldToNpcs.get(npc.getLocation().getWorld().getName()).removeObject(npc);
        worldToNearNpcs.get(npc.getLocation().getWorld().getName()).removeObject(npc);
    }


    public void showAllVisibleNPCs(Player player, Location location) {
        Set<NPC> npcs = getVisibleNPCs(location);

        for (NPC npc : npcs) {
            npc.showTo(player);
        }
    }

    public void hideAllVisibleNPCs(Player player, Location location) {
        Set<NPC> npcs = getVisibleNPCs(location);

        for (NPC npc : npcs) {
            npc.hideFrom(player);
        }
    }

    public void updateVisibleNPCs(Player player, Location from, Location to) {
        if (from.getBlockX() >> 4 != to.getBlockX() >> 4 || from.getBlockZ() >> 4 != to.getBlockZ() >> 4) {
            // Only update on change in chunk
            Set<NPC> oldNPCs = getVisibleNPCs(from);
            Set<NPC> newNPCs = getVisibleNPCs(to);

            for (NPC oldNPC : oldNPCs) {
                if (!newNPCs.contains(oldNPC)) {
                    oldNPC.hideFrom(player);
                }
            }

            for (NPC newNPC : newNPCs) {
                if (!oldNPCs.contains(newNPC)) {
                    newNPC.showTo(player);
                }
            }
        }
    }

    public void updateNPCRotations(Player player, Location from, Location to) {
        Set<NPC> farNPCs = getNearNPCs(from);
        Set<NPC> nearNPCs = getNearNPCs(to);

        for (NPC farNPC : farNPCs) {
            if (!nearNPCs.contains(farNPC)) {
                farNPC.stopLookingAt(player);
            }
        }

        for (NPC nearNPC : nearNPCs) {
            nearNPC.sendLookAt(player);
        }
    }

    public NPC getNPC(UUID uuid) {
        return npcs.get(uuid);
    }

    public NPC getNPC(int entityId) {
        return npcsByEntityId.get(entityId);
    }
}
