package xyz.critterz.holonpc;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

public class NPC {

    private final HoloNPCPlugin plugin;
    private final ServerPlayer nmsPlayer;
    private final Player bukkitPlayer;

    public NPC(HoloNPCPlugin plugin, World world, UUID uuid, String name, double x, double y, double z, float yaw, float pitch) {
        this.plugin = plugin;
        nmsPlayer = new ServerPlayer(((CraftWorld) world).getHandle().getServer(), ((CraftWorld) world).getHandle(), new GameProfile(uuid, name));
        nmsPlayer.absMoveTo(x, y, z, yaw, pitch);
        bukkitPlayer = nmsPlayer.getBukkitEntity();
    }

    public Player getPlayer() {
        return bukkitPlayer;
    }

    public void showTo(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, nmsPlayer));
        connection.send(new ClientboundAddPlayerPacket(nmsPlayer));
        connection.send(new ClientboundRotateHeadPacket(nmsPlayer, (byte) (nmsPlayer.getYRot() * 256 / 360)));

        // Remove npc from tab list
        ClientboundPlayerInfoPacket removePlayerInfoPacket = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, nmsPlayer);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> connection.send(removePlayerInfoPacket), 20);
    }

    public void hideFrom(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(nmsPlayer.getId()));
    }

    public void lookAt(Player player) {
        Vector difference = player.getLocation().subtract(bukkitPlayer.getLocation()).toVector().normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-difference.getX(), difference.getZ()));
        float pitch = (float) Math.toDegrees(Math.atan2(-difference.getY(), Math.sqrt(difference.getX() * difference.getX() + difference.getZ() * difference.getZ())));

        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        connection.send(new ClientboundRotateHeadPacket(nmsPlayer, (byte) (yaw * 256 / 360)));
        connection.send(new ClientboundMoveEntityPacket.Rot(nmsPlayer.getId(), (byte) (yaw * 256 / 360), (byte) (pitch * 256 / 360), true));
    }

    public void stopLookingAt(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        connection.send(new ClientboundRotateHeadPacket(nmsPlayer, (byte) (nmsPlayer.getYRot() * 256 / 360)));
        connection.send(new ClientboundMoveEntityPacket.Rot(nmsPlayer.getId(), (byte) (nmsPlayer.getYRot() * 256 / 360), (byte) (nmsPlayer.getXRot() * 256 / 360), true));
    }
}
