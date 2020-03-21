package ru.leymooo.figuresfix;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Metadata {
    private static Map<String, Metadata> metadataMap = new ConcurrentHashMap<>();

    public Metadata(String player) {
        this.player = player;
    }

    public static Map<String, Metadata> getMetadataMap() {
        return metadataMap;
    }

    public static Metadata get(Player player) {
        return metadataMap.computeIfAbsent(player.getName(), Metadata::new);
    }

    private String player;

    public int sendItemChars = 0;
    public long startItemSendChars = System.currentTimeMillis();
    public long lastSendDenyMessage;
    public long lastLog;
    public long lastLogChars;
    public long lastKick;


}
