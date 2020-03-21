package ru.leymooo.figuresfix;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.WeakHashMap;

public class Metadata {
    private static Map<Player, Metadata> metadataMap = new WeakHashMap<>();

    public Metadata(Player player) {
        this.player = player;
    }

    public static Map<Player, Metadata> getMetadataMap() {
        return metadataMap;
    }

    public static Metadata get(Player player) {
        return metadataMap.computeIfAbsent(player, Metadata::new);
    }

    private Player player;

    private int vl = 0;
    private long startVl = System.currentTimeMillis();

    public void addVl(int vl) {

    }
}
