package jp.pro.daylight.minecraftplugin;

import jdk.internal.jline.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class WerewolfManager{
    List<WerewolfPlayer> players;
    MinecraftPlugin plg;
    Turn turn;
    HashMap<Role,String> roleName;
    Role role1,role2;
    WerewolfPlayer thiefTarget;
    public WerewolfManager(MinecraftPlugin _plg){
        plg = _plg;
        players = new ArrayList<>();
        turn = Turn.Not_started;
        roleName = new HashMap<Role,String>();
        roleName.put(Role.Not_assinged,"§7未割当§f");
        roleName.put(Role.Villager,"§a村人§f");
        roleName.put(Role.Werewolf,"§4人狼§f");
        roleName.put(Role.Fortune_teller,"§5占い師§f");
        roleName.put(Role.Thief,"§e怪盗§f");
        role1 = Role.Not_assinged;
        role2 = Role.Not_assinged;
        thiefTarget = null;
    }

    //ゲーム参加のコマンド.メッセージを返す.
    public String join(Player pl){
        if(turn != Turn.Not_started)return "§4参加募集中ではありません。§f";
        for(WerewolfPlayer p : players){
            if(pl.equals(p.pl)){
                //return "§4すでに参加しています。§f";
                return null;
            }
        }
        players.add(new WerewolfPlayer(pl));
        return "§a参加しました。§f";
    }

    //ゲーム開始のコマンド.メッセージを返す.
    public String start(String roles){
        if(roles.length() != players.size()+2){
            return "§4役職数がプレイヤー数と会いません。§f";
        }
        List<Role> R = new ArrayList<>();
        for(int i = 0;i < roles.length();i++){
            switch(roles.charAt(i)){
                case 'V':
                    R.add(Role.Villager);
                    break;
                case 'W':
                    R.add(Role.Werewolf);
                    break;
                case 'F':
                    R.add(Role.Fortune_teller);
                    break;
                case 'T':
                    R.add(Role.Thief);
                    break;
                default:
                    return "§4役職名に不正があります。§f";
            }
        }
        Collections.shuffle(R);
        for(int i = 0; i < players.size();i++){
            players.get(i).role = R.get(i);
            players.get(i).pl.sendTitle(String.format("あなたの役職:「%s」",roleName.get(players.get(i).role)),null,10,120,10);
            players.get(i).pl.sendMessage(String.format("§e[DaylightPlugin]§f あなたの役職は「%s」です。",roleName.get(players.get(i).role)));
        }
        role1 = R.get(players.size());
        role2 = R.get(players.size()+1);
        new MyTimer(plg,10).runTaskLater(plg, 20);
        turn = Turn.Role_check;
        return "§a役職を配布しました。§f";
    }

    public void getFortune(WerewolfPlayer wpl, @Nullable String target){
        if(wpl.role != Role.Fortune_teller)return;
        if(wpl.pl.getDisplayName().equals(target))return;
        if(target == null){
            wpl.pl.sendTitle(String.format("%s＆%s",roleName.get(role1),roleName.get(role2)),null,10,70,10);
            wpl.pl.sendMessage(String.format("§e[DaylightPlugin]§f 残った2役は「%s」と「%s」です。",roleName.get(role1),roleName.get(role2)));
            wpl.moved = true;
        }else{
            for(int i = 0; i < players.size();i++){
                if(target.equals(players.get(i).pl.getDisplayName())){
                    wpl.pl.sendTitle(roleName.get(players.get(i).role),null,10,70,10);
                    wpl.pl.sendMessage(String.format("§e[DaylightPlugin]§f %sさんの役職は「%s」です。",players.get(i).pl.getDisplayName(),roleName.get(players.get(i).role)));
                    wpl.moved = true;
                }
            }
        }
    }

    public void changeRole(WerewolfPlayer wpl, @Nullable String target){
        if(wpl.role != Role.Thief)return;
        if(wpl.pl.getDisplayName().equals(target))return;
        if(target == null){
            wpl.pl.sendMessage(String.format("§e[DaylightPlugin]§f 役職を盗みませんでした。"));
            wpl.moved = true;
        }else{
            for(int i = 0; i < players.size();i++){
                if(target.equals(players.get(i).pl.getDisplayName())){
                    wpl.pl.sendTitle(roleName.get(players.get(i).role),null,10,70,10);
                    wpl.pl.sendMessage(String.format("§e[DaylightPlugin]§f %sさんから「%s」を盗みました。",players.get(i).pl.getDisplayName(),roleName.get(players.get(i).role)));
                    wpl.moved = true;
                    thiefTarget = players.get(i);
                }
            }
        }
    }

    public void vote(WerewolfPlayer wpl, String target){
        if(wpl.pl.getDisplayName().equals(target))return;
        for(int i = 0; i < players.size();i++){
            if(target.equals(players.get(i).pl.getDisplayName())){
                players.get(i).vote++;
                wpl.moved = true;
            }
        }
        boolean flag = true;
        for(int i = 0;i < players.size();i++){
            if(!players.get(i).moved)flag = false;
        }
        if(flag){
            getResult();
        }
    }

    public void getResult(){
        List<WerewolfPlayer> wps = new ArrayList<>();
        int max_vote = 0;
        String dead = "";
        String result = "";
        for(int i = 0;i < players.size();i++){
            max_vote = Math.max(max_vote,players.get(i).vote);
        }
        if(max_vote == 1){
            dead = "誰も処刑されませんでした。";
            for(int i = 0;i < players.size();i++){
                if(players.get(i).role == Role.Werewolf){
                    wps.add(players.get(i));
                }
                if(wps.size() != 0){
                    result = "§4人狼陣営§fの勝利";
                }else{
                    result = "§a村人陣営§fの勝利";
                }
            }
        }else{
            boolean winWerewolf = true;
            for(int i = 0; i < players.size();i++){
                if(players.get(i).vote == max_vote) {
                    dead += "「" + players.get(i).pl.getDisplayName() + "」";
                    if(players.get(i).role == Role.Werewolf)winWerewolf = false;
                }
            }
            dead += "が処刑されました。";
            if(winWerewolf){
                result = "§4人狼陣営§fの勝利";
            }else{
                result = "§a村人陣営§fの勝利";
            }
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < players.size();i++){
            if(i != 0) sb.append("\n");
            sb.append("「"+players.get(i).pl.getDisplayName()+"」:").append(roleName.get(players.get(i).role));
            if(thiefTarget != null && players.get(i).pl.equals(thiefTarget.pl)){
                sb.append("(変更前:"+roleName.get(Role.Thief)+")");
            }
            if(players.get(i).role == Role.Thief){
                sb.append("(変更前:"+roleName.get(thiefTarget.role)+")");
            }
        }
        String roles = sb.toString();
        for(int i = 0;i < players.size();i++){
            players.get(i).pl.sendMessage("§e[DaylightPlugin]§f " + dead+result+"です。");
            players.get(i).pl.sendTitle(result,null,10,150,10);
            players.get(i).pl.sendMessage(roles);
        }
        plg.getServer().getWorld("world").playSound(new Location(plg.getServer().getWorld("world"),0,0,0), Sound.UI_TOAST_CHALLENGE_COMPLETE,100000,1);
    }

    public Inventory createVillagerInv(Player pl){
        Inventory ret = Bukkit.createInventory(pl,27,"[WW]完了ボタンをクリックしてください。");
        ItemStack okButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = okButton.getItemMeta();
        meta.setDisplayName("§a完了§f");
        okButton.setItemMeta(meta);
        ret.setItem(13,okButton);
        return ret;
    }

    public Inventory createWerewolfInv(Player pl){
        Inventory ret = Bukkit.createInventory(pl,27,"[WW]仲間の人狼を確認してください。");
        ItemStack okButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = okButton.getItemMeta();
        meta.setDisplayName("§a完了§f");
        okButton.setItemMeta(meta);
        ret.setItem(26,okButton);
        int invind = 0;
        for(int i = 0;i < players.size();i++){
            if(players.get(i).role == Role.Werewolf){
                ItemStack wolf = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta metaWolf = (SkullMeta)wolf.getItemMeta();
                metaWolf.setOwningPlayer(players.get(i).pl);
                metaWolf.setDisplayName(players.get(i).pl.getDisplayName());
                wolf.setItemMeta(metaWolf);
                ret.setItem(invind,wolf);
                invind++;
            }
        }
        return ret;
    }

    public Inventory createFortuneTellerInv(Player pl){
        Inventory ret = Bukkit.createInventory(pl,27,"[WW]占いたい人をクリックしてください。");
        ItemStack restRole = new ItemStack(Material.ZOMBIE_HEAD);
        ItemMeta meta = restRole.getItemMeta();
        meta.setDisplayName("残った2役を占う");
        restRole.setItemMeta(meta);
        ret.setItem(26,restRole);
        int invind = 0;
        for (WerewolfPlayer player : players) {
            if (!player.pl.equals(pl)) {
                ItemStack p = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta metap = (SkullMeta) p.getItemMeta();
                metap.setOwningPlayer(player.pl);
                metap.setDisplayName(player.pl.getDisplayName());
                p.setItemMeta(metap);
                ret.setItem(invind, p);
                invind++;
            }
        }
        return ret;
    }

    public Inventory createThiefInv(Player pl){
        Inventory ret = Bukkit.createInventory(pl,27,"[WW]盗みたい人をクリックしてください。");
        ItemStack notChange = new ItemStack(Material.BARRIER);
        ItemMeta meta = notChange.getItemMeta();
        meta.setDisplayName("盗まない");
        notChange.setItemMeta(meta);
        ret.setItem(26,notChange);
        int invind = 0;
        for (WerewolfPlayer player : players) {
            if (!player.pl.equals(pl)) {
                ItemStack p = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta metap = (SkullMeta) p.getItemMeta();
                metap.setOwningPlayer(player.pl);
                metap.setDisplayName(player.pl.getDisplayName());
                p.setItemMeta(metap);
                ret.setItem(invind, p);
                invind++;
            } else {
                plg.getLogger().info(player.role.getClass().toString());
            }
        }
        return ret;
    }

    public void openNightInv(WerewolfPlayer wpl){
        plg.getLogger().info(wpl.pl.getDisplayName());
        switch(wpl.role){
            case Villager:
                wpl.pl.openInventory(createVillagerInv(wpl.pl));
                break;
            case Werewolf:
                wpl.pl.openInventory(createWerewolfInv(wpl.pl));
                break;
            case Fortune_teller:
                wpl.pl.openInventory(createFortuneTellerInv(wpl.pl));
                break;
            case Thief:
                wpl.pl.openInventory(createThiefInv(wpl.pl));
                break;
            default:
                break;
        }
    }

    public void openVoteInv(WerewolfPlayer wpl){
        Player pl = wpl.pl;
        Inventory ret = Bukkit.createInventory(pl,27,"[WW]投票したい人をクリックしてください。");
        int invind = 0;
        for (WerewolfPlayer player : players) {
            if (!player.pl.equals(pl)) {
                ItemStack wolf = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta metaWolf = (SkullMeta) wolf.getItemMeta();
                metaWolf.setOwningPlayer(player.pl);
                metaWolf.setDisplayName(player.pl.getDisplayName());
                wolf.setItemMeta(metaWolf);
                ret.setItem(invind, wolf);
                invind++;
            }
        }
        wpl.pl.openInventory(ret);
    }

    public void openAdminInv(Player pl){
        Inventory ret = Bukkit.createInventory(pl,27,"[WWAdmin]Admin画面");
        for(int i = 0;i < players.size();i++){
            ItemStack p = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta metap = (SkullMeta)p.getItemMeta();
            metap.setOwningPlayer(players.get(i).pl);
            metap.setDisplayName(players.get(i).pl.getDisplayName());
            List<String> lore = new ArrayList<String>();
            lore.add(roleName.get(players.get(i).role));
            metap.setLore(lore);
            p.setItemMeta(metap);
            ret.setItem(i,p);
        }
        pl.openInventory(ret);
    }
    public void startNight(){
        turn = Turn.Night;
        for (WerewolfPlayer player : players) {
            openNightInv(player);
        }
        new MyTimer(plg,15,true).runTask(plg);
    }

    public void startNoon(){
        turn = Turn.Noon;
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("投票用紙");
        item.setItemMeta(meta);
        String targetName = thiefTarget==null?"":thiefTarget.pl.getDisplayName();
        Role r = thiefTarget==null?Role.Not_assinged:thiefTarget.role;
        for (WerewolfPlayer player : players) {
            player.pl.getInventory().addItem(item);
            player.moved = false;
            if (thiefTarget != null) {
                if (player.role == Role.Thief) {
                    player.role = r;
                    thiefTarget = player;
                } else if (player.pl.getDisplayName().equals(targetName)) {
                    player.role = Role.Thief;
                }
            }
        }
        new MyTimer(plg,180,true).runTask(plg);
    }

    public void next() {
        if (turn == Turn.Role_check) {
            startNight();
        }else if(turn == Turn.Night){
            startNoon();
        }
    }

    public void reset(){
        role1 = Role.Not_assinged;
        role2 = Role.Not_assinged;
        turn = Turn.Not_started;
        players.clear();
        thiefTarget = null;
        Iterator<KeyedBossBar> it  =Bukkit.getBossBars();
        while(it.hasNext()){
            KeyedBossBar bar = it.next();
            plg.getLogger().info(bar.getTitle());
            bar.removeAll();
        }
        if(plg.boss != null){
            plg.boss.removeAll();
        }
    }
}

class WerewolfPlayer {
    Role role;
    Player pl;
    boolean moved;
    int vote;
    WerewolfPlayer(Player _pl){
        pl = _pl;
        role = Role.Not_assinged;
        moved = false;
        vote = 0;
    }
}

enum Turn{
    Not_started,
    Role_check,
    Night,
    Noon,
    Result,
}

enum Role{
    Not_assinged,
    Villager,
    Werewolf,
    Fortune_teller,
    Thief,
}

