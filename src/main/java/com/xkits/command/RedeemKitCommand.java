package com.xkits.command;

import com.xkits.kit.Kit;
import com.xkits.kit.KitManager;
import com.xkits.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class RedeemKitCommand implements CommandExecutor {

    private final KitManager kitManager;
    private final JavaPlugin plugin;

    public RedeemKitCommand(JavaPlugin plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = kitManager.getPlugin().getConfig().getString("messages.prefix", "&8[&bxkit&8] &r");
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.commandOnlyInGame", "&cQuesto comando è disponibile solo in gioco."),
                    Map.of()));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.usageRedeem", "&7Uso: /kit <kit>"),
                    Map.of()));
            return true;
        }

        String kitName = args[0];
        Optional<Kit> optionalKit = kitManager.getKit(kitName);
        if (optionalKit.isEmpty()) {
            player.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.kitNotFound", "&cKit non trovato: &e%kit%"),
                    Map.of("kit", kitName)));
            return true;
        }

        Kit kit = optionalKit.get();
        long now = Instant.now().getEpochSecond();
        OptionalLong expiry = kitManager.getPlayerKitExpiry(player.getUniqueId(), kit.getName());
        if (expiry.isPresent() && expiry.getAsLong() > now) {
            player.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.kitRedeemFailCooldown", "&cNon puoi riscattare il kit &e%kit%&c ora. Riprova tra &e%countdown%&c."),
                    Map.of("kit", kit.getName(), "countdown", MessageUtil.formatDuration(expiry.getAsLong() - now))));
            return true;
        }

        if (kit.getPermission() != null && !kit.getPermission().isBlank() && !player.hasPermission(kit.getPermission())) {
            player.sendMessage(MessageUtil.format(prefix,
                    kitManager.getPlugin().getConfig().getString("messages.kitNoPermission", "&cNon hai il permesso per riscattare il kit &e%kit%&c."),
                    Map.of("kit", kit.getName())));
            return true;
        }

        kitManager.redeemKitAsync(player, kit.getName()).thenAccept(success -> {
            if (success) {
                player.sendMessage(MessageUtil.format(prefix,
                        kitManager.getPlugin().getConfig().getString("messages.kitRedeemed", "&aHai riscattato il kit &e%kit%&a!"),
                        Map.of("kit", kit.getName())));
            } else {
                player.sendMessage(MessageUtil.format(prefix,
                        kitManager.getPlugin().getConfig().getString("messages.kitRedeemFail", "&cNon puoi riscattare il kit ora (permesso/cooldown)."),
                        Map.of()));
            }
        });
        return true;
    }
}
