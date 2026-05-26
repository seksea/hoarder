package me.sekc.hoarder;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigurationManager {
	static YamlConfiguration config = null;

	static public void loadConfiguration() { // load/reload the config
		Plugin plugin = Hoarder.getPlugin(Hoarder.class);

		File configFile = new File(plugin.getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			Logger.warn("Could not find " + configFile.getPath() + ", creating a new one from resources.");
			plugin.saveResource("config.yml", false);
		}

		plugin.reloadConfig();

		config = (YamlConfiguration) plugin.getConfig();
	}

	static public String getString(String path) {
		return config.getString(path);
	}

	static public long getLong(String path) {
		return config.getLong(path);
	}

	static public boolean getBool(String path) {
		return config.getBoolean(path);
	}
}
