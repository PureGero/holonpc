package xyz.critterz.holonpc;

import com.github.puregero.multilib.MultiLib;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.provider.PlayerDataProvider;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.SkinSection;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class NPC {

    private final HoloNPCPlugin plugin;
    private final UUID uuid;
    private final String name;
    private final HashMap<String, TextureProperty> textures = new HashMap<>();
    private final int entityId;
    private Location location;

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

        this.uuid = uuid;
        this.name = name;
        this.entityId = new Random(this.uuid.getLeastSignificantBits() ^ this.uuid.getMostSignificantBits()).nextInt();
        this.location = new Location(world, x, y, z, yaw, pitch);
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Location getLocation() {
        return location;
    }

    public Map<String, TextureProperty> getTextures() {
        return textures;
    }

    public void setTexture(TextureProperty textureProperty) {
        textures.put(textureProperty.getName(), textureProperty);
    }

    public void showTo(Player player) {
        WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                new WrapperPlayServerPlayerInfo.PlayerData(Component.text(name), new UserProfile(uuid, name, new ArrayList<>(textures.values())), GameMode.SURVIVAL, 0)
        );

        WrapperPlayServerSpawnPlayer spawnPlayer = new WrapperPlayServerSpawnPlayer(
                entityId,
                uuid,
                new com.github.retrooper.packetevents.protocol.world.Location(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()),
                new PlayerDataProvider.PlayerBuilder<>(new PlayerDataProvider()).skinParts(SkinSection.ALL).build().encode()
        );

        WrapperPlayServerEntityHeadLook headLook = new WrapperPlayServerEntityHeadLook(
            entityId,
            location.getYaw()
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, playerInfo);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPlayer);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, headLook);

        // Remove npc from tab list
        WrapperPlayServerPlayerInfo removePlayerInfo = new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
                new WrapperPlayServerPlayerInfo.PlayerData(Component.text(name), new UserProfile(uuid, name, new ArrayList<>(textures.values())), GameMode.SURVIVAL, 0)
        );
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, removePlayerInfo), 100);
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
        WrapperPlayServerDestroyEntities destroyEntity = new WrapperPlayServerDestroyEntities(
                entityId
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyEntity);
    }

    public void hideFromAllNearbyPlayers() {
        for (Player player : MultiLib.getLocalOnlinePlayers()) {
            Set<NPC> npcs = plugin.getNPCManager().getVisibleNPCs(player.getLocation());
            if (npcs.contains(this)) {
                hideFrom(player);
            }
        }
    }

    public void sendLookAt(Player player) {
        Vector difference = player.getLocation().subtract(location).toVector().normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-difference.getX(), difference.getZ()));
        float pitch = (float) Math.toDegrees(Math.atan2(-difference.getY(), Math.sqrt(difference.getX() * difference.getX() + difference.getZ() * difference.getZ())));

        sendLookAt(player, yaw, pitch);
    }

    public void stopLookingAt(Player player) {
        sendLookAt(player, location.getYaw(), location.getPitch());
    }

    private void sendLookAt(Player player, float yaw, float pitch) {
        WrapperPlayServerEntityHeadLook headLook = new WrapperPlayServerEntityHeadLook(
                entityId,
                yaw
        );

        WrapperPlayServerEntityRotation rotation = new WrapperPlayServerEntityRotation(
                entityId,
                yaw,
                pitch,
                true
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, headLook);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, rotation);
    }
}
