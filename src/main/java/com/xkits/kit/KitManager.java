package com.xkits.kit;

import com.xkits.db.MySQLManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class KitManager {

    private final JavaPlugin plugin;
    private final MySQLManager mysql;

    private final Map<String, Kit> kits = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    public KitManager(JavaPlugin plugin, MySQLManager mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public CompletableFuture<Void> initializeAsync() {
        return loadAllKitsAsync().thenCompose(v -> loadPlayerCooldownsAsync());
    }

    public Optional<Kit> getKit(String name) {
        return Optional.ofNullable(kits.get(name.toLowerCase()));
    }

    public CompletableFuture<Void> saveKitAsync(Kit kit) {
        kits.put(kit.getName().toLowerCase(), kit);
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = mysql.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "REPLACE INTO xkits_kits (name, items, permission, cooldown) VALUES (?, ?, ?, ?)")) {

                ps.setString(1, kit.getName().toLowerCase());
                ps.setString(2, itemStackArrayToBase64(kit.getItems().toArray(new ItemStack[0])));
                ps.setString(3, kit.getPermission());
                ps.setLong(4, kit.getCooldownSeconds());
                ps.executeUpdate();

            } catch (SQLException | IOException e) {
                plugin.getLogger().severe("Errore salvando kit: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> deleteKitAsync(String name) {
        kits.remove(name.toLowerCase());
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = mysql.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM xkits_kits WHERE name = ?")) {
                ps.setString(1, name.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Errore eliminando kit: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> renameKitAsync(String oldName, String newName) {
        Optional<Kit> opt = getKit(oldName);
        if (opt.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Kit non trovato: " + oldName));
        }

        Kit oldKit = opt.get();
        Kit renamed = new Kit(newName, oldKit.getItems(), oldKit.getPermission(), oldKit.getCooldownSeconds());
        return saveKitAsync(renamed).thenCompose(v -> deleteKitAsync(oldName));
    }

    public List<Kit> getAllKits() {
        return new ArrayList<>(kits.values());
    }

    public CompletableFuture<Void> loadAllKitsAsync() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = mysql.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM xkits_kits");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String name = rs.getString("name");
                    String itemsData = rs.getString("items");
                    String perm = rs.getString("permission");
                    long cooldown = rs.getLong("cooldown");

                    ItemStack[] items = itemStackArrayFromBase64(itemsData);
                    Kit kit = new Kit(name, Arrays.asList(items), perm, cooldown);
                    kits.put(name.toLowerCase(), kit);
                }

            } catch (SQLException | IOException | ClassNotFoundException e) {
                plugin.getLogger().severe("Errore caricando i kit: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> loadPlayerCooldownsAsync() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = mysql.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT uuid, cooldowns FROM xkits_player_cooldowns");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String cooldownsData = rs.getString("cooldowns");
                    if (cooldownsData == null || cooldownsData.isBlank()) {
                        continue;
                    }
                    Map<String, Long> cooldowns = deserializeMap(Base64.getDecoder().decode(cooldownsData));
                    playerCooldowns.put(uuid, cooldowns);
                }

            } catch (SQLException | IOException | ClassNotFoundException e) {
                plugin.getLogger().severe("Errore caricando i cooldown dei giocatori: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Boolean> redeemKitAsync(Player player, String kitName) {
        Optional<Kit> opt = getKit(kitName);
        if (opt.isEmpty()) return CompletableFuture.completedFuture(false);
        Kit kit = opt.get();

        if (kit.getPermission() != null && !kit.getPermission().isBlank()) {
            if (!player.hasPermission(kit.getPermission())) return CompletableFuture.completedFuture(false);
        }

        UUID uid = player.getUniqueId();
        long now = Instant.now().getEpochSecond();
        Map<String, Long> pcd = playerCooldowns.computeIfAbsent(uid, k -> new HashMap<>());
        long expiry = pcd.getOrDefault(kit.getName().toLowerCase(), 0L);
        if (expiry > now) return CompletableFuture.completedFuture(false);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (ItemStack item : kit.getItems()) {
                player.getInventory().addItem(item.clone());
            }
        });

        long newExpiry = now + kit.getCooldownSeconds();
        pcd.put(kit.getName().toLowerCase(), newExpiry);

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = mysql.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "REPLACE INTO xkits_player_cooldowns (uuid, cooldowns) VALUES (?, ?)")) {

                ps.setString(1, uid.toString());
                ps.setString(2, Base64.getEncoder().encodeToString(serializeMap(pcd)));
                ps.executeUpdate();

            } catch (SQLException | IOException e) {
                plugin.getLogger().severe("Errore salvando cooldown giocatore: " + e.getMessage());
            }
        }).thenApply(v -> true);
    }

    public OptionalLong getPlayerKitExpiry(UUID uuid, String kitName) {
        Map<String, Long> map = playerCooldowns.get(uuid);
        if (map == null) return OptionalLong.empty();
        Long expiry = map.get(kitName.toLowerCase());
        return (expiry != null) ? OptionalLong.of(expiry) : OptionalLong.empty();
    }

    private String itemStackArrayToBase64(ItemStack[] items) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
        boos.writeInt(items.length);
        for (ItemStack item : items) {
            boos.writeObject(item);
        }
        boos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private ItemStack[] itemStackArrayFromBase64(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
        int length = bois.readInt();
        ItemStack[] items = new ItemStack[length];
        for (int i = 0; i < length; i++) {
            items[i] = (ItemStack) bois.readObject();
        }
        bois.close();
        return items;
    }

    private byte[] serializeMap(Map<String, Long> map) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
        boos.writeObject(map);
        boos.close();
        return baos.toByteArray();
    }

    private Map<String, Long> deserializeMap(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
        Map<String, Long> map = (Map<String, Long>) bois.readObject();
        bois.close();
        return map;
    }
}
