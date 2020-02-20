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

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class WindowClickFix extends PacketAdapter {

  private final FiguresFix plugin;
  private final Cache<UUID, ActionData> actions = CacheBuilder.newBuilder().concurrencyLevel(2).initialCapacity(10)
      .expireAfterWrite(5, TimeUnit.SECONDS).build();

  public WindowClickFix(FiguresFix plugin) {
    super(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.WINDOW_CLICK);
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

    ItemStack itemStackInPacket = event.getPacket().getItemModifier().readSafely(0);
    if (!plugin.shouldCheck(itemStackInPacket)) {
      return;
    }

    CheckResult result = plugin.checkItem(itemStackInPacket);
    if (!CheckResult.isOk(result)) {
      event.setCancelled(true);
      plugin.forceKick(player);
      plugin.getLogger().log(Level.INFO,
          "{0} Kicked by WCF: ".concat(result.getMessage()),
          player.getName());
      return;
    }

    ActionData data = actions.getIfPresent(player.getUniqueId());
    if (data == null) {
      data = new ActionData();
      actions.put(player.getUniqueId(), data);
    }
    if (data.handle(event.getPacket().getShorts().read(0))) {
      event.setCancelled(true);
      plugin.forceKick(player);
      plugin.getLogger().log(Level.INFO, "Игрок {0} был кикнут. Был использован .figure2 ", player.getName());
      return;
    }
  }

  private void registerCleanUpTask() {
    Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, actions::cleanUp, 20, 20);
  }

  private static class ActionData {
    private int prevNumber = -1;
    private int numbersInRow = 0;

    public synchronized boolean handle(int currNum) {
      if (currNum != prevNumber) {
        prevNumber = currNum;
        numbersInRow = 0;
        return false;
      }
      return ++numbersInRow >= 3;
    }
  }

}
