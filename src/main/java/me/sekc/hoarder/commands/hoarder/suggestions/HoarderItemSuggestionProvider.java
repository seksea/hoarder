package me.sekc.hoarder.commands.hoarder.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.sekc.hoarder.DatabaseConnection;
import me.sekc.hoarder.Hoarder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class HoarderItemSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
    Hoarder plugin;

    public HoarderItemSuggestionProvider(Hoarder plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

		for (int index=0; index < plugin.dbConn.getNumHoarderItems(); index++) {
			DatabaseConnection.HoarderItem item = plugin.dbConn.getHoarderItemAtIndex(index);

			PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();
			String itemName = plainTextSerializer.serialize(Component.translatable(item.stack));

			builder.suggest(itemName);
		}

        return builder.buildFuture();
    }
}