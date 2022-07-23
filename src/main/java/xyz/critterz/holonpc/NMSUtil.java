package xyz.critterz.holonpc;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public class NMSUtil {

//    public static Player createPlayer(Location location, UUID uuid, String name) {
//        try {
//            Object serverLevel = getServerLevel(location.getWorld());
//            Object minecraftServer = getMinecraftServer(serverLevel);
//            Object serverPlayer = constructPlayer(minecraftServer, serverLevel, new GameProfile(uuid, name));
//            absMoveTo(serverPlayer, location);
//            return getBukkitEntity(serverPlayer);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private static Class<?> getMinecraftServerClass() throws Exception {
//        return Class.forName("net.minecraft.server.MinecraftServer"); // Both Mojang mappings and Obfuscated
//    }
//
//    private static Object getServerLevel(Object bukkitWorld) throws Exception {
//        return bukkitWorld.getClass().getMethod("getHandle").invoke(bukkitWorld);
//    }
//
//    private static Object getMinecraftServer(Object worldServer) throws Exception {
//        Class<?> minecraftServerClass = getMinecraftServerClass();
//        for (Method method : worldServer.getClass().getMethods()) {
//            if (method.getParameterCount() == 0 && method.getReturnType().isAssignableFrom(minecraftServerClass)) {
//                return method.invoke(worldServer);
//            }
//        }
//        throw new RuntimeException("Could not find method MinecraftServer getServer() for " + worldServer);
//    }
//
//    private static Object constructPlayer(Object minecraftServer, Object serverLevel, GameProfile gameProfile) throws Exception {
//        Constructor<?> constructor = getServerPlayerClass().getDeclaredConstructors()[0];
//        if (constructor.getParameterCount() == 3) {
//            // MinecraftServer, ServerLevel, GameProfile (1.18)
//            return constructor.newInstance(minecraftServer, serverLevel, gameProfile);
//        } else if (constructor.getParameterCount() == 4) {
//            // MinecraftServer, ServerLevel, GameProfile, ProfilePublicKey (1.19)
//            return constructor.newInstance(minecraftServer, serverLevel, gameProfile, null);
//        } else {
//            throw new RuntimeException("Can not handle ServerPlayer constructor " + constructor);
//        }
//    }
//
//    private static Class<?> getServerPlayerClass() throws Exception {
//        try {
//            return Class.forName("net.minecraft.server.level.ServerPlayer"); // Mojang mappings
//        } catch (ClassNotFoundException e) {
//            return Class.forName("net.minecraft.server.level.EntityPlayer"); // Obfuscated
//        }
//    }
//
//    private static Player getBukkitEntity(Object serverPlayer) throws Exception {
//        return (Player) serverPlayer.getClass().getMethod("getBukkitEntity").invoke(serverPlayer);
//    }

}
