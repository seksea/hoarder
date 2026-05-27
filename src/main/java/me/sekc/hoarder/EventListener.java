package me.sekc.hoarder;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.sekc.hoarder.gui.MenuManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class EventListener implements Listener {
    Hoarder plugin;

    EventListener(Hoarder plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        if (!plugin.dbConn.playerExists(playerUUID)) {
            Logger.log("Player " + event.getPlayer().getName() + " joined for the first time, registering them in the database.");
            plugin.dbConn.createPlayer(playerUUID);
        }
    }

    @EventHandler
    public void OnInventoryClick(InventoryClickEvent e) {
        MenuManager.onClick(e);
    }

    @EventHandler
    public void OnInventoryDrag(InventoryDragEvent e) {
        MenuManager.onDrag(e);
    }

    @EventHandler
    public void OnInventoryClose(InventoryCloseEvent e) {
        MenuManager.onInventoryClose(e);
    }

    @EventHandler
    public void OnPlayer(AsyncChatEvent e) {
        MenuManager.onChat(e);
    }
}
