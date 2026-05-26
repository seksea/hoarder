package me.sekc.hoarder;

import org.bukkit.Bukkit;

public class Logger {
	static java.util.logging.Logger logger = null;

	static public void getLogger() {
		logger = Hoarder.getPlugin(Hoarder.class).getLogger();
	}

	static public void log(String msg) {
		if (logger == null) getLogger();
		logger.info(msg);
	}

	static public void warn(String msg) {
		if (logger == null) getLogger();
		logger.warning(msg);
	}

	static public void err(String msg) {
		if (logger == null) getLogger();
		logger.severe(msg);
	}
}
