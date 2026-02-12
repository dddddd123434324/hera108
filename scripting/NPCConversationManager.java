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
package scripting;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import client.inventory.Equip;
import client.Skill;
import client.inventory.Item;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import constants.GameConstants;
import client.inventory.ItemFlag;
import client.MapleClient;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.SkillFactory;
import client.SkillEntry;
import client.MapleStat;
import client.PlayerStats;
import client.inventory.MapleRing;
import client.status.MonsterStatus;
import constants.ServerConstants;
import server.MapleCarnivalParty;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.MapleShopFactory;
import server.MapleSquad;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import server.MapleItemInformationProvider;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import database.DatabaseConnection;
import handling.channel.handler.HiredMerchantHandler;
import handling.channel.handler.PlayersHandler;
import handling.login.LoginInformationProvider;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PlayerBuffValueHolder;
import handling.world.World;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuild;
import server.MapleCarnivalChallenge;
import java.util.HashMap;
import handling.world.guild.MapleGuildAlliance;
import java.awt.Point;
import java.io.File;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Iterator;
import javax.script.Invocable;
import server.MapleStatEffect;
import server.MedalRanking;
import server.MedalRanking.MedalRankingType;
import server.RankingWorker;
import server.RateManager;
import server.Timer;
import server.Timer.CloneTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.life.MobSkill;
import server.life.MonsterDropEntry;
import server.life.PlayerNPC;
import server.maps.Event_DojoAgent;
import server.maps.Event_PyramidSubway;
import server.maps.MapleMapObject;
import server.marriage.MarriageDataEntry;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.packet.LoginPacket;
import tools.MaplePacketCreator;

public class NPCConversationManager extends AbstractPlayerInteraction {

    private String getText;
    private byte type; // -1 = NPC, 0 = start quest, 1 = end quest
    private byte lastMsg = -1;
    public boolean pendingDisposal = false;
    private Invocable iv;
    private int objectId;

    public java.util.List<scripting.NPCScriptManager.SkillSearchHit> searchSkills(String keyword, int limit) {
        return scripting.NPCScriptManager.searchSkills(keyword, limit);
    }

    public String getSkillInfo(int skillId, int level) {
        return scripting.NPCScriptManager.getSkillInfoText(skillId, level);
    }
    
    public NPCConversationManager(MapleClient c, int npc, int questid, byte type, Invocable iv) {
        super(c, npc, questid);
        this.type = type;
        this.iv = iv;
    }

    public Invocable getIv() {
        return iv;
    }

    public String getPNPCName() {
        if (id >= 9901000) {
            for (PlayerNPC pnpc : c.getChannelServer().getAllPlayerNPC()) {
                if (pnpc.getId() == id) {
                    return pnpc.getName();
                }
            }
        }
        return "";
    }

    public int getNpc() {
        return id;
    }

    public int getQuest() {
        return id2;
    }

    public byte getType() {
        return type;
    }

    public void safeDispose() {
        pendingDisposal = true;
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(c);
    }

    public void setObjectId(int i) {
        objectId = i;
    }

    public int getObjectId() {
        return objectId;
    }

    public void askMapSelection(final String sel) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().write(MaplePacketCreator.getMapSelection(id, sel));
        lastMsg = (byte) 14;
    }

    public void sendNext(String text) {
        sendNext(text, id);
    }

    public void sendNext(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "00 01", (byte) 0));
        lastMsg = 0;
    }

    public void sendNextS(String text, byte type) {
        sendNextS(text, type, id);
    }

    public void sendNextS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "00 01", type, idd));
        lastMsg = 0;
    }

    public void sendPrev(String text) {
        sendPrev(text, id);
    }

    public void sendPrev(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "01 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendPrevS(String text, byte type) {
        sendPrevS(text, type, id);
    }

    public void sendPrevS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "01 00", type, idd));
        lastMsg = 0;
    }

    public void sendNextPrev(String text) {
        sendNextPrev(text, id);
    }

    public void sendNextPrev(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "01 01", (byte) 0));
        lastMsg = 0;
    }

    public void sendNextPrevS(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text, byte type) {
        sendNextPrevS(text, type, id);
    }

    public void sendNextPrevS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "01 01", type, idd));
        lastMsg = 0;
    }

    public void sendOk(String text) {
        sendOk(text, id);
    }

    public void sendOk(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "00 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendOkS(String text, byte type) {
        sendOkS(text, type, id);
    }

    public void sendOkS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 0, text, "00 00", type, idd));
        lastMsg = 0;
    }

    public void sendYesNo(String text) {
        sendYesNo(text, id);
    }

    public void sendYesNo(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 2, text, "", (byte) 0));
        lastMsg = 1;
    }

    public void sendYesNoS(String text, byte type) {
        sendYesNoS(text, type, id);
    }

    public void sendYesNoS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 2, text, "", type, idd));
        lastMsg = 1;
    }

    public void sendAcceptDecline(String text) {
        askAcceptDecline(text);
    }

    public void sendAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text);
    }

    public void askAcceptDecline(String text) {
        askAcceptDecline(text, id);
    }

    public void askAcceptDecline(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 12, text, "", (byte) 0));
        lastMsg = (byte) 11; // 화스 솟 때매 11
    }

    public void askAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text, id);
    }

    public void askAcceptDeclineNoESC(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 12, text, "", (byte) 1));
        lastMsg = (byte) 11; // 화스 솟 때매 11
    }

    public void askAvatar(String text, int... args) {
        if (lastMsg > -1) {
            return;
        }
        List<Integer> avatars = new ArrayList<>();
        for (int i : args) {
            if (hasPath(i) || i < 100/* && (c.getPlayer().getFace() != i && c.getPlayer().getHair() != i && c.getPlayer().getSkinColor() != i)*/) {
                avatars.add(i);
            }
        }
        int[] avat = new int[avatars.size()];
        for (int i = 0; i < avatars.size(); ++i) {
            avat[i] = avatars.get(i);
        }
        c.getSession().write(MaplePacketCreator.getNPCTalkStyle(id, text, avat));
//        c.getSession().write(MaplePacketCreator.getNPCTalkStyle(id, text, args));
        lastMsg = 8;
    }

    public boolean hasPath(int avatar) {
        StringBuilder path = new StringBuilder("wz/Character.wz/");
        if (avatar >= 20000 && avatar < 30000 || avatar >= 50000 && avatar < 60000) {
            path.append("Face/");
        } else if (avatar >= 30000 && avatar < 50000 || avatar >= 60000) {
            path.append("Hair/");
        } else if (avatar < 100) {
            return true;
        }
        path.append("000" + avatar + ".img.xml");
        File f = new File(path.toString());
        if (!f.exists()) {
            c.getPlayer().dropMessage(5, "Avatar " + avatar + " does not exists..");
        }
        return f.exists();
    }

    public void sendSimple(String text) {
        sendSimple(text, id);
    }

    public void sendSimple(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNext(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 5, text, "", (byte) 0));
        lastMsg = 5;
    }

    public void sendSimpleS(String text, byte type) {
        sendSimpleS(text, type, id);
    }

    public void sendSimpleS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNextS(text, type);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalk(id, (byte) 5, text, "", (byte) type, idd));
        lastMsg = 5;
    }

    public void sendStyle(String text, int styles[]) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalkStyle(id, text, styles));
        lastMsg = 8;
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalkNum(id, text, def, min, max));
        lastMsg = 4;
    }

    public void sendGetText(String text) {
        sendGetText(text, id);
    }

    public void sendGetText(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalkText(id, text));
        lastMsg = 3;
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return getText;
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor((byte) color);
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        List<Integer> avatars = new ArrayList<Integer>();
        for (int i : args_all) {
            if (hasPath(i) || i < 100/* && (c.getPlayer().getFace() != i && c.getPlayer().getHair() != i && c.getPlayer().getSkinColor() != i)*/) {
                avatars.add(i);
            }
        }
        int[] avat = new int[avatars.size()];
        for (int i = 0; i < avatars.size(); ++i) {
            avat[i] = avatars.get(i);
        }
        int args = avat[Randomizer.nextInt(avat.length)];
        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public int setAvatar(int ticket, int args) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();
        return 1;
    }

    public int setAvatar(int args) {
        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public void sendStorage() {
        c.getPlayer().setConversation(4);
        c.getPlayer().getStorage().sendStorage(c, id);
    }

    public void packetTest() {
        final MapleCharacter player = getPlayer();
        final MapleFamilyCharacter mgc = player.getMFC();
        if (player.getMapId() / 1000000 == 923) {
            player.getClient().getSession().write(MaplePacketCreator.showEffect("killing/clear"));
        }
        int quantity = +1000;
        type = 1;
        /*final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Item weapon_item = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -1);
        final Equip toGive = (Equip) ii.getEquipById(weapon_item.getItemId());
        toGive.setUniqueId(MapleInventoryManipulator.getUniqueId(toGive.getItemId(), null, true));
        toGive.setUpgradeSlots((byte) 0);
        toGive.setLevel((byte) 0);
        toGive.setStr((byte) 0);
        toGive.setDex((byte) 0);
        toGive.setInt((byte) 0);
        toGive.setLuk((byte) 0);
        toGive.setHp((byte) 0);
        toGive.setMp((byte) 0);
        toGive.setWatk((byte) 0);
        toGive.setMatk((byte) 0);
        toGive.setWdef((byte) 0);
        toGive.setMdef((byte) 0);
        toGive.setAcc((byte) 0);
        toGive.setAvoid((byte) 0);
        toGive.setHands((byte) 0);
        toGive.setSpeed((byte) 0);
        toGive.setJump((byte) 0);
        toGive.setItemEXP((byte) 0);
        toGive.setGMLog("모루");
        toGive.setDurability((byte) -1);
        toGive.setEnhance((byte) 0);
        toGive.setPotential1((byte) 0);
        toGive.setPotential2((byte) 0);
        toGive.setPotential3((byte) 0);
        toGive.setHpR((byte) 0);
        toGive.setMpR((byte) 0);
        toGive.setIncSkill((byte) 0);
        toGive.setPVPDamage((byte) 0);
        toGive.setCharmEXP((byte) 0);
        toGive.setCubedCount((byte) 0);
        MapleInventoryManipulator.addbyItem(c, toGive);*/
        //c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 3));
        c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 11, (byte) 11));
        //c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 7, (byte) 4));
        //c.getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 9, (byte) 1));
        //c.getSession().write(MaplePacketCreator.report(63));
        //c.getSession().write(MaplePacketCreator.onChatMessage((short) 6, (type == 1 ? "캐시를 " : type == 3 ? "본섭 캐시를 " : "메이플포인트를 ") + (quantity > 0 ? "얻었습니다 (+" + quantity + ")" : "잃었습니다. (" + quantity + ")")));
        //c.getSession().write(MaplePacketCreator.showPlayerRanks(id, RankingWorker.getInstance().getRank()));
        //player.getMap().talkMonster("나를 잘 호위하도록 해. 너무 먼저 멀리 가버리면 아마 모든것은 실패하게 될거야.", 0, 500025); //temporary for now. itemID is located in WZ file

        //c.getSession().write(MaplePacketCreator.musicChange("Bgm25/Title_Japan"));
        //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange("Bgm24/KreaseField"));
//        for (final MapleMapObject mmo : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
//            MapleMonster monster = (MapleMonster) mmo;
//            MapleMonster mob;
//            mob = (MapleMonster) mmo;
//            if (monster != null) {
//                Map<MonsterStatus, Integer> stats = new EnumMap<MonsterStatus, Integer>(MonsterStatus.class);
//                //stats.put(MonsterStatus.WEAPON_DAMAGE_REFLECT, 20000);
//                //stats.put(MonsterStatus.WEAPON_IMMUNITY, 1);
//                stats.put(MonsterStatus.VENOM, 1);
//                List<Integer> reflection = new LinkedList<Integer>();
//                //reflection.add(10000);
//                monster.applyMonsterBuff(stats, 4220005, 1000 * 180, null, reflection);
//                //c.getSession().write(LoginPacket.packetTest(c, monster));
//            }
//        }
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 0, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 1, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 2, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 3, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 4, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 5, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 6, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 7, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 8, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 9, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 10, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 11, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 12, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 15, "안녕!"));
//        final StringBuilder sb = new StringBuilder();
//        sb.append(c.getPlayer().getName());
//        sb.append(" : ");
//        sb.append("ㅎㅇㅎㅇ");
//        //c.getSession().write(MaplePacketCreator.onChatMessage((short) 17, sb.toString()));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 16, sb.toString()));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 18, "안녕!"));
//        c.getSession().write(MaplePacketCreator.onChatMessage((short) 19, "안녕!"));
//        final MapleMonster mob_from = player.getMap().getMonsterById(500006); // From
//        mob_from.setLastNode(2);
//        player.getMap().talkMonster("봉인이 잘 되었는지 확인을 해볼까?", 0, mob_from.getObjectId());
//            player.getClient().getSession().write(MaplePacketCreator.getNodeProperties(mob_from, player.getMap()));
//        player.getMap().getReactorById(2118003).forceHitReactor((byte) 1);
//        Timer.EventTimer.getInstance().schedule(new Runnable() {
//            @Override
//            public final void run() {
//                player.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300281), new Point(328, 174));
//            }
//        }, 2000);
        //spawnMonster(9300281, 328, 174);
        //player.getMap().talkMonster("이런 자물쇠 따위. 낄낄.", 0, mob_from.getObjectId());
//        for (int i = 0; i < 1; i++) {
//            c.getPlayer().levelUp();
//            c.getPlayer().setExp(0);
//            c.getPlayer().updateSingleStat(MapleStat.EXP, c.getPlayer().getExp());
//        }
        //c.getSession().write(MaplePacketCreator.serverNotice(4,1,"[안내] 이 기능을 찾아 냈다구요??!! 어서빨리 캐시샵에서 {}를 만나보세요",false));
        //c.getSession().write(MaplePacketCreator.boatPacket(1034));
        //c.getSession().write(MaplePacketCreator.boatEffect(1034));
        //c.getSession().write(MaplePacketCreator.boatPacket(true));
        //c.getSession().write(MaplePacketCreator.skillCooldown(1001, 1500000000));
        //c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 29)); // 26 아이템과 메소를 모두 찾았어.
        // c.getSession().write(MaplePacketCreator.getINITIALQuiz(2041024, "dsadsadsa")); // removeQuestTimeLimit
        //c.getSession().write(MaplePacketCreator.showPQreward(c.getPlayer().getId()));
        //c.getSession().write(MaplePacketCreator.addQuestTimeLimit(23127, 3*60*1000));
        //c.getSession().write(MaplePacketCreator.removeQuestTimeLimit(23127));
        //c.getSession().write(MaplePacketCreator.sendString(3, "fieldsetkeeptime", "3"));
        //c.getPlayer().tryPartyQuest(1206);
        //c.getPlayer().endPartyQuest(1206);
        //c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showPQreward(c.getPlayer().getId()));
        //c.getSession().write(MaplePacketCreator.showFlameEffect(player.getId()));
        //    c.getSession().write(MaplePacketCreator.showFlameEffect2());
    }

    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c);
    }

    public void openShopNPC(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c, this.id);
    }

    public int gainGachaponItem(int id, int quantity) {
        return gainGachaponItem(id, quantity, c.getPlayer().getMap().getStreetName() + " - " + c.getPlayer().getMap().getMapName());
    }

    public int gainGachaponItem(int id, int quantity, final String msg) {
        try {
            if (!MapleItemInformationProvider.getInstance().itemExists(id)) {
                return -1;
            }
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, id, (short) quantity);

            if (item == null) {
                return -1;
            }
            final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
            if (rareness > 0) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega("[" + msg + "] " + c.getPlayer().getName(), " : Lucky winner of Gachapon!", item, rareness));
            }
            return item.getItemId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void changeJob(int job) {
        c.getPlayer().changeJob(job);
    }

    public void startQuest(int idd) {
        MapleQuest.getInstance(idd).start(getPlayer(), id);
    }

    public void completeQuest(int idd) {
        MapleQuest.getInstance(idd).complete(getPlayer(), id);
    }

    public void forfeitQuest(int idd) {
        MapleQuest.getInstance(idd).forfeit(getPlayer());
    }

    public void forceStartQuest() {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(int idd) {
        MapleQuest.getInstance(idd).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(String customData) {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), customData);
    }

    public void forceCompleteQuest() {
        MapleQuest.getInstance(id2).forceComplete(getPlayer(), getNpc());
    }

    public void forceCompleteQuest(final int idd) {
        MapleQuest.getInstance(idd).forceComplete(getPlayer(), getNpc());
    }

    public String getQuestCustomData() {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).getCustomData();
    }

    public void setQuestCustomData(String customData) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).setCustomData(customData);
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainAp(final int amount) {
        c.getPlayer().gainAp((short) amount);
    }

    public void expandInventory(byte type, int amt) {
        c.getPlayer().expandInventory(type, amt);
    }

    public void unequipEverything() {
        MapleInventory equipped = getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<Short> ids = new LinkedList<Short>();
        for (Item item : equipped.newList()) {
            ids.add(item.getPosition());
        }
        for (short id : ids) {
            MapleInventoryManipulator.unequip(getC(), id, equip.getNextFreeSlot());
        }
    }

    public final void clearSkills() {
        Map<Skill, SkillEntry> skills = new HashMap<Skill, SkillEntry>(getPlayer().getSkills());
        for (Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            getPlayer().changeSkillLevel(skill.getKey(), (byte) 0, (byte) 0);
        }
        skills.clear();
    }

    public boolean hasSkill(int skillid) {
        Skill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return c.getPlayer().getSkillLevel(theSkill) > 0;
        }
        return false;
    }

    public void updateBuddyCapacity(int capacity) {
        c.getPlayer().setBuddyCapacity((byte) capacity);
    }

    public int getBuddyCapacity() {
        return c.getPlayer().getBuddyCapacity();
    }

    public int partyMembersInMap() {
        int inMap = 0;
        if (getPlayer().getParty() == null) {
            return inMap;
        }
        for (MapleCharacter char2 : getPlayer().getMap().getCharactersThreadsafe()) {
            if (char2.getParty() != null && char2.getParty().getId() == getPlayer().getParty().getId()) {
                inMap++;
            }
        }
        return inMap;
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>(); // creates an empty array full of shit..
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) { // double check <3
                    chars.add(ch);
                }
            }
        }
        return chars;
    }

    public void warpPartyWithExp(int mapId, int exp) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            gainMeso(meso);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
                curChar.gainMeso(meso, true);
            }
        }
    }

    public MapleSquad getSquad(String type) {
        return c.getChannelServer().getMapleSquad(type);
    }

    public int getSquadAvailability(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        }
        return squad.getStatus();
    }

    public boolean registerSquad(String type, int minutes, String startText) {
        if (c.getChannelServer().getMapleSquad(type) == null) {
            final MapleSquad squad = new MapleSquad(c.getChannel(), type, c.getPlayer(), minutes * 60 * 1000, startText);
            final boolean ret = c.getChannelServer().addMapleSquad(squad, type);
            if (ret) {
                final MapleMap map = c.getPlayer().getMap();

                map.broadcastMessage(MaplePacketCreator.getClock(minutes * 60));
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + startText));
            } else {
                squad.clear();
            }
            return ret;
        }
        return false;
    }

    public boolean getSquadList(String type, byte type_) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad == null) {
                return false;
            }
            if (type_ == 0 || type_ == 3) { // Normal viewing
                sendNext(squad.getSquadMemberString(type_));
            } else if (type_ == 1) { // Squad Leader banning, Check out banned participant
                sendSimple(squad.getSquadMemberString(type_));
            } else if (type_ == 2) {
                if (squad.getBannedMemberSize() > 0) {
                    sendSimple(squad.getSquadMemberString(type_));
                } else {
                    sendNext(squad.getSquadMemberString(type_));
                }
            }
            return true;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return false;
        }
    }

    public byte isSquadLeader(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else if (squad.getLeader() != null && squad.getLeader().getId() == c.getPlayer().getId()) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean reAdd(String eim, String squad) {
        EventInstanceManager eimz = getDisconnected(eim);
        MapleSquad squadz = getSquad(squad);
        if (eimz != null && squadz != null) {
            squadz.reAddMember(getPlayer());
            eimz.registerPlayer(getPlayer());
            return true;
        }
        return false;
    }

    public void banMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(pos);
        }
    }

    public void acceptMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.acceptMember(pos);
        }
    }

    public int addMember(String type, boolean join) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad != null) {
                return squad.addMember(c.getPlayer(), join);
            }
            return -1;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return -1;
        }
    }

    public byte isSquadMember(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else if (squad.getMembers().contains(c.getPlayer())) {
            return 1;
        } else if (squad.isBanned(c.getPlayer())) {
            return 2;
        } else {
            return 0;
        }
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void genericGuildMessage(int code) {
        c.getSession().write(MaplePacketCreator.genericGuildMessage((byte) code));
    }

    public void disbandGuild() {
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0 || c.getPlayer().getGuildRank() != 1) {
            return;
        }
        World.Guild.disbandGuild(gid);
    }

    public void increaseGuildCapacity(boolean trueMax) {
        if (c.getPlayer().getMeso() < 500000 && !trueMax) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "자네.. 메소는 충분히 갖고 있는건가?"));
            return;
        }
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0) {
            return;
        }
        if (World.Guild.increaseGuildCapacity(gid, trueMax)) {
            if (!trueMax) {
                c.getPlayer().gainMeso(-500000, true, true);
            } else {
                gainGP(-2000);
            }
            //sendNext("축하하네~ 길드 최대 인원이 늘어났네.");
        } else if (!trueMax) {
            sendNext("이미 길드 최대 인원 제한인 100 명이 된 것 같군.");
        } else {
            sendNext("길드 포인트가 충분히 있는지, 또는 이미 최대 인원 200명이 된건 아닌지 확인해 보게나.");
        }
    }

    public void displayGuildRanks() {
        c.getSession().write(MaplePacketCreator.showGuildRanks(id, MapleGuildRanking.getInstance().getRank()));
    }

    public void displayPlayerRanks() {
        c.getSession().write(MaplePacketCreator.showPlayerRanks(id, RankingWorker.getInstance().getRank()));
    }

    public boolean removePlayerFromInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            c.getPlayer().getEventInstance().removePlayer(c.getPlayer());
            return true;
        }
        return false;
    }

    public boolean isPlayerInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            return true;
        }
        return false;
    }

    public void changeStat(byte slot, int type, short amount) {
        Equip sel = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                sel.setStr(amount);
                break;
            case 1:
                sel.setDex(amount);
                break;
            case 2:
                sel.setInt(amount);
                break;
            case 3:
                sel.setLuk(amount);
                break;
            case 4:
                sel.setHp(amount);
                break;
            case 5:
                sel.setMp(amount);
                break;
            case 6:
                sel.setWatk(amount);
                break;
            case 7:
                sel.setMatk(amount);
                break;
            case 8:
                sel.setWdef(amount);
                break;
            case 9:
                sel.setMdef(amount);
                break;
            case 10:
                sel.setAcc(amount);
                break;
            case 11:
                sel.setAvoid(amount);
                break;
            case 12:
                sel.setHands(amount);
                break;
            case 13:
                sel.setSpeed(amount);
                break;
            case 14:
                sel.setJump(amount);
                break;
            case 15:
                sel.setUpgradeSlots((byte) amount);
                break;
            case 16:
                sel.setViciousHammer((byte) amount);
                break;
            case 17:
                sel.setLevel((byte) amount);
                break;
            case 18:
                sel.setEnhance((byte) amount);
                break;
            case 19:
                sel.setPotential1(amount);
                break;
            case 20:
                sel.setPotential2(amount);
                break;
            case 21:
                sel.setPotential3(amount);
                break;
            case 22:
                sel.setOwner(getText());
                break;
            default:
                break;
        }
        c.getPlayer().equipChanged();
    }

    public void openDuey() {
        c.getPlayer().setConversation(2);
        c.getSession().write(MaplePacketCreator.sendDuey((byte) 9, null, null));
    }

    public void openMerchantItemStore() {
        c.getPlayer().setConversation(3);
        HiredMerchantHandler.displayMerch(c);
        //c.getSession().write(PlayerShopPacket.merchItemStore((byte) 0x22));
        //c.getPlayer().dropMessage(5, "Please enter ANY 13 characters.");
    }

    public void sendRepairWindow() {
        c.getSession().write(MaplePacketCreator.sendRepairWindow(id));
    }

    public final short getKegs() {
        return c.getChannelServer().getFireWorks().getKegsPercentage();
    }

    public void giveKegs(final int kegs) {
        c.getChannelServer().getFireWorks().giveKegs(c.getPlayer(), kegs);
    }

    public final short getSunshines() {
        return c.getChannelServer().getFireWorks().getSunsPercentage();
    }

    public void addSunshines(final int kegs) {
        c.getChannelServer().getFireWorks().giveSuns(c.getPlayer(), kegs);
    }

    public final short getDecorations() {
        return c.getChannelServer().getFireWorks().getDecsPercentage();
    }

    public void addDecorations(final int kegs) {
        try {
            c.getChannelServer().getFireWorks().giveDecs(c.getPlayer(), kegs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final MapleCarnivalParty getCarnivalParty() {
        return c.getPlayer().getCarnivalParty();
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return c.getPlayer().getNextCarnivalRequest();
    }

    public final MapleCarnivalChallenge getCarnivalChallenge(MapleCharacter chr) {
        return new MapleCarnivalChallenge(chr);
    }

    public void HpReverseMp() { // HP를 MP로 By. 가군
        final PlayerStats playerst = c.getPlayer().getStat();
        Map<MapleStat, Integer> statupdate = new EnumMap<MapleStat, Integer>(MapleStat.class);

        int maxhp = playerst.getMaxHp(), maxmp = playerst.getMaxMp();

        if (getJob() == 0 || getJob() == 1000 || getJob() == 2000) { // Beginner
            maxhp -= Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (getJob() >= 100 && getJob() <= 132) { // Warrior
            maxhp -= Randomizer.rand(24, 28);
            maxmp += Randomizer.rand(4, 6);
        } else if (getJob() >= 200 && getJob() <= 232) { // Magician
            maxhp -= Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(22, 24);
        } else if (getJob() >= 1200 && getJob() <= 1212) { // Magician
            maxhp -= Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(22, 24);
        } else if ((getJob() >= 300 && getJob() <= 322) || (getJob() >= 400 && getJob() <= 434) || (getJob() >= 1300 && getJob() <= 1311) || (getJob() >= 1400 && getJob() <= 1411)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp -= Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((getJob() >= 510 && getJob() <= 512) || (getJob() >= 1510 && getJob() <= 1512)) { // Pirate
            maxhp -= Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((getJob() >= 500 && getJob() <= 532) || getJob() == 1500) { // Pirate
            maxhp -= Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        }

        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        playerst.setMaxHp(maxhp, c.getPlayer());
        playerst.setMaxMp(maxmp, c.getPlayer());
        statupdate.put(MapleStat.MAXHP, (int) maxhp);
        statupdate.put(MapleStat.MAXMP, (int) maxmp);
        c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true, c.getPlayer().getJob()));
    }

    public void MpReverseHp() { // MP를 HP로 By. 가군
        final PlayerStats playerst = c.getPlayer().getStat();
        Map<MapleStat, Integer> statupdate = new EnumMap<MapleStat, Integer>(MapleStat.class);

        int maxhp = playerst.getMaxHp(), maxmp = playerst.getMaxMp();

        if (getJob() == 0 || getJob() == 1000 || getJob() == 2000) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp -= Randomizer.rand(10, 12);
        } else if (getJob() >= 100 && getJob() <= 132) { // Warrior
            maxhp += Randomizer.rand(24, 28);
            maxmp -= Randomizer.rand(4, 6);
        } else if (getJob() >= 200 && getJob() <= 232) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp -= Randomizer.rand(22, 24);
        } else if (getJob() >= 1200 && getJob() <= 1212) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp -= Randomizer.rand(22, 24);
        } else if ((getJob() >= 300 && getJob() <= 322) || (getJob() >= 400 && getJob() <= 434) || (getJob() >= 1300 && getJob() <= 1311) || (getJob() >= 1400 && getJob() <= 1411)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp -= Randomizer.rand(14, 16);
        } else if ((getJob() >= 510 && getJob() <= 512) || (getJob() >= 1510 && getJob() <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp -= Randomizer.rand(18, 22);
        } else if ((getJob() >= 500 && getJob() <= 532) || getJob() == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp -= Randomizer.rand(18, 22);
        }

        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        playerst.setMaxHp(maxhp, c.getPlayer());
        playerst.setMaxMp(maxmp, c.getPlayer());
        statupdate.put(MapleStat.MAXHP, (int) maxhp);
        statupdate.put(MapleStat.MAXMP, (int) maxmp);
        c.getSession().write(MaplePacketCreator.updatePlayerStats(statupdate, true, c.getPlayer().getJob()));
    }

    public void maxStats() {
        Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
        c.getPlayer().getStat().str = (short) 32767;
        c.getPlayer().getStat().dex = (short) 32767;
        c.getPlayer().getStat().int_ = (short) 32767;
        c.getPlayer().getStat().luk = (short) 32767;

        c.getPlayer().getStat().maxhp = 99999;
        c.getPlayer().getStat().maxmp = 99999;
        c.getPlayer().getStat().setHp(99999, c.getPlayer());
        c.getPlayer().getStat().setMp(99999, c.getPlayer());

        statup.put(MapleStat.STR, Integer.valueOf(32767));
        statup.put(MapleStat.DEX, Integer.valueOf(32767));
        statup.put(MapleStat.LUK, Integer.valueOf(32767));
        statup.put(MapleStat.INT, Integer.valueOf(32767));
        statup.put(MapleStat.HP, Integer.valueOf(99999));
        statup.put(MapleStat.MAXHP, Integer.valueOf(99999));
        statup.put(MapleStat.MP, Integer.valueOf(99999));
        statup.put(MapleStat.MAXMP, Integer.valueOf(99999));
        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
        c.getSession().write(MaplePacketCreator.updatePlayerStats(statup, c.getPlayer().getJob()));
    }

    public boolean getSR(Triple<String, Map<Integer, String>, Long> ma, int sel) {
        if (ma.mid.get(sel) == null || ma.mid.get(sel).length() <= 0) {
            dispose();
            return false;
        }
        sendOk(ma.mid.get(sel));
        return true;
    }

    public Equip getEquip(int itemid) {
        return (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
    }

    public void setExpiration(Object statsSel, long expire) {
        if (statsSel instanceof Equip) {
            ((Equip) statsSel).setExpiration(System.currentTimeMillis() + (expire * 24 * 60 * 60 * 1000));
        }
    }

    public void setLock(Object statsSel) {
        if (statsSel instanceof Equip) {
            Equip eq = (Equip) statsSel;
            if (eq.getExpiration() == -1) {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.LOCK.getValue()));
            } else {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
            }
        }
    }

    public boolean addFromDrop(Object statsSel) {
        if (statsSel instanceof Item) {
            final Item it = (Item) statsSel;
            return MapleInventoryManipulator.checkSpace(getClient(), it.getItemId(), it.getQuantity(), it.getOwner()) && MapleInventoryManipulator.addFromDrop(getClient(), it, false);
        }
        return false;
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type) {
        return replaceItem(slot, invType, statsSel, offset, type, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type, boolean takeSlot) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        Item item = getPlayer().getInventory(inv).getItem((byte) slot);
        if (item == null || statsSel instanceof Item) {
            item = (Item) statsSel;
        }
        if (offset > 0) {
            if (inv != MapleInventoryType.EQUIP) {
                return false;
            }
            Equip eq = (Equip) item;
            if (takeSlot) {
                if (eq.getUpgradeSlots() < 1) {
                    return false;
                } else {
                    eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() - 1));
                }
                if (eq.getExpiration() == -1) {
                    eq.setFlag((byte) (eq.getFlag() | ItemFlag.LOCK.getValue()));
                } else {
                    eq.setFlag((byte) (eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
                }
            }
            if (type.equalsIgnoreCase("Slots")) {
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + offset));
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("Level")) {
                eq.setLevel((byte) (eq.getLevel() + offset));
            } else if (type.equalsIgnoreCase("Hammer")) {
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("STR")) {
                eq.setStr((short) (eq.getStr() + offset));
            } else if (type.equalsIgnoreCase("DEX")) {
                eq.setDex((short) (eq.getDex() + offset));
            } else if (type.equalsIgnoreCase("INT")) {
                eq.setInt((short) (eq.getInt() + offset));
            } else if (type.equalsIgnoreCase("LUK")) {
                eq.setLuk((short) (eq.getLuk() + offset));
            } else if (type.equalsIgnoreCase("HP")) {
                eq.setHp((short) (eq.getHp() + offset));
            } else if (type.equalsIgnoreCase("MP")) {
                eq.setMp((short) (eq.getMp() + offset));
            } else if (type.equalsIgnoreCase("WATK")) {
                eq.setWatk((short) (eq.getWatk() + offset));
            } else if (type.equalsIgnoreCase("MATK")) {
                eq.setMatk((short) (eq.getMatk() + offset));
            } else if (type.equalsIgnoreCase("WDEF")) {
                eq.setWdef((short) (eq.getWdef() + offset));
            } else if (type.equalsIgnoreCase("MDEF")) {
                eq.setMdef((short) (eq.getMdef() + offset));
            } else if (type.equalsIgnoreCase("ACC")) {
                eq.setAcc((short) (eq.getAcc() + offset));
            } else if (type.equalsIgnoreCase("Avoid")) {
                eq.setAvoid((short) (eq.getAvoid() + offset));
            } else if (type.equalsIgnoreCase("Hands")) {
                eq.setHands((short) (eq.getHands() + offset));
            } else if (type.equalsIgnoreCase("Speed")) {
                eq.setSpeed((short) (eq.getSpeed() + offset));
            } else if (type.equalsIgnoreCase("Jump")) {
                eq.setJump((short) (eq.getJump() + offset));
            } else if (type.equalsIgnoreCase("ItemEXP")) {
                eq.setItemEXP(eq.getItemEXP() + offset);
            } else if (type.equalsIgnoreCase("Expiration")) {
                eq.setExpiration((long) (eq.getExpiration() + offset));
            } else if (type.equalsIgnoreCase("Flag")) {
                eq.setFlag((byte) (eq.getFlag() + offset));
            }
            item = eq.copy();
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), inv, (short) slot, item.getQuantity(), false);
        return MapleInventoryManipulator.addFromDrop(getClient(), item, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int upgradeSlots) {
        return replaceItem(slot, invType, statsSel, upgradeSlots, "Slots");
    }

    public boolean itemExists(final int itemId) {
        return MapleItemInformationProvider.getInstance().itemExists(itemId);
    }

    public boolean isCash(final int itemId) {
        return MapleItemInformationProvider.getInstance().isCash(itemId);
    }

    public int getTotalStat(final int itemId) {
        return MapleItemInformationProvider.getInstance().getTotalStat((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
    }

    public int getReqLevel(final int itemId) {
        return MapleItemInformationProvider.getInstance().getReqLevel(itemId);
    }

    public MapleStatEffect getEffect(int buff) {
        return MapleItemInformationProvider.getInstance().getItemEffect(buff);
    }

    public void buffGuild(final int buff, final int duration, final String msg) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getItemEffect(buff) != null && getPlayer().getGuildId() > 0) {
            final MapleStatEffect mse = ii.getItemEffect(buff);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                    if (chr.getGuildId() == getPlayer().getGuildId()) {
                        mse.applyTo(chr, chr, true, null, duration, true);
                        chr.dropMessage(5, "Your guild has gotten a " + msg + " buff.");
                    }
                }
            }
        }
    }

    public void givePartyBuff(int buff) {
        if (c.getPlayer().getParty() != null) {
            MapleStatEffect effect = getEffect(buff);
            for (MaplePartyCharacter pchr : c.getPlayer().getParty().getMembers()) {
                MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pchr.getId());
                if (chr != null) {
                    effect.applyTo(chr);
                }
            }
        }
    }

    public void cancelBuff(int buff) {
        boolean canCancel = false;
        for (PlayerBuffValueHolder pbvh : new ArrayList<PlayerBuffValueHolder>(c.getPlayer().getAllBuffs())) {
            if (!pbvh.effect.isSkill() && pbvh.effect.getSourceId() == buff) {
                canCancel = true;
                break;
            }
        }
        if (canCancel) {
            c.getPlayer().cancelEffect(getEffect(buff), -1);
        }
    }

    public boolean createAlliance(String alliancename) {
        MapleParty pt = c.getPlayer().getParty();
        MapleCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(pt.getMemberByIndex(1).getId());
        if (otherChar == null || otherChar.getId() == c.getPlayer().getId()) {
            return false;
        }
        try {
            return World.Alliance.createAlliance(alliancename, c.getPlayer().getId(), otherChar.getId(), c.getPlayer().getGuildId(), otherChar.getGuildId());
        } catch (Exception re) {
            re.printStackTrace();
            return false;
        }
    }

    public boolean addCapacityToAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1 && getMeso() > 5000000) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.changeAllianceCapacity(gs.getAllianceId())) {
                    gainMeso(-MapleGuildAlliance.CHANGE_CAPACITY_COST);
                    return true;
                }
            }
        } catch (Exception re) {
            re.printStackTrace();
        }
        return false;
    }

    public boolean disbandAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.disbandAlliance(gs.getAllianceId())) {
                    return true;
                }
            }
        } catch (Exception re) {
            re.printStackTrace();
        }
        return false;
    }

    public byte getLastMsg() {
        return lastMsg;
    }

    public final void setLastMsg(final byte last) {
        this.lastMsg = last;
    }

    public final void maxAllSkills() {
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (GameConstants.isApplicableSkill(skil.getId()) && skil.getId() < 90000000) { //no db/additionals/resistance skills
                teachSkill(skil.getId(), (byte) skil.getMaxLevel(), (byte) skil.getMaxLevel());
            }
        }
    }

    public final void maxSkillsByJob() {
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (GameConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(getPlayer().getJob())) { //no db/additionals/resistance skills
                teachSkill(skil.getId(), (byte) skil.getMaxLevel(), (byte) skil.getMaxLevel());
            }
        }
    }

    public final void resetStats(int str, int dex, int z, int luk) {
        c.getPlayer().resetStats(str, dex, z, luk);
    }

    public void giveLessSP(int maxLvl, int advLvl) {
        int diffLvl = Math.min(maxLvl, c.getPlayer().getLevel()) - advLvl;
        c.getPlayer().gainSP(diffLvl * 3);
    }

    public final boolean dropItem(int slot, int invType, int quantity) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        return MapleInventoryManipulator.drop(c, inv, (short) slot, (short) quantity, true);
    }

    public final void sendRPS() {
        c.getSession().write(MaplePacketCreator.getRPSMode((byte) 8, -1, -1, -1));
    }

    public final void setQuestRecord(Object ch, final int questid, final String data) {
        ((MapleCharacter) ch).getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(data);
    }

    public final void doWeddingEffect(final Object ch) {
        final MapleCharacter chr = (MapleCharacter) ch;
        final MapleCharacter player = getPlayer();
        getMap().broadcastMessage(MaplePacketCreator.yellowChat(player.getName() + ", do you take " + chr.getName() + " as your wife and promise to stay beside her through all downtimes, crashes, and lags?"));
        CloneTimer.getInstance().schedule(new Runnable() {
            public void run() {
                if (chr == null || player == null) {
                    warpMap(680000500, 0);
                } else {
                    chr.getMap().broadcastMessage(MaplePacketCreator.yellowChat(chr.getName() + ", do you take " + player.getName() + " as your husband and promise to stay beside him through all downtimes, crashes, and lags?"));
                }
            }
        }, 10000);
        CloneTimer.getInstance().schedule(new Runnable() {
            public void run() {
                if (chr == null || player == null) {
                    if (player != null) {
                        setQuestRecord(player, 160001, "3");
                        setQuestRecord(player, 160002, "0");
                    } else if (chr != null) {
                        setQuestRecord(chr, 160001, "3");
                        setQuestRecord(chr, 160002, "0");
                    }
                    warpMap(680000500, 0);
                } else {
                    setQuestRecord(player, 160001, "2");
                    setQuestRecord(chr, 160001, "2");
                    sendNPCText(player.getName() + " and " + chr.getName() + ", I wish you two all the best on your " + chr.getClient().getChannelServer().getServerName() + " journey together!", 9201002);
                    chr.getMap().startExtendedMapEffect("You may now kiss the bride, " + player.getName() + "!", 5120006);
                    if (chr.getGuildId() > 0) {
                        World.Guild.guildPacket(chr.getGuildId(), MaplePacketCreator.sendMarriage(false, chr.getName()));
                    }
                }
            }
        }, 20000); //10 sec 10 sec

    }

    public void putKey(int key, int type, int action) {
        getPlayer().changeKeybinding(key, (byte) type, action);
        getClient().getSession().write(MaplePacketCreator.getKeymap(getPlayer().getKeyLayout()));
    }

    public void doRing(final String name, final int itemid) {
        PlayersHandler.DoRing(getClient(), name, itemid);
    }

    public int getNaturalStats(final int itemid, final String it) {
        Map<String, Integer> eqStats = MapleItemInformationProvider.getInstance().getEquipStats(itemid);
        if (eqStats != null && eqStats.containsKey(it)) {
            return eqStats.get(it);
        }
        return 0;
    }

    public boolean isEligibleName(String t) {
        return MapleCharacterUtil.canCreateChar(t, getPlayer().isGM()) && (!LoginInformationProvider.getInstance().isForbiddenName(t) || getPlayer().isGM());
    }

    public String checkDrop(int mobId) {
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        if (ranks != null && ranks.size() > 0) {
            int num = 0, itemId = 0, ch = 0;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    if (num == 0) {
                        name.append("몹 이름 : #o" + mobId + "# (몹 코드 : " + mobId + ")\r\n");
                        name.append("───────────────────────────\r\n");
                    }
                    String namez = "#z" + itemId + "#";
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        namez = (de.Minimum * RateManager.DISPLAY_MESO) + "~" + (de.Maximum * RateManager.DISPLAY_MESO) + " 메소";
                    }
                    ch = de.chance * RateManager.DISPLAY_DROP;
                    name.append("#L" + itemId + "#" + (num + 1) + ") #v" + itemId + "#" + namez + " - " + (Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "%" + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("퀘스트 : " + MapleQuest.getInstance(de.questid).getName() + "") : "") + "\r\n");
                    num++;
                }
            }
            if (name.length() > 0) {
                return name.toString();
            }

        }
        return "아무것도 드롭하지 않거나 없는 몬스터 코드네요.";
    }

    public String getLeftPadded(final String in, final char padchar, final int length) {
        return StringUtil.getLeftPaddedStr(in, padchar, length);
    }

    public String getReadableMillis(long startMillis, long endMillis) {
        return StringUtil.getReadableMillis(startMillis, endMillis);
    }

    public void sendUltimateExplorer() {
        getClient().getSession().write(MaplePacketCreator.ultimateExplorer());
    }

    public void sendPendant(boolean b) {
        c.getSession().write(MaplePacketCreator.pendantSlot(b));
    }

    public void showNpcSpecialEffect(String str) {
        showNpcSpecialEffect(getNpc(), str);
    }

    public void showNpcSpecialEffect(int npcid, String str) {
        MapleMap map = getPlayer().getMap();
        for (MapleNPC obj : map.getAllNPCs()) {
            if (obj.getId() == npcid) {
                map.broadcastMessage(MaplePacketCreator.showNpcSpecialAction(obj.getObjectId(), str), obj.getPosition());
            }
        }
    }

    public final int getDojoPoints() {
        return dojo_getPts();
    }

    public final int dojo_getPts() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO);
    }

    public final int getDojoRecord() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO_RECORD);
    }

    public void setDojoRecord(final boolean reset) {
        if (reset) {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData("0");
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData("0");
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData(String.valueOf(c.getPlayer().getIntRecord(GameConstants.DOJO_RECORD) + 1));
        }
    }

    public boolean start_DojoAgent(final boolean dojo, final boolean party) {
        if (dojo) {
            return Event_DojoAgent.warpStartDojo(c.getPlayer(), party);
        }
        return Event_DojoAgent.warpStartAgent(c.getPlayer(), party);
    }

    public boolean start_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpStartPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpStartSubway(c.getPlayer());
    }

    public boolean bonus_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpBonusPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpBonusSubway(c.getPlayer());
    }

    public void openWeddingPresent(int type, int gender) {
        MarriageDataEntry dataEntry = getMarriageAgent().getDataEntry();
        if (dataEntry != null) {
            if (type == 1) { // give
                c.getPlayer().setWeddingGive(gender);
                List<String> wishes;
                if (gender == 0) {
                    wishes = dataEntry.getGroomWishList();
                } else {
                    wishes = dataEntry.getBrideWishList();
                }
                c.sendPacket(MaplePacketCreator.showWeddingWishGiveDialog(wishes));
            } else if (type == 2) { // recv
                List<Item> gifts;
                if (gender == 0) {
                    gifts = dataEntry.getGroomPresentList();
                } else {
                    gifts = dataEntry.getBridePresentList();
                }
                c.sendPacket(MaplePacketCreator.showWeddingWishRecvDialog(gifts));
            }
        }
    }

    public boolean exchangeWeddingRing() {
        for (int i = 4210000; i <= 4210011; ++i) {
            int newItemId = 1112300 + (i % 100);
            if (haveItem(i, 1) && canHold(newItemId, 1)) {
                Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).findById(i);
                MapleRing ring = item.getRing();
                gainItem(i, (short) -1);
                try {
                    MapleRing.changeItemIdByUniqueId(newItemId, ring.getRingId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                Item newRing = MapleItemInformationProvider.getInstance().getEquipById(newItemId, ring.getRingId());
//                newRing.setRing(ring);
//                ring.setItemId(newItemId);
//                MapleInventoryManipulator.addbyItem(c, newRing);
                gainItem(newItemId, (short) 1);
                c.getPlayer().equipChanged();
                return true;
            }
        }
        return false;
    }

    public double getDistance() {
        return getPlayer().getMap().getNPCByOid(getObjectId()).getPosition().distanceSq(getPlayer().getPosition());
    }

    public String getMedalRanking(String type) {
        String ret = "현재 순위 ";
        ret += "\r\n\r\n";
        List<Pair<String, Integer>> l = MedalRanking.getReadOnlyRanking(MedalRanking.MedalRankingType.valueOf(type));
        if (l.isEmpty()) {
            ret += "현재 랭킹이 없습니다.";
        } else {
            int rank = 1;
            for (Pair<String, Integer> p : l) {
                String str;
                if (MedalRanking.MedalRankingType.valueOf(type).isDonor()) {
                    if (rank == 1) {
                        str = new DecimalFormat("#,###").format(p.getRight()).replace("0", "?").replace("1", "?").replace("2", "?").replace("3", "?").replace("4", "?").replace("5", "?").replace("6", "?").replace("7", "?").replace("8", "?").replace("9", "?") + "#k 메소";
                    } else {
                        str = new DecimalFormat("#,###").format(p.getRight()) + "#k 메소";
                    }
                } else if (MedalRankingType.valueOf(type) == MedalRankingType.ExpertHunter) {
                    str = new DecimalFormat("#,###").format(p.getRight()) + "#k 마리";
                } else {
                    str = new DecimalFormat("#,###").format(p.getRight()) + "#k";
                }
                ret += (rank++) + ". #b" + p.getLeft() + "#k : #r" + str + "\r\n";
            }
        }
        return ret;
    }

    public int checkMedalScore(String type, int score) {
        int z = MedalRanking.canMedalRank(MedalRanking.MedalRankingType.valueOf(type), c.getPlayer().getName(), score);
        if (z >= 0) {
            MedalRanking.addNewMedalRank(MedalRanking.MedalRankingType.valueOf(type), c.getPlayer().getName(), score);
        }
        return z;
    }

    public void removeItemFromWorld(int itemid, String msg, boolean involveSelf) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                if (chr.getId() != c.getPlayer().getId() || involveSelf) {
                    if (chr.haveItem(itemid, 1, true, true)) {
                        if (itemid / 1000000 == 1) {
                            chr.removeAllEquip(itemid, false);
                        } else {
                            chr.removeAll(itemid, true);
                        }
                        if (msg != null && !msg.isEmpty()) {
                            chr.dropMessage(5, msg);
                        }
                    }
                }
            }
        }
    }

public static class BookEntry {
    public int bookcode;
    public int quantity;

    public BookEntry(int bookcode, int quantity) {
        this.bookcode = bookcode;
        this.quantity = quantity;
    }
}

public List<List<Integer>> getMasteryBookList() {
    List<List<Integer>> result = new ArrayList<>();
    try (Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT bookcode, quantity FROM masterybook WHERE quantity > 0 ORDER BY bookcode ASC");
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            List<Integer> entry = new ArrayList<>();
            entry.add(rs.getInt("bookcode"));
            entry.add(rs.getInt("quantity"));
            result.add(entry);
        }
    } catch (SQLException e) {
        System.err.println("Failed to fetch masterybook list: " + e);
    }
    return result;
}

    public boolean isMasteryBook(int itemId) {
        return itemId >= 2290000 && itemId <= 2290284;
    }

    public void sellMasteryBook(int itemId, int quantity) {
        if (!isMasteryBook(itemId)) return;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                "INSERT INTO masterybook (bookcode, quantity) VALUES (?, ?) ON DUPLICATE KEY UPDATE quantity = quantity + ?")) {
            ps.setInt(1, itemId);
            ps.setInt(2, quantity);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to record masterybook sale: " + e);
        }
    }

    public boolean buyMasteryBook(int itemId) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE masterybook SET quantity = quantity - 1 WHERE bookcode = ? AND quantity > 0")) {
            ps.setInt(1, itemId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update masterybook purchase: " + e);
        }
        return false;
    }

    public void changeMusicSelf(String bgm) {
    if (c != null && c.getSession() != null) {
        c.getSession().write(MaplePacketCreator.musicChange(bgm));
    }
}
}