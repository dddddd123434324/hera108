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

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.PlayerStats;
import client.RockPaperScissors;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import handling.SendPacketOpcode;
import scripting.NPCConversationManager;
import scripting.NPCScriptManager;
import scripting.vm.NPCScriptInvoker;
import scripting.vm.NPCScriptVirtualMachine;
import server.*;
import server.life.MapleNPC;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.log.LogType;
import server.log.ServerLogger;

public class NPCHandler {

    private static PlayerStats stats;

    public static final void NPCAnimation(final LittleEndianAccessor slea, final MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
        final int length = (int) slea.available();
        if (length == (6)) { // NPC Talk
            mplew.writeInt(slea.readInt());
            mplew.writeShort(slea.readShort());
        } else if (length > (6)) { // NPC Move
            mplew.write(slea.read(length - 9));
        } else {
            return;
        }
        c.getSession().write(mplew.getPacket());
    }

    public static final void NPCShop(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte bmode = slea.readByte();
        if (chr == null) {
            return;
        }

        switch (bmode) {
            case 0: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final short select = slea.readShort();
                final int itemId = slea.readInt();
                final short quantity = slea.readShort();
                shop.buy(c, itemId, quantity, select);
                break;
            }
            case 1: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final byte slot = (byte) slea.readShort();
                final int itemId = slea.readInt();
                final short quantity = slea.readShort();
                shop.sell(c, GameConstants.getInventoryType(itemId), slot, quantity);
                break;
            }
            case 2: {
                final MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                final byte slot = (byte) slea.readShort();
                shop.recharge(c, slot);
                break;
            }
            default:
                chr.setConversation(0);
                break;
        }
    }

    public static final void NPCTalk(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleNPC npc = chr.getMap().getNPCByOid(slea.readInt());

        if (npc == null) {
            return;
        }
        if (c.getPlayer().isGM()) {
            c.getPlayer().dropMessage(6, "[NPC] : " + npc.getId());
        }
        if (chr.hasBlockedInventory()) {
//            chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
            return;
        }
        //c.getPlayer().updateTick(slea.readInt());
        if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(c);
        } else {
            if (NPCScriptInvoker.runNpc(c, npc.getId(), npc.getObjectId()) != 0) {
                NPCScriptManager.getInstance().start(c, npc.getId(), null, npc.getObjectId());
            }
        }
    }

    public static final void QuestAction(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte action = slea.readByte();
        int quest = slea.readUShort();
        if (chr == null) {
            return;
        }
        final MapleQuest q = MapleQuest.getInstance(quest);
        if (chr.isGM()) {
            chr.dropMessage(6, "퀘스트 : " + q.getName() + " / 코드 : " + q.getId() + " / action : " + action);
        }
        switch (action) {
            case 0: { // Restore lost item
                //chr.updateTick(slea.readInt());
                slea.readInt();
                final int itemid = slea.readInt();
                q.RestoreLostItem(chr, itemid);
                break;
            }
            case 1: { // Start Quest
                final int npc = slea.readInt();
                if (!q.hasStartScript()) {
                    q.start(chr, npc);
                }
                break;
            }
            case 2: { // Complete Quest
                /*if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() <= 2
                 || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() <= 2) {
                 chr.dropMessage(1, "버그 악용 방지를 위해 모든 인벤토리칸을 각각 3칸이상 비워 주세요.");
                 return;
                 }*/
                
                if (q.getId() == 20616) { //시그너스의 코히누르
                    if (c.getPlayer().getInvSlots(1) <= 1) {
                            chr.dropMessage(6, "장비 아이템 칸이 부족합니다. 1칸 이상 비워 주세요.");
                            return;
                        } else {
                    chr.gainItem(1112597, (short) 1);
                    chr.dropMessage(6, "여제의 은총 퀘스트를 클리어하여 시그너스의 코히누르를 획득하였습니다.");
                    chr.dropMessage(6, "또한 기사의 전당의 키단으로부터 이 캐릭터의 레벨을 200으로 만들어 동결시킬 수 있습니다.");
                }
                }
                
                final int npc = slea.readInt();
                if (q.hasEndScript()) {
                    return;
                }
                if (slea.available() >= 8) {
                    slea.readInt(); //tick
                    q.complete(chr, npc, slea.readInt());
                } else {
                    q.complete(chr, npc, slea.readInt());
                }
                // c.getSession().write(MaplePacketCreator.completeQuest(c.getPlayer(), quest));
                //c.getSession().write(MaplePacketCreator.updateQuestInfo(c.getPlayer(), quest, npc, (byte)14));
                // 6 = start quest
                // 7 = unknown error
                // 8 = equip is full
                // 9 = not enough mesos
                // 11 = due to the equipment currently being worn wtf o.o
                // 12 = you may not posess more than one of this item
                break;
            }
            case 3: { // Forefit Quest
                if (GameConstants.canForfeit(q.getId())) {
                    q.forfeit(chr);
                } else {
                    chr.dropMessage(1, "이 퀘스트는 포기하실 수 없습니다.");
                }
                break;
            }

            case 4: { // Scripted Start Quest
                final int npc = slea.readInt();
                if (chr.hasBlockedInventory()) {
//                    chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
                    return;
                }
//                c.getPlayer().updateTick(slea.readInt());
                NPCScriptManager.getInstance().startQuest(c, npc, quest);
                break;
            }
            case 5: { // Scripted End Quest
                final int npc = slea.readInt();
                if (chr.hasBlockedInventory()) {
//                    chr.dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
                    return;
                }
//                c.getPlayer().updateTick(slea.readInt());
                NPCScriptManager.getInstance().endQuest(c, npc, quest, false);
                break;
            }
        }
    }

    public static final void Storage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte mode = slea.readByte();
        if (chr == null) {
            return;
        }
        final MapleStorage storage = chr.getStorage();

        switch (mode) {
            case 4: { // Take Out
                final byte type = slea.readByte();
                final byte slot = storage.getSlot(MapleInventoryType.getByType(type), slea.readByte());
                final Item item = storage.takeOut(slot);

                if (item != null) {
                    if (!MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                        storage.store(item);
                        c.getSession().write(MaplePacketCreator.getInventoryFull());
                    } else {
                        if (c.getPlayer().getMapId() == 910000000) {
                            if (c.getPlayer().getMeso() < 1000) {
                                c.getSession().write(MaplePacketCreator.getStorageNotEnoughMeso());
                                storage.store(item);
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                            chr.gainMeso(-1000, false);
                        }
                        MapleInventoryManipulator.addFromDrop(c, item, false);
                        storage.sendTakenOut(c, GameConstants.getInventoryType(item.getItemId()));
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                break;
            }
            case 5: { // Store
                final byte slot = (byte) slea.readShort();
                final int itemId = slea.readInt();
                MapleInventoryType type = GameConstants.getInventoryType(itemId);
                short quantity = slea.readShort();
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (quantity < 1) {
                    if (quantity != 0) {
                        AutobanManager.getInstance().autoban(c, "Trying to store " + quantity + " of " + itemId);
                    }
                    return;
                }
                if (storage.isFull()) {
                    c.getSession().write(MaplePacketCreator.getStorageFull());
                    return;
                }
                if (chr.getInventory(type).getItem(slot) == null) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }

                if (chr.getMeso() < (c.getPlayer().getMapId() == 910000000 ? 500 : 100)) {
                    c.getSession().write(MaplePacketCreator.getStorageNotEnoughMeso());
                } else {
                    Item item = chr.getInventory(type).getItem(slot).copy();

                    if (GameConstants.isPet(item.getItemId())) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    final short flag = item.getFlag();
                    if (ii.isPickupRestricted(item.getItemId()) && storage.findById(item.getItemId()) != null) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (item.getItemId() == itemId && (item.getQuantity() >= quantity || GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId))) {
                        if (ii.isDropRestricted(item.getItemId())) {
                            if (ItemFlag.KARMA_EQ.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_EQ.getValue()));
                                //item.setFlag((short) (item.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
                            } else if (ItemFlag.KARMA_USE.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_USE.getValue()));
                            } else if (ItemFlag.KARMA_ACC.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_ACC.getValue()));
                            } else if (ItemFlag.KARMA_ACC_USE.check(flag)) {
                                item.setFlag((short) (flag - ItemFlag.KARMA_ACC_USE.getValue()));
                            } else {
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                        }
                        if (GameConstants.isAccountSharableOnce(itemId)) {
                            item.setFlag((short) (flag | ItemFlag.KARMA_EQ.getValue()));
                        }
                        if (GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId)) {
                            quantity = item.getQuantity();
                        }
                        chr.gainMeso(-(c.getPlayer().getMapId() == 910000000 ? 500 : 100), false, false);
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                        item.setQuantity(quantity);
                        ServerLogger.getInstance().logStorage(LogType.Item.Stroage, "☆창고에 넣음☆ / " + chr.getName() + " (Lv. " + chr.getLevel() + ") / 직업 : " + chr.getJob() + " / " + MapleItemInformationProvider.getInstance().getName(item.getItemId()) + " (코드 : " + item.getItemId() + ") " + quantity + "개 / 강화 : " + item.getOwner(), chr.getAccountID());
                        storage.store(item);
                    } else {
                        AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store non-matching itemid (" + itemId + "/" + item.getItemId() + ") or quantity not in posession (" + quantity + "/" + item.getQuantity() + ")");
                        return;
                    }
                    storage.sendStored(c, GameConstants.getInventoryType(itemId));
                }
                //

                break;
            }
            case 6: { //arrange 1.2.6x
                storage.arrange();
                storage.update(c);
                break;
            }
            case 7: {
                int meso = slea.readInt();
                final int storageMesos = storage.getMeso();
                final int playerMesos = chr.getMeso();

                if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                    if (meso < 0 && (storageMesos - meso) < 0) { // storing with overflow
                        meso = -(Integer.MAX_VALUE - storageMesos);
                        if ((-meso) > playerMesos) { // should never happen just a failsafe
                            return;
                        }
                    } else if (meso > 0 && (playerMesos + meso) < 0) { // taking out with overflow
                        meso = (Integer.MAX_VALUE - playerMesos);
                        if ((meso) > storageMesos) { // should never happen just a failsafe
                            return;
                        }
                    }
                    storage.setMeso(storageMesos - meso);
                    chr.gainMeso(meso, false, false);
                } else {
                    AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store or take out unavailable amount of mesos (" + meso + "/" + storage.getMeso() + "/" + c.getPlayer().getMeso() + ")");
                    return;
                }
                storage.sendMeso(c);
                break;
            }
            case 8: {
                storage.close();
                chr.setConversation(0);
                break;
            }
            default:
                System.out.println("Unhandled Storage mode : " + mode);
                break;
        }
    }

    public static final void NPCMoreTalk(final LittleEndianAccessor slea, final MapleClient c) {
        byte lastMsg = slea.readByte(); // 00 (last msg type I think)
        byte action = slea.readByte(); // 00 = end chat, 01 == follow
        //c.getPlayer().dropMessage(6, "lastMsg: " + lastMsg + " action: " + action);

        if (lastMsg == 2 || lastMsg == 12) { // 화스 솟 때문에 강제 조정 : YesNo AcceptDecline
            lastMsg -= 1;
        }

        if (NPCScriptInvoker.isVmConversation(c)) {
            NPCScriptVirtualMachine vm = NPCScriptInvoker.getVM(c);
            if (vm != null) {
                String str = "";
                int type = vm.getLastMsg();
                int selection = -1;
                if (type == 2) {
                    if (action != 0) {
                        str = slea.readMapleAsciiString();
                    }
                } else if (slea.available() >= 4) {
                    selection = slea.readInt();
                } else if (slea.available() > 0) {
                    selection = slea.readByte() & 0xFF;
                }

                NPCScriptInvoker.actionNpc(c, action, type, selection, str);
                return;
            }
        }
        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
        if (cm == null || c.getPlayer().getConversation() == 0 || cm.getLastMsg() != lastMsg) {
            if (cm != null && cm.getLastMsg() != lastMsg) {
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "기본 : " + cm.getLastMsg() + " / 패킷 :  " + lastMsg + " -> 수정바람");
                }
            }
            return;
        }
        cm.setLastMsg((byte) -1);
        if (lastMsg == 3) {
            if (action != 0) {
                cm.setGetText(slea.readMapleAsciiString());
                if (cm.getType() == 0) {
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, -1);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, -1);
                } else {
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else {
                cm.dispose();
            }
        } else {
            int selection = -1;
            if (slea.available() >= 4) {
                selection = slea.readInt();
            } else if (slea.available() > 0) {
                selection = slea.readByte() & 0xFF;
            }
            if (lastMsg == 5 && selection == -1) {
                cm.dispose();
                return;//h4x
            }
            if (selection >= -1 && action != -1) {
                if (cm.getType() == 0) { // 여기로 박힘
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, selection);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, selection);
                } else {
                    NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
                }
            } else {
                switch (cm.getNpc()) {
                    case 2003: //hard code cuz safety
                        cm.playMusic(false, "Bgm00/RestNPeace");
                        break;
                }
                cm.dispose();
            }
        }
    }

    public static final void repairAll(final MapleClient c) {
        Equip eq;
        double price = 0;
        Map<String, Integer> eqStats;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<Equip, Integer> eqs = new HashMap<Equip, Integer>();
        final MapleInventoryType[] types = {MapleInventoryType.EQUIP, MapleInventoryType.EQUIPPED};
        for (MapleInventoryType type : types) {
            for (Item item : c.getPlayer().getInventory(type).newList()) {
                if (item instanceof Equip) { //redundant
                    eq = (Equip) item;
                    if (eq.getDurability() >= 0) {
                        eqStats = ii.getEquipStats(eq.getItemId());
                        final double rPercentage = Math.ceil((100.0 - (eq.getDurability() * 100.0) / (eqStats.get("durability"))));
                        eqs.put(eq, eqStats.get("durability"));
                        price += ii.getWholePrice(eq.getItemId()) * 0.02 * (ii.getReqLevel(eq.getItemId()) * ii.getReqLevel(eq.getItemId())) / (eqStats.get("durability") * 0.01) * rPercentage;//* (ii.getWholePrice(eq.getItemId()) * 0.25 + 1)
                    }
                }
            }
        }
        if (c.getPlayer().getMeso() < price) {
            c.getPlayer().dropMessage(1, "수리비가 부족합니다.");
            return;
        }
        if (eqs.size() <= 0) {
            c.getPlayer().dropMessage(1, "수리 할 아이템이 없습니다.");
            return;
        }
        c.getPlayer().gainMeso(-(int) price, true, true, false);
        Equip ez;
        for (Entry<Equip, Integer> eqqz : eqs.entrySet()) {
            ez = eqqz.getKey();
            ez.setDurability(eqqz.getValue());
            c.getPlayer().forceReAddItem(ez.copy(), ez.getPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP);
            stats.durabilityHandling.add((Equip) ez.copy());
        }
        c.getPlayer().dropMessage(1, "모든 아이템의 수리가 완료되었습니다.");
    }

    public static final void repair(final LittleEndianAccessor slea, final MapleClient c) {
        /*if (c.getPlayer().getMapId() != 240000000 || slea.available() < 4) { //leafre for now
         return;
         }*/
        final int position = slea.readInt(); //who knows why this is a int
        final MapleInventoryType type = position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
        final Item item = c.getPlayer().getInventory(type).getItem((byte) position);
        if (item == null) {
            return;
        }
        final Equip eq = (Equip) item; //이 부분 사용해보자
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<String, Integer> eqStats = ii.getEquipStats(item.getItemId());
        if (eq.getDurability() < 0 || !eqStats.containsKey("durability") || eqStats.get("durability") <= 0 || eq.getDurability() >= eqStats.get("durability")) {
            return;
        }
        final double rPercentage = Math.ceil((100.0 - (eq.getDurability() * 100.0) / (eqStats.get("durability"))));
        double price = 0.00;
        price = ii.getWholePrice(eq.getItemId()) * 0.02 * (ii.getReqLevel(eq.getItemId()) * ii.getReqLevel(eq.getItemId())) / (eqStats.get("durability") * 0.01) * rPercentage;//* (ii.getWholePrice(eq.getItemId()) * 0.25 + 1)
        //c.getPlayer().dropMessage(5, "rPercentage: " + rPercentage + " price: " + (long) price);
        if (c.getPlayer().getMeso() < price) {
            c.getPlayer().dropMessage(1, "수리비가 부족합니다.");
            return;
        }
        c.getPlayer().gainMeso(-(int) price, true, true, false);
        eq.setDurability(eqStats.get("durability"));
        c.getPlayer().forceReAddItem(eq.copy(), type);
        stats.durabilityHandling.add((Equip) eq.copy());
        c.getPlayer().dropMessage(1, "수리가 완료되었습니다.");
    }

    public static final void UpdateQuest(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleQuest quest = MapleQuest.getInstance(slea.readShort());
        if (quest != null) {
            c.getPlayer().updateQuest(c.getPlayer().getQuest(quest), true);
        }
    }

    public static final void UseItemQuest(final LittleEndianAccessor slea, final MapleClient c) {
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        final int qid = slea.readInt();
        final MapleQuest quest = MapleQuest.getInstance(qid);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Pair<Integer, List<Integer>> questItemInfo = null;
        boolean found = false;
        for (Item i : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
            if (i.getItemId() / 10000 == 422) {
                questItemInfo = ii.questItemInfo(i.getItemId());
                if (questItemInfo != null && questItemInfo.getLeft() == qid && questItemInfo.getRight() != null && questItemInfo.getRight().contains(itemId)) {
                    found = true;
                    break; //i believe it's any order
                }
            }
        }
        if (quest != null && found && item != null && item.getQuantity() > 0 && item.getItemId() == itemId) {
            final int newData = slea.readInt();
            final MapleQuestStatus stats = c.getPlayer().getQuestNoAdd(quest);
            if (stats != null && stats.getStatus() == 1) {
                stats.setCustomData(String.valueOf(newData));
                c.getPlayer().updateQuest(stats, true);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
            }
        }
    }

    public static final void RPSGame(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.available() == 0 || c.getPlayer() == null || c.getPlayer().getMap() == null || !c.getPlayer().getMap().containsNPC(9000019)) {
            if (c.getPlayer() != null && c.getPlayer().getRPS() != null) {
                c.getPlayer().getRPS().dispose(c);
            }
            return;
        }
        final byte mode = slea.readByte();
        switch (mode) {
            case 0: //start game
            case 5: //retry
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().reward(c);
                }
                if (c.getPlayer().getMeso() >= 1000) {
                    c.getPlayer().setRPS(new RockPaperScissors(c, mode));
                } else {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x08, -1, -1, -1));
                }
                break;
            case 1: //answer
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().answer(c, slea.readByte())) {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 2: //time over
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().timeOut(c)) {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 3: //continue
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().nextRound(c)) {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 4: //leave
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().dispose(c);
                } else {
                    c.getSession().write(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
        }

    }

    public static final void OpenPublicNpc(final LittleEndianAccessor slea, final MapleClient c) {
        final int npcid = slea.readInt();
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().isInBlockedMap() || c.getPlayer().getLevel() < 10) {
//            c.getPlayer().dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
            return;
        }
        for (int i = 0; i < GameConstants.publicNpcIds.length; i++) {
            if (GameConstants.publicNpcIds[i] == npcid) { //for now
                NPCScriptManager.getInstance().start(c, npcid);
                return;
            }
        }
    }
}
