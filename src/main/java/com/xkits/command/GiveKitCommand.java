package com.xkits.command;

import com.xkits.kit.KitManager;
import com.xkits.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class GiveKitCommand implements CommandExecutor {

    private final KitManager manager;

    public GiveKitCommand(KitManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = manager.getPlugin().getConfig().getString("messages.prefix", "&8[&bxkit&8] &r");
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.format(prefix,
                    manager.getPlugin().getConfig().getString("messages.usageGive", "&7Uso: /kitgive <player> <kit>"),
                    Map.of()));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.format(prefix,
                    manager.getPlugin().getConfig().getString("messages.playerOffline", "&cGiocatore non online."),
                    Map.of()));
            return true;
        }

        String kitName = args[1];
        manager.redeemKitAsync(target, kitName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(MessageUtil.format(prefix,
                        manager.getPlugin().getConfig().getString("messages.kitGiven", "&aKit dato: &e%kit% &aper %player%"),
                        Map.of("kit", kitName, "player", target.getName())));
                target.sendMessage(MessageUtil.format(prefix,
                        manager.getPlugin().getConfig().getString("messages.receivedKit", "&aHai ricevuto il kit &e%kit%&a!"),
                        Map.of("kit", kitName)));
            } else {
                sender.sendMessage(MessageUtil.format(prefix,
                        manager.getPlugin().getConfig().getString("messages.giveFail", "&cImpossibile riscattare il kit: permessi mancanti o cooldown attivo."),
                        Map.of()));
            }
        });
        return true;
    }
}
