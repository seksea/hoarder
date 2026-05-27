package me.sekc.hoarder;

import me.sekc.hoarder.gui.MenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class HoarderEventManager {
	static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	static ScheduledFuture<?> endTaskFuture = null;

	public static class HoarderEvent {
		public ItemStack itemStack; // The item the hoarder wants (uses .isSimilar) to compare

		public long endTime; // When the event will end (unix epoch seconds)

		public HoarderEvent(ItemStack itemStack, long endTime) {
			this.itemStack = itemStack;
			this.endTime = endTime;
		}
	}
	static HoarderEvent currentEvent = null; // null = no event

	static public void startNewHoarderEvent(HoarderEvent newEvent, boolean saveToDatabase) {
		Hoarder plugin = Hoarder.getPlugin(Hoarder.class);

		if (currentEvent != null) {
			throw new RuntimeException("Hoarder tried to start a new event without ending the previous event first!!");
		}
		currentEvent = newEvent;

		// How many seconds until the event should end
		long curSeconds = System.currentTimeMillis() / 1000;
		long secondsUntilEnd = currentEvent.endTime - curSeconds;

		if (secondsUntilEnd <= 0) {
			// The event already ended (probably because the server was offline during the event ending)
			Logger.warn("Started hoarder event that has already ended (" + Math.abs(secondsUntilEnd) + "sec ago), this probably just means your server was offline during the end, will end it now.");
			secondsUntilEnd = 1;
		}

		// cancel old task (schedule() should be a one-off, but seems to be repeating?? wtf)
		if (endTaskFuture != null) {
			endTaskFuture.cancel(true);
		}

		// Schedule ending the event
		endTaskFuture = scheduler.schedule(() -> {endHoarderEvent();}, secondsUntilEnd, TimeUnit.SECONDS);

		if (saveToDatabase) {
			// save it in the database for reboot persistence
			plugin.dbConn.setCurrentHoarderEvent(currentEvent);
		}

		// broadcast in chat
		PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
		String itemName = plainTextSerializer.serialize(Component.translatable(currentEvent.itemStack));
		Hoarder.broadcastIfEnabled(MessageFormatter.getAsChatMessageAndDeserialise("event.hoarder-starting", Map.ofEntries(
			Map.entry("%item_name%", itemName)
		), null));

		MenuManager.closeAllGUIs(); // cheap refresh hack
	}

	static public HoarderEvent startRandomHoarderEvent() {
		Hoarder plugin = Hoarder.getPlugin(Hoarder.class);
		int numItems = plugin.dbConn.getNumHoarderItems();
		ItemStack item = plugin.dbConn.getHoarderItemAtIndex(ThreadLocalRandom.current().nextInt(0, numItems));

		long curSeconds = System.currentTimeMillis() / 1000;
		startNewHoarderEvent(new HoarderEvent(item, curSeconds + ConfigurationManager.getLong("event.event-length-seconds")), true);

		return currentEvent;
	}

	static public void endHoarderEvent(boolean allowStartNextEvent) {
		if (currentEvent == null) {
			throw new RuntimeException("Hoarder tried to end the event when there was no event running!!");
		}

		Hoarder plugin = Hoarder.getPlugin(Hoarder.class);

		// Give top 3 players rewards TODO

		// clear event
		currentEvent = null;

		// make sure to clear the event so if we restart the server with
		// "start-random-event-on-completion: false", it won't award it twice
		plugin.dbConn.clearCurrentHoarderEvent();

		// broadcast in chat
		List<DatabaseConnection.PlayerData> leaderboard = plugin.dbConn.getLeaderboardFromPlayerColumn("items_fed_this_event", 0, 5);
		if (leaderboard.isEmpty()) {
			Hoarder.broadcastIfEnabled(MessageFormatter.getAsChatMessageAndDeserialise("event.hoarder-ending-no-participants"));
		} else {
			Hoarder.broadcastIfEnabled(MessageFormatter.getAsChatMessageAndDeserialise("event.hoarder-ending"));
			for (int i = 0; i < Math.min(leaderboard.size(), 3); i++) {
				Hoarder.broadcastIfEnabled(MessageFormatter.getAsChatMessageAndDeserialise("event.hoarder-ending-leaderboard-line", Map.ofEntries(
					Map.entry("%leaderboard_place%", String.valueOf(i+1)),
					Map.entry("%player_name%", Bukkit.getOfflinePlayer(leaderboard.get(i).uuid).getName()),
					Map.entry("%num_items_fed%", String.valueOf(leaderboard.get(i).itemsFedThisEvent))
				), null));
			}
		}

		// reset "item_fed_this_event"
		plugin.dbConn.resetAllPlayersItemsFed();

		if (allowStartNextEvent && ConfigurationManager.config.getBoolean("event.start-random-event-if-none-running")){
			startRandomHoarderEvent();
		}

		MenuManager.closeAllGUIs(); // cheap refresh hack
	}
	static public void endHoarderEvent() {
		endHoarderEvent(true);
	}

	static public HoarderEvent getCurrentEvent() {
		return currentEvent;
	}
}
