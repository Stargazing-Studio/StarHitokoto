package com.xinglingqaq.starhitokoto;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class StarHitokoto extends JavaPlugin implements Listener {
    
    private String hitokotoApiUrl;
    private String currentHitokoto = "Loading...";
    private List<String> wrappedHitokoto = new ArrayList<>();
    private long lastFetchTime = 0;
    private long cacheTime;
    private long updateInterval;
    private int maxCharsPerLine = 8;
    private boolean addColorPrefix = true;
    
    // 消息配置
    private boolean joinMessageEnabled = true;
    private String joinMessageFormat = "&e欢迎 &b%player% &e加入服务器! &7今日一言: &f%hitokoto%";
    private boolean deathMessageEnabled = true;
    private String deathMessageFormat = "&c%player% &7已死亡. &7死亡感言: &f%hitokoto%";
    
    // Title配置
    private boolean joinTitleEnabled = true;
    private String joinTitleFormat = "&b欢迎 %player% 加入服务器";
    private String joinSubtitleFormat = "&7%hitokoto%";
    private int titleFadeIn = 10;
    private int titleStay = 70;
    private int titleFadeOut = 20;
    
    // Debug配置
    private boolean debugEnabled = false;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Load configuration
        loadConfig();
        
        // Fetch initial quote
        fetchHitokoto();
        
        // Register placeholder expansion
        new HitokotoExpansion(this).register();
        
        // Set up scheduled task to update the quote
        new BukkitRunnable() {
            @Override
            public void run() {
                fetchHitokoto();
            }
        }.runTaskTimerAsynchronously(this, updateInterval, updateInterval);
        
        // Register command
        getCommand("hitokoto").setExecutor(this);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("[XingLingQAQ]StarHitokoto - 已成功启动!");
    }

    @Override
    public void onDisable() {
        getLogger().info("[XingLingQAQ]StarHitokoto - 已成功卸载!");
    }
    
    // 日志记录方法
    private void debug(String message) {
        if (debugEnabled) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    // 警告级别日志
    private void debugWarning(String message) {
        if (debugEnabled) {
            getLogger().warning("[DEBUG] " + message);
        }
    }
    
    // 错误级别日志
    private void debugError(String message, Throwable throwable) {
        if (debugEnabled) {
            getLogger().log(Level.SEVERE, "[DEBUG] " + message, throwable);
        }
    }
    
    /**
     * 玩家加入服务器事件处理
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 处理加入消息
        if (joinMessageEnabled) {
            String message = joinMessageFormat
                    .replace("%player%", player.getName())
                    .replace("%hitokoto%", currentHitokoto);
            
            // 取消默认的加入消息
            event.setJoinMessage(null);
            
            // 发送自定义消息
            getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
            debug("已向玩家 " + player.getName() + " 发送加入消息");
        }
        
        // 处理Title显示
        if (joinTitleEnabled) {
            String title = joinTitleFormat.replace("%player%", player.getName());
            String subtitle = joinSubtitleFormat.replace("%hitokoto%", currentHitokoto);
            
            // 发送Title
            player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                titleFadeIn,
                titleStay,
                titleFadeOut
            );
            debug("已向玩家 " + player.getName() + " 发送加入Title");
        }
    }
    
    /**
     * 玩家死亡事件处理
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathMessageEnabled) return;
        
        Player player = event.getEntity();
        String message = deathMessageFormat
                .replace("%player%", player.getName())
                .replace("%hitokoto%", currentHitokoto);
        
        // 取消默认的死亡消息
        event.setDeathMessage(null);
        
        // 发送自定义消息
        getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
        debug("已向玩家 " + player.getName() + " 发送死亡消息");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hitokoto")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("refresh")) {
                    if (sender.hasPermission("hitokoto.refresh")) {
                        // Force refresh in async task
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                lastFetchTime = 0; // Reset cache timer
                                fetchHitokoto();
                                
                                // Send message in main thread
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        sender.sendMessage(ChatColor.GREEN + "Hitokoto refreshed: " + ChatColor.RESET + currentHitokoto);
                                        sender.sendMessage(ChatColor.GREEN + "分行显示: ");
                                        for (String line : wrappedHitokoto) {
                                            sender.sendMessage(ChatColor.RESET + line);
                                        }
                                    }
                                }.runTask(StarHitokoto.this);
                            }
                        }.runTaskAsynchronously(this);
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("debug")) {
                    // 添加诊断命令，帮助用户识别问题
                    if (sender.hasPermission("hitokoto.debug")) {
                        sender.sendMessage(ChatColor.GOLD + "===== Hitokoto Debug Info =====");
                        sender.sendMessage(ChatColor.GOLD + "当前一言: " + ChatColor.RESET + currentHitokoto);
                        sender.sendMessage(ChatColor.GOLD + "分行数量: " + ChatColor.RESET + wrappedHitokoto.size());
                        
                        sender.sendMessage(ChatColor.GOLD + "分行内容:");
                        for (int i = 0; i < wrappedHitokoto.size(); i++) {
                            sender.sendMessage(ChatColor.GOLD + "行 " + i + ": " + ChatColor.WHITE + 
                                wrappedHitokoto.get(i) + ChatColor.GRAY + " [占位符: %hitokoto_line_" + i + "%]");
                        }
                        
                        sender.sendMessage(ChatColor.GOLD + "TAB侧边栏配置示例:");
                        sender.sendMessage(ChatColor.GRAY + "scoreboard:");
                        sender.sendMessage(ChatColor.GRAY + "  enabled: true");
                        sender.sendMessage(ChatColor.GRAY + "  title: \"一言\"");
                        sender.sendMessage(ChatColor.GRAY + "  lines:");
                        for (int i = 0; i < Math.min(wrappedHitokoto.size(), 5); i++) {
                            sender.sendMessage(ChatColor.GRAY + "    - \"%hitokoto_line_" + i + "%\"");
                        }
                        
                        sender.sendMessage(ChatColor.GOLD + "调试模式: " + (debugEnabled ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭"));
                        sender.sendMessage(ChatColor.GOLD + "确保在TAB配置中使用上述占位符格式!");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "您没有权限使用此命令。");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("config")) {
                    // 显示或修改消息配置信息
                    if (sender.hasPermission("hitokoto.config")) {
                        if (args.length > 1 && args[1].equalsIgnoreCase("set")) {
                            // 命令格式: /hitokoto config set <category> <option> <value>
                            if (args.length >= 5) {
                                String category = args[2].toLowerCase();
                                String option = args[3].toLowerCase();
                                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                                
                                boolean success = setConfig(category, option, value);
                                if (success) {
                                    sender.sendMessage(ChatColor.GREEN + "配置已更新: " + category + "." + option + " = " + value);
                                    // 保存配置
                                    saveConfig();
                                    // 重新加载配置
                                    loadConfig();
                                } else {
                                    sender.sendMessage(ChatColor.RED + "无法更新配置。请检查分类和选项名称是否正确。");
                                    sender.sendMessage(ChatColor.RED + "用法: /hitokoto config set <category> <option> <value>");
                                    sender.sendMessage(ChatColor.RED + "示例: /hitokoto config set message.join enabled false");
                                    sender.sendMessage(ChatColor.RED + "示例: /hitokoto config set debug true");
                                }
                                return true;
                            } else {
                                sender.sendMessage(ChatColor.RED + "参数不足。");
                                sender.sendMessage(ChatColor.RED + "用法: /hitokoto config set <category> <option> <value>");
                                sender.sendMessage(ChatColor.RED + "示例: /hitokoto config set message.join enabled false");
                                return true;
                            }
                        }
                        
                        // 显示当前配置
                        sender.sendMessage(ChatColor.GOLD + "===== Hitokoto 消息配置 =====");
                        sender.sendMessage(ChatColor.GOLD + "玩家加入消息: " + (joinMessageEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
                        sender.sendMessage(ChatColor.GOLD + "加入消息格式: " + ChatColor.RESET + 
                            ChatColor.translateAlternateColorCodes('&', joinMessageFormat));
                        
                        sender.sendMessage(ChatColor.GOLD + "玩家加入Title: " + (joinTitleEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
                        sender.sendMessage(ChatColor.GOLD + "Title格式: " + ChatColor.RESET + 
                            ChatColor.translateAlternateColorCodes('&', joinTitleFormat));
                        sender.sendMessage(ChatColor.GOLD + "Subtitle格式: " + ChatColor.RESET + 
                            ChatColor.translateAlternateColorCodes('&', joinSubtitleFormat));
                        sender.sendMessage(ChatColor.GOLD + "Title显示时间: " + 
                            ChatColor.RESET + "淡入:" + titleFadeIn + "tick, 停留:" + titleStay + "tick, 淡出:" + titleFadeOut + "tick");
                        
                        sender.sendMessage(ChatColor.GOLD + "玩家死亡消息: " + (deathMessageEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
                        sender.sendMessage(ChatColor.GOLD + "死亡消息格式: " + ChatColor.RESET + 
                            ChatColor.translateAlternateColorCodes('&', deathMessageFormat));
                        
                        sender.sendMessage(ChatColor.GOLD + "调试模式: " + (debugEnabled ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭"));
                        
                        // 显示测试命令
                        sender.sendMessage(ChatColor.GOLD + "测试命令:");
                        sender.sendMessage(ChatColor.GRAY + "/hitokoto test join - 测试玩家加入消息");
                        sender.sendMessage(ChatColor.GRAY + "/hitokoto test death - 测试玩家死亡消息");
                        sender.sendMessage(ChatColor.GRAY + "/hitokoto test title - 测试玩家加入Title");
                        
                        // 显示管理命令
                        sender.sendMessage(ChatColor.GOLD + "管理命令:");
                        sender.sendMessage(ChatColor.GRAY + "/hitokoto reload - 重新加载插件配置");
                        sender.sendMessage(ChatColor.GRAY + "/hitokoto config set <分类> <选项> <值> - 修改配置");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "您没有权限使用此命令。");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    // 添加重载配置命令
                    if (sender.hasPermission("hitokoto.reload")) {
                        // 重新加载配置
                        reloadConfig();
                        loadConfig();
                        
                        // 强制刷新一言
                        lastFetchTime = 0;
                        fetchHitokoto();
                        
                        sender.sendMessage(ChatColor.GREEN + "StarHitokoto 配置已重新加载！");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "您没有权限使用此命令。");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("test") && args.length > 1) {
                    // 测试功能
                    if (args[1].equalsIgnoreCase("join")) {
                        if (sender.hasPermission("hitokoto.test.join")) {
                            // 测试加入消息
                            String playerName = sender.getName();
                            if (sender instanceof Player) {
                                playerName = ((Player) sender).getDisplayName();
                            }
                            
                            String message = joinMessageFormat
                                    .replace("%player%", playerName)
                                    .replace("%hitokoto%", currentHitokoto);
                            
                            getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                            sender.sendMessage(ChatColor.GREEN + "已发送测试加入消息");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "您没有权限使用此命令。");
                            return true;
                        }
                    } else if (args[1].equalsIgnoreCase("death")) {
                        if (sender.hasPermission("hitokoto.test.death")) {
                            // 测试死亡消息
                            String playerName = sender.getName();
                            if (sender instanceof Player) {
                                playerName = ((Player) sender).getDisplayName();
                            }
                            
                            String message = deathMessageFormat
                                    .replace("%player%", playerName)
                                    .replace("%hitokoto%", currentHitokoto);
                            
                            getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                            sender.sendMessage(ChatColor.GREEN + "已发送测试死亡消息");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "您没有权限使用此命令。");
                            return true;
                        }
                    } else if (args[1].equalsIgnoreCase("title")) {
                        if (sender.hasPermission("hitokoto.test.title")) {
                            // 测试Title
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                
                                String title = joinTitleFormat.replace("%player%", player.getName());
                                String subtitle = joinSubtitleFormat.replace("%hitokoto%", currentHitokoto);
                                
                                player.sendTitle(
                                    ChatColor.translateAlternateColorCodes('&', title),
                                    ChatColor.translateAlternateColorCodes('&', subtitle),
                                    titleFadeIn,
                                    titleStay,
                                    titleFadeOut
                                );
                                
                                sender.sendMessage(ChatColor.GREEN + "已显示测试Title");
                            } else {
                                sender.sendMessage(ChatColor.RED + "只有玩家可以测试Title");
                            }
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "您没有权限使用此命令。");
                            return true;
                        }
                    }
                }
            }
            
            // 基本的一言显示命令需要基本使用权限
            if (sender.hasPermission("hitokoto.use")) {
                sender.sendMessage(ChatColor.GOLD + "Hitokoto: " + ChatColor.RESET + currentHitokoto);
                sender.sendMessage(ChatColor.GOLD + "分行显示: ");
                for (String line : wrappedHitokoto) {
                    sender.sendMessage(ChatColor.RESET + line);
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "您没有权限使用此命令。");
                return true;
            }
        }
        return false;
    }
    
    private void loadConfig() {
        hitokotoApiUrl = getConfig().getString("api.url", "https://v1.hitokoto.cn/?c=f&encode=text");
        cacheTime = getConfig().getLong("cache.time", 60000);
        updateInterval = getConfig().getLong("update.interval", 1200);
        maxCharsPerLine = getConfig().getInt("line.max-chars", 8);
        addColorPrefix = getConfig().getBoolean("line.add-color-prefix", true);
        
        // 加载调试配置
        debugEnabled = getConfig().getBoolean("debug", false);
        
        // 加载消息配置
        joinMessageEnabled = getConfig().getBoolean("message.join.enabled", true);
        joinMessageFormat = getConfig().getString("message.join.format", 
                "&e欢迎 &b%player% &e加入服务器! &7今日一言: &f%hitokoto%");
        
        // 加载Title配置
        joinTitleEnabled = getConfig().getBoolean("message.join.title.enabled", true);
        joinTitleFormat = getConfig().getString("message.join.title.title", 
                "&b欢迎 %player% 加入服务器");
        joinSubtitleFormat = getConfig().getString("message.join.title.subtitle", 
                "&7%hitokoto%");
        titleFadeIn = getConfig().getInt("message.join.title.fade-in", 10);
        titleStay = getConfig().getInt("message.join.title.stay", 70);
        titleFadeOut = getConfig().getInt("message.join.title.fade-out", 20);
        
        deathMessageEnabled = getConfig().getBoolean("message.death.enabled", true);
        deathMessageFormat = getConfig().getString("message.death.format", 
                "&c%player% &7已死亡. &7死亡感言: &f%hitokoto%");
        
        getLogger().info("Configuration loaded.");
        debug("调试模式已开启");
        debug("API URL: " + hitokotoApiUrl);
        debug("Cache Time: " + cacheTime + "ms");
        debug("Update Interval: " + updateInterval + " ticks");
        debug("Max Chars Per Line: " + maxCharsPerLine);
        debug("Add Color Prefix: " + addColorPrefix);
        debug("Join Message Enabled: " + joinMessageEnabled);
        debug("Join Title Enabled: " + joinTitleEnabled);
        debug("Death Message Enabled: " + deathMessageEnabled);
    }
    
    private void fetchHitokoto() {
        // Only fetch if cache is expired
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFetchTime < cacheTime) {
            return;
        }
        
        try {
            URL url = new URL(hitokotoApiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    currentHitokoto = response.toString().trim();
                    updateWrappedHitokoto();
                    lastFetchTime = currentTime;
                    debug("成功获取新一言: " + currentHitokoto);
                }
            } else {
                debugWarning("获取一言失败，状态码: " + status);
            }
            connection.disconnect();
        } catch (IOException e) {
            debugError("获取一言时出错", e);
        }
    }
    
    // 将一言分割成每行最多指定字符数
    private void updateWrappedHitokoto() {
        wrappedHitokoto.clear();
        String hitokoto = currentHitokoto;
        
        // 按照设定的最大字符数分割
        for (int i = 0; i < hitokoto.length(); i += maxCharsPerLine) {
            int end = Math.min(i + maxCharsPerLine, hitokoto.length());
            String lineContent = hitokoto.substring(i, end);
            
            // 根据配置添加颜色前缀
            if (addColorPrefix) {
                String prefix = ChatColor.RESET.toString() + ChatColor.WHITE.toString();
                wrappedHitokoto.add(prefix + lineContent);
            } else {
                wrappedHitokoto.add(lineContent);
            }
        }
        
        // 如果没有内容，添加一个空行
        if (wrappedHitokoto.isEmpty()) {
            wrappedHitokoto.add("");
        }
        
        // 打印日志以便调试
        debug("已将一言分行为 " + wrappedHitokoto.size() + " 行 (每行最多 " + maxCharsPerLine + " 字符):");
        for (int i = 0; i < wrappedHitokoto.size(); i++) {
            debug("第 " + i + " 行: " + wrappedHitokoto.get(i));
        }
    }
    
    public String getCurrentHitokoto() {
        // If cache is expired, fetch new quote
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFetchTime > cacheTime) {
            fetchHitokoto();
        }
        return currentHitokoto;
    }
    
    public List<String> getWrappedHitokoto() {
        // If cache is expired, fetch new quote
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFetchTime > cacheTime) {
            fetchHitokoto();
        }
        return wrappedHitokoto;
    }
    
    // 获取指定行的分行一言
    public String getWrappedHitokotoLine(int line) {
        List<String> wrapped = getWrappedHitokoto();
        if (line >= 0 && line < wrapped.size()) {
            String content = wrapped.get(line);
            return content.isEmpty() ? null : content;  // 如果内容为空，返回null而不是空字符串
        }
        return null;  // 超出索引范围也返回null
    }
    
    // 获取debug状态
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    // 设置配置文件的方法
    private boolean setConfig(String category, String option, String value) {
        try {
            if (category.equals("debug")) {
                // 处理顶级配置项
                boolean boolValue = Boolean.parseBoolean(value);
                getConfig().set("debug", boolValue);
                return true;
            } else if (category.startsWith("message.")) {
                // 处理消息相关配置
                String[] parts = category.split("\\.");
                if (parts.length >= 2) {
                    String messageType = parts[1]; // join, death
                    
                    if (messageType.equals("join")) {
                        if (parts.length == 2) {
                            // 直接处理join下的选项
                            if (option.equals("enabled")) {
                                boolean boolValue = Boolean.parseBoolean(value);
                                getConfig().set("message.join.enabled", boolValue);
                                return true;
                            } else if (option.equals("format")) {
                                getConfig().set("message.join.format", value);
                                return true;
                            }
                        } else if (parts.length == 3 && parts[2].equals("title")) {
                            // 处理title下的选项
                            if (option.equals("enabled")) {
                                boolean boolValue = Boolean.parseBoolean(value);
                                getConfig().set("message.join.title.enabled", boolValue);
                                return true;
                            } else if (option.equals("title")) {
                                getConfig().set("message.join.title.title", value);
                                return true;
                            } else if (option.equals("subtitle")) {
                                getConfig().set("message.join.title.subtitle", value);
                                return true;
                            } else if (option.equals("fade-in")) {
                                int intValue = Integer.parseInt(value);
                                getConfig().set("message.join.title.fade-in", intValue);
                                return true;
                            } else if (option.equals("stay")) {
                                int intValue = Integer.parseInt(value);
                                getConfig().set("message.join.title.stay", intValue);
                                return true;
                            } else if (option.equals("fade-out")) {
                                int intValue = Integer.parseInt(value);
                                getConfig().set("message.join.title.fade-out", intValue);
                                return true;
                            }
                        }
                    } else if (messageType.equals("death")) {
                        if (option.equals("enabled")) {
                            boolean boolValue = Boolean.parseBoolean(value);
                            getConfig().set("message.death.enabled", boolValue);
                            return true;
                        } else if (option.equals("format")) {
                            getConfig().set("message.death.format", value);
                            return true;
                        }
                    }
                }
            } else if (category.equals("api")) {
                if (option.equals("url")) {
                    getConfig().set("api.url", value);
                    return true;
                }
            } else if (category.equals("cache")) {
                if (option.equals("time")) {
                    long longValue = Long.parseLong(value);
                    getConfig().set("cache.time", longValue);
                    return true;
                }
            } else if (category.equals("update")) {
                if (option.equals("interval")) {
                    long longValue = Long.parseLong(value);
                    getConfig().set("update.interval", longValue);
                    return true;
                }
            } else if (category.equals("line")) {
                if (option.equals("max-chars")) {
                    int intValue = Integer.parseInt(value);
                    getConfig().set("line.max-chars", intValue);
                    return true;
                } else if (option.equals("add-color-prefix")) {
                    boolean boolValue = Boolean.parseBoolean(value);
                    getConfig().set("line.add-color-prefix", boolValue);
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            getLogger().warning("设置配置时出错: " + e.getMessage());
            return false;
        }
    }
    
    // Inner class for PlaceholderAPI expansion
    private class HitokotoExpansion extends PlaceholderExpansion {
        
        private final StarHitokoto plugin;
        
        public HitokotoExpansion(StarHitokoto plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public String getIdentifier() {
            return "hitokoto";
        }
        
        @Override
        public String getAuthor() {
            return plugin.getDescription().getAuthors().toString();
        }
        
        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }
        
        @Override
        public boolean persist() {
            return true; // This is required or else PlaceholderAPI will unregister the expansion on reload
        }
        
        @Override
        public String onRequest(OfflinePlayer player, String params) {
            if (params.equalsIgnoreCase("quote")) {
                // 由于部分插件可能不支持换行符，我们返回每行间用空格分隔的完整一言
                return plugin.getCurrentHitokoto();
            }

            // 处理分行一言，格式为 line_X 其中X为行号（从0开始）
            if (params.startsWith("line_")) {
                try {
                    int lineNumber = Integer.parseInt(params.substring(5));
                    String lineContent = plugin.getWrappedHitokotoLine(lineNumber);
                    // 记录日志以便调试
                    if (plugin.isDebugEnabled()) {
                        plugin.getLogger().info("Placeholder request: %hitokoto_" + params + "% = " + lineContent);
                    }
                    
                    // 关键修改：确保返回空字符串而不是null
                    return lineContent != null ? lineContent : "";
                } catch (NumberFormatException e) {
                    return ""; // 返回空字符串而不是null
                }
            }
            
            // 返回总行数
            if (params.equalsIgnoreCase("lines")) {
                return String.valueOf(plugin.getWrappedHitokoto().size());
            }
            
            return ""; // 对于未知的占位符，返回空字符串而不是null
        }
    }
}