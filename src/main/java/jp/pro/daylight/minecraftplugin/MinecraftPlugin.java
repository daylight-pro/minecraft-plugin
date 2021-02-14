package jp.pro.daylight.minecraftplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("プラグインが有効化されました。");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("プラグラインが無効化されました。");
    }
}
