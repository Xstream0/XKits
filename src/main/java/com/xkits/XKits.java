package com.xkits;

import com.xkits.command.CreateKitCommand;
import com.xkits.command.DeleteKitCommand;
import com.xkits.command.GiveKitCommand;
import com.xkits.command.RenameKitCommand;
import com.xkits.command.RedeemKitCommand;
import com.xkits.db.MySQLManager;
import com.xkits.kit.KitManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class XKits extends JavaPlugin {

    private MySQLManager mysql;
    private KitManager kitManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String user = getConfig().getString("mysql.user");
        String pass = getConfig().getString("mysql.password");

        mysql = new MySQLManager(this, host, port, database, user, pass);
        kitManager = new KitManager(this, mysql);

        getCommand("kitcreate").setExecutor(new CreateKitCommand(kitManager));
        getCommand("kitdelete").setExecutor(new DeleteKitCommand(kitManager));
        getCommand("kitrename").setExecutor(new RenameKitCommand(kitManager));
        getCommand("kitgive").setExecutor(new GiveKitCommand(kitManager));
        getCommand("kit").setExecutor(new RedeemKitCommand(this, kitManager));

        mysql.createTablesAsync()
                .thenCompose(v -> kitManager.initializeAsync())
                .thenRun(() -> getLogger().info("✅ XKits abilitato e dati caricati!"))
                .exceptionally(error -> {
                    getLogger().severe("Errore inizializzazione XKits: " + error.getMessage());
                    return null;
                });
    }

    @Override
    public void onDisable() {
        if (mysql != null) {
            mysql.disconnect();
        }
    }

    public KitManager getKitManager() {
        return kitManager;
    }
}
