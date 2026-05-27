package me.sekc.hoarder.gui.menus;


import me.sekc.hoarder.DatabaseConnection;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.HoarderEventManager;
import me.sekc.hoarder.MessageFormatter;
import me.sekc.hoarder.gui.BaseMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedMenu extends BaseMenu {
	public FeedMenu(Hoarder plugin) {
		super(plugin);
	}

	List<ItemStack> itemsInWindow = new ArrayList<>();

	@Override
	public String getConfigPath() {
		return "gui/feedmenu.yml";
	}

	@Override
	public void fillContent(Player player, Inventory gui) {
		super.fillContent(player, gui);

		itemsInWindow.clear();
		int index = 0;
		for (LayoutItem lItem : layoutArray) {
			if (lItem == null) {
				index++;
				continue;
			}

			if (lItem.custom) // initialise itemsInFurnace
				itemsInWindow.add(ItemStack.empty());

			gui.setItem(index, lItem.getItemStack());
			index++;
		}
	}

	@Override
	protected void layoutItemClicked(LayoutItem clickedItem, InventoryClickEvent e) {
		super.layoutItemClicked(clickedItem, e);

		// handle putting and taking items from this inventory (only "_" chars in the gui yml) and keep in itemsIn
		super.handleStorageClicked(clickedItem, e, (itemStack, customSlotID) -> {

			if (!itemStack.isEmpty()) {
				HoarderEventManager.HoarderEvent event = HoarderEventManager.getCurrentEvent();

				if (event == null || !event.itemStack.isSimilar(itemStack)) {
					// not in list, cancel and do nothing
					e.setCancelled(true);
					return;
				}
			}

			itemsInWindow.set(customSlotID, itemStack);
		});

		if (clickedItem.id != null) {
			if (clickedItem.id.equals("feed")) {
				int numItemsfed = 0;
				int index = 0;
				for (ItemStack item : itemsInWindow) {
					if (!item.isEmpty()) {
						numItemsfed += item.getAmount();
						e.getInventory().setItem(customSlotIDToSlotID(index), ItemStack.empty());
					}
					itemsInWindow.set(customSlotIDToSlotID(index), ItemStack.empty());
					index++;
				}

				DatabaseConnection.PlayerData playerData = plugin.dbConn.getPlayerFromDatabase(e.getWhoClicked().getUniqueId());
				playerData.itemsFedTotal += numItemsfed;
				playerData.itemsFedThisEvent += numItemsfed;
				plugin.dbConn.updatePlayerInDatabase(e.getWhoClicked().getUniqueId(), playerData);

				e.getWhoClicked().sendMessage(
					MessageFormatter.getAsChatMessageAndDeserialise("gui.feed.fed", Map.ofEntries(
						Map.entry("%items_fed%", String.valueOf(numItemsfed))
					), e.getWhoClicked().getUniqueId())
				);
			}
		}
	}

	@Override
	public void handleClose(InventoryCloseEvent e) {
		super.handleClose(e);

		// return items to player
		for (ItemStack item : itemsInWindow) {
			// return items in furnace back to inventory
			e.getView().getBottomInventory().addItem(item);
		}
	}
}
