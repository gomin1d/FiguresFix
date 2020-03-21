package ru.leymooo.figuresfix;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ListIterator;

public class SendItemsAdapter extends PacketAdapter {
    private FiguresFix plugin;
    private ItemStack air = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.AIR));

    public SendItemsAdapter(FiguresFix plugin) {
        super(plugin, ListenerPriority.LOWEST,
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
            StructureModifier<net.minecraft.server.v1_12_R1.ItemStack> specificModifier = packet.getSpecificModifier(net.minecraft.server.v1_12_R1.ItemStack.class);
            net.minecraft.server.v1_12_R1.ItemStack stack = specificModifier.read(0);
            int chars = calcDeepLenStringsInTag(stack.getTag());
            if (chars < plugin.limitSendItemCharsThreshold) {
                return;
            }
            if (this.check(stack, chars, current, metadata, player, packet)) {
                specificModifier.write(0, air);
            }
        } else if (packet.getType().equals(PacketType.Play.Server.WINDOW_ITEMS)) {
            StructureModifier<List> specificModifier = packet.getSpecificModifier(List.class);
            List<net.minecraft.server.v1_12_R1.ItemStack> items = specificModifier.read(0);
            ListIterator<ItemStack> iterator = items.listIterator();
            boolean replace = false;
            while (iterator.hasNext()) {
                ItemStack stack = iterator.next();
                if (stack == null) {
                    continue;
                }
                int chars = calcDeepLenStringsInTag(stack.getTag());
                if (chars < plugin.limitSendItemCharsThreshold) {
                    continue;
                }
                if (replace) {
                    iterator.set(air);
                } else if (this.check(stack, chars, current, metadata, player, packet)) {
                    replace = true;
                    iterator.set(air);
                }
            }
            if (replace) {
                specificModifier.write(0, items);
            }
        }
    }

    private boolean check(ItemStack stack, int chars, long current, Metadata metadata, Player player, PacketContainer packet) {
        metadata.sendItemChars += chars;
        if (metadata.sendItemChars > plugin.limitSendItemCharsPerTime) {
            if (current - metadata.lastSendDenyMessage > 2000) {
                metadata.lastSendDenyMessage = current;
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cНекоторые предметы не удалось отобразить, работает защита от крашей. Some items could not be displayed, crash protection works."));
            }
            plugin.getLogger().warning("Лимит отправки предметов " + player.getName() + " " + metadata.sendItemChars + "/" + plugin.limitSendItemCharsPerTime +
                    ". Последний пакет " + packet.getType().name() +
                    ", последний тег длиною " + chars + ", последний предмет " + stack.getItem().getName());
            return true;
        }

        return false;
    }

    /**
     * Посчитать суммарную длину всех строк в этом теге
     */
    public int calcDeepLenStringsInTag(NBTBase tag){
        if(tag == null){
            return 0;
        }
        if(tag instanceof NBTTagCompound){
            int counter = 0;
            for(NBTBase value : ((NBTTagCompound) tag).map.values()){
                counter += calcDeepLenStringsInTag(value);
            }
            return counter;
        } else if(tag instanceof NBTTagList){
            int counter = 0;
            for(NBTBase value : ((NBTTagList) tag).list){
                counter += calcDeepLenStringsInTag(value);
            }
            return counter;
        } else if(tag instanceof NBTTagString){
            return ((NBTTagString) tag).c_().length();
        }

        return 0;
    }
}
