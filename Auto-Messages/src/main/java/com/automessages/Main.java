package com.automessages;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound; 
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoMessages plugin: scheduled messages with clickable links and hex colors.
 */
public class Main extends JavaPlugin {
    private final List<BukkitTask> tasks = new ArrayList<>();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final long TICKS_PER_SECOND = 20L;
    private static final String LOG_PREFIX = "[AutoMessages] ";

    private void info(String msg) { getLogger().info(LOG_PREFIX + msg); }
    private void warn(String msg) { getLogger().warning(LOG_PREFIX + msg); }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        printBanner();
        validateAndReport();
        startupSummary();
        loadGroups();
        info("Enabled. Active groups: " + tasks.size());
    }

    @Override
    public void onDisable() {
        cancelAll();
        info("Disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"automessages".equalsIgnoreCase(command.getName())) return false;
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("automessages.admin")) {
                sender.sendMessage("\u00A7cMissing permission: automessages.admin");
                return true;
            }
            reloadConfig();
            cancelAll();
            loadGroups();
            sender.sendMessage("\u00A7aAutoMessages reloaded. Active groups: " + tasks.size());
            return true;
        }
        sender.sendMessage("\u00A7eUsage: /automessages reload");
        return true;
    }

    private void cancelAll() {
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
    }

    /**
     * Load groups from config and schedule periodic broadcasts.
     */
    private void loadGroups() {
        ConfigurationSection root = getConfig().getConfigurationSection("AutoMessages");
        if (root == null) {
            warn("Config is missing 'AutoMessages' section.");
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            boolean enabled = sec.getBoolean("enabled", true);
            if (!enabled) continue;

            long intervalSec = getAsLongSeconds(sec.get("interval"), 300);
            long waitSec = getAsLongSeconds(sec.get("wait"), 0);
            String permission = sec.getString("permission", "");

            boolean soundEnabled = false; Sound sound = null; float volume = 1.0f; float pitch = 1.0f;
            ConfigurationSection snd = sec.getConfigurationSection("sound");
            if (snd != null) {
                soundEnabled = snd.getBoolean("enabled", false);
                String name = snd.getString("name", "");
                volume = (float) snd.getDouble("volume", 1.0);
                pitch = (float) snd.getDouble("pitch", 1.0);
                if (soundEnabled && name != null && !name.isEmpty()) {
                    try { sound = Sound.valueOf(name); }
                    catch (IllegalArgumentException ex) {
                        soundEnabled = false;
                        getLogger().warning("Unknown sound '" + name + "' for group '" + key + "'. Disabling sound.");
                    }
                }
            }

            List<String> lines = sec.getStringList("messages");
            if (lines == null || lines.isEmpty()) {
                warn("Group '" + key + "' has no messages; skipping.");
                continue;
            }

            long initialDelayTicks = Math.max(1L, waitSec * TICKS_PER_SECOND);
            long periodTicks = Math.max(1L, intervalSec * TICKS_PER_SECOND);

            final List<String> linesFinal = new ArrayList<>(lines);
            final String permissionFinal = permission;
            final boolean soundEnabledFinal = soundEnabled;
            final Sound soundFinal = sound;
            final float volumeFinal = volume;
            final float pitchFinal = pitch;
            final String keyFinal = key;

            BukkitTask task = new BukkitRunnable() {
                @SuppressWarnings("deprecation") // legacy text conversion API
                @Override public void run() {
                    List<Player> targets = collectTargets(permissionFinal);
                    for (String raw : linesFinal) {
                        String withPlaceholders = applyPlaceholders(raw);
                        BaseComponent[] comps = buildComponents(withPlaceholders);
                        for (Player p : targets) {
                            try {
                                p.spigot().sendMessage(comps);
                            } catch (Exception t) {
                                String fallback = translateLegacy(preprocessHex(withPlaceholders));
                                p.sendMessage(fallback);
                            }
                            if (soundEnabledFinal && soundFinal != null) {
                                try { p.playSound(p.getLocation(), soundFinal, volumeFinal, pitchFinal); } catch (Exception ignored) {}
                            }
                        }
                    }
                    info("Sent group '" + keyFinal + "' to " + targets.size() + " player(s)");
                }
            }.runTaskTimer(this, initialDelayTicks, periodTicks);
            tasks.add(task);
        }
    }

    private void printBanner() {
        info("=====================================");
        info(" AutoMessages");
        info(" Compatible: Paper/Purpur/Spigot 1.13–1.21+ (no NMS)");
        info("=====================================");
    }

    /* Validate config and log summary of loaded groups. */
    private void validateAndReport() {
        ConfigurationSection root = getConfig().getConfigurationSection("AutoMessages");
        if (root == null) {
            warn("Config missing 'AutoMessages' section.");
            return;
        }
        int total = 0, enabled = 0, issues = 0;
        for (String key : root.getKeys(false)) {
            total++;
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) { issues++; warn("Group '" + key + "' is not a section."); continue; }
            boolean en = sec.getBoolean("enabled", true);
            if (en) enabled++;
            long intervalSec = getAsLongSeconds(sec.get("interval"), -1);
            long waitSec = getAsLongSeconds(sec.get("wait"), 0);
            List<String> lines = sec.getStringList("messages");
            if (intervalSec <= 0) { issues++; warn("Group '" + key + "' has invalid interval (seconds)." ); }
            if (waitSec < 0) { issues++; warn("Group '" + key + "' has negative wait."); }
            if (lines == null || lines.isEmpty()) { issues++; warn("Group '" + key + "' has no messages."); }
            ConfigurationSection snd = sec.getConfigurationSection("sound");
            if (snd != null && snd.getBoolean("enabled", false)) {
                String name = snd.getString("name", "");
                if (name == null || name.isEmpty()) { issues++; warn("Group '" + key + "' sound enabled but name missing."); }
                else {
                    try { Sound.valueOf(name); } catch (IllegalArgumentException ex) {
                        issues++; warn("Group '" + key + "' unknown sound '" + name + "'.");
                    }
                }
            }
        }
        boolean hexOk = supportsHexColors();
        info("Config groups: total=" + total + ", enabled=" + enabled + ", issues=" + issues);
        info("Hex colors: " + (hexOk ? "enabled (1.16+)" : "disabled (<1.16)"));
    }

    /* Print startup banner and group stats. */
    private void startupSummary() {
        String ver = getPluginMeta().getVersion();
        String bVer = Bukkit.getServer().getBukkitVersion();
        boolean paperTps = hasPaperTps();
        boolean hexOk = supportsHexColors();
        int online = Bukkit.getOnlinePlayers().size();

        info("=====================================");
        info(String.format("Startup Summary | Version %s", ver));
        info(String.format("Bukkit=%s | PaperTPS=%s | HexColors=%s | Online=%d",
                bVer,
                paperTps ? "yes" : "no",
                hexOk ? "on" : "off",
                online));

        ConfigurationSection root = getConfig().getConfigurationSection("AutoMessages");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(key);
                if (sec == null) continue;

                boolean enabled = sec.getBoolean("enabled", true);
                long intervalSec = getAsLongSeconds(sec.get("interval"), -1);
                long waitSec = getAsLongSeconds(sec.get("wait"), 0);
                String permission = sec.getString("permission", "");
                String recipients = (permission == null || permission.isEmpty()) ? "all" : permission;
                int lines = sec.getStringList("messages").size();

                ConfigurationSection snd = sec.getConfigurationSection("sound");
                boolean soundEnabled = snd != null && snd.getBoolean("enabled", false);
                String soundName = snd != null ? snd.getString("name", "") : "";
                String soundInfo = (soundEnabled && soundName != null && !soundName.isEmpty()) ? soundName : "disabled";

                info(String.format("- Group: %s", key));
                info(String.format("  status    : %s", enabled ? "enabled" : "disabled"));
                info(String.format("  timing    : interval=%ss, wait=%ss (first run in %ss)",
                        intervalSec, waitSec, Math.max(0, waitSec)));
                info(String.format("  recipients: %s", recipients));
                info(String.format("  sound     : %s", soundInfo));
                info(String.format("  messages  : %d line(s)", lines));
            }
        }
        info("=====================================");
    }

    private boolean hasPaperTps() {
        try { Bukkit.getServer().getClass().getMethod("getTPS"); return true; }
        catch (NoSuchMethodException ex) { return false; }
    }

    /* Collect recipients for a group (permission holders or all online). */
    private List<Player> collectTargets(String permission) {
        List<Player> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (permission == null || permission.isEmpty() || p.hasPermission(permission)) list.add(p);
        }
        return list;
    }

    private static long getAsLongSeconds(Object v, long def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (NumberFormatException ex) { return def; }
    }

    private BaseComponent[] buildComponents(String s) {
        String pre = preprocessHex(s);
        List<BaseComponent> result = new ArrayList<>();
        int idx = 0;
        while (idx < pre.length()) {
            int nextMd = indexOfMdLink(pre, idx);
            int nextBt = pre.indexOf('`', idx);
            int next = minPos(nextMd, nextBt);
            if (next == -1) {
                appendLegacy(result, pre.substring(idx));
                break;
            }
            appendLegacy(result, pre.substring(idx, next));
            if (next == nextMd) {
                int closeText = pre.indexOf(']', next);
                int openParen = closeText >= 0 ? pre.indexOf('(', closeText) : -1;
                int closeParen = openParen >= 0 ? pre.indexOf(')', openParen) : -1;
                if (closeText < 0 || openParen < 0 || closeParen < 0) { // fallback
                    appendLegacy(result, pre.substring(next));
                    break;
                }
                String text = pre.substring(next + 1, closeText);
                String target = pre.substring(openParen + 1, closeParen);
                appendClickable(result, text, target);
                idx = closeParen + 1;
            } else {
                int end = pre.indexOf('`', next + 1);
                if (end < 0) { appendLegacy(result, pre.substring(next)); break; }
                String target = pre.substring(next + 1, end).trim();
                appendClickable(result, target, target);
                idx = end + 1;
            }
        }
        return result.toArray(new BaseComponent[0]);
    }

    private static int minPos(int a, int b) {
        if (a == -1) return b;
        if (b == -1) return a;
        return Math.min(a, b);
    }

    private static int indexOfMdLink(String s, int from) {
        int i = s.indexOf('[', from);
        if (i == -1) return -1;
        int j = s.indexOf(']', i + 1);
        int k = j >= 0 ? s.indexOf('(', j + 1) : -1;
        return (j >= 0 && k >= 0) ? i : -1;
    }

    private void appendLegacy(List<BaseComponent> out, String legacy) {
        if (legacy.isEmpty()) return;
        BaseComponent[] comps = TextComponent.fromLegacyText(translateLegacy(legacy));
        for (BaseComponent c : comps) out.add(c);
    }

    private void appendClickable(List<BaseComponent> out, String text, String target) {
        if (target == null || target.isEmpty()) { appendLegacy(out, text); return; }
        BaseComponent[] comps = TextComponent.fromLegacyText(translateLegacy(text));
        ClickEvent.Action action = target.startsWith("/") ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.OPEN_URL;
        ClickEvent evt = new ClickEvent(action, target);
        for (BaseComponent c : comps) {
            c.setClickEvent(evt);
            out.add(c);
        }
    }

    /* Apply basic placeholders like online player count. */
    private String applyPlaceholders(String s) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getServer().getMaxPlayers();
        return s.replace("%online%", String.valueOf(online))
                .replace("%maxplayers%", String.valueOf(max))
                .replace("{online}", String.valueOf(online))
                .replace("{onlinePlayers}", String.valueOf(online));
    }

    /* Preprocess hex color codes for legacy support. */
    private String preprocessHex(String s) {
        Matcher m = HEX_PATTERN.matcher(s);
        StringBuffer sb = new StringBuffer();
        boolean any = false;
        while (m.find()) {
            String hex = m.group(1);
            String replacement = supportsHexColors() ? toSectionHex(hex) : "";
            if (!replacement.isEmpty()) any = true;
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return any ? sb.toString() : s;
    }

    /* Check if server supports hex colors (1.16+). */
    private boolean supportsHexColors() {
        String v = Bukkit.getServer().getBukkitVersion();
        try {
            String[] parts = v.split("-")[0].split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return (major > 1) || (major == 1 && minor >= 16);
        } catch (Exception ignored) { return false; }
    }

    private String toSectionHex(String hex) {
        StringBuilder sb = new StringBuilder("\u00A7x");
        for (char c : hex.toCharArray()) { sb.append('\u00A7').append(c); }
        return sb.toString();
    }

    private String translateLegacy(String s) {
        return s.replace('&', '\u00A7');
    }
}