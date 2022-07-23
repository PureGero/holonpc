package xyz.critterz.holonpc;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class NPCCommand implements CommandExecutor {

    private final HoloNPCPlugin plugin;
    private final HashMap<UUID, UUID> selectedNPCs = new HashMap<>();

    public NPCCommand(HoloNPCPlugin plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("npc")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return false;
        }

        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase();
        String[] subargs = new String[Math.max(0, args.length - 1)];
        if (args.length > 1) {
            System.arraycopy(args, 1, subargs, 0, subargs.length);
        }

        try {
            if (subcommand.equals("create")) {
                create(player, label, subargs);
            } else if (subcommand.equals("delete") || subcommand.equals("remove")) {
                remove(player, label, subargs);
            } else {
                sendHelp(player, label);
                return false;
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage()).color(NamedTextColor.RED));
            e.printStackTrace();
        }

        return true;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(Component.text(" --- --- === HoloNPC Commands === --- ---").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("/" + label + " create").color(NamedTextColor.GREEN)
                .append(Component.text(" [name] [x] [y] [z] [yaw] [pitch] [skinUrl]").color(NamedTextColor.DARK_GREEN)));
        player.sendMessage(Component.text("/" + label + " remove").color(NamedTextColor.GREEN));
    }

    private void create(Player player, String label, String[] args) {
        if (!player.hasPermission("npc.create")) {
            player.sendMessage(Component.text("Insufficient permissions").color(NamedTextColor.RED));
            return;
        }

        String name = null;
        String skinUrl = null;
        Location location = player.getLocation();

        for (int i = 0; i < args.length; i++) {
            if (args[i].toLowerCase().startsWith("http") && args[i].contains("://")) {
                skinUrl = args[i];
                String[] oldArgs = args;
                args = new String[oldArgs.length - 1];
                System.arraycopy(oldArgs, 0, args, 0, i);
                System.arraycopy(oldArgs, i + 1, args, i, args.length - i);
                break;
            }
        }

        if (args.length >= 1) {
            name = args[0];
        }
        if (args.length >= 4) {
            location.setX(parseCenteredDouble(args[1]));
            location.setY(Double.parseDouble(args[2]));
            location.setZ(parseCenteredDouble(args[3]));
            location.setYaw(0);
            location.setPitch(0);
        }
        if (args.length >= 6) {
            location.setYaw(Float.parseFloat(args[4]));
            location.setPitch(Float.parseFloat(args[5]));
        }

        if (name != null && name.length() > 16) {
            player.sendMessage(Component.text("Name too long: " + name).color(NamedTextColor.RED));
            return;
        }

        NPC npc = new NPC(plugin, location, UUID.randomUUID(), name);
        plugin.getNPCManager().registerNPC(npc);
        npc.showToAllNearbyPlayers();
        plugin.getConfigLoader().addNPC(npc);

        player.sendMessage(Component.text("Created npc " + name).color(NamedTextColor.GREEN));

        if (skinUrl != null) {
            if (!player.hasPermission("npc.setskin")) {
                player.sendMessage(Component.text("Insufficient permissions to set NPC skin").color(NamedTextColor.RED));
                return;
            }

            setSkin(player, npc, skinUrl);
        }
    }

    private void remove(Player player, String label, String[] args) {
        if (!player.hasPermission("npc.remove")) {
            player.sendMessage(Component.text("Insufficient permissions").color(NamedTextColor.RED));
            return;
        }

        NPC npc = getSelectedNPC(player);

        if (npc == null) {
            player.sendMessage(Component.text("You must be standing next to an npc to delete them.").color(NamedTextColor.RED));
            return;
        }

        npc.hideFromAllNearbyPlayers();
        plugin.getNPCManager().unregisterNPC(npc);
        plugin.getConfigLoader().removeNPC(npc);

        player.sendMessage(Component.text("Removed npc " + npc.getName()).color(NamedTextColor.GREEN));
    }

    private double parseCenteredDouble(String number) {
        if (number.contains(".")) {
            return Double.parseDouble(number);
        } else {
            return Integer.parseInt(number) + 0.5;
        }
    }

    private NPC getSelectedNPC(Player player) {
        UUID selectedUUID = selectedNPCs.remove(player.getUniqueId());

        if (selectedUUID != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.sendMessage(Component.text("Unselected NPC").color(NamedTextColor.GREEN)), 1);
            return plugin.getNPCManager().getNPC(selectedUUID);
        }

        return getNearestNPC(player.getLocation());
    }

    private NPC getNearestNPC(Location location) {
        Iterable<NPC> nearbyNPCs = plugin.getNPCManager().getNearNPCs(location);

        NPC nearestNPC = null;
        double nearestDistance = Double.MAX_VALUE;

        for (NPC npc : nearbyNPCs) {
            if (npc.getLocation().getWorld().equals(location.getWorld())) {
                double distance = npc.getLocation().distanceSquared(location);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestNPC = npc;
                }
            }
        }

        return nearestNPC;
    }

    public void setSkin(Player player, NPC npc, String url) {
        try {
            Class.forName("net.skinsrestorer.api.SkinsRestorerAPI"); // Check SkinsRestorer is installed
            player.sendMessage(Component.text("Downloading skin...").color(NamedTextColor.GREEN));
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    IProperty property = SkinsRestorerAPI.getApi().genSkinUrl(url, null);
                    npc.setTexture(new TextureProperty(property.getName(), property.getValue(), property.getSignature()));
                    npc.hideFromAllNearbyPlayers();
                    npc.showToAllNearbyPlayers();
                    plugin.getConfigLoader().removeNPC(npc);
                    plugin.getConfigLoader().addNPC(npc);
                    player.sendMessage(Component.text("Skin has been applied to NPC " + npc.getName()).color(NamedTextColor.GREEN));
                } catch (SkinRequestException e) {
                    e.printStackTrace();
                    player.sendMessage(Component.text("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage()).color(NamedTextColor.RED));
                }
            });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            player.sendMessage(Component.text("You must have SkinsRestorer installed to set NPC skins from a url").color(NamedTextColor.RED));
        }
    }
}
