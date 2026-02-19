package client.messages.commands;

import client.*;
import client.anticheat.ReportType;
import client.inventory.*;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.CheaterData;
import handling.world.World;
import java.awt.Point;
import scripting.EventInstanceManager;
import scripting.EventManager;
import server.*;
import server.Timer.EventTimer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.marriage.MarriageManager;
import server.shops.MinervaOwlSearchTop;
import tools.CPUSampler;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.life.MapleNPC;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import server.quest.MapleQuest;
import tools.Pair;

/**
 * @author Emilyx3
 */
public class AdminCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }

    public static class Invincible extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (player.isInvincible()) {
                player.setInvincible(false);
                player.dropMessage(6, "Invincibility deactivated.");
            } else {
                player.setInvincible(true);
                player.dropMessage(6, "Invincibility activated.");
            }
            return 1;
        }
    }

    public static class 테스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleBuffStatValueHolder v = c.getPlayer().getBSVH(MapleBuffStat.BLUE_AURA);
            if (v != null) {
                c.getPlayer().dropMessage(5, "버프 건사람 : " + v.cid);
            }

            return 1;
        }
    }

    public static class 명령어 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            player.dropMessage(6, "!현재좌표, 좌표 : 맵코드 및 현재 위치를 알려줍니다.");
            player.dropMessage(6, "!검색 <엔피시/몬스터/아이템/맵/스킬/퀘스트> <검색어> : 고유코드를 검색");
            player.dropMessage(6, "!아이템 <아이템코드> : 원하는 아이템 생성");
            player.dropMessage(6, "!맵 <대상이름/맵코드> : 대상에게 순간이동 또는 맵 고유코드로 순간이동");
            player.dropMessage(6, "!소환 <대상이름> : 대상을 자신에게 소환");
            player.dropMessage(6, "!cheaters : 현재 핵으로 의심가는 사람들 목록 (숫자는 의심 감지 횟수)");
            player.dropMessage(6, "!ban <대상이름/이메일주소/IP> <밴 사유> : 대상을 해당 밴 사유로 밴.");
            player.dropMessage(6, "!unban <대상이름/이메일주소/IP> : 대상을 밴 해제");
            player.dropMessage(6, "!hellban <대상이름/이메일주소/IP> <밴 사유> : 대상을 해당 밴 사유로 아이피 포함 영구밴.");
            player.dropMessage(6, "!unhellban <대상이름/이메일주소/IP> : 대상을 아이피 포함 영구밴 해제");
            player.dropMessage(6, "!캐릭터정보, 캐릭터정보2 <대상> : 계정, IP, 스탯, 메소 정보");
            player.dropMessage(6, "!드랍삭제, 드롭삭제, removedrops : 현재 맵에 떨어진 아이템 모두 삭제");
            player.dropMessage(6, "!킬올(killall), 킬올드롭(killalldrop), 킬올경험치(killallexp) : 맵에 있는 몬스터 모두 죽입니다.");
            player.dropMessage(6, "!서버타임 : 서버가 켜져있던 시간");
            player.dropMessage(6, "!캐시 <캐릭터> <숫자> : 캐시를 지급합니다. / !캐시 <숫자> : 캐시를 획득합니다.");
            player.dropMessage(6, "!동접, 온라인 : 동시 접속자를 파악하고 인원을 알려줍니다.");
            player.dropMessage(6, "!사냥동접 : 사냥 중인 유저를 파악하고 인원을 알려줍니다.");
            return 1;
        }
    }

    public static class 맵온라인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "현재 이 맵에 있는 유저:");
            StringBuilder builder = new StringBuilder();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(6, builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }

    public static class TempBanIP extends TempBan {

        public TempBanIP() {
            ipBan = true;
        }
    }

    public static class BanIP extends Ban {

        public BanIP() {
            ipBan = true;
        }
    }

    public static class 숨기 extends Hide {
    }

    public static class 하이드 extends Hide {
    }

    public static class Hide extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setHidden(!c.getPlayer().isHidden());
            return 0;
        }
    }

    public static class LowHP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().setHp((int) 1, c.getPlayer());
            c.getPlayer().getStat().setMp((int) 1, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.HP, 1);
            c.getPlayer().updateSingleStat(MapleStat.MP, 1);
            return 0;
        }
    }

    public static class 힐 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().heal(c.getPlayer());
            c.getPlayer().dispelDebuffs();
            return 0;
        }
    }

    public static class 맵힐 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                if (mch != null) {
                    mch.getStat().setHp(mch.getStat().getCurrentMaxHp(), mch);
                    mch.updateSingleStat(MapleStat.HP, mch.getStat().getMaxHp());
                    mch.getStat().setMp(mch.getStat().getCurrentMaxMp(), mch);
                    mch.updateSingleStat(MapleStat.MP, mch.getStat().getMaxMp());
                    mch.dispelDebuffs();
                }
            }
            return 1;
        }
    }

    public static class 스킬부분마스터 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chr = c.getPlayer();
            if (splitted.length > 1) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chra : ch.getPlayerStorage().getAllCharacters()) {
                        if (chra.getName().equals(splitted[1])) {
                            chr = chra;
                        }
                    }
                }
            }
            int Job = chr.getJob();
            int Job1 = Job / 100 * 100;
            int Job2 = Job1 + Job % 100 - Job % 10;
            for (Skill skill : SkillFactory.getAllSkills()) {
                if (!chr.checkMasterSkill(skill.getId())) {
                    continue;
                }
                if (skill.getId() / 10000 == Job1 || (skill.getId() / 10000 >= Job2 && skill.getId() / 10000 <= Job)) {
                    chr.changeSkillLevel(skill, skill.getMaxLevel(), (byte) skill.getMaxLevel());
                }
            }
            if (c.getPlayer().getId() == chr.getId()) {
                c.getPlayer().teachSkill(1004, (byte) Math.max(1, c.getPlayer().getSkillLevel(1004)), (byte) Math.max(1, c.getPlayer().getSkillLevel(1004)));
                c.getPlayer().teachSkill(1005, (byte) Math.max(1, c.getPlayer().getSkillLevel(1005)), (byte) Math.max(1, c.getPlayer().getSkillLevel(1005)));
                c.getPlayer().teachSkill(1007, (byte) 3, (byte) 3);
            }
            return 1;
        }
    }

    public static class 스텟초기화 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().setStr((short) 0, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.STR, c.getPlayer().getStat().getStr());
            c.getPlayer().getStat().setDex((short) 0, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.DEX, c.getPlayer().getStat().getDex());
            c.getPlayer().getStat().setInt((short) 0, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.INT, c.getPlayer().getStat().getInt());
            c.getPlayer().getStat().setLuk((short) 0, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.LUK, c.getPlayer().getStat().getLuk());
            c.getPlayer().setRemainingAp((short) 30000);
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
            return 0;
        }
    }

    public static class 스킬코드 extends AdminCommand.스킬이름 {
    }

    public static class 스킬명 extends AdminCommand.스킬이름 {
    }

    public static class 스킬이름 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int skill = Integer.parseInt(splitted[1]);
                if (SkillFactory.getSkillName(skill).equals("")) {
                    c.getPlayer().dropMessage(6, "검색한 코드를 가진 스킬은 없습니다.");
                } else {
                    c.getPlayer().dropMessage(6, "스킬 이름 : " + SkillFactory.getSkillName(skill) + " / 검색한 코드 (" + skill + ")");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "존재하지 않는 코드입니다.");
            }
            return 0;
        }
    }

    public static class 추가데미지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().setExtraDamage(0);
                c.getPlayer().dropMessage(5, "추가데미지를 제거하였습니다.");
            } else {
                c.getPlayer().setExtraDamage(Integer.parseInt(splitted[1]));
                c.getPlayer().dropMessage(6, Integer.parseInt(splitted[1]) + " 만큼 추가 데미지를 상승하였습니다.");
            }
            return 0;
        }
    }

    public static class 자살 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().updateSingleStat(MapleStat.HP, 0);
            c.getPlayer().getStat().setHp((short) 0, c.getPlayer());
            return 0;
        }
    }

    public static class 체력낮추기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().updateSingleStat(MapleStat.HP, 1);
                c.getPlayer().getStat().setHp((short) 1, c.getPlayer());
            } else {
                c.getPlayer().updateSingleStat(MapleStat.HP, c.getPlayer().getStat().getHp());
                c.getPlayer().getStat().setHp((short) Integer.parseInt(splitted[1]), c.getPlayer());
            }
            return 0;
        }
    }

    public static class 마나낮추기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().updateSingleStat(MapleStat.MP, 1);
            c.getPlayer().getStat().setMp((short) 1, c.getPlayer());
            return 0;
        }
    }

    public static class 모두낮추기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().updateSingleStat(MapleStat.HP, 1);
            c.getPlayer().updateSingleStat(MapleStat.MP, 1);
            c.getPlayer().getStat().setHp((short) 1, c.getPlayer());
            c.getPlayer().getStat().setMp((short) 1, c.getPlayer());
            return 0;
        }
    }

    public static class 회복 extends AdminCommand.체력회복 {
    }

    public static class 체력회복 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().heal(c.getPlayer());
            c.getPlayer().dispelDebuffs();
            return 0;
        }
    }

    public static class 체력 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "체력 : " + c.getPlayer().getStat().getHp() + " / 마나 : " + c.getPlayer().getStat().getMp());
            return 0;
        }
    }

    public static class 맥스체력 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "사용법 : !맥스체력 <수치>");
                return 1;
            }
            try {
                int value = Integer.parseInt(splitted[1]);
                c.getPlayer().getStat().setMaxHp(Math.min(Math.max(1, value), 30000), c.getPlayer());
                c.getPlayer().updateSingleStat(MapleStat.MAXHP, c.getPlayer().getStat().getMaxHp());
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법 : !맥스체력 <수치>");
            }
            return 0;
        }
    }

    public static class 맥스마나 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "사용법 : !맥스마나 <수치>");
                return 1;
            }
            try {
                int value = Integer.parseInt(splitted[1]);
                c.getPlayer().getStat().setMaxMp(Math.min(Math.max(1, value), 30000), c.getPlayer());
                c.getPlayer().updateSingleStat(MapleStat.MAXMP, c.getPlayer().getStat().getMaxMp());
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법 : !맥스마나 <수치>");
            }
            return 0;
        }
    }

    public static class TempB extends TempBan {
    }

    public static class TempBan extends CommandExecute {

        protected boolean ipBan = false;
        private String[] types = {"핵 사용", "매크로 사용", "광고", "욕설 / 비난 / 비방", "도배", "GM 괴롭힘 / 욕", "공개 욕설/비난/비방", "현금거래", "임시 정지 처분", "사칭", "관리자 사칭", "불법 / 비인가 프로그램 사용 (감지)", "계정 도용"};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(6, "Tempban [name] [REASON] [days]");
                StringBuilder s = new StringBuilder("Tempban reasons: ");
                for (int i = 0; i < types.length; i++) {
                    s.append(i).append(" - ").append(types[i]).append(", ");
                }
                c.getPlayer().dropMessage(6, s.toString());
                return 0;
            }
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            final int reason = Integer.parseInt(splitted[2]);
            final int numDay = Integer.parseInt(splitted[3]);

            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, numDay);
            final DateFormat df = DateFormat.getInstance();

            if (reason < 0 || reason >= types.length) {
                c.getPlayer().dropMessage(6, "Unable to find character or reason was not valid, type tempban to see reasons");
                return 0;
            }
            if (victim == null) {
                boolean res = MapleCharacter.tempban(types[reason], cal, reason, c.getPlayer().getName(), splitted[1]);
                if (!res) {
                    c.getPlayer().dropMessage(6, "Unable to find character or reason was not valid, type tempban to see reasons");
                    return 0;
                }
                c.getPlayer().dropMessage(6, "The character " + splitted[1] + " has been successfully offline tempbanned till " + df.format(cal.getTime()));
                return 1;
            }
            victim.tempban(types[reason], cal, reason, ipBan, c.getPlayer().getName());
            c.getPlayer().dropMessage(6, "The character " + splitted[1] + " has been successfully tempbanned till " + df.format(cal.getTime()));
            return 1;
        }
    }

    public static class ChatBlock extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Chatblock [name] [days]");
                return 0;
            }
            final int numDay = Integer.parseInt(splitted[2]);

            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, numDay);
            final DateFormat df = DateFormat.getInstance();
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE accounts SET `chatblocktime` = ? WHERE id = ?");
                ps.setTimestamp(1, new java.sql.Timestamp(cal.getTimeInMillis()));
                ps.setInt(2, MapleCharacterUtil.getAccIdByName(splitted[1]));
                ps.executeUpdate();
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "Error : " + e);
                return 0;
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
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim != null) {
                    victim.dropMessage(1, "대화가 금지되었습니다.");
                    victim.canTalk(false);
                }
            }
            c.getPlayer().dropMessage(6, "The character " + splitted[1] + " has been successfully offline tempbanned till " + df.format(cal.getTime()));
            return 1;
        }
    }

    public static class HellB extends HellBan {
    }

    public static class HellBan extends Ban {

        public HellBan() {
            hellban = true;
        }
    }

    public static class UnHellB extends UnHellBan {
    }

    public static class UnHellBan extends UnBan {

        public UnHellBan() {
            hellban = true;
        }
    }

    public static class 소환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if ((!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
            } else {
                int ch = World.Find.findChannel(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                c.getPlayer().dropMessage(5, "대상이 채널 이동 중입니다.");
                victim.dropMessage(5, "채널 이동 중입니다.");
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                    victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                }
                victim.changeChannel(c.getChannel());
            }
            return 1;
        }
    }

    public static class UnB extends UnBan {
    }

    public static class UnBan extends CommandExecute {

        protected boolean hellban = false;

        private String getCommand() {
            if (hellban) {
                return "UnHellBan";
            } else {
                return "UnBan";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !" + getCommand() + " <IGN>");
                return 0;
            }
            byte ret;
            if (hellban) {
                ret = MapleClient.unHellban(splitted[1]);
            } else {
                ret = MapleClient.unban(splitted[1]);
            }
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] SQL error.");
                return 0;
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] The character does not exist.");
                return 0;
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully unbanned!");

            }
            byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            return ret_ > 0 ? 1 : 0;
        }
    }

    public static class UnbanIP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !unbanip <IGN>");
                return 0;
            }
            byte ret = MapleClient.unbanIPMacs(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            if (ret > 0) {
                return 1;
            }
            return 0;
        }
    }

    public static class Ban extends CommandExecute {

        protected boolean hellban = false, ipBan = false;

        private String getCommand() {
            if (hellban) {
                return "HellBan";
            } else {
                return "Ban";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(5, "[Syntax] !" + getCommand() + " <IGN> <Reason>");
                return 0;
            }
            if (StringUtil.joinStringFrom(splitted, 2).length() < 10) {
                c.getPlayer().dropMessage(5, "밴 사유가 너무 짧습니다. 상세하게 적어주세요.");
                return 0;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("밴 캐릭터 : " + splitted[1]).append("\r\n사유 : ").append(StringUtil.joinStringFrom(splitted, 2));
            MapleCharacter target = World.getCharacterByName(splitted[1]);
            if (target != null) {
                if (c.getPlayer().getGMLevel() > target.getGMLevel() || c.getPlayer().isAdmin()) {
                    sb.append(" (IP: ").append(target.getClient().getSessionIPAddress()).append(")");
                    if (target.ban(sb.toString(), hellban || ipBan, false, hellban, c.getPlayer().getName())) {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully banned " + splitted[1] + ".");
                        return 1;
                    } else {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] Failed to ban.");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] May not ban GMs...");
                    return 1;
                }
            } else {
                if (MapleCharacter.ban(splitted[1], sb.toString(), false, c.getPlayer().isAdmin() ? 250 : c.getPlayer().getGMLevel(), hellban, c.getPlayer().getName())) {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully offline banned " + splitted[1] + ".");
                    return 1;
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] Failed to ban " + splitted[1]);
                    return 0;
                }
            }
        }
    }

    public static class CC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().changeChannel(Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class CCPlayer extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().changeChannel(World.Find.findChannel(splitted[1]));
            return 1;
        }
    }

    public static class Kill extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: !kill <list player names>");
                return 0;
            }
            MapleCharacter victim = null;
            for (int i = 1; i < splitted.length; i++) {
                try {
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Player " + splitted[i] + " not found.");
                }
                if (player.allowedToTarget(victim) && player.getGMLevel() >= victim.getGMLevel()) {
                    victim.getStat().setHp((int) 0, victim);
                    victim.getStat().setMp((int) 0, victim);
                    victim.updateSingleStat(MapleStat.HP, 0);
                    victim.updateSingleStat(MapleStat.MP, 0);
                }
            }
            return 1;
        }
    }

    public static class 맵코드 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "현재 맵 코드: " + c.getPlayer().getMap().getId());
            return 1;
        }
    }

    public static class ClearInv extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Pair<Short, Short>, MapleInventoryType> eqs = new HashMap<Pair<Short, Short>, MapleInventoryType>();
            if (splitted[1].equals("all")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), type);
                    }
                }
            } else if (splitted[1].equals("eqp")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIPPED);
                }
            } else if (splitted[1].equals("eq")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIP);
                }
            } else if (splitted[1].equals("u")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.USE);
                }
            } else if (splitted[1].equals("s")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.SETUP);
                }
            } else if (splitted[1].equals("e")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.ETC);
                }
            } else if (splitted[1].equals("c")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.CASH);
                }
            } else {
                c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
            }
            for (Map.Entry<Pair<Short, Short>, MapleInventoryType> eq : eqs.entrySet()) {
                MapleInventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
            }
            return 1;
        }
    }

    public static class 온라인2 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "CH." + c.getChannel() + "에 접속 중인 캐릭터:");
            c.getPlayer().dropMessage(6, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
            return 1;
        }
    }

    public static class 온라인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder text = new StringBuilder();
            int count = 0, value = 0;
            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                if (i < 2) {
                    text.append(i + "채널");
                } else if (i == 2) {
                    text.append("20세이상");
                } else {
                    text.append((i - 1) + "채널");
                }
                for (MapleCharacter chr : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (!chr.isGM()) {
                        count++;
                        value++;
                        if (value == 1) {
                            text.append(" : " + chr.getName());
                        } else {
                            text.append(", " + chr.getName());
                        }
                    }
                }
                c.getPlayer().dropMessage(6, text.toString());
                text.setLength(0);
                value = 0;
            }
            c.getPlayer().dropMessage(6, "총 접속자 : " + count + "명");
            return 1;
        }
    }

    public static class 사냥동접 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder text = new StringBuilder();
            int count = 0, value = 0;
            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                if (i < 2) {
                    text.append(i + "채널");
                } else if (i == 2) {
                    text.append("20세이상");
                } else {
                    text.append((i - 1) + "채널");
                }
                for (MapleCharacter chr : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (!chr.isGM() && chr.getMap().getAllMonster().size() > 0) {
                        count++;
                        value++;
                        if (value == 1) {
                            text.append(" : " + chr.getName());
                        } else {
                            text.append(", " + chr.getName());
                        }
                    }
                }
                c.getPlayer().dropMessage(6, text.toString());
                text.setLength(0);
                value = 0;
            }
            c.getPlayer().dropMessage(6, "사냥 중인 유저 : " + count + "명");
            return 1;
        }
    }

    public static class 채널온라인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder conStr = new StringBuilder();
            conStr.append("CH.");
            if (Integer.parseInt(splitted[1]) == 1) {
                conStr.append("1");
            } else if (Integer.parseInt(splitted[1]) == 2) {
                conStr.append("20세이상");
            } else {
                conStr.append(Integer.parseInt(splitted[1]) - 1);
            }
            conStr.append("에 접속 중인 캐릭터:");
            c.getPlayer().dropMessage(6, conStr.toString());
            c.getPlayer().dropMessage(6, ChannelServer.getInstance(Integer.parseInt(splitted[1])).getPlayerStorage().getOnlinePlayers(true));
            return 1;
        }
    }

    public static class ItemCheck extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                c.getPlayer().dropMessage(6, "!itemcheck <playername> <itemid>");
                return 0;
            } else {
                int item = Integer.parseInt(splitted[2]);
                MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                int itemamount = chr.getItemQuantity(item, true);
                if (itemamount > 0) {
                    c.getPlayer().dropMessage(6, chr.getName() + " has " + itemamount + " (" + item + ").");
                } else {
                    c.getPlayer().dropMessage(6, chr.getName() + " doesn't have (" + item + ")");
                }
            }
            return 1;
        }
    }

    public static class Song extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String splitString = StringUtil.joinStringFrom(splitted, 1);
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(splitString));
            return 1;
        }
    }

    public static class CheckPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Need playername.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                c.getPlayer().dropMessage(6, chrs.getName() + " has " + chrs.getPoints() + " points.");
            }
            return 1;
        }
    }

    public static class CheckVPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Need playername.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                c.getPlayer().dropMessage(6, chrs.getName() + " has " + chrs.getVPoints() + " vpoints.");
            }
            return 1;
        }
    }

    public static class 드롭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "펫은 !아이템 명령어를 이용해 주시기 바랍니다.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 번 아이템은 존재하지 않습니다.");
            } else {
                Item toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {

                    toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                }
                toDrop.setGMLog(c.getPlayer().getName() + "!드롭 명령어로 얻은 아이템.");
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, false); // 드롭 명령어는 플레이어 드랍x
            }
            return 1;
        }
    }

    public static class PermWeather extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getPermanentWeather() > 0) {
                c.getPlayer().getMap().setPermanentWeather(0);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeMapEffect());
                c.getPlayer().dropMessage(5, "Map weather has been disabled.");
            } else {
                final int weather = CommandProcessorUtil.getOptionalIntArg(splitted, 1, 5120000);
                if (!MapleItemInformationProvider.getInstance().itemExists(weather) || weather / 10000 != 512) {
                    c.getPlayer().dropMessage(5, "Invalid ID.");
                } else {
                    c.getPlayer().getMap().setPermanentWeather(weather);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.startMapEffect("", weather, false));
                    c.getPlayer().dropMessage(5, "Map weather has been enabled.");
                }
            }
            return 1;
        }
    }

    public static class 스공 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean isOnline = false;
            MapleCharacter player = c.getPlayer();
            final StringBuilder builder = new StringBuilder();

            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                for (MapleCharacter other : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (splitted.length < 2) {
                        builder.append("==================================================================");
                        c.getPlayer().dropMessage(6, builder.toString());
                        builder.delete(0, 9999999);
                        builder.append("크리티컬 확률: " + (int) player.getStat().getSharpEyeRate() + " 크뎀: " + (int) player.getStat().getSharpEyeDam());
                        c.getPlayer().dropMessage(6, builder.toString());
                        builder.delete(0, 9999999);
                        builder.append("스공: " + (int) player.getStat().getCurrentMinBaseDamage() + " ~ " + (int) player.getStat().getCurrentMaxBaseDamage());
                        c.getPlayer().dropMessage(6, builder.toString());
                        builder.delete(0, 9999999);
                        builder.append("마력: " + (int) player.getStat().getTotalMagic());
                        c.getPlayer().dropMessage(6, builder.toString());
                        builder.delete(0, 9999999);
                        builder.append("명중률: " + (int) player.getStat().getAccuracy());
                        c.getPlayer().dropMessage(6, builder.toString());
                        builder.delete(0, 9999999);
                        return 0;
                    } else if (other != null && other.getName().equals(splitted[1])) {
                        other.saveToDB(false, false); //세이브
                        isOnline = true;
                        player = other;
                    }
                }
            }

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM characters c INNER JOIN accounts a ON c.accountid = a.id WHERE c.name = ?");
                ps.setString(1, splitted[1]);
                rs = ps.executeQuery();
                if (rs.next()) {
                    if (isOnline) {
                        builder.append("크리티컬 확률: " + (int) player.getStat().getSharpEyeRate() + " 크뎀: " + (int) player.getStat().getSharpEyeDam());
                        builder.append(" 스공: " + (int) player.getStat().getCurrentMinBaseDamage() + " ~ " + (int) player.getStat().getCurrentMaxBaseDamage());
                        c.getPlayer().dropMessage(6, builder.toString());
                    } else {
                        c.getPlayer().dropMessage(6, "[시스템] 해당 유저가 접속중이지 않은것 같습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "[시스템] " + splitted[1] + " 닉네임을 가진 유저가 존재하지 않습니다.");
                    return 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (Exception e) {

                }
            }
            return 1;
        }
    }

    public static class 캐릭터정보 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean isOnline = false;
            MapleCharacter player = null;

            for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                for (MapleCharacter other : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (other != null && other.getName().equals(splitted[1])) {
                        other.saveToDB(false, false); //세이브
                        isOnline = true;
                        player = other;
                    }
                }
            }

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            StringBuilder text = new StringBuilder().append("#b" + splitted[1] + " #k님의 캐릭터 정보입니다.\r\n\r\n");

            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM characters c INNER JOIN accounts a ON c.accountid = a.id WHERE c.name = ?");
                ps.setString(1, splitted[1]);
                rs = ps.executeQuery();

                if (rs.next()) {
                    text.append("#b캐릭터 ID : #k" + rs.getInt("c.id") + " #b어카운트 ID : #k" + rs.getInt("a.id") + "\r\n\r\n");
                    text.append("#e캐릭터 스탯#n\r\n#b힘 : #k" + rs.getInt("c.str") + " #b덱 : #k" + rs.getInt("c.dex") + " #b인 : #k" + rs.getInt("c.int") + " #b럭 : #k" + rs.getInt("c.luk") + "\r\n");
                    text.append("#b최대 HP : #k" + rs.getInt("c.maxhp") + " #b최대 MP : #k" + rs.getInt("c.maxmp") + "\r\n");
                    text.append("#b현재 HP : #k" + rs.getInt("c.hp") + " #b현재 MP : #k" + rs.getInt("c.mp") + "\r\n\r\n");
                    if (isOnline) {
                        text.append("#b토탈 공격력 : #k" + player.getStat().getTotalWatk() + " #b토탈 마력 : #k" + player.getStat().getTotalMagic() + "\r\n");
                        text.append("#b토탈 힘 : #k" + player.getStat().getTotalStr() + " #b토탈 덱 : #k" + player.getStat().getTotalDex() + "\r\n");
                        text.append("#b토탈 인 : #k" + player.getStat().getTotalInt() + " #b토탈 럭 : #k" + player.getStat().getTotalLuk() + "\r\n\r\n");
                        text.append("#b스공 : #k" + (int) player.getStat().getCurrentMinBaseDamage() + "~" + (int) player.getStat().getCurrentMaxBaseDamage() + "\r\n\r\n");
                    }
                    text.append("#b직업 : #k" + c.getPlayer().getJobName(rs.getInt("c.job")) + " #d #b직업코드 : #k" + rs.getInt("c.job") + "\r\n");
                    text.append("#b레벨 : #k" + rs.getInt("c.level") + " #b경험치 : #k" + rs.getInt("c.exp") + "\r\n");
                    text.append("#b헤어 : #k" + rs.getInt("c.hair") + " #b성형 : #k" + rs.getInt("c.face") + "\r\n\r\n");
                    text.append("#b소지 중인 A캐시 : #k" + rs.getInt("DonateCash") + "\r\n");
                    text.append("#b소지 중인 캐시 : #k" + rs.getInt("ACash") + "\r\n");

                    text.append("#b소지 중인 메소 : #k" + c.getPlayer().getBanJum((long) rs.getInt("c.meso")) + "\r\n\r\n");

                    text.append("#b현재 맵 : #k" + c.getChannelServer().getMapFactory().getMap(rs.getInt("c.map")).getStreetName() + "-" + c.getChannelServer().getMapFactory().getMap(rs.getInt("c.map")).getMapName() + " (" + rs.getInt("c.map") + ")\r\n");

                    if (isOnline) {
                        text.append("#b캐릭터 좌표 : #k (#dX : " + player.getPosition().getX() + " Y : " + player.getPosition().getY() + "#k)\r\n");
                    }

                    if (rs.getInt("c.gm") > 0) {
                        text.append("#bGM : #k" + "권한 있음 (" + rs.getInt("c.gm") + " 레벨)\r\n");
                    } else {
                        text.append("#bGM : #k권한 없음\r\n");
                    }

                    String guild = "";
                    if (rs.getInt("c.guildid") == 0) {
                        guild = "없음";
                    } else {
                        guild = World.Guild.getGuild(rs.getInt("c.guildid")).getName();
                    }
                    text.append("#b소속된 길드 : #k" + guild + "\r\n\r\n");

                    String connect = "";
                    if (rs.getInt("a.loggedin") == 0) {
                        connect = "#r오프라인";
                    } else {
                        connect = "#g온라인";
                    }
                    text.append("#b접속 현황 : #k" + connect + "\r\n");
                    text.append("#b계정 아이디 : #k" + rs.getString("a.name") + "\r\n");
                    //text.append("#b계정 비밀번호 : #k" + rs.getString("a.password") + "\r\n");//비밀번호는 의미가 없으니.. 주석
                    text.append("#b아이피 : #k" + rs.getString("SessionIP") + "\r\n\r\n");
                    text.append("#b마지막 접속 : #k" + rs.getString("lastlogin") + "\r\n");
                    text.append("#b아이디 생성 날짜 : #k" + rs.getString("createdat") + "\r\n");

                } else {
                    c.getPlayer().dropMessage(5, "[시스템] " + splitted[1] + " 닉네임을 가진 유저가 존재하지 않습니다.");
                    return 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (Exception e) {

                }
            }
            c.getSession().write(MaplePacketCreator.getNPCTalk(9000019, (byte) 0, text.toString(), "00 00", (byte) 0));
            return 1;
        }
    }

    public static class 캐릭터정보2 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final StringBuilder builder = new StringBuilder();
            final MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (other == null) {
                builder.append("존재하지 않는 캐릭터입니다.");
                c.getPlayer().dropMessage(6, builder.toString());
                return 0;
            }
            if (other.getClient().getLastPing() <= 0) {
                other.getClient().sendPing();
            }
            if (other.getGMLevel() > c.getPlayer().getGMLevel()) {
                c.getPlayer().dropMessage(6, "이 캐릭터의 정보를 볼 수 없습니다.");
                return 0;
            }
            builder.append(MapleClient.getLogMessage(other, ""));
            builder.append(" at ").append(other.getPosition().x);
            builder.append("/").append(other.getPosition().y);

            builder.append(" || HP : ");
            builder.append(other.getStat().getHp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxHp());

            builder.append(" || MP : ");
            builder.append(other.getStat().getMp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxMp());

            builder.append(" || 물리공격력 : ");
            builder.append(other.getStat().getTotalWatk());
            builder.append(" || 마법공격력 : ");
            builder.append(other.getStat().getTotalMagic());
            builder.append(" || 스탯공격력 : ");
            builder.append(other.getStat().getCurrentMinBaseDamage());
            builder.append("~");
            builder.append(other.getStat().getCurrentMaxBaseDamage());
//            builder.append(" || DAMAGE% : ");
//            builder.append(other.getStat().dam_r);
//            builder.append(" || BOSSDAMAGE% : ");
//            builder.append(other.getStat().bossdam_r);
//            builder.append(" || CRIT CHANCE : ");
//            builder.append(other.getStat().passive_sharpeye_rate());
//            builder.append(" || CRIT DAMAGE : ");
//            builder.append(other.getStat().passive_sharpeye_percent());
//
            builder.append(" || STR : ");
            builder.append(other.getStat().getStr());
            builder.append(" || DEX : ");
            builder.append(other.getStat().getDex());
            builder.append(" || INT : ");
            builder.append(other.getStat().getInt());
            builder.append(" || LUK : ");
            builder.append(other.getStat().getLuk());

            builder.append(" || 총합 STR : ");
            builder.append(other.getStat().getTotalStr());
            builder.append(" || 총합 DEX : ");
            builder.append(other.getStat().getTotalDex());
            builder.append(" || 총합 INT : ");
            builder.append(other.getStat().getTotalInt());
            builder.append(" || 총합 LUK : ");
            builder.append(other.getStat().getTotalLuk());

            builder.append(" || EXP : ");
            builder.append(other.getExp());
            builder.append(" || 메소 : ");
            builder.append(other.getMeso());

            builder.append(" || party : ");
            builder.append(other.getParty() == null ? -1 : other.getParty().getId());

            builder.append(" || hasTrade : ");
            builder.append(other.getTrade() != null);
            builder.append(" || 딜레이 : ");
            builder.append(other.getClient().getLatency());
            builder.append(" || PING : ");
            builder.append(other.getClient().getLastPing());
            builder.append(" || PONG : ");
            builder.append(other.getClient().getLastPong());
            builder.append(" || 접속한 IP 주소 : ");

            other.getClient().DebugMessage(builder);

            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }

    public static class Reports extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<CheaterData> cheaters = World.getReports();
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(6, cheater.getInfo());
            }
            return 1;
        }
    }

    public static class ClearReport extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                StringBuilder ret = new StringBuilder("report [ign] [all/");
                for (ReportType type : ReportType.values()) {
                    ret.append(type.theId).append('/');
                }
                ret.setLength(ret.length() - 1);
                c.getPlayer().dropMessage(6, ret.append(']').toString());
                return 0;
            }
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "Does not exist");
                return 0;
            }
            final ReportType type = ReportType.getByString(splitted[2]);
            if (type != null) {
                victim.clearReports(type);
            } else {
                victim.clearReports();
            }
            c.getPlayer().dropMessage(5, "Done.");
            return 1;
        }
    }

    public static class Cheaters extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<CheaterData> cheaters = World.getCheaters();
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(6, cheater.getInfo());
            }
            return 1;
        }
    }

    public static class 근처포탈 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MaplePortal portal = c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition());
            c.getPlayer().dropMessage(6, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());

            return 1;
        }
    }

    public static class 맵디버그 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, c.getPlayer().getMap().spawnDebug());
            return 1;
        }
    }

    public static class 갱신 extends AdminCommand.FakeRelog {
    }

    public static class FakeRelog extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().fakeRelog();
            return 1;
        }
    }

    public static class 드롭삭제 extends AdminCommand.RemoveDrops {
    }

    public static class 드랍삭제 extends AdminCommand.RemoveDrops {
    }

    public static class RemoveDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "아이템 " + c.getPlayer().getMap().getNumItems() + "개를 제거했습니다.");
            c.getPlayer().getMap().removeDrops();
            return 1;
        }
    }

    public static class ListSquads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (Map.Entry<MapleSquad.MapleSquadType, MapleSquad> squads : c.getChannelServer().getAllSquads().entrySet()) {
                c.getPlayer().dropMessage(5, "TYPE: " + squads.getKey().name() + ", Leader: " + squads.getValue().getLeader().getName() + ", status: " + squads.getValue().getStatus() + ", numMembers: " + squads.getValue().getSquadSize() + ", numBanned: " + squads.getValue().getBannedMemberSize());
            }
            return 0;
        }
    }

    public static class ListInstances extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(StringUtil.joinStringFrom(splitted, 1));
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                for (EventInstanceManager eim : em.getInstances()) {
                    c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", charSize: " + eim.getPlayers().size() + ", dcedSize: " + eim.getDisconnected().size() + ", mobSize: " + eim.getMobs().size() + ", eventManager: " + em.getName() + ", timeLeft: " + eim.getTimeLeft() + ", iprops: " + eim.getProperties().toString() + ", eprops: " + em.getProperties().toString());
                }
            }
            return 0;
        }
    }

    public static class 서버타임 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "서버가 리붓 된지" + StringUtil.getReadableMillis(ChannelServer.serverStartTime, System.currentTimeMillis()) + "가 경과되었습니다.");
            return 1;
        }
    }

    public static class EventInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() == null) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                EventInstanceManager eim = c.getPlayer().getEventInstance();
                c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", charSize: " + eim.getPlayers().size() + ", dcedSize: " + eim.getDisconnected().size() + ", mobSize: " + eim.getMobs().size() + ", eventManager: " + eim.getEventManager().getName() + ", timeLeft: " + eim.getTimeLeft() + ", iprops: " + eim.getProperties().toString() + ", eprops: " + eim.getEventManager().getProperties().toString());
            }
            return 1;
        }
    }

    public static class 이동 extends CommandExecute {

        private static final HashMap<String, Integer> gotomaps = new HashMap<String, Integer>();

        static {
            //KMS
            gotomaps.put("사우스페리", 60000);
            gotomaps.put("암허스트", 1010000);
            gotomaps.put("헤네시스", 100000000);
            gotomaps.put("엘리니아", 101000000);
            gotomaps.put("페리온", 102000000);
            gotomaps.put("커닝시티", 103000000);
            gotomaps.put("리스항구", 104000000);
            gotomaps.put("슬리피우드", 105040300);
            gotomaps.put("플로리나비치", 110000000);
            gotomaps.put("노틸러스선착장", 120000000);
            gotomaps.put("에레브", 130000000);
            gotomaps.put("리엔", 140000000);
            gotomaps.put("오르비스", 200000000);
            gotomaps.put("엘나스", 211000000);
            gotomaps.put("루디브리엄", 220000000);
            gotomaps.put("지구방위본부", 221000000);
            gotomaps.put("아랫마을", 222000000);
            gotomaps.put("아쿠아리움", 230000000);
            gotomaps.put("리프레", 240000000);
            gotomaps.put("무릉", 250000000);
            gotomaps.put("백초마을", 251000000);
            gotomaps.put("아리안트", 260000000);
            gotomaps.put("마가티아", 261000000);
            gotomaps.put("시간의신전", 270000000);
            gotomaps.put("엘린숲", 300000000);
            gotomaps.put("황금사원", 950100000);

            //해외 MS
            //gotomaps.put("싱가포르", 540000000);
            //gotomaps.put("보트키타운", 541000000);
            gotomaps.put("플로팅마켓", 500000000);
            //gotomaps.put("말레이시아", 550000000);
            //gotomaps.put("캄풍마을", 551000000);
            //gotomaps.put("뉴리프시티", 600000000);
            //gotomaps.put("웨딩빌리지", 680000000);
            gotomaps.put("상해와이탄", 701000000);
            gotomaps.put("서문정", 740000000);
            gotomaps.put("야시장", 741000000);
            gotomaps.put("버섯신사", 800000000);
            //gotomaps.put("쇼와마을", 801000000);

            //보스
            gotomaps.put("피아누스", 230040420);
            gotomaps.put("파풀라투스", 220080001);
            gotomaps.put("그리프", 240020101);
            gotomaps.put("마뇽", 240020401);
            gotomaps.put("혼테일", 240060200);
            gotomaps.put("카오스혼테일", 240060201);
            gotomaps.put("핑크빈", 270050100);
            gotomaps.put("자쿰", 280030000);
            gotomaps.put("카오스자쿰", 280030001);

            //특수 맵
            gotomaps.put("OX퀴즈", 109020001);
            gotomaps.put("올라올라", 109030001);
            gotomaps.put("고지를향해서", 109040000);
            gotomaps.put("눈덩이굴리기", 109060000);
            gotomaps.put("운영자맵", 180000000);
            gotomaps.put("커닝시티게임방", 193000000);
            gotomaps.put("행복한마을", 209000000);
            gotomaps.put("코크타운", 219000000);
            //gotomaps.put("크림슨우드파퀘", 610030000);
            gotomaps.put("자유시장", 910000000);
            gotomaps.put("길드대항전", 990000000);

            // ㅋㅌㅂㅋ 거
            gotomaps.put("헤네시스", 100000000);
            gotomaps.put("헤파", 100000200);
            gotomaps.put("헤네파퀘", 100000200);
            gotomaps.put("월묘", 100000200);
            gotomaps.put("월묘파퀘", 100000200);
            gotomaps.put("엘리니아", 101000000);
            gotomaps.put("페리온", 102000000);
            gotomaps.put("커닝시티", 103000000);
            gotomaps.put("커파", 103000000);
            gotomaps.put("커닝파퀘", 103000000);
            gotomaps.put("커닝스퀘어", 103040000);
            gotomaps.put("리스항구", 104000000);
            gotomaps.put("슬피", 105040300);
            gotomaps.put("슬리피우드", 105040300);
            gotomaps.put("버섯의성", 106020000);
            gotomaps.put("플로리나비치", 110000000);
            gotomaps.put("마발", 105100100);
            gotomaps.put("마왕발록", 105100100);
            gotomaps.put("발록", 105100100);
            gotomaps.put("파풀", 220080000);
            gotomaps.put("자쿰", 211042400);
            gotomaps.put("혼테일", 240050400);
            gotomaps.put("핑크빈", 270050000);
            gotomaps.put("에레브", 130000000);
            gotomaps.put("리엔", 140000000);
            gotomaps.put("노틸러스", 120000000);
            gotomaps.put("오르비스", 200000000);
            gotomaps.put("행복한마을", 209000000);
            gotomaps.put("웨딩", 680000000);
            gotomaps.put("웨딩빌리지", 680000000);
            gotomaps.put("엘나스", 211000000);
            gotomaps.put("루디", 220000000);
            gotomaps.put("루디브리엄", 220000000);
            gotomaps.put("테마파크", 223000000);
            gotomaps.put("지구방위본부", 221000000);
            gotomaps.put("사자왕", 211060000);
            gotomaps.put("아랫마을", 222000000);
            gotomaps.put("아쿠아리움", 230000000);
            gotomaps.put("리프레", 240000000);
            gotomaps.put("무릉", 250000000);
            gotomaps.put("백초마을", 251000000);
            gotomaps.put("황사", 950100000);
            gotomaps.put("황금사원", 950100000);
            gotomaps.put("황금사원2", 252000000);
            gotomaps.put("황사2", 252000000);
            gotomaps.put("아리안트", 260000000);
            gotomaps.put("마가티아", 261000000);
            gotomaps.put("시간의신전", 270000000);
            gotomaps.put("헌티드맨션", 229000000);
            gotomaps.put("크리세", 200100000);
            gotomaps.put("코크타운", 219000000);
            gotomaps.put("엘린숲", 300000000);
            gotomaps.put("태국", 500000000);
            gotomaps.put("대만", 740000000);
            gotomaps.put("중국", 701000000);
            gotomaps.put("일본", 800000000);
            gotomaps.put("자시", 910000000);
            gotomaps.put("자유시장", 910000000);
            gotomaps.put("루파", 221024500);
            gotomaps.put("루디파퀘", 221024500);
            gotomaps.put("루디브리엄파퀘", 221024500);
            gotomaps.put("오르비스파퀘", 200080101);
            gotomaps.put("올비파퀘", 200080101);
            gotomaps.put("데비존", 251010404);
            gotomaps.put("데비존파퀘", 251010404);
            gotomaps.put("무릉파퀘", 251010404);
            gotomaps.put("엘린숲파퀘", 300030100);
            gotomaps.put("네트", 926010000);
            gotomaps.put("피라미드", 926010000);
            gotomaps.put("지하철", 910320000);
            gotomaps.put("임차장", 910320000);
            gotomaps.put("테라숲", 240070000);
            gotomaps.put("카니발", 980000000);
            gotomaps.put("카니발2", 980030000);
            gotomaps.put("싱가포르", 540000000);
            gotomaps.put("말레이시아", 550000000);
            gotomaps.put("안개바다", 923020000);
            gotomaps.put("드래곤라이더", 240080000);
            gotomaps.put("드라", 240080000);
            gotomaps.put("장로의관저", 211000001);
            gotomaps.put("관저", 211000001);
            gotomaps.put("샤모스", 211000001);
            gotomaps.put("차우", 300010410);
            gotomaps.put("에피네아", 300030300);
            gotomaps.put("투기장", 980010000);
            gotomaps.put("무릉도장", 925020001);
            gotomaps.put("크락셀", 541020700);
            gotomaps.put("타르가", 551030100);
            gotomaps.put("스칼리온", 551030100);
            gotomaps.put("라타니카", 541010060);
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법 : !이동 <맵 이름>");
            } else {
                if (gotomaps.containsKey(splitted[1])) {
                    MapleMap target = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                        return 0;
                    }
                    MaplePortal targetPortal = target.getPortal(0);
                    c.getPlayer().changeMap(target, targetPortal);
                } else {
                    if (splitted[1].equals("맵목록")) {
                        c.getPlayer().dropMessage(6, "!이동 <맵 이름>을 사용해 주세요. 지원되는 맵은 다음과 같습니다.");
                        StringBuilder sb = new StringBuilder();
                        for (String s : gotomaps.keySet()) {
                            sb.append(s).append(", ");
                        }
                        c.getPlayer().dropMessage(6, sb.substring(0, sb.length() - 2));
                    } else {
                        c.getPlayer().dropMessage(6, "입력 형식이 잘못되었습니다. !이동 <맵 이름>을 사용해 주세요. 지원되는 맵을 확인하시려면 '!이동 맵목록'을 사용해 주세요.");
                    }
                }
            }
            return 1;
        }
    }

    public static class 몹디버그 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                c.getPlayer().dropMessage(6, "Monster " + mob.toString());
            }
            return 1;
        }
    }

    public static class LookNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllNPCsThreadsafe()) {
                MapleNPC reactor2l = (MapleNPC) reactor1l;
                c.getPlayer().dropMessage(5, "NPC: oID: " + reactor2l.getObjectId() + " npcID: " + reactor2l.getId() + " Position: " + reactor2l.getPosition().toString() + " Name: " + reactor2l.getName());
            }
            return 0;
        }
    }

    public static class 근처리액터 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllReactorsThreadsafe()) {
                MapleReactor reactor2l = (MapleReactor) reactor1l;
                c.getPlayer().dropMessage(5, "Reactor: oID: " + reactor2l.getObjectId() + " reactorID: " + reactor2l.getReactorId() + " Position: " + reactor2l.getPosition().toString() + " State: " + reactor2l.getState() + " Name: " + reactor2l.getName());
            }
            return 0;
        }
    }

    public static class LookPortals extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MaplePortal portal : c.getPlayer().getMap().getPortals()) {
                c.getPlayer().dropMessage(5, "Portal: ID: " + portal.getId() + " script: " + portal.getScriptName() + " name: " + portal.getName() + " pos: " + portal.getPosition().x + "," + portal.getPosition().y + " target: " + portal.getTargetMapId() + " / " + portal.getTarget());
            }
            return 0;
        }
    }

    public static class 좌표 extends AdminCommand.현재좌표 {
    }

    public static class 현재좌표 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Point pos = c.getPlayer().getPosition();
            final String format = "[POSITION] Map : %09d  X : %d  Y : %d  RX0 : %d  RX1 : %d  FH : %d";
            c.getPlayer().dropMessage(6, String.format(format, c.getPlayer().getMap().getId(), pos.x, pos.y, (pos.x - 50), (pos.x + 50), c.getPlayer().getFH()));
            return 1;
        }
    }

    public static class Clock extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
            return 1;
        }
    }

    public static class 맵 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                        return 0;
                    }
                    MaplePortal targetPortal = null;
                    if (splitted.length > 3) {
                        try {
                            targetPortal = target.getPortal(Integer.parseInt(splitted[3]));
                        } catch (IndexOutOfBoundsException e) {
                            // noop, assume the gm didn't know how many portals there are
                            c.getPlayer().dropMessage(5, "존재하지 않는 포탈입니다.");
                        } catch (NumberFormatException a) {
                            // noop, assume that the gm is drunk
                        }
                    }
                    if (targetPortal == null) {
                        targetPortal = target.getPortal(0);
                    }
                    victim.changeMap(target, targetPortal);
                }
            } else {
                try {
                    victim = c.getPlayer();
                    int ch = World.Find.findChannel(splitted[1]);
                    if (ch < 0) {
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        if (target == null) {
                            c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                            return 0;
                        }
                        MaplePortal targetPortal = null;
                        if (splitted.length > 2) {
                            try {
                                targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                            } catch (IndexOutOfBoundsException e) {
                                // noop, assume the gm didn't know how many portals there are
                                c.getPlayer().dropMessage(5, "존재하지 않는 포탈입니다.");
                            } catch (NumberFormatException a) {
                                // noop, assume that the gm is drunk
                            }
                        }
                        if (targetPortal == null) {
                            targetPortal = target.getPortal(0);
                        }
                        c.getPlayer().changeMap(target, targetPortal);
                    } else {
                        victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().dropMessage(6, "채널 이동 중입니다. 잠시 기다려 주십시오.");
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                        }
                        c.getPlayer().changeChannel(ch);
                    }
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Something went wrong " + e.getMessage());
                    return 0;
                }
            }
            return 1;
        }
    }

    public static class Jail extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "jail [name] [minutes, 0 = forever]");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            final int minutes = Math.max(0, Integer.parseInt(splitted[2]));
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(GameConstants.JAIL);
                victim.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData(String.valueOf(minutes * 60));
                victim.changeMap(target, target.getPortal(0));
            } else {
                c.getPlayer().dropMessage(6, "Please be on their channel.");
                return 0;
            }
            return 1;
        }
    }

    public static class ListAllSquads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (Map.Entry<MapleSquad.MapleSquadType, MapleSquad> squads : cserv.getAllSquads().entrySet()) {
                    c.getPlayer().dropMessage(5, "[Channel " + cserv.getChannel() + "] TYPE: " + squads.getKey().name() + ", Leader: " + squads.getValue().getLeader().getName() + ", status: " + squads.getValue().getStatus() + ", numMembers: " + squads.getValue().getSquadSize() + ", numBanned: " + squads.getValue().getBannedMemberSize());
                }
            }
            return 1;
        }
    }

    public static class 말하기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                if (!c.getPlayer().isGM()) {
                    sb.append("Intern ");
                }
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "사용법 : 말하기 <메시지>");
                return 0;
            }
            return 1;
        }
    }

    public static class Letter extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "syntax: !letter <color (green/red)> <word>");
                return 0;
            }
            int start, nstart;
            if (splitted[1].equalsIgnoreCase("green")) {
                start = 3991026;
                nstart = 3990019;
            } else if (splitted[1].equalsIgnoreCase("red")) {
                start = 3991000;
                nstart = 3990009;
            } else {
                c.getPlayer().dropMessage(6, "Unknown color!");
                return 0;
            }
            String splitString = StringUtil.joinStringFrom(splitted, 2);
            List<Integer> chars = new ArrayList<Integer>();
            splitString = splitString.toUpperCase();
            // System.out.println(splitString);
            for (int i = 0; i < splitString.length(); i++) {
                char chr = splitString.charAt(i);
                if (chr == ' ') {
                    chars.add(-1);
                } else if ((int) (chr) >= (int) 'A' && (int) (chr) <= (int) 'Z') {
                    chars.add((int) (chr));
                } else if ((int) (chr) >= (int) '0' && (int) (chr) <= (int) ('9')) {
                    chars.add((int) (chr) + 200);
                }
            }
            final int w = 32;
            int dStart = c.getPlayer().getPosition().x - (splitString.length() / 2 * w);
            for (Integer i : chars) {
                if (i == -1) {
                    dStart += w;
                } else if (i < 200) {
                    int val = start + i - (int) ('A');
                    client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                    dStart += w;
                } else if (i >= 200 && i <= 300) {
                    int val = nstart + i - (int) ('0') - 200;
                    client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                    dStart += w;
                }
            }
            return 1;
        }
    }

    public static class ID extends 검색 {
    }

    public static class LookUp extends 검색 {
    }

    public static class 찾기 extends 검색 {
    }

    public static class 검색 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length == 1) {
                c.getPlayer().dropMessage(6, splitted[0] + ": <NPC> <몬스터> <아이템> <맵> <스킬> <퀘스트>");
            } else if (splitted.length == 2) {
                c.getPlayer().dropMessage(6, "Provide something to search.");
            } else {
                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                c.getPlayer().dropMessage(6, "<<종류 : " + type + " | 검색어 : " + search + ">>");

                if (type.equalsIgnoreCase("엔피시")) {
                    List<String> retNpcs = new ArrayList<String>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData npcIdData : data.getChildren()) {
                        npcPairList.add(new Pair<Integer, String>(Integer.parseInt(npcIdData.getName()), MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs != null && retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            c.getPlayer().dropMessage(6, singleRetNpc);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색 결과가 없습니다.");
                    }

                } else if (type.equalsIgnoreCase("맵")) {
                    List<String> retMaps = new ArrayList<String>();
                    data = dataProvider.getData("Map.img");
                    List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            mapPairList.add(new Pair<Integer, String>(Integer.parseInt(mapIdData.getName()), MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps != null && retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            c.getPlayer().dropMessage(6, singleRetMap);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색 결과가 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("몹")) {
                    List<String> retMobs = new ArrayList<String>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mobIdData : data.getChildren()) {
                        mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs != null && retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            c.getPlayer().dropMessage(6, singleRetMob);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색 결과가 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("헤어")) {
                    List<String> retHair = new ArrayList<String>();
                    List<Pair<Integer, String>> hairPairList = new LinkedList<Pair<Integer, String>>();
                    MapleDataProvider hairstring = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
                    MapleData hair = hairstring.getData("Eqp.img");
                    for (MapleData hairData : hair.getChildByPath("Eqp").getChildByPath("Hair")) {
                        hairPairList.add(new Pair<Integer, String>(Integer.parseInt(hairData.getName()), MapleDataTool.getString(hairData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> hairPair : hairPairList) {
                        if (hairPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retHair.add(hairPair.getLeft() + " - " + hairPair.getRight());
                        }
                    }
                    if (retHair != null && retHair.size() > 0) {
                        for (String singleRetHair : retHair) {
                            c.getPlayer().dropMessage(6, singleRetHair);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 헤어가 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("얼굴") || type.equalsIgnoreCase("성형")) {
                    List<String> retface = new ArrayList<String>();
                    List<Pair<Integer, String>> facePairList = new LinkedList<Pair<Integer, String>>();
                    MapleDataProvider facestring = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
                    MapleData face = facestring.getData("Eqp.img");
                    for (MapleData faceData : face.getChildByPath("Eqp").getChildByPath("Face")) {
                        facePairList.add(new Pair<Integer, String>(Integer.parseInt(faceData.getName()), MapleDataTool.getString(faceData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> facePair : facePairList) {
                        if (facePair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retface.add(facePair.getLeft() + " - " + facePair.getRight());
                        }
                    }
                    if (retface != null && retface.size() > 0) {
                        for (String singleRetface : retface) {
                            c.getPlayer().dropMessage(6, singleRetface);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 성형이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("아이템")) {
                    List<String> retItems = new ArrayList<String>();
                    for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.itemId + " - " + itemPair.name);
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색 결과가 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("퀘스트")) {
                    List<String> retItems = new ArrayList<String>();
                    for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                        if (itemPair.getName().length() > 0 && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.getId() + " - " + itemPair.getName());
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색 결과가 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("스킬")) {
                    List<String> retSkills = new ArrayList<String>();
                    for (Skill skil : SkillFactory.getAllSkills()) {
                        if (skil.getName() != null && skil.getName().toLowerCase().contains(search.toLowerCase())) {
                            retSkills.add(skil.getId() + " - " + skil.getName());
                        }
                    }
                    if (retSkills != null && retSkills.size() > 0) {
                        for (String singleRetSkill : retSkills) {
                            c.getPlayer().dropMessage(6, singleRetSkill);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색 결과가 없습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "Sorry, that search call is unavailable");
                }
            }
            return 0;
        }
    }

    public static class WhosFirst extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            //probably bad way to do it
            final long currentTime = System.currentTimeMillis();
            List<Pair<String, Long>> players = new ArrayList<Pair<String, Long>>();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (!chr.isIntern()) {
                    players.add(new Pair<String, Long>(MapleCharacterUtil.makeMapleReadable(chr.getName()) + (currentTime - chr.getCheatTracker().getLastAttack() > 600000 ? " (AFK)" : ""), chr.getChangeTime()));
                }
            }
            Collections.sort(players, new WhoComparator());
            StringBuilder sb = new StringBuilder("List of people in this map in order, counting AFK (10 minutes):  ");
            for (Pair<String, Long> z : players) {
                sb.append(z.left).append(", ");
            }
            c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }

        public static class WhoComparator implements Comparator<Pair<String, Long>>, Serializable {

            @Override
            public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                if (o1.right > o2.right) {
                    return 1;
                } else if (o1.right == o2.right) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }

    public static class WhosLast extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                StringBuilder sb = new StringBuilder("whoslast [type] where type can be:  ");
                for (MapleSquad.MapleSquadType t : MapleSquad.MapleSquadType.values()) {
                    sb.append(t.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            final MapleSquad.MapleSquadType t = MapleSquad.MapleSquadType.valueOf(splitted[1].toLowerCase());
            if (t == null) {
                StringBuilder sb = new StringBuilder("whoslast [type] where type can be:  ");
                for (MapleSquad.MapleSquadType z : MapleSquad.MapleSquadType.values()) {
                    sb.append(z.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            if (t.queuedPlayers.get(c.getChannel()) == null) {
                c.getPlayer().dropMessage(6, "The queue has not been initialized in this channel yet.");
                return 0;
            }
            c.getPlayer().dropMessage(6, "Queued players: " + t.queuedPlayers.get(c.getChannel()).size());
            StringBuilder sb = new StringBuilder("List of participants:  ");
            for (Pair<String, String> z : t.queuedPlayers.get(c.getChannel())) {
                sb.append(z.left).append('(').append(z.right).append(')').append(", ");
            }
            c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }
    }

    public static class WhosNext extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                StringBuilder sb = new StringBuilder("whosnext [type] where type can be:  ");
                for (MapleSquad.MapleSquadType t : MapleSquad.MapleSquadType.values()) {
                    sb.append(t.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            final MapleSquad.MapleSquadType t = MapleSquad.MapleSquadType.valueOf(splitted[1].toLowerCase());
            if (t == null) {
                StringBuilder sb = new StringBuilder("whosnext [type] where type can be:  ");
                for (MapleSquad.MapleSquadType z : MapleSquad.MapleSquadType.values()) {
                    sb.append(z.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            if (t.queue.get(c.getChannel()) == null) {
                c.getPlayer().dropMessage(6, "The queue has not been initialized in this channel yet.");
                return 0;
            }
            c.getPlayer().dropMessage(6, "Queued players: " + t.queue.get(c.getChannel()).size());
            StringBuilder sb = new StringBuilder("List of participants:  ");
            final long now = System.currentTimeMillis();
            for (Pair<String, Long> z : t.queue.get(c.getChannel())) {
                sb.append(z.left).append('(').append(StringUtil.getReadableMillis(z.right, now)).append(" ago),");
            }
            c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }
    }

    public static class WarpMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                final MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                if (target == null) {
                    c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                    return 0;
                }
                final MapleMap from = c.getPlayer().getMap();
                for (MapleCharacter chr : from.getCharactersThreadsafe()) {
                    chr.changeMap(target, target.getPortal(0));
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return 0; //assume drunk GM
            }
            return 1;
        }
    }

    public static class 공격력정보 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final StringBuilder builder = new StringBuilder();
            final MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (other == null) {
                builder.append("존재하지 않는 캐릭터입니다.");
                c.getPlayer().dropMessage(6, builder.toString());
                return 0;
            }
            if (other.getClient().getLastPing() <= 0) {
                other.getClient().sendPing();
            }
            if (other.getGMLevel() > c.getPlayer().getGMLevel()) {
                c.getPlayer().dropMessage(6, "이 캐릭터의 정보를 볼 수 없습니다.");
                return 0;
            }

            builder.append("물리공격력 : ");
            builder.append(other.getStat().getTotalWatk());
            builder.append(" || 숙련도 : ");
            builder.append(other.getStat().getMastery());
            builder.append("% || 스탯공격력 : ");
            builder.append(other.getStat().getCurrentMinBaseDamage());
            builder.append("~");
            builder.append(other.getStat().getCurrentMaxBaseDamage());

            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }

    public static class 피씨방 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int time = 0;
            try {
                time = Integer.parseInt(splitted[1]);
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "피씨방 시간을 초기화하였습니다.");
                c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, c.getPlayer().getCalcPcTime()));
                c.getPlayer().setPcTime((long) 0);
                return 0;
            }
            c.getPlayer().setPcTime((long) time);
            c.getPlayer().setPcDate(GameConstants.getCurrentDate_NoTime());
            c.getPlayer().dropMessage(6, time / 1000 + "초 피씨방을 충전하였습니다.");
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 2, c.getPlayer().getCalcPcTime()));
            return 0;
        }
    }

    public static class SpeakMega extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(3, victim == null ? c.getChannel() : victim.getClient().getChannel(), victim == null ? splitted[1] : victim.getName() + " : " + StringUtil.joinStringFrom(splitted, 2), true));
            return 1;
        }
    }

    public static class Speak extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "unable to find '" + splitted[1]);
                return 0;
            } else {
                victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGM(), 0));
            }
            return 1;
        }
    }

    public static class 스킬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);

            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            c.getPlayer().changeSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }

    public static class 단체버프 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "!버프 <메용, 메이플용사, 헤이, 헤이스트, 홀심, 홀리심볼, 피뻥, 하이퍼바디, 블레스, 쏜즈, 모두>");
                return 0;
            }
            if (splitted[1].contains("홀심") || splitted[1].contains("홀리심볼")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001002).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 홀리심볼");
                    }
                }
            } else if (splitted[1].contains("메용") || splitted[1].contains("메이플용사")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(1221000).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 일반 캐릭터의 메이플용사~");
                    }
                }
            } else if (splitted[1].contains("헤이") || splitted[1].contains("헤이스트")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001001).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 헤이스트");
                    }
                }
            } else if (splitted[1].contains("피뻥") || splitted[1].contains("하이퍼바디")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001008).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 일반 캐릭터의 하이퍼바디~");
                    }
                }
            } else if (splitted[1].contains("블레스")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001003).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 블레스~");
                    }
                }
            } else if (splitted[1].contains("샾") || splitted[1].contains("샤프")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(3121002).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 샤프 아이즈~");
                    }
                }
            } else if (splitted[1].contains("배리어")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(21120007).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 콤보 배리어~");
                    }
                }
            } else if (splitted[1].contains("쏜즈")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(4341007).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 쏜즈 이펙트~");
                    }
                }
            } else if (splitted[1].contains("윈부")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(5121009).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 윈부~");
                    }
                }
            } else if (splitted[1].contains("스왈로우")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(33101006).getEffect(20).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 스왈로우~");
                    }
                }
            } else if (splitted[1].contains("다이스")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(5111007).getEffect(20).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 다이스~");
                    }
                }
            } else if (splitted[1].contains("오더스")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(1211011).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 오더스~");
                    }
                }
            } else if (splitted[1].contains("모두")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001002).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(9001001).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(9001003).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(9001008).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(1221000).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(3121002).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(21120007).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(4341007).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(5111007).getEffect(20).applyTo(chr, false);//다이스 
                        SkillFactory.getSkill(5121009).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(33101006).getEffect(20).applyTo(chr, false);
                        SkillFactory.getSkill(1211011).getEffect(30).applyTo(chr, false);//오더스
                        chr.dropMessage(1, "운영자의 버프타임~");
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "!버프 <헤이, 헤이스트, 홀심, 홀리심볼, 피뻥, 하이퍼바디, 블레스, 배리어, 윈부, 쏜즈, 스왈로우, 오더스, 모두>");
            }
            return 1;
        }
    }

    public static class 단체버프2 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(9001001).getEffect(1).applyTo(chr, false);
                }
            }
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(9001003).getEffect(1).applyTo(chr, false);
                }
            }
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(9001002).getEffect(1).applyTo(chr, false);
                }
            }
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(1121000).getEffect(20).applyTo(chr, false);
                }
            }
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(1, "운영자의 버프타임~"));
            return 1;
        }
    }

    public static class Fame extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: !fame <player> <amount>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            int fame = 0;
            try {
                fame = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(6, "Invalid Number...");
                return 0;
            }
            if (victim != null && player.allowedToTarget(victim)) {
                victim.addFame(fame);
                victim.updateSingleStat(MapleStat.FAME, victim.getFame());
            }
            return 1;
        }
    }

    public static class SP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            c.getSession().write(MaplePacketCreator.updateSp(c.getPlayer(), false));
            return 1;
        }
    }

    public static class 직업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법: !직업 직업코드");
                return 0;
            }
            if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[1])).length() == 0) {
                c.getPlayer().dropMessage(5, "직업코드가 올바르지 않습니다.");
                return 0;
            }
            c.getPlayer().changeJob(Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class 상점 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = Integer.parseInt(splitted[1]);
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            }
            return 1;
        }
    }

    public static class 레벨업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().levelUp();
                c.getPlayer().setExp(0);
                c.getPlayer().updateSingleStat(MapleStat.EXP, c.getPlayer().getExp());
            } else {
                MapleCharacter player = null;
                for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                    for (MapleCharacter other : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                        if (other != null && other.getName().equals(splitted[1])) {
                            player = other;
                        }
                    }
                }
                if (player == null) {
                    return 0;
                }
                int max = 1;
                try {
                    max = Integer.parseInt(splitted[2]);
                } catch (Exception e) {
                    max = 1;
                }
                if (max > 30) {
                    max = 30;
                }
                if (max < 0) {
                    max = 1;
                }
                for (int i = 0; i < max; i++) {
                    player.levelUp();
                    player.setExp(0);
                    player.updateSingleStat(MapleStat.EXP, c.getPlayer().getExp());
                }
                c.getPlayer().dropMessage(5, player.getName() + " 님을 " + max + "번 레벨업 시켰습니다.");
            }
            return 1;
        }
    }

    public static class 경험치 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법 : !경험치 수치");
                return 0;
            }
            if (c.getPlayer().getLevel() < 200) {
                c.getPlayer().gainExp(Integer.parseInt(splitted[1]), true, false, true);
            }
            return 1;
        }
    }

    public static class 아이템 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

            if (!c.getPlayer().isAdmin()) {
                for (int i : GameConstants.itemBlock) {
                    if (itemId == i) {
                        c.getPlayer().dropMessage(5, "해당 아이템은 현재 GM 레벨에서는 생성이 불가능합니다.");
                        return 0;
                    }
                }
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, "존재하지 않는 아이템입니다.");
                return 0;
            } else {
                Item item;
                short flag = (short) ItemFlag.LOCK.getValue();

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.getEquipById(itemId);
                } else {
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                }
                if (!c.getPlayer().isSuperGM()) {
                    item.setFlag(flag);
                }
                //item.setOwner(c.getPlayer().getName());
                item.setGMLog(c.getPlayer().getName() + " !아이템 명령어로 생성된 아이템");
                if (GameConstants.isPet(itemId)) {
                    final int period = CommandProcessorUtil.getOptionalIntArg(splitted, 2, 90);
                    if (period > 0) {
                        item.setQuantity((short) 1);
                        item.setExpiration((long) (System.currentTimeMillis() + (long) ((long) period * 24 * 60 * 60 * 1000)));
                    }
                    final MaplePet pet = MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance());
                    if (pet != null) {
                        item.setPet(pet);
                    } else {
                        c.getPlayer().dropMessage(5, "펫 생성 실패");
                        return 0;
                    }
                }

                MapleInventoryManipulator.addbyItem(c, item);
            }
            return 1;
        }
    }

    public static class 레벨 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setLevel((short) (Short.parseShort(splitted[1])));
            c.getPlayer().levelUp();
            if (c.getPlayer().getExp() < 0) {
                c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false, true);
            }
            return 1;
        }
    }

    public static class StartAutoEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final EventManager em = c.getChannelServer().getEventSM().getEventManager("AutomatedEvent");
            if (em != null) {
                em.scheduleRandomEvent();
            }
            return 1;
        }
    }

    public static class SetEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleEvent.onStartEvent(c.getPlayer());
            return 1;
        }
    }

    public static class 이벤트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getChannelServer().getEvent() == c.getPlayer().getMapId()) {
                MapleEvent.setEvent(c.getChannelServer(), false);
                c.getPlayer().dropMessage(5, "Started the event and closed off");
                return 1;
            } else {
                c.getPlayer().dropMessage(5, "!이벤트예약 must've been done first, and you must be in the event map.");
                return 0;
            }
        }
    }

    public static class 이벤트예약 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleEventType type = MapleEventType.getByString(splitted[1]);
            if (type == null) {
                final StringBuilder sb = new StringBuilder("Wrong syntax: ");
                for (MapleEventType t : MapleEventType.values()) {
                    sb.append(t.name()).append(",");
                }
                c.getPlayer().dropMessage(5, sb.toString().substring(0, sb.toString().length() - 1));
                return 0;
            }
            final String msg = MapleEvent.scheduleEvent(type, c.getChannelServer());
            if (msg.length() > 0) {
                c.getPlayer().dropMessage(5, msg);
                return 0;
            }
            return 1;
        }
    }

    public static class 아이템제거 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "사용법 : !아이템제거 <캐릭터명> <아이템코드>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 캐릭터입니다.");
                return 0;
            }
            chr.removeAll(Integer.parseInt(splitted[2]), false);
            c.getPlayer().dropMessage(6, splitted[1] + "가 가진 모든 " + splitted[2] + "번 아이템이 제거되었습니다.");
            return 1;

        }
    }

    public static class LockItem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 캐릭터입니다.");
                return 0;
            }
            int itemid = Integer.parseInt(splitted[2]);
            MapleInventoryType type = GameConstants.getInventoryType(itemid);
            for (Item item : chr.getInventory(type).listById(itemid)) {
                item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                chr.getClient().getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, chr));
            }
            if (type == MapleInventoryType.EQUIP) {
                type = MapleInventoryType.EQUIPPED;
                for (Item item : chr.getInventory(type).listById(itemid)) {
                    item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                    //chr.getClient().getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                }
            }
            c.getPlayer().dropMessage(6, "All items with the ID " + splitted[2] + " has been locked from the inventory of " + splitted[1] + ".");
            return 1;
        }
    }

    public static class KillMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isGM()) {
                    map.getStat().setHp((short) 0, map);
                    map.getStat().setMp((short) 0, map);
                    map.updateSingleStat(MapleStat.HP, 0);
                    map.updateSingleStat(MapleStat.MP, 0);
                }
            }
            return 1;
        }
    }

    public static class 디버프 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "!디버프 <type> [charname] <level> where type = 봉인/암흑/허약/기절/저주/중독/슬로우/유혹/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            int type = 0;
            if (splitted[1].equalsIgnoreCase("봉인")) {
                type = 120;
            } else if (splitted[1].equalsIgnoreCase("암흑")) {
                type = 121;
            } else if (splitted[1].equalsIgnoreCase("허약")) {
                type = 122;
            } else if (splitted[1].equalsIgnoreCase("기절")) {
                type = 123;
            } else if (splitted[1].equalsIgnoreCase("저주")) {
                type = 124;
            } else if (splitted[1].equalsIgnoreCase("중독")) {
                type = 125;
            } else if (splitted[1].equalsIgnoreCase("슬로우")) {
                type = 126;
            } else if (splitted[1].equalsIgnoreCase("유혹")) {
                type = 128;
            } else if (splitted[1].equalsIgnoreCase("REVERSE")) {
                type = 132;
            } else if (splitted[1].equalsIgnoreCase("ZOMBIFY")) {
                type = 133;
            } else if (splitted[1].equalsIgnoreCase("POTION")) {
                type = 134;
            } else if (splitted[1].equalsIgnoreCase("SHADOW")) {
                type = 135;
            } else if (splitted[1].equalsIgnoreCase("BLIND")) {
                type = 136;
            } else if (splitted[1].equalsIgnoreCase("FREEZE")) {
                type = 137;
            } else if (splitted[1].equalsIgnoreCase("POTENTIAL")) {
                type = 138;
            } else {
                c.getPlayer().dropMessage(6, "!디버프 <type> [charname] <level> where type = 봉인/암흑/허약/기절/저주/중독/슬로우/유혹/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            if (splitted.length == 4) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                if (victim == null) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
            } else {
                for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                    victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
                }
            }
            return 1;
        }
    }

    public static class SetInstanceProperty extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                em.setProperty(splitted[2], splitted[3]);
                for (EventInstanceManager eim : em.getInstances()) {
                    eim.setProperty(splitted[2], splitted[3]);
                }
            }
            return 1;
        }
    }

    public static class ListInstanceProperty extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                for (EventInstanceManager eim : em.getInstances()) {
                    c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", eventManager: " + em.getName() + " iprops: " + eim.getProperty(splitted[2]) + ", eprops: " + em.getProperty(splitted[2]));
                }
            }
            return 0;
        }
    }

    public static class LeaveInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() == null) {
                c.getPlayer().dropMessage(5, "You are not in one");
            } else {
                c.getPlayer().getEventInstance().unregisterPlayer(c.getPlayer());
            }
            return 1;
        }
    }

    public static class StartInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() != null) {
                c.getPlayer().dropMessage(5, "You are in one");
            } else if (splitted.length > 2) {
                EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
                if (em == null || em.getInstance(splitted[2]) == null) {
                    c.getPlayer().dropMessage(5, "Not exist");
                } else {
                    em.getInstance(splitted[2]).registerPlayer(c.getPlayer());
                }
            } else {
                c.getPlayer().dropMessage(5, "!startinstance [eventmanager] [eventinstance]");
            }
            return 1;

        }
    }

    public static class ResetMobs extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().killAllMonsters(false);
            return 1;
        }
    }

    public static class MulungEF extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().mulung_EnergyModify(300);
            return 1;
        }
    }

    public static class KillMonsterByOID extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.killMonster(monster, c.getPlayer(), false, false, (byte) 1);
            }
            return 1;
        }
    }

    public static class 엔피시삭제 extends AdminCommand.RemoveNPCs {
    }

    public static class RemoveNPCs extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetNPCs();
            return 1;
        }
    }

    public static class 공지 extends CommandExecute {

        protected static int getNoticeType(String typestring) {
            if (typestring.equals("팝업")) {
                return 1;
            }
            return -1;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int joinmod = 1;
            int range = -1;
            if (splitted[1].equals("맵")) {
                range = 0;
            } else if (splitted[1].equals("채널")) {
                range = 1;
            } else if (splitted[1].equals("월드")) {
                range = 2;
            }

            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            StringBuilder sb = new StringBuilder();
            joinmod += tfrom;
            sb.append(StringUtil.joinStringFrom(splitted, joinmod));

            byte[] packet = MaplePacketCreator.serverNotice(type, sb.toString());
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class TDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().toggleDrops();
            return 1;
        }
    }

    public static class MesoEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.gainMeso(Integer.parseInt(splitted[1]), true);
                }
            }
            return 1;
        }
    }

    public static class 드롭배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                RateManager.TRUEDROP = rate;
                c.getPlayer().dropMessage(6, "드롭 배율을 " + RateManager.getTrueDropRate() + "배로 변경했습니다.");
            } else {
                c.getPlayer().dropMessage(6, "사용법 : !드롭배율 <숫자>");
            }
            return 1;
        }
    }

    public static class 경험치배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                try {
                    final int rate = Integer.parseInt(splitted[1]);

                    if (rate < 1 || rate > 500) {
                        c.getPlayer().dropMessage(6, "배율은 1 ~ 500 사이로 입력해 주세요.");
                        return 0;
                    }

                    server.RateManager.EXP = rate;
                    c.getPlayer().dropMessage(6, "경험치 배율을 " + rate + "배로 변경했습니다.");
                    return 1;

                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(6, "숫자만 입력해 주세요. 예) !경험치배율 2");
                    return 0;
                }
            } else {
                c.getPlayer().dropMessage(6, "사용법 : !경험치배율 <숫자>");
                return 0;
            }
        }
    }

    public static class 메소배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                RateManager.MESO = rate;
                c.getPlayer().dropMessage(6, "메소 배율을 " + rate + "배로 변경했습니다.");
            } else {
                c.getPlayer().dropMessage(6, "사용법 : !메소배율 <숫자>");
            }
            return 1;
        }
    }

    public static class DCAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 1;
            }
            if (range == 0) {
                c.getPlayer().getMap().disconnectAll();
            } else if (range == 1) {
                c.getChannelServer().getPlayerStorage().disconnectAll(true);
            } else if (range == 2) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.getPlayerStorage().disconnectAll(true);
                }
            }
            return 1;
        }
    }

    public static class TPetPickUp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean allowed = c.getPlayer().getMap().togglePetPick();
            c.getPlayer().dropMessage(6, "Current Map's Pet Pickup allowed : " + allowed);
            if (!allowed) {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 비활성화 되었습니다."));
            } else {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 활성화 되었습니다."));
            }
            return 1;
        }

    }

    public static class 서버종료 extends CommandExecute {

        protected static Thread t = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Shutting down...");
            if (t == null || !t.isAlive()) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "A shutdown thread is already in progress or shutdown has not been done. Please wait.");
            }
            return 1;
        }
    }

    public static class 서버종료시간 extends 서버종료 {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(6, minutesLeft + "분 후 서버가 종료됩니다.");
            if (ts == null && (t == null || !t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ts = EventTimer.getInstance().register(new Runnable() {

                    public void run() {
                        if (minutesLeft == 0) {
                            ShutdownServer.getInstance().shutdown();
                            t.start();
                            ts.cancel(false);
                            return;
                        }
                        World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage("서버가 " + minutesLeft + "분 후 종료됩니다. 안전하게 로그아웃해 주세요. 재시작 일정인 경우, 알림이나 공지사항을 참조해 주시기 바랍니다."));
                        minutesLeft--;
                    }
                }, 60000);
            } else {
                c.getPlayer().dropMessage(6, "A shutdown thread is already in progress or shutdown has not been done. Please wait.");
            }
            return 1;
        }
    }

    public static class Shutdown9 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long time = cal.getTimeInMillis();
            long schedulewait = 0;
            if (time > System.currentTimeMillis()) {
                schedulewait = time - System.currentTimeMillis();
            } else {
                schedulewait = time + 86400000L - System.currentTimeMillis();
            }
            if (schedulewait < 3600000) {
                schedulewait += 86400000L;
            }
//            schedulewait += (86400000L * 2);
            System.out.println("[Shutdown] Server will shutdown automatically in " + StringUtil.getReadableMillis(0, schedulewait).replace("일", "days ").replace("시간", "hours ").replace("분", "mins ").replace("초", "secs."));
            BroadcastMsgSchedule("잠시 후 서버 안정을 위하여 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 3600000);
            BroadcastMsgSchedule("30분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 1800000);
            BroadcastMsgSchedule("15분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 900000);
            BroadcastMsgSchedule("10분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 600000);
            BroadcastMsgSchedule("5분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 300000);
            BroadcastMsgSchedule("2분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 120000);
            BroadcastMsgSchedule("1분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 60000);
            Timer.WorldTimer.getInstance().schedule(new Start.Shutdown(), schedulewait);
            return 1;
        }

        public static void BroadcastMsgSchedule(final String msg, long schedule) {
            Timer.CloneTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat(msg));
                }
            }, schedule);
        }
    }

    public static class 노랑 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 2;
            }
            byte[] packet = MaplePacketCreator.yellowChat((splitted[0].equals("!y") ? ("[" + c.getPlayer().getName() + "] ") : "") + StringUtil.joinStringFrom(splitted, 2));
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class StartProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            sampler.addIncluded("client");
            sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
            sampler.addIncluded("database");
            sampler.addIncluded("handling");
            sampler.addIncluded("provider");
            sampler.addIncluded("scripting");
            sampler.addIncluded("server");
            sampler.addIncluded("tools");
            sampler.start();
            return 1;
        }
    }

    public static class StopProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            try {
                String filename = "odinprofile.txt";
                if (splitted.length > 1) {
                    filename = splitted[1];
                }
                File file = new File(filename);
                if (file.exists()) {
                    c.getPlayer().dropMessage(6, "The entered filename already exists, choose a different one");
                    return 0;
                }
                sampler.stop();
                FileWriter fw = new FileWriter(file);
                sampler.save(fw, 1, 10);
                fw.close();
            } catch (IOException e) {
                System.err.println("Error saving profile" + e);
            }
            sampler.reset();
            return 1;
        }
    }

    public static class 저장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            // User Data Save Start
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    chr.saveToDB(false, false);
                }
            }
            // User Data Save End
            // Server Data Save Start
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
            MarriageManager.getInstance().saveAll();
            MinervaOwlSearchTop.getInstance().saveToFile();
            MedalRanking.saveAll();
            //       RankingWorker.getInstance().run();
            // Server Data Save End
            c.getPlayer().dropMessage(6, "저장이 완료되었습니다.");
            return 1;
        }
    }

    public static class 아이피대조 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "사용법: !아이피대조 <캐릭터 닉네임>");
                return 0;
            }
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            PreparedStatement ps2 = null;
            ResultSet rs2 = null;

            int Accid = 0, Count = 0;
            String IP = "";
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
                ps.setString(1, splitted[1]);
                rs = ps.executeQuery();
                if (rs.next()) {
                    Accid = rs.getInt("accountid");
                    ps.close();
                    rs.close();
                    ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    ps.setInt(1, Accid);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        IP = rs.getString("SessionIP");
                        c.getPlayer().dropMessage(2, "검색한 캐릭터의 접속 아이디 파악 (검색 값 : " + splitted[1] + ") [아이피 : " + IP + "]");
                        ps.close();
                        rs.close();
                        ps = con.prepareStatement("SELECT * FROM accounts WHERE SessionIP = ?");
                        ps.setString(1, IP);
                        rs = ps.executeQuery();
                        String Text = "";
                        while (rs.next()) {
                            if (rs.getInt("banned") > 0) {
                                Text = " / 밴 당한 아이디";
                            }
                            c.getPlayer().dropMessage(5, "아이디 : " + rs.getString("name") + " / " + Text);
                            Accid = rs.getInt("id");
                            ps2 = con.prepareStatement("SELECT * FROM characters WHERE accountid = ?");
                            ps2.setInt(1, Accid);
                            rs2 = ps2.executeQuery();
                            while (rs2.next()) {
                                Count++;
                                c.getPlayer().dropMessage(6, Count + "번 캐릭터 : " + rs2.getString("name"));
                            }
                            if (Count == 0) {
                                c.getPlayer().dropMessage(6, rs.getString("name") + " 아이디는 캐릭터가 없습니다.");
                            }
                            Count = 0;
                            ps2.close();
                            rs2.close();
                        }
                        ps.close();
                        rs.close();
                    } else {
                        c.getPlayer().dropMessage(5, "버그가 발생했습니다.");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(5, "존재하지 않는 닉네임입니다.");
                    return 0;
                }
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                    if (rs2 != null) {
                        rs2.close();
                    }
                    if (ps2 != null) {
                        ps2.close();
                    }
                } catch (Exception e) {
                }
            }
            return 1;
        }
    }
}
