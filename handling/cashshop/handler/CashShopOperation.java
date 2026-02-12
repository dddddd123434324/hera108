package handling.cashshop.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleQuestStatus;
import client.inventory.*;
import constants.GameConstants;
import handling.cashshop.CashShopServer;
import handling.login.LoginServer;
import handling.world.*;
import server.*;
import server.log.LogType;
import server.log.ServerLogger;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CSPacket;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import server.quest.MapleQuest;

public class CashShopOperation {

    private static Connection Connection;

    public static void LeaveCS(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        chr.goDonateShop(false);
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        try {
            World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannel());

            LoginServer.setCodeHash(chr.getId(), c.getCodeHash());
            c.getSession().write(MaplePacketCreator.getChannelChange(c, c.getChannelServer().getPort()));
        } finally {
            final String s = c.getSessionIPAddress();
            LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
            chr.saveToDB(false, true);
            c.setPlayer(null);
            c.setReceiving(false);
            //chr.spawnSavedPets();
            //c.getSession().close();
        }
    }

    public static void CashShopEnter(final int playerid, final MapleClient c) {
        if (CashShopServer.isShutdown()) {
            c.getSession().close(true);
            return;
        }
        CharacterTransfer transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, c, false);

        c.setPlayer(chr);
        c.setAccID(chr.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close(true);
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
            if (!World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                allowLogin = true;
            }
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close(true);
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        if (chr.getParty() != null) {
            final MapleParty party = chr.getParty();
            World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(chr));
        }
        CashShopServer.getPlayerStorage().registerPlayer(chr);
        c.getSession().write(CSPacket.warpCS(c));
        CSUpdate(c);
    }

    public static final void doCSPackets(MapleClient c) {
        c.getSession().write(CSPacket.enableCSUse());
        c.getSession().write(CSPacket.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(CSPacket.getCSInventory(c));//
        c.getPlayer().getCashInventory().checkExpire(c);
    }

    public static void CSUpdate(final MapleClient c) {
        c.getSession().write(CSPacket.getCSGifts(c, c.getPlayer().isDonateShop())); //
        doCSPackets(c);//
        c.getSession().write(CSPacket.sendWishList(c.getPlayer(), false)); //
    }

    public static void CouponCode(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(2);
        final String code = slea.readMapleAsciiString();
        if (code == null || code.length() < 16 || code.length() > 32) {
            //c.getSession().write(CSPacket.sendCouponFail(c, 0x0E));
            doCSPackets(c);
            return;
        }
        Triple<Boolean, Integer, Integer> info = null;
        Pair<Short, Integer> info2 = null;
        try {
            info = MapleCharacterUtil.getNXCodeInfo(code);
            info2 = MapleCharacterUtil.getNXCodeInfo2(code);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (info != null && info.left) {
            int type = info.mid, item = info.right;
            short quantity = info2.left;
            int expiredate = info2.right;
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }

            int maplePoints = 0, mesos = 0;
            String couponType = null;
            boolean used = false;
            switch (type) {
                case 1: //캐시 지급
                    couponType = "캐시";
                    c.getPlayer().modifyCSPoints(type, item, false);
                    maplePoints = item;
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 넥슨캐시 " + item + "원을 받았습니다."));
                    used = true;
                    break;
                case 2: //메이플 포인트 지급
                    couponType = "메이플 포인트";
                    c.getPlayer().modifyCSPoints(type, item, false);
                    maplePoints = item;
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 메이플 포인트 " + item + "점을 받았습니다."));
                    used = true;
                    break;
                case 3: //아이템 지급
                    couponType = "아이템";
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    Item itemz;
                    Item itemz2;
                    if (GameConstants.getInventoryType(item) == MapleInventoryType.EQUIP) {
                        itemz = ii.getEquipById(item);
                        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() == 0) {
                            c.getSession().write(MaplePacketCreator.serverNotice(1, "장비 인벤토리 공간이 부족합니다."));
                        } else {
                            for (int i = 0; i < quantity; i++) {
                                if (expiredate > 0) { //기간이 있으면
                                    itemz.setExpiration((long) (System.currentTimeMillis() + (long) (expiredate * 24 * 60 * 60 * 1000)));
                                    c.getPlayer().getCashInventory().addToInventory(itemz);
                                    itemz2 = itemz.copy();
                                    short pos = MapleInventoryManipulator.addbyItem(c, itemz2, true);
                                    doCSPackets(c);
                                    c.getPlayer().getCashInventory().removeFromInventory(itemz);
                                    c.getSession().write(CSPacket.confirmFromCSInventory(itemz2, pos));
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + MapleItemInformationProvider.getInstance().getName(item) + " 을(를) 받았습니다. \r\n (유효기간 : " + expiredate + "일)")); //장비템 갯수가 애매하네
                                } else { //엘스
                                    c.getPlayer().getCashInventory().addToInventory(itemz);
                                    itemz2 = itemz.copy();
                                    short pos = MapleInventoryManipulator.addbyItem(c, itemz2, true);
                                    doCSPackets(c);
                                    c.getPlayer().getCashInventory().removeFromInventory(itemz);
                                    c.getSession().write(CSPacket.confirmFromCSInventory(itemz2, pos));
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + MapleItemInformationProvider.getInstance().getName(item) + " 을(를) 받았습니다.")); //
                                }
                            }
                            used = true;
                        }
                    } else {
                        itemz = new client.inventory.Item(item, (byte) 0, quantity, (byte) 0);
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() == 0 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() == 0 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() == 0 || c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() == 0) {
                            c.getSession().write(MaplePacketCreator.serverNotice(1, "인벤토리 공간이 부족합니다."));
                        } else {
                            if (expiredate > 0) { // 기간이 있으면
                                itemz.setExpiration((long) (System.currentTimeMillis() + (long) (expiredate * 24 * 60 * 60 * 1000)));

                                if (GameConstants.isPet(item)) {
                                    final MaplePet pet = MaplePet.createPet(item, MapleInventoryIdentifier.getInstance());
                                    itemz.setPet(pet);
                                    short pos = MapleInventoryManipulator.addbyItem(c, itemz, true);
                                    c.getSession().write(CSPacket.confirmFromCSInventory(itemz, pos));
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + MapleItemInformationProvider.getInstance().getName(item) + "을(를) 받았습니다.\r\n (유효기간 : " + expiredate + "일)")); //펫은 아마도 2개줄일은 없을듯
                                } else {
                                    itemz.setExpiration((long) (System.currentTimeMillis() + (long) (expiredate * 24 * 60 * 60 * 1000)));
                                    c.getPlayer().getCashInventory().addToInventory(itemz);
                                    itemz2 = itemz.copy();
                                    short pos = MapleInventoryManipulator.addbyItem(c, itemz2, true);
                                    doCSPackets(c);
                                    c.getPlayer().getCashInventory().removeFromInventory(itemz);
                                    c.getSession().write(CSPacket.confirmFromCSInventory(itemz2, pos));
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + MapleItemInformationProvider.getInstance().getName(item) + "을(를) " + quantity + "개 받았습니다. \r\n (유효기간 : " + expiredate + "일)"));
                                }
                            } else { // 엘스
                                //   itemz.setExpiration((long) (System.currentTimeMillis() + (long) (expiredate * 24 * 60 * 60 * 1000)));
                                if (GameConstants.isPet(item)) {
                                    final MaplePet pet = MaplePet.createPet(item, MapleInventoryIdentifier.getInstance());
                                    itemz.setPet(pet);
                                    short pos = MapleInventoryManipulator.addbyItem(c, itemz, true);
                                    c.getSession().write(CSPacket.confirmFromCSInventory(itemz, pos));
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + MapleItemInformationProvider.getInstance().getName(item) + "을(를) " + quantity + "개 받았습니다."));
                                } else {
                                    c.getPlayer().getCashInventory().addToInventory(itemz);
                                    itemz2 = itemz.copy();
                                    short pos = MapleInventoryManipulator.addbyItem(c, itemz2, true);
                                    doCSPackets(c);
                                    c.getPlayer().getCashInventory().removeFromInventory(itemz);
                                    c.getSession().write(CSPacket.confirmFromCSInventory(itemz2, pos));
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + MapleItemInformationProvider.getInstance().getName(item) + "을(를) " + quantity + "개 받았습니다."));
                                }
                            }
                            used = true;
                        }
                    }

                    //   c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + MapleItemInformationProvider.getInstance().getName(item) + "을(를) " + quantity + "개 받았습니다."));
                    break;

                case 4: //메소 지급
                    couponType = "메소";
                    c.getPlayer().gainMeso(item, false);
                    mesos = item;
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "쿠폰을 사용하여 " + item + "메소를 받았습니다."));
                    used = true;
                    break;
            }
            if (used) {
                MapleCharacterUtil.setNXCodeUsed(c.getPlayer().getName(), code);
                String etcData = "";
                if (type == 3) {
                    etcData = " " + quantity + "개 / 유효기간 : " + (expiredate > 0 ? String.valueOf(expiredate) + "일" : "없음");
                }
                ServerLogger.getInstance().logCoupon(LogType.Trade.Coupon, c.getPlayer().getId(), c.getPlayer().getName(), code, couponType, type == 3 ? " / 데이터 : " + MapleItemInformationProvider.getInstance().getName(item) : " / 획득량 : " + String.valueOf(item), etcData);
                //    c.getSession().write(CSPacket.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
            }
        } else {
            //   c.getSession().write(CSPacket.sendCSFail(info == null ? 88 : 88)); //A1, 9F
            c.getSession().write(MaplePacketCreator.serverNotice(1, "이미 사용되었거나 잘못된 쿠폰번호입니다. 쿠폰번호를 다시 한 번 확인하여 주십시오."));
            doCSPackets(c);
        }
    }

    public static final void BuyCashItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int action = slea.readByte();
        //chr.dropMessage(1, "action" + action);
        if (action == 3) { //buy  1.2.109 OK
            final int toCharge = slea.readByte() + 1;
            int sn = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(sn, c.getPlayer().isDonateShop());

            if (item != null && chr.getCSPoints(toCharge) >= item.getPrice()) {
                if (!item.genderEquals(c.getPlayer().getGender())) {
                    c.getSession().write(CSPacket.sendCSFail(130));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    c.getSession().write(CSPacket.sendCSFail(129));
                    doCSPackets(c);
                    return;
                }

                for (int i : GameConstants.cashBlock) {
                    if (item.getId() == i) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        doCSPackets(c);
                        return;
                    }
                }
                if (item.getPeriod() == 365) {
                    chr.dropMessage(1, "365일 기간제 아이템들은 모두 기간이 7일로 설정 됩니다.");
                }

                MapleInventoryType type = GameConstants.getInventoryType(item.getId());
                if (type != MapleInventoryType.CASH) {
                    if (c.getPlayer().getInventory(type).getNumFreeSlot() == 0) {
                        //c.getSession().write(CSPacket.sendCSFail(숫자)); 이거 패킷 알아서찾아주셈ㅋ id = 915
                        chr.dropMessage(1, "남은 아이템 슬롯이\r\n부족하지 않은지 확인해보세요");
                        doCSPackets(c);
                        return;
                    }
                }
                Item itemz = chr.getCashInventory().toItem(item, chr.isDonateShop());
                if (itemz != null && itemz.getItemId() == item.getId() && itemz.getQuantity() == item.getCount()) {
                    if (itemz.getUniqueId() > 0) {
                        if (toCharge == 1 && itemz.getType() == 1) {
                            itemz.setFlag((short) (ItemFlag.KARMA_EQ.getValue()));
                        } else if (toCharge == 1 && itemz.getType() != 1) {
                            itemz.setFlag((short) (ItemFlag.KARMA_USE.getValue()));
                        }
                        chr.getCashInventory().addToInventory(itemz);
                        //System.out.println("item.getSN()"+item.getSN());
                        itemz.setGMLog("캐시샵에서 구매 구매자: " + chr.getName());
                        c.getSession().write(CSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                        chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                    } else {
                        short pos = -1;
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        if (GameConstants.getInventoryType(item.getId()) == MapleInventoryType.EQUIP) {
                            itemz = ii.getEquipById(item.getId());

                            if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() == 0) {
                                c.getSession().write(MaplePacketCreator.serverNotice(1, "장비 인벤토리 공간이 부족합니다."));
                            } else {
                                if (item.getPeriod() > 0) { //기간이 있으면
                                    itemz.setExpiration((long) (System.currentTimeMillis() + (long) (item.getPeriod() * 24 * 60 * 60 * 1000)));
                                }
                                pos = MapleInventoryManipulator.addbyItem(c, itemz, true);
                            }
                        } else {
                            itemz = new client.inventory.Item(item.getId(), (byte) 0, (short) item.getCount(), (byte) 0);
                            if (toCharge == 1 && itemz.getType() != 1) {
                                if (itemz.getItemId() == 2439986 || itemz.getItemId() == 2439987 || itemz.getItemId() == 2439988 || itemz.getItemId() == 2439989) {
                                    itemz.setFlag((short) (ItemFlag.KARMA_USE.getValue()));
                                }
                            }
                            itemz.setGMLog("캐시샵에서 구매 구매자: " + chr.getName());
                            pos = MapleInventoryManipulator.addbyItem(c, itemz, true);
                        }
                        if (pos < 0) {
                            c.getSession().write(CSPacket.sendCSFail(129));
                            doCSPackets(c);
                            return;
                        }
                        c.getSession().write(CSPacket.showBoughtCSNormalItem(1, (short) item.getCount(), (byte) pos, itemz.getItemId()));
                        chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                    }
                    CashItemSaleRank.inc(sn, chr.getGender(), chr.isDonateShop());
                } else {
                    c.getSession().write(CSPacket.sendCSFail(0));
                }
            } else {
                c.getSession().write(CSPacket.sendCSFail(0));
            }
        } else if (action == 5) { // 찜  1.2.41 ok
            chr.clearWishlist();
            if (slea.available() < 40) {
                c.getSession().write(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            int[] wishlist = new int[10];
            for (int i = 0; i < 10; i++) {
                wishlist[i] = slea.readInt();
            }
            chr.setWishlist(wishlist);
            c.getSession().write(CSPacket.sendWishList(chr, true));
        } else if (action == 6) { // 슬롯늘리기  1.2.41 OK
            final int toCharge = slea.readByte() + 1;
            final boolean coupon = slea.readByte() > 0;
            if (coupon) {
                final MapleInventoryType type = getInventoryType(slea.readInt());
                if (type.getType() != 4 && chr.getCSPoints(toCharge) >= 100 && chr.getInventory(type).getSlotLimit() <= 96) {
                    chr.modifyCSPoints(toCharge, -100, false);
                    chr.getInventory(type).addSlot((byte) 8);
                    c.getSession().write(CSPacket.increasedInvSlots(type.getType(), chr.getInventory(type).getSlotLimit()));
                } else if (type.getType() == 4 && chr.getCSPoints(toCharge) >= 100 && chr.getInventory(type).getSlotLimit() <= 96) {
                    chr.modifyCSPoints(toCharge, -100, false);
                    chr.getInventory(type).addSlot((byte) 8);
                    c.getSession().write(CSPacket.increasedInvSlots(type.getType(), chr.getInventory(type).getSlotLimit()));
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "선택된 아아템 창 개수를\r\n더 이상 늘릴 수 없습니다"));
                }
            } else {
                final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                if (type.getType() != 4 && chr.getCSPoints(toCharge) >= 100 && chr.getInventory(type).getSlotLimit() <= 96) {
                    chr.modifyCSPoints(toCharge, -100, false);
                    chr.getInventory(type).addSlot((byte) 4);
                    c.getSession().write(CSPacket.increasedInvSlots(type.getType(), chr.getInventory(type).getSlotLimit()));
                } else if (type.getType() == 4 && chr.getCSPoints(toCharge) >= 100 && chr.getInventory(type).getSlotLimit() <= 96) {
                    chr.modifyCSPoints(toCharge, -100, false);
                    chr.getInventory(type).addSlot((byte) 4);
                    c.getSession().write(CSPacket.increasedInvSlots(type.getType(), chr.getInventory(type).getSlotLimit()));
                } else {
                    c.getSession().write(MaplePacketCreator.serverNotice(1, "선택된 아아템 창 개수를\r\n더 이상 늘릴 수 없습니다"));
                }
            }
        } else if (action == 7) { // 창고 증가 1.2.41 ok
            final int toCharge = slea.readByte() + 1;
            final int coupon = 1;
            if (chr.getCSPoints(toCharge) >= 100 * coupon && chr.getStorage().getSlots() <= (48 - (4 * coupon))) {
                chr.modifyCSPoints(toCharge, -100 * coupon, false);
                chr.getStorage().increaseSlots((byte) (4 * coupon));
                Connection con = null;
                try {
                    chr.getStorage().saveToDB(con);
                    con.close();
                } catch (Exception e) {
                } finally {
                    try {
                        if (con != null) {
                            con.close();
                        }
                    } catch (Exception e) {
                    }
                }
                c.getSession().write(CSPacket.increasedStorageSlots(chr.getStorage().getSlots()));
                //창고 멘트가 없는것 같다
            } else {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "창고 개수를 더 이상 늘릴 수 없습니다"));
            }
        } else if (action == 8) { // 캐릭터 슬롯 늘리기
            slea.skip(1);
            final int toCharge = 1;
            CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt(), c.getPlayer().isDonateShop());
            int slots = c.getCharacterSlots();
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || slots > 15 || item.getId() != 5430000) {
                c.getSession().write(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            if (c.gainCharacterSlot()) {
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                c.sendPacket(CSPacket.increasedCharacterSlots(slots + 1));
            } else {
                c.getSession().write(CSPacket.sendCSFail(0));
            }
        } else if (action == 10) { // 펜던트 슬롯 늘리기
            final int toCharge = slea.readByte() + 1;
            final int sn = slea.readInt();
            CashItemInfo item = CashItemFactory.getInstance().getItem(sn, c.getPlayer().isDonateShop());
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || item.getId() / 10000 != 555) {
                c.getSession().write(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            MapleQuestStatus marr = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
            long days = 0;
            if (item.getId() == 5550000) { // 펜던트 슬롯늘리기 : 30일
                days = 30;
            } else if (item.getId() == 5550001) { // 펜던트 슬롯늘리기 : 7일
                days = 7;
            }
            if (marr != null && marr.getCustomData() != null && Long.parseLong(marr.getCustomData()) >= System.currentTimeMillis()) {//연장
                long exstingDay = Long.parseLong(c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).getCustomData());
                c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(String.valueOf(exstingDay + days * 24 * 60 * 60000));
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                c.getSession().write(CSPacket.extraSlotDone((short) days));
                doCSPackets(c);
            } else {//첫구매
                c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(String.valueOf(System.currentTimeMillis() + days * 24 * 60 * 60000));
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                c.getSession().write(CSPacket.extraSlotDone((short) days));
                doCSPackets(c);
            }
        } else if (action == 14) { //get item from csinventory 1.2.41 ok
            //uniqueid, 00 01 01 00, type->position(short)
            long uniqueId = slea.readLong();
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            Item item = c.getPlayer().getCashInventory().findByCashId((int) uniqueId);
            if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                Item item_ = item.copy();
                short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        item_.getPet().setInventoryPosition(pos);
                        c.getPlayer().addPet(item_.getPet());
                    }
                    c.getPlayer().getCashInventory().removeFromInventory(item);
                    c.getSession().write(CSPacket.confirmFromCSInventory(item_, pos));
                } else {
                    c.getSession().write(CSPacket.sendCSFail(0));
                    System.out.println("pos가 0보다 작아@@@@@@@@@@@");
                }
            } else {
                c.getSession().write(CSPacket.sendCSFail(0));
                System.out.println((item != null) + " " + (item.getQuantity() > 0) + " " + MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner()));
            }
        } else if (action == 15) { //put item in cash inventory  1.2.41 ok
            int uniqueid = (int) slea.readLong();
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            Item item = c.getPlayer().getInventory(type).findByUniqueId(uniqueid);
            if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && c.getPlayer().getCashInventory().getItemsSize() < 100) {
                Item item_ = item.copy();
                MapleInventoryManipulator.removeFromSlot(c, type, item.getPosition(), item.getQuantity(), false, false, false);
                if (item_.getPet() != null) {
                    c.getPlayer().removePetCS(item_.getPet());
                }
                item_.setPosition((byte) 0);
                c.getPlayer().getCashInventory().addToInventory(item_);
                c.getSession().write(CSPacket.confirmToCSInventory(item, c.getAccID(), -1));
            } else {
                c.getSession().write(CSPacket.sendCSFail(0));
            }
        } else if (action == 31 || action == 37) { //36 = friendship, 30 = crush   1.2.65 OK
            if (c.getPlayer().isGM() && !c.getPlayer().isSuperGM()) {
                c.getPlayer().dropMessage(1, "GM은 캐시를 선물할 수 없습니다.");
                doCSPackets(c);
                return;
            }
            //1.2.41 : 0x19 : crush
            //1.2.41 : 0x1F : friendship

            slea.skip(4); //idcode2
            final int toCharge = 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt(), c.getPlayer().isDonateShop());
            final String partnerName = slea.readMapleAsciiString();
            final String msg = slea.readMapleAsciiString();
            if (item == null || !GameConstants.isEffectRing(item.getId()) || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || msg.length() > 73 || msg.length() < 1) {
                c.getSession().write(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.getSession().write(CSPacket.sendCSFail(143));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                c.getSession().write(CSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            for (int i : GameConstants.cashBlock) { //just incase hacker
                if (item.getId() == i) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId()) {
                c.getSession().write(CSPacket.sendCSFail(144)); //9E v75
                doCSPackets(c);
                return;
            } else if (info.getMid().intValue() == c.getAccID()) {
                c.getSession().write(CSPacket.sendCSFail(130)); //9D v75
                doCSPackets(c);
                return;
            } else {
                if (info.getRight().intValue() == c.getPlayer().getGender() && action == 0x1C) {
                    c.getSession().write(CSPacket.sendCSFail(143)); //9B v75
                    doCSPackets(c);
                    return;
                }

                int err = MapleRing.createRing(item.getId(), c.getPlayer(), partnerName, msg, info.getLeft().intValue(), item.getSN());

                if (err != 1) {
                    c.getSession().write(CSPacket.sendCSFail(0)); //9E v75
                    doCSPackets(c);
                    return;
                }
                ServerLogger.getInstance().logTrade(LogType.Trade.CashShopGift, c.getPlayer().getId(), c.getPlayer().getName(), partnerName, "시리얼 넘버 : " + item.getSN() + " - " + item.getCount() + " 개 / 캐시 : " + item.getPrice(), (c.getPlayer().isDonateShop() ? "본섭캐시샵" : "일반캐시샵") + " / 메시지 : " + msg + " / " + (action == 0x1C ? "커플링" : "우정링"));
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                c.getSession().write(CSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
                MapleCharacterUtil.sendNote(partnerName, c.getPlayer().getName(), "캐시샵에 선물이 도착했습니다. 확인해 주세요.", 0);
            }

        } else if (action == 32) { // 패키지 구매. 1.2.41 OK
            final int toCharge = slea.readByte() + 1;
            //   slea.skip(1);
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt(), c.getPlayer().isDonateShop());
            List<Integer> ccc = null;
            if (item != null) {
                ccc = CashItemFactory.getInstance().getPackageItems(item.getId());
            }
            if (item == null || ccc == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()) {
                c.getSession().write(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.getSession().write(CSPacket.sendCSFail(130));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - ccc.size())) {
                c.getSession().write(CSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            for (int iz : GameConstants.cashBlock) {
                if (item.getId() == iz) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            Map<Integer, Item> ccz = new HashMap<Integer, Item>();
            for (int i : ccc) {
                final CashItemInfo cii = CashItemFactory.getInstance().getSimpleItem(i);
                if (cii == null) {
                    continue;
                }
                Item itemz = c.getPlayer().getCashInventory().toItem(cii, chr.isDonateShop());
                if (itemz == null || itemz.getUniqueId() <= 0) {
                    continue;
                }
                for (int iz : GameConstants.cashBlock) {
                    if (itemz.getItemId() == iz) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        doCSPackets(c);
                        return;
                    }
                }
                ccz.put(i, itemz);
            }
            for (Item itemsa : ccz.values()) {
                c.getPlayer().getCashInventory().addToInventory(itemsa);
            }
            CashItemSaleRank.inc(item.getSN(), chr.getGender(), c.getPlayer().isDonateShop());
            chr.modifyCSPoints(toCharge, -item.getPrice(), false);
            c.getSession().write(CSPacket.showBoughtCSPackage(ccz, c.getAccID()));

        } else if (action == 34) { // Quest Item.. 1.2.31 OK
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt(), c.getPlayer().isDonateShop());
            if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getId())) {
                c.getSession().write(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getMeso() < item.getPrice()) {
                c.getSession().write(CSPacket.sendCSFail(148));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() < 0) {
                c.getSession().write(CSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            for (int iz : GameConstants.cashBlock) {
                if (item.getId() == iz) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            byte pos = MapleInventoryManipulator.addId(c, item.getId(), (short) item.getCount(), null, "Cash shop: quest item" + " on " + FileoutputUtil.CurrentReadable_Date());
            if (pos < 0) {
                c.getSession().write(CSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            chr.gainMeso(-item.getPrice(), false);
            c.getSession().write(CSPacket.showBoughtCSNormalItem(1, (short) item.getCount(), pos, item.getId()));
        } else if (action == 0x1C) { //Pay Back.. 1.2.65 OK
            slea.skip(4); //idcode2
            int uid = (int) slea.readLong();
            Item item = c.getPlayer().getCashInventory().findByCashId(uid);
            if (item == null || item.getExpiration() != -1 || item.getItemId() / 1000000 != 1) {
                c.getSession().write(CSPacket.sendCSFail(129));
                doCSPackets(c);
                return;
            }
            c.getPlayer().getCashInventory().removeFromInventory(item);
            c.getSession().write(CSPacket.payBackResult(uid, 0));
        } else if (action == 35) { //뭔진 모름
            chr.dropMessage(1, "응 구라야~~");
            c.getSession().write(CSPacket.응모());
        } else if (action == 42) { //뭔진 모름
            c.getSession().write(CSPacket.redeemResponse());
        } else {
            c.getSession().write(CSPacket.sendCSFail(0));
        }
        doCSPackets(c);
    }

    public static final void GiftCashItem(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4); //idcode 2
        final int itemSN = slea.readInt();
        boolean donateShop = c.getPlayer().isDonateShop();
        final CashItemInfo item = CashItemFactory.getInstance().getItem(itemSN, donateShop);

        //System.out.println("itemSN:" + itemSN);
        if (donateShop) {
            c.getPlayer().dropMessage(1, "후원상점 아이템들은 선물하실 수 없습니다. \r\n캐시교환을 이용해 주세요.");
            doCSPackets(c);
            return;
        }
        String partnerName = slea.readMapleAsciiString();
        String msg = slea.readMapleAsciiString();
        if (item == null || c.getPlayer().getCSPoints(donateShop ? 3 : 1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) { //dont want packet editors gifting random stuff =P
            c.getSession().write(CSPacket.sendCSFail(0));
            doCSPackets(c);
            return;
        }
        Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
        if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId() || info.getMid().intValue() == c.getAccID()) {
            c.getSession().write(CSPacket.sendCSFail(130)); //9E v75
            doCSPackets(c);
            return;
        } else if (!item.genderEquals(info.getRight().intValue())) {
            c.getSession().write(CSPacket.sendCSFail(130));
            doCSPackets(c);
            return;
        } else {
            if (!c.getPlayer().getCashInventory().giftMax(info.getLeft().intValue())) {
                c.getPlayer().dropMessage(1, "선물 받는 캐릭터의 선물함이 가득찼습니다.");
                doCSPackets(c);
                return;
            }
            c.getPlayer().getCashInventory().gift(info.getLeft().intValue(), c.getPlayer().getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
            ServerLogger.getInstance().logTrade(LogType.Trade.CashShopGift, c.getPlayer().getId(), c.getPlayer().getName(), partnerName, "시리얼 넘버 : " + item.getSN() + " - " + item.getCount() + " 개 / 캐시 : " + item.getPrice(), (c.getPlayer().isDonateShop() ? "본섭캐시샵" : "일반캐시샵") + " / 메시지 : " + msg);

            if (itemSN / 10000000 == 4 || itemSN / 10000000 == 7 || itemSN / 100 == 100019) {
                c.getSession().write(CSPacket.sendPackageGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
            } else {
                c.getSession().write(CSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName));
            }
            c.getPlayer().modifyCSPoints(donateShop ? 3 : 1, -item.getPrice(), false);
            MapleCharacterUtil.sendNote(partnerName, c.getPlayer().getName(), "캐시샵에 선물이 도착했습니다. 확인해 주세요.", 0);
            doCSPackets(c);
        }
    }

    private static final MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 50200016:
                return MapleInventoryType.EQUIP;
            case 50200017:
                return MapleInventoryType.USE;
            case 50200018:
                return MapleInventoryType.SETUP;
            case 50200019:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }
}
