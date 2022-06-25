package xyz.critterz.holonpc;

import com.github.puregero.multilib.MultiLib;

public class NPCSyncer {

    private final HoloNPCPlugin plugin;

    public NPCSyncer(HoloNPCPlugin plugin) {
        this.plugin = plugin;

        MultiLib.onString(plugin, "holonpc:create", string -> handleCreate(string));
    }

    private void handleCreate(String string) {
    }
}
