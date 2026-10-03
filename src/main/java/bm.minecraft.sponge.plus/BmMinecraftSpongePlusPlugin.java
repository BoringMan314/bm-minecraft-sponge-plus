package bm.minecraft.sponge.plus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Expands sponge absorption into a configurable cubic water scan. */
public final class BmMinecraftSpongePlusPlugin extends JavaPlugin implements Listener {
    private static final int MAX_ABSORPTION_RADIUS = 128;
    private static final int ABSORB_DEBOUNCE_TICKS = 5;
    private final Map<String, Integer> recentAbsorbs = new HashMap<>();
    private YamlConfiguration language;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLanguage();
        getServer().getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("bm-minecraft-sponge-plus");
        if (command == null) throw new IllegalStateException("Missing command in plugin.yml: bm-minecraft-sponge-plus");
        command.setExecutor(this::runCommand);
        command.setTabCompleter((sender, ignored, label, args) -> args.length == 1 ? List.of("0", "1", "reload", "info", "status", "set").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList() : List.of());
        getLogger().info(console("enabled").replace("{version}", getPluginMeta().getVersion()));
    }

    @Override
    public void onDisable() {
        if (language != null) {
            getLogger().info(console("disabled"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        Block center = event.getBlock();
        if (!isFeatureEnabled()) return;
        if (!tryBeginAbsorb(center)) return;
        int radius = getAbsorptionRadius();
        Set<Block> connectedWater = findConnectedWater(center, radius);
        Set<Block> vanillaWater = new HashSet<>();
        for (BlockState state : event.getBlocks()) vanillaWater.add(state.getBlock());
        for (Block block : connectedWater) {
            if (!vanillaWater.contains(block)) event.getBlocks().add(toAbsorbedState(block));
        }
        Set<Block> absorbedBlocks = Set.copyOf(connectedWater);
        for (long delay = 1; delay <= 3; delay++) {
            getServer().getScheduler().runTaskLater(this, () -> {
                if (!event.isCancelled()) clearResidualWater(absorbedBlocks);
            }, delay);
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            if (event.isCancelled()) return;
            clearResidualWater(absorbedBlocks);
            syncClientBlocks(center, absorbedBlocks, radius);
        }, 4);
        notifyAbsorption(center, connectedWater.size());
    }

    private boolean tryBeginAbsorb(Block center) {
        String key = center.getWorld().getUID() + ":" + center.getX() + ":" + center.getY() + ":" + center.getZ();
        int tick = getServer().getCurrentTick();
        Integer last = recentAbsorbs.get(key);
        if (last != null && tick - last < ABSORB_DEBOUNCE_TICKS) return false;
        recentAbsorbs.put(key, tick);
        if (recentAbsorbs.size() > 64) {
            recentAbsorbs.entrySet().removeIf(entry -> tick - entry.getValue() >= ABSORB_DEBOUNCE_TICKS);
        }
        return true;
    }

    private Set<Block> findConnectedWater(Block center, int radius) {
        Set<Block> connectedWater = new HashSet<>();
        Set<Block> visited = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) queue.add(center.getRelative(face));
        while (!queue.isEmpty()) {
            Block block = queue.removeFirst();
            if (!visited.add(block) || !isWithinRadius(center, block, radius) || !isAbsorbableWater(block)) continue;
            connectedWater.add(block);
            for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) queue.add(block.getRelative(face));
        }
        return connectedWater;
    }

    private boolean isWithinRadius(Block center, Block block, int radius) {
        return Math.abs(block.getX() - center.getX()) <= radius
                && Math.abs(block.getY() - center.getY()) <= radius
                && Math.abs(block.getZ() - center.getZ()) <= radius;
    }

    private boolean isAbsorbableWater(Block block) {
        if (block.getType() == Material.WATER || block.getType() == Material.BUBBLE_COLUMN || isWaterPlant(block.getType())) return true;
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    private BlockState toAbsorbedState(Block block) {
        BlockState state = block.getState();
        if (block.getType() == Material.WATER || block.getType() == Material.BUBBLE_COLUMN || isWaterPlant(block.getType())) {
            state.setType(Material.AIR);
            return state;
        }
        BlockData data = state.getBlockData();
        if (data instanceof Waterlogged waterlogged) {
            waterlogged.setWaterlogged(false);
            state.setBlockData(data);
        }
        return state;
    }

    private void clearResidualWater(Set<Block> absorbedBlocks) {
        for (Block block : absorbedBlocks) {
            Material material = block.getType();
            if (material == Material.WATER || material == Material.BUBBLE_COLUMN || isWaterPlant(material)) {
                block.setType(Material.AIR, false);
                continue;
            }
            BlockData data = block.getBlockData();
            if (data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
                waterlogged.setWaterlogged(false);
                block.setBlockData(data, false);
            }
        }
    }

    private void syncClientBlocks(Block center, Set<Block> absorbedBlocks, int radius) {
        List<BlockState> states = absorbedBlocks.stream().map(Block::getState).toList();
        int syncRadius = radius + getServer().getViewDistance() * 16;
        for (Player player : center.getWorld().getPlayers()) {
            if (Math.abs(player.getLocation().getX() - center.getX()) <= syncRadius
                    && Math.abs(player.getLocation().getZ() - center.getZ()) <= syncRadius) {
                player.sendBlockChanges(states);
            }
        }
    }

    private boolean isWaterPlant(Material material) {
        return material == Material.SEAGRASS
                || material == Material.TALL_SEAGRASS
                || material == Material.KELP
                || material == Material.KELP_PLANT;
    }

    private void notifyAbsorption(Block center, int count) {
        if (count <= 0) return;
        center.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distanceSquared(center.getLocation()) <= 64)
                .min((first, second) -> Double.compare(first.getLocation().distanceSquared(center.getLocation()), second.getLocation().distanceSquared(center.getLocation())))
                .ifPresent(player -> send(player, "absorbed", Map.of("count", Integer.toString(count))));
    }

    private boolean runCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        if (!canManage(sender)) { send(sender, "no-permission", Map.of()); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "0", "1" -> { boolean enabled = args[0].equals("1"); getConfig().set("enabled", enabled); saveConfig(); send(sender, enabled ? "enabled" : "disabled", Map.of()); }
            case "reload" -> { reloadConfig(); reloadLanguage(); send(sender, "reloaded", Map.of()); }
            case "info" -> send(sender, "info", Map.of("version", getPluginMeta().getVersion()));
            case "status" -> send(sender, "status", Map.of("enabled", isFeatureEnabled() ? "ON" : "OFF", "radius", Integer.toString(getAbsorptionRadius()), "diameter", Integer.toString(getAbsorptionRadius() * 2 + 1)));
            case "set" -> setRadius(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void setRadius(CommandSender sender, String[] args) {
        if (args.length != 2) {
            send(sender, "usage-set", Map.of());
            return;
        }
        try { int radius = Integer.parseInt(args[1]); if (radius < 1 || radius > MAX_ABSORPTION_RADIUS) throw new NumberFormatException(); getConfig().set("absorption-radius", radius); saveConfig(); send(sender, "radius-set", Map.of("radius", Integer.toString(radius), "diameter", Integer.toString(radius * 2 + 1))); }
        catch (NumberFormatException exception) { send(sender, "invalid-radius", Map.of()); }
    }

    private void help(CommandSender sender) { for (String key : List.of("help-header", "help-toggle", "help-reload", "help-info", "help-status", "help-set", "help-footer")) send(sender, key, Map.of()); }
    private boolean isFeatureEnabled() { return getConfig().getBoolean("enabled", true); }
    private int getAbsorptionRadius() { return Math.clamp(getConfig().getInt("absorption-radius", 10), 1, MAX_ABSORPTION_RADIUS); }
    private boolean canManage(CommandSender sender) { return !(sender instanceof Player) || !getConfig().getBoolean("admin-require-op", true) || sender.isOp() || sender.hasPermission("bm-minecraft-sponge-plus.admin"); }
    private void reloadLanguage() {
        saveResource("active-language.yml", true);
        String locale = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "active-language.yml")).getString("language", "zh_TW");
        String path = getResource("lang/" + locale + ".yml") == null ? "lang/zh_TW.yml" : "lang/" + locale + ".yml";
        File file = new File(getDataFolder(), path);
        YamlConfiguration bundled = loadBundled(path);
        boolean mayUpdate = prepareLanguageFile(file, path, bundled);
        language = YamlConfiguration.loadConfiguration(file);
        language.setDefaults(bundled);
        language.options().copyDefaults(true);
        if (!mayUpdate) return;
        try {
            language.save(file);
        } catch (IOException exception) {
            getLogger().warning(console(bundled, "language-update-failed").replace("{file}", file.getName()));
        }
    }

    private YamlConfiguration loadBundled(String path) {
        try (InputStream input = getResource(path)) {
            if (input == null) return new YamlConfiguration();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return new YamlConfiguration();
        }
    }

    private boolean prepareLanguageFile(File file, String path, YamlConfiguration bundled) {
        if (!file.isFile()) {
            return copyBundled(path, file, bundled);
        }
        int bundledVersion = bundled.getInt("language-format-version", 1);
        int localVersion = YamlConfiguration.loadConfiguration(file).getInt("language-format-version", 1);
        if (localVersion >= bundledVersion) return true;
        File backup = new File(file.getParentFile(),
                file.getName() + ".pre-v" + bundledVersion + ".bak");
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            getLogger().warning(console(bundled, "language-backup-failed")
                    .replace("{file}", file.getName()).replace("{backup}", backup.getName()));
            return false;
        }
        return copyBundled(path, file, bundled);
    }

    private boolean copyBundled(String path, File file, YamlConfiguration bundled) {
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            getLogger().warning(console(bundled, "language-directory-failed").replace("{directory}", parent.getPath()));
            return false;
        }
        try (InputStream input = getResource(path)) {
            if (input == null) return false;
            Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            getLogger().warning(console(bundled, "language-copy-failed").replace("{file}", file.getName()));
            return false;
        }
    }

    private void send(CommandSender sender, String key, Map<String, String> values) { sender.sendMessage(colour(replace(language.getString("messages." + key, key), values))); }
    private String console(String key) { return console(language, key); }
    private String console(YamlConfiguration source, String key) { return source.getString("console." + key, key); }
    private String replace(String value, Map<String, String> values) { for (Map.Entry<String, String> entry : values.entrySet()) value = value.replace("{" + entry.getKey() + "}", entry.getValue()); return value; }
    private String colour(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
}
