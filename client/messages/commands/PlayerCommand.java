package client.messages.commands;

//import client.MapleInventory;
//import client.MapleInventoryType;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.PlayerStats;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import scripting.NPCScriptManager;
import scripting.vm.NPCScriptInvoker;
import tools.MaplePacketCreator;

import java.util.Map;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.log.ServerLogger;
import server.maps.MapleMap;
import server.maps.SavedLocationType;

import server.life.MapleMonster;
import server.maps.MapleMapObject;

import handling.channel.handler.DueyHandler;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * @author Emilyx3
 */
public class PlayerCommand {

    public static ServerConstants.PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.NORMAL;
    }

    public static class For extends 렉 {
    }

    public static class fpr extends 렉 {
    }

    public static class 랙 extends 렉 {
    }

    public static class 렉 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            return 1;
        }
    }

    public static class 아이템드랍검색 extends 아이템드롭검색 {
    }

    public static class 아이템드롭검색 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000003);

            return 1;
        }
    }

    public static class 몬스터드랍검색 extends 몬스터드롭검색 {
    }

    public static class 몬스터드롭검색 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            NPCScriptInvoker.runNpc(c, 9900001, 0);

            return 1;
        }
    }

    public static class 큐브교환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9010015);
            return 0;
        }
    }

    public static class 마북 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 2084002);
            return 0;
        }
    }

    public static class 일괄판매 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int a = 0;
            int meso = c.getPlayer().getMeso();
            MapleInventory use = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            for (int i = 0; i < use.getSlotLimit(); i++) {
                Item item = use.getItem((byte) i);
                if (item != null) {
                    Equip ep = (Equip) item;

                    if (!ii.isPickupRestricted(item.getItemId()) // 고유
                            && !ii.isDropRestricted(item.getItemId())
                            && !ii.isCash(item.getItemId()) // 캐시
                            && !ii.isAccountShared(item.getItemId()) // 계정공유
                            && !ii.isKarmaEnabled(item.getItemId()) // 카르마
                            && !ii.isPKarmaEnabled(item.getItemId()) // 플래티넘카르마
                            && ep.getState() == 0 // 미확인
                            && ii.getReqLevel(item.getItemId()) < 120 //  레벨제한 120 미만만 판매
                            && c.getPlayer().haveItem(item.getItemId(), 1, true, true)) {
                        MapleShop.playersell(c, GameConstants.getInventoryType(item.getItemId()), (byte) i, (short) item.getQuantity());
                        a++;
                    }
                }
            }

            int meso2 = c.getPlayer().getMeso();
            c.getPlayer().dropMessage(6, "일괄판매로 " + a + "개의 아이템을 판매하였습니다. 획득한 메소 " + (meso2 - meso));
            return 1;
        }
    }

    public static class 일괄판매2 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int a = 0;
            int meso = c.getPlayer().getMeso();
            MapleInventory use = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            for (int i = 0; i < use.getSlotLimit(); i++) {
                Item item = use.getItem((byte) i);
                if (item != null) {
                    Equip ep = (Equip) item;

                    if (!ii.isPickupRestricted(item.getItemId())
                            && !ii.isDropRestricted(item.getItemId())
                            && !ii.isCash(item.getItemId())
                            && !ii.isAccountShared(item.getItemId())
                            && !ii.isKarmaEnabled(item.getItemId())
                            && !ii.isPKarmaEnabled(item.getItemId())
                            && ep.getState() <= 2 // 미확인
                            && ii.getReqLevel(item.getItemId()) < 120 // 벨제한 120 미만만 판매
                            && c.getPlayer().haveItem(item.getItemId(), 1, true, true)) {
                        MapleShop.playersell(c, GameConstants.getInventoryType(item.getItemId()), (byte) i, (short) item.getQuantity());
                        a++;
                    }
                }
            }

            int meso2 = c.getPlayer().getMeso();
            c.getPlayer().dropMessage(6, "일괄판매로 " + a + "개의 아이템을 판매하였습니다. 획득한 메소 " + (meso2 - meso));
            return 1;
        }
    }

    public static class 동접 extends 동접확인 {
    }

    public static class 동접확인 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {//왜 지엠까지 동접을 체크하는가?
            Map<Integer, Integer> connected = World.getConnected();
            for (int i : connected.keySet()) {
                StringBuilder conStr = new StringBuilder();
                if (i == 0) {
                    conStr.append("현재 접속 중인 인원 : 총 ");
                    conStr.append(connected.get(i));
                    conStr.append("명");
                } else if (i == -10) {
                    conStr.append("CS. : ");
                    conStr.append(connected.get(i));
                    conStr.append("명");
                } else {
                    conStr.append("CH.");
                    if (i == 1) {
                        conStr.append("1");
                    } else if (i == 2) {
                        conStr.append("20세이상");
                    } else {
                        conStr.append(i - 1);
                    }
                    conStr.append(" : ");
                    conStr.append(connected.get(i));
                    conStr.append("명");
                }
                c.getPlayer().dropMessage(6, conStr.toString());
            }
            return 1;
        }
    }

    public static class 몹피 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();

            // 현재 맵의 몬스터 수집
            final java.util.List<MapleMonster> mobs = new java.util.ArrayList<>();
            for (final MapleMapObject mmo : chr.getMap().getAllMonstersThreadsafe()) {
                // getAllMonstersThreadsafe()는 컬렉션을 리턴하므로 캐스팅
                final MapleMonster mob = (MapleMonster) mmo;
                // 죽은 개체 필터링(HP 0 이하)
                if (mob.getHp() > 0) {
                    mobs.add(mob);
                }
            }

            if (mobs.isEmpty()) {
                chr.dropMessage(6, "이 맵에는 살아있는 몬스터가 없습니다.");
                return 1;
            }

            // 너무 길어지는 것 방지: 최대 30마리까지만 출력
            final int limit = Math.min(30, mobs.size());
            chr.dropMessage(6, "[몹피] 현재 맵 몬스터 HP (" + mobs.size() + "마리 중 " + limit + "마리 표시)");

            for (int i = 0; i < limit; i++) {
                final MapleMonster mob = mobs.get(i);
                final long cur = mob.getHp();                     // 현재 HP
                final long max = mob.getStats().getHp();          // 최대 HP
                final long lost = max - cur;                      // 잃은 HP
                final String name = mob.getStats().getName();     // 몬스터 이름
                final int level = mob.getStats().getLevel();      // 레벨

                double pct = (max > 0) ? (cur * 100.0 / max) : 0.0;

                chr.dropMessage(6,
                        String.format(
                                " - %s (Lv.%d): %,d / %,d (%.1f%%) | 잃은 HP: %,d",
                                name, level, cur, max, pct, lost
                        )
                );
            }

            if (mobs.size() > limit) {
                chr.dropMessage(6, "(※ 표시 제한으로 나머지 " + (mobs.size() - limit) + "마리는 생략됨)");
            }
            return 1;
        }
    }

    public static class 큐브옵션 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            // 이미 열려있는 대화가 있으면 정리(선택)
            NPCScriptManager.getInstance().dispose(c);
            NPCScriptManager.getInstance().start(c, 9000002, "miracle");
            return 1;
        }
    }

    public static class 타이머 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();

            if (splitted.length < 2) {
                chr.dropMessage(6, "사용법: @타이머 <초> (1~7200 사이)");
                return 0;
            }

            int seconds;
            try {
                seconds = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException e) {
                chr.dropMessage(6, "숫자만 입력해 주세요. (예: @타이머 60)");
                return 0;
            }

            if (seconds < 1 || seconds > 7200) {
                chr.dropMessage(6, "입력 범위는 1~7200초입니다.");
                return 0;
            }

            chr.dropMessage(6, seconds + "초 타이머를 시작합니다,");

            // 타이머 스케줄
            java.util.concurrent.ScheduledExecutorService scheduler
                    = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

            scheduler.schedule(() -> {
                try {
                    if (chr != null && chr.getClient() != null && chr.getClient().getSession() != null) {
                        chr.dropMessage(6, "시간 " + seconds + "초가 경과했습니다,");
                    }
                } catch (Exception ignored) {
                }
                scheduler.shutdown(); // 스레드 종료
            }, seconds, java.util.concurrent.TimeUnit.SECONDS);

            return 1;
        }
    }

    public static class 드랍 extends PlayerCommand.드롭검색 {
    }

    public static class 아이템검색 extends PlayerCommand.드롭검색 {
    }

    public static class 드롭 extends PlayerCommand.드롭검색 {
    }

    public static class 드롭검색 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            //c.getPlayer().searchMob(splitted[1]);
            NPCScriptManager.getInstance().start(c, 9000019, "9000019");
            //NPCScriptInvoker.runNpc(c, 9000019, 0);
            return 1;
        }
    }

    public static class 피부 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000004);
            return 0;
        }
    }

    public static class 헤어검색 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000006);
            return 0;
        }
    }

    public static class 자석 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9900000);
            return 0;
        }
    }

    public static class 코디검색 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000005);
            return 0;
        }
    }

    public static class 스킬검색 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9010007);
            return 0;
        }
    }

    public static class 이동기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9010017);
            return 0;
        }
    }

    public static class 캐시버리기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 1012121);
            return 0;
        }
    }

    public static class 도움말 extends PlayerCommand.명령어 {
    }

    public static class 명령어 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            player.dropMessage(6, "@렉 : 오류로 인한 이미 활성중인 상태를 비활성으로 전환.");
            player.dropMessage(6, "@동접, 동접확인 : 현재 동시 접속자 수 확인.");
            player.dropMessage(6, "@드롭, 드롭검색, 아이템검색 : 드롭데이터 정보센터.");
            player.dropMessage(6, "@힘, 덱, 인, 럭 <숫자> : 숫자만큼 AP를 소비하여 스텟을 올립니다.");
            player.dropMessage(6, "@헤어검색, @코디검색, @피부, @몬스터드롭검색, @아이템드롭검색");
            player.dropMessage(6, "@주사위 : 굴리기, @자석 : 자석 기능 설정, @스킬검색 : 스킬 정보 확인");
            player.dropMessage(6, "@캐시버리기 : 의류수거함을 불러올 수 있습니다.");
            player.dropMessage(6, "@큐브교환 : 큐브를 교환해 주는 NPC를 불러옵니다.");
            player.dropMessage(6, "@마북 : 마스터리 북을 거래하는 NPC를 불러옵니다.");
            player.dropMessage(6, "@일괄판매, @일괄판매2 : 장비 아이템을 일괄 판매할 수 있습니다.");
            player.dropMessage(6, "@큐브옵션 : 희망 큐브 옵션을 설정하는 NPC를 불러옵니다.");
            player.dropMessage(6, "@이동기 : 100 레벨 이상의 캐릭터에 플래시 점프 또는 텔레포트를 익힐 수 있습니다. (도적 사용 불가)");
            return 1;
        }
    }

    public static class 힘 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int str = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();
                if (stat.getStr() + str > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < str || c.getPlayer().getRemainingAp() < 0 || str < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setStr((short) (stat.getStr() + str), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - str));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.STR, stat.getStr());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 지 extends PlayerCommand.인 {
    }

    public static class 지력 extends PlayerCommand.인 {
    }

    public static class 인트 extends PlayerCommand.인 {
    }

    public static class 인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int int_ = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();

                if (stat.getInt() + int_ > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < int_ || c.getPlayer().getRemainingAp() < 0 || int_ < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setInt((short) (stat.getInt() + int_), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - int_));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.INT, stat.getInt());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 민첩 extends PlayerCommand.덱 {
    }

    public static class 민 extends PlayerCommand.덱 {
    }

    public static class 덱스 extends PlayerCommand.덱 {
    }

    public static class 덱 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int dex = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();

                if (stat.getDex() + dex > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < dex || c.getPlayer().getRemainingAp() < 0 || dex < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setDex((short) (stat.getDex() + dex), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - dex));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.DEX, stat.getDex());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 운 extends PlayerCommand.럭 {
    }

    public static class 행운 extends PlayerCommand.럭 {
    }

    public static class 럭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int luk = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();

                if (stat.getLuk() + luk > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < luk || c.getPlayer().getRemainingAp() < 0 || luk < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setLuk((short) (stat.getLuk() + luk), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - luk));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.LUK, stat.getLuk());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 길드공지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            final String notice = splitted[1];
            if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) {
                player.dropMessage(6, "길드를 가지고 있지 않거나 권한이 부족한것 같은데?");
                return 1;
            }
            if (notice.length() > 100) {
                player.dropMessage(6, "너무 길어 씹년아");
                return 1;
            }
            World.Guild.setGuildNotice(c.getPlayer().getGuildId(), notice);
            return 1;
        }
    }

    public static class 보상수령 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c == null || c.getPlayer() == null) {
                return 0;
            }

            final int itemId = 2430192;
            final int itemQty = 10;
            final int charId = c.getPlayer().getId();
            final int accId = c.getAccID();

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                con = DatabaseConnection.getConnection();
                con.setAutoCommit(false);

                LocalDate today = LocalDate.now(); // 기간 설정

                LocalDate startDate = LocalDate.of(2026, 1, 25);
                LocalDate endDate = LocalDate.of(2026, 2, 28);

                if (today.isBefore(startDate) || today.isAfter(endDate)) {
                    c.getPlayer().dropMessage(5,
                            "이 보상은 2026년 1월 25일 ~ 2월 28일에만 받을 수 있습니다.");
                    return 0;
                }

                // 1) 내 계정의 SessionIP + totalvotes 잠금 조회
                String sessionIP = null;
                int myVotes = 0;

                ps = con.prepareStatement("SELECT SessionIP, totalvotes FROM accounts WHERE id = ? FOR UPDATE");
                ps.setInt(1, accId);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    con.rollback();
                    c.getPlayer().dropMessage(5, "계정 정보를 찾을 수 없습니다.");
                    return 0;
                }

                sessionIP = rs.getString("SessionIP");
                myVotes = rs.getInt("totalvotes");

                rs.close();
                rs = null;
                ps.close();
                ps = null;

                if (sessionIP == null || sessionIP.isEmpty()) {
                    con.rollback();
                    c.getPlayer().dropMessage(5, "회원가입 IP 정보가 없습니다.");
                    return 0;
                }

                // 이미 이 계정이 받은 상태면 바로 차단(빠른 리턴)
                if (myVotes > 0) {
                    con.rollback();
                    c.getPlayer().dropMessage(5, "이미 보상을 수령하셨습니다.");
                    return 0;
                }

                // 2) 같은 SessionIP 그룹 중 totalvotes > 0 이 있는지 확인 (잠금)
                ps = con.prepareStatement(
                        "SELECT COUNT(*) AS cnt FROM accounts WHERE SessionIP = ? AND totalvotes > 0 FOR UPDATE"
                );
                ps.setString(1, sessionIP);
                rs = ps.executeQuery();

                int claimed = 0;
                if (rs.next()) {
                    claimed = rs.getInt("cnt");
                }

                rs.close();
                rs = null;
                ps.close();
                ps = null;

                if (claimed > 0) {
                    con.rollback();
                    c.getPlayer().dropMessage(5, "동일 회원가입 IP에서는 보상을 1회만 받을 수 있습니다.");
                    return 0;
                }

                // 3) 조건 만족 시 내 계정 totalvotes = 1로 변경
                ps = con.prepareStatement("UPDATE accounts SET totalvotes = 1 WHERE id = ? AND totalvotes = 0");
                ps.setInt(1, accId);
                int updated = ps.executeUpdate();

                if (updated <= 0) {
                    con.rollback();
                    c.getPlayer().dropMessage(5, "보상 수령 처리에 실패했습니다.)");
                    return 0;
                }

                con.commit();

            } catch (Exception e) {
                try {
                    if (con != null) {
                        con.rollback();
                    }
                } catch (Exception ignore) {
                }
                c.getPlayer().dropMessage(5, "오류로 인해 보상 수령이 실패했습니다.");
                e.printStackTrace();
                return 0;

            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                } catch (Exception ignore) {
                }
                try {
                    if (ps != null) {
                        ps.close();
                    }
                } catch (Exception ignore) {
                }
                try {
                    if (con != null) {
                        con.setAutoCommit(true);
                        con.close();
                    }
                } catch (Exception ignore) {
                }
            }

            // 4) 지급 (DB 커밋 성공 후)
            c.getSession().write(MaplePacketCreator.receiveParcel("[운영자]", true));
            DueyHandler.addNewItemToDb(
                    itemId,
                    itemQty,
                    charId,
                    "[운영자]",
                    "보상 지급입니다. 광장의 퀘스트 NPC에게 가 주세요.",
                    true
            );

            c.getPlayer().dropMessage(5, "보상 수령이 완료되었습니다. 택배함을 확인해 주세요.");
            c.getPlayer().dropMessage(5, "다른 IP로 회원가입하여 보상을 수급할시 정지 처분을 받을 수 있습니다.");
            return 1;
        }
    }

    public static class 주사위 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int random = (int) (Math.random() * 100) + 1;
            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                chr.dropMessage(6, c.getPlayer().getName() + "님의 주사위 결과 : " + random);
            }
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound("Coconut/Fall"));

            return 1;
        }
    }
}
