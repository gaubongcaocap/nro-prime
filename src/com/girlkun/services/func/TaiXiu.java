package com.girlkun.services.func;

import com.girlkun.database.GirlkunDB;
import com.girlkun.models.item.Item;
import com.girlkun.models.player.Player;
import com.girlkun.network.io.Message;
import com.girlkun.server.Client;
import com.girlkun.services.ChatGlobalService;
import com.girlkun.services.InventoryServiceNew;
import com.girlkun.services.ItemService;
import com.girlkun.services.Service;
import com.girlkun.utils.Util;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaiXiu implements Runnable {

    public int goldTai;
    public int goldXiu;
    public boolean ketquaTai = false;
    public boolean ketquaXiu = false;

    public boolean baotri = false;
    public long lastTimeEnd;
    public List<Player> PlayersTai = new ArrayList<>();
    public List<Player> PlayersXiu = new ArrayList<>();
    public List<Integer> tongHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 15;
    public List<String> tongHistoryString = new ArrayList<>();
    private static TaiXiu instance;
    public int x, y, z;
    public int win;

    public static TaiXiu gI() {
        if (instance == null) {
            instance = new TaiXiu();
        }
        return instance;
    }

    public void addPlayerXiu(Player pl) {
        if (!PlayersXiu.equals(pl)) {
            PlayersXiu.add(pl);
        }
    }

    public void addPlayerTai(Player pl) {
        if (!PlayersTai.equals(pl)) {
            PlayersTai.add(pl);
        }
    }

    public void removePlayerXiu(Player pl) {
        if (PlayersXiu.equals(pl)) {
            PlayersXiu.remove(pl);
        }
    }

    public void removePlayerTai(Player pl) {
        if (PlayersTai.equals(pl)) {
            PlayersTai.remove(pl);
        }
    }

    private String echotaixiu(int tong) {
        return (tong > 10) ? "Tài" : "Xỉu";
    }

    private void addvaolist(int tong) {
        tongHistory.add(tong);

    }

    private void trim() {
        while (tongHistory.size() > MAX_HISTORY_SIZE) {
            tongHistory.clear();
        }
    }

    private void chuyenthanhstring() {
        tongHistoryString.clear();
        for (int i = 0; i < tongHistory.size(); i++) {
            int tong = tongHistory.get(i);
            String tongStr = echotaixiu(tong);
            tongHistoryString.add(tongStr);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (((TaiXiu.gI().lastTimeEnd - System.currentTimeMillis()) / 1000) <= 0) {
                    int x, y, z;
                    if (TaiXiu.gI().goldTai >= TaiXiu.gI().goldXiu) {
                        if (Util.isTrue(60, 100)) {
                            x = Util.nextInt(1, 2);
                            y = Util.nextInt(1, 3);
                            z = Util.nextInt(1, 3);
                            TaiXiu.gI().x = x;
                            TaiXiu.gI().y = y;
                            TaiXiu.gI().z = z;
                        } else {
                            x = Util.nextInt(3, 6);
                            y = Util.nextInt(4, 6);
                            z = Util.nextInt(4, 6);
                            TaiXiu.gI().x = x;
                            TaiXiu.gI().y = y;
                            TaiXiu.gI().z = z;
                        }
                    } else {
                        if (Util.isTrue(60, 100)) {
                            x = Util.nextInt(3, 6);
                            y = Util.nextInt(4, 6);
                            z = Util.nextInt(4, 6);
                            TaiXiu.gI().x = x;
                            TaiXiu.gI().y = y;
                            TaiXiu.gI().z = z;
                        } else {
                            x = Util.nextInt(1, 2);
                            y = Util.nextInt(1, 3);
                            z = Util.nextInt(1, 3);
                            TaiXiu.gI().x = x;
                            TaiXiu.gI().y = y;
                            TaiXiu.gI().z = z;
                        }
                    }
                    int tong = (x + y + z);
                    addvaolist(tong);
                    if (tong < 11) {
                        ketquaXiu = true;
                        ketquaTai = false;
                    }
                    if (tong > 10) {
                        ketquaXiu = false;
                        ketquaTai = true;
                    }
                    if (ketquaTai == true) {
                        if (!TaiXiu.gI().PlayersTai.isEmpty()) {
                            for (int i = 0; i < PlayersTai.size(); i++) {
                                Player pl = this.PlayersTai.get(i);
                                if (pl != null ) {
                                  
                                    Item tvthang = ItemService.gI().createNewItem((short) 457);
                                    tvthang.quantity = (int) Math.round(pl.goldTai * 1.8);
                                    int sovthang = tvthang.quantity;
                                    InventoryServiceNew.gI().addItemBag(pl, tvthang);
                                    InventoryServiceNew.gI().sendItemBags(pl);
                                    Service.getInstance().sendThongBao(pl, "Số hệ thống quay ra\n" + x + " : "
                                            + y + " : " + z + "\n|5|Tổng là : " + tong + "\n(TÀI)\n\n|1|Bạn đã chiến thắng!!");
                                    Service.getInstance().sendThongBao(pl, "Chúc mừng bạn đã dành chiến thắng và nhận được " + Util.format(sovthang) + " Thỏi Vàng");
                                    pl.taixiu.win += pl.goldTai * 80 / 100;
                                    Message m;
                                    try {
                                        m = new Message(-126);
                                        m.writer().writeByte(1);
                                        m.writer().writeByte(1);
                                        m.writer().writeUTF("00" + TaiXiu.gI().x + TaiXiu.gI().y + TaiXiu.gI().z);
                                        m.writer().writeUTF("99999");
                                        pl.sendMessage(m);
                                        m.cleanup();
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                        for (int i = 0; i < PlayersXiu.size(); i++) {
                            Player pl = this.PlayersXiu.get(i);
                            if (pl != null ) {
                                Service.getInstance().sendThongBao(pl, "Số hệ thống quay ra\n" + x + " : "
                                        + y + " : " + z + "\n|5|Tổng là : " + tong + "\n(TÀI)\n\n|7|Trắng tay gòi, chơi lại đi!!!");

                                Message m;
                                try {
                                    m = new Message(-126);
                                    m.writer().writeByte(1);
                                    m.writer().writeByte(1);
                                    m.writer().writeUTF("00" + TaiXiu.gI().x + TaiXiu.gI().y + TaiXiu.gI().z);
                                    m.writer().writeUTF("99999");
                                    pl.sendMessage(m);
                                    m.cleanup();
                                } catch (Exception e) {
                                }
                            }
                        }
                    } else if (ketquaXiu == true) {
                        if (!TaiXiu.gI().PlayersXiu.isEmpty()) {
                            for (int i = 0; i < PlayersXiu.size(); i++) {
                                Player pl = this.PlayersXiu.get(i);
                                if (pl != null ) {

                                    Item tvthang = ItemService.gI().createNewItem((short) 457);
                                    tvthang.quantity = (int) Math.round(pl.goldXiu * 1.8);
                                    int sovthang = tvthang.quantity;
                                    InventoryServiceNew.gI().addItemBag(pl, tvthang);
                                    InventoryServiceNew.gI().sendItemBags(pl);
                                    Service.getInstance().sendThongBao(pl, "Số hệ thống quay ra\n" + x + " : "
                                            + y + " : " + z + "\n|5|Tổng là : " + tong + "\n(XỈU)\n\n|1|Bạn đã chiến thắng!!");
                                    Service.getInstance().sendThongBao(pl, "Chúc mừng bạn đã dành chiến thắng và nhận được " + Util.format(sovthang) + " Thỏi vàng");
                                    pl.taixiu.win += pl.goldXiu * 80 / 100;
                                    Message m;
                                    try {
                                        m = new Message(-126);
                                        m.writer().writeByte(1);
                                        m.writer().writeByte(1);
                                        m.writer().writeUTF("00" + TaiXiu.gI().x + TaiXiu.gI().y + TaiXiu.gI().z);
                                        m.writer().writeUTF("99999");
                                        pl.sendMessage(m);
                                        m.cleanup();
                                    } catch (Exception e) {
                                    }

                                }
                            }
                        }
                        for (int i = 0; i < PlayersTai.size(); i++) {
                            Player pl = this.PlayersTai.get(i);
                            if (pl != null ) {

                                Service.getInstance().sendThongBao(pl, "Số hệ thống quay ra\n" + x + " : "
                                        + y + " : " + z + "\n|5|Tổng là : " + tong + "\n(XỈU)\n\n|7|Trắng tay gòi, chơi lại đi!!!");
                                Message m;
                                try {
                                    m = new Message(-126);
                                    m.writer().writeByte(1);
                                    m.writer().writeByte(1);
                                    m.writer().writeUTF("00" + TaiXiu.gI().x + TaiXiu.gI().y + TaiXiu.gI().z);
                                    m.writer().writeUTF("99999");
                                    pl.sendMessage(m);
                                    m.cleanup();
                                } catch (Exception e) {
                                }
                            }

                        }
                    }
                    for (int i = 0; i < TaiXiu.gI().PlayersTai.size(); i++) {
                        Player pl = TaiXiu.gI().PlayersTai.get(i);
                        if (pl != null) {
                            pl.goldTai = 0;
                        }
                    }
                    for (int i = 0; i < TaiXiu.gI().PlayersXiu.size(); i++) {
                        Player pl = TaiXiu.gI().PlayersXiu.get(i);
                        if (pl != null) {
                            pl.goldXiu = 0;
                        }
                    }
                    ketquaXiu = false;
                    ketquaTai = false;
                    TaiXiu.gI().goldTai = 0;
                    TaiXiu.gI().goldXiu = 0;
                    TaiXiu.gI().PlayersTai.clear();
                    TaiXiu.gI().PlayersXiu.clear();
                    trim();
                    chuyenthanhstring();
                    TaiXiu.gI().lastTimeEnd = System.currentTimeMillis() + 60000;
                }
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
    }

}
