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
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.events.MapleCoconut;
import server.events.MapleCoconut.MapleCoconuts;
import server.events.MapleEventType;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.*;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageEventAgent;
import server.marriage.MarriageManager;
import server.marriage.MarriageTicketType;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import server.Timer;

public class PlayersHandler {

    public static void NoteFame(final MapleCharacter chr) {
//        int added = 0;
//        while (chr.checkCanGainFameNGive()) {
//            added++;
//        }
//        if (added > 0) {
//            chr.getClient().getSession().write(MaplePacketCreator.getShowFameGain(added));
//        }
    }

    public static void Note(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final byte type = slea.readByte();

        switch (type) {
            case 0:
            case 10:
                String name = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                boolean fame = slea.readByte() > 0;
                //slea.readInt(); //0?
//                Item itemz = chr.getCashInventory().findByCashId((int) slea.readLong());
//                if (itemz == null || !itemz.getGiftFrom().equalsIgnoreCase(name) || !chr.getCashInventory().canSendNote(itemz.getUniqueId())) {
//                    return;
//                }
                try {
                    chr.sendNote(name, msg, fame ? 1 : 0);
//                    chr.getCashInventory().sendedNote(itemz.getUniqueId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                int num = slea.readByte() & 0xFF;
                slea.skip(2);
                for (int i = 0; i < num; i++) {
                    final int id = slea.readInt();
                    chr.deleteNote(id, slea.readByte() > 0 ? 1 : 0);
                }
                break;
            default:
                System.out.println("Unhandled note action, " + type + "");
        }
    }

    public static void GiveFame(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int who = slea.readInt();
        final int mode = slea.readByte();

        final int famechange = mode == 0 ? -1 : 1;
        final MapleCharacter target = chr.getMap().getCharacterById(who);

        if (target == null || target == chr) { // faming self
            chr.getCheatTracker().registerOffense(CheatingOffense.FAMING_SELF);
            return;
        } else if (chr.getLevel() < 15) {
            chr.getCheatTracker().registerOffense(CheatingOffense.FAMING_UNDER_15);
            return;
        }
        switch (chr.canGiveFame(target)) {
            case OK:
                if (Math.abs(target.getFame() + famechange) <= 30000) {
                    target.addFame(famechange);
                    target.updateSingleStat(MapleStat.FAME, target.getFame());
                }
                if (!chr.isGM()) {
                    chr.hasGivenFame(target);
                }
                c.getSession().write(MaplePacketCreator.giveFameResponse(mode, target.getName(), target.getFame()));
                target.getClient().getSession().write(MaplePacketCreator.receiveFame(mode, chr.getName()));
                break;
            case NOT_TODAY:
                c.getSession().write(MaplePacketCreator.giveFameErrorResponse(3));
                break;
            case NOT_THIS_MONTH:
                c.getSession().write(MaplePacketCreator.giveFameErrorResponse(4));
                break;
        }
    }

    public static void UseDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int oid = slea.readInt();
        final boolean mode = slea.readByte() == 0; // specifies if backwarp or not, 1 town to target, 0 target to town

        for (MapleMapObject obj : chr.getMap().getAllDoorsThreadsafe()) {
            final MapleDoor door = (MapleDoor) obj;
            if (door.getOwnerId() == oid) {
                door.warp(chr, mode);
                break;
            }
        }
    }

    public static void UseMechDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int oid = slea.readInt();
        final Point pos = slea.readPos();
        final int mode = slea.readByte(); // specifies if backwarp or not, 1 town to target, 0 target to town
        chr.getClient().getSession().write(MaplePacketCreator.enableActions());
        for (MapleMapObject obj : chr.getMap().getAllMechDoorsThreadsafe()) {
            final MechDoor door = (MechDoor) obj;
            if (door.getOwnerId() == oid && door.getId() == mode) {
                chr.checkFollow();
                chr.getMap().movePlayer(chr, pos);
                break;
            }
        }
    }

    public static void TransformPlayer(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        // D9 A4 FD 00
        // 11 00
        // A0 C0 21 00
        // 07 00 64 66 62 64 66 62 64
        chr.updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        final String target = slea.readMapleAsciiString();

        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        switch (itemId) {
            case 2212000:
                final MapleCharacter search_chr = chr.getMap().getCharacterByName(target);
                if (search_chr != null) {
                    MapleItemInformationProvider.getInstance().getItemEffect(2210023).applyTo(search_chr);
                    search_chr.dropMessage(6, chr.getName() + " has played a prank on you!");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                }
                break;
        }
    }

    public static void HitReactor(final LittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final int charPos = slea.readInt();
        final short stance = slea.readShort();
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null || !reactor.isAlive()) {
            return;
        }
        reactor.hitReactor(charPos, stance, c);
    }

    public static void TouchReactor(final LittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final boolean touched = slea.available() == 0 || slea.readByte() > 0; //the byte is probably the state to set it to
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (!touched || reactor == null || !reactor.isAlive() || reactor.getTouch() == 0) {
            return;
        }
        if (reactor.getTouch() == 2) {
            ReactorScriptManager.getInstance().act(c, reactor); //not sure how touched boolean comes into play
        } else if (reactor.getTouch() == 1 && !reactor.isTimerActive()) {
            if (reactor.getReactorType() == 100) {
                final int itemid = GameConstants.getCustomReactItem(reactor.getReactorId(), reactor.getReactItem().getLeft());
                if (c.getPlayer().haveItem(itemid, reactor.getReactItem().getRight())) {
                    if (reactor.getArea().contains(c.getPlayer().getTruePosition())) {
                        MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemid), itemid, reactor.getReactItem().getRight(), true, false);
                        reactor.hitReactor(c);
                    } else {
                        c.getPlayer().dropMessage(5, "You are too far away.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "You don't have the item required.");
                }
            } else {
                //just hit it
                reactor.hitReactor(c);
            }
        }
    }

    public static void hitCoconut(LittleEndianAccessor slea, MapleClient c) {
        /*CB 00 A6 00 06 01
         * A6 00 = coconut id
         * 06 01 = ?
         */
        int id = slea.readShort();
        String co = "coconut";
        MapleCoconut map = (MapleCoconut) c.getChannelServer().getEvent(MapleEventType.Coconut);
        if (map == null || !map.isRunning()) {
            map = (MapleCoconut) c.getChannelServer().getEvent(MapleEventType.CokePlay);
            co = "coke cap";
            if (map == null || !map.isRunning()) {
                return;
            }
        }
        //System.out.println("Coconut1");
        MapleCoconuts nut = map.getCoconut(id);
        if (nut == null || !nut.isHittable()) {
            return;
        }
        if (System.currentTimeMillis() < nut.getHitTime()) {
            return;
        }
        //System.out.println("Coconut2");
        if (nut.getHits() > 2 && Math.random() < 0.4 && !nut.isStopped()) {
            //System.out.println("Coconut3-1");
            nut.setHittable(false);
            if (Math.random() < 0.01 && map.getStopped() > 0) {
                nut.setStopped(true);
                map.stopCoconut();
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 1));
                return;
            }
            nut.resetHits(); // For next event (without restarts)
            //System.out.println("Coconut4");
            if (Math.random() < 0.05 && map.getBombings() > 0) {
                //System.out.println("Coconut5-1");
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 2));
                map.bombCoconut();
            } else if (map.getFalling() > 0) {
                //System.out.println("Coconut5-2");
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 3));
                map.fallCoconut();
                if (c.getPlayer().getTeam() == 0) {
                    map.addMapleScore();
                    //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + " of Team Maple knocks down a " + co + "."));
                } else {
                    map.addStoryScore();
                    //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + " of Team Story knocks down a " + co + "."));
                }
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.coconutScore(map.getCoconutScore()));
            }
        } else {
            //System.out.println("Coconut3-2");
            nut.hit();
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.hitCoconut(false, id, 1));
        }
    }

    /*   3B
     04 00 B9 D9 BA B8
     18 00
     B0 6A 21 00
     */
    public static void LieDetector(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final boolean isItem) { // Person who used
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final String target = slea.readMapleAsciiString();
        byte slot = 0;
        if (isItem) {
            slot = (byte) slea.readShort(); // 01 00 (first pos in use) 
            final int itemId = slea.readInt(); // B0 6A 21 00 
            final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
            if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemId || itemId != 2190000) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
        }
        if ((FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit()) && isItem) || chr.getMap().getReturnMapId() == chr.getMapId() || chr.getMap().getReturnMapId() == 999999999) {
            chr.dropMessage(5, "You may not use the Lie Detector on this area.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final MapleCharacter search_chr = chr.getMap().getCharacterByName(target);
        if (search_chr == null || search_chr.getId() == chr.getId() || search_chr.isGM() && !chr.isGM()) {
            chr.dropMessage(1, "The user cannot be found.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (search_chr.getEventInstance() != null) {
            chr.dropMessage(5, "You may not use the Lie Detector on this area.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (search_chr.getAntiMacro().inProgress()) {
            Timer.EtcTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 3)); //현재 받고 있는중
                }
            }, 1000);
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (search_chr.getAntiMacro().isPassed() && isItem || search_chr.getAntiMacro().getAttempt() == 2) {
            c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 2)); //이미 탐지 받음
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!search_chr.getAntiMacro().startLieDetector(chr.getName(), isItem, false)) {
            chr.dropMessage(5, "Sorry! The Captcha Server is not available now, please try again later."); //error occured, usually cannot access to captcha server 
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (isItem) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        search_chr.dropMessage(5, chr.getName() + " 님이 거짓말 탐지기를 사용하셨습니다.");
    }

    public static void LieDetectorResponse(final LittleEndianAccessor slea, final MapleClient c) { // Person who typed 
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final String answer = slea.readMapleAsciiString();

        final MapleLieDetector ld = c.getPlayer().getAntiMacro();
        if (!ld.inProgress() || (ld.isPassed() && ld.getLastType() == 0) || ld.getAnswer() == null || answer.length() <= 0) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (answer.equals(ld.getAnswer())) {
            final MapleCharacter search_chr = c.getPlayer().getMap().getCharacterByName(ld.getTester());
            if (search_chr != null && search_chr.getId() != c.getPlayer().getId()) {
                search_chr.dropMessage(5, "해당 유저가 거짓말 탐지기를 통과 했습니다.");
            }
            c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 9, (byte) 1));
            c.getPlayer().gainMeso(5000, true, true);
            ld.end();
        } else if (ld.getAttempt() < 2) { // redo again 
            ld.startLieDetector(ld.getTester(), ld.getLastType() == 0, true); // new attempt 
        } else {
            final MapleCharacter search_chr = c.getPlayer().getMap().getCharacterByName(ld.getTester());
            if (search_chr != null && search_chr.getId() != c.getPlayer().getId()) {
                search_chr.getClient().getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 11, (byte) 11));
                //search_chr.dropMessage(5, "거짓말 탐지기 테스트 결과 테스트 대상자가 매크로로 적발되었습니다. 대상자의 소지 금액중 7000메소를 보상으로 지급받게 됩니다.");
                search_chr.gainMeso(7000, true, true);
            }
            ld.end();
            c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 7, (byte) 4));
            //c.getPlayer().dropMessage(5, "거짓말 탐지기 테스트 결과 매크로로 적발되었습니다. 적발이 누적될 경우 운영자의 제재를 받을 수 있습니다.");
            final MapleMap to = c.getPlayer().getMap().getReturnMap();
            c.getPlayer().changeMap(to, to.getPortal(0));
        }
    }

    public static void DoRing(final MapleClient c, final String name, final int itemid) {
        final int newItemId = 1112300 + (itemid - 2240004);
        final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        int errcode = 0;
        if (c.getPlayer().getMarriageId() > 0) {
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
            if (data.getStatus() == 1) {
                errcode = 0x17; // 이미 약혼했습니다.
            } else if (data.getStatus() == 2) {
                errcode = 0x19; // 이미 결혼했습니다.
            }
        } else if (chr == null) {
            errcode = 0x12; // 잘못된 캐릭터 이름입니다.
        } else if (chr.getMapId() != c.getPlayer().getMapId()) {
            errcode = 0x13; // 같은 맵에 없습니다.
        } else if (chr.getGender() == c.getPlayer().getGender()) {
            errcode = 0x16; // 같은 맵에 없습니다.
        } else if (!c.getPlayer().haveItem(itemid, 1) || itemid < 2240000 || itemid > 2240015) {
            errcode = 0x0D; // 약혼이 취소되었습니다.
        } else if (chr.getMarriageId() > 0) {
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(chr.getMarriageId());
            if (data.getStatus() == 1) {
                errcode = 0x18; // 상대가 이미 약혼했습니다.
            } else if (data.getStatus() == 2) {
                errcode = 0x1A; // 상대가 이미 결혼했습니다.
            }
        } else if (chr.getMarriageItemId() > 0) {
            errcode = 0x18; // 상대가 이미 약혼했습니다.
        } else if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "")) {
            errcode = 0x14; // 인벤토리가 부족해요
        } else if (!MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
            errcode = 0x15; // 상대 인벤토리가 부족해요
        }
        if (errcode > 0) {
            c.getSession().write(MaplePacketCreator.sendEngagement((byte) errcode, 0, null, null));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //c.getPlayer().dropMessage(1, "상대방의 승낙을 기다리고 있습니다.");
        c.getPlayer().setMarriageItemId(itemid);
        chr.getClient().getSession().write(MaplePacketCreator.sendEngagementRequest(c.getPlayer().getName(), c.getPlayer().getId()));
    }

    public static void RingAction(final LittleEndianAccessor slea, final MapleClient c) {
        final byte mode = slea.readByte();
        if (mode == 0) {
            DoRing(c, slea.readMapleAsciiString(), slea.readInt());
            //
        } else if (mode == 1) {
            c.getPlayer().setMarriageItemId(0);
        } else if (mode == 2) { //accept/deny proposal
            final boolean accepted = slea.readByte() > 0;
            final String name = slea.readMapleAsciiString();
            final int id = slea.readInt();
            final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (c.getPlayer().getMarriageId() > 0 || chr == null || chr.getId() != id || chr.getMarriageItemId() <= 0 || !chr.haveItem(chr.getMarriageItemId(), 1) || chr.getMarriageId() > 0 || !chr.isAlive() || chr.getEventInstance() != null || !c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null) {
                c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x1D, 0, null, null));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (accepted) {
                final int itemid = chr.getMarriageItemId();
                int newItemId = (itemid - 2240004) + 4210000;
                //final int newItemId = itemid == 2240000 ? 1112803 : (itemid == 2240001 ? 1112806 : (itemid == 2240002 ? 1112807 : (itemid == 2240003 ? 1112809 : (1112300 + (itemid - 2240004)))));
                if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "") || !MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
                    c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x15, 0, null, null));
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                try {
                    MarriageDataEntry data = MarriageManager.getInstance().makeNewMarriage(chr.getId());
                    data.setStatus(1);
                    data.setGroomId(chr.getId());
                    data.setBrideId(c.getPlayer().getId());
                    data.setBrideName(c.getPlayer().getName());
                    data.setGroomName(chr.getName());

                    final int[] ringID = MapleRing.makeRing(newItemId, c.getPlayer(), chr);
                    MapleRing ring = MapleRing.loadFromDb(ringID[1]);
                    Item ring1 = new Item(newItemId, (short) 0, (short) 1, (short) 0, ringID[1]);
                    if (ring != null) {
                        ring1.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(c, ring1);

                    ring = MapleRing.loadFromDb(ringID[0]);
                    Item ring2 = new Item(newItemId, (short) 0, (short) 1, (short) 0, ringID[0]);
                    if (ring != null) {
                        ring2.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(chr.getClient(), ring2);

                    MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, chr.getMarriageItemId(), 1, false, false);

                    c.getPlayer().setMarriageId(data.getMarriageId());
                    chr.setMarriageId(data.getMarriageId());

                    chr.getClient().getSession().write(MaplePacketCreator.sendEngagement((byte) 0xB, newItemId, chr, c.getPlayer()));
                    c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0xB, newItemId, chr, c.getPlayer()));

                    data.setEngagementTime(System.currentTimeMillis());
                } catch (Exception e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                }

            } else {
                chr.getClient().getSession().write(MaplePacketCreator.sendEngagement((byte) 0x1E, 0, null, null)); // 프로포즈를 정중히 거절요
            }
            c.getSession().write(MaplePacketCreator.enableActions());
            chr.setMarriageItemId(0);
        } else if (mode == 3) { //drop, only works for ETC
            // 73 00 03 52 3D 40 00
            final int itemId = slea.readInt();
            final MapleInventoryType type = GameConstants.getInventoryType(itemId);
            final Item item = c.getPlayer().getInventory(type).findById(itemId);
            if (item != null && type == MapleInventoryType.ETC && itemId / 10000 == 421) {
                if (c.getPlayer().getMarriageId() > 0) {
                    MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());

                    if (data.getWeddingStatus() >= 1) {
                        c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x20, 0, null, null));
                        return;
                    }

                    c.getPlayer().setMarriageId(0);
                    c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x0D, 0, null, null));
                    MapleRing.DeleteRingForItemAndChrId(c.getPlayer().getId(), itemId);
                    if (data != null && data.getStatus() == 1) {
                        MarriageManager.getInstance().deleteMarriage(data.getMarriageId());
                        int channel = -1;
                        MapleCharacter chr = null;
                        if (data.getGroomId() == c.getPlayer().getId()) {
                            channel = World.Find.findChannel(data.getBrideId());
                            if (channel >= 0) {
                                chr = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(data.getBrideId());
                            }
                        } else {
                            channel = World.Find.findChannel(data.getGroomId());
                            if (channel >= 0) {
                                chr = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(data.getGroomId());
                            }
                        }
                        if (chr != null) {
                            MapleRing.DeleteRingForItemAndChrId(chr.getId(), itemId);
                            chr.setMarriageId(0);
                            chr.getClient().getSession().write(MaplePacketCreator.sendEngagement((byte) 0x0D, 0, null, null));
                            MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.ETC, itemId, 1, true, false);
                        }
                    }
                }
                MapleInventoryManipulator.drop(c, type, item.getPosition(), item.getQuantity());
            }
        } else if (mode == 5) {
            // 청첩장 보내기
            // 73 00 05 04 00 BC BD BD BA 01 00 00 00 0C 00 00 00
            String receiver = slea.readMapleAsciiString();
            int marriageId = slea.readInt();
            int slot = slea.readInt();
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
            if (data != null) {
                if (data.getMarriageId() == marriageId) {
                    Item invatation = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem((short) slot);
                    if (invatation != null && invatation.getItemId() == data.getTicketType().getInvitationItemId()) {
                        // Send Invitation
                        int channel = World.Find.findChannel(receiver);
                        MapleCharacter chr = null;
                        if (channel >= 0) {
                            chr = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(receiver);
                        }
                        if (chr != null) {
                            if (data.getReservedPeopleList().contains(Integer.valueOf(chr.getId()))) {
                                c.getPlayer().dropMessage(1, "대상은 이미 결혼식에 초대되었습니다.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                return;
                            }
                            MarriageTicketType type = data.getTicketType();
                            if (MapleInventoryManipulator.checkSpace(chr.getClient(), type.getInvitedItemId(), 1, "")) {
                                MapleCharacterUtil.sendNote(receiver, c.getPlayer().getName(), "Congratulations! 당신은 결혼식에 초대되었습니다! 기타창을 확인해주세요.", 0);
                                Item item = new Item(type.getInvitedItemId(), (short) 0, (short) 1, (short) 0);
                                item.setMarriageId(data.getMarriageId());
                                MapleInventoryManipulator.addbyItem(chr.getClient(), item);
                                c.getPlayer().dropMessage(1, "청첩장을 보냈습니다.");
                                c.getSession().write(MaplePacketCreator.enableActions());
                                data.getReservedPeopleList().add(Integer.valueOf(chr.getId()));
                                MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, invatation.getItemId(), 1, true, false);
                            } else {
                                c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x15, 0, null, null));
                                c.getSession().write(MaplePacketCreator.enableActions());
                            }
                        } else {
                            c.getPlayer().dropMessage(1, "초대 받을 하객이 접속중이 아닙니다.");
                            c.getSession().write(MaplePacketCreator.enableActions());
                        }
                    }
                }
            }
        } else if (mode == 6) {
            // 청첩장 읽기
            // 73 00 06 07 00 00 00 22 45 40 00
            int slot = slea.readInt();
            int itemid = slea.readInt();
            Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem((short) slot);
            if (item != null && item.getItemId() == itemid && item.getMarriageId() > 0) {
                MarriageDataEntry data = MarriageManager.getInstance().getMarriage(item.getMarriageId());
                if (data != null) {
                    c.sendPacket(MaplePacketCreator.showWeddingInvitation(data.getGroomName(), data.getBrideName(), data.getTicketType().getItemId() - 5251004));
                    return;
                }
            }
            c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x22, 0, null, null));
        } else if (mode == 9) {
            int wishes = slea.readByte();
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
            if (data != null) {
                if (data.getStatus() == 1 && data.getWeddingStatus() >= 1 && data.getWeddingStatus() < 8) {
                    if (data.getGroomId() == c.getPlayer().getId()) {
                        if ((data.getStatus() & 2) > 0) {
                            return;
                        }
                        for (int i = 0; i < wishes; ++i) {
                            data.getGroomWishList().add(slea.readMapleAsciiString());
                        }
                    } else if (data.getBrideId() == c.getPlayer().getId()) {
                        if ((data.getStatus() & 4) > 0) {
                            return;
                        }
                        for (int i = 0; i < wishes; ++i) {
                            data.getBrideWishList().add(slea.readMapleAsciiString());
                        }
                    }
                    if (data.getGroomId() == c.getPlayer().getId()) {
                        MarriageTicketType type = data.getTicketType();
                        if (MapleInventoryManipulator.checkSpace(c, type.getInvitationItemId(), type.getInvitationQuantity(), "")) {
                            data.setWeddingStatus(data.getWeddingStatus() | 2);
                            if (data.getWeddingStatus() < 7) {
                                c.getPlayer().dropMessage(1, "위시리스트를 등록했습니다. 여성분이 위시리스트 등록을 끝낼 때 까지 잠시 기다려주세요.");
                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x14, 0, null, null));
                            data.getGroomWishList().clear();
                        }
                    } else if (data.getBrideId() == c.getPlayer().getId()) {
                        MarriageTicketType type = data.getTicketType();
                        if (MapleInventoryManipulator.checkSpace(c, type.getInvitationItemId(), type.getInvitationQuantity(), "")) {
                            data.setWeddingStatus(data.getWeddingStatus() | 4);
                            if (data.getWeddingStatus() < 7) {
                                c.getPlayer().dropMessage(1, "위시리스트를 등록했습니다. 남성분이 위시리스트 등록을 끝낼 때 까지 잠시 기다려주세요.");
                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x14, 0, null, null));
                            data.getBrideWishList().clear();
                        }
                    }
                    if (data.getWeddingStatus() == 7) {
                        // Do Register Reservation
                        MarriageTicketType type = data.getTicketType();
                        c.getSession().write(MaplePacketCreator.sendEngagement((byte) 0x10, 0, null, null)); // 예약 완료
                        MapleInventoryManipulator.addById(c, type.getInvitationItemId(), (short) type.getInvitationQuantity(), "");
                        ServerLogger.getInstance().logItem(LogType.Item.FromScript, c.getPlayer().getId(), c.getPlayer().getName(), type.getInvitationItemId(), type.getInvitationQuantity(), MapleItemInformationProvider.getInstance().getName(type.getInvitationItemId()), 0, "결혼식 예약 완료");
                        int channel = World.Find.findChannel(data.getPartnerId(c.getPlayer().getId()));
                        if (channel >= 0) {
                            MapleCharacter chr = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(data.getPartnerId(c.getPlayer().getId()));
                            if (chr != null) {
                                MapleInventoryManipulator.addById(chr.getClient(), type.getInvitationItemId(), (short) type.getInvitationQuantity(), "");
                                ServerLogger.getInstance().logItem(LogType.Item.FromScript, chr.getId(), chr.getName(), type.getInvitationItemId(), type.getInvitationQuantity(), MapleItemInformationProvider.getInstance().getName(type.getInvitationItemId()), 0, "결혼식 예약 완료");
                                chr.getClient().getSession().write(MaplePacketCreator.sendEngagement((byte) 0x10, 0, null, null)); // 예약 완료
                            }
                        }
                        data.setMakeReservationTime(System.currentTimeMillis());
                    }

                }
            }
            // 결혼 선물 등록
            // [R] 73 00 09 08 00 04 00 BE C8 B3 C9 04 00 31 32 33 34 04 00 35 36 37 38 09 00 31 32 33 34 35 33 32 34 35 04 00 BE EE BE EE 06 00 BE EE C7 E3 C0 CC 04 00 C0 CC B7 B1 08 00 C0 E7 A4 C4 A4 A4 C0 E5
            // s......안냥..1234..5678..123453245..어어..어허이..이런..재ㅔㄴ장
        }
    }

    public static void WeddingPresent(final LittleEndianAccessor slea, final MapleClient c) {
        byte mode = slea.readByte();
        if (mode == 7) {
            // receive present.
            byte invtype = slea.readByte();
            byte slot = slea.readByte();
            MarriageDataEntry entry = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
            if (entry != null) {
                List<Item> items = c.getPlayer().getGender() == 0 ? entry.getGroomPresentList() : entry.getBridePresentList();
                if (null != items) {
                    try {
                        Item item = items.get(slot);
                        if (item != null && MapleInventoryType.getByType(invtype) == GameConstants.getInventoryType(item.getItemId())) {
                            if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), "")) {
                                items.remove(slot);
                                MapleInventoryManipulator.addbyItem(c, item);
                                c.sendPacket(MaplePacketCreator.showWeddingWishRecvToLocalResult(items));
                            } else {
                                c.sendPacket(MaplePacketCreator.showWeddingWishRecvDisableHang());
                                c.getPlayer().dropMessage(1, "인벤토리 공간이 부족합니다.");
                                return;
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException d) {
                    }
                }
            }
        } else if (mode == 6) {
            // give present
            short slot = slea.readShort();
            int itemid = slea.readInt();
            short quantity = slea.readShort();
            MapleInventoryType type = MapleInventoryType.getByType((byte) (itemid / 1000000));
            Item item = c.getPlayer().getInventory(type).getItem(slot);
            MarriageEventAgent agent = MarriageManager.getInstance().getEventAgent(c.getChannel());
            if (agent != null) {
                MarriageDataEntry dataEntry = agent.getDataEntry();
                if (dataEntry != null) {
                    if (item != null && item.getItemId() == itemid && item.getQuantity() >= quantity) {
                        Item item2 = item.copy();
                        if (GameConstants.isRechargable(itemid)) {
                            quantity = item.getQuantity();
                        }
                        item2.setQuantity(quantity);
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                        if (c.getPlayer().getWeddingGive() == 0) {
                            dataEntry.getGroomPresentList().add(item2);
                            c.sendPacket(MaplePacketCreator.showWeddingWishGiveToServerResult(dataEntry.getGroomWishList(), type, item2));
                            ServerLogger.getInstance().logTrade(LogType.Trade.WeddingPresent, c.getPlayer().getId(), c.getPlayer().getName(), dataEntry.getGroomName(), MapleItemInformationProvider.getInstance().getName(itemid), "결혼 선물 (" + c.getPlayer().getName() + "->" + dataEntry.getGroomName() + ")");
                        } else {
                            dataEntry.getBridePresentList().add(item2);
                            c.sendPacket(MaplePacketCreator.showWeddingWishGiveToServerResult(dataEntry.getBrideWishList(), type, item2));
                            ServerLogger.getInstance().logTrade(LogType.Trade.WeddingPresent, c.getPlayer().getId(), c.getPlayer().getName(), dataEntry.getBrideName(), MapleItemInformationProvider.getInstance().getName(itemid), "결혼 선물 (" + c.getPlayer().getName() + "->" + dataEntry.getBrideName() + ")");
                        }
                    }
                }
            }
            // [R] 74 00 06 02 00 80 E2 0F 00 01 00
            // [R] 74 00 06 1F 00 81 EF 14 00 01 00
            // [R] 74 00 06 06 00 85 84 1E 00 07 00
        }
    }

    public static void Solomon(final LittleEndianAccessor slea, final MapleClient c) {
        c.getSession().write(MaplePacketCreator.enableActions());
        c.getPlayer().updateTick(slea.readInt());
        Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slea.readShort());
        if (item == null || item.getItemId() != slea.readInt() || item.getQuantity() <= 0 || c.getPlayer().getLevel() > 50 || MapleItemInformationProvider.getInstance().getItemEffect(item.getItemId()).getEXP() <= 0) {
            return;
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, item.getPosition(), (short) 1, false);
    }

    public static void Report(final LittleEndianAccessor slea, final MapleClient c) {
        int type = slea.readByte(); //01 = Conversation claim 00 = illegal program
        String victim = slea.readMapleAsciiString();
        int reason = slea.readByte();
        String description = slea.readMapleAsciiString();
        if (type == 0) {
            if (c.getPlayer().getPossibleReports() > 0) {
                if (c.getPlayer().getMeso() > 1000) {
                    c.getPlayer().decreaseReports();
                    c.getPlayer().gainMeso(-1000, true);
                } else {
                    c.getSession().write(MaplePacketCreator.report((byte) 67));
                    return;
                }
            } else {
                c.getSession().write(MaplePacketCreator.report((byte) 2));
                return;
            }
            addReport(c.getPlayer().getId(), MapleCharacter.getIdByName(victim), 0, description, null);
            c.getPlayer().dropMessage(1, "신고가 성공적으로 접수되었습니다!");
            c.getSession().write(MaplePacketCreator.report(0));
        } else if (type == 1) {
            String chatlog = slea.readMapleAsciiString();
            if (chatlog == null) {
                return;
            }
            if (c.getPlayer().getPossibleReports() > 0) {
                if (c.getPlayer().getMeso() > 1000) {
                    c.getPlayer().decreaseReports();
                    c.getPlayer().gainMeso(-1000, true);
                } else {
                    c.getSession().write(MaplePacketCreator.report((byte) 67));
                    return;
                }
            }
            addReport(c.getPlayer().getId(), MapleCharacter.getIdByName(victim), reason, description, chatlog);
            c.getPlayer().dropMessage(1, "신고가 성공적으로 접수되었습니다!");
            c.getSession().write(MaplePacketCreator.report(0));
        }
    }

    public static void addReport(int reporterid, int victimid, int reason, String description, String chatlog) {
        Calendar calendar = Calendar.getInstance();
        Timestamp currentTimestamp = new java.sql.Timestamp(calendar.getTime().getTime());
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO reports (`reporttime`, `reporterid`, `victimid`, `reason`, `chatlog`, `description`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, currentTimestamp.toString());
            ps.setInt(2, reporterid);
            ps.setInt(3, victimid);
            ps.setInt(4, reason);
            ps.setString(5, chatlog);
            ps.setString(6, description);
            ps.addBatch();
            ps.executeBatch();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean inArea(MapleCharacter chr) {
        for (Rectangle rect : chr.getMap().getAreas()) {
            if (rect.contains(chr.getTruePosition())) {
                return true;
            }
        }
        for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
            if (mist.getOwnerId() == chr.getId() && mist.isPoisonMist() == 2 && mist.getBox().contains(chr.getTruePosition())) {
                return true;
            }
        }
        return false;
    }
}
