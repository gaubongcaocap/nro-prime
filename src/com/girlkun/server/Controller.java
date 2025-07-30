package com.girlkun.server;

import GiaiSieuHang.SieuHangService;
import com.girlkun.database.GirlkunDB;
import com.girlkun.models.item.Item;
import com.girlkun.result.GirlkunResultSet;
import com.girlkun.consts.ConstIgnoreName;
import com.girlkun.consts.ConstMap;
import com.girlkun.services.*;
import com.girlkun.utils.Util;
import com.girlkun.data.DataGame;
import com.girlkun.server.io.MySession;

import java.io.IOException;

import com.girlkun.models.card.Card;
import com.girlkun.models.card.RadarCard;
import com.girlkun.models.card.RadarService;

import com.girlkun.services.func.ChangeMapService;
import com.girlkun.services.func.UseItem;
import com.girlkun.models.kygui.ShopKyGuiService;
import com.girlkun.services.func.Input;
import com.girlkun.consts.ConstNpc;
import com.girlkun.consts.ConstTask;
import static com.girlkun.data.DataGame.sendEffectTemplate;
import com.girlkun.data.ItemData;
import com.girlkun.jdbc.daos.PlayerDAO;
import com.girlkun.models.boss.Boss;
import com.girlkun.models.boss.BossManager;
import com.girlkun.models.map.Zone;
import com.girlkun.models.map.blackball.BlackBallWar;
import com.girlkun.models.npc.NpcManager;
import com.girlkun.models.player.Player;
import com.girlkun.models.matches.PVPService;
import static com.girlkun.models.npc.NpcFactory.PLAYERID_OBJECT;
import com.girlkun.models.shop.ShopServiceNew;
import com.girlkun.models.skill.PlayerSkill;
import com.girlkun.network.handler.IMessageHandler;
import com.girlkun.network.io.Message;
import com.girlkun.network.session.ISession;
import com.girlkun.services.func.CombineServiceNew;
import com.girlkun.services.func.GoiRongBang;
import com.girlkun.services.func.GoiRongXuong;

import static com.girlkun.services.func.Input.CHOOSE_LEVEL_BDKB;
import static com.girlkun.services.func.Input.NUMERIC;

import com.girlkun.services.func.LuckyRound;
import com.girlkun.services.func.RadaService;
import com.girlkun.services.func.SummonDragon;
import com.girlkun.services.func.TransactionService;
import com.girlkun.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller implements IMessageHandler {

    private static Controller instance;

    public static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    @Override
    public void onMessage(ISession s, Message _msg) throws IOException {
        MySession _session = (MySession) s;
        long st = System.currentTimeMillis();
        Player player = null;
        try {
            player = _session.player;
            byte cmd = _msg.command;
            switch (cmd) {
                case -100:
                    byte action = _msg.reader().readByte();
                    switch (action) {
                        case 0:
                            short idItem = _msg.reader().readShort();
                            byte moneyType = _msg.reader().readByte();
                            int money = _msg.reader().readInt();
                            int quantity;
                            if (player.getSession().version >= 222) {
                                quantity = _msg.reader().readInt();
                            } else {
                                quantity = _msg.reader().readByte();
                            }
                            if (quantity > 0) {
                                ShopKyGuiService.gI().KiGui(player, idItem, money, moneyType, quantity);
                            }
                            break;
                        case 1:
                        case 2:
                            idItem = _msg.reader().readShort();
                            ShopKyGuiService.gI().claimOrDel(player, action, idItem);
                            break;
                        case 3:
                            idItem = _msg.reader().readShort();
                            _msg.reader().readByte();
                            _msg.reader().readInt();
                            ShopKyGuiService.gI().buyItem(player, idItem);
                            break;
                        case 4:
                            moneyType = _msg.reader().readByte();
                            money = _msg.reader().readByte();
                            ShopKyGuiService.gI().openShopKyGui(player, moneyType, money);
                            break;
                        case 5:
                            idItem = _msg.reader().readShort();
                            ShopKyGuiService.gI().upItemToTop(player, idItem);
                            break;
                        default:
                            Service.getInstance().sendThongBao(player, "Không thể thực hiện");
                            break;
                    }
                    break;
                case -105:
                    if (player != null) {
                        if (player.type == 0 && player.maxTime == 30) {
                            ChangeMapService.gI().changeMap(player, 102, 0, 100, 336);
                        } else if (player.type == 2 && player.maxTime == 5) {
                            ChangeMapService.gI().changeMap(player, 135, 0, -1, 5);
                        } else if (player.type == 1 && player.maxTime == 5) {
                            ChangeMapService.gI().changeMap(player, 160, 0, -1, 5);
                        } else if (player.type == 1 && player.maxTime == 6) {
                            ChangeMapService.gI().changeMap(player, 202, 0, -1, 5);
                        }
                    }
                    break;
                case 42:
                    Service.getInstance().regisAccount(_session, _msg);
                    break;
                case 127:
                    if (player != null) {
                        byte actionRadar = _msg.reader().readByte();
                        switch (actionRadar) {
                            case 0:
                                RadarService.gI().sendRadar(player, player.Cards);
                                break;
                            case 1:
                                short idC = _msg.reader().readShort();
                                Card card = player.Cards.stream().filter(r -> r != null && r.Id == idC).findFirst().orElse(null);
                                if (card != null) {
                                    if (card.Level == 0) {
                                        return;
                                    }
                                    if (card.Used == 0) {
                                        if (player.Cards.stream().anyMatch(c -> c != null && c.Used == 2)) {
                                            Service.gI().sendThongBao(player, "Số thẻ sử dụng đã đạt tối đa");
                                            return;
                                        }
                                        card.Used = 1;
                                        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream()
                                                .filter(r -> r.Id == idC).findAny().orElse(null);
                                        if (radarTemplate != null && card.Level >= 2) {
                                            player.idAura = radarTemplate.AuraId;
                                        }
                                    } else {
                                        card.Used = 0;
                                        player.idAura = -1;
                                    }
                                    RadarService.gI().Radar1(player, idC, card.Used);
                                    Service.gI().point(player);
                                }
                                break;
                        }
                    }
                    break;
                case -127:
                    if (player != null) {
                        LuckyRound.gI().readOpenBall(player, _msg);
                    }
                    break;
                case -125:
                    if (player != null) {
                        Input.gI().doInput(player, _msg);
                    }
                    break;
                case 112:
                    if (player != null) {
                        IntrinsicService.gI().showMenu(player);
                    }
                    break;
                case -34:
                    if (player != null) {
                        switch (_msg.reader().readByte()) {
                            case 1:
                                player.magicTree.openMenuTree();
                                break;
                            case 2:
                                player.magicTree.loadMagicTree();
                                break;
                        }
                    }
                    break;
                case -99:
                    if (player != null) {
                        FriendAndEnemyService.gI().controllerEnemy(player, _msg);
                    }
                    break;
                case 18:
                    if (player != null) {
                        FriendAndEnemyService.gI().goToPlayerWithYardrat(player, _msg);
                    }
                    break;
                case -72:
                    if (player != null) {
                        FriendAndEnemyService.gI().chatPrivate(player, _msg);
                    }
                    break;
                case -80:
                    if (player != null) {
                        FriendAndEnemyService.gI().controllerFriend(player, _msg);
                    }
                    break;
                case -59:
                    if (player != null) {
                        PVPService.gI().controllerThachDau(player, _msg);
                    }
                    break;
                case -86:
                    if (player != null) {
                        TransactionService.gI().controller(player, _msg);
                    }
                    break;
                case -107: //info pet
                    if (player != null) {
                        Service.getInstance().showInfoPet(player);
                    }
                    break;
                case -108: //info pet
                    if (player != null && player.pet != null) {
                        player.pet.changeStatus(_msg.reader().readByte());
                    }
                    break;
                case 6: //buy item
                    if (player != null && !Maintenance.isRuning) {
                        byte typeBuy = _msg.reader().readByte();
                        int tempId = _msg.reader().readShort();
                        int quantity = 1;
                        try {
                            quantity = _msg.reader().readShort();
                        } catch (Exception e) {
                        }
                        ShopServiceNew.gI().takeItem(player, typeBuy, tempId);
                    }
                    break;
                case 7: //sell item
                    if (player != null && !Maintenance.isRuning) {
                        action = _msg.reader().readByte();
                        if (action == 0) {
                            ShopServiceNew.gI().showConfirmSellItem(player, _msg.reader().readByte(),
                                    _msg.reader().readShort());
                        } else {
                            ShopServiceNew.gI().sellItem(player, _msg.reader().readByte(),
                                    _msg.reader().readShort());
                        }
                    }
                    break;
                case 29:
                    if (player != null) {
                        ChangeMapService.gI().openZoneUI(player);
                    }
                    break;
                case 21:
                    if (player != null) {
                        int zoneId = _msg.reader().readByte();
                        ChangeMapService.gI().changeZone(player, zoneId);
                    }
                    break;
                case -71:
                    if (player != null) {
                        ChatGlobalService.gI().chat(player, _msg.reader().readUTF());
                    }
                    break;
                case -79:
                    if (player != null) {
                        Service.getInstance().getPlayerMenu(player, _msg.reader().readInt());
                    }
                    break;
                case -76:
                    if (player != null) {
                        int index = _msg.reader().readByte();
                        player.achievement.receiveGem(index);
                    }
                    break;
                case -113:
                    if (player != null) {
                        for (int i = 0; i <10; i++) {
                            player.playerSkill.skillShortCut[i] = _msg.reader().readByte();
                        }
                        player.playerSkill.sendSkillShortCut();
                    }
                    break;
                   
                case -101:
                    login2(_session, _msg);
                    break;
                case -118:
                    int Id = _msg.reader().readInt();
                    if (Id != -1 && player.id != Id && player.zone.map.mapId == 113) {
                        SieuHangService.gI().startChallenge(player, Id);
                    }else
                    {
                        if (player != null) {
                        boolean processedBoss = false;
                        Item.ItemOption option = null;
                        for (Boss bosse : BossManager.gI().getBosses()) {
                            Item caitrang = player.inventory.itemsBody.get(5);
                                if (bosse != null && bosse.id == Id && !bosse.isDie()) {
                                    if (caitrang.isNotNullItem()) {
                                        for (Item.ItemOption io : caitrang.itemOptions) {
                                            option = io;
                                        }                    
                                        if (option.optionTemplate.id == 251) {
                                            if (option.param > 0) {
                                                option.param--;
                                                InventoryServiceNew.gI().sendItemBody(player);
                                                ChangeMapService.gI().changeMapInYard(player, bosse.zone, bosse.location.x);
                                            
                                            } else {
                                            Service.getInstance().sendThongBao(player, "|7|Bạn đã hết lượt dịch chuyển");
                                            }
                                        } 
                                    } else {
                                        Service.getInstance().sendThongBao(player, "|7|Bạn không có trang bị yardat");
                                    }
                                    if (player.isAdmin()) {
                                        if (player.haveBeQuynh == false) {
                                            ChangeMapService.gI().changeMapInYard(player, bosse.zone, bosse.location.x);
                                            //InventoryServiceNew.gI().sendItemBags(player);
                                        } else {
                                            Service.getInstance().sendThongBao(player, "|7|Không thể thực hiện khi đang Hộ tống");
                                        }
                                    }
                                    processedBoss = true;
                                    break;
                                }
                
                            }
                            if (!processedBoss) {
                                for (Player onlinePlayer : Client.gI().getPlayers()) {
                                    if (player.isAdmin() && onlinePlayer != null && onlinePlayer.id == Id) {
                                        int genderValue = onlinePlayer.gender;
                                        String planetName = Service.getInstance().get_HanhTinh(genderValue);
                                        Item sothoivang = InventoryServiceNew.gI().findItemBag(onlinePlayer, 457);
                                        int hongngoc = onlinePlayer.inventory.ruby;
                                        Item sothoivang1 = InventoryServiceNew.gI().findItemBox(onlinePlayer, 457);
                                        int quantityBag = (sothoivang != null) ? sothoivang.quantity : 0;
                                        int quantityBox = (sothoivang1 != null) ? sothoivang1.quantity : 0;
                                        int totalQuantity = quantityBag + quantityBox;
                                        String sothoivangStr = String.valueOf(totalQuantity);
                                        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_QUAN_LY_PLAYER, 12713,
                                                "|7|THÔNG TIN NHÂN VẬT"
                                                + "\b\b|7|Tên nhân vật: " + (onlinePlayer.name)
                                                + "\b\b|5|Số tiền: " + Util.format(onlinePlayer.getSession().vnd)
                                                + "\b\b|5|Trang thái: " + (onlinePlayer.getSession().actived == false ? "Chưa kích hoạt" : "Đã kích hoạt")
                                                + "\b\b|8|Số thỏi vàng hiện có: " + sothoivangStr
                                                + "\b|8|Số hồng ngọc: " + hongngoc
                                                + "\b|2|Sức mạnh: " + Util.numberToMoney(onlinePlayer.nPoint.power)
                                                + "\b|2|Nhiệm vụ: " + Util.numberToMoney(onlinePlayer.playerTask.taskMain.id)
                                                + "\b|2|Hành tinh: " + planetName
                                                + "\b\b|1|HP Gốc: " + Util.powerToStringnew(onlinePlayer.nPoint.hpg)
                                                + "\b|1|KI Gốc: " + Util.powerToStringnew(onlinePlayer.nPoint.mpg)
                                                + "\b|1|Sức đánh Gốc: " + Util.powerToStringnew(onlinePlayer.nPoint.dameg)
                                                + "\b|1|Giáp Gốc: " + Util.powerToStringnew(onlinePlayer.nPoint.defg)
                                                + "\b\b|5|Tổng vàng nhặt: " + Util.format(onlinePlayer.vangnhat)
                                                + "\b|3|Tổng Hồng ngọc nhặt: " + Util.format(onlinePlayer.hngocnhat),
                                                new String[]{"Player", "Tặng Đệ", "Buff tiền", "Ban", "Kick"},
                                                onlinePlayer);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case -103:
                    if (player != null) {
                        byte act = _msg.reader().readByte();
                        if (act == 0) {
                            Service.getInstance().openFlagUI(player);
                        } else if (act == 1) {
                            Service.getInstance().chooseFlag(player, _msg.reader().readByte());
                        } else {
                        }
                    }
                    break;
                case -7:
                    if (player != null) {
                        int toX = player.location.x;
                        int toY = player.location.y;
                        try {
                            byte b = _msg.reader().readByte();
                            toX = _msg.reader().readShort();
                            toY = _msg.reader().readShort();
                        } catch (Exception e) {
                        }
                        PlayerService.gI().playerMove(player, toX, toY);
                    }
                    break;
                case -74:
                    byte type = _msg.reader().readByte();
                    if (_session != null) {
                        if (type == 1) {
                            DataGame.sendSizeRes(_session);
                        } else if (type == 2) {
                            DataGame.sendRes(_session);
                        }
                    }
                    break;
                case -81:
                    if (player != null) {
                        _msg.reader().readByte();
                        int[] indexItem = new int[_msg.reader().readByte()];
                        for (int i = 0; i < indexItem.length; i++) {
                            indexItem[i] = _msg.reader().readByte();
                        }
                        CombineServiceNew.gI().showInfoCombine(player, indexItem);
                    }
                    break;
                case -87:
                    DataGame.updateData(_session);
                    break;
                case -67:
                    if (!_session.isRIcon) {
                        int id = _msg.reader().readInt();
                        DataGame.sendIcon(_session, id);
                    }
                    break;
                case 66:
                    if (_session != null) {
                        DataGame.sendImageByName(_session, _msg.reader().readUTF());
                    }
                    break;
                case -66:
                    int effId = _msg.reader().readShort();
                    int idT = effId;
                    if (effId == 25 && GoiRongXuong.gI().isRongxuongAppear == true) {
                        idT = 51; // id eff rong muon thay doi ( hien tai la rong xuong) 
                    }
                    if (effId == 25 && player.zone.map.mapId == 7) {
                        idT = 50; // id eff rong muon thay doi ( hien tai la rong vang) 
                    }
                    if (effId == 25 && GoiRongBang.gI().isRongICEAppear == true) {
                        idT = 50; // id eff rong muon thay doi ( hien tai la rong băng) 
                    }
                    DataGame.effData(_session, effId, idT);
                    break;
                case -62:
                    if (player != null) {
                        FlagBagService.gI().sendIconFlagChoose(player, _msg.reader().readByte());
                    }
                    break;
                case -63:
                    if (player != null) {
                        FlagBagService.gI().sendIconEffectFlag(player, _msg.reader().read());
                    }
                    break;
                case -32:
                    int bgId = _msg.reader().readShort();
                    DataGame.sendItemBGTemplate(_session, bgId);
                    break;
                case 22:
                    if (player != null) {
                        _msg.reader().readByte();
                        NpcManager.getNpc(ConstNpc.DAU_THAN).confirmMenu(player, _msg.reader().readByte());
                    }
                    break;
                case -33:
                case -23:
                    if (player != null) {
                        ChangeMapService.gI().changeMapWaypoint(player);
                        Service.getInstance().hideWaitDialog(player);
                    }
                    break;
                case -45:
                    if (player != null) {
                        SkillService.gI().useSkill(player, null, null, _msg);
                    }

                    break;
                case -46:
                    if (player != null) {
                        ClanService.gI().getClan(player, _msg);
                    }
                    break;
                case -51:
                    if (player != null) {
                        ClanService.gI().clanMessage(player, _msg);
                    }
                    break;
                case -54:
                    if (player != null) {
                        ClanService.gI().clanDonate(player, _msg);
                    }
                    break;
                case -49:
                    if (player != null) {
                        ClanService.gI().joinClan(player, _msg);
                    }
                    break;
                case -50:
                    if (player != null) {
                        ClanService.gI().sendListMemberClan(player, _msg.reader().readInt());
                    }
                    break;
                case -56:
                    if (player != null) {
                        ClanService.gI().clanRemote(player, _msg);
                    }
                    break;
                case -47:
                    if (player != null) {
                        ClanService.gI().sendListClan(player, _msg.reader().readUTF());
                    }
                    break;
                case -55:
                    if (player != null) {
                        ClanService.gI().showMenuLeaveClan(player);
                    }
                    break;
                case -57:
                    if (player != null) {
                        ClanService.gI().clanInvite(player, _msg);
                    }
                    break;
                case -40:
                    if (_session != null) {
                        UseItem.gI().getItem(_session, _msg);
                    }
                    break;
                case -41:
                    Service.getInstance().sendCaption(_session, _msg.reader().readByte());
                    break;
                case -43:
                    if (player != null) {
                        UseItem.gI().doItem(player, _msg);
                    }
                    break;
                case -91:
                    if (player != null) {
                        switch (player.iDMark.getTypeChangeMap()) {
                            case ConstMap.CHANGE_CAPSULE:
                                UseItem.gI().choseMapCapsule(player, _msg.reader().readByte());
                                break;
                            case ConstMap.CHANGE_BLACK_BALL:

                                BlackBallWar.gI().changeMap(player, _msg.reader().readByte());
                                break;
                        }
                    }
                    break;
                case -109:
                    Client.gI().createBot(_session);
                    break;
                case -110:
                    Client.gI().clear();
                    break;
                case -39:
                    if (player != null) {
                        //finishLoadMap
                        ChangeMapService.gI().finishLoadMap(player);
                        if (player.zone.map.mapId == (21 + player.gender)) {
                            if (player.mabuEgg != null) {
                                player.mabuEgg.sendMabuEgg();
                            }
//                            Logger.log(Logger.PURPLE, "done load map nhà!\n");
                        }
                        ChangeMapService.gI().finishLoadMap(player);
                        if (player.zone.map.mapId == (21 + player.gender)) {
                            if (player.mabuEgg != null) {
                                player.mabuEgg.sendMabuEgg();
                            }
                        }
                    }
                    break;
                case 11:
                    byte modId = _msg.reader().readByte();
                    DataGame.requestMobTemplate(_session, modId);
                    break;
                case 44:
                    if (player != null) {
                        Service.getInstance().chat(player, _msg.reader().readUTF());
                    }
                    break;
                case 32:
                    if (player != null) {
                        int npcId = _msg.reader().readShort();
                        int select = _msg.reader().readByte();
                        MenuController.getInstance().doSelectMenu(player, npcId, select);
                    }
                    break;
                case 33:
                    if (player != null) {
                        int npcId = _msg.reader().readShort();
                        if (npcId != 54) {
                            MenuController.getInstance().openMenuNPC(_session, npcId, player);
                        } else {
                            Service.gI().minigame_taixiu(player);
                        }
                    }
                    break;
                case 34:
                    if (player != null) {
                        int selectSkill = _msg.reader().readShort();
                        SkillService.gI().selectSkill(player, selectSkill);
                    }
                    break;
                case 54:
                    if (player != null) {
                        Service.getInstance().attackMob(player, (_msg.reader().readByte()));
                    }
                    break;
                case -60:
                    if (player != null) {
                        int playerId = _msg.reader().readInt();
                        Service.getInstance().attackPlayer(player, playerId);
                    }
                    break;
                case -27:
                    _session.sendKey();
                    DataGame.sendVersionRes(_session);
                    break;
                case -111:
                    DataGame.sendDataImageVersion(_session);
                    break;
                case -20:
                    if (player != null && !player.isDie()) {
                        int itemMapId = _msg.reader().readShort();
                        ItemMapService.gI().pickItem(player, itemMapId, false);
                    }
                    break;
                case -28:
                    messageNotMap(_session, _msg);
                    break;
                case -29:
                    messageNotLogin(_session, _msg);
                    break;
                case -30:
                    messageSubCommand(_session, _msg);
                    break;
                case -15: // về nhà
                    if (player != null) {
                        ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, 0, -1);
                    }
                    break;
                case -16: // hồi sinh
                    if (player != null) {
                        PlayerService.gI().hoiSinh(player);
                    }
                default:
//                    Util.log("CMD: " + cmd);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            byte cmd = _msg.command;
            System.out.println("nnnnn     " + cmd + " - " + _msg.reader().readUTF());
        } finally {
            _msg.cleanup();
            _msg.dispose();
        }
    }

    public void messageNotLogin(MySession session, Message msg) {
        if (msg != null) {
            try {
                byte cmd = msg.reader().readByte();
                switch (cmd) {
                    case 0:
                        try {
                        session.login(msg.reader().readUTF(), msg.reader().readUTF());
                    } catch (IOException e) {
                        session.login(String.valueOf(msg.reader().readChar()), String.valueOf(msg.reader().readChar()));
                    }
                    if (Manager.LOCAL) {
                        break;
                    }
                    break;
                    case 2:
                        Service.getInstance().setClientType(session, msg);
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                Logger.logException(Controller.class, e);
            }
        }
    }



    public void messageNotMap(MySession _session, Message _msg) {
        if (_msg != null) {
            Player player = null;
            try {
                player = _session.player;
                byte cmd = _msg.reader().readByte();
//                System.out.println("CMD receive -28 / " + cmd);
                switch (cmd) {
                    case 2:
                        createChar(_session, _msg);
                        break;
                    case 6:
                        DataGame.updateMap(_session);
                        break;
                    case 7:
                        DataGame.updateSkill(_session);
                        break;
                    case 8:
                        ItemData.updateItem(_session);
                        break;
                    case 10:
                        DataGame.sendMapTemp(_session, _msg.reader().readUnsignedByte());
                        break;
                    case 13:
                        //client ok
                        if (player != null) {
                            Service.getInstance().player(player);
                            Service.getInstance().Send_Caitrang(player);
                            player.zone.load_Another_To_Me(player);

                            // -64 my flag bag
                            Service.getInstance().sendFlagBag(player);
                            ItemTimeService.gI().sendTextBanDoKhoBau(player);
                            ItemTimeService.gI().sendTextDoanhTrai(player);
                            ItemTimeService.gI().sendTextGas(player);

                            // -113 skill shortcut
                            player.playerSkill.sendSkillShortCut();
                            // item time
                            ItemTimeService.gI().sendAllItemTime(player);

                            // send current task
                            TaskService.gI().sendInfoCurrentTask(player);
                        }
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                Logger.logException(Controller.class, e);
            }
        }
    }

    public void messageSubCommand(MySession _session, Message _msg) {
        if (_msg != null) {
            Player player = null;
            try {
                player = _session.player;
                byte command = _msg.reader().readByte();
                switch (command) {
                    case 16:
                        byte type = _msg.reader().readByte();
                        short point = _msg.reader().readShort();
                        if (player != null && player.nPoint != null) {
                            player.nPoint.increasePoint(type, point);
                        }
                        break;
                    case 64:
                        int playerId = _msg.reader().readInt();
                        int menuId = _msg.reader().readShort();
                        SubMenuService.gI().controller(player, playerId, menuId);
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                Logger.logException(Controller.class, e);
            }
        }
    }

    public void createChar(MySession session, Message msg) {
        if (!Maintenance.isRuning) {
            GirlkunResultSet rs = null;
            boolean created = false;
            try {
                String name = msg.reader().readUTF();
                int gender = msg.reader().readByte();
                int hair = msg.reader().readByte();
                if (name.length() <= 10) {
                    rs = GirlkunDB.executeQuery("select * from player where name = ?", name);
                    if (rs.first()) {
                        Service.getInstance().sendThongBaoOK(session, "Tên nhân vật đã tồn tại");
                    } else {
                        if (Util.haveSpecialCharacter(name)) {
                            Service.getInstance().sendThongBaoOK(session, "Tên nhân vật không được chứa ký tự đặc biệt");
                        } else if (Util.kituvip(name) == false) {
                            Service.getInstance().sendThongBaoOK(session, "Tên nhân vật không được chứa ký tự VIP");
                        } else {
                            boolean isNotIgnoreName = true;
                            for (String n : ConstIgnoreName.IGNORE_NAME) {
                                if (name.equals(n)) {
                                    Service.getInstance().sendThongBaoOK(session, "Tên nhân vật đã tồn tại");
                                    isNotIgnoreName = false;
                                    break;
                                }
                            }
                            if (isNotIgnoreName) {
                                created = PlayerDAO.createNewPlayer(session.userId, name.toLowerCase(), (byte) gender, hair);
                            }
                        }
                    }
                } else {
                    Service.getInstance().sendThongBaoOK(session, "Tên nhân vật tối đa 10 ký tự");
                }
            } catch (Exception e) {
                Logger.logException(Controller.class, e);
            } finally {
                if (rs != null) {
                    rs.dispose();
                }
            }
            if (created) {
                session.login(session.uu, session.pp);
            }
        }
    }

    public void login2(MySession session, Message msg) {
        Service.getInstance().switchToRegisterScr(session);
        //  Service.getInstance().sendThongBaoOK(session, "Vui lòng đăng ký tài khoản tại trang chủ!");
    }

    public void sendInfo(MySession session) {
        Player player = session.player;

        // -82 set tile map
        DataGame.sendTileSetInfo(session);

        // 112 my info intrinsic
        IntrinsicService.gI().sendInfoIntrinsic(player);

        // -42 my point
        Service.getInstance().point(player);

        // 40 task
        TaskService.gI().sendTaskMain(player);

        // -22 reset all
        Service.getInstance().clearMap(player);

        // -53 my clan
        ClanService.gI().sendMyClan(player);

        // -69 max statima
        PlayerService.gI().sendMaxStamina(player);

        // -68 cur statima
        PlayerService.gI().sendCurrentStamina(player);

        // -97 năng động
        // -107 have pet
        Service.getInstance().sendHavePet(player);

        // -119 top rank
        
        Service.getInstance().sendMessage(session, -119, "1630679754740_-119_r");

        // -50 thông tin bảng thông báo
        ServerNotify.gI().sendNotifyTab(player);
        // -24 join map - map info
        player.zone.load_Me_To_Another(player);
        player.zone.mapInfo(player);

        // -70 thông báo bigmessage
        sendThongBaoServer(player);

        //check activation set
        player.setClothes.setup();
        if (player.pet != null) {
            player.pet.setClothes.setup();
        }
        // last time use skill
        Service.gI().sendTimeSkill(player);
        
        //clear vt sk
        clearVTSK(player);

        if (TaskService.gI().getIdTask(player) == ConstTask.TASK_0_0) {
            NpcService.gI().createTutorial(player, -1,
                    "|7|Chào mừng " + player.name + " đến với Ngọc rồng Online"
                    + "Nhiệm vụ đầu tiên của bạn là di chuyển\n"
                    + "Bạn hãy di chuyển nhân vật theo mũi tên chỉ hướng");
        } else {
            NpcService.gI().createTutorial(player, -1,
                    "|7|Chào mừng " + player.name + " đến với Ngọc rồng Online"
                    + "\n|7|Chúc anh em chơi game vui vẻ");
        }
        if (GoiRongXuong.gI().playerRongXuong != null
                && GoiRongXuong.gI().playerRongXuong.id == player.id) {
            ServerNotify.gI().notify("Người chơi: " + player.name + " gọi Rồng Xương tại "
                    + GoiRongXuong.gI().mapRongxuongAppear.map.mapName + " khu " + GoiRongXuong.gI().mapRongxuongAppear.zoneId + " đã vào lại Game");
        }
        if (GoiRongBang.gI().playerRongICE != null
                && GoiRongBang.gI().playerRongICE.id == player.id) {
            ServerNotify.gI().notify("Người chơi: " + player.name + " gọi Rồng Băng tại "
                    + GoiRongBang.gI().mapRongICEAppear.map.mapName + " khu " + GoiRongBang.gI().mapRongICEAppear.zoneId + " đã vào lại Game");
        }
        if (SummonDragon.gI().playerSummonShenron != null
                && SummonDragon.gI().playerSummonShenron.id == player.id) {
            ServerNotify.gI().notify("Người chơi: " + player.name + " gọi Rồng Thần tại "
                    + SummonDragon.gI().mapShenronAppear.map.mapName + " khu " + SummonDragon.gI().mapShenronAppear.zoneId + " đã vào lại Game");
        }
        if (player.TrieuHoiCapBac != -1) {
            ServerNotify.gI().notify("Người chơi: " + player.name + " sở hữu ĐỆ TỬ cấp bậc: "
                    + player.NameThanthu(player.TrieuHoiCapBac) + " Đã vào game");
        }
        if (player.inventory.itemsBody.get(11).isNotNullItem()) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.getInstance().sendFoot(player, (short) player.inventory.itemsBody.get(11).template.id);
                } catch (Exception e) {

                }
            }).start();
        }
        if (player.inventory.itemsBody.get(10).isNotNullItem()) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().sendPetFollow(player,
                            (short) (player.inventory.itemsBody.get(10).template.iconID - 1));
                } catch (Exception e) {
                }
            }).start();
        }
        if (player.inventory.itemsBody.get(7).isNotNullItem()) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Item it = player.inventory.itemsBody.get(7);
                    player.newpet=null;
                    PetService.Pet2(player, it.template.head, it.template.body, it.template.leg,
                    it.template.name);
                     Service.getInstance().point(player);
                } catch (Exception e) {
                }
            }).start();
        }
        if (player.lastTimeTitle1 > 0 ||player.lastTimeTitle2 > 0||player.lastTimeTitle3 > 0||player.lastTimeTitle4 > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().removeTitle(player);
                } catch (Exception e) {
                }
            }).start();
        }

        if (player.nPoint.dame >= 30000000 && !player.isAdmin()) {
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    PlayerService.gI().banPlayer(player);
                } catch (Exception e) {
                }
            }).start();
        }
        
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    GirlkunResultSet ipBanned = GirlkunDB.executeQuery("SELECT ip_address FROM account WHERE ban = true");
                    while (ipBanned.next()) {
                        String bannedIP = ipBanned.getString("ip_address");
                        // So sánh địa chỉ IP của người truy cập với các địa chỉ IP bị cấm
                        if (session.ipAddress.equals(bannedIP)) {
                            Thread.sleep(10000);
                            PlayerService.gI().banPlayer(player);
                        }
                    }
                } catch (Exception e) {
                }
            }).start();
//        if (player.lastTimeTitle1 > 0) {
//            scheduler.scheduleAtFixedRate(() -> {
//                try {
//                    Service.getInstance().sendTitle(player, (short) 888);
//                } catch (Exception e) {
//
//                }
//            }, 0, 5000, TimeUnit.MILLISECONDS);
//        }
//        if (player.lastTimeTitle2 > 0) {
//            scheduler.scheduleAtFixedRate(() -> {
//                try {
//                    Service.getInstance().sendTitle(player, (short) 889);
//                } catch (Exception e) {
//
//                }
//            }, 0, 5000, TimeUnit.MILLISECONDS);
//        }
//        if (player.lastTimeTitle3 > 0) {
//            scheduler.scheduleAtFixedRate(() -> {
//                try {
//                    Service.getInstance().sendTitle(player, (short) 890);
//                } catch (Exception e) {
//
//                }
//            }, 0, 5000, TimeUnit.MILLISECONDS);
//        }
//        if (player.lastTimeTitle4 > 0) {
//            scheduler.scheduleAtFixedRate(() -> {
//                try {
//                    Service.getInstance().sendTitle(player, (short) 891);
//                } catch (Exception e) {
//
//                    e.printStackTrace();
//                }
//            }, 0, 5000, TimeUnit.MILLISECONDS);
//        }
    }

    private void sendThongBaoServer(Player player) {
        Service.getInstance().sendThongBaoFromAdmin(player, "|7| Chào mừng bạn đến với Ngọc rồng "
                + "Server với nhiều tính năng phù hợp cho anh em cày cuốc lâu dài\n|1| Chúc mọi người chơi Game vui vẻ !!!");
    }

    private void clearVTSK(Player player) {
        player.inventory.itemsBag.stream().filter(item -> item.isNotNullItem() && item.template.id == 610).forEach(item -> {
            InventoryServiceNew.gI().subQuantityItemsBag(player, item, item.quantity);
        });
        player.inventory.itemsBox.stream().filter(item -> item.isNotNullItem() && item.template.id == 610).forEach(item -> {
            InventoryServiceNew.gI().subQuantityItemsBox(player, item, item.quantity);
        });
        InventoryServiceNew.gI().sendItemBags(player);
    }
}
