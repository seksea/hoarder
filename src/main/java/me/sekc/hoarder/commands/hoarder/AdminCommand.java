package me.sekc.hoarder.commands.hoarder;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.sekc.hoarder.Hoarder;
import me.sekc.hoarder.HoarderEventManager;
import me.sekc.hoarder.MessageFormatter;
import me.sekc.hoarder.commands.BaseCommand;
import me.sekc.hoarder.commands.hoarder.suggestions.HoarderItemSuggestionProvider;
import me.sekc.hoarder.gui.MenuManager;
import me.sekc.hoarder.gui.menus.MainMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class AdminCommand extends BaseCommand {
	static public void openMainMenu(Hoarder plugin, Entity player) {
		MenuManager.open(player, new MainMenu(plugin));
	}

	static public void register(Hoarder plugin, LiteralArgumentBuilder<CommandSourceStack> root) {
		root.then(Commands.literal("admin")
			.then(Commands.literal("forcestartrandom")
				.executes(ctx -> {
					if (HoarderEventManager.getCurrentEvent() != null) {
						// end the old event
						HoarderEventManager.endHoarderEvent(false);
					}

					HoarderEventManager.startRandomHoarderEvent();
					return Command.SINGLE_SUCCESS;
				})).requires(sender -> sender.getSender().hasPermission("hoarder.admin")));

		root.then(Commands.literal("admin")
			.then(Commands.literal("forcestartfromhand")
				.then(Commands.argument("duration_seconds", IntegerArgumentType.integer(1))
					.executes(ctx -> {
						ItemStack itemInHand = ((Player)ctx.getSource().getExecutor()).getInventory().getItemInMainHand().clone();

						if (itemInHand.isEmpty()) {
							throw new RuntimeException("You need to have something in your main hand.");
						}

						if (HoarderEventManager.getCurrentEvent() != null) {
							// end the old event
							HoarderEventManager.endHoarderEvent(false);
						}

						long curSeconds = System.currentTimeMillis() / 1000;
						HoarderEventManager.startNewHoarderEvent(new HoarderEventManager.HoarderEvent(
							itemInHand,
							curSeconds + ctx.getArgument("duration_seconds", Integer.class)
						), true);

						return Command.SINGLE_SUCCESS;
					}))).requires(sender -> sender.getSender().hasPermission("hoarder.admin")));

		root.then(Commands.literal("admin")
			.then(Commands.literal("forceend")
				.executes(ctx -> {
					try {
						HoarderEventManager.endHoarderEvent();
					} catch (Exception e) {
						ctx.getSource().getExecutor().sendMessage("An error occured while ending the event: " + e.getMessage());
					}
					return Command.SINGLE_SUCCESS;
				})).requires(sender -> sender.getSender().hasPermission("hoarder.admin")));


		root.then(Commands.literal("admin")
			.then(Commands.literal("reload")
				.executes(ctx -> {
					UUID playerUUID = ctx.getSource().getExecutor().getUniqueId();
					ctx.getSource().getExecutor().sendMessage(
						MessageFormatter.getAsChatMessageAndDeserialise("admin.reloading", playerUUID)
					);

					plugin.reloadConfigFiles();

					ctx.getSource().getExecutor().sendMessage(
						MessageFormatter.getAsChatMessageAndDeserialise("admin.reloaded", playerUUID)
					);
					return Command.SINGLE_SUCCESS;
				})
			)
			.requires(sender -> sender.getSender().hasPermission("clans.admin"))
		);

		root.then(Commands.literal("admin")
			.then(Commands.literal("itemlist")
				.then(Commands.literal("addfromhand")
					.executes(ctx -> {
						UUID playerUUID = ctx.getSource().getExecutor().getUniqueId();
						ItemStack itemInHand = ((Player)ctx.getSource().getExecutor()).getInventory().getItemInMainHand().clone();

						if (itemInHand.isEmpty()) {
							throw new RuntimeException("You need to have something in your main hand.");
						}

						plugin.dbConn.addItemToHoarderItems(itemInHand);

						PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
						String itemName = plainTextSerializer.serialize(Component.translatable(itemInHand));

						ctx.getSource().getExecutor().sendMessage(
							MessageFormatter.getAsChatMessageAndDeserialise("admin.itemlist.add-item", Map.ofEntries(
								Map.entry("%item_name%", itemName)
							), playerUUID)
						);

						return Command.SINGLE_SUCCESS;
					})
				)
			)
			.requires(sender -> sender.getSender().hasPermission("clans.admin"))
		);

		root.then(Commands.literal("admin")
			.then(Commands.literal("itemlist")
				.then(Commands.literal("list")
					.executes(ctx -> {
						UUID playerUUID = ctx.getSource().getExecutor().getUniqueId();

						for (int index=0; index < plugin.dbConn.getNumHoarderItems(); index++) {
							String itemName = plugin.dbConn.getHoarderItemNameAtIndex(index);

							ctx.getSource().getExecutor().sendMessage(
								MessageFormatter.getAsChatMessageAndDeserialise("admin.itemlist.list-item", Map.ofEntries(
									Map.entry("%item_name%", itemName)
								), playerUUID)
							);
						}

						return Command.SINGLE_SUCCESS;
					})
				)
			)
			.requires(sender -> sender.getSender().hasPermission("clans.admin"))
		);

		root.then(Commands.literal("admin")
			.then(Commands.literal("itemlist")
				.then(Commands.literal("remove")
					.then(Commands.argument("item_name", StringArgumentType.greedyString()).suggests(new HoarderItemSuggestionProvider(plugin))
						.executes(ctx -> {
							UUID playerUUID = ctx.getSource().getExecutor().getUniqueId();
							String itemName = ctx.getArgument("item_name", String.class);

							ctx.getSource().getExecutor().sendMessage(
								MessageFormatter.getAsChatMessageAndDeserialise("admin.itemlist.remove-item", Map.ofEntries(
									Map.entry("%item_name%", itemName)
								),playerUUID)
							);

							return Command.SINGLE_SUCCESS;
						})
					)
				)
			)
			.requires(sender -> sender.getSender().hasPermission("clans.admin"))
		);
	}

}
