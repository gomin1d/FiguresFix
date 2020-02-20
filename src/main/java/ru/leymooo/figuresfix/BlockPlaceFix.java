package ru.leymooo.figuresfix;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.leymooo.figuresfix.FiguresFix.CheckResult;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.ViaAPI;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BlockPlaceFix extends PacketAdapter {

  private final ViaAPI api;
  private final FiguresFix plugin;
  private final Cache<UUID, Integer> limit = CacheBuilder.newBuilder().concurrencyLevel(2).initialCapacity(10)
      .expireAfterWrite(550, TimeUnit.MILLISECONDS).build();

  public BlockPlaceFix(FiguresFix plugin) {
    super(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.BLOCK_PLACE);
    this.plugin = plugin;
    registerCleanUpTask();
    api = Bukkit.getPluginManager().getPlugin("ViaVersion") != null ? Via.getAPI() : null;
  }

  @Override
  public void onPacketReceiving(PacketEvent event) {
    Player player = event.getPlayer();
    if (player == null) {
      event.setCancelled(true);
      return;
    }
    if (api != null && api.getPlayerVersion(player.getUniqueId()) != 47) {
      return;
    }

    Integer attemps = limit.getIfPresent(player.getUniqueId());
    if (checkLimit(player, attemps)) {
      plugin.getLogger().log(Level.INFO, "Игрок {0} был кикнут. Возможно игрок использовал .figure", player.getName());
      event.setCancelled(true);
      return;
    }

    ItemStack itemStackInPacket = event.getPacket().getItemModifier().readSafely(0);
    if (!plugin.shouldCheck(itemStackInPacket)) {
      return;
    }

    ItemStack inHand = player.getItemInHand();
    if (inHand == null || inHand.getType() != itemStackInPacket.getType()) {
      event.setCancelled(true);
      plugin.forceKick(player);
      plugin.getLogger().log(Level.INFO, "Игрок {0} был кикнут. Обнаружено использование .figure", player.getName());
      return;
    }

    CheckResult result = plugin.checkItem(itemStackInPacket);
    if (!CheckResult.isOk(result)) {
      event.setCancelled(true);
      plugin.forceKick(player);
      plugin.getLogger().log(Level.INFO,
          "Игрок {0} был кикнут. Был использован .figure или читерская книга: ".concat(result.getMessage()),
          player.getName());
      return;
    }

    limit.put(player.getUniqueId(), attemps == null ? 0 : attemps + 1);
  }

  private boolean checkLimit(Player player, Integer attemps) {
    if (attemps != null && attemps >= 4) {
      if (attemps >= 8) {
        plugin.forceKick(player);
      }
      Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer("Вы очень быстро открываете книгу"));
      return true;
    }
    return false;
  }

  private void registerCleanUpTask() {
    Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, limit::cleanUp, 20, 20);
  }
}
