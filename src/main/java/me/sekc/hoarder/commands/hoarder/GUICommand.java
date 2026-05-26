package me.sekc.hoarder.commands.hoarder;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.commands.BaseCommand;
import me.sekc.hoarder.gui.MenuManager;
import me.sekc.hoarder.gui.menus.MainMenu;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class GUICommand extends BaseCommand {
	static public void openMainMenu(Hoarder plugin, Entity player) {
		MenuManager.open(player, new MainMenu(plugin));
	}

	static public void register(Hoarder plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
		root.executes(ctx -> {
			openMainMenu(plugin, ctx.getSource().getExecutor());

			return Command.SINGLE_SUCCESS;
		}).requires(sender -> sender.getSender().hasPermission("hoarder.gui"));

		root.then(Commands.literal("gui")
			.executes(ctx -> {
				openMainMenu(plugin, ctx.getSource().getExecutor());

				return Command.SINGLE_SUCCESS;
			}).requires(sender -> sender.getSender().hasPermission("hoarder.gui")));
	}
}
