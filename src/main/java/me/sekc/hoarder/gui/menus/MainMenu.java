package me.sekc.hoarder.gui.menus;


import me.sekc.hoarder.DatabaseConnection;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.HoarderEventManager;
import me.sekc.hoarder.MessageFormatter;
import me.sekc.hoarder.gui.BaseMenu;
import me.sekc.hoarder.gui.MenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainMenu extends BaseMenu {
	public MainMenu(Hoarder plugin) {
		super(plugin);
	}

	@Override
	public String getConfigPath() {
		return "gui/mainmenu.yml";
	}

	@Override
	public void fillContent(Player player, Inventory gui) {
		super.fillContent(player, gui);

		int curIndex = 0;
		for (LayoutItem lItem : layoutArray) {
			if (lItem != null && lItem.id.equals("hoarder")) {
				HoarderEventManager.HoarderEvent currentEvent = HoarderEventManager.getCurrentEvent();
				if (currentEvent != null) {
					lItem.customItemStack = currentEvent.itemStack.clone();

					lItem.customItemStack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
					lItem.customItemStack.addUnsafeEnchantment(Enchantment.INFINITY, 1);

					PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
					String itemName = plainTextSerializer.serialize(Component.translatable(lItem.customItemStack));

					lItem.customItemStack.lore(MessageFormatter.getAndDeserialiseLines("gui.main-menu.feed-btn-lore", Map.ofEntries(
						Map.entry("%item_name%", itemName),
						Map.entry("%payout%", String.valueOf(currentEvent.awardMoney)),
						Map.entry("%pretty_time_remaining%", Duration.ofSeconds(currentEvent.endTime - (System.currentTimeMillis()/1000)).toString().substring(2).toLowerCase())
					)));
					gui.setItem(curIndex, lItem.getItemStack());
				}
			} else if (lItem != null && lItem.id.equals("playerstats")) {
				lItem.customItemStack = ItemStack.of(Material.PLAYER_HEAD);

				SkullMeta meta = (SkullMeta)lItem.customItemStack.getItemMeta();
				meta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
				meta.customName(MiniMessage.miniMessage().deserialize(lItem.name));
				lItem.customItemStack.setItemMeta(meta);

				DatabaseConnection.PlayerData playerData = plugin.dbConn.getPlayerFromDatabase(player.getUniqueId());

				lItem.customItemStack.lore(MessageFormatter.getAndDeserialiseLines("gui.main-menu.player-stats-lore", Map.ofEntries(
					Map.entry("%items_fed_this_event%", String.valueOf(playerData.itemsFedThisEvent)),
					Map.entry("%total_items_fed%", String.valueOf(playerData.itemsFedTotal))
				), player.getUniqueId()));

				gui.setItem(curIndex, lItem.getItemStack());
			} else if (lItem != null && lItem.id.equals("prizes")) {
				List<ItemStack> prizes = plugin.dbConn.getPrizesForPlayer(player.getUniqueId());

				ItemStack itemStack = lItem.getItemStack();

				if (!prizes.isEmpty()) {
					// they have won a prize!
					itemStack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
					itemStack.addUnsafeEnchantment(Enchantment.INFINITY, 1);

					itemStack.lore(MessageFormatter.getAndDeserialiseLines("gui.main-menu.prize-lore-won-prize", Map.ofEntries(
						Map.entry("%num_prizes_won%", String.valueOf(prizes.size()))
					)));
				}

				gui.setItem(curIndex, itemStack);
			} else if (lItem != null && lItem.id.startsWith("leaderboard_")) {
				int leaderboard_place = Integer.valueOf(lItem.id.split("_")[1]) - 1;

				// only contains one player because limit 1
				List<DatabaseConnection.PlayerData> leaderboard = plugin.dbConn.getLeaderboardFromPlayerColumn("items_fed_this_event", leaderboard_place, 1);

				if (!leaderboard.isEmpty()) {
					DatabaseConnection.PlayerData playerData = leaderboard.getFirst();
					lItem.customItemStack = ItemStack.of(Material.PLAYER_HEAD);

					SkullMeta meta = (SkullMeta) lItem.customItemStack.getItemMeta();
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerData.uuid);
					meta.setOwningPlayer(offlinePlayer);
					meta.customName(MessageFormatter.getAndDeserialise("gui.leaderboard.player-title", Map.ofEntries(
						Map.entry("%place%", String.valueOf(leaderboard_place+1)),
						Map.entry("%player_name%", offlinePlayer.getName())
					), null));

					lItem.customItemStack.setItemMeta(meta);

					lItem.customItemStack.lore(MessageFormatter.getAndDeserialiseLines("gui.leaderboard.player-lore", Map.ofEntries(
						Map.entry("%items_fed_this_event%", String.valueOf(playerData.itemsFedThisEvent)),
						Map.entry("%total_items_fed%", String.valueOf(playerData.itemsFedTotal))
					), player.getUniqueId()));

					gui.setItem(curIndex, lItem.getItemStack());
				}
			}
			curIndex++;
		}
	}

	@Override
	protected void layoutItemClicked(LayoutItem clickedItem, InventoryClickEvent e) {
		super.layoutItemClicked(clickedItem, e);

		if (clickedItem.id != null) {
			if (clickedItem.id.equals("hoarder")) {
				HoarderEventManager.HoarderEvent currentEvent = HoarderEventManager.getCurrentEvent();
				if (currentEvent != null)
					MenuManager.open(e.getWhoClicked(), new FeedMenu(plugin));
			}
			if (clickedItem.id.equals("leaderboard")) {
				MenuManager.open(e.getWhoClicked(), new LeaderboardMenu(plugin, LeaderboardMenu.SortType.FED_THIS_EVENT));
			}
			if (clickedItem.id.equals("prizes")) {
				List<ItemStack> prizes = plugin.dbConn.getPrizesForPlayer(e.getWhoClicked().getUniqueId());
				if (!prizes.isEmpty()) {
					int emptySlots = 0; // check player has space in inventory
					for (ItemStack item : e.getWhoClicked().getInventory().getStorageContents()) {
						if (item == null) {
							emptySlots++;
						}
					}

					if (emptySlots >= prizes.size()) {
						for (ItemStack prize : prizes) {
							e.getWhoClicked().getInventory().addItem(prize);
						}
						e.getWhoClicked().sendMessage(MessageFormatter.getAsChatMessageAndDeserialise("gui.main-menu.collected-prize"));

						plugin.dbConn.clearPlayerPrizes(e.getWhoClicked().getUniqueId());

						MenuManager.open(e.getWhoClicked(), new MainMenu(plugin)); // re-open main menu to refresh
					} else {
						e.getWhoClicked().sendMessage(MessageFormatter.getAsChatMessageAndDeserialise("gui.main-menu.could-not-collect-prize"));
					}

				}
			}
		}
	}
}
