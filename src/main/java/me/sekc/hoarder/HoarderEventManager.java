package me.sekc.hoarder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HoarderEventManager {
	public static class HoarderEvent {
		ItemStack itemStack; // The item the hoarder wants (uses .isSimilar) to compare

		long endTime; // When the event will end (unix epoch seconds)

		HoarderEvent(ItemStack itemStack, long endTime) {
			this.itemStack = itemStack;
			this.endTime = endTime;
		}
	}
	static HoarderEvent currentEvent = null; // null = no event

	static public void startNewHoarderEvent(HoarderEvent newEvent) {
		currentEvent = newEvent;

		// How many seconds until the event should end
		long curSeconds = System.currentTimeMillis() / 1000;
		long secondsUntilEnd = currentEvent.endTime - curSeconds;

		if (secondsUntilEnd <= 0) {
			// The event already ended (probably because the server was offline during the event ending)
			Logger.warn("Started hoarder event that has already ended, this probably just means your server was offline during the end, will end it now.");
			secondsUntilEnd = 0;
		}

		// broadcast in chat
		Hoarder.broadcastIfEnabled(MessageFormatter.getAndDeserialise("event.hoarder-starting"));

		// Schedule ending the event
		Executors.newScheduledThreadPool(1).schedule(HoarderEventManager::endHoarderEvent, secondsUntilEnd, TimeUnit.SECONDS);
	}

	static public HoarderEvent startRandomHoarderEvent() {
		// TODO: pick random item from the database
		long curSeconds = System.currentTimeMillis() / 1000;

		startNewHoarderEvent(new HoarderEvent(ItemStack.of(Material.GRASS_BLOCK), curSeconds + ConfigurationManager.getLong("event.event-length-seconds")));

		return currentEvent;
	}

	static public void endHoarderEvent() {
		currentEvent = null;

		// broadcast in chat
		Hoarder.broadcastIfEnabled(MessageFormatter.getAndDeserialise("event.hoarder-ending-no-participants"));

		if (ConfigurationManager.config.getBoolean("event.start-random-event-on-completion")){
			startRandomHoarderEvent();
		}
	}


	static public HoarderEvent getCurrentEvent() {
		return currentEvent;
	}
}
