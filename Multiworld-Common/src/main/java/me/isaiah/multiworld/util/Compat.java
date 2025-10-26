package me.isaiah.multiworld.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Cross-version compatibility helpers for accessing fields/methods that
 * are renamed across mappings.
 */
public final class Compat {

    private Compat() {}

    /**
     * Get the World for a ServerPlayerEntity across mappings.
     */
    public static World getWorld(ServerPlayerEntity plr) {
        // Try common method names first
        World w = (World) invokeNoArg(plr, "getWorld");
        if (w != null) return w;

        w = (World) invokeNoArg(plr, "getEntityWorld");
        if (w != null) return w;

        // Known obf name from crash log on some versions
        w = (World) invokeNoArg(plr, "method_5770");
        if (w != null) return w;

        // Fallback to Entity.world field
        return getWorldField(plr);
    }

    /**
     * Get the World for a PlayerEntity across mappings.
     */
    public static World getWorld(PlayerEntity plr) {
        World w = (World) invokeNoArg(plr, "getWorld");
        if (w != null) return w;

        w = (World) invokeNoArg(plr, "getEntityWorld");
        if (w != null) return w;

        // Possible obf on older/newer versions
        w = (World) invokeNoArg(plr, "method_5770");
        if (w != null) return w;

        return getWorldField(plr);
    }

    /**
     * Attempt to return a ServerWorld if available; otherwise tries to cast.
     */
    public static ServerWorld getServerWorld(ServerPlayerEntity plr) {
        Object res = invokeNoArg(plr, "getServerWorld");
        if (res instanceof ServerWorld sw) return sw;
        World w = getWorld(plr);
        return (ServerWorld) w; // may throw if not a server context
    }

    private static Object invokeNoArg(Object target, String name) {
        try {
            Method m = target.getClass().getMethod(name);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static World getWorldField(Object entity) {
        try {
            // Field is defined on net.minecraft.entity.Entity as 'world' in many mappings
            Field f = findField(entity.getClass(), "world");
            if (f != null) {
                f.setAccessible(true);
                Object o = f.get(entity);
                if (o instanceof World) return (World) o;
            }
        } catch (Throwable ignored) {}
        throw new IllegalStateException("Unable to resolve World from entity across mappings");
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}

