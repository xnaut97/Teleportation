package io.github.tezvn.teleportation.commands;

import io.github.tezvn.teleportation.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TpacceptCommand extends CommandBuilder{

    public TpacceptCommand(JavaPlugin plugin) {
        super(plugin, "tpaccept", "", "/tpaccept", Collections.emptyList());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(sender instanceof ConsoleCommandSender) {
            MessageUtils.sendMessage(sender, "&cThis command is for player only!");
            return true;
        }
        Player player = (Player) sender;
        if(!player.hasMetadata("teleport")) {
            MessageUtils.sendMessage(player, "&cYou don't have any teleport request!");
            return true;
        }
        if(args.length == 0) {
            MessageUtils.sendMessage(player, "&cPlease type player name!");
            return true;
        }
        final String input = args[0];
        final Player target = getTarget(input);
        if(target == null) {
            MessageUtils.sendMessage(player, "&cCould not find player with name &6" + input);
            return true;
        }
        if(!player.hasMetadata(target.getName())) {
            MessageUtils.sendMessage(player, "&cYou don't have teleport request from player &6" + target.getName());
            return true;
        }
        //Remove target name if he/she is offline
        if(!target.isOnline()) {
            player.removeMetadata(target.getName(), getPlugin());
            return true;
        }
        Location playerLocation = player.getLocation();
        target.teleport(playerLocation);
        player.removeMetadata(target.getName(), getPlugin());
        MessageUtils.sendMessage(target, "&aPlayer &6" + player.getName() + " &ahas accepted the teleport request!");
        MessageUtils.sendMessage(player, "&aTeleported player &6" + target.getName() + " &ato you!");
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
