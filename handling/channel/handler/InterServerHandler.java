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
import client.SkillFactory;
import constants.GameConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleGuild;
import server.MedalRanking;
import server.maps.FieldLimitType;
import server.marriage.MarriageManager;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CSPacket;

import java.util.ArrayList;
import java.util.List;
import scripting.vm.NPCScriptInvoker;
import server.MaplePortal;
import server.RateManager;
import tools.packet.FamilyPacket;

public class InterServerHandler {

    private static final List<Pair<Integer, String>> npcs = new ArrayList<Pair<Integer, String>>();

    static {
        npcs.add(new Pair<Integer, String>(2012010, "[엘마의 네펜데스 주스] 퀘스트가 없는 분은 클릭하세요."));
    }

    public static void EnterCS(MapleClient c) {
        c.sendPacket(MaplePacketCreator.enableActions());
        if (NPCScriptInvoker.runNpc(c, 9900002, 0) != 0) {
            CashShopEnter(c, c.getPlayer());
        }
    }

    public static final void CashShopEnter(final MapleClient c, final MapleCharacter chr) {
        final MaplePortal closest = chr.getMap().findClosestSpawnpoint(chr.getTruePosition());
        chr.setSpawnpoint((byte) closest.getId());
        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.getEventInstance() != null || c.getChannelServer() == null) {
            c.getSession().write(MaplePacketCreator.serverBlocked(2));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "The server is busy at the moment. Please try again in a minute or less.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //if (c.getChannel() == 1 && !c.getPlayer().isGM()) {
        //    c.getPlayer().dropMessage(5, "You may not enter on this channel. Please change channels and try again.");
        //    c.getSession().write(MaplePacketCreator.enableActions());
        //    return;
        //}
        final ChannelServer ch = ChannelServer.getInstance(c.getChannel());

        chr.changeRemoval();
        if (chr.checkPcTime()) {
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, chr.getCalcPcTime()));
        }
        //PC방
        if (c.burning) {
            chr.dropMessage(-1, "현재 메소 배율: " + RateManager.MESO);
            chr.dropMessage(-1, "현재 드롭 배율: " + RateManager.getTrueDropRate());
            chr.dropMessage(-1, "현재 추가 경험치 배율: " + RateManager.BURNING);
            chr.dropMessage(-1, "현재 버닝이 진행중 입니다.");
        }

        if (chr.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
            World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
        }
        PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());

        World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -10);
        ch.removePlayer(chr);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
        chr.saveToDB(false, false);
        chr.getMap().removePlayer(chr);

        LoginServer.setCodeHash(chr.getId(), c.getCodeHash());
        c.getSession().write(MaplePacketCreator.getChannelChange(c, CashShopServer.getPort()));
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static final void Loggedin(final int playerid, final MapleClient c) {
        final ChannelServer channelServer = c.getChannelServer();
        if (channelServer.isShutdown()) {
            c.getSession().close(true);
            return;
        }
        MapleCharacter player;
        final CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);

        boolean newLogin = false;
        if (transfer == null) { // Player isn't in storage, probably isn't CC
            player = MapleCharacter.loadCharFromDB(playerid, c, true);
            Pair<String, String> ip = LoginServer.getLoginAuth(playerid);
            String s = c.getSessionIPAddress();
            if (ip == null || !s.substring(s.indexOf('/') + 1, s.length()).equals(ip.left)) {
                if (ip != null) {
                    LoginServer.putLoginAuth(playerid, ip.left, ip.right);
                }
                c.getSession().close(true);
                return;
            }
            c.setTempIP(ip.right);
            newLogin = true;
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());
        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close(true);
            return;
        }

        String codeHash = LoginServer.getCodeHash(playerid);
        if (codeHash == null) { // Hash is not found?!
            c.getSession().close(true);
            return;
        }
        c.setCodeHash(codeHash);

        final int state = c.getLoginState();
        boolean allowLogin = false;

        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL || state == MapleClient.LOGIN_NOTLOGGEDIN) {
            allowLogin = !World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close(true);
            return;
        }
        c.getChatBlockData();
        if (c.isChatBlocked()) {
            player.canTalk(false);
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        channelServer.addPlayer(player);
        player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
        player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
        player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));
        player.getStat().recalcLocalStats(false, player, false);
        player.setFh((byte) 0);
        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        c.getSession().write(MaplePacketCreator.enableReport());
        c.sendCRCRequest(0);
        player.removeAll(4031282, false);
        if (player.getMarriageId() > 0 && MarriageManager.getInstance().getMarriage(player.getMarriageId()) == null) {
            player.setMarriageId(0); // 파혼된 결혼/약혼
            for (int i = 1112300; i <= 1112311; ++i) { // 결혼 반지
                player.removeAllEquip(i, false);
            }
            for (int i = 4210000; i <= 4210011; ++i) { // 약혼 반지
                player.removeAll(i, false);
            }
            player.dropMessage(1, "이혼이 성립되었습니다.");
        }
        player.removeAll(2430007, false); //나침반

        if (player.isGM()) {
            //SkillFactory.getSkill(9001004).getEffect(1).applyTo(player);
        }
        //c.getSession().write(MaplePacketCreator.temporaryStats_Reset()); // .
        player.getMap().addPlayer(player);

        c.getSession().write(CSPacket.enableCSUse());
        if (player.checkPcTime()) {
            player.clearPc(true);
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 2, player.getCalcPcTime()));
        } else {
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, player.getCalcPcTime()));
        }
        //PC방

        c.getSession().write(MaplePacketCreator.setNPCScriptable(npcs));
        c.getSession().write(MaplePacketCreator.setQuestTime());

        try {
            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                final MapleParty party = player.getParty();
                World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
//                if (party != null && party.getExpeditionId() > 0) {
//                    final MapleExpedition me = World.Party.getExped(party.getExpeditionId());
//                    if (me != null) {
//                        c.getSession().write(PartyPacket.expeditionStatus(me, false));
//                    }
//                }
            }
            final CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                player.getBuddylist().get(onlineBuddy.getCharacterId()).setChannel(onlineBuddy.getChannel());
            }
            c.getSession().write(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));

            // Start of Messenger
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
            }

            // Start of Guild and alliance
            /*if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs == null) {
                    player.setGuildId((byte) 0);
                    player.setGuildRank((byte) 5);
                    player.saveGuildStatus();
                }
            }*/
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                } else { //guild not found, change guild id
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            }
            if (player.getFamilyId() > 0) {
                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel());
            }
            c.getSession().write(FamilyPacket.getFamilyData());
            c.getSession().write(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
        }
        player.getClient().getSession().write(MaplePacketCreator.serverMessage(channelServer.getServerMessage()));
        player.sendMacros();
        player.showNote();
        //쪽지 확인 필요
        //	player.sendImp();
        player.updatePartyMemberHP();
        player.baseSkills();
        if (newLogin) {
            player.setEquippedTimeAll();
        }
        player.acceptUpdate();

        if (player.haveItem(1142005, 1, true, true) && player.getQuestStatus(29400) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.ExpertHunter).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.ExpertHunter, player.getName(), Integer.parseInt(player.getOneInfo(29400, "mon"))) != 0) {
                //노련한 사냥꾼의 자격이 없다면
                player.removeAllEquip(1142005, false);
                player.dropMessage(5, "전설적인 사냥꾼 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142006, 1, true, true) && player.getQuestStatus(29500) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.Pop).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.Pop, player.getName(), player.getFame()) != 0) {
                player.removeAllEquip(1142006, false);
                player.dropMessage(5, "메이플 아이돌스타 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142014, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.HenesysDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.HenesysDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142014, false);
                player.dropMessage(5, "헤네시스 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142015, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.ElliniaDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.ElliniaDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142015, false);
                player.dropMessage(5, "엘리니아 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142016, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.PerionDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.PerionDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142016, false);
                player.dropMessage(5, "페리온 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142017, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.KerningDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.KerningDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142017, false);
                player.dropMessage(5, "커닝시티 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142018, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.SleepyWoodDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.SleepyWoodDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142018, false);
                player.dropMessage(5, "슬리피우드 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142019, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.NautilusDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.NautilusDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142019, false);
                player.dropMessage(5, "노틸러스 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (player.haveItem(1142030, 1, true, true) && player.getQuestStatus(29503) >= 1) {
            if (MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.LithDonor).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.LithDonor, player.getName(), Integer.parseInt(player.getOneInfo(29503, "money"))) != 0) {
                player.removeAllEquip(1142030, false);
                player.dropMessage(5, "리스항구 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142007, 1, true, true) && player.getQuestStatus(29501) >= 1) {
            String d = player.getQuestNAdd(MapleQuest.getInstance(136000)).getCustomData();
            int d2 = 0;
            if (d == null) {
                d2 = 0;
            }
            d2 = Integer.parseInt(d);
            if (d2 == 0 || MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.HorntailSlayer).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.HorntailSlayer, player.getName(), d2) != 0) {
                player.removeAllEquip(1142007, false);
                player.dropMessage(5, "혼테일 슬레이어 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        if (player.haveItem(1142008, 1, true, true) && player.getQuestStatus(29502) >= 1) {
            String d = player.getQuestNAdd(MapleQuest.getInstance(136001)).getCustomData();
            int d2 = 0;
            if (d == null) {
                d2 = 0;
            }
            d2 = Integer.parseInt(d);
            if (d2 == 0 || MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.PinkbeanSlayer).isEmpty() || MedalRanking.canMedalRank(MedalRanking.MedalRankingType.PinkbeanSlayer, player.getName(), d2) != 0) {
                player.removeAllEquip(1142008, false);
                player.dropMessage(5, "핑크빈 슬레이어 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }

        c.getSession().write(MaplePacketCreator.getKeymap(player.getKeyLayout()));
        player.updatePetAuto();
        player.expirationTask(true, transfer == null);
        player.getStat().recalcLocalStats(false, player, true);
        if (player.getJob() == 132) { // DARKKNIGHT
            player.checkBerserk();
        }
        player.spawnSavedPets();
        if (player.getStat().equippedSummon > 0) {
            SkillFactory.getSkill(player.getStat().equippedSummon).getEffect(1).applyTo(player);
        }
        if (player.getJob() >= 2100 && player.getJob() <= 2112) {
            player.changeSkillLevel(SkillFactory.getSkill(20000014), (byte) -1, (byte) 1);
            player.changeSkillLevel(SkillFactory.getSkill(20000015), (byte) -1, (byte) 1);
            player.changeSkillLevel(SkillFactory.getSkill(20000016), (byte) -1, (byte) 1);
            player.changeSkillLevel(SkillFactory.getSkill(20000017), (byte) -1, (byte) 1);
            player.changeSkillLevel(SkillFactory.getSkill(20000018), (byte) -1, (byte) 1);
        }
        if (c.burning) {
            player.dropMessage(-1, "현재 메소 배율: " + RateManager.MESO);
            player.dropMessage(-1, "현재 드롭 배율: " + RateManager.getTrueDropRate());
            player.dropMessage(-1, "현재 추가 경험치 배율: " + RateManager.BURNING);
            player.dropMessage(-1, "현재 버닝이 진행중 입니다.");
        }
        DueyHandler.checkReceivePackage(player);
        //       player.getStat().createOrUpdateSetOptionEquip(player);
        //세트효과
        if (player.isHidden()) {
            c.getSession().write(MaplePacketCreator.GmHide(player.isHidden()));
            //player.setHidden(false);
        }
        MapleQuestStatus stat = player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        c.getSession().write(MaplePacketCreator.pendantSlot(stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()));
        stat = player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT));
        c.getSession().write(MaplePacketCreator.quickSlot(stat != null && stat.getCustomData() != null && stat.getCustomData().length() == 8 ? stat.getCustomData() : null));
    }

    public static final void ChangeChannel(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.hasBlockedInventory() || chr.getEventInstance() != null || chr.getMap() == null || (chr.isInBlockedMap() && !(chr.getMapId() >= 270010200 && chr.getMapId() <= 270030500)) || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "요청이 많아 일시적으로 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int chc = slea.readByte() + 1;
        if (!World.isChannelAvailable(chc)) {
            chr.dropMessage(1, "채널 수용량을 초과하였습니다. 잠시 후 다시 시도해 주세요.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.changeChannel(chc);
    }
}
