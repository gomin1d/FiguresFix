package ru.leymooo.figuresfix;

import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Metadata {
    @SuppressWarnings("unchecked")
    private static Map<String, Metadata> metadataMap = (Map)CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build().asMap();

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


}
