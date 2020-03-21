package ru.leymooo.figuresfix;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FiguresFix extends JavaPlugin implements CommandExecutor {

    private int pageLenght, writablePageLenght;
    private int maxPages;

    private Config customConfig;

    public int limitSendItemCharsThreshold;
    public int limitSendItemCharsPerTime;
    public int limitSendItemCharsTimeMillis;
    public int limitSendItemCharsKick;

    @Override
    public void onEnable() {
        customConfig = new Config(new File(this.getDataFolder(), "config.yml"));
        Bukkit.getPluginManager().registerEvents(new Events(), this);
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        if (version.startsWith("v1_8_R")) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new BlockPlaceFix(this));
        }
        ProtocolLibrary.getProtocolManager().addPacketListener(new WindowClickFix(this));
        ProtocolLibrary.getProtocolManager().addPacketListener(new SetCreativeSlotFix(this));
        ProtocolLibrary.getProtocolManager().addPacketListener(new SendItemsAdapter(this));
//        ProtocolLibrary.getProtocolManager().addPacketListener(new SendItemsMonitorAdapter(this)); for debug

        this.getCommand("figuresfix").setExecutor(this);

        this.reloadParametersFromConfig();
    }

    public void reloadParametersFromConfig() {
        customConfig.setDescription("/figuresfix reload - перезагрузить этот конфиг, право figuresfix.command\n" +
                "\n" +
                "limit-send-item-chars-per-time - лимит кол-ва символов в теге предметов, которое может быть отправлено игроку за время\n" +
                "limit-send-item-chars-time-millis - время, в котором действует лимит limit-send-item-chars-per-time\n" +
                "limit-send-item-chars-threshold - начиная с какой длины тега учитывать предмет. если тег будет меньше этого значения, предмет не будет учитываться\n" +
                "limit-send-item-chars-kick - кол-во нарушений, когда должно кикнуть");
        pageLenght = customConfig.getOrSetNumber("max-book-page-lenght", 320).intValue();
        writablePageLenght = customConfig.getOrSetNumber("max-writable-book-page-lenght", 280).intValue();
        maxPages = customConfig.getOrSetNumber("max-pages", 50).intValue();

        if (customConfig.contains("limit-open-inventory-enable")) {
            customConfig.setAndSave("limit-open-inventory-enable", null);
        }
        if (customConfig.contains("limit-open-inventory-per-10-sec")) {
            customConfig.setAndSave("limit-open-inventory-per-10-sec", null);
        }
        if (customConfig.contains("limit-open-inventory-per-1-min")) {
            customConfig.setAndSave("limit-open-inventory-per-1-min", null);
        }

        limitSendItemCharsThreshold = customConfig.getOrSetNumber("limit-send-item-chars-threshold", 64).intValue();
        limitSendItemCharsPerTime = customConfig.getOrSetNumber("limit-send-item-chars-per-time", 60000).intValue();
        limitSendItemCharsTimeMillis = customConfig.getOrSetNumber("limit-send-item-chars-time-millis", 1000).intValue();
        limitSendItemCharsKick = customConfig.getOrSetNumber("limit-send-item-chars-kick", 120000).intValue();

        Metadata.getMetadataMap().clear();
    }

    public void forceKick(Player p) {
        try {
            Object nmsPlayer = p.getClass().getMethod("getHandle").invoke(p);
            Object plrConnection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
            if (plrConnection == null)
                return;
            Object networkManager = plrConnection.getClass().getField("networkManager").get(plrConnection);
            if (networkManager == null)
                return;
            Channel channel = (Channel) networkManager.getClass().getField("channel").get(networkManager);
            if (channel == null)
                return;
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CheckResult checkBook(ItemStack stack) {
        NbtCompound root = (NbtCompound) NbtFactory.fromItemTag(stack);

        if (root != null && root.containsKey("pages")) {
            NbtList<String> pages = root.getList("pages");
            if (pages.size() > maxPages) {
                getLogger().info("Too much pages. (" + pages.size() + ">" + maxPages + ")");
                return CheckResult.create("Book contains to many pages (" + pages.size() + ">" + maxPages + ")");
            }
            int max = stack.getType() == Material.BOOK_AND_QUILL ? writablePageLenght : pageLenght;
            for (String page : pages) {
                if (page.length() > max) {
                    getLogger().info("Page too long. (" + page.length() + ">" + max + ")");
                    return CheckResult.create("Page too long");
                }
            }
        }
        if (stack.getType() == Material.BOOK_AND_QUILL) {
            return checkWritableBook(root);
        }
        return CheckResult.ok();
    }

    private CheckResult checkWritableBook(NbtCompound root) {
        if (root.containsKey("author") || root.containsKey("title")) {
            return CheckResult.create("Invalid tags");
        }
        return CheckResult.ok();
    }

    private final Set<Material> shulkers =
            Stream.of(Material.values()).filter(m -> m.toString().endsWith("SHULKER_BOX")).collect(Collectors.toSet());

    private boolean isBook(Material material) {
        return material == Material.BOOK_AND_QUILL || material == Material.WRITTEN_BOOK;
    }

    private boolean isShulkerBox(Material material) {
        return shulkers.contains(material);
    }

    private CheckResult checkShulkerBox(ItemStack stack) {
        BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        for (ItemStack toCheck : box.getInventory().getContents()) {
            if (toCheck != null && isBook(toCheck.getType())) {
                CheckResult checkResult = checkBook(toCheck);
                if (!CheckResult.isOk(checkResult)) {
                    //box.getInventory().remove(toCheck);
                    getLogger().log(Level.INFO, "ShulkerBox contains bad book: {0}", checkResult.getMessage());
                    return CheckResult.create("ShulkerBox contains bad book (" + checkResult.getMessage() + ")");
                }
            }
        }
        return CheckResult.ok();
    }


    public boolean shouldCheck(ItemStack stack) {
        Material type = stack == null ? null : stack.getType();
        return stack != null && (isBook(type) || isShulkerBox(type));
    }

    public CheckResult checkItem(ItemStack stack) {
        if (isBook(stack.getType())) {
            return checkBook(stack);
        } else {
            return checkShulkerBox(stack);
        }
    }

    public static class CheckResult {
        private static CheckResult OK = new CheckResult(null);

        private final String message;

        private CheckResult(String resultMessage) {
            this.message = resultMessage;
        }

        public String getMessage() {
            return message;
        }

        public static CheckResult ok() {
            return OK;
        }

        public static CheckResult create(String resultMessage) {
            return new CheckResult(resultMessage);
        }

        public static boolean isOk(CheckResult checkResult) {
            return checkResult == OK;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e=========[FiguresFix]=========");
            sender.sendMessage("§4/figuresfix reload §7- перезагрузить конфиг");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            customConfig.reload();
            reloadParametersFromConfig();
            sender.sendMessage("§cКонфиг перезагружен.");
            return true;
        }

        sender.sendMessage("§cАргумент команды не найден.");
        return true;
    }
}
