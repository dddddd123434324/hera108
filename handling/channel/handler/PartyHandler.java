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

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import server.maps.FieldLimitType;
import server.maps.MapleDoor;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.Event_DojoAgent;
import tools.packet.PartyPacket;

public class PartyHandler {

    public static final void DenyPartyRequest(final LittleEndianAccessor slea, final MapleClient c) {
        final int action = slea.readByte();
        final int partyid = slea.readInt();
        if (c.getPlayer().getParty() == null && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null) {
//                if (party.getExpeditionId() > 0) {
//                    c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
//                    return;
//                }
                if (action == 0x1B) { //accept
                    if (party.getMembers().size() < 6) {
                        c.getPlayer().setParty(party);
                        World.Party.updateParty(partyid, PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                        c.getPlayer().receivePartyMemberHP();
                        c.getPlayer().updatePartyMemberHP();
                    } else {
                        c.getSession().write(MaplePacketCreator.partyStatusMessage(18));  //may be 18?
                    }
                } else if (action != 0x16) {
                    final MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(party.getLeader().getId());
                    if (cfrom != null) {
                        cfrom.dropMessage(5, c.getPlayer().getName() + "님이 파티 초대를 거절하셨습니다.");
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "가입하려는 파티가 존재하지 않습니다.");
            }
        } else {
            c.getPlayer().dropMessage(5, "파티에 이미 가입된 상태로는 가입할 수 없습니다.");
        }

    }

    public static final void PartyOperation(final LittleEndianAccessor slea, final MapleClient c) {
        final int operation = slea.readByte();
        MapleParty party = c.getPlayer().getParty();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());

        switch (operation) {
            case 1: // create
                if (party == null) {
                    party = World.Party.createParty(partyplayer);
                    c.getPlayer().setParty(party);
                    c.getSession().write(PartyPacket.partyCreated(party.getId()));
                    ServerLogger.getInstance().logParty(LogType.Etc.Party, "partyid:" + party.getId() + " 파티를 만듬 / " + c.getPlayer().getName() + " (Lv. " + c.getPlayer().getLevel() + ") / 직업 : " + c.getPlayer().getJob() + " / ", c.getPlayer().getAccountID());

                    if (c.getPlayer().getDoors().size() == 2) {
                        try {
                            MapleDoor door1 = c.getPlayer().getDoors().get(0);
                            MapleDoor door2 = c.getPlayer().getDoors().get(1);
                            door1.joinPartyElseDoorOwner(c);
                            door2.joinPartyElseDoorOwner(c);
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                    }

                } else {
                    if (partyplayer.equals(party.getLeader()) && party.getMembers().size() == 1) { //only one, reupdate
                        c.getSession().write(PartyPacket.partyCreated(party.getId()));
                    } else {
                        c.getPlayer().dropMessage(5, "파티에 이미 가입된 상태로는 가입할 수 없습니다.");
                    }
                }
                break;
            case 2: // leave
                if (party != null) { //are we in a party? o.O"
                    if (party.getExpeditionId() > 0) {
                        final MapleExpedition exped = World.Party.getExped(party.getExpeditionId());
                        if (exped != null) {
                            if (exped.getLeader() == c.getPlayer().getId()) { // disband
                                World.Party.expedPacket(exped.getId(), PartyPacket.expeditionBreak(), null);
                                World.Party.disbandExped(exped.getId()); //should take care of the rest
                            } else if (party.getLeader().getId() == c.getPlayer().getId()) {
                                World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(c, exped.getIndex(c.getPlayer().getParty().getId()), c.getPlayer().getParty()), null);
                                World.Party.updateParty(party.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                                World.Party.expedPacket(exped.getId(), PartyPacket.expeditionLeft(c.getPlayer().getName()), null);
                                c.sendPacket(PartyPacket.expeditionLeft());
                            } else {
                                World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(c, exped.getIndex(c.getPlayer().getParty().getId()), c.getPlayer().getParty()), null);
                                World.Party.updateParty(party.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                                World.Party.expedPacket(exped.getId(), PartyPacket.expeditionLeft(c.getPlayer().getName()), null);
                                c.sendPacket(PartyPacket.expeditionLeft());
                            }
                        }
                    } else if (partyplayer.equals(party.getLeader())) { // disband
                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                    } else {
                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                    }
                    if (c.getPlayer().getEventInstance() != null) {
                        c.getPlayer().getEventInstance().disbandParty();
                    }
                    if (c.getPlayer().getPyramidSubway() != null) {
                        c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                    }
                    
//                    if (partyplayer.equals(party.getLeader())) { // disband
//                        if (c.getPlayer().getPyramidSubway() != null) {
//                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
//                        } else if (GameConstants.isDojo(c.getPlayer().getMapId())) {
//                            Event_DojoAgent.failed(c.getPlayer());
//                        }
//                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
//                        if (c.getPlayer().getEventInstance() != null) {
//                            c.getPlayer().getEventInstance().disbandParty();
//                        }
//                    } else {
//                        if (c.getPlayer().getPyramidSubway() != null) {
//                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
//                        } else if (GameConstants.isDojo(c.getPlayer().getMapId())) {
//                            Event_DojoAgent.failed(c.getPlayer());
//                        }
//                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
//                        if (c.getPlayer().getEventInstance() != null) {
//                            c.getPlayer().getEventInstance().leftParty(c.getPlayer());
//                        }
//                    }
                    if (c.getPlayer().getDoors().size() == 2) {
                        try {
                            c.getPlayer().getDoors().get(0).sendSinglePortal();
                            c.getPlayer().getDoors().get(1).sendSinglePortal();
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                    }
                    c.getPlayer().cancelBuffStats(true, MapleBuffStat.DARK_AURA);
                    c.getPlayer().cancelBuffStats(true, MapleBuffStat.BLUE_AURA);
                    c.getPlayer().cancelBuffStats(true, MapleBuffStat.YELLOW_AURA);
                    ServerLogger.getInstance().logParty(LogType.Etc.Party, "partyid:" + party.getId() + " 파티를 나감 / " + c.getPlayer().getName() + " (Lv. " + c.getPlayer().getLevel() + ") / 직업 : " + c.getPlayer().getJob() + " / ", c.getPlayer().getAccountID());
                    c.getPlayer().setParty(null);
                }
                break;
            case 3: // accept invitation
                final int partyid = slea.readInt();
                String targets = "";
                if (party == null) {
                    party = World.Party.getParty(partyid);
                    if (party != null) {
                        if (party.getMembers().size() < 6 && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
                            targets += partyplayer.getName() + ", ";
                            targets += c.getPlayer().getName() + ", ";
                            c.getPlayer().setParty(party);
                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                        }
                        ServerLogger.getInstance().logParty(LogType.Etc.Party, "partyid:" + partyid + " 파티에 가입함 / " + c.getPlayer().getName() + " (Lv. " + c.getPlayer().getLevel() + ") / 직업 : " + c.getPlayer().getJob() + " / " + targets, c.getPlayer().getAccountID());
                    } else {
                        c.getPlayer().dropMessage(5, "해당 파티는 존재하지 않습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "이미 파티에 가입되어 있습니다.");
                }
                break;
            case 4: // invite
                if (party == null) {
                    party = World.Party.createParty(partyplayer);
                    c.getPlayer().setParty(party);
                    c.getSession().write(PartyPacket.partyCreated(party.getId()));
                    return;
                }
                // TODO store pending invitations and check against them
                final String theName = slea.readMapleAsciiString();
                final int theCh = World.Find.findChannel(theName);
                if (party != null) {
                    for (MaplePartyCharacter partychar : party.getMembers()) {
                        if (partychar.getName().equals(theName)) {
                            c.getPlayer().dropMessage(5, "별짓을 다하시네요;;");
                            return;
                        }
                    }
                }
                if (theCh > 0) {
                    final MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(theName);
                    if (invited != null && invited.getParty() == null && invited.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
                        if (party.getMembers().size() < 6) {
                            ServerLogger.getInstance().logParty(LogType.Etc.Party, "partyid:" + party.getId() + "초대된 캐릭터:" + invited.getName() + " 파티를 초대함/ " + c.getPlayer().getName() + " (Lv. " + c.getPlayer().getLevel() + ") / 직업 : " + c.getPlayer().getJob() + " / ", c.getPlayer().getAccountID());
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(22, invited.getName()));
                            invited.getClient().getSession().write(MaplePacketCreator.partyInvite(c.getPlayer(), false));
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(18));  //이미 최대인원
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "이미 파티에 가입되어 있습니다.");
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.partyStatusMessage(19)); //발견 실패
                }
                break;
            case 5: // expel
                if (party != null && partyplayer != null && partyplayer.equals(party.getLeader())) {
                    final MaplePartyCharacter expelled = party.getMemberById(slea.readInt());
                    if (expelled != null) {
                        if (expelled.getId() == c.getPlayer().getId()) {
                            c.getPlayer().dropMessage(5, "자기 자신을 강퇴할 수 없습니다.");
                            break;
                        }
                        if (c.getPlayer().getPyramidSubway() != null && expelled.isOnline()) {
                            c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                        } else if (GameConstants.isDojo(c.getPlayer().getMapId()) && expelled.isOnline()) {
                            Event_DojoAgent.failed(c.getPlayer());
                        }
                        World.Party.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                        if (c.getPlayer().getEventInstance() != null) {
                            /*if leader wants to boot someone, then the whole party gets expelled
                             TODO: Find an easier way to get the character behind a MaplePartyCharacter
                             possibly remove just the expellee.*/
                            if (expelled.isOnline()) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                        }
                    }
                }
                break;
            case 6: // change leader
                if (party != null) {
                    final MaplePartyCharacter newleader = party.getMemberById(slea.readInt());
                    if (newleader != null && partyplayer.equals(party.getLeader())) {
                        if (newleader.isOnline()) {
                            if (newleader.getChannel() == partyplayer.getChannel()) {
                                if (newleader.getMapid() == partyplayer.getMapid()) {
                                    World.Party.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newleader);
                                } else {
                                    c.getSession().write(MaplePacketCreator.partyStatusMessage(26)); //ok
                                }
                            } else {
                                c.getSession().write(MaplePacketCreator.partyStatusMessage(18)); // 
                                /*
                                 18:현재 채널
                                 22: 파끝
                                 25: 파끝
                                 26:같은 장소에 있는 파티원에게만 양도할 수 있습니다.
                                 27:파티장과 같은 장소에 있는 양도 가능한 파티원이 없습니다.
                                 29:운영자는 파티를 만들 수 없습니다.
                                 */
                            }
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(28)); //같은 장소에 있는 파티원에게만 양도할 수 있습니다.
                        }
                    }
                }
                break;

            //<editor-fold defaultstate="collapsed" desc="After BB Functions">
//            case 7: //request to  join a party
//                if (party != null) {
//                    if (c.getPlayer().getEventInstance() != null || c.getPlayer().getPyramidSubway() != null || party.getExpeditionId() > 0 || GameConstants.isDojo(c.getPlayer().getMapId())) {
//                        c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
//                        return;
//                    }
//                    if (partyplayer.equals(party.getLeader())) { // disband
//                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
//                    } else {
//                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
//                    }
//                    c.getPlayer().setParty(null);
//                }
//                final int partyid_ = slea.readInt();
//                break;
//            case 8: //allow party requests
//                if (slea.readByte() > 0) {
//                    c.getPlayer().getQuestRemove(MapleQuest.getInstance(GameConstants.PARTY_REQUEST));
//                } else {
//                    c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PARTY_REQUEST));
//                }
//                break;
            //</editor-fold>
            default:
                System.out.println("Unhandled Party function." + operation);
                break;
        }
    }

    public static final void PartySearchStart(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().isInBlockedMap() || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())) {
            c.getPlayer().dropMessage(5, "이곳에서는 파티 찾기를 시도할 수 없습니다. 요청이 무시되었습니다.");
            return;
        } else if (c.getPlayer().getParty() == null) {
            MapleParty party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()));
            c.getPlayer().setParty(party);
            c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
        }
        final int min = slea.readInt();
        final int max = slea.readInt();
        final int members = slea.readInt();
        final int jobs = slea.readInt();
        final List<Integer> jobsList = new ArrayList<Integer>();
        if (max <= min || max - min > 30 || members > 6 || min > c.getPlayer().getLevel() || max < c.getPlayer().getLevel() || jobs == 0) {
            c.getPlayer().dropMessage(1, "오류가 발생했습니다.");
            return;
        }
        //all jobs = FF FF F7 0F
        if ((jobs & 0x1) != 0) {
            //all jobs? skip check or what
            c.getPlayer().startPartySearch(jobsList, max, min, members);
            return;
        }
        if ((jobs & 0x2) != 0) { //beginner
            jobsList.add(0);
            jobsList.add(1);
            jobsList.add(1000);
            jobsList.add(2000);
            jobsList.add(2001);
            jobsList.add(3000);
        }
        if ((jobs & 0x4) != 0) { //aran
            jobsList.add(2100);
            jobsList.add(2110);
            jobsList.add(2111);
            jobsList.add(2112);
        }
        if ((jobs & 0x8) != 0) { //evan
            jobsList.add(2200);
            jobsList.add(2210);
            jobsList.add(2211);
            jobsList.add(2212);
            jobsList.add(2213);
            jobsList.add(2214);
            jobsList.add(2215);
            jobsList.add(2216);
            jobsList.add(2217);
            jobsList.add(2218);
        }
        if ((jobs & 0x10) != 0) { //swordman
            jobsList.add(100);
        }
        if ((jobs & 0x20) != 0) { //crusader
            jobsList.add(110);
            jobsList.add(111);
            jobsList.add(112);
        }
        if ((jobs & 0x40) != 0) { //knight
            jobsList.add(120);
            jobsList.add(121);
            jobsList.add(122);
        }
        if ((jobs & 0x80) != 0) { //dk
            jobsList.add(130);
            jobsList.add(131);
            jobsList.add(132);
        }
        if ((jobs & 0x100) != 0) { //soul
            jobsList.add(1100);
            jobsList.add(1110);
            jobsList.add(1111);
            jobsList.add(1112);
        }
        if ((jobs & 0x200) != 0) { //mage
            jobsList.add(200);
        }
        if ((jobs & 0x400) != 0) { //fp
            jobsList.add(210);
            jobsList.add(211);
            jobsList.add(212);
        }
        if ((jobs & 0x800) != 0) { //il
            jobsList.add(220);
            jobsList.add(221);
            jobsList.add(222);
        }
        if ((jobs & 0x1000) != 0) { //priest
            jobsList.add(230);
            jobsList.add(231);
            jobsList.add(232);
        }
        if ((jobs & 0x2000) != 0) { //flame
            jobsList.add(1200);
            jobsList.add(1210);
            jobsList.add(1211);
            jobsList.add(1212);
        }
        if ((jobs & 0x4000) != 0) { //battle mage <-- new
            jobsList.add(3200);
            jobsList.add(3210);
            jobsList.add(3211);
            jobsList.add(3212);
        }
        if ((jobs & 0x8000) != 0) { //pirate
            jobsList.add(500);
            jobsList.add(501);
        }
        if ((jobs & 0x10000) != 0) { //viper
            jobsList.add(510);
            jobsList.add(511);
            jobsList.add(512);
        }
        if ((jobs & 0x20000) != 0) { //gs
            jobsList.add(520);
            jobsList.add(521);
            jobsList.add(522);
        }
        if ((jobs & 0x40000) != 0) { //strikr
            jobsList.add(1500);
            jobsList.add(1510);
            jobsList.add(1511);
            jobsList.add(1512);
        }
        if ((jobs & 0x80000) != 0) { //mechanic <-- new
            jobsList.add(3500);
            jobsList.add(3510);
            jobsList.add(3511);
            jobsList.add(3512);
        }
        if ((jobs & 0x100000) != 0) { //teef
            jobsList.add(400);
        }
        //0x200000 doesn't exist in gms
        if ((jobs & 0x400000) != 0) { //hermit
            jobsList.add(410);
            jobsList.add(411);
            jobsList.add(412);
        }
        if ((jobs & 0x800000) != 0) { //cb
            jobsList.add(420);
            jobsList.add(421);
            jobsList.add(422);
        }
        if ((jobs & 0x1000000) != 0) { //nw
            jobsList.add(1400);
            jobsList.add(1410);
            jobsList.add(1411);
            jobsList.add(1412);
        }
        if ((jobs & 0x2000000) != 0) { //db
            jobsList.add(430);
            jobsList.add(431);
            jobsList.add(432);
            jobsList.add(433);
            jobsList.add(434);
        }
        if ((jobs & 0x4000000) != 0) { //archer
            jobsList.add(300);
        }
        if ((jobs & 0x8000000) != 0) { //ranger
            jobsList.add(310);
            jobsList.add(311);
            jobsList.add(312);
        }
        if ((jobs & 0x10000000) != 0) { //sniper
            jobsList.add(320);
            jobsList.add(321);
            jobsList.add(322);
        }
        if ((jobs & 0x20000000) != 0) { //wind breaker
            jobsList.add(1300);
            jobsList.add(1310);
            jobsList.add(1311);
            jobsList.add(1312);
        }
        if ((jobs & 0x40000000) != 0) { //wild hunter <-- new
            jobsList.add(3300);
            jobsList.add(3310);
            jobsList.add(3311);
            jobsList.add(3312);
        }
        if (jobsList.size() > 0) {
            c.getPlayer().startPartySearch(jobsList, max, min, members);
        } else {
            c.getPlayer().dropMessage(1, "오류가 발생했습니다.");
        }
    }

    public static final void PartySearchStop(final LittleEndianAccessor slea, final MapleClient c) {
        if (c != null && c.getPlayer() != null) {
            c.getPlayer().stopPartySearch();
        }
    }

    public static final void AllowPartyInvite(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.readByte() > 0) {
            c.getPlayer().getQuestRemove(MapleQuest.getInstance(GameConstants.PARTY_INVITE));
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE));
        }
    }

    public static final void PartyListing(final LittleEndianAccessor slea, final MapleClient c) {
        final int mode = slea.readByte();
        PartySearchType pst;
        MapleParty party;
        if (mode != 9999) {
            c.getPlayer().dropMessage(1, "현재 이용하실 수 없는 시스템 입니다.");
            return;
        }

        switch (mode) {
            case 80: //make
                int psType = slea.readInt();
                pst = PartySearchType.getById(psType);
                if (pst == null || c.getPlayer().getLevel() > pst.maxLevel || c.getPlayer().getLevel() < pst.minLevel) {
                    return;
                }
                if (World.Party.searchParty(pst).size() > 10) {
                    c.getPlayer().dropMessage(1, "Unable to create. Please leave the party.");
                    return;
                }
                if (c.getPlayer().getParty() == null) {
                    party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()), pst.id);
                    c.getPlayer().setParty(party);
                    c.getSession().write(MaplePacketCreator.partyCreated(party.getId()));
                    final PartySearch ps = new PartySearch(slea.readMapleAsciiString(), pst.exped ? party.getExpeditionId() : party.getId(), pst);
                    World.Party.addSearch(ps);
                    if (pst.exped) {
                        c.getSession().write(PartyPacket.expeditionStatus(World.Party.getExped(party.getExpeditionId()), true));
                    }
                    c.getSession().write(PartyPacket.partyListingAdded(ps));
                } else {
                    party = c.getPlayer().getParty();
                    final PartySearch ps = new PartySearch(slea.readMapleAsciiString(), pst.exped ? party.getExpeditionId() : party.getId(), pst);
                    World.Party.addSearch(ps);
                    if (pst.exped) {
                        c.getSession().write(PartyPacket.expeditionStatus(World.Party.getExped(party.getExpeditionId()), true));
                    }
                    c.getSession().write(PartyPacket.partyListingAdded(ps));
                }
                break;
            case 81: //close
                party = c.getPlayer().getParty();
                PartySearch toRemove = World.Party.getSearchByExped(party.getExpeditionId());
                if (toRemove != null) {
                    World.Party.removeSearch(toRemove, "파티광고가 삭제되었습니다.");
                    //갱신 패킷 필요
                }
                break;
            case 82: //display
                psType = slea.readInt();
                pst = PartySearchType.getById(psType);
                if (pst == null || c.getPlayer().getLevel() > pst.maxLevel || c.getPlayer().getLevel() < pst.minLevel) {
                    return;
                }
                c.getSession().write(PartyPacket.getPartyListing(pst));
                break;
            case 84: //join
                party = c.getPlayer().getParty();
                final MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());
                if (party == null) { //are we in a party? o.O"
                    final int theId = slea.readInt();
                    party = World.Party.getParty(theId);
                    if (party != null) {
                        PartySearch ps = World.Party.getSearchByParty(party.getId());
                        if (ps != null && c.getPlayer().getLevel() <= ps.getType().maxLevel && c.getPlayer().getLevel() >= ps.getType().minLevel && party.getMembers().size() < 6) {
                            c.getPlayer().setParty(party);
                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                        }
                    } else {
                        MapleExpedition exped = World.Party.getExped(theId);
                        System.out.println("exped" + exped);
                        if (exped != null) {
                            PartySearch ps = World.Party.getSearchByExped(exped.getId());
                            if (ps != null && c.getPlayer().getLevel() <= ps.getType().maxLevel && c.getPlayer().getLevel() >= ps.getType().minLevel && exped.getAllMembers() < exped.getType().maxMembers) {
                                int partyId = exped.getFreeParty();
                                if (partyId < 0) {
                                    c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                                } else if (partyId == 0) { //signal to make a new party
                                    party = World.Party.createPartyAndAdd(partyplayer, exped.getId());
                                    c.getPlayer().setParty(party);
                                    c.getSession().write(PartyPacket.partyCreated(party.getId()));
                                    c.getSession().write(PartyPacket.expeditionStatus(exped, true));
                                    World.Party.expedPacket(exped.getId(), PartyPacket.expeditionJoined(c.getPlayer().getName()), null);
                                    World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                } else {
                                    c.getPlayer().setParty(World.Party.getParty(partyId));
                                    World.Party.updateParty(partyId, PartyOperation.JOIN, partyplayer);
                                    c.getPlayer().receivePartyMemberHP();
                                    c.getPlayer().updatePartyMemberHP();
                                    c.getSession().write(PartyPacket.expeditionStatus(exped, true));
                                    World.Party.expedPacket(exped.getId(), PartyPacket.expeditionJoined(c.getPlayer().getName()), null);
                                }
                            } else {
                                c.getSession().write(PartyPacket.expeditionError(0, c.getPlayer().getName()));
                            }
                        }
                    }
                }
                break;
            default:
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(6, "PartyListing : " + mode + " / " + slea);
                    //System.out.println("Unknown PartyListing : " + mode + "\n" + slea);
                }
                break;
        }
        if (c.getPlayer().isGM()) {
            c.getPlayer().dropMessage(5, mode + " / ");
        }
    }

    public static final void Expedition(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final int mode = slea.readByte() - 6;
        MapleParty part, party;
        String name;
        switch (mode) {
            case 42: // create
                final ExpeditionType et = ExpeditionType.getById(slea.readInt());
                if (et == null) {
                    c.getPlayer().dropMessage(1, "코딩되지 않은 원정대입니다.");
                    return;
                }
                if (c.getPlayer().getParty() != null) {
                    return;
                }
                if (c.getPlayer().getLevel() <= et.maxLevel && c.getPlayer().getLevel() >= et.minLevel) {
                    party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()), et.exped);
                    c.getPlayer().setParty(party);
                    c.getSession().write(PartyPacket.partyCreated(party.getId()));
                    c.getSession().write(PartyPacket.expeditionStatus(World.Party.getExped(party.getExpeditionId()), true));
                } else {
                    c.getPlayer().dropMessage(5, et.getName() + "는 레벨 " + et.minLevel + " 이상 " + et.maxLevel + " 이하인 캐릭터만 참여할 수 있습니다.");
                }
                break;
            case 43: //invite [name]
                name = slea.readMapleAsciiString();
                final int theCh = World.Find.findChannel(name);
                if (theCh > 0) {
                    final MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(name);
                    if (invited != null) {
                        if (invited.getId() == c.getPlayer().getId()) {
                            //c.getPlayer().dropMessage(1, "자기 자신은 초대할 수 없습니다.");
                            break;
                        }
                        if (invited.getParty() != null && invited.getParty().getLeader().getId() != invited.getId()) {
                            c.getSession().write(PartyPacket.expeditionError(2, invited.getName())); // '" + name + "' 님은 이미 가입한 파티가\r\n있습니다.
                            break;
                        }
                        party = c.getPlayer().getParty();
                        MapleExpedition exped = World.Party.getExped(party.getExpeditionId());
                        if (exped == null) {
                            break;
                        }
                        if (exped.getAllMembers() >= exped.getType().maxMembers) {
                            break;
                        }
                        if (invited.getLevel() > exped.getType().maxLevel || invited.getLevel() < exped.getType().minLevel) {
                            c.getSession().write(PartyPacket.expeditionError(3, name)); // '" + name + "'님의\r\n레벨이 맞지 않아서\r\n원정대에 초대할 수 없습니다.
                            break;
                        }
                        invited.getClient().getSession().write(PartyPacket.expeditionInvite(c.getPlayer(), exped.getType().exped));
                    } else {
                        c.getSession().write(PartyPacket.expeditionError(0, name)); // 현재 서버에서\r\n'" + name + "' 님을 찾을 수 없습니다.
                    }
                } else {
                    c.getSession().write(PartyPacket.expeditionError(0, name)); // 현재 서버에서\r\n'" + name + "' 님을 찾을 수 없습니다.
                }
                break;
            case 44: //accept invite [name]
                name = slea.readMapleAsciiString();
                final int action = slea.readInt();
//                if (c.getPlayer().isGM()) {
//                    c.getPlayer().dropMessage(6, "액션 : " + action);
//                }
                final int theChh = World.Find.findChannel(name);
                if (theChh > 0) {
                    final MapleCharacter cfrom = ChannelServer.getInstance(theChh).getPlayerStorage().getCharacterByName(name);
                    if (cfrom != null) {
                        party = cfrom.getParty();
                        if (party != null && party.getExpeditionId() > 0) {
                            MapleExpedition exped = World.Party.getExped(party.getExpeditionId());
                            if (exped == null) {
                                break;
                            }
                            switch (action) {
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                    cfrom.getClient().getSession().write(PartyPacket.expeditionError(action, c.getPlayer().getName()));
                                    break;
                                case 8:
                                    if (c.getPlayer().getLevel() > exped.getType().maxLevel || c.getPlayer().getLevel() < exped.getType().minLevel) {
                                        c.getPlayer().dropMessage(1, exped.getType().getName() + "는 레벨 " + exped.getType().minLevel + " 이상 " + exped.getType().maxLevel + " 이하인 캐릭터만 참여할 수 있습니다.");
                                        break;
                                    }
                                    if (exped.getAllMembers() < exped.getType().maxMembers) {
                                        int partyId = exped.getFreeParty();
                                        if (partyId < 0) {
                                            c.getPlayer().dropMessage(5, "무언가 잘못되었습니다.");
                                            //c.getSession().write(MaplePacketCreator.partyStatusMessage(17));
                                        } else if (c.getPlayer().getParty() != null) { //signal to make a new party
                                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionJoined(name), null);
                                            party = World.Party.createPartyAndAdd(new MaplePartyCharacter(c.getPlayer()), exped.getId()); // 파티장과 다른 파티로
                                            c.getPlayer().setParty(party);
                                            c.getSession().write(PartyPacket.expeditionStatus(exped, false));
                                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                            //c.getPlayer().dropMessage(5, "가입하려는 원정대에 모든 파티장이 존재합니다. 파티를 탈퇴하고 수락해주세요.");
                                        } else {
                                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionJoined(c.getPlayer().getName()), null);
                                            World.Party.updateParty(partyId, PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                                            c.getPlayer().setParty(World.Party.getParty(partyId)); // 파티장과 같은 파티로
                                            c.getPlayer().receivePartyMemberHP();
                                            c.getPlayer().updatePartyMemberHP();
                                            c.getSession().write(PartyPacket.expeditionStatus(exped, false));
                                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                        }
                                    } else {
                                        c.getPlayer().dropMessage(5, "가입하려는 원정대에 이미 최대 인원 수만큼의 원정대원이 가입했습니다.");
                                        //c.getSession().write(MaplePacketCreator.expeditionError(3, cfrom.getName()));
                                    }
                                    break;
                                case 9:
                                    cfrom.dropMessage(5, "'" + name + "'님이 원정대 초대를 거절하였습니다.");
                                    break;
                                default:
                                    if (c.getPlayer().isGM()) {
                                        c.getPlayer().dropMessage(6, "Expedition mode : " + mode + " action : " + action);
                                    }
                                    break;
                            }
                        }
                    }
                }
                break;
            case 45: //leaving
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null) {
                        if (exped.getLeader() == c.getPlayer().getId()) { // disband
                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionBreak(), null);
                            World.Party.disbandExped(exped.getId()); //should take care of the rest
                        } else if (part.getLeader().getId() == c.getPlayer().getId()) {
                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(c, exped.getIndex(c.getPlayer().getParty().getId()), c.getPlayer().getParty()), null);
                            World.Party.updateParty(part.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionLeft(c.getPlayer().getName()), null);
                            c.getSession().write(PartyPacket.expeditionLeft());
                        } else {
                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(c, exped.getIndex(c.getPlayer().getParty().getId()), c.getPlayer().getParty()), null);
                            World.Party.updateParty(part.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionLeft(c.getPlayer().getName()), null);
                            c.getSession().write(PartyPacket.expeditionLeft());
                        }
                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                            Event_DojoAgent.failed(c.getPlayer());
                        }
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().disbandParty();
                        }
                        if (c.getPlayer().getPyramidSubway() != null) {
                            //c.getPlayer().getPyramidSubway().disbandParty(c.getPlayer());
                        }
                        c.getPlayer().setParty(null);
                    }
                }
                break;
            case 46: //kick [cid]
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int cid = slea.readInt();
                        if (c.getPlayer().getId() != cid) {
                            for (int i : exped.getParties()) {
                                final MapleParty par = World.Party.getParty(i);
                                if (par != null) {
                                    final MaplePartyCharacter expelled = par.getMemberById(cid);
                                    if (expelled != null) {
                                        name = expelled.getName();
                                        World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                        World.Party.expedPacket(exped.getId(), PartyPacket.expeditionKick(expelled.getName()), null);
                                        //World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(c, exped.getIndex(c.getPlayer().getParty().getId()), c.getPlayer().getParty()), null);
                                        int theCha = World.Find.findChannel(name);
                                        if (theCha > 0) {
                                            final MapleCharacter chr = ChannelServer.getInstance(theCha).getPlayerStorage().getCharacterByName(name);
                                            if (chr != null) {
                                                chr.getClient().getSession().write(PartyPacket.expeditionKick());
                                                if (c.getPlayer().getEventInstance() != null) {
                                                    c.getPlayer().getEventInstance().disbandParty();
                                                }
                                                if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                                                    Event_DojoAgent.failed(c.getPlayer());
                                                }
                                                if (c.getPlayer().getPyramidSubway() != null) {
                                                    //c.getPlayer().getPyramidSubway().disbandParty(c.getPlayer());
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "자기 자신은 강퇴할 수 없습니다.");
                        }
                    }
                }
                break;
            case 47: //give exped leader [cid]
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final MaplePartyCharacter newleader = part.getMemberById(slea.readInt());
                        if (newleader != null) {
                            World.Party.updateParty(part.getId(), PartyOperation.CHANGE_LEADER, newleader);
                            exped.setLeader(newleader.getId());
                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionLeaderChanged(), null);
                        }
                    }
                }
                break;
            case 49: //change party of diff player [partyIndexTo] [cid]
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int partyIndexTo = slea.readInt();
                        final int cid = slea.readInt();
                        if (c.getPlayer().getId() == cid) {
                            break;
                        }
                        for (int i : exped.getParties()) {
                            final MapleParty par = World.Party.getParty(i);
                            if (par != null) {
                                MaplePartyCharacter expelled = par.getMemberById(cid);
                                if (expelled != null) {
                                    if (expelled.isOnline()) {
                                        final MapleCharacter chr = World.getStorage(expelled.getChannel()).getCharacterById(expelled.getId());
                                        if (chr == null) {
                                            break;
                                        }
                                        if (partyIndexTo < exped.getParties().size()) { //already exists
                                            party = World.Party.getParty(exped.getParties().get(partyIndexTo));
                                            if (party == null) {
                                                c.getPlayer().dropMessage(5, "null party.");
                                                break;
                                            }
                                            if (partyIndexTo > exped.getType().maxParty - 1) {
                                                if (party.getMembers().size() >= exped.getType().lastParty) {
                                                    break;
                                                }
                                            }
                                            World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, expelled);
                                            chr.receivePartyMemberHP();
                                            chr.updatePartyMemberHP();
                                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(exped), null);
                                        } else {
                                            World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                            party = World.Party.createPartyAndAdd(expelled, exped.getId());
                                            chr.setParty(party);
                                            chr.getClient().getSession().write(PartyPacket.partyCreated(party.getId()));
                                            chr.getClient().getSession().write(PartyPacket.expeditionUpdate(exped));
                                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                        }
                                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                                            Event_DojoAgent.failed(c.getPlayer());
                                        }
                                        if (c.getPlayer().getEventInstance() != null) {
                                            c.getPlayer().getEventInstance().disbandParty();
                                        }
                                        if (c.getPlayer().getPyramidSubway() != null) {
                                            //c.getPlayer().getPyramidSubway().disbandParty(c.getPlayer());
                                        }
                                    } else {
                                        if (partyIndexTo < exped.getParties().size()) { //already exists
                                            party = World.Party.getParty(exped.getParties().get(partyIndexTo));
                                            if (party == null) {
                                                c.getPlayer().dropMessage(5, "null party.");
                                                break;
                                            }
                                            if (partyIndexTo > exped.getType().maxParty - 1) {
                                                if (party.getMembers().size() >= exped.getType().lastParty) {
                                                    break;
                                                }
                                            }
                                            World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, expelled);
                                        } else {
                                            World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                            party = World.Party.createPartyAndAdd(expelled, exped.getId());
                                            World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            default:
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(6, "원정대 : " + mode + " / " + slea);
                    //System.out.println("Unknown Expedition : " + mode + "\n" + slea);
                }
                break;
        }
    }
}
