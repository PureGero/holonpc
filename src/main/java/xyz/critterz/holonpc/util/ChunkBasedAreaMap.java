package xyz.critterz.holonpc.util;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ChunkBasedAreaMap<E> extends AreaMap<E> {

    public ChunkBasedAreaMap(int range) {
        super(range >> 4);
    }

    @Override
    @NotNull
    public Set<E> getObjects(int x, int z) {
        return super.getObjects(x >> 4, z >> 4);
    }

    @Override
    public void addObject(@NotNull E object, int x, int z) {
        super.addObject(object, x >> 4, z >> 4);
    }
}
