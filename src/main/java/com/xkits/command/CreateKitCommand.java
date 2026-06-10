package com.xkits.command;

import com.xkits.kit.Kit;
import com.xkits.kit.KitManager;
import com.xkits.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateKitCommand implements CommandExecutor {

    private final KitManager kitManager;

    public CreateKitCommand(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = kitManager.getPlugin().getConfig().getString("messages.prefix", "&8[&bxkit&8] &r");
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.commandOnlyInGame", "&cQuesto comando è disponibile solo in gioco."),
                    Map.of()));
            return true;
        }

        if (args.length < 2) {
            p.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.usageCreate", "&7Uso: /kitcreate <name> <cooldown_seconds> [permission]"),
                    Map.of()));
            return true;
        }

        String name = args[0];
        long cooldown;
        try {
            cooldown = MessageUtil.parseDuration(args[1]);
        } catch (IllegalArgumentException e) {
            p.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.invalidNumber", "&cCooldown non valido. Usa 10d, 5h, 30m o 120"),
                    Map.of()));
            return true;
        }

        String perm = args.length >= 3 ? args[2] : "";
        List<ItemStack> items = Arrays.stream(p.getInventory().getContents())
                .filter(item -> item != null && item.getType().isAir() == false)
                .map(ItemStack::clone)
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            p.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.inventoryEmpty", "&cL'inventario è vuoto, non è stato salvato alcun kit."),
                    Map.of()));
            return true;
        }

        Kit kit = new Kit(name, items, perm, cooldown);
        kitManager.saveKitAsync(kit).thenRun(() -> p.sendMessage(MessageUtil.format(prefix,
                kitManager.getPlugin().getConfig().getString("messages.kitSaved", "&aKit salvato: &e%kit%"),
                Map.of("kit", name))));
        return true;
    }
}
