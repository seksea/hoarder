package me.sekc.hoarder.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.Logger;
import org.bukkit.plugin.Plugin;

public class BaseCommand {
	static public void register(Hoarder plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
		Logger.warn("Ran base BaseCommand.register, something has gone seriously wrong");
	}
}
