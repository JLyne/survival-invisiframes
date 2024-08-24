package com.darkender.plugins.survivalinvisiframes;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings("UnstableApiUsage")
public class InvisiFramesCommand
{
    private final SurvivalInvisiframes plugin;

    public InvisiFramesCommand(SurvivalInvisiframes plugin, Commands commands)
    {
        this.plugin = plugin;

        LiteralCommandNode<CommandSourceStack> getCommand = literal("reload")
                .requires(source -> source.getSender().hasPermission("survivalinvisiframes.reload"))
                .executes(ctx -> {
                    plugin.reload();
                    ctx.getSource().getSender()
                            .sendMessage(Component.text("Configuration has been reloaded")
                                                 .color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .build();

        LiteralCommandNode<CommandSourceStack> reloadCommand = literal("get")
                .requires(source -> source.getSender().hasPermission("survivalinvisiframes.get"))
                .executes(ctx -> giveItem(ctx.getSource().getSender()))
                .build();

        LiteralCommandNode<CommandSourceStack> forceRecheckCommand = literal("force-recheck")
                .requires(source -> source.getSender().hasPermission("survivalinvisiframes.reload"))
                .executes(ctx -> {
                    plugin.forceRecheck();
                    ctx.getSource().getSender()
                            .sendMessage(Component.text("Rechecked invisible item frames")
                                                 .color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .build();

        commands.register(literal("iframe")
								  .requires(source -> source.getSender().hasPermission("survivalinvisiframes.cmd"))
                                  .then(getCommand)
                                  .then(reloadCommand)
                                  .then(forceRecheckCommand)
								  .build(), "Main command");
    }
    
    private int giveItem(CommandSender sender)
    {
        if(!(sender instanceof Player player))
        {
            sender.sendMessage(Component.text("This command can only be used in-game")
                                       .color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

		player.getInventory().addItem(plugin.generateInvisibleItemFrame(false));
        player.sendMessage(Component.text("Added an invisible item frame to your inventory")
                                   .color(NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }
}
