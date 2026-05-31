package top.lordgamer.minerseller;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinerSellerPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private static final Pattern HEX = Pattern.compile("&?#([A-Fa-f0-9]{6})");

    private String menuTitle;
    private int menuSize;
    private String ecoCommand;
    private Sound sellSound;

    private Material fillerMat;
    private String fillerName;
    private final List<Integer> fillerSlots = new ArrayList<>();

    private int exitSlot;
    private Material exitMat;
    private String exitName;

    private int sellAllSlot;
    private Material sellAllMat;
    private String sellAllName;
    private List<String> sellAllLore = new ArrayList<>();

    private List<String> itemLoreTemplate = new ArrayList<>();

    private final Map<Integer, SellEntry> entriesBySlot = new LinkedHashMap<>();

    private String msgPrefix, msgNoItems, msgSold, msgSoldAll, msgNothing;

    static class SellEntry {
        final Material material;
        final double price;
        final String name;
        SellEntry(Material material, double price, String name) {
            this.material = material;
            this.price = price;
            this.name = name;
        }
    }

    static class MinerHolder implements InventoryHolder {
        Inventory inv;
        @Override
        public Inventory getInventory() { return inv; }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("miner") != null) getCommand("miner").setExecutor(this);
        getLogger().info("MinerSeller enabled.");
    }

    private void load() {
        reloadConfig();
        entriesBySlot.clear();
        fillerSlots.clear();

        menuTitle = getConfig().getString("menu.title", "&8| Miner |");
        menuSize = getConfig().getInt("menu.size", 54);
        if (menuSize % 9 != 0 || menuSize <= 0 || menuSize > 54) menuSize = 54;

        ecoCommand = getConfig().getString("eco-command", "eco give %player% %amount%");
        String soundName = getConfig().getString("sell-sound", "");
        Sound s = null;
        if (soundName != null && !soundName.isEmpty()) {
            try { s = Sound.valueOf(soundName.toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ignored) {}
        }
        sellSound = s;

        msgPrefix = getConfig().getString("messages.prefix", "");
        msgNoItems = getConfig().getString("messages.no-items", "&cNo items.");
        msgSold = getConfig().getString("messages.sold", "&aSold %amount%x %material% for %total%");
        msgSoldAll = getConfig().getString("messages.sold-all", "&aSold everything for %total%");
        msgNothing = getConfig().getString("messages.nothing-to-sell", "&cNothing to sell.");

        fillerMat = matOr(getConfig().getString("filler.material"), Material.GRAY_STAINED_GLASS_PANE);
        fillerName = getConfig().getString("filler.name", "&7");
        fillerSlots.addAll(getConfig().getIntegerList("filler.slots"));

        exitSlot = getConfig().getInt("exit.slot", 48);
        exitMat = matOr(getConfig().getString("exit.material"), Material.BARRIER);
        exitName = getConfig().getString("exit.name", "&cExit");

        sellAllSlot = getConfig().getInt("sell-all.slot", 50);
        sellAllMat = matOr(getConfig().getString("sell-all.material"), Material.HOPPER);
        sellAllName = getConfig().getString("sell-all.name", "&aSell all");
        sellAllLore = getConfig().getStringList("sell-all.lore");

        itemLoreTemplate = getConfig().getStringList("item-lore");

        ConfigurationSection items = getConfig().getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection sec = items.getConfigurationSection(key);
                if (sec == null) continue;
                Material m = matOr(sec.getString("material"), null);
                if (m == null) { getLogger().warning("Bad material in items." + key); continue; }
                int slot = sec.getInt("slot", -1);
                if (slot < 0 || slot >= menuSize) { getLogger().warning("Bad slot in items." + key); continue; }
                double price = sec.getDouble("price", 0);
                String name = sec.getString("name", m.name());
                entriesBySlot.put(slot, new SellEntry(m, price, name));
            }
        }
    }

    private Material matOr(String name, Material def) {
        if (name == null) return def;
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return m == null ? def : m;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this command."); return true; }
        Player p = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("minerseller.admin")) { p.sendMessage(color(msgPrefix + "&cNo permission.")); return true; }
            load();
            p.sendMessage(color(msgPrefix + "&aConfig reloaded."));
            return true;
        }
        openMenu(p);
        return true;
    }

    private void openMenu(Player player) {
        MinerHolder holder = new MinerHolder();
        Inventory inv = Bukkit.createInventory(holder, menuSize, color(menuTitle));
        holder.inv = inv;

        ItemStack filler = icon(fillerMat, fillerName, Collections.<String>emptyList());
        for (int slot : fillerSlots) if (slot >= 0 && slot < menuSize) inv.setItem(slot, filler);

        if (exitSlot >= 0 && exitSlot < menuSize)
            inv.setItem(exitSlot, icon(exitMat, exitName, Collections.<String>emptyList()));
        if (sellAllSlot >= 0 && sellAllSlot < menuSize)
            inv.setItem(sellAllSlot, icon(sellAllMat, sellAllName, sellAllLore));

        for (Map.Entry<Integer, SellEntry> e : entriesBySlot.entrySet())
            inv.setItem(e.getKey(), buildSellIcon(player, e.getValue()));

        player.openInventory(inv);
    }

    private ItemStack buildSellIcon(Player player, SellEntry e) {
        int have = countMaterial(player, e.material);
        List<String> lore = new ArrayList<>();
        for (String line : itemLoreTemplate) {
            lore.add(line
                .replace("%price1%", fmt(e.price))
                .replace("%price64%", fmt(e.price * 64))
                .replace("%priceAll%", fmt(e.price * have))
                .replace("%have%", String.valueOf(have)));
        }
        return icon(e.material, e.name, lore);
    }

    private ItemStack icon(Material m, String name, List<String> lore) {
        ItemStack is = new ItemStack(m);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) colored.add(color(l));
                meta.setLore(colored);
            }
            is.setItemMeta(meta);
        }
        return is;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MinerHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getInventory())) return;
        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == exitSlot) { p.closeInventory(); return; }
        if (slot == sellAllSlot) { sellAll(p); refresh(p, event.getInventory()); return; }

        SellEntry e = entriesBySlot.get(slot);
        if (e == null) return;

        int amount;
        switch (event.getClick()) {
            case RIGHT:
            case SHIFT_RIGHT:
                amount = 64; break;
            case SHIFT_LEFT:
                amount = Integer.MAX_VALUE; break;
            case LEFT:
            default:
                amount = 1; break;
        }
        sellMaterial(p, e, amount);
        event.getInventory().setItem(slot, buildSellIcon(p, e));
    }

    private void sellMaterial(Player p, SellEntry e, int requested) {
        int have = countMaterial(p, e.material);
        if (have <= 0) { p.sendMessage(color(msgPrefix + msgNoItems.replace("%material%", e.name))); return; }
        int toSell = Math.min(requested, have);
        removeMaterial(p, e.material, toSell);
        double total = e.price * toSell;
        payout(p, total);
        p.sendMessage(color(msgPrefix + msgSold
            .replace("%amount%", String.valueOf(toSell))
            .replace("%material%", e.name)
            .replace("%total%", fmt(total))));
        if (sellSound != null) p.playSound(p.getLocation(), sellSound, 1f, 1f);
    }

    private void sellAll(Player p) {
        double total = 0;
        boolean any = false;
        for (SellEntry e : entriesBySlot.values()) {
            int have = countMaterial(p, e.material);
            if (have <= 0) continue;
            removeMaterial(p, e.material, have);
            total += e.price * have;
            any = true;
        }
        if (!any) { p.sendMessage(color(msgPrefix + msgNothing)); return; }
        payout(p, total);
        p.sendMessage(color(msgPrefix + msgSoldAll.replace("%total%", fmt(total))));
        if (sellSound != null) p.playSound(p.getLocation(), sellSound, 1f, 1f);
    }

    private void payout(Player p, double total) {
        if (total <= 0) return;
        String cmd = ecoCommand.replace("%player%", p.getName()).replace("%amount%", fmt(total));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private void refresh(Player p, Inventory inv) {
        for (Map.Entry<Integer, SellEntry> e : entriesBySlot.entrySet())
            inv.setItem(e.getKey(), buildSellIcon(p, e.getValue()));
    }

    private int countMaterial(Player p, Material m) {
        int c = 0;
        for (ItemStack is : p.getInventory().getContents())
            if (is != null && is.getType() == m) c += is.getAmount();
        return c;
    }

    private void removeMaterial(Player p, Material m, int amount) {
        int left = amount;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() != m) continue;
            int a = is.getAmount();
            if (a <= left) { left -= a; p.getInventory().setItem(i, null); }
            else { is.setAmount(a - left); left = 0; }
        }
        p.updateInventory();
    }

    private String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 100.0) / 100.0);
    }

    public static String color(String s) {
        if (s == null) return "";
        Matcher m = HEX.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(ChatColor.of("#" + m.group(1)).toString()));
        }
        m.appendTail(sb);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
