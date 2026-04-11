package org.sRandomRTP.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PlayerResourceMap<V> extends AbstractMap<Player, V> {
    private final ConcurrentHashMap<UUID, V> backing = new ConcurrentHashMap<>();

    @Override
    public V put(Player key, V value) {
        if (key == null || value == null) {
            return null;
        }
        return backing.put(key.getUniqueId(), value);
    }

    public V put(UUID playerId, V value) {
        if (playerId == null || value == null) {
            return null;
        }
        return backing.put(playerId, value);
    }

    @Override
    public V get(Object key) {
        UUID uuid = extractUuid(key);
        if (uuid == null) {
            return null;
        }
        return backing.get(uuid);
    }

    public V get(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return backing.get(playerId);
    }

    @Override
    public V remove(Object key) {
        UUID uuid = extractUuid(key);
        if (uuid == null) {
            return null;
        }
        return backing.remove(uuid);
    }

    public V remove(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return backing.remove(playerId);
    }

    @Override
    public boolean containsKey(Object key) {
        UUID uuid = extractUuid(key);
        return uuid != null && backing.containsKey(uuid);
    }

    public boolean containsKey(UUID playerId) {
        return playerId != null && backing.containsKey(playerId);
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public Collection<V> values() {
        return backing.values();
    }

    @Override
    public void clear() {
        backing.clear();
    }

    /**
     * Returns a snapshot of online-player entries.
     * <p><strong>Must be called from the primary server thread.</strong>
     * {@code Bukkit.getPlayer(UUID)} is not thread-safe off the main thread.</p>
     */
    @Override
    public Set<Entry<Player, V>> entrySet() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "PlayerResourceMap.entrySet() must be called from the primary server thread. " +
                    "Use forEachUUID() for off-thread iteration.");
        }
        Set<Entry<Player, V>> entries = new HashSet<>();
        backing.forEach((uuid, value) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                entries.add(new SimpleEntry<>(player, value));
            }
        });
        return entries;
    }

    /**
     * Iterates over all entries whose player is currently online.
     * More efficient than {@link #entrySet()} because it avoids creating a temporary Set.
     * <p><strong>Must be called from the primary server thread.</strong></p>
     */
    public void forEachOnline(BiConsumer<UUID, V> action) {
        backing.forEach((uuid, value) -> {
            if (Bukkit.getPlayer(uuid) != null) {
                action.accept(uuid, value);
            }
        });
    }

    /**
     * Iterates over all UUID→value pairs without resolving online-player objects.
     * Safe to call from any thread (no Bukkit API involved).
     * Use this instead of {@link #forEachOnline} when you only need the UUID (e.g. in async tasks).
     */
    public void forEachUUID(BiConsumer<UUID, V> action) {
        backing.forEach(action);
    }

    public void clear(UUID playerId) {
        if (playerId != null) {
            backing.remove(playerId);
        }
    }

    private UUID extractUuid(Object key) {
        if (key instanceof UUID) {
            return (UUID) key;
        }
        if (key instanceof Player) {
            return ((Player) key).getUniqueId();
        }
        if (key instanceof String) {
            Player player = Bukkit.getPlayerExact((String) key);
            return player != null ? player.getUniqueId() : null;
        }
        return null;
    }
}
