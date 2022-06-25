package xyz.critterz.holonpc;

import com.github.puregero.multilib.MultiLib;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public class NPC {

    private final HoloNPCPlugin plugin;
    private final ServerPlayer nmsPlayer;
    private final Player bukkitPlayer;

    public NPC(HoloNPCPlugin plugin, World world, UUID uuid, @Nullable String name, double x, double y, double z, float yaw, float pitch) {
        this.plugin = plugin;

        if (name == null || name.equals("HoloNPC")) {
            name = "HoloNPC";
            Team team = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam("npcs");
            if (team == null) {
                team = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewTeam("npcs");
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            }
            team.addEntry(name);
        }

        nmsPlayer = new ServerPlayer(((CraftWorld) world).getHandle().getServer(), ((CraftWorld) world).getHandle(), new GameProfile(uuid, name));
        nmsPlayer.absMoveTo(x, y, z, yaw, pitch);
        bukkitPlayer = nmsPlayer.getBukkitEntity();
    }

    public Player getPlayer() {
        return bukkitPlayer;
    }

    public GameProfile getProfile() {
        return nmsPlayer.gameProfile;
    }

    public void showTo(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, nmsPlayer));
        connection.send(new ClientboundAddPlayerPacket(nmsPlayer));
        connection.send(new ClientboundRotateHeadPacket(nmsPlayer, (byte) (nmsPlayer.getYRot() * 256 / 360)));

        // Remove npc from tab list
        ClientboundPlayerInfoPacket removePlayerInfoPacket = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, nmsPlayer);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> connection.send(removePlayerInfoPacket), 100);

        //Add second skin layer
        SynchedEntityData watcher = nmsPlayer.getEntityData();
        watcher.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 126);
        connection.send(new ClientboundSetEntityDataPacket(bukkitPlayer.getEntityId(), watcher, true));
    }

    public void showToAllNearbyPlayers() {
        for (Player player : MultiLib.getLocalOnlinePlayers()) {
            Set<NPC> npcs = plugin.getNPCManager().getVisibleNPCs(player.getLocation());
            if (npcs.contains(this)) {
                showTo(player);
            }
        }
    }

    public void hideFrom(Player player) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        connection.send(new ClientboundRemoveEntitiesPacket(bukkitPlayer.getEntityId()));
    }

    public void hideFromAllNearbyPlayers() {
        for (Player player : MultiLib.getLocalOnlinePlayers()) {
            Set<NPC> npcs = plugin.getNPCManager().getVisibleNPCs(player.getLocation());
            if (npcs.contains(this)) {
                hideFrom(player);
            }
        }
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
