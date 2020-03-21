package ru.leymooo.figuresfix;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class Events implements Listener {
    @EventHandler
    public void on(PlayerQuitEvent event) {
        Metadata.getMetadataMap().remove(event.getPlayer());
    }
}
