package jp.pro.daylight.minecraftplugin;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;


//イベントを処理するクラス.
public class WerewolfListener implements Listener {
    MinecraftPlugin plg;

    public WerewolfListener(MinecraftPlugin _plg){
        plg = _plg;
        //このクラスをプラグインに登録
        plg.getServer().getPluginManager().registerEvents(this,plg);
    }


    @EventHandler //看板ブロックを右クリック
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clickedBlock = event.getClickedBlock();
        Material material = clickedBlock.getType();
        if (material == Material.OAK_SIGN || material == Material.OAK_WALL_SIGN) {
            Sign sign = (Sign) clickedBlock.getState();

            String[] lines = sign.getLines();//全行取得
            String line = sign.getLine(0);//引数は[0-3]
            if(line.equals("[WW]join")){
                Bukkit.dispatchCommand(event.getPlayer(),"ww join");
            }
        }
    }
    
    @EventHandler
    public void inventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().startsWith("[WW]")) return;
        Player pl = (Player) e.getPlayer();
        for (int i = 0; i < plg.wm.players.size(); i++) {
            WerewolfPlayer wpl = plg.wm.players.get(i);
            if (wpl.pl.equals(pl)) {
                if (!wpl.moved) {
                    if (plg.wm.turn == Turn.Night) {

                        new ReopenInventoryTask(plg, wpl).runTaskLater(plg, 1);
                    } else if (plg.wm.turn == Turn.Noon) {
                        Inventory inv = pl.getInventory();
                        ItemStack item = new ItemStack(Material.PAPER);
                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName("投票用紙");
                        item.setItemMeta(meta);
                        if (!inv.contains(item)) {
                            inv.addItem(item);
                        }
                    }
                }
            }
        }
    }


    @EventHandler
    public void inventoryClick(InventoryClickEvent e){
        Player pl = (Player)e.getWhoClicked();
        if(e.getCurrentItem() == null)return;
        for(int i = 0; i < plg.wm.players.size();i++){
            WerewolfPlayer wpl = plg.wm.players.get(i);
            if(!wpl.pl.equals(pl))continue;
            if(plg.wm.turn == Turn.Night) {
                if (!wpl.moved) {
                    if (e.getCurrentItem().getItemMeta().getDisplayName().equals("§a完了§f")) {
                        e.setCancelled(true);
                        wpl.moved = true;
                        pl.playSound(pl.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                        new CloseInventoryTask(plg, pl).runTaskLater(plg, 1);
                    } else if (e.getCurrentItem().getItemMeta().getDisplayName().equals("残った2役を占う")) {
                        e.setCancelled(true);
                        wpl.moved = true;
                        plg.wm.getFortune(wpl, null);
                        pl.playSound(pl.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                        new CloseInventoryTask(plg, pl).runTaskLater(plg, 1);
                    } else if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                        e.setCancelled(true);
                        if (wpl.role == Role.Fortune_teller)
                            plg.wm.getFortune(wpl, e.getCurrentItem().getItemMeta().getDisplayName());
                        else if (wpl.role == Role.Thief)
                            plg.wm.changeRole(wpl, e.getCurrentItem().getItemMeta().getDisplayName());
                        pl.playSound(pl.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                        new CloseInventoryTask(plg, pl).runTaskLater(plg, 1);
                    } else if (e.getCurrentItem().getType() == Material.BARRIER) {
                        e.setCancelled(true);
                        plg.wm.changeRole(wpl, null);
                        pl.playSound(pl.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                        new CloseInventoryTask(plg, pl).runTaskLater(plg, 1);
                    }
                }
            }else if(plg.wm.turn == Turn.Noon && !wpl.moved){
                if (e.getCurrentItem().getType() == Material.PLAYER_HEAD){
                    e.setCancelled(true);
                    plg.wm.vote(wpl,e.getCurrentItem().getItemMeta().getDisplayName());
                    pl.playSound(pl.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
                    new CloseInventoryTask(plg, pl).runTaskLater(plg, 1);
                }
            }
        }
    }

    @EventHandler
    public void playerDropItem(PlayerDropItemEvent e){
        Player pl = e.getPlayer();
        for(int i = 0; i < plg.wm.players.size();i++) {
            WerewolfPlayer wpl = plg.wm.players.get(i);
            if (!wpl.pl.equals(pl)) continue;
            if(plg.wm.turn == Turn.Noon) {
                if(e.getItemDrop().getItemStack().getItemMeta().getDisplayName().equals("投票用紙")&& e.getItemDrop().getItemStack().getType() == Material.PAPER){
                    plg.wm.openVoteInv(wpl);
                    e.getItemDrop().remove();
                }
            }
        }
    }
}

//インベントリを閉じるのは1Tick遅らせてからじゃないとダメ
class CloseInventoryTask extends BukkitRunnable {

    private final MinecraftPlugin plugin;
    private Player pl;
    public CloseInventoryTask(MinecraftPlugin plugin,Player _pl) {
        this.plugin = plugin;
        this.pl = _pl;
    }

    @Override
    public void run() {
        this.pl.closeInventory();
    }
}

class ReopenInventoryTask extends BukkitRunnable{
    private final MinecraftPlugin plg;
    private WerewolfPlayer wpl;
    public ReopenInventoryTask(MinecraftPlugin plg, WerewolfPlayer wpl){
        this.plg = plg;
        this.wpl = wpl;
    }

    @Override
    public void run() {
        if(plg.wm.turn == Turn.Night) {
            plg.wm.openNightInv(wpl);
        }
    }
}
