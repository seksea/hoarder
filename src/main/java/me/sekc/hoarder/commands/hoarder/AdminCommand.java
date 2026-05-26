package me.sekc.hoarder.commands.hoarder;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.HoarderEventManager;
import me.sekc.hoarder.MessageFormatter;
import me.sekc.hoarder.commands.BaseCommand;
import me.sekc.hoarder.gui.MenuManager;
import me.sekc.hoarder.gui.menus.MainMenu;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class AdminCommand extends BaseCommand {
	static public void openMainMenu(Hoarder plugin, Entity player) {
		MenuManager.open(player, new MainMenu(plugin));
	}

	static public void register(Hoarder plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("admin")
			.then(Commands.literal("forcestart")
			.executes(ctx -> {
				HoarderEventManager.startRandomHoarderEvent();
				return Command.SINGLE_SUCCESS;
			})).requires(sender -> sender.getSender().hasPermission("hoarder.admin")));


		root.then(Commands.literal("admin")
			.then(Commands.literal("reload")
				.executes(ctx -> {
					UUID playerUUID = ctx.getSource().getExecutor().getUniqueId();
					ctx.getSource().getExecutor().sendMessage(MessageFormatter.getAsChatMessageAndDeserialise("admin.reloading", playerUUID));

					plugin.reloadConfigFiles();

					ctx.getSource().getExecutor().sendMessage(MessageFormatter.getAsChatMessageAndDeserialise("admin.reloaded", playerUUID));
					return Command.SINGLE_SUCCESS;
				})
			)
			.requires(sender -> sender.getSender().hasPermission("clans.admin"))
		);
	}

}
