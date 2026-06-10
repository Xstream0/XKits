package com.xkits.command;

import com.xkits.kit.KitManager;
import com.xkits.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class DeleteKitCommand implements CommandExecutor {

    private final KitManager manager;

    public DeleteKitCommand(KitManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = manager.getPlugin().getConfig().getString("messages.prefix", "&8[&bxkit&8] &r");
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.format(prefix,
                    manager.getPlugin().getConfig().getString("messages.usageDelete", "&7Uso: /kitdelete <name>"),
                    Map.of()));
            return true;
        }

        String name = args[0];
        manager.deleteKitAsync(name).thenRun(() -> sender.sendMessage(MessageUtil.format(prefix,
                manager.getPlugin().getConfig().getString("messages.kitDeleted", "&cKit eliminato: &e%kit%"),
                Map.of("kit", name))));
        return true;
    }
}
