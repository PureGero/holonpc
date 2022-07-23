package xyz.critterz.holonpc;

import com.github.puregero.multilib.MultiLib;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.provider.PlayerDataProvider;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class NPC {

    private final HoloNPCPlugin plugin;
    private final UUID uuid;
    private final String name;
    private final HashMap<String, TextureProperty> textures = new HashMap<>();
    private final int entityId;
    private Location location;
    private final HashSet<UUID> lookingAt = new HashSet<>();
    private final List<Pair<Plugin, BiConsumer<NPC, Player>>> lookAtListeners = new ArrayList<>();
    private final List<Pair<Plugin, BiConsumer<NPC, Player>>> clickListeners = new ArrayList<>();
    private final HashMap<EquipmentSlot, Equipment> equipment = new HashMap<>();

    public NPC(HoloNPCPlugin plugin, Location location, UUID uuid, @Nullable String name) {
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
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Location getLocation() {
        return location;
    }

    public void setEquipment(org.bukkit.inventory.EquipmentSlot slot, @Nullable ItemStack item) {
        EquipmentSlot packetSlot = EquipmentSlot.values()[slot.ordinal()];
        if (item == null) {
            equipment.remove(packetSlot);
        } else {
            Equipment equipment = new Equipment(packetSlot, SpigotConversionUtil.fromBukkitItemStack(item));
            this.equipment.put(packetSlot, equipment);
        }
    }

    public Map<String, TextureProperty> getTextures() {
        return textures;
    }

    public void setTexture(String key, String value, @Nullable String signature) {
        setTexture(new TextureProperty(key, value ,signature));
    }

    public void setTexture(TextureProperty textureProperty) {
        textures.put(textureProperty.getName(), textureProperty);
    }

    public void showTo(Player player) {
        // Set skin and username
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.ADD_PLAYER,
                new WrapperPlayServerPlayerInfo.PlayerData(Component.text(name), new UserProfile(uuid, name, new ArrayList<>(textures.values())), GameMode.SURVIVAL, 0)
        ));

        // Spawn entity
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerSpawnPlayer(
                entityId,
                uuid,
                SpigotConversionUtil.fromBukkitLocation(location)
        ));

        // Turn the head
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityHeadLook(
            entityId,
            location.getYaw()
        ));

        // Show second layer of skin
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityMetadata(
                entityId,
                new PlayerDataProvider.PlayerBuilder<>(new PlayerDataProvider()).skinPartsMask((byte) 126).build().encode()
        ));

        // Equipment
        if (!equipment.isEmpty()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityEquipment(
                    entityId,
                    new ArrayList<>(equipment.values())
            ));
        }

        // Remove npc from tab list after 5 seconds, after the player has spawned in
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPlayerInfo(
                        WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER,
                        new WrapperPlayServerPlayerInfo.PlayerData(Component.text(name), new UserProfile(uuid, name, new ArrayList<>(textures.values())), GameMode.SURVIVAL, 0)
                )),
        100);
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
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerDestroyEntities(
                entityId
        ));
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

        if (lookingAt.add(player.getUniqueId())) {
            dispatchLookAtEvent(player);
        }

        sendLookAt(player, yaw, pitch);
    }

    public void stopLookingAt(Player player) {
        lookingAt.remove(player.getUniqueId());
        sendLookAt(player, location.getYaw(), location.getPitch());
    }

    private void sendLookAt(Player player, float yaw, float pitch) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityHeadLook(
                entityId,
                yaw
        ));

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerEntityRotation(
                entityId,
                yaw,
                pitch,
                true
        ));
    }

    public void addClickListener(Plugin plugin, BiConsumer<NPC, Player> onClick) {
        clickListeners.add(new ObjectObjectImmutablePair<>(plugin, onClick));
    }

    public void dispatchClickEvent(Player player) {
        dispatchEventToListeners(player, clickListeners);
    }

    public void addLookAtListener(Plugin plugin, BiConsumer<NPC, Player> onClick) {
        lookAtListeners.add(new ObjectObjectImmutablePair<>(plugin, onClick));
    }

    public void dispatchLookAtEvent(Player player) {
        dispatchEventToListeners(player, lookAtListeners);
    }

    private void dispatchEventToListeners(Player player, List<Pair<Plugin, BiConsumer<NPC, Player>>> listeners) {
        for (Pair<Plugin, BiConsumer<NPC, Player>> pair : listeners) {
            try {
                if (pair.left().isEnabled()) {
                    pair.right().accept(this, player);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
