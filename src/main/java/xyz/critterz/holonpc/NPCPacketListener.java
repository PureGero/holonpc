package xyz.critterz.holonpc;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NPCPacketListener extends PacketListenerAbstract {

    private final HoloNPCPlugin plugin;
    private final Map<UUID, CompletableFuture<Void>> interactCooldown = new ConcurrentHashMap<>();

    public NPCPacketListener(HoloNPCPlugin plugin) {
        super(PacketListenerPriority.LOW);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactEntity = new WrapperPlayClientInteractEntity(event);
            NPC npc = plugin.getNPCManager().getNPC(interactEntity.getEntityId());
            if (npc != null && cooldown(interactCooldown, ((Player) event.getPlayer()).getUniqueId(), 100, TimeUnit.MILLISECONDS)) {
                npc.dispatchClickEvent((Player) event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    private <T> boolean cooldown(Map<T, CompletableFuture<Void>> cooldownMap, T key, int time, TimeUnit units) {
        CompletableFuture<Void> existing = cooldownMap.get(key);

        if (existing != null && !existing.isDone()) {
            return false;
        }

        CompletableFuture<Void> future = delayedCompletableFuture(time, units);
        cooldownMap.put(key, future);
        future.thenRun(() -> cooldownMap.remove(key, future));

        return true;
    }

    private CompletableFuture<Void> delayedCompletableFuture(int time, TimeUnit units) {
        return CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(time, units));
    }

}
