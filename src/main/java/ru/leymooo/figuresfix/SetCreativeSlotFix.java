package ru.leymooo.figuresfix;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.leymooo.figuresfix.FiguresFix.CheckResult;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SetCreativeSlotFix extends PacketAdapter {

  private final FiguresFix plugin;
  private final Cache<UUID, Integer> limit = CacheBuilder.newBuilder().concurrencyLevel(2).initialCapacity(10)
      .expireAfterWrite(500, TimeUnit.MILLISECONDS).build();

  public SetCreativeSlotFix(FiguresFix plugin) {
    super(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.SET_CREATIVE_SLOT);
    this.plugin = plugin;
    registerCleanUpTask();
  }

  @Override
  public void onPacketReceiving(PacketEvent event) {
    Player player = event.getPlayer();
    if (player == null) {
      event.setCancelled(true);
      return;
    }

    if (player.getGameMode() != GameMode.CREATIVE) {
      Integer attemps = limit.getIfPresent(player.getUniqueId());
      if (checkLimit(player, attemps, 3)) {
        plugin.getLogger().log(Level.INFO, "Игрок {0} был кикнут. SetSlot в сурвивале", player.getName());
        event.setCancelled(true);
        return;
      }
      limit.put(player.getUniqueId(), attemps == null ? 1 : attemps + 1);
    }

    ItemStack itemStackInPacket = event.getPacket().getItemModifier().readSafely(0);
    if (!plugin.shouldCheck(itemStackInPacket)) {
      return;
    }

    Integer attemps = limit.getIfPresent(player.getUniqueId());
    if (checkLimit(player, attemps, 10)) {
      plugin.getLogger().log(Level.INFO, "{0} Kicked by SSF. Spam using SetSlot", player.getName());
      event.setCancelled(true);
      return;
    }

    limit.put(player.getUniqueId(), attemps == null ? 1 : attemps + 1);

    CheckResult result = plugin.checkItem(itemStackInPacket);
    if (!CheckResult.isOk(result)) {
      event.setCancelled(true);
      Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(result.getMessage()));
      plugin.getLogger().log(Level.INFO,
          "{0} Kicked by SSF: ".concat(result.getMessage()),
          player.getName());
    }
  }

  private boolean checkLimit(Player player, Integer attemps, int max) {
    if (attemps != null && attemps >= max) {
      plugin.forceKick(player);
      return true;
    }
    return false;
  }

  private void registerCleanUpTask() {
    Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, limit::cleanUp, 20, 20);
  }
}
