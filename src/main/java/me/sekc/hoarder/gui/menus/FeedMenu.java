package me.sekc.hoarder.gui.menus;


import me.sekc.hoarder.DatabaseConnection;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.HoarderEventManager;
import me.sekc.hoarder.MessageFormatter;
import me.sekc.hoarder.gui.BaseMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.time.Duration;
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

			if (lItem.custom) // initialise
				itemsInWindow.add(ItemStack.empty());

			if (lItem.id != null) {
				if (lItem.id.equals("hoarderitem")) {
					HoarderEventManager.HoarderEvent currentEvent = HoarderEventManager.getCurrentEvent();
					lItem.customItemStack = currentEvent.itemStack.clone();

					PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
					String itemName = plainTextSerializer.serialize(Component.translatable(lItem.customItemStack));

					lItem.customItemStack.lore(MessageFormatter.getAndDeserialiseLines("gui.feed.current-hoarder-item-lore", Map.ofEntries(
						Map.entry("%item_name%", itemName),
						Map.entry("%payout%", String.valueOf(currentEvent.awardMoney)),
						Map.entry("%pretty_time_remaining%", Duration.ofSeconds(currentEvent.endTime - (System.currentTimeMillis()/1000)).toString().substring(2).toLowerCase())
					)));
				}
			}

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
					// clear itemsInWindow with air
					itemsInWindow.set(customSlotIDToSlotID(index), ItemStack.empty());
					index++;
				}

				// add to db
				DatabaseConnection.PlayerData playerData = plugin.dbConn.getPlayerFromDatabase(e.getWhoClicked().getUniqueId());
				playerData.itemsFedTotal += numItemsfed;
				playerData.itemsFedThisEvent += numItemsfed;
				plugin.dbConn.updatePlayerInDatabase(e.getWhoClicked().getUniqueId(), playerData);

				// award money
				float payout = 0;
				if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
					RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
					if (rsp != null) {
						Economy econ = rsp.getProvider();
						if (econ != null) {
							HoarderEventManager.HoarderEvent currentEvent = HoarderEventManager.getCurrentEvent();
							payout = currentEvent.awardMoney * numItemsfed;
							econ.depositPlayer(Bukkit.getOfflinePlayer(e.getWhoClicked().getUniqueId()), payout);
						}
					}
				}

				e.getWhoClicked().sendMessage(
					MessageFormatter.getAsChatMessageAndDeserialise("gui.feed.fed", Map.ofEntries(
						Map.entry("%items_fed%", String.valueOf(numItemsfed)),
						Map.entry("%payout%", String.valueOf(payout))
					), e.getWhoClicked().getUniqueId())
				);
			}
		}
	}

	@Override
	protected void shiftClickInv(ItemStack clickedItem, InventoryClickEvent e) {
		super.shiftClickInv(clickedItem, e);

		HoarderEventManager.HoarderEvent event = HoarderEventManager.getCurrentEvent();

		if (event == null || !event.itemStack.isSimilar(clickedItem)) {
			// not in list, cancel and do nothing
			e.setCancelled(true);
			return;
		}


		ItemStack clonedItem = clickedItem.clone();

		// get the first available slot
		int firstEmptySpaceIndex = -1;
		int idx = 0;
		for (ItemStack item : itemsInWindow) {
			if (item.isEmpty()) {
				itemsInWindow.set(idx, clonedItem);
				e.getInventory().setItem(idx, clonedItem);
				e.setCurrentItem(ItemStack.empty());
				return;
			}
			if (item.isSimilar(clickedItem) && item.getAmount() < item.getMaxStackSize()) {
				int amountWantToTransfer = clonedItem.getAmount();
				int amountCanTransfer = item.getMaxStackSize() - item.getAmount();
				int amountToTransfer = Math.min(amountWantToTransfer, amountCanTransfer);


				item.setAmount(item.getAmount() + amountToTransfer);
				itemsInWindow.set(idx, item);
				e.getInventory().setItem(idx, item);
				clonedItem.setAmount(clonedItem.getAmount() - amountToTransfer);
			}
			idx++;
		}

		e.setCurrentItem(clickedItem);
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
