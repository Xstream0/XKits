package com.xkits.kit;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class Kit {

    private final String name;
    private final List<ItemStack> items;
    private final String permission;
    private final long cooldownSeconds;

    public Kit(String name, List<ItemStack> items, String permission, long cooldownSeconds) {
        this.name = Objects.requireNonNull(name, "name");
        this.items = List.copyOf(items);
        this.permission = permission == null ? "" : permission.trim();
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
    }

    public String getName() {
        return name;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public String getPermission() {
        return permission;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }
}
