package me.sekc.hoarder.gui.menus;


import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.gui.BaseMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class FeedMenu extends BaseMenu {
	public FeedMenu(Hoarder plugin) {
		super(plugin);
	}

	@Override
	public String getConfigPath() {
		return "gui/feedmenu.yml";
	}

	@Override
	public void fillContent(Player player, Inventory gui) {
		super.fillContent(player, gui);
	}

	@Override
	protected void layoutItemClicked(LayoutItem clickedItem, InventoryClickEvent e) {
		super.layoutItemClicked(clickedItem, e);

		if (clickedItem.id != null) {
			if (clickedItem.id.equals("hoarder")) {
				//MenuManager.open(e.getWhoClicked(), new HoarderSellMenu(plugin));
			}
		}
	}
}
