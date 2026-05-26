package me.sekc.hoarder.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuManager {
	static Map<UUID, BaseMenu> playerToOpenGUIMap = new HashMap<>();
	static Map<String, YamlConfiguration> menuConfigurationCache = new HashMap<>();

	static public void open(Entity entityToOpenGUI, BaseMenu menu) {
		// load yaml if not in cache
		menuConfigurationCache.putIfAbsent(
			menu.getConfigPath(),
			menu.initialiseAndUpdateYaml( // load if null
				menuConfigurationCache.getOrDefault(menu.getConfigPath(), null)
			)
		);

		if (entityToOpenGUI instanceof Player player) {
			closeInventory(player);

			playerToOpenGUIMap.put(player.getUniqueId(), menu);

			Inventory gui = Bukkit.getServer().createInventory(player, 9*6, Component.text(menu.getTitle()).color(TextColor.color(255, 255, 255)));

			menu.fillContent(player, gui); // get the initial content of the menu

			player.openInventory(gui);
		} else {
			Logger.warn("tried to open GUI for non-player.");
		}
	}

	static public void clearMenuConfigCache() {
		menuConfigurationCache.clear();
	}

	static public void closeInventory(Entity playerToCloseGUI) {
		if (playerToCloseGUI instanceof Player player) {
			player.closeInventory(); // Close any currently open gui
		}
	}

	public interface ActionAfterTypingRunnable {
		public void run(String message);
	}
	static Map<UUID, ActionAfterTypingRunnable> playerToTypeInChatMap = new HashMap<>();

	static public void performActionAfterTyping(UUID playerUUID, ActionAfterTypingRunnable action) {
		playerToTypeInChatMap.put(playerUUID, action);
	}

	static public void onClick(InventoryClickEvent e) {
		BaseMenu openGUI = playerToOpenGUIMap.get(e.getWhoClicked().getUniqueId());

		if (openGUI != null) {
			//Clans.log("rawSlot: " + e.getRawSlot() + "   slot: " + e.getSlot());

			if (e.getClickedInventory() == null)
				return; // Let the player click around their own inventory

			if (e.getClickedInventory().getType().equals(InventoryType.PLAYER)) {
				if (e.isShiftClick()) e.setCancelled(true); // don't allow shift clicks in inventory
				return; // Don't send clicks in own inventory to GUIs
			}

			openGUI.itemClicked(e);
		}
	}

	static public void onDrag(InventoryDragEvent e) {
		BaseMenu openGUI = playerToOpenGUIMap.get(e.getWhoClicked().getUniqueId());

		if (openGUI != null) {
			e.setCancelled(true); // don't allow any inventory drag when a GUI is open
		}
	}

	static public void onInventoryClose(InventoryCloseEvent e) {
		BaseMenu openGUI = playerToOpenGUIMap.get(e.getPlayer().getUniqueId());
		if (openGUI != null) {
			openGUI.handleClose(e);
			playerToOpenGUIMap.remove(e.getPlayer().getUniqueId()); // no UI open anymore
		}
	}

	static public void onChat(AsyncChatEvent e) {
		UUID playerUUID = e.getPlayer().getUniqueId();
		ActionAfterTypingRunnable runnable = playerToTypeInChatMap.get(playerUUID);

		if (runnable != null) {
			e.setCancelled(true); // Don't send to public chat

			String plainTextMessage = PlainTextComponentSerializer.plainText().serialize(e.message());
			new BukkitRunnable() { // Make sure it runs on main thread
				@Override
				public void run() {
					runnable.run(plainTextMessage);
				}
			}.runTask(Hoarder.getPlugin(Hoarder.class));
		}

		playerToTypeInChatMap.remove(playerUUID); // now ran, remove from map
	}
}