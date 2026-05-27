package me.sekc.hoarder.gui;


import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseMenu {
	static boolean replaceConfigs = false; // turn this on for easy debug of default GUI configs

	public Hoarder plugin;
	public YamlConfiguration menuConfiguration; // this is set by the MenuManager
	protected int numCustomSlots = 0;

	protected class LayoutItem { // an item that has been parsed from a yaml file
		public Material material;
		public String id;
		public String name;
		public String lore;
		public boolean custom = false; // is it a `_`?
		public ItemStack customItemStack = ItemStack.empty(); // if this is set then it will return this instead of making an itemstack

		LayoutItem(Material material, String id, String name, String lore) {
			this.material = material;
			this.id = id;
			this.name = name;
			this.lore = lore;
		}
		LayoutItem(boolean custom) {
			this.custom = custom;
		}

		public ItemStack getItemStack() {
			if (!customItemStack.isEmpty()) return customItemStack;

			if (material == null) {
				return ItemStack.empty();
			}
			ItemStack item = ItemStack.of(material);

			ItemMeta meta = item.getItemMeta();

			if (name != null) {
				meta.customName(MiniMessage.miniMessage().deserialize(name));
			}

			if (lore != null) {
				List<Component> loreList = new ArrayList<>();
				for (String loreLine : lore.split("\\n")) {
					loreList.add(MiniMessage.miniMessage().deserialize(loreLine));
				}
				meta.lore(loreList);
			}

			item.setItemMeta(meta);
			return item;
		}
	}

	// The layout, contains an item for every slot in the custom gui
	//  "." = null
	//  "_" = LayoutItem.custom = true
	protected List<LayoutItem> layoutArray = new ArrayList<>();

	public BaseMenu(Hoarder plugin) {
		this.plugin = plugin;
	}

	// called by MenuManager, if YamlConfiguration is null then load it (either via `/clan admin reload`, or startup)
	// returns the new yaml configuration
	public YamlConfiguration initialiseAndUpdateYaml(YamlConfiguration configuration) {
		if (configuration == null) {
			File messagesFile = new File(plugin.getDataFolder(), getConfigPath());
			if (!messagesFile.exists()) {
				Logger.warn("Could not find " + messagesFile.getPath() + ", creating a new one from resources.");
				plugin.saveResource(getConfigPath(), false);
			}

			configuration = YamlConfiguration.loadConfiguration(messagesFile);

			final InputStream defConfigStream = plugin.getResource(getConfigPath());

			// set defaults from the messages.yml in resources
			configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
		}

		this.menuConfiguration = configuration;
		return configuration;
	}

	public String getConfigPath() { return ""; } // override me to have the yaml path

	public void fillContent(Player player, Inventory gui) {
		// Parse the items from yml
		String layoutString = menuConfiguration.getString("layout").strip();
		List<String> layoutChars = new ArrayList<>(Arrays.asList(layoutString.split("\\n| ")));

		numCustomSlots = 0;
		for (String layoutChar : layoutChars) {
			if (layoutChar.equals(".")) {
				layoutArray.add(null); // Nothing in this spot
				continue;
			}
			if (layoutChar.equals("_")) {
				numCustomSlots++;
				layoutArray.add(new LayoutItem(true)); // Custom item in this slot, override this func to customise these
				continue;
			}

			// Get the data for this item
			Material material = Material.valueOf(menuConfiguration.getString("items."+layoutChar+".material"));
			String name = menuConfiguration.getString("items."+layoutChar+".name");
			String id = menuConfiguration.getString("items."+layoutChar+".id");
			String lore = menuConfiguration.getString("items."+layoutChar+".lore");
			layoutArray.add(new LayoutItem(material, id, name, lore));
		}

		// Add the items to the inventory
		int idx = 0;
		for (LayoutItem layoutItem : layoutArray) {
			if (layoutItem == null || layoutItem.custom) {
				// Don't set this item here, if it is custom then override this func and iterate over layoutArray to fill the custom items yourself
				idx++;
				continue;
			}

			gui.setItem(idx, layoutItem.getItemStack());
			idx++;
		}
	}

	protected void layoutItemClicked(LayoutItem clickedItem, InventoryClickEvent e) {
		// Override me!

		// item ids that work everywhere!
		if (clickedItem.id != null) {
			if (clickedItem.id.equals("mainmenu")) {
				// in a clan, open the main menu
				((Player) e.getWhoClicked()).performCommand("hoarder gui");
			}
		}
	}

	public void handleClose(InventoryCloseEvent e) {
		// Override me!
	}

	public void itemClicked(InventoryClickEvent e) {
		e.setCancelled(true); // always cancel by default when clicking in UI (can be uncancelled if required)

		if (e.getSlot() >= layoutArray.size() || e.getSlot() < 0) {
			return; // clicking outside the inventory is slot -999
		}

		LayoutItem item = layoutArray.get(e.getSlot());

		if (item != null) {
			layoutItemClicked(item, e);
		}
	}

	public String getTitle() { return menuConfiguration.getString("title"); }

	public int slotIdToCustomSlotID(int slotID) {
		// translates a slot ID to the id of the custom slot
		int numCustomSlotsBeforeSlotID = 0;
		int curSlotID = 0;
		for (LayoutItem item : layoutArray) {
			if (curSlotID >= slotID) {
				return item.custom ? numCustomSlotsBeforeSlotID : -1; // -1 if this slot is not custom
			}

			if (item != null && item.custom)
				numCustomSlotsBeforeSlotID++;
			curSlotID++;
		}

		return -1;
	}

	public int customSlotIDToSlotID(int customSlotID) {
		// translates a slot ID to the id of the custom slot
		int numSlotsBeforeCustomSlotID = 0;
		int curCustomSlotID = 0;
		for (LayoutItem item : layoutArray) {
			if (curCustomSlotID >= customSlotID) {
				return numSlotsBeforeCustomSlotID;
			}

			if (item != null && item.custom)
				curCustomSlotID++;
			numSlotsBeforeCustomSlotID++;
		}

		return -1; // -1 if not enough custom slots
	}

	public interface SetItemInStorageRunnable {
		// custom slot id is the count of all "_" before this, not raw slotID from inventory (so u can have borders and such)
		public void set(ItemStack itemStack, int customSlotID);
	}
	protected void handleStorageClicked(LayoutItem clickedLayoutItem, InventoryClickEvent e, SetItemInStorageRunnable setItemInStorageRunnable) {
		// Use this function in LayoutItemClicked to automatically treat all "_" chars as storage space
		//  - when an item is taken or added to the inventory then it calls the Runnable with the itemstack and slotid (id ignoring non-"_" chars)
		//  - handles right&left click, with&without an item on the cursor
		//  - does not handle shift click as of right now (TODO)
		if (clickedLayoutItem.custom) {
			ItemStack clickedItemStack = e.getCurrentItem();

			int customSlotId = this.slotIdToCustomSlotID(e.getSlot());
			ItemStack cursor = e.getCursor();

			// Handle inventory
			if (e.isShiftClick()) {
				e.setCancelled(true); // don't allow any shift clicks
			} else if (customSlotId != -1) {
				if (clickedItemStack == null || clickedItemStack.isEmpty()) {
					if (!cursor.isEmpty()) {
						// An item has been dragged into the UI, add it and uncancel the event
						ItemStack newItemStack = cursor.clone();

						if (e.isRightClick()) {
							newItemStack.setAmount(1); // if right click then add 1
						}

						e.setCancelled(false);
						setItemInStorageRunnable.set(newItemStack, customSlotId);
					} else {
						// An empty slot has been clicked with nothing in cursor, do nothing
					}
				} else {
					if (!cursor.isEmpty()) {
						// An item has been dragged onto another item on the UI, add it and uncancel the event
						ItemStack newItemStack = cursor.clone();

						if (newItemStack.isSimilar(clickedItemStack)) {
							if (e.isRightClick()) {
								int newAmount = Math.clamp(clickedItemStack.getAmount()+1, 1, newItemStack.getMaxStackSize());
								newItemStack.setAmount(newAmount); // if right click and is same item then add 1
								e.setCancelled(false);
							}
							if (e.isLeftClick()) {
								int newAmount = Math.clamp(clickedItemStack.getAmount()+newItemStack.getAmount(), 1, newItemStack.getMaxStackSize());
								newItemStack.setAmount(newAmount); // if left click and is same item then add all that we can
								e.setCancelled(false);
							}
						}
						e.setCancelled(false);
						setItemInStorageRunnable.set(newItemStack, customSlotId); // swap items
					} else {
						if (e.isLeftClick()) {
							// An item is being removed from the UI, uncancel and remove it from the db
							e.setCancelled(false);
							setItemInStorageRunnable.set(ItemStack.empty(), customSlotId);
						} else if (e.isRightClick()) {
							// grab half of the item
							ItemStack newItemStack = clickedItemStack.clone();
							newItemStack.setAmount(newItemStack.getAmount()/2);
							e.setCancelled(false);
							setItemInStorageRunnable.set(newItemStack, customSlotId);
						}
					}
				}
			}
		}
	}
}