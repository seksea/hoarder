package me.sekc.hoarder;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DatabaseConnection {
	public String sanitiseString(String input) {
		// prepared statements don't allow custom table names, so needs to be sanitised manually
		return input.replace("'", "").replace(";", "").replace("\"", "").replace("\\", "");
	}
	Connection connection;

	DatabaseConnection(String databasePath) throws Exception {
		Logger.log("Loading SQLite database at \"" + databasePath + "\"...");

		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection(databasePath);

		if (connection != null) {
			var meta = connection.getMetaData();
			Logger.log("Database driver: " + meta.getDriverName());
		}

		connection.createStatement().executeUpdate(
			"CREATE TABLE IF NOT EXISTS current_event ("
				+ "	  id 			  INTEGER PRIMARY KEY CHECK (id = 1)," // enforces only one row
				+ "   item_name		  VARCHAR(256)," // The items' name
				+ "   data  		  TEXT,"
				+ "   award_money   FLOAT NOT NULL DEFAULT 0,"
				+ "   end_time        INTEGER(64)" // unix timestamp
				+ ")"
		);

		connection.createStatement().executeUpdate(
			"CREATE TABLE IF NOT EXISTS players ("
				+ "   uuid                  VARCHAR(36) NOT NULL PRIMARY KEY,"
				+ "   total_items_fed      	INTEGER NOT NULL DEFAULT 0,"
				+ "   items_fed_this_event 	INTEGER NOT NULL DEFAULT 0,"
				+ "   pending_prizes 		TEXT" // list of items in bin form
				+ ")"
		);

		connection.createStatement().executeUpdate(
			"CREATE TABLE IF NOT EXISTS hoarder_items ("
				+ "   item_name		VARCHAR(256) NOT NULL PRIMARY KEY," // The items name
				+ "   data          TEXT NOT NULL," // Binary data of this item
				+ "   award_money   FLOAT NOT NULL DEFAULT 0,"
				+ "   deleted       BOOL NOT NULL DEFAULT FALSE"
				+ ")"
		);

		connection.createStatement().executeUpdate(
			"CREATE TABLE IF NOT EXISTS hoarder_prizes ("
				+ "   item_name		VARCHAR(256) NOT NULL UNIQUE," // The items name
				+ "   data          TEXT NOT NULL," // Binary data of this item
				+ "   deleted       BOOL NOT NULL DEFAULT FALSE"
				+ ")"
		);
	}

	/* *************************************
	  Hoarder Items (items the hoarder wants)
	**************************************** */

	public void addItemToHoarderItems(ItemStack itemStack, float awardMoney) {
		itemStack.setAmount(1);

		PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
		String itemName = plainTextSerializer.serialize(Component.translatable(itemStack));

		try {

			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO hoarder_items (item_name, data, award_money) VALUES (?, ?, ?)"
			);
			stmt.setString(1, itemName);
			stmt.setBytes(2, itemStack.serializeAsBytes());
			stmt.setFloat(3, awardMoney);
			stmt.executeUpdate();
		} catch (SQLException e) {
			try {
				if (e.getErrorCode() == 19) { // UNIQUE (means already exists, undelete or update this item)
					PreparedStatement stmt = connection.prepareStatement(
						"UPDATE hoarder_items SET data=?, deleted=false WHERE item_name=?"
					);
					stmt.setBytes(1, itemStack.serializeAsBytes()); // make sure to set the data incase it changed
					stmt.setString(2, itemName);
					stmt.executeUpdate();
				} else {
					throw  new RuntimeException(e);
				}
			} catch (SQLException ee) {
				throw new RuntimeException(ee);
			}
		}
	}

	public int getNumHoarderItems() { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT COUNT(1) FROM hoarder_items"
			);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.isBeforeFirst()) return 0; // no results
				return results.getInt(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static class HoarderItem {
		public ItemStack stack;
		public float awardMoney;

		HoarderItem(ItemStack stack, float awardMoney) {
			this.stack = stack;
			this.awardMoney = awardMoney;
		}
	}
	public HoarderItem getHoarderItemAtIndex(int index) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT data, award_money FROM hoarder_items WHERE rowid=? AND deleted=false"
			);
			stmt.setInt(1, index+1);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.isBeforeFirst()) return null; // no results
				return new HoarderItem(ItemStack.deserializeBytes(results.getBytes("data")), results.getFloat("award_money")); // serialise to b64 bytes
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public String getHoarderItemNameAtIndex(int index) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT item_name FROM hoarder_items WHERE rowid=? AND deleted=false"
			);
			stmt.setInt(1, index+1);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.isBeforeFirst()) return null; // no results
				return results.getString("item_name");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteHoarderItemFromItemName(String name) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"UPDATE hoarder_items SET deleted=true WHERE item_name=?"
			);
			stmt.setString(1, name);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/* *************************************
	  Hoarder Prizes
	**************************************** */

	public void addItemToHoarderPrizes(ItemStack itemStack) {
		itemStack.setAmount(1);

		PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
		String itemName = plainTextSerializer.serialize(Component.translatable(itemStack));

		try {
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO hoarder_prizes (item_name, data) VALUES (?, ?)"
			);

			stmt.setString(1, itemName);
			stmt.setBytes(2, itemStack.serializeAsBytes());
			stmt.executeUpdate();
		} catch (SQLException e) {
			try {
				if (e.getErrorCode() == 19) { // UNIQUE (means already exists, undelete or update this item)
					PreparedStatement stmt = connection.prepareStatement(
						"UPDATE hoarder_prizes SET data=?, deleted=false WHERE item_name=?"
					);
					stmt.setBytes(1, itemStack.serializeAsBytes()); // make sure to set the data incase it changed
					stmt.setString(2, itemName);
					stmt.executeUpdate();
				} else {
					throw  new RuntimeException(e);
				}
			} catch (SQLException ee) {
				throw new RuntimeException(ee);
			}
		}
	}

	public int getNumHoarderPrizes() { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT COUNT(1) FROM hoarder_prizes"
			);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.isBeforeFirst()) return 0; // no results
				return results.getInt(1);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public ItemStack getHoarderPrizeAtIndex(int index) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT data FROM hoarder_prizes WHERE rowid=? AND deleted=false"
			);
			stmt.setInt(1, index+1);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.isBeforeFirst()) return null; // no results
				return ItemStack.deserializeBytes(results.getBytes("data")); // serialise to b64 bytes
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public String getHoarderPrizeNameAtIndex(int index) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT item_name FROM hoarder_prizes WHERE rowid=? AND deleted=false"
			);
			stmt.setInt(1, index+1);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.isBeforeFirst()) return null; // no results
				return results.getString("item_name");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteHoarderPrizeFromItemName(String name) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"UPDATE hoarder_prizes SET deleted=true WHERE item_name=?"
			);
			stmt.setString(1, name);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/* *************************************
	  Players
	**************************************** */

	public void createPlayer(UUID playerUUID) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO players (uuid) VALUES (?)"
			);
			stmt.setString(1, playerUUID.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean playerExists(UUID uuid) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT 1 FROM players WHERE uuid=?"
			);
			stmt.setString(1, uuid.toString());
			try (ResultSet results = stmt.executeQuery()) {
				return results.isBeforeFirst(); // false if empty
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static class PlayerData {
		public UUID uuid;
		public int itemsFedThisEvent;
		public int itemsFedTotal;

		PlayerData(UUID uuid, int itemsFedThisEvent, int itemsFedTotal) {
			this.uuid = uuid;
			this.itemsFedThisEvent = itemsFedThisEvent;
			this.itemsFedTotal = itemsFedTotal;
		}
	}
	public PlayerData getPlayerFromDatabase(UUID uuid) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT total_items_fed, items_fed_this_event FROM players WHERE uuid=?"
			);
			stmt.setString(1, uuid.toString());
			try (ResultSet results = stmt.executeQuery()) {
				return new PlayerData(
					uuid,
					results.getInt("items_fed_this_event"),
					results.getInt("total_items_fed")
				);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void updatePlayerInDatabase(UUID uuid, PlayerData data) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"UPDATE players SET total_items_fed=?, items_fed_this_event=? WHERE uuid=?"
			);
			stmt.setInt(1, data.itemsFedTotal);
			stmt.setInt(2, data.itemsFedThisEvent);
			stmt.setString(3, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void resetAllPlayersItemsFed() {
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"UPDATE players SET items_fed_this_event=0"
			);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<ItemStack> getPrizesForPlayer(UUID uuid) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT pending_prizes FROM players WHERE uuid=?"
			);
			stmt.setString(1, uuid.toString());
			try (ResultSet results = stmt.executeQuery()) {
				byte[] pendingPrizes = results.getBytes("pending_prizes");
				if (pendingPrizes == null) {
					return new ArrayList<>();
				}
				return new ArrayList<>(Arrays.asList(ItemStack.deserializeItemsFromBytes(pendingPrizes)));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void addPrizeToPlayer(UUID uuid, ItemStack prize) { // returns null if no item
		try {
			List<ItemStack> curPrizes = getPrizesForPlayer(uuid);
			curPrizes.add(prize);

			PreparedStatement stmt = connection.prepareStatement(
				"UPDATE players SET pending_prizes=? WHERE uuid=?"
			);
			stmt.setBytes(1, ItemStack.serializeItemsAsBytes(curPrizes));
			stmt.setString(2, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void clearPlayerPrizes(UUID uuid) { // returns null if no item
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"UPDATE players SET pending_prizes=null WHERE uuid=?"
			);
			stmt.setString(1, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/* *************************************
	  Leaderboard
	**************************************** */

	public List<PlayerData> getLeaderboardFromPlayerColumn(String sortColumn, int offset, int limit) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT uuid, total_items_fed, items_fed_this_event FROM players ORDER BY " + sanitiseString(sortColumn) + " DESC LIMIT " + limit + " OFFSET " + offset
			);
			try (ResultSet results = stmt.executeQuery()) {
				List<PlayerData> resultArr = new ArrayList<>();
				while (results.next()) {
					int value = results.getInt(sortColumn);
					if (value == 0) // if the value you're sorting the leaderboard by is 0 then don't add to result
						continue;

					resultArr.add(new PlayerData(
						UUID.fromString(results.getString("uuid")),
						results.getInt("items_fed_this_event"),
						results.getInt("total_items_fed")
					));
				}
				return resultArr;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/* *************************************
	  Hoarder Events
	**************************************** */

	public HoarderEventManager.HoarderEvent getCurrentHoarderEvent() {
		// Only use this for persisting events when restarting the server,
		// you can use the HoarderEventManager instead to get the current event from memory
		try {
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT item_name, data, award_money, end_time FROM current_event"
			);
			try (ResultSet results = stmt.executeQuery()) {
				if (!results.isBeforeFirst()) // false if empty
					return null;

				return new HoarderEventManager.HoarderEvent(
					ItemStack.deserializeBytes(results.getBytes("data")),
					results.getLong("end_time"),
					results.getFloat("award_money")
				);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void clearCurrentHoarderEvent() {
		try {
			HoarderEventManager.HoarderEvent oldEvent = getCurrentHoarderEvent();

			PreparedStatement stmt = connection.prepareStatement(
				"DELETE FROM current_event WHERE id=1"
			);

			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}


	public void setCurrentHoarderEvent(HoarderEventManager.HoarderEvent event) {
		// Only use this for persisting events when restarting the server,
		// you can use the HoarderEventManager instead to set the current event
		// in memory (that will also set it in the database)
		try {
			HoarderEventManager.HoarderEvent oldEvent = getCurrentHoarderEvent();

			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO current_event (item_name, data, end_time, award_money) VALUES (?, ?, ?, ?)"
			);

			PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
			String itemName = plainTextSerializer.serialize(Component.translatable(event.itemStack));

			stmt.setString(1, itemName);
			stmt.setBytes(2, event.itemStack.serializeAsBytes());
			stmt.setLong(3, event.endTime);
			stmt.setFloat(4, event.awardMoney);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}