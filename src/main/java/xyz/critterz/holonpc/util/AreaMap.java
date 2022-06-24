package xyz.critterz.holonpc.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * Great for quickly accessing fairly static objects by coordinates. However,
 * currently not great if the objects move frequently.
 */
public class AreaMap<E> {

    private final Long2ObjectOpenHashMap<Set<E>> coordinatesToObjects = new Long2ObjectOpenHashMap<>();
    private final Object2LongOpenHashMap<E> objectToLastCoordinate = new Object2LongOpenHashMap<>();
    private final int range;

    public AreaMap(int range) {
        this.range = range;
    }

    @NotNull
    public Set<E> getObjects(int x, int z) {
        return coordinatesToObjects.getOrDefault(toLong(x, z), Collections.emptySet());
    }

    public void addObject(@NotNull E object, int x, int z) {
        if (objectToLastCoordinate.containsKey(object)) {
            removeObject(object);
        }

        objectToLastCoordinate.put(object, toLong(x, z));

        for (int dx = -range; dx <= range; dx ++) {
            for (int dz = -range; dz <= range; dz ++) {
                addObjectTo(object, toLong(x + dx, z + dz));
            }
        }
    }

    public void removeObject(@NotNull E object) {
        long lastCoordinate = objectToLastCoordinate.getLong(object);
        int x = longToX(lastCoordinate);
        int z = longToZ(lastCoordinate);

        for (int dx = -range; dx <= range; dx ++) {
            for (int dz = -range; dz <= range; dz ++) {
                removeObjectFrom(object, toLong(x + dx, z + dz));
            }
        }
    }

    private void addObjectTo(E object, long key) {
        Set<E> objects = coordinatesToObjects.computeIfAbsent(key, key2 -> new ObjectLinkedOpenHashSet<>());
        objects.add(object);
    }

    private void removeObjectFrom(E object, long key) {
        Set<E> objects = coordinatesToObjects.get(key);

        if (objects == null) {
            throw new IllegalStateException("Set of objects is null at " + key + " when removing object " + object);
        }

        if (!objects.remove(object)) {
            throw new IllegalStateException("Set of objects at " + key + " did not contain object " + object);
        }

        if (objects.isEmpty()) {
            coordinatesToObjects.remove(key);
        }
    }

    private static long toLong(int x, int z) {
        return ((long) z << 32) | (x & 0xFFFFFFFFL);
    }

    private static int longToX(long key) {
        return (int) key;
    }

    private static int longToZ(long key) {
        return (int) (key >>> 32);
    }
}
