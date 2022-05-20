package io.github.tezvn.teleportation.commands;

import io.github.tezvn.teleportation.utils.AbstractThread;
import io.github.tezvn.teleportation.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TpaCommand extends CommandBuilder {

    public TpaCommand(JavaPlugin plugin) {
        super(plugin, "tpa", "", "/tpa", Collections.emptyList());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(sender instanceof ConsoleCommandSender) {
            MessageUtils.sendMessage(sender, "&cThis command is for player only!");
            return true;
        }
        Player player = (Player) sender;
        if(args.length == 0) {
            MessageUtils.sendMessage(player, "&cPlease type player name!");
            return true;
        }
        final String playerName = args[0];
        final Player target = getTarget(playerName);
        if(target == null) {
            MessageUtils.sendMessage(player, "&cCould not find player with name &6" + playerName);
            return true;
        }
        if(target.hasMetadata(player.getName())) {
            MessageUtils.sendMessage(player, "&cRequest have been sent to player &6" + target.getName());
            return true;
        }
        target.setMetadata(player.getName(), new FixedMetadataValue(getPlugin(), true));
        MessageUtils.sendMessage(player, "&6Sent request to player &b" + target.getName());
        MessageUtils.sendMessage(target,
                "&6Player &b" + player.getName() + " want to teleport to you.",
                "&6You have 60s to accept/deny.",
                "&6- &a/tpaccept &6to accept.",
                "&6- &c/tpadeny &6to deny.");
        new AbstractThread.DelayedThread(getPlugin(), true, 1200) {
            @Override
            public void onTick() {
                if(target.hasMetadata(player.getName())) {
                    target.removeMetadata(player.getName(), getPlugin());
                }
            }
        }.start();
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if(args.length == 1) {
            String playerName = args[0];
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.startsWith(playerName))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Player getTarget(String name) {
        return Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().equals(name)).findAny().orElse(null);
    }


}
