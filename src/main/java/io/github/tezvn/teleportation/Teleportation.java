package io.github.tezvn.teleportation;

import io.github.tezvn.teleportation.commands.TpaCommand;
import io.github.tezvn.teleportation.commands.TpacceptCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Teleportation extends JavaPlugin {

    @Override
    public void onEnable() {
        new TpaCommand(this);
        new TpacceptCommand(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
