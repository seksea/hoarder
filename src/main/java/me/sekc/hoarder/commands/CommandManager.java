package me.sekc.hoarder.commands;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.Logger;
import me.sekc.hoarder.commands.hoarder.AdminCommand;
import me.sekc.hoarder.commands.hoarder.GUICommand;

public class CommandManager {
	static public void registerCommands(Hoarder plugin) {
		Logger.log("Creating commands...");

		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("hoarder");
		AdminCommand.register(plugin, root);
		GUICommand.register(plugin, root);

		LiteralCommandNode<CommandSourceStack> buildRoot = root.build();

		plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			commands.registrar().register(buildRoot);
		});
	}
}
