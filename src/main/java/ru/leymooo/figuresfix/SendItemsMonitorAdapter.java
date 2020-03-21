package ru.leymooo.figuresfix;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import net.minecraft.server.v1_12_R1.ItemStack;
import org.bukkit.entity.Player;

import java.util.List;

public class SendItemsMonitorAdapter extends PacketAdapter {
    private FiguresFix plugin;

    public SendItemsMonitorAdapter(FiguresFix plugin) {
        super(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.SET_SLOT,
                PacketType.Play.Server.WINDOW_ITEMS);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Metadata metadata = Metadata.get(player);

        long current = System.currentTimeMillis();
        if (current - metadata.startItemSendChars > plugin.limitSendItemCharsTimeMillis) {
            // reset
            metadata.startItemSendChars = current;
            metadata.sendItemChars = 0;
        }

        PacketContainer packet = event.getPacket();
        if (packet.getType().equals(PacketType.Play.Server.SET_SLOT)) {
            StructureModifier<ItemStack> specificModifier = packet.getSpecificModifier(ItemStack.class);
            ItemStack stack = specificModifier.read(0);
            System.out.println("SET_SLOT " + stack);
        } else if (packet.getType().equals(PacketType.Play.Server.WINDOW_ITEMS)) {
            StructureModifier<List> specificModifier = packet.getSpecificModifier(List.class);
            List<ItemStack> items = specificModifier.read(0);
            System.out.println("WINDOW_ITEMS " + items);
        }
    }
}
