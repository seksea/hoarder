package me.sekc.hoarder.gui.menus;


import me.sekc.hoarder.DatabaseConnection;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.HoarderEventManager;
import me.sekc.hoarder.MessageFormatter;
import me.sekc.hoarder.gui.BaseMenu;
import me.sekc.hoarder.gui.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LeaderboardMenu extends BaseMenu {
	public enum SortType {
		FED_THIS_EVENT(0, "items_fed_this_event", "Items fed during current event"),
		TOTAL_ITEMS_FED(1, "total_items_fed", "Total items fed in all events");

		public final int index;
		public final String columnName;
		public final String readableName;
		private SortType(int index, String columnName, String readableName) {
			this.index = index;
			this.columnName = columnName;
			this.readableName = readableName;
		}

		public static SortType atIndex(int index) {
			return values()[index];
		}
	}
	SortType sortType; // column in database to sort
	public LeaderboardMenu(Hoarder plugin, SortType sortType) {
		super(plugin);
		this.sortType = sortType;
	}

	@Override
	public String getConfigPath() {
		return "gui/leaderboardmenu.yml";
	}

	@Override
	public void fillContent(Player player, Inventory gui) {
		super.fillContent(player, gui);

		List<DatabaseConnection.PlayerData> leaderboard = plugin.dbConn.getLeaderboardFromPlayerColumn(sortType.columnName, 0, numCustomSlots);

		int index = 0;
		int customIndex = 0;
		for (LayoutItem lItem : layoutArray) {
			if (lItem != null) {
				if (lItem.custom) {
					if (leaderboard.size() > customIndex) {
						DatabaseConnection.PlayerData playerData = leaderboard.get(customIndex);
						OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerData.uuid);
						lItem.customItemStack = ItemStack.of(Material.PLAYER_HEAD);

						SkullMeta meta = (SkullMeta)lItem.customItemStack.getItemMeta();
						meta.setOwningPlayer(offlinePlayer);
						meta.customName(MessageFormatter.getAndDeserialise("gui.leaderboard.player-title", Map.ofEntries(
							Map.entry("%place%", String.valueOf(customIndex+1)),
							Map.entry("%player_name%", offlinePlayer.getName())
						), null));

						lItem.customItemStack.setItemMeta(meta);

						lItem.customItemStack.lore(MessageFormatter.getAndDeserialiseLines("gui.leaderboard.player-lore", Map.ofEntries(
							Map.entry("%items_fed_this_event%", String.valueOf(playerData.itemsFedThisEvent)),
							Map.entry("%total_items_fed%", String.valueOf(playerData.itemsFedTotal))
						), player.getUniqueId()));
					}

					customIndex++;
				}

				if (lItem.id != null) {
					if (lItem.id.equals("changesortby")) {
						lItem.lore = sortType.readableName;
					}
				}

				gui.setItem(index, lItem.getItemStack());
			}
			index++;
		}
	}

	@Override
	protected void layoutItemClicked(LayoutItem clickedItem, InventoryClickEvent e) {
		super.layoutItemClicked(clickedItem, e);

		if (clickedItem.id != null) {
			if (clickedItem.id.equals("changesortby")) {
				MenuManager.open(e.getWhoClicked(), new LeaderboardMenu(this.plugin, SortType.atIndex((this.sortType.index + 1) % 2)));
			}
		}
	}
}
