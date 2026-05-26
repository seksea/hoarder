package me.sekc.hoarder;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class MessageFormatter {
	static YamlConfiguration messagesConfig = null;
	static boolean placeholderAPIInstalled = false;

	static public void loadMessagesYml(Plugin plugin) { // Load (or reload) the messages.yml
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			placeholderAPIInstalled = true;
		} else {
			Logger.warn("Could not find PlaceholderAPI, it will not be used.");
		}

		File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
		if (!messagesFile.exists()) {
			Logger.warn("Could not find " + messagesFile.getPath() + ", creating a new one from resources.");
			plugin.saveResource("messages.yml", false);
		}

		messagesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

		final InputStream defConfigStream = plugin.getResource("messages.yml");
		if (defConfigStream == null) {
			return;
		}

		// set defaults from the messages.yml in resources if messages.yml exists
		messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
	}

	static public String getRaw(String yamlPath, Map<String, String> customPlaceholders, UUID playerUUID) {
		if (messagesConfig == null) {
			loadMessagesYml(Hoarder.getPlugin(Hoarder.class));
		}

		OfflinePlayer offlinePlayer = null;
		if (playerUUID != null)
			offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);

		String original = messagesConfig.getString(yamlPath);

		if (original == null) {
			Logger.warn("MISSING " + yamlPath + " FROM messages.yml");
			return "MISSING " + yamlPath + " FROM messages.yml";
		}

		if (customPlaceholders != null) {
			for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
				original = original.replace(entry.getKey(), entry.getValue());
			}
		}

		if (!placeholderAPIInstalled) {
			return original.stripTrailing(); // dont use PlaceholderAPI if not installed
		}
		return PlaceholderAPI.setPlaceholders(offlinePlayer, original.stripTrailing()); // strip trailing as multiline yaml strings always end with unnecessary newline
	}

	static public Component getAndDeserialise(String yamlPath, Map<String, String> customPlaceholders, UUID playerUUID) { // playerUUID is only used for PlaceholderAPI placeholders
		return MiniMessage.miniMessage().deserialize(
			getRaw(yamlPath, customPlaceholders, playerUUID)
		);
	}
	static public Component getAndDeserialise(String yamlPath, Map<String, String> customPlaceholders) {
		return getAndDeserialise(yamlPath, customPlaceholders, null);
	}
	static public Component getAndDeserialise(String yamlPath, UUID playerUUID) {
		return getAndDeserialise(yamlPath, null, playerUUID);
	}
	static public Component getAndDeserialise(String yamlPath) {
		return getAndDeserialise(yamlPath, null, null);
	}

	static public Component getAsChatMessageAndDeserialise(String yamlPath, Map<String, String> customPlaceholders, UUID playerUUID) {
		return MiniMessage.miniMessage().deserialize(
			getRaw("chat-prefix", customPlaceholders, playerUUID) + "<reset>" + getRaw(yamlPath, customPlaceholders, playerUUID)
		);
	}
	static public Component getAsChatMessageAndDeserialise(String yamlPath, UUID playerUUID) {
		return getAsChatMessageAndDeserialise(yamlPath, null, playerUUID);
	}


}
