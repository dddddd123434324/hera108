/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import client.*;
import client.anticheat.CheatingOffense;
import client.inventory.*;
import client.inventory.Equip.ScrollResult;
import client.inventory.MaplePet.PetFlag;
import constants.GameConstants;
import static constants.GameConstants.isWeapon;
import database.DatabaseConnection;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import scripting.EtcScriptInvoker;
import scripting.ItemScriptManager;
import server.*;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.*;
import server.quest.MapleQuest;
import server.shops.*;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CSPacket;
import tools.packet.PetPacket;
import tools.packet.PlayerShopPacket;
import constants.ServerConstants;
import client.MapleCharacter;
import client.MapleCharacterUtil;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import scripting.NPCScriptManager;
import scripting.vm.NPCScriptInvoker;
import tools.packet.UIPacket;

public class InventoryHandler {

    public static void ItemMove(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        //2D 96 4F 9E 00 01 F5 FF 01 00 FF FF
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte()); //04
        final short src = slea.readShort();                                            //01 00
        final short dst = slea.readShort();                                            //00 00
        final short quantity = slea.readShort();                                       //53 01

        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            if (dst > c.getPlayer().getInventory(type).getSlotLimit()) {
                c.getPlayer().dropMessage(1, "아이템을 황천으로 보내는 것은 불가능합니다.");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }

    public static final void SwitchBag(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        final short src = (short) slea.readInt();                                       //01 00
        final short dst = (short) slea.readInt();                                            //00 00
        if (src < 100 || dst < 100) {
            return;
        }
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, src, dst);
    }

    public static final void MoveBag(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        c.getPlayer().updateTick(slea.readInt());
        final boolean srcFirst = slea.readInt() > 0;
        short dst = (short) slea.readInt();                                       //01 00
        if (slea.readByte() != 4) { //must be etc) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        short src = slea.readShort();                                            //00 00
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, srcFirst ? dst : src, srcFirst ? src : dst);
    }

    public static final void ItemSort(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final MapleInventoryType pInvType = MapleInventoryType.getByType(slea.readByte());
        if (pInvType == MapleInventoryType.UNDEFINED || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final MapleInventory pInv = c.getPlayer().getInventory(pInvType); //Mode should correspond with MapleInventoryType
        boolean sorted = false;

        while (!sorted) {
            final byte freeSlot = (byte) pInv.getNextFreeSlot();
            if (freeSlot != -1) {
                byte itemSlot = -1;
                for (byte i = (byte) (freeSlot + 1); i <= pInv.getSlotLimit(); i++) {
                    if (pInv.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(c, pInvType, itemSlot, freeSlot);
                } else {
                    sorted = true;
                }
            } else {
                sorted = true;
            }
        }
        c.getSession().write(MaplePacketCreator.finishedSort(pInvType.getType()));
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void ItemGather(final LittleEndianAccessor slea, final MapleClient c) {
        // [41 00] [E5 1D 55 00] [01]
        // [32 00] [01] [01] // Sent after
        /*c.getPlayer().updateTick(slea.readInt());
         c.getPlayer().setScrolledPosition((short) 0);
         if (c.getPlayer().hasBlockedInventory()) {
         c.getSession().write(MaplePacketCreator.enableActions());
         return;
         }
         final byte mode = slea.readByte();
         final MapleInventoryType invType = MapleInventoryType.getByType(mode);
         MapleInventory Inv = c.getPlayer().getInventory(invType);

         MaplePet pet1 = c.getPlayer().getPet(0);
         MaplePet pet2 = c.getPlayer().getPet(1);
         MaplePet pet3 = c.getPlayer().getPet(2);
         if (mode == 5 && (pet1 != null || pet2 != null || pet3 != null)) { //임시방편
         c.getSession().write(MaplePacketCreator.serverNotice(1, "모든 펫을 장착 해제후 시도해 주세요"));
         c.getSession().write(MaplePacketCreator.enableActions());
         return;
         }
         final List<Item> itemMap = new LinkedList<Item>();
         for (Item item : Inv.list()) {
         itemMap.add(item.copy()); // clone all  items T___T.
         }
         for (Item itemStats : itemMap) {
         MapleInventoryManipulator.removeFromSlot(c, invType, itemStats.getPosition(), itemStats.getQuantity(), true, false);
         }

         final List<Item> sortedItems = sortItems(itemMap);
         for (Item item : sortedItems) {
         //MapleInventoryManipulator.addFromDrop(c, item, false);
         MapleInventoryManipulator.addbyItem(c, item, false);
         }
         c.getSession().write(MaplePacketCreator.finishedGather(mode));
         c.getSession().write(MaplePacketCreator.enableActions());
         itemMap.clear();
         sortedItems.clear();*/
        // [41 00] [E5 1D 55 00] [01]
        // [32 00] [01] [01] // Sent after
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        if (c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final byte mode = slea.readByte();
        final MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory Inv = c.getPlayer().getInventory(invType);
        MaplePet pet1 = c.getPlayer().getPet(0);
        MaplePet pet2 = c.getPlayer().getPet(1);
        MaplePet pet3 = c.getPlayer().getPet(2);
        if (mode == 5 && (pet1 != null || pet2 != null || pet3 != null)) { //임시방편
            c.getSession().write(MaplePacketCreator.serverNotice(1, "모든 펫을 장착 해제후 시도해 주세요"));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final List<Item> itemMap = new LinkedList<Item>();
        for (Item item : Inv.list()) {
            itemMap.add(item.copy()); // clone all  items T___T.
        }
        for (Item itemStats : itemMap) {
            MapleInventoryManipulator.removeFromSlot(c, invType, itemStats.getPosition(), itemStats.getQuantity(), true, false);
        }

        final List<Item> sortedItems = sortItems(itemMap);
        for (Item item : sortedItems) {
            MapleInventoryManipulator.addbyItem(c, item);
        }
        c.getSession().write(MaplePacketCreator.finishedGather(mode));
        c.getSession().write(MaplePacketCreator.enableActions());
        itemMap.clear();
        sortedItems.clear();
    }

    private static boolean suc = false;

    public static final void UseGoldHammer(final LittleEndianAccessor slea, final MapleClient c) {
        suc = false;
        boolean used = false;

        final byte slot = (byte) slea.readShort();
        slea.skip(2);
        final int itemId = slea.readInt();
        slea.readInt(); // Inventory type, Hammered eq is always EQ.
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        final Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (item != null) {
            if (GameConstants.canHammer(item.getItemId()) && MapleItemInformationProvider.getInstance().getSlots(item.getItemId()) > 0 && item.getViciousHammer() < 2) {
                switch (itemId) {
                    case 2470000:
                        item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                        used = true;
                        suc = true;
                        break;
                    case 2470001:
                        if (Randomizer.nextInt(100) < 50) {
                            item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                            item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                            used = true;
                            suc = true;
                        } else {
                            item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                            c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                            used = true;
                            suc = false;
                        }
                        break;
                }
                c.getSession().write(MaplePacketCreator.ViciousHammer(true, suc));
            } else {
                c.getPlayer().dropMessage(5, "황금망치를 사용할 수 없는 아이템 입니다.");
            }
        }
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, true);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void UsedGoldHammer(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(8);
        c.getSession().write(MaplePacketCreator.ViciousHammer(false, suc));
        if (!suc) {
            c.getPlayer().dropMessage(1, "황금망치로 제련에 실패하였습니다.");
        }
    }

    private static final List<Item> sortItems(final List<Item> passedMap) {
        final List<Integer> itemIds = new ArrayList<Integer>(); // empty list.
        for (Item item : passedMap) {
            itemIds.add(item.getItemId()); // adds all item ids to the empty list to be sorted.
        }
        Collections.sort(itemIds); // sorts item ids

        final List<Item> sortedList = new LinkedList<Item>(); // ordered list pl0x <3.

        for (Integer val : itemIds) {
            for (Item item : passedMap) {
                if (val == item.getItemId()) { // Goes through every index and finds the first value that matches
                    sortedList.add(item);
                    passedMap.remove(item);
                    break;
                }
            }
        }
        return sortedList;
    }

    public static final boolean UseRewardItem(final byte slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        c.getSession().write(MaplePacketCreator.enableActions());
        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.hasBlockedInventory()) {
            if (chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1) {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);
                if (rewards != null && rewards.getLeft() > 0) {
                    while (true) {
                        for (StructRewardItem reward : rewards.getRight()) {
                            if (reward.prob > 0 && Randomizer.nextInt(rewards.getLeft()) < reward.prob) { // Total prob
                                if (GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                                    final Item item = ii.getEquipById(reward.itemid);
                                    if (reward.period > 0) {
                                        item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                                    } else if (item.getItemId() == 1122017 && reward.period <= 0) { //정령의 펜던트
                                        item.setExpiration(System.currentTimeMillis() + (7200 * 60 * 60 * 10));
                                    }
                                    item.setGMLog("Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                    MapleInventoryManipulator.addbyItem(c, item);
                                } else {
                                    Item item = new Item(reward.itemid, (byte) 0, (short) reward.quantity, (byte) 0);
                                    if (reward.period > 0) {
                                        Item itemz;
                                        Item itemz2;
                                        itemz = new client.inventory.Item(reward.itemid, (byte) 0, reward.quantity, (byte) 0);
                                        itemz.setExpiration(System.currentTimeMillis() + reward.period * 1000 * 60);
                                        itemz2 = itemz.copy();
                                        MapleInventoryManipulator.addbyItem(c, itemz2);
                                    } else {
                                        MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity, "Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                    }
                                }
                                c.getSession().write(MaplePacketCreator.getShowItemGain(reward.itemid, reward.quantity, true));
                                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);
                                c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
                                if (reward.worldmsg != null) {
                                    World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, reward.itemid, chr.getName() + "님이 " + MapleItemInformationProvider.getInstance().getName(itemId) + "에서 " + MapleItemInformationProvider.getInstance().getName(reward.itemid) + "(을)를 얻었습니다."));
                                }
                                c.getSession().write(MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect));
                                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(reward.itemid, reward.effect, chr.getId()), false);
                                return true;
                            }
                        }
                    }
                } else {
                    chr.dropMessage(6, "Unknown error.");
                }
            } else {
                chr.dropMessage(6, "아이템창이 부족한건 아닌지 확인해주세요.");
            }
        }
        return false;
    }

    private volatile byte pendingOneClickCubeSlot = -1;

    public static final void UseItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMapId() == 749040100 || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();

        boolean oneCube = false;
        switch (itemId) {
            case 2000024:
            case 2000025:
            case 2000026:
            case 2000027:
                oneCube = true;
                break;
        }
        final long time = System.currentTimeMillis();
        if (!oneCube && chr.getMap().getConsumeItemCoolTime() <= 0 && chr.getNextConsume() > time) {
            chr.dropMessage(5, "아직 아이템을 사용할 수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        if (slot < 0 || slot > c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getSlotLimit() || itemId > 3000000) {
            c.getPlayer().dropMessage(1, "오류가 발생하였습니다. : - 7");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        switch (itemId) {
            case 2022035: //펩시콜라
                c.getPlayer().giveHolySymbol(1800000);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            case 2022337: //마법제련술사의 약
                chr.addHP(-chr.getStat().getMaxHp());
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            case 2009999: //스페셜 뷰티 쿠폰
                NPCScriptInvoker.runNpc(c, 9010000, 0);
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            case 2000024:
            case 2000025:
            case 2000026:
            case 2000027: //원클릭큐브
                if (chr.cubeitemid < 0) { // 최초 장비 셋팅

                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getPlayer().getClient().removeClickedNPC();
                    NPCScriptManager.getInstance().start(c, 9000002, "miracle");

                    return;
                }
                if (chr.cubeitemid == 0) { // 변경 장비 셋팅

                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getPlayer().getClient().removeClickedNPC();
                    NPCScriptManager.getInstance().start(c, 9000002, "miracle");
                    return;
                }

                boolean use = false;
                int cubeid = 0;

                if (itemId == 2000024) { // [원클릭] 미라클 큐브
                    cubeid = 5062000;
                } else if (itemId == 2000025) { // [원클릭] 마스터 미라클 큐브
                    cubeid = 5062002;
                } else if (itemId == 2000026) { // [원클릭] 레드 큐브
                    cubeid = 5062100;
                } else if (itemId == 2000027) {
                }

                if (cubeid <= 0) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                final Item checkItem = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) 1);
                if (checkItem == null) {
                    c.getPlayer().dropMessage(5, "설정된 아이템이 없어 재설정할 수 없습니다.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                if (chr.cubeitemid > 1 && chr.cubeitemid != checkItem.getItemId()) {
                    c.getPlayer().dropMessage(5, "설정된 아이템이 변경되어 재설정할 수 없습니다. 채널을 변경하셔서 아이템을 새로이 등록해 주세요.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                final Equip checkEq = (Equip) checkItem;

                if (checkEq.getState() >= 5) {
                    final MapleCharacter p = c.getPlayer();

                    // confirm에서 '예' 눌러서 1회만 감지 무시하는 상태면 스킵
                    if (p.oneClickCubeIgnoreBreakOnce) {
                        p.oneClickCubeIgnoreBreakOnce = false;
                    } else {
                        final String reason = p.getOneClickCubeBreakReason(checkEq, itemId);
                        if (reason != null) {
                            p.setPendingOneClickCube(itemId, reason, slot);
                            NPCScriptManager.getInstance().start(c, 9000002, "miracle_confirm");
                            c.getSession().write(MaplePacketCreator.enableActions());
                            return;
                        }
                    }
                }

                use = MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false);

                if (use && cubeid > 0) {
                    final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) 1);
                    if (item == null) {
                        c.getPlayer().dropMessage(5, "설정된 아이템이 없어 재설정할 수 없습니다.");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }

                    if (chr.cubeitemid > 1 && chr.cubeitemid != item.getItemId()) { //설정된 값이 안맞으면 
                        c.getPlayer().dropMessage(5, "설정된 아이템이 변경되어 재설정할 수 없습니다. 채널을 변경하셔서 아이템을 새로이 등록해 주세요.");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        final Equip eq = (Equip) item;
                        if (chr.cubeitemid == 1) { // 확인 값 1로 변경시
                            chr.cubeitemid = eq.getItemId();
                        }
                        int piece = 0;
                        if (eq.getState() >= 5) {
                            final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());

                            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                            final Map<String, Integer> eqstats = ii.getEquipStats(eq.getItemId());
                            if (!eqstats.containsKey("tuc") || eqstats.containsKey("tuc") && eqstats.get("tuc") == 0) {
                                c.getPlayer().dropMessage(5, "설정된 아이템은 업그레이드 횟수가 없어 재설정할 수 없습니다.");
                                chr.cubeitemid = 0; // 재설정으로 보냄
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                            /*
                                    
                             eqstats.containsKey("tuc")
                             */
                            final int reqLevel = (ii.getReqLevel(eq.getItemId()) < 10 ? 10 : ii.getReqLevel(eq.getItemId()) / 10);
                            final int reqMeso = (reqLevel > 12 ? 128000 : reqLevel > 7 ? 32000 : reqLevel > 3 ? 8000 : 2000);
                            if (c.getPlayer().getMeso() > reqMeso) {
                                int rank = 0;
                                int prop = 0;
                                if (cubeid == 5062000) { // 미라클 큐브 등급 업 확률
                                    prop = 0; // 야 이거 2퍼가 아니라 0.2퍼네 일단 기본값 / 1당 0.1%
                                    if (eq.getState() == 6) {
                                        prop = 0;  // 에픽 -> 유니크
                                    } else if (eq.getState() == 5) {
                                        prop = 10;  // 레어 -> 에픽
                                    }
                                    if ((eq.getPotential1() > 30000 && eq.getPotential1() < 40000)) {
                                        c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                                        c.getSession().write(MaplePacketCreator.enableActions());
                                        return;
                                    }
                                    piece = 2430112; // 미라클 큐브 조각
                                }
                                if (cubeid == 5062002) { // 마스터 미라클 큐브 등급 업 확률
                                    prop = 0; //
                                    if (eq.getState() == 6) {
                                        prop = 10;  // 에픽 -> 유니크
                                    } else if (eq.getState() == 5) {
                                        prop = 30;  // 레어 -> 에픽
                                    }
                                    piece = 2430481; // 마스터 미라클 큐브 조각
                                }
                                if (cubeid == 5062100) { // 레드 큐브 등급 업 확률
                                    prop = 0; // 3% 기본값
                                    if (eq.getState() == 6) {
                                        prop = 30;  // 에픽 -> 유니크
                                    } else if (eq.getState() == 5) {
                                        prop = 50;  // 레어 -> 에픽
                                    }
                                    piece = 2431893; // 레드 큐브 조각
                                }

                                if (ServerConstants.cubeDayValue > 1.0) {
                                    prop *= ServerConstants.cubeDayValue;
                                }

                                if (eq.getState() == 7) {
                                    if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                            || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                        rank = -7; // 유니크
                                    } else {
                                        rank = Randomizer.nextInt(1000) < prop ? -8 : -7; // 유니크 > 레전드리
                                    }
                                } else if (eq.getState() == 6) {
                                    rank = Randomizer.nextInt(1000) < prop ? -7 : -6; // 에픽 > 유니크
                                } else if (eq.getState() == 5) {
                                    rank = Randomizer.nextInt(1000) < prop ? -6 : -5; // 레어 > 에픽
                                }
//                            c.getPlayer().dropMessage(5, "rank  : " + rank + " | " + eq.getItemId() + " | " + eq.getPosition());

                                int new_state = Math.abs(rank);

                                if (new_state > 12 || new_state < 5) { //보정
                                    new_state = 5;
                                }

                                StructPotentialItem pot2 = pots.get(90).get(reqLevel);
//                            c.getPlayer().dropMessage(5, "통과 : " + pot2.potentialID);
                                final int lines = (eq.getPotential3() != 0 ? 3 : 2);
                                for (int i = 0; i < lines; i++) {
                                    boolean rewarded = false;
                                    while (!rewarded) { // Randomizer.nextInt(pots.size()) >> 이게 레어가 나오면 ㄱㄱ
                                        int a = Randomizer.nextInt(pots.size());
                                        StructPotentialItem pot = pots.get(a).get(reqLevel);

                                        if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eq.getItemId(), pot.potentialID) && GameConstants.potentialIDFits(pot.potentialID, new_state, i)) {
                                            if (pot.boss && pot.incDAMr > 0) { //보공일 때
                                                double per = 30d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨 새로 옵션 뽑음
                                            } else if (pot.ignoreTargetDEF > 0) { //방무일 때
                                                double per = 50d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                            } else if (pot.incRewardProp > 0) { //아이템 드롭률 증가일 때
                                                double per = 10d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨 새로 옵션 뽑음
                                            } else if (pot.incMesoProp > 0) { //메소 획득량 증가일 때
                                                double per = 10d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                            } else {
                                                switch (pot.potentialID) {
                                                    case 30040: //쓸어블
                                                    case 30400: //쓸컴뱃
                                                    case 30401: //쓸윈부
                                                    case 31003: //쓸샾
                                                    {
                                                        double per = 15d; //확률
                                                        double secondRandom = Math.random() * 100;
                                                        if (secondRandom > per) {
                                                            continue;
                                                        } //미당첨
                                                        break;
                                                    }
                                                    case 20051: //공격력 %
                                                    case 30051:
                                                    case 30023:
                                                    case 20052: //마력 %
                                                    case 30052:
                                                    case 30024: {
                                                        double per = 35d; //확률
                                                        double secondRandom = Math.random() * 100;
                                                        if (secondRandom > per) {
                                                            continue;
                                                        } //미당첨
                                                        break;
                                                    }
                                                    //여기서부터
//                                                case 30041:
//                                                case 30042:
//                                                case 30043:
//                                                case 30044: {
//                                                //여기까지 힘,덱,인,럭 유니크 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
//                                                //여기서부터
//                                                case 30015:
//                                                case 30016:
//                                                case 30017:
//                                                case 30018: {
//                                                //여기까지 힘,덱,인,럭 레전드리 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
                                                }
                                            }

                                            if (i == 0) {
                                                eq.setPotential1(pot.potentialID);
                                            } else if (i == 1) {
                                                eq.setPotential2(pot.potentialID);
                                            } else if (i == 2) {
                                                eq.setPotential3(pot.potentialID);
                                            }
                                            rewarded = true;
//                                        if (pot.incPADr > 0 || pot.incMADr > 0) {
//                                            System.err.println((i + 1) + "번째 옵션 코드 : " + pot.potentialID);
//                                        }
                                            if (i == lines) {
                                                break;
                                            }
//                                        System.out.print(a + " : 큐브 옵션\r\n");
//                                        c.getPlayer().dropMessage(5, "a : " + a);
//                                        c.getPlayer().dropMessage(5, "pot.potentialID : " + pot.potentialID);
                                        }
                                    }
                                }
                                c.getPlayer().gainMeso(-(reqMeso), true, true);
                            } else {
                                c.getPlayer().dropMessage(1, "'" + ii.getName(eq.getItemId()) + "'(을)를 감정하기 위해서는 " + reqMeso + "메소가 필요합니다.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                            c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1), true);
                            c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                            if (piece > 0) {
                                MapleInventoryManipulator.addById(c, piece, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                            }
                            chr.setNextConsume(time + (1 * 1000)); // 쿨타임
                        } else {
                            c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재 능력 재설정을 실패하였습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "연결된 큐브가 부족하여 잠재 능력 재설정을 실패하였습니다.");
                }
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
        }

        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleStatEffect specEx = MapleItemInformationProvider.getInstance().getItemEffectEX(toUse.getItemId());
                if (specEx != null) {
                    specEx.applyTo(chr);
                }
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }
            //
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static void processOneClickCubeFromScript(final MapleClient c, final MapleCharacter chr, final int itemId, final byte slot) {
        if (c == null || chr == null) {
            return;
        }

        final long time = System.currentTimeMillis();
        // 아래 로직은 UseItem()의 case 2000024~2000027 블록과 동일하게 유지해야 합니다.
        // 핵심은: itemId(2000024~2000027) -> cubeid(5062xxx) 매핑 후,
        // EQUIP 1번 아이템에 큐브 적용을 1회 실행시키는 것입니다.

        // ✅ 1) cubeid 매핑 (UseItem과 동일)
        int cubeid = 0;
        if (itemId == 2000024) {
            cubeid = 5062000;
        } else if (itemId == 2000025) {
            cubeid = 5062002;
        } else if (itemId == 2000026) {
            cubeid = 5062100;
        } else if (itemId == 2000027) {
            // 필요 시 여기도 지정
            // cubeid = ????;
        }

        if (cubeid <= 0) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        // ✅ 2) 원본 로직과 동일한 체크
        if (chr.cubeitemid < 0 || chr.cubeitemid == 0) {
            c.getSession().write(MaplePacketCreator.enableActions());
            c.getPlayer().getClient().removeClickedNPC();
            NPCScriptManager.getInstance().start(c, 9000002, "miracle");
            return;
        }

        final Item checkItem = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) 1);
        if (checkItem == null) {
            c.getPlayer().dropMessage(5, "설정된 아이템이 없어 재설정할 수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.cubeitemid > 1 && chr.cubeitemid != checkItem.getItemId()) {
            c.getPlayer().dropMessage(5, "설정된 아이템이 변경되어 재설정할 수 없습니다. 채널을 변경하셔서 아이템을 새로이 등록해 주세요.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        // ✅ 3) cube 소모 및 실제 큐브 실행
        boolean use = MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, cubeid, 1, false, false);
        if (!use) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        // ✅ 4) 이후의 “잠재 재설정 실제 적용” 부분은
        // UseItem()의 원클릭큐브 블록에서
        //    if (use && cubeid > 0) { ... }
        // 이 안쪽 로직을 그대로 가져오셔야 100% 동일 동작합니다.
        //
        // ⚠️ 여기부터는 서버마다 커스텀 분기/이펙트/브로드캐스트가 섞여있을 수 있어서,
        //    제가 지금 답변에 100줄 넘게 그대로 붙이면 오히려 복붙 실수 위험이 큽니다.
        //
        // 따라서:
        //  - InventoryHandler.java에서
        //    "if (use && cubeid > 0) {" (원클릭큐브 블록 내부)
        //    부터 그 블록의 끝까지를
        //    이 함수 안으로 그대로 복사해 넣어주세요.
        //
        // ✅ 복사 앵커:
        //   UseItem() -> case 2000024~2000027 -> "if (use && cubeid > 0) {"
        //
        // ✅ 붙여넣기 앵커:
        //   바로 이 주석 아래.
        if (use && cubeid > 0) {
            final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) 1);
            if (item == null) {
                c.getPlayer().dropMessage(5, "설정된 아이템이 없어 재설정할 수 없습니다.");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }

            if (chr.cubeitemid > 1 && chr.cubeitemid != item.getItemId()) { //설정된 값이 안맞으면 
                c.getPlayer().dropMessage(5, "설정된 아이템이 변경되어 재설정할 수 없습니다. 채널을 변경하셔서 아이템을 새로이 등록해 주세요.");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                final Equip eq = (Equip) item;
                if (chr.cubeitemid == 1) { // 확인 값 1로 변경시
                    chr.cubeitemid = eq.getItemId();
                }
                int piece = 0;
                if (eq.getState() >= 5) {
                    final List<List<StructPotentialItem>> pots = new LinkedList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().values());

                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    final Map<String, Integer> eqstats = ii.getEquipStats(eq.getItemId());
                    if (!eqstats.containsKey("tuc") || eqstats.containsKey("tuc") && eqstats.get("tuc") == 0) {
                        c.getPlayer().dropMessage(5, "설정된 아이템은 업그레이드 횟수가 없어 재설정할 수 없습니다.");
                        chr.cubeitemid = 0; // 재설정으로 보냄
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    /*
                                    
                     eqstats.containsKey("tuc")
                     */
                    final int reqLevel = (ii.getReqLevel(eq.getItemId()) < 10 ? 10 : ii.getReqLevel(eq.getItemId()) / 10);
                    final int reqMeso = (reqLevel > 12 ? 128000 : reqLevel > 7 ? 32000 : reqLevel > 3 ? 8000 : 2000);
                    if (c.getPlayer().getMeso() > reqMeso) {
                        int rank = 0;
                        int prop = 0;
                        if (cubeid == 5062000) { // 미라클 큐브 등급 업 확률
                            prop = 0; // 야 이거 2퍼가 아니라 0.2퍼네 일단 기본값 / 1당 0.1%
                            if (eq.getState() == 6) {
                                prop = 0;  // 에픽 -> 유니크
                            } else if (eq.getState() == 5) {
                                prop = 10;  // 레어 -> 에픽
                            }
                            if ((eq.getPotential1() > 30000 && eq.getPotential1() < 40000)) {
                                c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                            piece = 2430112; // 미라클 큐브 조각
                        }
                        if (cubeid == 5062002) { // 마스터 미라클 큐브 등급 업 확률
                            prop = 0; //
                            if (eq.getState() == 6) {
                                prop = 10;  // 에픽 -> 유니크
                            } else if (eq.getState() == 5) {
                                prop = 30;  // 레어 -> 에픽
                            }
                            piece = 2430481; // 마스터 미라클 큐브 조각
                        }
                        if (cubeid == 5062100) { // 레드 큐브 등급 업 확률
                            prop = 0; // 3% 기본값
                            if (eq.getState() == 6) {
                                prop = 30;  // 에픽 -> 유니크
                            } else if (eq.getState() == 5) {
                                prop = 50;  // 레어 -> 에픽
                            }
                            piece = 2431893; // 레드 큐브 조각
                        }

                        if (ServerConstants.cubeDayValue > 1.0) {
                            prop *= ServerConstants.cubeDayValue;
                        }

                        if (eq.getState() == 7) {
                            if ((eq.getPotential1() > 30000 && eq.getPotential1() < 30041)
                                    || eq.getPotential1() == 30400 || eq.getPotential1() == 30401 || eq.getPotential1() == 30402 || eq.getPotential1() == 30403) {
                                rank = -7; // 유니크
                            } else {
                                rank = Randomizer.nextInt(1000) < prop ? -8 : -7; // 유니크 > 레전드리
                            }
                        } else if (eq.getState() == 6) {
                            rank = Randomizer.nextInt(1000) < prop ? -7 : -6; // 에픽 > 유니크
                        } else if (eq.getState() == 5) {
                            rank = Randomizer.nextInt(1000) < prop ? -6 : -5; // 레어 > 에픽
                        }
//                            c.getPlayer().dropMessage(5, "rank  : " + rank + " | " + eq.getItemId() + " | " + eq.getPosition());

                        int new_state = Math.abs(rank);

                        if (new_state > 12 || new_state < 5) { //보정
                            new_state = 5;
                        }

                        StructPotentialItem pot2 = pots.get(90).get(reqLevel);
//                            c.getPlayer().dropMessage(5, "통과 : " + pot2.potentialID);
                        final int lines = (eq.getPotential3() != 0 ? 3 : 2);
                        for (int i = 0; i < lines; i++) {
                            boolean rewarded = false;
                            while (!rewarded) { // Randomizer.nextInt(pots.size()) >> 이게 레어가 나오면 ㄱㄱ
                                int a = Randomizer.nextInt(pots.size());
                                StructPotentialItem pot = pots.get(a).get(reqLevel);

                                if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eq.getItemId(), pot.potentialID) && GameConstants.potentialIDFits(pot.potentialID, new_state, i)) {
                                    if (pot.boss && pot.incDAMr > 0) { //보공일 때
                                        double per = 30d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨 새로 옵션 뽑음
                                    } else if (pot.ignoreTargetDEF > 0) { //방무일 때
                                        double per = 50d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨
                                    } else if (pot.incRewardProp > 0) { //아이템 드롭률 증가일 때
                                        double per = 10d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨 새로 옵션 뽑음
                                    } else if (pot.incMesoProp > 0) { //메소 획득량 증가일 때
                                        double per = 10d; //확률
                                        double secondRandom = Math.random() * 100;
                                        if (secondRandom > per) {
                                            continue;
                                        } //미당첨
                                    } else {
                                        switch (pot.potentialID) {
                                            case 30040: //쓸어블
                                            case 30400: //쓸컴뱃
                                            case 30401: //쓸윈부
                                            case 31003: //쓸샾
                                            {
                                                double per = 15d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                                break;
                                            }
                                            case 20051: //공격력 %
                                            case 30051:
                                            case 30023:
                                            case 20052: //마력 %
                                            case 30052:
                                            case 30024: {
                                                double per = 35d; //확률
                                                double secondRandom = Math.random() * 100;
                                                if (secondRandom > per) {
                                                    continue;
                                                } //미당첨
                                                break;
                                            }
                                            //여기서부터
//                                                case 30041:
//                                                case 30042:
//                                                case 30043:
//                                                case 30044: {
//                                                //여기까지 힘,덱,인,럭 유니크 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
//                                                //여기서부터
//                                                case 30015:
//                                                case 30016:
//                                                case 30017:
//                                                case 30018: {
//                                                //여기까지 힘,덱,인,럭 레전드리 옵션
//                                                    double per = 20d; //확률
//                                                    double secondRandom = Math.random() * 100;
//                                                    if (secondRandom > per) {
//                                                        continue;
//                                                    } //미당첨
//                                                    break;
//                                                }
                                        }
                                    }

                                    if (i == 0) {
                                        eq.setPotential1(pot.potentialID);
                                    } else if (i == 1) {
                                        eq.setPotential2(pot.potentialID);
                                    } else if (i == 2) {
                                        eq.setPotential3(pot.potentialID);
                                    }
                                    rewarded = true;
//                                        if (pot.incPADr > 0 || pot.incMADr > 0) {
//                                            System.err.println((i + 1) + "번째 옵션 코드 : " + pot.potentialID);
//                                        }
                                    if (i == lines) {
                                        break;
                                    }
//                                        System.out.print(a + " : 큐브 옵션\r\n");
//                                        c.getPlayer().dropMessage(5, "a : " + a);
//                                        c.getPlayer().dropMessage(5, "pot.potentialID : " + pot.potentialID);
                                }
                            }
                        }
                        c.getPlayer().gainMeso(-(reqMeso), true, true);
                    } else {
                        c.getPlayer().dropMessage(1, "'" + ii.getName(eq.getItemId()) + "'(을)를 감정하기 위해서는 " + reqMeso + "메소가 필요합니다.");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
                    if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1), true);
                    c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                    if (piece > 0) {
                        MapleInventoryManipulator.addById(c, piece, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                    }
                    chr.setNextConsume(time + (1 * 1000)); // 쿨타임
                } else {
                    c.getPlayer().dropMessage(5, "이 아이템의 잠재 능력은 재설정할 수 없습니다.");
                }
            } else {
                c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재 능력 재설정을 실패하였습니다.");
            }
        } else {
            c.getPlayer().dropMessage(5, "연결된 큐브가 부족하여 잠재 능력 재설정을 실패하였습니다.");
        }
        c.getSession().write(MaplePacketCreator.enableActions());
        return;
    }

    public static final void UseCosmetic(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 254 || (itemId / 1000) % 10 != chr.getGender()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }

    }

    public static final void UseReturnScroll(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.getMapId() == 749040100 || chr.hasBlockedInventory() || !chr.canUseReturnScroll()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            } else {
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void addToScrollLog(int accountID, int charID, int scrollID, int itemID, byte oldSlots, byte newSlots, byte viciousHammer, String result, boolean ws, boolean ls, int vega) {

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO scroll_log VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, accountID);
            ps.setInt(2, charID);
            ps.setInt(3, scrollID);
            ps.setInt(4, itemID);
            ps.setByte(5, oldSlots);
            ps.setByte(6, newSlots);
            ps.setByte(7, viciousHammer);
            ps.setString(8, result);
            ps.setByte(9, (byte) (ws ? 1 : 0));
            ps.setByte(10, (byte) (ls ? 1 : 0));
            ps.setInt(11, vega);
            ps.execute();
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static final void useMeterGi(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        short slot = slea.readShort();
        int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.ETC).getItem(slot);

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {
            c.getSession().write(MaplePacketCreator.openUI((byte) 35));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void meterGi(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        byte 바이트 = slea.readByte();
        if (바이트 == 1) {
            c.getSession().write(MaplePacketCreator.OnDotDamageInfo(c, 0, 0, false, 0));
            c.getSession().write(MaplePacketCreator.calcRequestResult(c, (byte) 1));
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    /*    private static final Set<Integer> BLOCK_CHAOSSCROLL_TARGET_EQUIPS = new HashSet<Integer>(Arrays.asList( //혼줌밴 리스트
     1082610,
     1082611,
     1082612,
     1082613,
     1052812,
     1052813,
     1052814,
     1052815,
     1004246,
     1004245,
     1004244,
     1004243,
     1102721,
     1102722,
     1102726,
     1102727,
     1072969,
     1072970,
     1072971,
     1072972
     )); */
    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr) {
        return UseUpgradeScroll(slot, dst, ws, c, chr, 0);
    }

    public static final boolean UseUpgradeScroll(final short slot, final short dst, final short ws, final MapleClient c, final MapleCharacter chr, final int vegas) {
        boolean whiteScroll = false; // white scroll being used?
        boolean legendarySpirit = false; // legendary spirit skill
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        chr.setScrolledPosition((short) 0);
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        Equip toScroll;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else { // legendary spirit
            legendarySpirit = true;
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        if (toScroll == null || c.getPlayer().hasBlockedInventory()) {
            return false;
        }
        final byte oldLevel = toScroll.getLevel();
        final byte oldEnhance = toScroll.getEnhance();
        final byte oldState = toScroll.getState();
        final short oldFlag = toScroll.getFlag();
        final byte oldSlots = toScroll.getUpgradeSlots();
        final byte oldVH = (byte) toScroll.getViciousHammer();
        final int itemID = toScroll.getItemId();

        Item scroll = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (scroll == null) {
            scroll = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
            if (scroll == null) {
                final int scrollId = scroll.getItemId();
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        }
        if (!GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() < 1) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isEquipScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() >= 1 || toScroll.getEnhance() >= 100 || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isPotentialScroll(scroll.getItemId())) {
            if (toScroll.getState() >= 1 || (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0 && toScroll.getItemId() / 10000 != 135) || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        } else if (GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (ii.isCash(toScroll.getItemId()) || toScroll.getEnhance() >= 8) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return false;
            }
        }
        if (!GameConstants.canScroll(toScroll.getItemId()) && !GameConstants.isChaosScroll(toScroll.getItemId())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if ((GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId()) || GameConstants.isChaosScroll(scroll.getItemId())) && (vegas > 0 || ii.isCash(toScroll.getItemId()))) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        if (GameConstants.isTablet(scroll.getItemId()) && toScroll.getDurability() < 0) { //not a durability item
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        } else if ((!GameConstants.isTablet(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId())) && toScroll.getDurability() >= 0) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        Item wscroll = null;

        if (GameConstants.isCleanSlate(scroll.getItemId())) {
            Map<String, Integer> eqstats = ii.getEquipStats(toScroll.getItemId());
            if (eqstats != null) {
                int baseTuc = eqstats.get("tuc");
                int hammer = toScroll.getViciousHammer();
                int maxSlots = baseTuc + hammer;
                int currentUsed = maxSlots - toScroll.getUpgradeSlots();

                System.out.println("백의 주문서 체크 - baseTuc: " + baseTuc + ", hammer: " + hammer
                        + ", maxSlots: " + maxSlots + ", used: " + currentUsed);

                if (currentUsed > 0) {
                    // 복구 가능
                } else {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    return false;
                }
            }
        }

        // Anti cheat and validation
        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs != null && scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }

        if (whiteScroll) {
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000);
            if (wscroll == null) {
                whiteScroll = false;
            }
        }
        if (GameConstants.isTablet(scroll.getItemId()) || GameConstants.isGeneralScroll(scroll.getItemId())) {
            switch (scroll.getItemId() % 1000 / 100) {
                case 0: //1h
                    if (GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 1: //2h
                    if (!GameConstants.isTwoHanded(toScroll.getItemId()) || !GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 2: //armor
                    if (GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
                case 3: //accessory
                    if (!GameConstants.isAccessory(toScroll.getItemId()) || GameConstants.isWeapon(toScroll.getItemId())) {
                        return false;
                    }
                    break;
            }
        } else if (!GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isChaosScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId()) && !GameConstants.isEquipScroll(scroll.getItemId()) && !GameConstants.isPotentialScroll(scroll.getItemId()) && !GameConstants.isSpecialScroll(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                return false;
            }
        }
        if (GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isAccessory(toScroll.getItemId())) {
            return false;
        }
        if (scroll.getQuantity() <= 0) {
            return false;
        }

        if (legendarySpirit && vegas == 0) {
            if (chr.getSkillLevel(SkillFactory.getSkill(chr.getStat().getSkillByJob(1003, chr.getJob()))) <= 0) {
                return false;
            }
        }

        if (scroll.getItemId() == 2041200 && toScroll.getLevel() > 0 && toScroll.getItemId() == 1122000) { //혼목 & 드래곤의 돌
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return false;
        }
        final Equip beforeSnapshot = (Equip) toScroll.copy();
        // Scroll Success/ Failure/ Curse
        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr, vegas);
        ScrollResult scrollSuccess;
        if (scrolled == null) {
            if (ItemFlag.SHIELD_WARD.check(oldFlag)) {
                scrolled = toScroll;
                scrollSuccess = Equip.ScrollResult.FAIL;
                scrolled.setFlag((short) (oldFlag - ItemFlag.SHIELD_WARD.getValue()));
            } else {
                scrollSuccess = Equip.ScrollResult.CURSE;
            }
        } else if (scrolled.getLevel() > oldLevel || scrolled.getEnhance() > oldEnhance || scrolled.getState() > oldState || scrolled.getFlag() > oldFlag) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else if ((GameConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() > oldSlots)) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else {
            scrollSuccess = Equip.ScrollResult.FAIL;
        }
        // Update
        chr.getInventory(GameConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false);
        if (whiteScroll) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        } else if (scrollSuccess == Equip.ScrollResult.FAIL && scrolled.getUpgradeSlots() < oldSlots && c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000) != null) {
            chr.setScrolledPosition(scrolled.getPosition());
            if (vegas == 0) {
                c.getSession().write(MaplePacketCreator.pamSongUI());
            }
        }

        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, toScroll, true, false));
            if (dst < 0) {
                chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else if (vegas == 0) {
            c.getSession().write(MaplePacketCreator.scrolledItem(scroll, scrolled, false, false));
        }

        //c.getSession().write(MaplePacketCreator.getScrollEffect2(c.getPlayer().getId(), scrollSuccess, legendarySpirit, whiteScroll));
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit, whiteScroll), vegas == 0);
        //주문서 이펙트 부분 1.2.6은 없음 1.2.15때 추가 근데 이패킷쏘면 위젯 문제나는데 뭔가 수동으로 추가해주면 될삘?
        //일단 idb엔 없는걸로 판정 
        //addToScrollLog(chr.getAccountID(), chr.getId(), scroll.getItemId(), itemID, oldSlots, (byte)(scrolled == null ? -1 : scrolled.getUpgradeSlots()), oldVH, scrollSuccess.name(), whiteScroll, legendarySpirit, vegas);
        // equipped item was scrolled and changed
        if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE) && vegas == 0) {
            chr.equipChanged();
        }
        if (vegas == 0 && scrollSuccess == Equip.ScrollResult.SUCCESS && chr.haveItem(2430192)) { //리턴 스크롤 아이템 코드
            final MapleInventoryType invType = (dst < 0) ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
            final Equip afterSnapshot = (Equip) scrolled.copy();

            chr.setReturnScrollData(beforeSnapshot, afterSnapshot, invType, dst);

            NPCScriptManager.getInstance().start(c, 9010000, "ReturnScroll");
        }
        return true;
    }

    public static final void UseMagnify(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4);
        c.getPlayer().setScrolledPosition((short) 0);
        final byte src = (byte) slea.readShort();
        final byte check = (byte) slea.readShort();
        //final boolean insight = src == 127 && c.getPlayer().getTrait(MapleTraitType.sense).getLevel() >= 30;
        Equip toReveal;
        final Item magnify = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(src);
        if (check < 0) {
            toReveal = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) check);
        } else {
            toReveal = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) check);
        }
        if ((magnify == null /*&& !insight*/) || toReveal == null || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
        final Equip eqq = (Equip) toReveal;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final int reqLevel = ii.getReqLevel(eqq.getItemId()) / 10;
        if (eqq.getState() == 1 && (magnify.getItemId() == 2460003 || (magnify.getItemId() == 2460002 && reqLevel <= 12) || (magnify.getItemId() == 2460001 && reqLevel <= 7) || (magnify.getItemId() == 2460000 && reqLevel <= 3))) {
            final List<List<StructPotentialItem>> pots = new LinkedList<List<StructPotentialItem>>(ii.getAllPotentialInfo().values());
            int new_state = Math.abs(eqq.getPotential1());
            if (new_state > 7 || new_state < 5) { //luls tooo legend
                new_state = 5;
            }
            final int lines = (eqq.getPotential2() != 0 ? 3 : 2);
            boolean special = false;
            if (eqq.getCubedCount() >= 10) { //스폐셜 횟수
                if (Randomizer.rand(1, 100) <= 70) {
                    special = true;
                    eqq.setCubedCount(0);//초기화
                }
            }
            while (eqq.getState() != new_state) {
                //31001 = haste, 31002 = door, 31003 = se, 31004 = hb
                for (int i = 0; i < lines; i++) { //2 or 3 line
                    boolean rewarded = false;
                    while (!rewarded) {
                        // System.out.println("pots :" + pots + " new_state :" + new_state + " lines :" + lines + " reqLevel :" + reqLevel);
                        StructPotentialItem pot = pots.get(Randomizer.nextInt(pots.size())).get(reqLevel);
                        if (pot != null && pot.reqLevel / 10 <= reqLevel && GameConstants.optionTypeFits(pot.optionType, eqq.getItemId()) && GameConstants.potentialIDFits(pot.potentialID, new_state, i)) { //optionType
                            //have to research optionType before making this truely sea-like
                            if (i == 0) {
                                if (special && !GameConstants.isWeapon(eqq.getItemId())) {
                                    eqq.setPotential1((new_state - 4) * 10000 + 41 + Randomizer.rand(0, 3));
                                } else {
                                    eqq.setPotential1(pot.potentialID);
                                }
                            } else if (i == 1) {
                                if (special && !GameConstants.isWeapon(eqq.getItemId())) {
                                    /*int header = 1;
                                     if (new_state == 5) {
                                     header = 1;
                                     } else if (new_state == 6) {
                                     header = Randomizer.rand(1, 100) <= 30 ? 2 : 1;
                                     }*/
                                    eqq.setPotential2((new_state - 4) * 10000 + 41 + Randomizer.rand(0, 3));
                                } else {
                                    eqq.setPotential2(pot.potentialID);
                                }
                            } else if (i == 2) {
                                eqq.setPotential3(pot.potentialID);
                            }
                            rewarded = true;
                        }
                    }
                }
            }
            //c.getPlayer().getTrait(MapleTraitType.insight).addExp((insight ? 10 : ((magnify.getItemId() + 2) - 2460000)) * 2, c.getPlayer());
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getPotentialReset(c.getPlayer().getId(), eqq.getPosition()));
            c.getSession().write(MaplePacketCreator.scrolledItem(magnify, toReveal, false, true));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, magnify.getPosition(), (short) 1, false);
        } else {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            return;
        }
    }

    public static final boolean UseSkillBook(final byte slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || chr.hasBlockedInventory()) {
            return false;
        }
        final Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getEquipStats(toUse.getItemId());
        if (skilldata == null) { // Hacking or used an unknown item
            return false;
        }
        boolean canuse = false, success = false;
        int skill = 0, maxlevel = 0;

        final Integer SuccessRate = skilldata.get("success");
        final Integer ReqSkillLevel = skilldata.get("reqSkillLevel");
        final Integer MasterLevel = skilldata.get("masterLevel");

        byte i = 0;
        Integer CurrentLoopedSkillId;
        while (true) {
            CurrentLoopedSkillId = skilldata.get("skillid" + i);
            i++;
            if (CurrentLoopedSkillId == null || MasterLevel == null) {
                //System.out.println("Break1");
                break; // End of data
            }
            final Skill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId);
            if (CurrSkillData != null && CurrSkillData.canBeLearnedBy(chr.getJob()) && (ReqSkillLevel == null || chr.getSkillLevel(CurrSkillData) >= ReqSkillLevel) && chr.getMasterLevel(CurrSkillData) < MasterLevel) {
                canuse = true;
                if (SuccessRate == null || Randomizer.nextInt(100) <= SuccessRate) {
                    success = true;
                    chr.changeSkillLevel(CurrSkillData, chr.getSkillLevel(CurrSkillData), (byte) (int) MasterLevel);
                } else {
                    //System.out.println("Break2");
                    success = false;
                }
                MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemId), slot, (short) 1, false);
                break;
            }
        }
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.useSkillBook(chr, skill, maxlevel, canuse, success));
        c.getSession().write(MaplePacketCreator.enableActions());
        return canuse;
    }

    public static final void UseCatchItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMap map = chr.getMap();

        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null && !chr.hasBlockedInventory() && itemid / 10000 == 227 && MapleItemInformationProvider.getInstance().getCardMobId(itemid) == mob.getId()) {
            final ItemInformation i = MapleItemInformationProvider.getInstance().getItemInformation(itemid);
            int mobhp = i.flag & 0x1000;
            if (mobhp != 0 && mob.getHp() <= mob.getMobMaxHp() * mobhp / 10000 || mobhp == 0 && mob.getHp() <= mob.getMobMaxHp() / 2) {
                //c.getSession().write(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 1));
                map.broadcastMessage(MaplePacketCreator.showMagnet(mob.getObjectId(), (byte) 1));
                map.killMonster(mob, chr, true, false, (byte) 1);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
                if (MapleItemInformationProvider.getInstance().getCreateId(itemid) > 0) {
                    MapleInventoryManipulator.addById(c, MapleItemInformationProvider.getInstance().getCreateId(itemid), (short) 1, "Catch item " + itemid + " on " + FileoutputUtil.CurrentReadable_Date());
                }
            } else {
                //map.broadcastMessage(MaplePacketCreator.showMagnet(mob.getObjectId(), (byte) 0));
                map.broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), itemid, (byte) 0));
                c.getSession().write(MaplePacketCreator.catchMob(mob.getId(), itemid, (byte) 0));
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void UseMountFood(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        //90 DD 35 00 25 00 20 7C 22 00
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt(); //2260000 usually
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMount mount = chr.getMount();

        if (itemid / 10000 == 226 && toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mount != null && !c.getPlayer().hasBlockedInventory()) {
            final int fatigue = mount.getFatigue();

            boolean levelup = false;
            mount.setFatigue((byte) -30);

            if (fatigue > 0) {
                mount.increaseExp();
                final int level = mount.getLevel();
                if (level < 30 && mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1)) {
                    mount.setLevel((byte) (level + 1));
                    levelup = true;
                }
            }
            chr.getMap().broadcastMessage(MaplePacketCreator.updateMount(chr, levelup));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static void UseScriptedNPCItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        short slot = slea.readShort();
        int itemId = slea.readInt();
        long expiration_days = 0;
        int mountid = 0;
        switch (itemId) {
            case 2430014: {
                c.getPlayer().openNpc(1300010);
                break;
            }
            case 2430015: {
                if (c.getPlayer().getMapId() != 106020500) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MaplePacketCreator.getNPCTalk(1300011, (byte) 0, "이곳에선 사용할 수 없습니다.", "00 00", (byte) 3));
                    return;
                } else if (c.getPlayer().getPosition().x < 180) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(MaplePacketCreator.getNPCTalk(1300011, (byte) 0, "조금 더 가까이 가서 사용하자.", "00 00", (byte) 3));
                    return;
                }
                c.getPlayer().openNpc(1300011);
                break;
            }
            case 2430016: {
                if (!c.getPlayer().canHoldSlots(1)) {
                    c.getPlayer().dropMessage(1, "각 아이템창의 슬롯을 2칸 이상 비워주세요.");
                    break;
                }
                int prop = Randomizer.rand(1, 100);
                int prop2 = Randomizer.rand(1, 100);
                if (prop <= 10) {//아이스바
                    c.getPlayer().gainItem(1012070 + Randomizer.rand(0, 3), (short) 1, false, prop2 >= 70 ? 7 : 3, 0, null, c);
                } else if (prop <= 20) {//서핑보드
                    List<Pair<Integer, Short>> items = new ArrayList<>();
                    int[] id = {1442011, 1442026, 1442027, 1442028, 1442029, 1442054, 1442055, 1442056, 1442057}; // 아이템코드
                    int[] q = {1, 2, 5}; // 갯수
                    for (int i = 0; i < id.length; i++) {
                        items.add(new Pair<>(id[i], (short) 1));
                    }
                    byte num = (byte) Randomizer.rand(0, items.size() - 1);
                    chr.gainItem(items.get(num).getLeft(), items.get(num).getRight(), true);
                } else if (prop <= 30) { //아이스피크, 튜브, 라이딩
                    if (prop2 <= 50) {//피크
                        c.getPlayer().gainItem(4310007, (short) Randomizer.rand(1, 10), false, 0, 0, null, c);
                    } else if (prop2 <= 70) {//꽃무늬 튜브
                        Item itemz = new client.inventory.Item(4001320, (byte) 0, (short) 1, (byte) 0);
                        itemz.setUniqueId(MapleInventoryManipulator.getUniqueId(itemz.getItemId(), null));
                        itemz.setFlag((short) (ItemFlag.KARMA_USE.getValue()));
                        MapleInventoryManipulator.addbyItem(c, itemz);
                    } else if (prop2 <= 100) {//라이딩 쿠폰
                        c.getPlayer().gainItem(2430082, (short) 1, false, 3, 0, null, c);
                    }
                } else if (prop <= 100) { //물약
                    int itemid = 2001500 + Randomizer.rand(0, 28);
                    if (itemid >= 2001515 && itemid <= 2001525) {
                        itemid -= 11;
                    }
                    c.getPlayer().gainItem(itemid, (short) (prop2 <= 10 ? 15 : prop2 <= 60 ? 10 : 5), true);//크리스마스 요정
                }
                c.getPlayer().modifyCSPoints(1, 200, true);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                break;
            }
            case 2430008:
            case 2430030: {
                if (chr.getMapId() >= 390000000 && chr.getMapId() <= 390009999) {
                    c.getPlayer().dropMessage(-1, "이곳에서는 사용하실 수 없습니다.");
                } else {
                    NPCScriptManager.getInstance().start(c, 9310021);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                }
                break;
            }
            case 2430071: {
                if (!c.getPlayer().canHold(4032616, 1)) {
                    c.getPlayer().dropMessage(1, "기타 인벤토리 공간이 부족합니다.");
                    break;
                }
                if (c.getPlayer().haveItem(4032616, 1)) {
                    c.getPlayer().dropMessage(1, "이미 혜안을 소지하고 있습니다.");
                    break;
                }
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getSession().write(UIPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/itemEffect/quest/2430071"));
                if ((int) (Math.random() * 5) == 0) {
                    chr.dropMessage(5, "탁한 유리구슬이 부서졌습니다. 혜안을 얻었습니다.");
                    c.getPlayer().gainItem(4032616, (short) 1, true);
                } else {
                    chr.dropMessage(5, "탁한 유리구슬이 부서졌습니다. 아무것도 나오지 않았습니다.");
                }
                break;
            }
            case 2430131: {
                c.getPlayer().openNpc(9000523);
                break;
            }
            case 2430143: {//정체불명의 러브레터
                int prop = Randomizer.rand(0, 100);
                int count = 0;
                if (prop <= 20) {
                    count = 1;
                } else if (prop <= 40) {
                    count = 2;
                } else if (prop <= 60) {
                    count = 3;
                } else if (prop <= 80) {
                    count = 4;
                } else {
                    count = 5;
                }
                c.getSession().write(MaplePacketCreator.getShowFameGain(count));
                c.getPlayer().addFame(count);
                c.getPlayer().updateSingleStat(MapleStat.FAME, c.getPlayer().getFame());
                c.getPlayer().dropMessage(5, "편지 속에 담긴 사랑의 힘으로 인기도가 올라갔습니다. 하지만 누가 보낸 편지인지 알 수 없군요.");
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                break;
            }
            case 2430159: {
                if (c.getPlayer().getMapId() == 211060400) {
                    c.getPlayer().openNpc(2160000);
                } else {
                    c.getPlayer().dropMessage(5, "이곳에선 사용할 수 없다. 머트가 있는 곳으로 가자.");
                }
                break;
            }
            case 2430180: {
                if (c.getPlayer().getMapId() == 211041400) {
                    if (c.getPlayer().getQuestStatus(3192) == 1) {
                        MapleQuest.getInstance(3192).forceStart(c.getPlayer(), 0, "1");
                    }
                    c.getPlayer().dropMessage(5, "결계의 토템이 밝은 빛을 내며 화로가 타기 시작했고, 토템에 새겨진 알케스터의 결계 마법이 발동합니다.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (byte) -1, true));
                } else {
                    c.getPlayer().dropMessage(5, "이곳에선 사용할 수 없다.");
                }
                break;
            }
            case 2430033: {
                if (!c.getPlayer().canHoldSlots(1)) {
                    c.getPlayer().dropMessage(1, "각 아이템창의 슬롯을 2칸 이상 비워주세요.");
                    break;
                }
                int prop = Randomizer.nextInt(100);
                int prop2 = Randomizer.nextInt(100);
                if (prop <= 40) {
                    if (prop2 <= 33) {
                        c.getPlayer().gainItem(4032528, (short) 1, true);//크리스마스 요정
                        // c.getPlayer().gainItem(2020033, (short) (Math.random() * 5), true);//진저맨 쿠키
                    } else if (prop2 <= 66) {
                        c.getPlayer().gainItem(4032528, (short) 1, true);//크리스마스 요정
                        // c.getPlayer().gainItem(2020034, (short) (Math.random() * 5), true);//진저레이디 쿠키
                    } else {
                        //        c.getPlayer().gainItem(2020035, (short) (Math.random() * 5), true);//막대사탕
                        c.getPlayer().gainItem(4032528, (short) 1, true);//크리스마스 요정
                    }
                } else if (prop <= 50) {
                    if (prop2 <= 33) {
                        c.getPlayer().gainItem(4032528, (short) 1, true);//크리스마스 요정
                    } else if (prop2 <= 66) {
                        c.getPlayer().gainItem(4031063, (short) 1, true);//토르의 뿔
                    } else {
                        c.getPlayer().gainItem(4032528, (short) 1, true);//크리스마스 요정
                    }
                } else if (prop <= 60) { //원석류 집어넣기
                    c.getPlayer().gainItem(4010000 + Randomizer.rand(0, 7), (short) Randomizer.rand(1, 10), true);//크리스마스 요정
                } else {
                    c.getPlayer().dropMessage(-1, "꽝! 빈 상자입니다.");
                }
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                break;
            }

            case 2430191: //프로텍트 실드
                break;

            case 2430036: //croco 1 day
                mountid = 1027;
                expiration_days = 1;
                break;
            case 2430037: //black scooter 1 day
                mountid = 1028;
                expiration_days = 1;
                break;
            case 2430038:  //pink scooter 1 day
                mountid = 1029;
                expiration_days = 1;
                break;
            case 2430039: //clouds 1 day
                mountid = 1030;
                expiration_days = 1;
                break;
            case 2430040: //balrog 1 day
                mountid = 1031;
                expiration_days = 1;
                break;
            case 2430050: //balrog random
                mountid = 1031;
                expiration_days = Randomizer.rand(1, 7);
                break;
            case 2430053: //croco 30 day
                mountid = 1027;
                expiration_days = 1;
                break;
            case 2430054: //black scooter 30 day
                mountid = 1028;
                expiration_days = 30;
                break;
            case 2430055: //pink scooter 30 day
                mountid = 1029;
                expiration_days = 30;
                break;
            case 2430056: //mist rog 30 day
                mountid = 1035;
                expiration_days = 30;
                break;
            case 2430057: //경주용 카트 30 day
                mountid = 1033;
                expiration_days = 30;
                break;
            case 2430072: //ZD tiger 7 day
                mountid = 1034;
                expiration_days = 7;
                break;
            case 2430079: //ZD tiger 7 day
                mountid = 1034;
                expiration_days = 1;
                break;
            case 2430080: //shinjo 20 day
                mountid = 1042;
                expiration_days = 20;
                break;
            case 2430082: //orange mush 7 day
                mountid = 1044;
                expiration_days = 7;
                break;
            case 2430091: //nightmare 10 day
                mountid = 1049;
                expiration_days = 7;
                break;
            case 2430092: //yeti 10 day
                mountid = 1050;
                expiration_days = 10;
                break;
            case 2430093: //ostrich 10 day
                mountid = 1051;
                expiration_days = 10;
                break;
            case 2430101: //pink bear 10 day
                mountid = 1052;
                expiration_days = 10;
                break;
            case 2430102: //transformation robo 10 day
                mountid = 1053;
                expiration_days = 10;
                break;
            case 2430144: { //비밀의 마스터리북
                for (int i = 0; i < 100; i++) {
                    final int itemid = Randomizer.nextInt(284) + 2290000;
                    if (MapleItemInformationProvider.getInstance().itemExists(itemid) && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Special") && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Event")) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getSession().write(MaplePacketCreator.getShowItemGain(2430144, (short) -1, true));
                        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + 2430144 + " on " + FileoutputUtil.CurrentReadable_Date());
                        c.getSession().write(MaplePacketCreator.getShowItemGain(itemid, (short) 1, true));
                        //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.useSkillBook(chr, 0, 0, true, true));
                        break;
                    }
                }
                break;
            }
            case 2439986: { //1천
                int amount = 1000;
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
                c.getPlayer().modifyCSPoints(3, amount, true);
                c.getSession().write(MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success"));
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success", chr.getId()), false);
                break;
            }
            case 2439987: { //5천
                int amount = 5000;
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
                c.getPlayer().modifyCSPoints(3, amount, true);
                c.getSession().write(MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success"));
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success", chr.getId()), false);
                break;
            }
            case 2439988: { //1만
                int amount = 10000;
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
                c.getPlayer().modifyCSPoints(3, amount, true);
                c.getSession().write(MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success"));
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success", chr.getId()), false);
                break;
            }
            case 2439989: { //5만
                int amount = 50000;
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
                c.getPlayer().modifyCSPoints(3, amount, true);
                c.getSession().write(MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success"));
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRewardItemAnimation(2022309, "Effect/BasicEff/FindPrize/Success", chr.getId()), false);
                break;
            }
            case 2439985: { //각인의 큐브 교환권
                int amount = 1;
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
                MapleInventoryManipulator.addById(c, 5062001, (short) amount, "큐브 쿠폰: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                c.getSession().write(MaplePacketCreator.getShowItemGain(5062001, (short) amount, true));
                break;
            }
            case 2439990: //큐브 교환권
            case 2439991:
            case 2439992:
            case 2439993:
            case 2439994:
            case 2439995:
            case 2439996:
            case 2439997: {
                int toget = 0;
                byte lastnum = (byte) (itemId - 2439990);
                byte amount = 0;
                switch (lastnum) {
                    case 0:
                        toget = 5062000;
                        amount = 1;
                        break;
                    case 1:
                        toget = 5062000;
                        amount = 3;
                        break;
                    case 2:
                        toget = 5062000;
                        amount = 5;
                        break;
                    case 3:
                        toget = 5062000;
                        amount = 10;
                        break;
                    case 4:
                        toget = 5062002;
                        amount = 1;
                        break;
                    case 5:
                        toget = 5062002;
                        amount = 3;
                        break;
                    case 6:
                        toget = 5062002;
                        amount = 5;
                        break;
                    case 7:
                        toget = 5062002;
                        amount = 10;
                        break;
                }
                MapleInventoryManipulator.addById(c, toget, (short) amount, "큐브 쿠폰: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                c.getSession().write(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getSession().write(MaplePacketCreator.getShowItemGain(toget, (short) amount, true));
                break;
            }
            case 2439983: // 블링 버블리 헤어쿠폰
            case 2439984: // 블링 시원한 아침등교 헤어쿠폰
                // 하드코딩 ㅅㄱ링
                if (chr.getGender() == 0 && chr.getHairCoupon() > 0) {
                    chr.dropMessage(5, "이거 여자 머린데 그래도할거임? 할거면 " + chr.getHairCoupon() + "번 누르면 바뀜");
                    chr.setHairCoupon(chr.getHairCoupon() - 1);
                    break;
                }
                int nHair = 0;
                if (itemId == 2439983) {
                    nHair = 39510;
                } else if (itemId == 2439984) {
                    nHair = 39500;
                } else {
                    break;
                }
                int hairColor = chr.getHair() % 10;
                int hair = nHair + hairColor;
                File f = new File("wz/Character.wz/Hair/000" + hair + ".img.xml");
                if (!f.exists()) {
                    chr.dropMessage(5, "니 머리색깔은 서버에 없으니깐 검은색 머리로 해준다.");
                    hair -= hairColor;
                }
                chr.setHair(hair);
                chr.updateSingleStat(MapleStat.HAIR, hair);
                chr.equipChanged();
                chr.dropMessage(-1, "변신 완료!");
                break;
            default:
                Pair<Integer, String> info = ii.getScriptedItemInfo(itemId);
                if (info == null) {
                    return;
                }
                ItemScriptManager ism = ItemScriptManager.getInstance();
                Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
                if (item == null || item.getItemId() != itemId || item.getQuantity() < 1 || !ism.scriptExists(info.getRight())) {
                    return;
                }
                ism.getItemScript(c, info.getRight(), info.getLeft(), itemId);
                break;
        }
        if (mountid > 0) {
            mountid += (GameConstants.isAran(c.getPlayer().getJob()) ? 20000000 : (GameConstants.isEvan(c.getPlayer().getJob()) ? 20010000 : (GameConstants.isKOC(c.getPlayer().getJob()) ? 10000000 : (GameConstants.isResist(c.getPlayer().getJob()) ? 30000000 : 0))));
            if (c.getPlayer().getSkillLevel(mountid) > 0) {
                c.getPlayer().dropMessage(5, "이미 스킬을 가지고 계십니다.");
            } else if (expiration_days > 0) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(mountid), (byte) 1, (byte) 1, System.currentTimeMillis() + (long) (expiration_days * 24 * 60 * 60 * 1000));
                c.getPlayer().dropMessage(-1, "스킬 " + SkillFactory.getSkillName(mountid) + "(을)를 획득했습니다!!");
            }
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void UseSummonBag(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && (c.getPlayer().getMapId() < 910000000 || c.getPlayer().getMapId() > 910000022)) {
            final Map<String, Integer> toSpawn = MapleItemInformationProvider.getInstance().getEquipStats(itemId);

            if (toSpawn == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleMonster ht = null;
            int type = 0;
            for (Entry<String, Integer> i : toSpawn.entrySet()) {
                if (i.getKey().startsWith("mob") && Randomizer.nextInt(99) <= i.getValue()) {
                    ht = MapleLifeFactory.getMonster(Integer.parseInt(i.getKey().substring(3)));
                    if (ht == null) {
                        chr.dropMessage(1, "존재하지 않는 몬스터 입니다. 몹 아이디: " + Integer.parseInt(i.getKey().substring(3)));
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;

                    }
                    chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
                }
            }
            if (ht == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void UseTreasureChest(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final short slot = slea.readShort();
        final int itemid = slea.readInt();

        final Item toUse = chr.getInventory(MapleInventoryType.ETC).getItem((byte) slot);
        if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemid || chr.hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        int reward;
        int keyIDforRemoval = 0;
        String box;

        switch (toUse.getItemId()) {
            case 4280000: // Gold box
                reward = RandomRewards.getGoldBoxReward();
                keyIDforRemoval = 5490000;
                box = "Gold";
                break;
            case 4280001: // Silver box
                reward = RandomRewards.getSilverBoxReward();
                keyIDforRemoval = 5490001;
                box = "Silver";
                break;
            default: // Up to no good
                return;
        }

        // Get the quantity
        int amount = 1;
        switch (reward) {
            case 2000004:
                amount = 200; // Elixir
                break;
            case 2000005:
                amount = 100; // Power Elixir
                break;
        }
        if (chr.getInventory(MapleInventoryType.CASH).countById(keyIDforRemoval) > 0) {
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, reward, (short) amount);

            if (item == null) {
                chr.dropMessage(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, (byte) slot, (short) 1, true);
            MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, keyIDforRemoval, 1, true, false);
            c.getSession().write(MaplePacketCreator.getShowItemGain(reward, (short) amount, true));

            if (GameConstants.gachaponRareItem(item.getItemId()) > 0) {
                World.Broadcast.broadcastSmega(MaplePacketCreator.getGachaponMega("[" + box + " Chest] " + c.getPlayer().getName(), " : Lucky winner of Gachapon!", item, (byte) 2));
            }
        } else {
            chr.dropMessage(5, "Please check your item inventory and see if you have a Master Key, or if the inventory is full.");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void UseSPResetScroll(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleCharacter chr = c.getPlayer();
        c.getPlayer().updateTick(slea.readInt());
        final short slot = slea.readShort();
        if (slot < 1 && slot > 96) {//hmm
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final Item item = chr.getInventory(MapleInventoryType.USE).getItem((byte) slot);
        final int itemId = slea.readInt();
        if (item.getItemId() / 1000 != 2500 || item == null || item.getItemId() != itemId || GameConstants.isBeginnerJob(chr.getJob())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int[] spToGive = chr.getRemainingSps();
        int skillshit = 0;
        int skillLevel;
        final List<Skill> toRemove = new ArrayList<Skill>();
        for (Skill skill : chr.getSkills().keySet()) {
            final int sid = skill.getId();

            if (GameConstants.isProtectedFromSpReset(sid)) {
                continue;
            }
            if (!skill.isBeginnerSkill() && skill.getId() / 10000000 != 9) {
                skillLevel = chr.getSkillLevel(skill);
                if (skillLevel > 0) {
                    skillshit = skillLevel;
                }
                spToGive[GameConstants.getSkillBookForSkill(skill.getId())] += skillLevel;
                toRemove.add(skill);
            }
        }
        for (Skill skill : toRemove) {
            chr.changeSkillLevel(skill, -1, (byte) -1, -1);
        }
        c.getSession().write(MaplePacketCreator.useSPReset(chr.getId()));
        if (skillshit == 0 && spToGive[0] == 0 && chr.getLevel() > 10) {
            if (GameConstants.isExtendedSPJob(chr.getJob())) {
                chr.dropMessage(1, "현재 직업은 SP초기화가 불가능합니다.");
            } else {
                int sp = 1;
                sp += (chr.getLevel() - (chr.getJob() / 100 % 10 == 2 ? 8 : 10)) * 3;
                if (sp < 0) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                sp += (chr.getJob() % 100 != 0 && chr.getJob() % 100 != 1) ? ((chr.getJob() % 10) + 1) : 0;
                if (chr.getJob() % 10 >= 2) {
                    sp += 2;
                }
                spToGive[0] = sp;
            }
        }
        chr.baseSkills();
        for (int i = 0; i < spToGive.length; i++) {
            chr.setRemainingSp(spToGive[i], i);
        }
        chr.updateSingleStat(MapleStat.AVAILABLESP, 0);//lol
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (byte) slot, (short) 1, true);
    }

    public static final void UseCashItem(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        final byte slot = (byte) slea.readShort(); //슬롯을 바이트로  
        final int itemId = slea.readInt();

        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (toUse.getItemId() != itemId || toUse.getQuantity() < 1 || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        short flag2 = toUse.getFlag();
        if (ItemFlag.KARMA_USE.check(flag2)) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            flag2 |= ItemFlag.KARMA_USE.getValue();
            toUse.setFlag((byte) (flag2 - ItemFlag.KARMA_USE.getValue()));
            c.getSession().write(MaplePacketCreator.updateSpecialItemUse_(toUse, MapleInventoryType.CASH.getType(), c.getPlayer(), true));
            if (toUse.getQuantity() > 1) {//임시처리
                c.getSession().write(MaplePacketCreator.serverNotice(1, ii.getName(toUse.getItemId()) + "의 교환가능 횟수가 차감됐습니다."));
            }
        }

        boolean used = false, cc = false;

        switch (itemId) {
            case 5330000: //퀵배송
            case 5330001: //퀵배송
            {
                boolean canUse = true;
                if ((c.getPlayer().getMapId() >= 922010000 || c.getPlayer().getMapId() == 800040410) && c.getPlayer().getMapId() <= c.getPlayer().getMapId()) {
                    c.getPlayer().dropMessage(5, "현재 맵에서는 사용할 수 없는 아이템입니다.");
                    canUse = false;
                }
                if (canUse && !c.getPlayer().hasBlockedInventory()) {
                    c.getPlayer().setConversation(2);
                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 0x1B, null, null));
                    //NPCScriptManager.getInstance().start(c, 9010009);
                }
                break;
            }
//            case 5043001: // NPC Teleport Rock
//            case 5043000: { // NPC Teleport Rock
//                final short questid = slea.readShort();
//                final int npcid = slea.readInt();
//                final MapleQuest quest = MapleQuest.getInstance(questid);
//
//                if (c.getPlayer().getQuest(quest).getStatus() == 1 && quest.canComplete(c.getPlayer(), npcid)) {
//                    final int mapId = MapleLifeFactory.getNPCLocation(npcid);
//                    if (mapId != -1) {
//                        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
//                        if (map.containsNPC(npcid) && !FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(map.getFieldLimit()) && !c.getPlayer().isInBlockedMap()) {
//                            c.getPlayer().changeMap(map, map.getPortal(0));
//                        }
//                        used = true;
//                    } else {
//                        c.getPlayer().dropMessage(1, "Unknown error has occurred.");
//                    }
//                }
//                break;
//            }
            case 5041001:
            case 5040004:
            case 5040003:
            case 5040002:
            case 2320000: // The Teleport Rock
            case 5041000: // VIP Teleport Rock
            case 5040000: // 그냥 순돌
            case 5040001: { // Teleport Coke
                used = UseTeleRock(slea, c, itemId);
                break;
            }
//            case 5450005: {
//                c.getPlayer().setConversation(4);
//                c.getPlayer().getStorage().sendStorage(c, 1022005);
//                break;
//            }
            case 5050000: { // AP Reset
                Map<MapleStat, Integer> statupdate = new EnumMap<MapleStat, Integer>(MapleStat.class);
                final int apto = slea.readInt();
                final int apfrom = slea.readInt();

                if (apto == apfrom) {
                    break; // Hack
                }

                if (apto == 2048 || apto == 8192 || apfrom == 2048 || apfrom == 8192) {
                    c.getPlayer().dropMessage(1, "스탯을 HP나 MP에 투자하실 수 없습니다.");
                    return;
                }

                final int job = c.getPlayer().getJob();
                final PlayerStats playerst = c.getPlayer().getStat();
                used = true;

                switch (apto) { // AP to
                    case 64: // str
                        if (playerst.getStr() >= 9999) {
                            used = false;
                        }
                        break;
                    case 128: // dex
                        if (playerst.getDex() >= 9999) {
                            used = false;
                        }
                        break;
                    case 256: // int
                        if (playerst.getInt() >= 9999) {
                            used = false;
                        }
                        break;
                    case 512: // luk
                        if (playerst.getLuk() >= 9999) {
                            used = false;
                        }
                        break;
                    case 2048: // hp
                        if (playerst.getMaxHp() >= 99999) {
                            used = false;
                        }
                        break;
                    case 8192: // mp
                        if (playerst.getMaxMp() >= 99999) {
                            used = false;
                        }
                        break;
                    default:
                        AutobanManager.getInstance().autoban(c, "Invalid AP Reset Hacked.");
                        return;
                }
                switch (apfrom) { // AP to
                    case 64: // str
                        if (playerst.getStr() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 1 && playerst.getStr() <= 35)) {
                            used = false;
                        }
                        break;
                    case 128: // dex
                        if (playerst.getDex() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 3 && playerst.getDex() <= 25) || (c.getPlayer().getJob() % 1000 / 100 == 4 && playerst.getDex() <= 25)) {
                            used = false;
                        }
                        break;
                    case 256: // int
                        if (playerst.getInt() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 2 && playerst.getInt() <= 20)) {
                            used = false;
                        }
                        break;
                    case 512: // luk
                        if (playerst.getLuk() <= 4) {
                            used = false;
                        }
                        break;
                    case 2048: // hp
                        if (/*playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) + 134) || */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                            used = false;
                            c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                        }
                        break;
                    case 8192: // mp
                        if (/*playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) + 134) || */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                            used = false;
                            c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                        }
                        break;
                    default:
                        AutobanManager.getInstance().autoban(c, "Invalid AP Reset Hacked.");
                        return;
                }
                if (used) {
                    switch (apto) { // AP to
                        case 64: { // str
                            final int toSet = playerst.getStr() + 1;
                            playerst.setStr((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.STR, toSet);
                            break;
                        }
                        case 128: { // dex
                            final int toSet = playerst.getDex() + 1;
                            playerst.setDex((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.DEX, toSet);
                            break;
                        }
                        case 256: { // int
                            final int toSet = playerst.getInt() + 1;
                            playerst.setInt((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.INT, toSet);
                            break;
                        }
                        case 512: { // luk
                            final int toSet = playerst.getLuk() + 1;
                            playerst.setLuk((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.LUK, toSet);
                            break;
                        }
                        case 2048: // hp
                            int maxhp = playerst.getMaxHp();
                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxhp += Randomizer.rand(8, 12);
                            } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112)) { // Warrior

                                Skill improvingMaxHP = SkillFactory.getSkill(1000001);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(20, 24);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212)) { // Magician
                                maxhp += Randomizer.rand(6, 10);
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412)) { // Bowman & Thief
                                Skill improvingMaxHP = SkillFactory.getSkill(5100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(16, 20);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 510 && job <= 522) {
                                Skill improvingMaxHP = SkillFactory.getSkill(5100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(18, 20);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1510 && job <= 1512) {
                                Skill improvingMaxHP = SkillFactory.getSkill(15100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp += Randomizer.rand(18, 20);
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job == 500 || job == 1500) { // Pirate
                                maxhp += Randomizer.rand(18, 20);
                            } else { // GameMaster
                                maxhp += Randomizer.rand(50, 100);
                            }
                            maxhp = Math.min(99999, Math.abs(maxhp));
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                            playerst.setMaxHp(maxhp, c.getPlayer());
                            statupdate.put(MapleStat.MAXHP, (int) maxhp);
                            break;

                        case 8192: // mp
                            int maxmp = playerst.getMaxMp();

                            if (job == 0) { // Beginner
                                maxmp += Randomizer.rand(6, 8);
                            } else if (job >= 100 && job <= 132) { // Warrior
                                maxmp += Randomizer.rand(2, 4);
                            } else if (job >= 200 && job <= 232) { // Magician
                                Skill improvingMaxMP = SkillFactory.getSkill(2000001);
                                int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                                maxmp += Randomizer.rand(18, 20);
                                if (improvingMaxMPLevel >= 1) {
                                    maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                                }
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434)) { // Bowman & Thief
                                maxmp += Randomizer.rand(10, 12);
                            } else if (job >= 500 && job <= 522) { // Pirate
                                maxmp += Randomizer.rand(14, 18);
                            } else if (job >= 1100 && job <= 1111) { // Soul Master
                                maxmp += Randomizer.rand(6, 9);
                            } else if (job >= 1200 && job <= 1211) { // Flame Wizard
                                Skill improvingMaxMP = SkillFactory.getSkill(12000000);
                                int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                                maxmp += Randomizer.rand(33, 36);
                                if (improvingMaxMPLevel >= 1) {
                                    maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                                }
                            } else if ((job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411)) { // Wind Breaker and Night Walker
                                maxmp += Randomizer.rand(21, 24);
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxmp += Randomizer.rand(4, 6);
                            } else { // GameMaster
                                maxmp += Randomizer.rand(50, 100);
                            }
                            maxmp = Math.min(99999, Math.abs(maxmp));
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                            playerst.setMaxMp(maxmp, c.getPlayer());
                            statupdate.put(MapleStat.MAXMP, (int) maxmp);
                            break;
                    }
                    switch (apfrom) { // AP from
                        case 64: { // str
                            final int toSet = playerst.getStr() - 1;
                            playerst.setStr((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.STR, toSet);
                            break;
                        }
                        case 128: { // dex
                            final int toSet = playerst.getDex() - 1;
                            playerst.setDex((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.DEX, toSet);
                            break;
                        }
                        case 256: { // int
                            final int toSet = playerst.getInt() - 1;
                            playerst.setInt((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.INT, toSet);
                            break;
                        }
                        case 512: { // luk
                            final int toSet = playerst.getLuk() - 1;
                            playerst.setLuk((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.LUK, toSet);
                            break;
                        }
                        case 2048: // HP
                            int maxhp = playerst.getMaxHp();
                            if (job == 0) { // Beginner
                                maxhp -= 12;
                            } else if (job >= 100 && job <= 132) { // Warrior
                                Skill improvingMaxHP = SkillFactory.getSkill(1000001);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= 24;
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 200 && job <= 232) { // Magician
                                maxhp -= 10;
                            } else if (job >= 300 && job <= 322 || job >= 400 && job <= 434) { // Bowman, Thief
                                Skill improvingMaxHP = SkillFactory.getSkill(5100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= 15;
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 510 && job <= 522) { // Pirate(1)
                                Skill improvingMaxHP = SkillFactory.getSkill(5100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= 20;
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job >= 1510 && job <= 1512) { // Pirate(1)
                                Skill improvingMaxHP = SkillFactory.getSkill(15100000);
                                int improvingMaxHPLevel = c.getPlayer().getSkillLevel(improvingMaxHP);
                                maxhp -= 20;
                                if (improvingMaxHPLevel >= 1) {
                                    maxhp -= improvingMaxHP.getEffect(improvingMaxHPLevel).getY();
                                }
                            } else if (job == 500 || job == 1500) { // Pirate(2)
                                maxhp -= 20;
                            }
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                            playerst.setMaxHp(maxhp, c.getPlayer());
                            statupdate.put(MapleStat.MAXHP, (int) maxhp);
                            break;
                        case 8192: // MP
                            int maxmp = playerst.getMaxMp();
                            if (job == 0) { // Beginner
                                maxmp -= 8;
                            } else if (job >= 100 && job <= 132) { // Warrior
                                maxmp -= 4;
                            } else if (job >= 200 && job <= 232) { // Magician
                                Skill improvingMaxMP = SkillFactory.getSkill(2000001);
                                int improvingMaxMPLevel = c.getPlayer().getSkillLevel(improvingMaxMP);
                                maxmp -= 30;
                                if (improvingMaxMPLevel >= 1) {
                                    maxmp -= improvingMaxMP.getEffect(improvingMaxMPLevel).getY();
                                }
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434)) { // Pirate, Bowman. Thief
                                maxmp -= 12;
                            } else if (job >= 500 && job <= 522) { // Pirate, Bowman. Thief
                                maxmp -= 16;
                            }
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                            playerst.setMaxMp(maxmp, c.getPlayer());
                            statupdate.put(MapleStat.MAXMP, (int) maxmp);
                            break;
                    }
                    c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true, c.getPlayer().getJob()));
                }
                break;
            }
            case 5050001: // SP Reset (1st job)
            case 5050002: // SP Reset (2nd job)
            case 5050003: // SP Reset (3rd job)
            case 5050004:  // SP Reset (4th job)
            case 5050005: //evan sp resets
            case 5050006:
            case 5050007:
            case 5050008:
            case 5050009: {
                if (itemId >= 5050005 && !GameConstants.isEvan(c.getPlayer().getJob())) {
                    c.getPlayer().dropMessage(1, "This reset is only for Evans.");
                    break;
                } //well i dont really care other than this o.o
                if (itemId < 5050005 && GameConstants.isEvan(c.getPlayer().getJob())) {
                    c.getPlayer().dropMessage(1, "This reset is only for non-Evans.");
                    break;
                } //well i dont really care other than this o.o
                int skill1 = slea.readInt();
                int skill2 = slea.readInt();
                for (int i : GameConstants.blockedSkills) {
                    if (skill1 == i) {
                        c.getPlayer().dropMessage(1, "You may not add this skill.");
                        return;
                    }
                }

                Skill skillSPTo = SkillFactory.getSkill(skill1);
                Skill skillSPFrom = SkillFactory.getSkill(skill2);

                if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
                    c.getPlayer().dropMessage(1, "You may not add beginner skills.");
                    break;
                }
                if (GameConstants.getSkillBookForSkill(skill1) != GameConstants.getSkillBookForSkill(skill2)) { //resistance evan
                    c.getPlayer().dropMessage(1, "You may not add different job skills.");
                    break;
                }
                //if (GameConstants.getJobNumber(skill1 / 10000) > GameConstants.getJobNumber(skill2 / 10000)) { //putting 3rd job skillpoints into 4th job for example
                //    c.getPlayer().dropMessage(1, "You may not add skillpoints to a higher job.");
                //    break;
                //}
                if ((c.getPlayer().getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && c.getPlayer().getSkillLevel(skillSPFrom) > 0 && skillSPTo.canBeLearnedBy(c.getPlayer().getJob())) {
                    if (skillSPTo.isFourthJob() && (c.getPlayer().getSkillLevel(skillSPTo) + 1 > c.getPlayer().getMasterLevel(skillSPTo))) {
                        c.getPlayer().dropMessage(1, "You will exceed the master level.");
                        break;
                    }
                    if (itemId >= 5050005) {
                        if (GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 && GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 + 1) {
                            c.getPlayer().dropMessage(1, "You may not add this job SP using this reset.");
                            break;
                        }
                    }
                    c.getPlayer().changeSkillLevel(skillSPFrom, (byte) (c.getPlayer().getSkillLevel(skillSPFrom) - 1), c.getPlayer().getMasterLevel(skillSPFrom));
                    c.getPlayer().changeSkillLevel(skillSPTo, (byte) (c.getPlayer().getSkillLevel(skillSPTo) + 1), c.getPlayer().getMasterLevel(skillSPTo));
                    used = true;
                }
                break;
            }
            case 5500000: { // Magic Hourglass 1 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 1;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500001: { // Magic Hourglass 7 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 7;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500002: { // Magic Hourglass 20 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 20;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500005: { // Magic Hourglass 50 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 50;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5500006: { // Magic Hourglass 99 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 99;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "이 아이템에는 사용할 수 없습니다.");
                    }
                }
                break;
            }
            case 5060000: { // Item Tag
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());

                if (item != null && item.getOwner().equals("")) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setOwner(c.getPlayer().getName());
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    }
                }
                break;
            }
            case 5062000: { //미라클 큐브
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    switch (item.getItemId()) {
                        case 1112402: //리플렉스 링
                        case 1132000: //허리띠류
                        case 1132001:
                        case 1132002:
                        case 1132003:
                        case 1132004:
                            c.getPlayer().dropMessage(5, "이 아이템의 잠재능력은 재설정 하실 수 없습니다.");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                    }
                    final Equip eq = (Equip) item;
                    if (eq.getState() >= 5) {
                        if (eq.getState() >= 7) {
                            c.getPlayer().dropMessage(5, "에픽이상의 아이템은 재설정 하실 수 없습니다.");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        }
                        eq.renewPotential(false, c.getPlayer(), itemId);
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1));
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        MapleInventoryManipulator.addById(c, 2430112, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "잠재능력을 재설정 할 수 없습니다..");
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 0));
                }
                break;
            }
            case 5062002: { //마스터 미라클 큐브
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    switch (item.getItemId()) {
                        case 1112402: //리플렉스 링
                        case 1132000: //허리띠류
                        case 1132001:
                        case 1132002:
                        case 1132003:
                        case 1132004:
                            c.getPlayer().dropMessage(5, "이 아이템의 잠재능력은 재설정 하실 수 없습니다.");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                    }
                    final Equip eq = (Equip) item;
                    if (eq.getState() >= 5) {
                        eq.renewPotential(false, c.getPlayer(), itemId);
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1));
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        MapleInventoryManipulator.addById(c, 2430481, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "잠재능력을 재설정 할 수 없습니다..");
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 0));
                }
                break;
            }
            case 5062100: { //레드큐브
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    switch (item.getItemId()) {
                        case 1112402: //리플렉스 링
                        case 1132000: //허리띠류
                        case 1132001:
                        case 1132002:
                        case 1132003:
                        case 1132004:
                            c.getPlayer().dropMessage(5, "이 아이템의 잠재능력은 재설정 하실 수 없습니다.");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                    }
                    final Equip eq = (Equip) item;
                    if (eq.getState() >= 5) {
                        if (eq.getState() >= 8) {
                            c.getPlayer().dropMessage(5, "에픽이상의 아이템은 재설정 하실 수 없습니다.");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        }
                        eq.renewPotential(false, c.getPlayer(), itemId);
                        int gcc = eq.getCubedCount();
                        eq.setCubedCount(gcc + 1);
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1));
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        MapleInventoryManipulator.addById(c, 2431893, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "잠재능력을 재설정 할 수 없습니다..");
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 0));
                }
                break;
            }
            case 5062001: { //각인의 큐브
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    switch (item.getItemId()) {
                        case 1112402: //리플렉스 링
                        case 1132000: //허리띠류
                        case 1132001:
                        case 1132002:
                        case 1132003:
                        case 1132004:
                            c.getPlayer().dropMessage(5, "이 아이템의 잠재능력은 재설정 하실 수 없습니다.");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                    }
                    final Equip eq = (Equip) item;
                    if (eq.getState() >= 4) {
                        if (eq.getState() != 5) {
                            c.getPlayer().dropMessage(5, "레어아이템만 잠재능력을 재설정 하실 수 있습니다..");
                            c.sendPacket(MaplePacketCreator.enableActions());
                            return;
                        }
                        eq.renewPotential(true, c.getPlayer(), itemId);
                        c.getSession().write(MaplePacketCreator.scrolledItem(toUse, item, false, true));
                        c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 1));
                        c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                        //MapleInventoryManipulator.addById(c, 2431893, (short) 1, "Cube" + " on " + FileoutputUtil.CurrentReadable_Date());
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "잠재능력을 재설정 할 수 없습니다..");
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.getPotentialEffect(c.getPlayer().getId(), 0));
                }
                break;
            }
            case 5521000: { // Karma
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());

                if (item != null && !ItemFlag.KARMA_ACC.check(item.getFlag()) && !ItemFlag.KARMA_ACC_USE.check(item.getFlag())) {
                    if (MapleItemInformationProvider.getInstance().isShareTagEnabled(item.getItemId())) {
                        short flag = item.getFlag();
                        if (ItemFlag.UNTRADEABLE.check(flag)) {
                            flag -= ItemFlag.UNTRADEABLE.getValue();
                        } else if (type == MapleInventoryType.EQUIP) {
                            flag |= ItemFlag.KARMA_ACC.getValue();
                        } else {
                            flag |= ItemFlag.KARMA_ACC_USE.getValue();
                        }
                        item.setFlag(flag);
                        c.getPlayer().forceReAddItem_NoUpdate(item, type);
                        c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, c.getPlayer()));
                        used = true;
                    }
                }
                break;
            }
            case 5520001: //p.karma
            case 5520000: { // Karma
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());

                if (item != null && !ItemFlag.KARMA_EQ.check(item.getFlag()) && !ItemFlag.KARMA_USE.check(item.getFlag())) {
                    if ((itemId == 5520000 && MapleItemInformationProvider.getInstance().isKarmaEnabled(item.getItemId())) || (itemId == 5520001 && MapleItemInformationProvider.getInstance().isPKarmaEnabled(item.getItemId()))) {
                        short flag = item.getFlag();
                        if (ItemFlag.UNTRADEABLE.check(flag)) {
                            flag -= ItemFlag.UNTRADEABLE.getValue();
                            //c.sendPacket(MaplePacketCreator.enableActions());
                            //return;
                        } else if (type == MapleInventoryType.EQUIP) {
                            flag |= ItemFlag.KARMA_EQ.getValue();
                        } else {
                            flag |= ItemFlag.KARMA_USE.getValue();
                        }
                        item.setFlag(flag);
                        c.getPlayer().forceReAddItem_NoUpdate(item, type);
                        c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, c.getPlayer()));
                        used = true;
                    }
                }
                break;
            }
            case 5610001:
            case 5610000: { // Vega 30
                slea.readInt(); // Inventory type, always eq
                final short dst = (short) slea.readInt();
                slea.readInt(); // Inventory type, always use
                final short src = (short) slea.readInt();
                used = UseUpgradeScroll(src, dst, (short) 2, c, c.getPlayer(), itemId); //cannot use ws with vega but we dont care
                cc = used;
                break;
            }
            case 5060001: { // Sealing Lock
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061000: { // Sealing Lock 7 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);
                    item.setExpiration(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061001: { // Sealing Lock 30 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061002: { // Sealing Lock 90 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (90 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5061003: { // Sealing Lock 365 days
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getExpiration() == -1) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.LOCK.getValue();
                    item.setFlag(flag);

                    item.setExpiration(System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000));

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5063000: {
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getType() == 1) { //equip
                    short flag = item.getFlag();
                    flag |= ItemFlag.LUCKS_KEY.getValue();
                    item.setFlag(flag);

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5064000: {
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
                // another int here, lock = 5A E5 F2 0A, 7 day = D2 30 F3 0A
                if (item != null && item.getType() == 1) { //equip
                    if (((Equip) item).getEnhance() >= 8) {
                        break; //cannot be used
                    }
                    short flag = item.getFlag();
                    flag |= ItemFlag.SHIELD_WARD.getValue();
                    item.setFlag(flag);

                    c.getPlayer().forceReAddItem_Flag(item, type);
                    used = true;
                }
                break;
            }
            case 5060002:  //부화기
            case 5060003: { //땅콩
                slea.readInt();
                int nSlot = slea.readInt();
                Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem((short) nSlot);
                if (item == null || item.getQuantity() <= 0) {
                    return;
                }
                if (getIncubatedItems(c, item.getItemId())) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, item.getPosition(), (short) 1, false);
                    used = true;
                }
                break;
            }
            /*2A 
             08 00 
             E8 C0 1F 00 
             01 
             00 2E*/

            case 5071000: { //확성기
                if (!c.getPlayer().getCanTalk()) {
                    c.getPlayer().dropMessage(5, "채팅금지 상태가 풀린 후 이용 가능합니다.");
                    break;
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "이곳에서 사용할 수 없습니다.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "60초마다 한번씩만 사용할 수 있습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    c.getChannelServer().broadcastSmegaPacket(MaplePacketCreator.serverNotice(2, sb.toString()));
                    ServerLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName());
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "확성기를 사용할 수 없는 상태입니다.");
                }
                break;
            }
            case 5077000: { //세줄 확성기
                if (!c.getPlayer().getCanTalk()) {
                    c.getPlayer().dropMessage(5, "채팅금지 상태가 풀린 후 이용 가능합니다.");
                    break;
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "이곳에서 사용할 수 없습니다.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "60초마다 한 번씩만 사용할 수 있습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final byte numLines = slea.readByte();
                    if (numLines > 3) {
                        return;
                    }
                    final List<String> messages = new LinkedList<String>();
                    String message;
                    for (int i = 0; i < numLines; i++) {
                        message = slea.readMapleAsciiString();
                        if (message.length() > 65) {
                            break;
                        }

                        final StringBuilder sb = new StringBuilder();
                        addMedalString(c.getPlayer(), sb);
                        sb.append(c.getPlayer().getName());
                        sb.append(" : ");
                        sb.append(message);
                        messages.add(sb.toString());
                    }
                    final Item medal = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                    String playerString = "<" + MapleItemInformationProvider.getInstance().getName(medal.getItemId()).replace("의 훈장", "") + "> " + c.getPlayer().getName() + " : ";
                    String message1 = messages.get(0).replaceFirst(playerString, "");
                    String message2 = messages.get(1).replaceFirst(playerString, "");
                    String message3 = messages.get(2).replaceFirst(playerString, "");
                    final boolean ear = slea.readByte() > 0;

                    World.Broadcast.broadcastSmega(MaplePacketCreator.tripleSmega(messages, ear, c.getChannel()));
                    ServerLogger.getInstance().logChat(LogType.Chat.TripleMegaphone, c.getPlayer().getId(), c.getPlayer().getName(), "\r\n" + message1 + "\r\n" + message2 + "\r\n" + message3 + "\r\n", "채널 : " + c.getRealChannelName() + " / 귀 : " + (ear ? "예" : "아니오"));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5079004: { // Heart Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "You may only use this every 15 seconds.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    World.Broadcast.broadcastSmega(MaplePacketCreator.echoMegaphone(c.getPlayer().getName(), message));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5073000: { // Heart Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "You may only use this every 15 seconds.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() != 0;
                    World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(9, c.getChannel(), sb.toString(), ear));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5074000: { // Skull Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "You may only use this every 15 seconds.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() != 0;

                    World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(10, c.getChannel(), sb.toString(), ear));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5072000: { // 고성능 확성기
                if (!c.getPlayer().getCanTalk()) {
                    c.getPlayer().dropMessage(5, "채팅금지 상태가 풀린 후 이용 가능합니다.");
                    break;
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "이곳에서 사용할 수 없습니다.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "60초마다 한번씩만 사용할 수 있습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() != 0;

                    World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(3, c.getChannel(), sb.toString(), ear));
                    ServerLogger.getInstance().logChat(LogType.Chat.SuperMegaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName() + " / 귀 : " + (ear ? "예" : "아니오"));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5076000: { // Item Megaphone
                if (!c.getPlayer().getCanTalk()) {
                    c.getPlayer().dropMessage(5, "채팅금지 상태가 풀린 후 이용 가능합니다.");
                    break;
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용할 수 있습니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "이곳에서 사용할 수 없습니다.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "60초마다 한번씩만 사용할 수 있습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() > 0;

                    Item item = null;
                    if (slea.readByte() == 1) { //item
                        byte invType = (byte) slea.readInt();
                        byte pos = (byte) slea.readInt();
                        if (pos < 0) {
                            item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                        } else {
                            item = c.getPlayer().getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
                        }
                    }
                    ServerLogger.getInstance().logChat(LogType.Chat.ItemMegaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName() + " / 귀 : " + (ear ? "예" : "아니오"));
                    World.Broadcast.broadcastSmega(MaplePacketCreator.itemMegaphone(sb.toString(), ear, c.getChannel(), item));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5075000: // MapleTV Messenger
            case 5075001: // MapleTV Star Messenger
            case 5075002: { // MapleTV Heart Messenger
                c.getPlayer().dropMessage(5, "There are no MapleTVs to broadcast the message to.");
                break;
            }
            case 5075003:
            case 5075004:
            case 5075005: {
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canSmega()) {
                    c.getPlayer().dropMessage(5, "You may only use this every 15 seconds.");
                    break;
                }
                int tvType = itemId % 10;
                if (tvType == 3) {
                    slea.readByte(); //who knows
                }
                boolean ear = tvType != 1 && tvType != 2 && slea.readByte() > 1; //for tvType 1/2, there is no byte. 
                MapleCharacter victim = tvType == 1 || tvType == 4 ? null : c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString()); //for tvType 4, there is no string.
                if (tvType == 0 || tvType == 3) { //doesn't allow two
                    victim = null;
                } else if (victim == null) {
                    c.getPlayer().dropMessage(1, "That character is not in the channel.");
                    break;
                }
                String message = slea.readMapleAsciiString();
                World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(3, c.getChannel(), c.getPlayer().getName() + " : " + message, ear));
                used = true;
                break;
            }
            case 5090100: // Wedding Invitation Card
            case 5090000: { // Note
                final String sendTo = slea.readMapleAsciiString();
                final String msg = slea.readMapleAsciiString();
                int VictimChannel = World.Find.findChannel(sendTo);
                if (VictimChannel > 0) {
                    c.getSession().write(CSPacket.SendNoteResult((byte) 4));
                } else {
                    c.getPlayer().sendNote(sendTo, msg);
                    c.getSession().write(CSPacket.SendNoteResult((byte) 3));
                    ServerLogger.getInstance().logChat(LogType.Chat.Note, c.getPlayer().getId(), c.getPlayer().getName(), msg, "수신 : " + sendTo);
                    used = true;
                }
                break;
            }

            case 5100000: { // Congratulatory Song
                c.getPlayer().getMap().startMapEffect(c.getPlayer().getName(), itemId, true); //CashSong
//                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange("Jukebox/Congratulation"));
                used = true;
                break;
            }
            case 5190001:
            case 5190002:
            case 5190003:
            case 5190004:
            case 5190005:
            case 5190006:
            case 5190007:
            case 5190008:
            case 5190000: { // Pet Flags
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = null;
                for (Item pe : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (pe.getUniqueId() == uniqueid) {
                        pet = pe.getPet();
                    }
                }

                if (pet == null) {
                    break;
                }
                PetFlag zz = PetFlag.getByAddId(itemId);
                if (zz != null && !zz.check(pet.getFlags())) {
                    pet.setFlags(pet.getFlags() | zz.getValue());
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(CSPacket.changePetFlag(uniqueid, true, zz.getValue()));
                    used = true;
                }
                break;
            }
            case 5191001:
            case 5191002:
            case 5191003:
            case 5191004:
            case 5191000: { // Pet Flags
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;

                if (pet == null) {
                    break;
                }
                if (pet.getUniqueId() != uniqueid) {
                    pet = c.getPlayer().getPet(1);
                    slo = 1;
                    if (pet != null) {
                        if (pet.getUniqueId() != uniqueid) {
                            pet = c.getPlayer().getPet(2);
                            slo = 2;
                            if (pet != null) {
                                if (pet.getUniqueId() != uniqueid) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                PetFlag zz = PetFlag.getByDelId(itemId);
                if (zz != null && zz.check(pet.getFlags())) {
                    pet.setFlags(pet.getFlags() - zz.getValue());
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getSession().write(CSPacket.changePetFlag(uniqueid, false, zz.getValue()));
                    used = true;
                }
                break;
            }
            case 5501001:
            case 5501002: { //expiry mount
                final Skill skil = SkillFactory.getSkill(slea.readInt());
                if (skil == null || skil.getId() / 10000 != 8000 || c.getPlayer().getSkillLevel(skil) <= 0 || !skil.isTimeLimited() || GameConstants.getMountItem(skil.getId(), c.getPlayer()) <= 0) {
                    break;
                }
                final long toAdd = (itemId == 5501001 ? 30 : 60) * 24 * 60 * 60 * 1000L;
                final long expire = c.getPlayer().getSkillExpiry(skil);
                if (expire < System.currentTimeMillis() || (long) (expire + toAdd) >= System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)) {
                    break;
                }
                c.getPlayer().changeSkillLevel(skil, c.getPlayer().getSkillLevel(skil), c.getPlayer().getMasterLevel(skil), (long) (expire + toAdd));
                used = true;
                break;
            }

            case 5170000: { // Pet name change
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = null;
                for (Item pe : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (pe.getUniqueId() == uniqueid) {
                        pet = pe.getPet();
                    }
                }
                if (pet == null) {
                    break;
                }
                String nName = slea.readMapleAsciiString();
                if (MapleCharacterUtil.canChangePetName(nName)) {
                    pet.setName(nName);
                    c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    if (pet.getSummoned()) {
                        c.getPlayer().getMap().broadcastMessage(CSPacket.changePetName(c.getPlayer(), nName, c.getPlayer().getPetIndex(pet)));
                    }
                    used = true;
                }
                break;
            }
            case 5230001:
            case 5230000: {// owl of minerva
                final int itemSearch = slea.readInt();
                MinervaOwlSearchTop.getInstance().searchItem(itemSearch);
                List<AbstractPlayerStore> hms = new LinkedList<AbstractPlayerStore>();
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    hms = cserv.searchShop(itemSearch, hms);
                }
                if (hms.size() > 0) {
                    c.getSession().write(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(1, "아이템을 발견하지 못하였습니다.");
                }
                break;
            }
            case 5080000: //메시지 박스
            case 5080001:
            case 5080002:
            case 5080003: {
                Point pos = c.getPlayer().getPosition();
                used = true;
                List<MapleMapObjectType> list = new LinkedList<MapleMapObjectType>();
                list.add(MapleMapObjectType.NPC);
                list.add(MapleMapObjectType.MESSAGEBOX);
                list.add(MapleMapObjectType.HIRED_MERCHANT);
                list.add(MapleMapObjectType.SHOP);
                if (!c.getPlayer().getMap().getMapObjectsInRange(pos, 30000, list).isEmpty()) {
                    used = false;
                    MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
                    mplew.writeOpcode(SendPacketOpcode.SPAWN_MESSAGEBOX.getValue() - 1);
                    c.getSession().write(mplew.getPacket());
                    break;
                }
                list.clear();
                if (used) {
                    String owner = c.getPlayer().getName();
                    String message = slea.readMapleAsciiString();
                    MapleMessageBox mmb = new MapleMessageBox(itemId, pos, owner, message);
                    c.getPlayer().getMap().spawnMessageBox(mmb);
                    ServerLogger.getInstance().logChat(LogType.Chat.MessageBox, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getRealChannelName() + " / 맵 : " + c.getPlayer().getMapId() + " / 아이템 : " + MapleItemInformationProvider.getInstance().getName(itemId));
                }
            }
            break;

            case 5281001: //idk, but probably
            case 5280001: // Gas Skill
            case 5281000: { // Passed gas
                Rectangle bounds = new Rectangle((int) c.getPlayer().getPosition().getX(), (int) c.getPlayer().getPosition().getY(), 1, 1);
                MapleMist mist = new MapleMist(bounds, c.getPlayer());
                c.getPlayer().getMap().spawnMist(mist, 10000, true);
                c.getSession().write(MaplePacketCreator.enableActions());
                used = true;
                break;
            }
            case 5370001:
            case 5370000: { // Chalkboard
                for (MapleEventType t : MapleEventType.values()) {
                    final MapleEvent e = ChannelServer.getInstance(c.getChannel()).getEvent(t);
                    if (e.isRunning()) {
                        for (int i : e.getType().mapids) {
                            if (c.getPlayer().getMapId() == i) {
                                c.getPlayer().dropMessage(5, "You may not use that here.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                        }
                    }
                }
                c.getPlayer().setChalkboard(slea.readMapleAsciiString());
                break;
            }
            case 5079000:
            case 5079001:
            case 5390007:
            case 5390008:
            case 5390009:
            case 5390000: // Diablo Messenger
            case 5390001: // Cloud 9 Messenger
            case 5390002: // Loveholic Messenger
            case 5390003: // New Year Megassenger 1
            case 5390004: // New Year Megassenger 2
            case 5390005: // Cute Tiger Messenger
            case 5390006: { // Tiger Roar's Messenger
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getPlayer().getCheatTracker().canAvatarSmega()) {
                    c.getPlayer().dropMessage(5, "You may only use this every 5 minutes.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String text = slea.readMapleAsciiString();
                    if (text.length() > 55) {
                        break;
                    }
                    final boolean ear = slea.readByte() != 0;
                    World.Broadcast.broadcastSmega(MaplePacketCreator.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, text, ear));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "현재 확성기 사용 금지 상태입니다.");
                }
                break;
            }
            case 5450000: { // Mu Mu the Travelling Merchant
                if (FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                    c.getPlayer().dropMessage(1, "이곳에서는 사용할 수 없습니다.");
                    return;
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "레벨 10 이상만 사용 가능합니다.");
                } else if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000) {
                    c.getPlayer().dropMessage(5, "이곳에서는 사용할 수 없습니다.");
                } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                    c.getPlayer().dropMessage(5, "이곳에서는 사용할 수 없습니다.");
                } else {
                    MapleShopFactory.getInstance().getShop(9090000).sendShop(c);
                }
                used = true;
                break;
            }
            case 5152100:
            case 5152101:
            case 5152102:
            case 5152103:
            case 5152104:
            case 5152105:
            case 5152106:
            case 5152107:
                used = true;
                int face = ((c.getPlayer().getFace() / 1000) * 1000) + (c.getPlayer().getFace() % 100);
                if (face == 20071 || face == 20072 || face == 20079 || face == 21066 || face == 21067 || face == 24076) {
                    c.getPlayer().dropMessage(1, "컬러렌즈를 사용할 수 없는 성형입니다.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                face += (itemId % 10) * 100;
                c.getPlayer().setFace(face);
                c.getPlayer().updateSingleStat(MapleStat.FACE, face);
                c.getPlayer().equipChanged();
                break;
            default:
                if (itemId / 10000 == 524) {
                    used = UseCashPetFood(c, itemId);
                    break;
                } else if (itemId / 10000 == 512) {
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    String msg = ii.getMsg(itemId);
                    final String ourMsg = slea.readMapleAsciiString();
//                    if (!msg.contains("%s")) {
//                        msg = ourMsg;
//                    } else {
//                        msg = msg.replaceFirst("%s", c.getPlayer().getName());
//                        if (!msg.contains("%s")) {
//                            msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
//                        } else {
//                            try {
//                                msg = msg.replaceFirst("%s", ourMsg);
//                            } catch (Exception e) {
//                                msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
//                            }
//                        }
//                    }
//                    if (ourMsg.startsWith(msg)) {
                    c.getPlayer().getMap().startMapEffect(/*msg*/ourMsg, itemId);

                    final int buff = ii.getStateChangeItem(itemId);
                    if (buff != 0) {
                        for (MapleCharacter mChar : c.getPlayer().getMap().getCharactersThreadsafe()) {
                            ii.getItemEffect(buff).applyTo(mChar);
                        }
                    }
                    ServerLogger.getInstance().logChat(LogType.Chat.Weather, c.getPlayer().getId(), c.getPlayer().getName(), ourMsg, "아이템 : " + MapleItemInformationProvider.getInstance().getName(itemId) + " / 맵 : " + c.getPlayer().getMapId() + " / 채널 : " + c.getRealChannelName());
                    used = true;
//                    }
                } else if (itemId / 10000 == 510) {
                    c.getPlayer().getMap().startJukebox(c.getPlayer().getName(), itemId);
                    used = true;
                } else if (itemId / 10000 == 562) {
                    if (UseSkillBook(slot, itemId, c, c.getPlayer())) {
                        c.getPlayer().gainSP(1);
                    } //this should handle removing
                } else if (itemId / 10000 == 553) {
                    UseRewardItem(slot, itemId, c, c.getPlayer());// this too
                } else if (itemId / 10000 != 519) {
                    System.out.println("Unhandled CS item : " + itemId);
                    System.out.println(slea.toString(true));
                }
                break;
        }

        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, true);
        }

        c.getSession().write(MaplePacketCreator.enableActions());
        if (cc) {
            if (!c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null || FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().dropMessage(1, "Auto relog failed.");
                return;
            }
            c.getPlayer().dropMessage(5, "Auto relogging. Please wait.");
            c.getPlayer().fakeRelog();
            if (c.getPlayer().getScrolledPosition() != 0) {
                c.getSession().write(MaplePacketCreator.pamSongUI());
            }
        }
    }
    static byte index = 0;

    private static boolean UseCashPetFood(final MapleClient c, final int itemId) {
        // Pet food
        MaplePet pet = c.getPlayer().getPet(0);
        if (pet == null) {
            return false;
        }
        if (!pet.canConsume(itemId)) {
            pet = c.getPlayer().getPet(1);
            if (pet != null) {
                if (!pet.canConsume(itemId)) {
                    pet = c.getPlayer().getPet(2);
                    if (pet != null) {
                        if (!pet.canConsume(itemId)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        final byte petindex = c.getPlayer().getPetIndex(pet);
        pet.setFullness(100);
        if (pet.getCloseness() < 30000) {
            if (pet.getCloseness() + (100 * RateManager.TRAIT) > 30000) {
                pet.setCloseness(30000);
            } else {
                pet.setCloseness(pet.getCloseness() + (100 * RateManager.TRAIT));
            }
            if (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                pet.setLevel(pet.getLevel() + 1);
                c.getSession().write(PetPacket.showOwnPetLevelUp(c.getPlayer().getPetIndex(pet)));
                c.getPlayer().getMap().broadcastMessage(PetPacket.showPetLevelUp(c.getPlayer(), petindex));
            }
        }
        c.getSession().write(PetPacket.updatePet(pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), true));
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(c.getPlayer().getId(), (byte) 0, petindex, true, true), true);
        return true;
    }

    public static final void Pickup_Player(final LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        chr.updateTick(slea.readInt());
        c.getPlayer().setScrolledPosition((short) 0);
        slea.skip(1); // or is this before tick?
        final Point Client_Reportedpos = slea.readPos();
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (mapitem.getMeso() <= 0) {
                final boolean canShow;
                Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(mapitem.getItemId());
                if (questInfo != null && questInfo.getLeft() == mapitem.getQuest()) {
                    canShow = !chr.haveItem(mapitem.getItemId(), questInfo.getRight(), true, true);
                } else {
                    canShow = true;
                }
                //퀘스트 아이템은 필요 갯수를 초과하여 먹을 수 없음.
//                chr.dropMessage(6, MapleItemInformationProvider.getInstance().getName(mapitem.getItemId()) + " is can pickup ? " + canShow + " / questInfo : " + questInfo);
                if (!canShow) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
            }
            if (GameConstants.isPickupRestrictedMap(c.getPlayer().getMapId()) && mapitem.getOwner() != c.getPlayer().getId()) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            if (Distance > 5000 && (mapitem.getMeso() > 0 || mapitem.getItemId() != 4001025)) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_CLIENT, String.valueOf(Distance));
            } else if (chr.getPosition().distanceSq(mapitem.getPosition()) > 640000.0) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ITEMVAC_SERVER);
            }
            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null) {
                    final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    int givenMeso = 0;
                    for (MaplePartyCharacter pChr : chr.getParty().getMembers()) {
                        MapleCharacter otherChar = chr.getMap().getCharacterById(pChr.getId());
                        if (otherChar != null && otherChar.getId() != chr.getId()) {
                            toGive.add(otherChar);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        int meso = splitMeso / toGive.size();
                        m.gainMeso(meso, true);
                        givenMeso += meso;
                    }
                    chr.gainMeso(mapitem.getMeso() - givenMeso, true);
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
                removeItem(chr, mapitem, ob);
            } else {
                if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getPlayer().dropMessage(5, "이 아이템은 주울 수 없습니다.");
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem(c.getPlayer(), mapitem, ob);
                    //another hack
                    if (mapitem.getItemId() / 10000 == 291) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getCapturePosition(c.getPlayer().getMap()));
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.resetCapture());
                    }
                } else if (mapitem.getItemId() == 4039999) { //캐시 쿠폰
                    byte rand = (byte) Randomizer.rand(2, 10);
                    chr.modifyCSPoints(1, 100 * rand, false);
                    chr.getClient().getSession().write(MaplePacketCreator.showGainNx(100, rand));
                    removeItem(chr, mapitem, ob);
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else if (mapitem.getItemId() == 2439986 || mapitem.getItemId() == 2439987
                        || mapitem.getItemId() == 2439988 || mapitem.getItemId() == 2439989) {
                    int amount = 0;
                    switch (mapitem.getItemId()) {
                        case 2439986:
                            amount = 1000;
                            break;
                        case 2439987:
                            amount = 5000;
                            break;
                        case 2439988:
                            amount = 10000;
                            break;
                        case 2439989:
                            amount = 50000;
                            break;
                    }
                    chr.modifyCSPoints(3, amount, false);
                    chr.getClient().getSession().write(MaplePacketCreator.showGainACash(amount, (byte) 1));
                    removeItem(chr, mapitem, ob);
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else if (mapitem.getItemId() / 10000 != 291 && MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                    if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                        c.setMonitored(true); //hack check
                    }
                    if (mapitem.isPlayerDrop()) {
                        MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                        ServerLogger.getInstance().logTrade(LogType.Trade.DropAndPick, c.getPlayer().getId(), c.getPlayer().getName(), mapitem.getDropperName(), MapleItemInformationProvider.getInstance().getName(mapitem.getItem().getItemId()) + " " + mapitem.getItem().getQuantity() + "개", "맵 : " + c.getPlayer().getMapId());
                    } else {
                        // 플레이어 드랍이 아닐 때 옵션을 부여. -> 아이템 뿌릴 때 제대로 플레이어 드랍인지 아닌지 체크 바람
                        switch (mapitem.getItemId()) { //무조건 잠재 부여
                            case 1002357: //자쿰의 투구
                            case 1022114: //미카엘의 안경
                            case 1022115: //미카엘라의 안경
                            case 1122000: //혼테일의 목걸이
                            case 1032241: //데아 시두스 이어링
                            case 1012478: //응축된 힘의 결정석
                            case 1022231: //아쿠아틱 레터 눈장식    
                            case 1032077:
                            case 1032078:
                            case 1032079: //렉스의 이어링 시리즈  
                                MapleInventoryManipulator.checkEnhanced2(mapitem.getItem(), c.getPlayer());
                                break;
                        }
                        switch (mapitem.getItemId()) { //먹었을때 세부잠재 부여
                            case 1152134: { //견갑
                                Equip eq = (Equip) mapitem.getItem();
                                eq.setPotential1(20086);
                                eq.setPotential2(20086);
                                eq.setPotential3(20086);
                                Item it = eq.copy();
                                MapleInventoryManipulator.addFromDrop(c, it, true, mapitem.getDropper() instanceof MapleMonster);
                                break;
                            }
                            case 1012161: { //루돌프의 빛나는 코
                                Equip eq = (Equip) mapitem.getItem();
                                eq.setPotential1(30086);
                                eq.setPotential2(30086);
                                eq.setPotential3(30086);
                                Item it = eq.copy();
                                MapleInventoryManipulator.addFromDrop(c, it, true, mapitem.getDropper() instanceof MapleMonster);
                                break;
                            }
                            default:
                                MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                        }
                    }
                    /*if (mapitem.getItem().getItemId() == 5062000) {
                     Item itemz = mapitem.getItem();//
                     itemz.setUniqueId(MapleInventoryManipulator.getUniqueId(mapitem.getItem().getItemId(), null));
                     itemz.setFlag((short) (ItemFlag.KARMA_USE.getValue()));
                     MapleInventoryManipulator.addbyItem(c, itemz);
                     c.getSession().write(MaplePacketCreator.getShowItemGain(itemz.getItemId(), (byte) itemz.getQuantity()));
                     c.getSession().write(MaplePacketCreator.enableActions());
                     } else {
                     MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                     }*/ //봉인
                    removeItem(chr, mapitem, ob);
                    if (mapitem.getItemId() >= 2022570 && mapitem.getItemId() <= 2022584) { // 페페킹 아이템
                        for (MapleMapItem box : chr.getMap().getAllItemsThreadsafe()) {
                            if (box.getItemId() >= 2022570 && box.getItemId() <= 2022584) {
                                box.expire(chr.getMap());
                            }
                        }
                    }
                    if (mapitem.getItemId() >= 2028033 && mapitem.getItemId() <= 2028037) { // 페페킹 아이템
                        for (MapleMapItem box : chr.getMap().getAllItemsThreadsafe()) {
                            if (box.getItemId() >= 2028033 && box.getItemId() <= 2028037) {
                                box.expire(chr.getMap());
                            }
                        }
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static final void Pickup_Pet(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (chr.getTrade() != null) {//교환중일때 펫이 먹어서 가득차는 경우
            return;
        }
        if (c.getPlayer().hasBlockedInventory()) { //hack
            return;
        }
        if (!c.getPlayer().getMap().canPetPick()) { //restricted pet pickup - for event
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        final byte petz = (byte) slea.readInt();
        final MaplePet pet = chr.getPet(petz);
        slea.skip(1); // [4] Zero, [4] Seems to be tickcount, [1] Always zero
        chr.updateTick(slea.readInt()); //아마도 안쓸듯...
        final Point Client_Reportedpos = slea.readPos();
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null || pet == null) {
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        final Lock lock = mapitem.getLock();
        lock.lock();
        try {
            if (mapitem.isPickedUp()) {
                c.getSession().write(MaplePacketCreator.getInventoryFull());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && mapitem.isPlayerDrop()) {
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                return;
            }
            if (mapitem.getMeso() <= 0) {
                final int itemId = mapitem.getItemId();
                if (chr.isMagnetBanned(itemId)) { // 금지 아이템이면 펫 줍기 차단
                    return;
                }
                boolean peitem = false;
                if (mapitem.getItemId() >= 2022570 && mapitem.getItemId() <= 2022584) {
                    peitem = true;
                }
                if (mapitem.getItemId() >= 2028033 && mapitem.getItemId() <= 2028037) {
                    peitem = true;
                }
                if (peitem) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                final boolean canShow;
                Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(mapitem.getItemId());
                if (questInfo != null && questInfo.getLeft() == mapitem.getQuest()) {
                    canShow = !chr.haveItem(mapitem.getItemId(), questInfo.getRight(), true, true);
                } else {
                    canShow = true;
                }
                //퀘스트 아이템은 필요 갯수를 초과하여 먹을 수 없음.
//                chr.dropMessage(6, MapleItemInformationProvider.getInstance().getName(mapitem.getItemId()) + " is can pickup ? " + canShow + " / questInfo : " + questInfo);
                if (!canShow) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
            }
            final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
            if (Distance > 10000 && (mapitem.getMeso() > 0 || mapitem.getItemId() != 4001025)) {
                chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_CLIENT, String.valueOf(Distance));
            } else if (pet.getPos().distanceSq(mapitem.getPosition()) > 640000.0) {
                chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_SERVER);

            }

            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null) {
                    final List<MapleCharacter> toGive = new LinkedList<>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    int givenMeso = 0;
                    for (MaplePartyCharacter pChr : chr.getParty().getMembers()) {
                        MapleCharacter otherChar = chr.getMap().getCharacterById(pChr.getId());
                        if (otherChar != null && otherChar.getId() != chr.getId()) {
                            toGive.add(otherChar);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        int meso = splitMeso / toGive.size();
                        m.gainMeso(meso, true);
                        givenMeso += meso;
                    }
                    chr.gainMeso(mapitem.getMeso() - givenMeso, true);
                } else {
                    chr.gainMeso(mapitem.getMeso(), true);
                }
                removeItem_Pet(chr, mapitem, petz);
            } else {
                if (mapitem.getItemId() == 4039999) {
                    byte rand = (byte) Randomizer.rand(1, 5);
                    chr.modifyCSPoints(1, 100 * rand, false);
                    chr.getClient().getSession().write(MaplePacketCreator.showGainNx(100, rand));
                    removeItem_Pet(chr, mapitem, petz);
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else if (mapitem.getItemId() == 2439986 || mapitem.getItemId() == 2439987
                        || mapitem.getItemId() == 2439988 || mapitem.getItemId() == 2439989) {
                    int amount = 0;
                    switch (mapitem.getItemId()) {
                        case 2439986:
                            amount = 1000;
                            break;
                        case 2439987:
                            amount = 5000;
                            break;
                        case 2439988:
                            amount = 10000;
                            break;
                        case 2439989:
                            amount = 50000;
                            break;
                    }
                    chr.modifyCSPoints(3, amount, false);
                    chr.getClient().getSession().write(MaplePacketCreator.showGainACash(amount, (byte) 1));
                    removeItem_Pet(chr, mapitem, petz);
                    c.getSession().write(MaplePacketCreator.enableActions());
                } else if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId()) || mapitem.getItemId() / 10000 == 291) {
                    c.getSession().write(MaplePacketCreator.enableActions(false));
                } else if (useItem(c, mapitem.getItemId())) {
                    removeItem_Pet(chr, mapitem, petz);
                } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                    if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                        c.setMonitored(true); //hack check
                    }
                    if (mapitem.isPlayerDrop()) {
                        MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster);
                        ServerLogger.getInstance().logTrade(LogType.Trade.DropAndPick, c.getPlayer().getId(), c.getPlayer().getName(), mapitem.getDropperName(), MapleItemInformationProvider.getInstance().getName(mapitem.getItem().getItemId()) + " " + mapitem.getItem().getQuantity() + "개", "맵 : " + c.getPlayer().getMapId());
                    } else {
                        // 플레이어 드랍이 아닐 때 옵션을 부여. -> 아이템 뿌릴 때 제대로 플레이어 드랍인지 아닌지 체크 바람
                        switch (mapitem.getItemId()) { //무조건 잠재 부여
                            case 1002357: //자쿰의 투구
                            case 1022114: //미카엘의 안경
                            case 1022115: //미카엘라의 안경
                            case 1122000: //혼테일의 목걸이
                            case 1032241: //데아 시두스 이어링
                            case 1012478: //응축된 힘의 결정석
                            case 1022231: //아쿠아틱 레터 눈장식    
                            case 1032077:
                            case 1032078:
                            case 1032079: //렉스의 이어링 시리즈  
                                MapleInventoryManipulator.checkEnhanced2(mapitem.getItem(), c.getPlayer());
                                break;
                        }
                        switch (mapitem.getItemId()) { //먹었을때 세부잠재 부여
                            case 1152134: { //견갑
                                Equip eq = (Equip) mapitem.getItem();
                                eq.setPotential1(20086);
                                eq.setPotential2(20086);
                                eq.setPotential3(20086);
                                Item it = eq.copy();
                                MapleInventoryManipulator.addFromDrop(c, it, true, mapitem.getDropper() instanceof MapleMonster, false);
                                break;
                            }
                            case 1012161: { //루돌프의 빛나는 코
                                Equip eq = (Equip) mapitem.getItem();
                                eq.setPotential1(30086);
                                eq.setPotential2(30086);
                                eq.setPotential3(30086);
                                Item it = eq.copy();
                                MapleInventoryManipulator.addFromDrop(c, it, true, mapitem.getDropper() instanceof MapleMonster, false);
                                break;
                            }
                            default:
                                MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster, false);
                        }
                    }
                    /*if (mapitem.getItem().getItemId() == 5062000) {
                     Item itemz = mapitem.getItem();//
                     itemz.setUniqueId(MapleInventoryManipulator.getUniqueId(mapitem.getItem().getItemId(), null));
                     itemz.setFlag((short) (ItemFlag.KARMA_USE.getValue()));
                     MapleInventoryManipulator.addbyItem(c, itemz);
                     c.getSession().write(MaplePacketCreator.getShowItemGain(itemz.getItemId(), (byte) itemz.getQuantity()));
                     c.getSession().write(MaplePacketCreator.enableActions());
                     } else {
                     MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster, false);
                     }*///봉인
                    if (mapitem.isPlayerDrop()) {
                        ServerLogger.getInstance().logTrade(LogType.Trade.DropAndPick, c.getPlayer().getId(), c.getPlayer().getName(), mapitem.getDropperName(), MapleItemInformationProvider.getInstance().getName(mapitem.getItem().getItemId()) + " " + mapitem.getItem().getQuantity() + "개", "맵 : " + c.getPlayer().getMapId());
                    }
                    removeItem_Pet(chr, mapitem, petz);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static final boolean useItem(final MapleClient c, final int id) {
        if (GameConstants.isUse(id)) { // TO prevent caching of everything, waste of mem
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleStatEffect eff = ii.getItemEffect(id);
            if (eff == null) {
                return false;
            }
            final int consumeval = eff.getConsume();

            if (consumeval > 0) {
                consumeItem(c, eff);
                consumeItem(c, ii.getItemEffectEX(id));
                c.getSession().write(MaplePacketCreator.getShowItemGain(id, (byte) 1));
                return true;
            }
        }
        return false;
    }

    public static final void consumeItem(final MapleClient c, final MapleStatEffect eff) {
        if (eff == null) {
            return;
        }
        if (eff.getConsume() == 2) {
            if (c.getPlayer().getParty() != null && c.getPlayer().isAlive()) {
                for (final MaplePartyCharacter pc : c.getPlayer().getParty().getMembers()) {
                    final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pc.getId());
                    if (chr != null && chr.isAlive()) {
                        eff.applyTo(chr);
                    }
                }
            } else {
                eff.applyTo(c.getPlayer());
            }
        } else if (c.getPlayer().isAlive()) {
            eff.applyTo(c.getPlayer());
        }
    }

    public static final void removeItem_Pet(final MapleCharacter chr, final MapleMapItem mapitem, int pet) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), pet));
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static final void removeItem(final MapleCharacter chr, final MapleMapItem mapitem, final MapleMapObject ob) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
        chr.getMap().removeMapObject(ob);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    private static final void addMedalString(final MapleCharacter c, final StringBuilder sb) {
        final Item medal = c.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
        if (medal != null) { // Medal
            sb.append("<");
            sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()).replace("의 훈장", ""));
            sb.append("> ");
        }
    }

    private static final boolean getIncubatedItems(MapleClient c, int itemId) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 3 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 1) {
            c.getPlayer().dropMessage(5, "장비창 1개, 소비창 3개, 기타창 1개의 공간이 필요합니다.");
            return false;
        }

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        String invoked = null;
        if (itemId != 4170023) {
            try {
                invoked = (String) EtcScriptInvoker.getInvocable("etc/incubator.js").invokeFunction("run", (itemId % 10));
                String[] ids = invoked.split(",");
                if (ids.length < 4) {
                    c.getPlayer().dropMessage(1, "현재 부화기를 사용할 수 없는 기간입니다.");
                    return false;
                }
                ServerLogger.getInstance().logItem(LogType.Item.Incubator, c.getPlayer().getId(), c.getPlayer().getName(), Integer.parseInt(ids[2]), Integer.parseInt(ids[3]), ii.getName(Integer.parseInt(ids[2])), 0, "[피그미 알 : " + itemId + "]");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, Integer.parseInt(ids[2]), c.getPlayer().getName() + "님이 피그미 에그에서 " + "[{" + "}]" + "를 얻었습니다."));
                MapleInventoryManipulator.addById(c, Integer.parseInt(ids[2]), (short) Integer.parseInt(ids[3]), ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
                c.getSession().write(MaplePacketCreator.getIncubatorResult(Integer.parseInt(ids[2]), (short) Integer.parseInt(ids[3]), 0, (short) 0, 5060002));
                return true;
            } catch (Exception e) {
                System.err.println("Error executing Etc script. Path: " + "etc/incubator.js" + "\nException " + e);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Etc script. Path: " + "etc/incubator.js" + "\nException " + e);
                return false;
            }
        } else {
            try {
                invoked = (String) EtcScriptInvoker.getInvocable("etc/peanutMachine.js").invokeFunction("run", 0);
                String[] ids = invoked.split(",");
                if (ids.length < 4) {
                    c.getPlayer().dropMessage(1, "현재 피넛 머신을 사용할 수 없는 기간입니다.");
                    return false;
                }
                ServerLogger.getInstance().logItem(LogType.Item.Incubator, c.getPlayer().getId(), c.getPlayer().getName(), Integer.parseInt(ids[2]), Integer.parseInt(ids[3]), ii.getName(Integer.parseInt(ids[2])), 0, "[땅콩 : " + itemId + "]");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, Integer.parseInt(ids[2]), c.getPlayer().getName() + "님이 땅콩에서 " + "[{" + "}]" + "를 얻었습니다."));
                if (GameConstants.getInventoryType(Integer.parseInt(ids[2])) == MapleInventoryType.EQUIP) {
                    final Equip toGive = (Equip) ii.getEquipById(Integer.parseInt(ids[2]));
                    toGive.resetPotential_Fuse(0);
                    MapleInventoryManipulator.addbyItem(c, toGive);
                } else {
                    MapleInventoryManipulator.addById(c, Integer.parseInt(ids[2]), (short) Integer.parseInt(ids[3]), c.getPlayer().getName() + ": " + ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
                }
                ServerLogger.getInstance().logItem(LogType.Item.Incubator, c.getPlayer().getId(), c.getPlayer().getName(), Integer.parseInt(ids[4]), Integer.parseInt(ids[5]), ii.getName(Integer.parseInt(ids[4])), 0, "[땅콩 : " + itemId + "]");
                if (GameConstants.getInventoryType(Integer.parseInt(ids[4])) == MapleInventoryType.EQUIP) {
                    final Equip toGive = (Equip) ii.getEquipById(Integer.parseInt(ids[4]));
                    toGive.resetPotential_Fuse(0);
                    MapleInventoryManipulator.addbyItem(c, toGive);
                } else {
                    MapleInventoryManipulator.addById(c, Integer.parseInt(ids[4]), (short) Integer.parseInt(ids[5]), ii.getName(itemId) + " on " + FileoutputUtil.CurrentReadable_Date());
                }
                c.getSession().write(MaplePacketCreator.getIncubatorResult(Integer.parseInt(ids[2]), (short) Integer.parseInt(ids[3]), Integer.parseInt(ids[4]), (short) Integer.parseInt(ids[5]), 5060003));
                return true;
            } catch (Exception e) {
                System.err.println("Error executing Etc script. Path: " + "etc/incubator.js" + "\nException " + e);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Etc script. Path: " + "etc/incubator.js" + "\nException " + e);
                return false;
            }
        }
    }

    public static final void OwlMinerva(final LittleEndianAccessor slea, final MapleClient c) {
        final byte slot = (byte) slea.readShort();
        final int itemid = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && itemid == 2310000 && !c.getPlayer().hasBlockedInventory()) {
            final int itemSearch = slea.readInt();
            List<AbstractPlayerStore> hms = new LinkedList<AbstractPlayerStore>();
            MinervaOwlSearchTop.getInstance().searchItem(itemSearch);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                hms = cserv.searchShop(itemSearch, hms);
            }
            if (hms.size() > 0) {
                c.getSession().write(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
            } else {
                c.getPlayer().dropMessage(1, "아이템을 발견하지 못하였습니다.");
            }
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void Owl(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().haveItem(5230000, 1, true, true) || c.getPlayer().haveItem(2310000, 1, true, true)) {
            if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022) {
                c.getSession().write(MaplePacketCreator.getOwlOpen());
            } else {
                c.getPlayer().dropMessage(5, "자유시장 내에서만 사용할 수 있습니다.");
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }
    }

    public static final int OWL_ID = 2; //don't change. 0 = owner ID, 1 = store ID, 2 = object ID

    public static final void OwlWarp(final LittleEndianAccessor slea, final MapleClient c) {
        c.getSession().write(MaplePacketCreator.enableActions());
        if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022 && !c.getPlayer().hasBlockedInventory()) {
            final int id = slea.readInt();
            final int map = slea.readInt();
            if (map >= 910000001 && map <= 910000022) {
                final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(map);
                c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                AbstractPlayerStore merchant = null;
                List<MapleMapObject> objects;
                switch (OWL_ID) {
                    case 0:
                        boolean bln = false;
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getOwnerId() == id) {
                                        merchant = merch;
                                        bln = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!bln) {
                            List<MapleCharacter> objs = mapp.getCharactersThreadsafe();
                            for (MapleCharacter chr : objs) {
                                if (chr.getPlayerShop() != null && chr.getMapId() == map) {
                                    if (chr.getPlayerShop() instanceof MaplePlayerShop) {
                                        MaplePlayerShop shop = (MaplePlayerShop) chr.getPlayerShop();
                                        if (shop.isOpen()) {
                                            if (shop.getOwnerId() == id) {
                                                merchant = shop;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case 1:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getStoreId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        final MapleMapObject ob = mapp.getMapObject(id, MapleMapObjectType.HIRED_MERCHANT);
                        if (ob instanceof IMaplePlayerShop) {
                            final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                            if (ips instanceof HiredMerchant) {
                                merchant = (HiredMerchant) ips;
                            }
                        }
                        final MapleMapObject ob2 = mapp.getMapObject(id, MapleMapObjectType.SHOP);
                        if (ob2 instanceof IMaplePlayerShop) {
                            final IMaplePlayerShop ips = (IMaplePlayerShop) ob2;
                            if (ips instanceof MaplePlayerShop) {
                                merchant = (MaplePlayerShop) ips;
                            }
                        }
                        break;
                }
                if (merchant != null) {
                    if (merchant instanceof HiredMerchant) {
                        if (merchant.isOwner(c.getPlayer())) {
                            HiredMerchant merc = (HiredMerchant) merchant;
                            merc.setOpen(false);
                            merc.removeAllVisitors((byte) 16, (byte) 0);
                            c.getPlayer().setPlayerShop(merc);
                            c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merc, false));
                        } else {
                            HiredMerchant merc = (HiredMerchant) merchant;
                            if (!merc.isOpen() || !merc.isAvailable()) {
                                c.getPlayer().dropMessage(1, "상점이 준비중에 있습니다. 잠시 후에 다시 시도해 주세요.");
                            } else {
                                if (merc.getFreeSlot() == -1) {
                                    c.getPlayer().dropMessage(1, "상점 최대 수용 인원을 초과하였습니다.");
                                } else if (merc.isInBlackList(c.getPlayer().getName())) {
                                    c.getPlayer().dropMessage(1, "당신은 이 상점에 입장이 금지되었습니다.");
                                } else {
                                    c.getPlayer().setPlayerShop(merc);
                                    merc.addVisitor(c.getPlayer());
                                    c.getSession().write(PlayerShopPacket.getHiredMerch(c.getPlayer(), merc, false));
                                }
                            }
                        }
                    } else if (merchant instanceof MaplePlayerShop) {
                        if (((MaplePlayerShop) merchant).isBanned(c.getPlayer().getName())) {
                            c.getPlayer().dropMessage(1, "당신은 이 상점에 입장이 금지되었습니다.");
                            return;
                        } else {
                            if (merchant.getFreeSlot() < 0 || merchant.getVisitorSlot(c.getPlayer()) > -1 || !merchant.isOpen() || !merchant.isAvailable()) {
                                c.getSession().write(PlayerShopPacket.getMiniGameFull());
                            } else {
                                c.getPlayer().setPlayerShop(merchant);
                                merchant.addVisitor(c.getPlayer());
                                c.getSession().write(PlayerShopPacket.getPlayerStore(c.getPlayer(), false));
                            }
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(1, "상점을 발견하지 못하였습니다.");
                }
            }
        }
    }

    public static final void PamSong(LittleEndianAccessor slea, MapleClient c) {
        final Item pam = c.getPlayer().getInventory(MapleInventoryType.CASH).findById(5640000);
        if (slea.readByte() > 0 && c.getPlayer().getScrolledPosition() != 0 && pam != null && pam.getQuantity() > 0) {
            final MapleInventoryType inv = c.getPlayer().getScrolledPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
            final Item item = c.getPlayer().getInventory(inv).getItem(c.getPlayer().getScrolledPosition());
            c.getPlayer().setScrolledPosition((short) 0);
            if (item != null) {
                final Equip eq = (Equip) item;
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + 1));
                c.getPlayer().forceReAddItem_Flag(eq, inv);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, pam.getPosition(), (short) 1, true, false);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.pamsSongEffect(c.getPlayer().getId()));
            }
        } else {
            c.getPlayer().setScrolledPosition((short) 0);
        }
    }

    public static final void TeleRock(LittleEndianAccessor slea, MapleClient c) {
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 232 || c.getPlayer().hasBlockedInventory()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        boolean used = UseTeleRock(slea, c, itemId);
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final boolean UseTeleRock(LittleEndianAccessor slea, MapleClient c, int itemId) {
        if (c.getPlayer().checkCooltime(itemId)) {
            c.getPlayer().dropMessage(1, "다음 순간이동의 돌 사용까지 남은시간: " + c.getPlayer().remainingCooltime(itemId));
            return true;
        }
        if (c.getPlayer().getMapId() / 100000 == 1100 || c.getPlayer().getMapId() >= 190000000 && c.getPlayer().getMapId() <= 198000000) {
            c.getSession().write(MaplePacketCreator.enableActions());
            c.getPlayer().dropMessage(6, "여기선 돌 금지야! 금지!!");
            return true;
        }
        boolean used = false;
        if (itemId == 5041001 || itemId == 5040004) {
            // slea.readByte(); //useless //프리미엄 고성
        }
        int realItemId = itemId;
        if (itemId == 5041001) {
            itemId = 5041000;
        }
        int mmap = c.getPlayer().getMapId();
        if (mmap >= 190000000 && mmap <= 198000000) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방 도둑을 드디어 찾은것 같습니다."));
            return true;
        }
        if (slea.readByte() == 0) { // Rocktype
            final MapleMap target = c.getChannelServer().getMapFactory().getMap(slea.readInt());
            if (target.getId() >= 190000000 && target.getId() <= 198000000) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방 도둑을 드디어 찾은것 같습니다. 내 돈 돌려주세요....."));
                return true;
            }
            if (((itemId == 5041000 && c.getPlayer().isRockMap(target.getId())) || (itemId != 5041000 && c.getPlayer().isRegRockMap(target.getId()) && (target.getId() / 100000000) == (c.getPlayer().getMapId() / 100000000)) || ((itemId == 5040004 || itemId == 5041001) && (c.getPlayer().isHyperRockMap(target.getId()) || GameConstants.isHyperTeleMap(target.getId())))) && target.getId() >= 100000000) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(target.getFieldLimit()) && !c.getPlayer().isInBlockedMap()) { //Makes sure this map doesn't have a forced return map
                    if ((target.getId() == 110000000 || c.getPlayer().getMapId() == 110000000) && itemId == 5040000) {
                        int tmap = target.getId();
                        int retmap = c.getPlayer().getSavedLocation(SavedLocationType.FLORINA);
                        if (tmap / 100000000 != retmap / 100000000) {
                            c.getPlayer().changeMap(target, target.getPortal(0));
                            used = true;
                        }
                    } else {
                        c.getPlayer().changeMap(target, target.getPortal(0));
                        used = true;
                    }
                }
            }
        } else {
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
            if (victim != null && !victim.isIntern() && c.getPlayer().getEventInstance() == null && victim.getEventInstance() == null) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(c.getChannelServer().getMapFactory().getMap(victim.getMapId()).getFieldLimit()) && !victim.isInBlockedMap() && !c.getPlayer().isInBlockedMap()) {
                    if ((itemId == 5041000 || itemId == 5040004 || itemId == 5041001 || (victim.getMapId() / 100000000) == (c.getPlayer().getMapId() / 100000000)) && victim.getMapId() >= 100000000) { // Viprock or same continent
                        if (itemId == 5040000) {
                            if (victim.getMapId() == 110000000 || c.getPlayer().getMapId() == 110000000) {
                                int tmap = victim.getMapId();
                                int retmap = c.getPlayer().getSavedLocation(SavedLocationType.FLORINA);
                                if (tmap / 100000000 != retmap / 100000000) {
                                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                    used = true;
                                }
                            } else {
                                int tmap = victim.getMapId();
                                if (mmap >= 190000000 && mmap <= 198000000) {
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방 도둑을 드디어 찾은것 같습니다."));
                                    return true;
                                }
                                if (tmap >= 190000000 && tmap <= 198000000) {
                                    c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방비 얼마나 한다고 이걸......ㅡㅡ"));
                                    return true;
                                }
                                switch (tmap / 10000000) {
                                    case 20:
                                    case 21:
                                    case 23:
                                    case 22:
                                        if (tmap / 100000000 == mmap / 100000000 && (tmap / 10000000 == 20 || tmap / 10000000 == 21 || tmap / 10000000 == 22 || tmap / 10000000 == 23)) {
                                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                            used = true;
                                        }
                                        break;
                                    case 24:
                                    case 25:
                                    case 26:
                                        if (tmap / 10000000 == mmap / 10000000) {
                                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                            used = true;
                                        }
                                        break;
                                    default:
                                        c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                                        used = true;
                                        break;
                                }
                            }
                        } else {
                            if (victim.getMapId() >= 190000000 && victim.getMapId() <= 198000000) {
                                c.getSession().write(MaplePacketCreator.serverNotice(1, "피시방비 얼마나 한다고 이걸......ㅡㅡ\r\n 안돼!! 돌아가"));
                                return true;
                            }
                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                            used = true;
                        }
                    }
                }
            }
        }
        boolean bln = (used && itemId != 5041001 && itemId != 5040004);
        if (!bln) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "이동할 수 없습니다."));
        }
        if (used) {
            if (realItemId != 5041001) {
                //c.getPlayer().cooltime(itemId, (long) 30 * 60 * 1000);
            }
        }
        return bln;
    }

    public static void useRemoteHiredMerchant(LittleEndianAccessor slea, MapleClient c) {
        short slot = slea.readShort();
        Item item = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null) {
            c.getSession().close(); //hack
            return;
        }
        if (item.getItemId() != 5470000 || item.getQuantity() <= 0) {
            c.getSession().close(); //hack
            return;
        }
        boolean use = false;

        HiredMerchant merchant = c.getChannelServer().findAndGetMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());

        if (merchant == null) {
            c.getPlayer().dropMessage(1, "현재 채널에서 열려있는 고용상점이 없습니다.");
            return;
        }

        if (FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
            c.getPlayer().dropMessage(1, "이곳에서는 사용할 수 없습니다.");
            return;
        }

        MapleCharacter chr = c.getPlayer();

        if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
            merchant.setOpen(false);
            merchant.broadcastToVisitors(MaplePacketCreator.serverNotice(1, "고용상점이 점검중에 있습니다. 나중에 다시 이용해 주세요."));
            merchant.removeAllVisitors((byte) 16, (byte) -2);
            chr.setPlayerShop(merchant);
            c.getSession().write(PlayerShopPacket.getHiredMerch(chr, merchant, false));
            use = true;
        }
    }

    /*
     [R] C4 00 9B 1B 00 00 00
     퀘스트 포트 열기(타입 0)
     
     [R] C4 00 9B 1B 00 00 01
     퀘스트 포트 열기(타입 1)
     
     [R] C3 00 9B 1B 
     
     
     */
    public static void QuestPotOpen(LittleEndianAccessor slea, MapleClient c) {
        int qid = slea.readUShort();
        MapleQuest q = MapleQuest.getInstance(qid);
        if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
            MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
            if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                return;
            }
            c.getSession().write(MaplePacketCreator.updateQuest(qs));
            if (c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(6, "" + qs.getCustomData());
            }
        }
    }

    /*
     [R] C5 00 03 00 F8 85 3D 00 9B 1B 00 00 64 00 00 00
     퀘스트 포트 먹이 주기
     */
    public static void QuestPotFeed(LittleEndianAccessor slea, MapleClient c) {
        short slot = slea.readShort();
        int itemid = slea.readInt();
        int qid = slea.readUShort();
        slea.skip(2);
        int value = slea.readInt();
        Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        if (item == null || item.getItemId() != itemid || item.getQuantity() <= 0) {
            return;
        }
        if (qid == 7067) {
            if (qid != 7067) { // 작전 3단계 : 아기새
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }

            MapleQuest q = MapleQuest.getInstance(qid);
            if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    return;
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                if (feeds < 3000) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                    feeds = Math.min(feeds + 100, 3000);
                    qs.setCustomData(feeds + "");
                    if (feeds == 3000) {
                        c.sendPacket(MaplePacketCreator.getShowQuestCompletion(3250));
                    }
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        } else if (qid == 7691) {
            if (!c.getPlayer().haveItem(4220045)) {
                c.getPlayer().dropMessage(6, "상자를 구매한뒤 사용해 주세요.");
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            if (qid != 7691) { //5주년 케이크
                c.sendPacket(MaplePacketCreator.enableActions());
                return;
            }
            MapleQuest q = MapleQuest.getInstance(qid);
            if (c.getPlayer().getQuestNoAdd(q) == null) {
                q.forceStart(c.getPlayer(), 9000021, "0");
            }
            if (q != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    qs.setCustomData("0");
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                if (feeds < 500) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                    feeds = (feeds + value);
                    qs.setCustomData(feeds + "");
                } else if (feeds == 500) {
                    //c.sendPacket(MaplePacketCreator.getShowQuestCompletion(9983));
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        } else if (qid == 21763) {
            MapleQuest q = MapleQuest.getInstance(qid);
            if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    return;
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                if (feeds < 800) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                    feeds = Math.min(feeds + value, 3000);
                    qs.setCustomData(feeds + "");
                    if (feeds == 800) {
                        //c.sendPacket(MaplePacketCreator.getShowQuestCompletion(21763));
                    }
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        } else if (qid == 10430) {
            MapleQuest q = MapleQuest.getInstance(qid);
            if (q != null && c.getPlayer().getQuestNoAdd(q) != null) {
                MapleQuestStatus qs = c.getPlayer().getQuestNoAdd(q);
                if (qs.getCustomData() == null || qs.getCustomData().isEmpty()) {
                    return;
                }
                int feeds = Integer.parseInt(qs.getCustomData());
                if (feeds < 800) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
                    feeds = Math.min(feeds + value, 3000);
                    qs.setCustomData(feeds + "");
                    if (feeds == 800) {
                        //c.sendPacket(MaplePacketCreator.getShowQuestCompletion(21763));
                    }
                }
                c.getSession().write(MaplePacketCreator.updateQuest(qs));
            }
        }
        c.sendPacket(MaplePacketCreator.enableActions());
    }

    public static final void ChoosePqReward(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        byte type = slea.readByte();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int itemid = 0;
        int itemType = Randomizer.rand(1, 100);
        if (itemType <= 30) {//포션 30퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultC();
            }
            //chr.dropMessage(6, "potion: " + itemid);
        } else if (itemType <= 60) {//장비 30퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultE();
            }
            //chr.dropMessage(6, "equip: " + itemid);
        } else if (itemType <= 80) {//미라클 큐브 20퍼센트
            itemid = 5062000;
            //chr.dropMessage(6, "equip: " + itemid);
        } else if (itemType <= 90) {//줌서 10퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultS();
            }
            //chr.dropMessage(6, "scroll: " + itemid);
        } else if (itemType <= 100) {//촉진제 10퍼센트
            while (!ii.itemExists(itemid)) {
                itemid = RandomRewards.getPQResultEtc();
            }
            //chr.dropMessage(6, "etc: " + itemid);
        }

        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            final Item nEquip = ii.getEquipById(itemid);
            if (chr.getMapId() == 910340500) {//커닝파퀘
                if (ii.getReqLevel(itemid) > 30) {//다시
                    while (ii.getReqLevel(itemid) > 30 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 922010900) {//루디파퀘
                if (ii.getReqLevel(itemid) > 40 || ii.getReqLevel(itemid) <= 30 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 40 || ii.getReqLevel(itemid) <= 30 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 930000600) {//독안개의숲
                if (ii.getReqLevel(itemid) > 50 || ii.getReqLevel(itemid) <= 40 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 50 || ii.getReqLevel(itemid) <= 40 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 926100401 || chr.getMapId() == 926110401) {//중앙 연구실
                if (ii.getReqLevel(itemid) > 90 || ii.getReqLevel(itemid) <= 70 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 90 || ii.getReqLevel(itemid) <= 70 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 921120500) {//만년 빙하동굴
                if (ii.getReqLevel(itemid) > 100 || ii.getReqLevel(itemid) <= 80 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 100 || ii.getReqLevel(itemid) <= 80 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            } else if (chr.getMapId() == 240080800) {//천공의 둥지
                if (ii.getReqLevel(itemid) > 110 || ii.getReqLevel(itemid) <= 80 || !ii.itemExists(itemid)) {//다시
                    while (ii.getReqLevel(itemid) > 110 || ii.getReqLevel(itemid) <= 80 || !ii.itemExists(itemid)) {
                        itemid = RandomRewards.getPQResultE();
                    }
                }
            }
        }
        if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 1
                || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < 1) {
            c.getSession().write(MaplePacketCreator.recievePQrewardFail((byte) 2));
            return;
        } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1) {
            if (Randomizer.rand(1, 100) <= 50 && GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
                //c.getSession().write(MaplePacketCreator.selectPQrewardSuccess(chr.getId(),chr.getName(),(byte) type));
                c.getSession().write(MaplePacketCreator.recievePQrewardSuccess((byte) type, itemid, true));
                MapleInventoryManipulator.addByIdPotential(c, itemid, ((short) 1), null, null, "파티퀘스트", true);//잠재 on!
            } else {
                //c.getSession().write(MaplePacketCreator.selectPQrewardSuccess(chr.getId(),chr.getName(),(byte) type));
                c.getSession().write(MaplePacketCreator.recievePQrewardSuccess((byte) type, itemid, false));
                MapleInventoryManipulator.addByIdPotential(c, itemid, (isPotion(itemid) ? (short) 30 : itemid == 5062000 ? (short) 3 : (short) 1), null, null, "파티퀘스트", false);
            }
        }
    }

    private static final boolean isPotion(final int itemId) {
        if (itemId >= 2000000 && itemId <= 2020015) {
            return true;
        }
        return false;
    }
}
