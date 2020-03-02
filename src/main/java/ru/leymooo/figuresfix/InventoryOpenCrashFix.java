package ru.leymooo.figuresfix;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Фикс частого открытия инвентаря, что порождаем нагрузку
 */
public class InventoryOpenCrashFix implements Listener {

    private Map<Player, PlayerData> metadata = new WeakHashMap<>();
    private int per10sec;
    private int per1min;

    public InventoryOpenCrashFix(int per10sec, int per1min) {
        this.per10sec = per10sec;
        this.per1min = per1min;
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        metadata.remove(event.getPlayer()); // на всякий случай
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(InventoryOpenEvent event) {
        PlayerData playerData = get((Player) event.getPlayer());
        long current = System.currentTimeMillis();

        // удаляем историю старше минуты
        playerData.historyOpen.removeIf(openTime -> current - openTime > 60_000);

        // не больше 30 открытий гуи в минуту
        if (playerData.historyOpen.size() >= per1min) {
            event.getPlayer().sendMessage("§cYou can’t open your inventory so often. §cНельзя так часто открывать инвентарь.");
            event.setCancelled(true);
            return;
        }

        long countPer10Sec = playerData.historyOpen.stream()
                .filter(openTime -> current - openTime < 10_000)
                .count();
        // не больше 10 открытий гуи за 10 сек
        if (countPer10Sec >= per10sec) {
            event.getPlayer().sendMessage("§cYou can’t open your inventory so often. §cНельзя так часто открывать инвентарь.");
            event.setCancelled(true);
            return;
        }

        // добавляем новое открытие
        playerData.historyOpen.add(current);
    }

    public PlayerData get(Player player) {
        return metadata.computeIfAbsent(player, key -> new PlayerData());
    }

    public int getPer10sec() {
        return per10sec;
    }

    public int getPer1min() {
        return per1min;
    }

    private static class PlayerData {
        private List<Long> historyOpen = new LinkedList<>();
    }
}
