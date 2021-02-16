package jp.pro.daylight.minecraftplugin;

import jdk.internal.jline.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class MinecraftPlugin extends JavaPlugin {

    WerewolfManager wm;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("プラグインが有効化されました。");
        wm = new WerewolfManager(this);
        new WerewolfListener(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("プラグラインが無効化されました。");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if(cmd.getName().equalsIgnoreCase("ww")){
            if(args.length == 0){
                sender.sendMessage("§e[DaylightPlugin]§f §4コマンド引数が足りません。§f");
                return false;
            }
            if(args[0].equalsIgnoreCase("join")){
                Player pl = (Player) sender;
                String ret = wm.join(pl);
                if(ret != null){
                    sender.sendMessage("§e[DaylightPlugin]§f "+ret);
                }
                return true;
            }else if(args[0].equalsIgnoreCase("start")){
                if(args.length != 2){
                    sender.sendMessage("§e[DaylightPlugin]§f §4コマンド引数が不正です。§f");
                    return false;
                }
                String ret = wm.start(args[1]);
                if(ret != null){
                    sender.sendMessage("§e[DaylightPlugin]§f "+ret);
                }
                return true;
            }else if(args[0].equalsIgnoreCase("reset")){
                wm.reset();
                sender.sendMessage("§e[DaylightPlugin]§f §aゲームをリセットしました。§f");
                return true;
            }else if(args[0].equalsIgnoreCase("admin")){
                wm.openAdminInv((Player)sender);
            }else if(args[0].equalsIgnoreCase("all")){
                List<Player> ps = (List<Player>) Bukkit.getOnlinePlayers();
                for(int i = 0; i < ps.size();i++){
                    Bukkit.dispatchCommand(ps.get(i),"ww join");
                }
            }
        }
        return false;
    }
}

/**
 * 非同期タイマークラス
 * @author ecolight
 */
class MyTimer extends BukkitRunnable {
    private final MinecraftPlugin plg;
    private int count;
    public BossBar boss;

    /**
     * コンストラクタ
     * @param plg_ プラグインメインクラスのインスタンス
     * @param count_ 表示する値
     */
    public MyTimer(MinecraftPlugin plg_, int count_)
    {
        this(plg_,count_,false,null);
        boss = null;
    }
    public MyTimer(MinecraftPlugin plg_, int count_,boolean isBoss,@Nullable BossBar boss_){
        plg = plg_;
        count = count_;
        if(isBoss){
            if(boss_ != null){
                boss = boss_;
            }else {
                if (plg.wm.turn == Turn.Night) {
                    boss = Bukkit.createBossBar(String.format("夜時間終了まで%02d:%02d", count / 60, count % 60), BarColor.PURPLE, BarStyle.SOLID);
                } else if(plg.wm.turn == Turn.Noon){
                    boss = Bukkit.createBossBar(String.format("投票終了まで%02d:%02d", count / 60, count % 60), BarColor.YELLOW, BarStyle.SOLID);
                }
                boss.setProgress(1.0);
                for (int i = 0; i < plg.wm.players.size(); i++) {
                    boss.addPlayer(plg.wm.players.get(i).pl);
                }
            }
        }
    }

    /**
     * 非同期処理実行メソッド
     */
    public void run()
    {
        // countを 1 減算し、0 以上であれば次のタイマーを起動する
        count--;

        if (count >= 0) {
            if(plg.wm.turn == Turn.Night){
                if(boss != null) {
                    boss.setProgress((double) count / 15.0);
                    boss.setTitle(String.format("夜時間終了まで%02d:%02d", count / 60, count % 60));
                    if(count <= 2){
                        for (int i = 0; i < plg.wm.players.size(); i++) {
                            plg.wm.players.get(i).pl.playSound(plg.wm.players.get(i).pl.getLocation(),Sound.ENTITY_ARROW_HIT_PLAYER,1,1);
                        }
                    }
                }
            }
            if(plg.wm.turn == Turn.Noon){
                if(boss != null) {
                    boss.setProgress((double) count / 180.0);
                    boss.setTitle(String.format("投票終了まで%02d:%02d", count / 60, count % 60));
                    if(count <= 4){
                        for (int i = 0; i < plg.wm.players.size(); i++) {
                            plg.wm.players.get(i).pl.playSound(plg.wm.players.get(i).pl.getLocation(),Sound.ENTITY_ARROW_HIT_PLAYER,1,1);
                        }
                    }
                }
            }
            new MyTimer(plg, count, boss != null,boss).runTaskLater(plg, 20);
        }else{
            if(boss != null){
                boss.removeAll();
                boss = null;
            }
            plg.wm.next();
        }
    }
}
