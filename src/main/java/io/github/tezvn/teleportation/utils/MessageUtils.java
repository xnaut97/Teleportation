package io.github.tezvn.teleportation.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtils {

    public static void sendMessage(Player player, String... msg) {
        for (String s : msg) {
            player.sendMessage(s.replace("&", "§"));
        }
    }

    public static void sendMessage(CommandSender sender, String... msg) {
        for (String s : msg) {
            sender.sendMessage(s.replace("&", "§"));
        }
    }

}
