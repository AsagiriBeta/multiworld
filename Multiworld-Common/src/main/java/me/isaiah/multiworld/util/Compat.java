package me.isaiah.multiworld.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class Compat {

    private Compat() {}

    public static World getWorld(ServerPlayerEntity plr) {
        // Preferred: find any public no-arg method returning World/ServerWorld
        World w = (World) findNoArgReturning(plr.getClass(), plr, World.class);
        if (w != null) return w;
        w = (World) findNoArgReturning(plr.getClass(), plr, ServerWorld.class);
        if (w != null) return w;
        // Fallback: field of type World/ServerWorld
        w = getWorldFromField(plr);
        if (w != null) return w;
        throw new IllegalStateException("Unable to resolve World from entity across mappings");
    }

    public static World getWorld(PlayerEntity plr) {
        World w = (World) findNoArgReturning(plr.getClass(), plr, World.class);
        if (w != null) return w;
        w = (World) findNoArgReturning(plr.getClass(), plr, ServerWorld.class);
        if (w != null) return w;
        w = getWorldFromField(plr);
        if (w != null) return w;
        throw new IllegalStateException("Unable to resolve World from entity across mappings");
    }

    public static ServerWorld getServerWorld(ServerPlayerEntity plr) {
        Object o = findNoArgReturning(plr.getClass(), plr, ServerWorld.class);
        if (o instanceof ServerWorld sw) return sw;
        World w = getWorld(plr);
        return (ServerWorld) w;
    }

    private static Object findNoArgReturning(Class<?> start, Object target, Class<?> returnType) {
        Class<?> c = start;
        while (c != null && c != Object.class) {
            for (Method m : c.getMethods()) {
                try {
                    if (m.getParameterCount() == 0 && returnType.isAssignableFrom(m.getReturnType())) {
                        m.setAccessible(true);
                        Object res = m.invoke(target);
                        if (res != null) return res;
                    }
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static World getWorldFromField(Object entity) {
        Class<?> c = entity.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    if (World.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object o = f.get(entity);
                        if (o instanceof World w) return w;
                    }
                } catch (Throwable ignored) {}
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
