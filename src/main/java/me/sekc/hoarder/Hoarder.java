package me.sekc.hoarder;

import me.sekc.hoarder.commands.CommandManager;
import me.sekc.hoarder.gui.MenuManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Hoarder extends JavaPlugin {

	@Override
	public void onEnable() {
		// Plugin startup logic
		Logger.log("=======================================");
		Logger.log("  ▗▖ ▗▖ ▗▄▖  ▗▄▖ ▗▄▄▖ ▗▄▄▄ ▗▄▄▄▖▗▄▄▖ ");
		Logger.log("  ▐▌ ▐▌▐▌ ▐▌▐▌ ▐▌▐▌ ▐▌▐▌  █▐▌   ▐▌ ▐▌");
		Logger.log("  ▐▛▀▜▌▐▌ ▐▌▐▛▀▜▌▐▛▀▚▖▐▌  █▐▛▀▀▘▐▛▀▚▖");
		Logger.log("  ▐▌ ▐▌▝▚▄▞▘▐▌ ▐▌▐▌ ▐▌▐▙▄▄▀▐▙▄▄▖▐▌ ▐▌");
		Logger.log("=======================================");

		ConfigurationManager.loadConfiguration();

		CommandManager.registerCommands(this);

		getServer().getPluginManager().registerEvents(new EventListener(this), this);
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}

	public void reloadConfigFiles() {
		ConfigurationManager.loadConfiguration();
		MessageFormatter.loadMessagesYml(this);
		MenuManager.clearMenuConfigCache();
	}

	static public void broadcastIfEnabled(Component message) {
		if (ConfigurationManager.getBool("allow-broadcast"))
			Bukkit.broadcast(message);
	}
}
