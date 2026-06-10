package com.xkits.command;

import com.xkits.kit.KitManager;
import com.xkits.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class RenameKitCommand implements CommandExecutor {

    private final KitManager kitManager;

    public RenameKitCommand(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = kitManager.getPlugin().getConfig().getString("messages.prefix", "&8[&bxkit&8] &r");
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.usageRename", "&7Uso: /kitrename <oldName> <newName>"),
                    Map.of()));
            return true;
        }

        String oldName = args[0];
        String newName = args[1];

        kitManager.renameKitAsync(oldName, newName).thenRun(() -> sender.sendMessage(MessageUtil.format(prefix,
                kitManager.getPlugin().getConfig().getString("messages.kitRenamed", "&eKit rinominato: &6%old% &7-> &6%new%"),
                Map.of("old", oldName, "new", newName))))
                .exceptionally(error -> {
                    Throwable cause = error.getCause() != null ? error.getCause() : error;
                    sender.sendMessage(MessageUtil.format(prefix,
                            kitManager.getPlugin().getConfig().getString("messages.errorMessage", "&c%error%"),
                            Map.of("error", cause.getMessage())));
                    return null;
                });
        return true;
    }
}
