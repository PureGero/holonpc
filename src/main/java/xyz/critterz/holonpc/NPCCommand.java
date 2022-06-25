package xyz.critterz.holonpc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                delete(player, label, subargs);
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
                .append(Component.text(" <name> [x] [y] [z] [yaw] [pitch]").color(NamedTextColor.DARK_GREEN)));
        player.sendMessage(Component.text("/" + label + " delete").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("/" + label + " move").color(NamedTextColor.GREEN)
                .append(Component.text(" [x] [y] [z] [yaw] [pitch]").color(NamedTextColor.DARK_GREEN)));
        player.sendMessage(Component.text("/" + label + " select").color(NamedTextColor.GREEN));
    }

    private void create(Player player, String label, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /" + label + " create <name> [x] [y] [z] [yaw] [pitch]").color(NamedTextColor.RED));
            return;
        }

        String name = args[0];
        Location location = player.getLocation();
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

        NPC npc = new NPC(plugin, location.getWorld(), UUID.randomUUID(), name, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        plugin.getNPCManager().registerNPC(npc);
        npc.showToAllNearbyPlayers();

        player.sendMessage(Component.text("Created npc " + name).color(NamedTextColor.GREEN));
    }

    private void delete(Player player, String label, String[] args) {
        NPC npc = getSelectedNPC(player);

        if (npc == null) {
            player.sendMessage(Component.text("You must be standing next to an npc to delete them.").color(NamedTextColor.RED));
            return;
        }

        npc.hideFromAllNearbyPlayers();
        plugin.getNPCManager().unregisterNPC(npc);

        player.sendMessage(Component.text("Deleted npc " + npc.getPlayer().getName()).color(NamedTextColor.GREEN));
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
            if (npc.getPlayer().getWorld().equals(location.getWorld())) {
                double distance = npc.getPlayer().getLocation().distanceSquared(location);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestNPC = npc;
                }
            }
        }

        return nearestNPC;
    }
}
