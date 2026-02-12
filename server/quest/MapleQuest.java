package server.quest;

import client.MapleCharacter;
import client.MapleQuestStatus;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import scripting.NPCScriptManager;
import server.ItemInfo;
import server.MapleInventoryManipulator;
import server.log.LogType;
import server.log.ServerLogger;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import tools.packet.PetPacket;

import server.MapleInventoryManipulator;

public class MapleQuest implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private static final Map<Integer, MapleQuest> quests = new LinkedHashMap<Integer, MapleQuest>();
    protected int id, timeLimit, timeLimit2;
    protected final List<MapleQuestRequirement> startReqs = new LinkedList<MapleQuestRequirement>();
    protected final List<MapleQuestRequirement> completeReqs = new LinkedList<MapleQuestRequirement>();
    protected final List<MapleQuestAction> startActs = new LinkedList<MapleQuestAction>();
    protected final List<MapleQuestAction> completeActs = new LinkedList<MapleQuestAction>();
    protected final Map<String, List<Pair<String, Pair<String, Integer>>>> partyQuestInfo = new LinkedHashMap<String, List<Pair<String, Pair<String, Integer>>>>(); //[rank, [more/less/equal, [property, value]]]
    protected final Map<Integer, Integer> relevantMobs = new LinkedHashMap<Integer, Integer>();
    private boolean autoStart = false, autoPreComplete = false, repeatable = false, customend = false, blocked = false, autoAccept = false, autoComplete = false, scriptedStart = false, customQuest = true;
    private int viewMedalItem = 0, selectedSkillID = 0;
    protected String name = "";

    protected MapleQuest(final int id) {
        this.id = id;
    }

    private static MapleQuest loadQuest(ResultSet rs, PreparedStatement psr, PreparedStatement psa, PreparedStatement pss, PreparedStatement psq, PreparedStatement psi, PreparedStatement psp) throws SQLException {
        final MapleQuest ret = new MapleQuest(rs.getInt("questid"));
        ret.name = rs.getString("name");
        ret.autoStart = rs.getInt("autoStart") > 0;
        ret.autoPreComplete = rs.getInt("autoPreComplete") > 0;
        ret.autoAccept = rs.getInt("autoAccept") > 0;
        ret.autoComplete = rs.getInt("autoComplete") > 0;
        ret.viewMedalItem = rs.getInt("viewMedalItem");
        ret.selectedSkillID = rs.getInt("selectedSkillID");
        ret.blocked = rs.getInt("blocked") > 0; //ult.explorer quests will dc as the item isn't there...
        ret.timeLimit = rs.getInt("timeLimit");
        ret.timeLimit2 = rs.getInt("timeLimit2");
        ret.customQuest = false;

        psr.setInt(1, ret.id);
        ResultSet rse = psr.executeQuery();
        while (rse.next()) {
            final MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(rse.getString("name"));
            final MapleQuestRequirement req = new MapleQuestRequirement(ret, type, rse);
            if (type.equals(MapleQuestRequirementType.interval)) {
                ret.repeatable = true;
            } else if (type.equals(MapleQuestRequirementType.normalAutoStart)) {
                ret.repeatable = true;
                ret.autoStart = true;
            } else if (type.equals(MapleQuestRequirementType.dayByDay)) { //일일퀘스트 daybyday
                ret.repeatable = true;
            } else if (type.equals(MapleQuestRequirementType.startscript)) {
                ret.scriptedStart = true;
            } else if (type.equals(MapleQuestRequirementType.endscript)) {
                ret.customend = true;
            } else if (type.equals(MapleQuestRequirementType.mob)) {
                for (Pair<Integer, Integer> mob : req.getDataStore()) {
                    ret.relevantMobs.put(mob.left, mob.right);
                }
            }
            if (rse.getInt("type") == 0) {
                ret.startReqs.add(req);
            } else {
                ret.completeReqs.add(req);
            }
        }
        rse.close();

        psa.setInt(1, ret.id);
        rse = psa.executeQuery();
        while (rse.next()) {
            final MapleQuestActionType ty = MapleQuestActionType.getByWZName(rse.getString("name"));
            if (rse.getInt("type") == 0) { //pass it over so it will set ID + type once done
                if (ty == MapleQuestActionType.item && ret.id == 7103) { //pap glitch
                    continue;
                }
                ret.startActs.add(new MapleQuestAction(ty, rse, ret, pss, psq, psi));
            } else {
                if (ty == MapleQuestActionType.item && ret.id == 7102) { //pap glitch
                    continue;
                }
                ret.completeActs.add(new MapleQuestAction(ty, rse, ret, pss, psq, psi));
            }
        }
        rse.close();

        psp.setInt(1, ret.id);
        rse = psp.executeQuery();
        while (rse.next()) {
            if (!ret.partyQuestInfo.containsKey(rse.getString("rank"))) {
                ret.partyQuestInfo.put(rse.getString("rank"), new ArrayList<Pair<String, Pair<String, Integer>>>());
            }
            ret.partyQuestInfo.get(rse.getString("rank")).add(new Pair<String, Pair<String, Integer>>(rse.getString("mode"), new Pair<String, Integer>(rse.getString("property"), rse.getInt("value"))));
        }
        rse.close();
        return ret;
    }

    public List<Pair<String, Pair<String, Integer>>> getInfoByRank(final String rank) {
        return partyQuestInfo.get(rank);
    }

    public boolean isPartyQuest() {
        return partyQuestInfo.size() > 0;
    }

    public final int getSkillID() {
        return selectedSkillID;
    }

    public final String getName() {
        return name;
    }

    public final List<MapleQuestAction> getCompleteActs() {
        return completeActs;
    }

    public static void initQuests() {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM wz_questdata");
            PreparedStatement psr = con.prepareStatement("SELECT * FROM wz_questreqdata WHERE questid = ?");
            PreparedStatement psa = con.prepareStatement("SELECT * FROM wz_questactdata WHERE questid = ?");
            PreparedStatement pss = con.prepareStatement("SELECT * FROM wz_questactskilldata WHERE uniqueid = ?");
            PreparedStatement psq = con.prepareStatement("SELECT * FROM wz_questactquestdata WHERE uniqueid = ?");
            PreparedStatement psi = con.prepareStatement("SELECT * FROM wz_questactitemdata WHERE uniqueid = ?");
            PreparedStatement psp = con.prepareStatement("SELECT * FROM wz_questpartydata WHERE questid = ?");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                quests.put(rs.getInt("questid"), loadQuest(rs, psr, psa, pss, psq, psi, psp));
            }
            rs.close();
            ps.close();
            psr.close();
            psa.close();
            pss.close();
            psq.close();
            psi.close();
            psp.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static MapleQuest getInstance(int id) {
        MapleQuest ret = quests.get(id);
        if (ret == null) {
            ret = new MapleQuest(id);
            quests.put(id, ret); //by this time we have already initialized
        }
        return ret;
    }

    public static Collection<MapleQuest> getAllInstances() {
        return quests.values();
    }

    public boolean canStart(MapleCharacter c, Integer npcid) {
        if (getId() == 3048) {
            return true;
        }
        if (c.getQuest(this).getStatus() != 0 && !(c.getQuest(this).getStatus() == 2 && repeatable)) {
            return false;
        }
        if (blocked && !c.isGM()) {
            return false;
        }
        if (customQuest) {
            return false; // 퀘스트 유저가 만들어서 강제 클리어 해버린다리
        }
        //if (autoAccept) {
        //    return true; //need script
        //}
        for (MapleQuestRequirement r : startReqs) {
            if (!r.check(c, npcid)) {
                return false;
            }
            /*if (!r.check(c, npcid)) {
             if (r.getType() == MapleQuestRequirementType.item) {
             int ret = r.checkItem(c);
             if (ret != 0) {
             c.dropMessage(1, ItemInfo.getName(ret) + "을(를) 장착중이어서 퀘스트를 시작할 수 없습니다.");
             return false;
             }
             }
             return false;
             }*/
            // 3.18 퀘스트 체크 부분 임시 주석 나중에 퀘스트 제한에 문제생기면 다시 봐보자
        }
        return true;
    }

    public boolean canComplete(MapleCharacter c, Integer npcid) {
        if (c.getQuest(this).getStatus() != 1) {
            System.out.println("debug1");
            return false;
        }
        if (blocked && !c.isGM()) {
            System.out.println("debug2");
            return false;
        }
        /*if (autoComplete && npcid != null && viewMedalItem <= 0) {
         forceComplete(c, npcid);
         return false; //skip script
         }*/
        for (MapleQuestRequirement r : completeReqs) {
            if (!r.check(c, npcid)) {
                if (r.getType() == MapleQuestRequirementType.item) {
                    int ret = r.checkItem(c);
                    if (ret != 0) {
                        c.dropMessage(1, ItemInfo.getName(ret) + "을(를) 장착중이어서 퀘스트를 완료할 수 없습니다.");
                        return false;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public final void RestoreLostItem(final MapleCharacter c, final int itemid) {
        if (blocked && !c.isGM()) {
            return;
        }
        for (final MapleQuestAction a : startActs) {
            if (a.RestoreLostItem(c, itemid)) {
                break;
            }
        }
    }

    public void start(MapleCharacter c, int npc) {
        if ((autoStart && canStart(c, npc) || checkNPCOnMap(c, npc)) && canStart(c, npc) || (getId() / 1000 == 29 && canStart(c, npc))) {
            for (MapleQuestAction a : startActs) {
                if (!a.checkEnd(c, null)) { //just in case
                    return;
                }
            }
            String info = null;
            for (MapleQuestAction a : startActs) {
                a.runStart(c, null);
                if (info == null) {
                    info = a.getInfoData(); //temp fix
                }
                if (a.getNpcAct() != null) {
                    c.getMap().broadcastMessage(MaplePacketCreator.showNpcSpecialAction(c.getMap().getNPCById(npc).getObjectId(), a.getNpcAct()));
                }
            }
            if (getId() == 29003) { //fucking hardcoding
                info = "time_" + (System.currentTimeMillis() + (86400000L * 30));
            }
            if (!customend) {
                forceStart(c, npc, info);
            } else {
                if (c.getQuestStatus(id) == 0 || (c.getQuestStatus(id) == 2 && repeatable)) {
                    if (!scriptedStart) {
                        forceStart(c, npc, null);
                        return;
                    }
                }
                NPCScriptManager.getInstance().endQuest(c.getClient(), npc, getId(), true);
            }
        }
    }

    public void complete(MapleCharacter c, int npc) {
        complete(c, npc, null);
    }

    public void complete(MapleCharacter c, int npc, Integer selection) {
        if (c.getMap() != null && (autoComplete && canComplete(c, npc) || autoPreComplete && canComplete(c, npc) || checkNPCOnMap(c, npc)) && canComplete(c, npc)) {
            for (MapleQuestAction a : completeActs) {
                if (!a.checkEnd(c, selection)) {
                    return;
                }
            }
            forceComplete(c, npc);
            for (MapleQuestAction a : completeActs) {
                a.runEnd(c, selection);
                if (a.getNpcAct() != null) {
                    c.getMap().broadcastMessage(MaplePacketCreator.showNpcSpecialAction(c.getMap().getNPCById(npc).getObjectId(), a.getNpcAct()));
                }
            }
            // we save forfeits only for logging purposes, they shouldn't matter anymore
            // completion time is set by the constructor

            c.getClient().getSession().write(MaplePacketCreator.showSpecialEffect(11)); // Quest completion
            c.getMap().broadcastMessage(c, MaplePacketCreator.showSpecialEffect(c.getId(), 11), false);

            final int QNX = ServerConstants.CashRate * 50;
            final int QcountPerCoke = 3;
            int getDailyQuestBonusStatus = c.getDailyQuestBonus(1);
            //int getDailyQuestBonusCount = c.getDailyQuestBonus(2);
            if (c.getLevel() >= 8 && !repeatable) {
                c.dropMessage(6, "퀘스트 클리어 보상으로 일정량의 캐시를 얻었습니다.");
                c.modifyCSPoints(1, QNX, true);
                if (ServerConstants.dailyQuestBonus) {
                    //if (getDailyQuestBonusCount == 0) {
                    if (getDailyQuestBonusStatus == QcountPerCoke - 1) { //보상받을떄 필요한 q갯수 -1해줘야함
                        /*if (c.getInventory(MapleInventoryType.USE).getNumFreeSlot() == 0) {
                         c.giveHolySymbol(1800000);
                         c.dropMessage(6, "소비 인벤토리 공간이 부족하여 경험치 1.5배 버프가 즉시 적용됩니다.");
                         } else {
                         MapleInventoryManipulator.addById(c.getClient(), 2022035, (short) 1, "", null, 1, "Obtained from Daily Bonus on " + FileoutputUtil.CurrentReadable_Date());
                         c.dropMessage(6, "5번째 퀘스트 클리어 보상으로 '펩시콜라'를 얻었습니다. 사용시 30분의 홀리심볼 버프가 적용 됩니다.");
                         }*/
                        int 분 = 1000 * 60;//1000은 1초
                        int 값 = 30; //30분 충전
                        long time = 분 * 값;
                        if (c.getPcTime() > System.currentTimeMillis()) {
                            c.addPcTime((long) time);
                        } else {
                            c.setPcTime((long) time);
                        }
                        c.setPcDate(GameConstants.getCurrentDate_NoTime());
                        c.dropMessage(6, "5번째 퀘스트 클리어 보상으로 프리미엄 PC방 " + time / 분 + "분을 충전하였습니다.");
                        c.getClient().getSession().write(MaplePacketCreator.enableInternetCafe((byte) 2, c.getCalcPcTime()));
                        c.setDailyQuestBonus(2, 1); //type 2는 0으로 초기화 
                        ServerLogger.getInstance().logDailyQuestBonus(LogType.Item.DailyQuestBonus, c.getId(), c.getName(), true, true);
                    } else {
                        c.setDailyQuestBonus(1, 0); //type 1은 인크리스
                    }
                    //}
                }
            }
        }
    }

    /*    public void complete(MapleCharacter c, int npc, Integer selection) {
     if (c.getMap() != null && (autoComplete || autoPreComplete || checkNPCOnMap(c, npc)) && canComplete(c, npc)) {
     for (MapleQuestAction a : completeActs) {
     if (!a.checkEnd(c, selection)) {
     return;
     }
     }
     forceComplete(c, npc);
     for (MapleQuestAction a : completeActs) {
     a.runEnd(c, selection);
     if (a.getNpcAct() != null) {
     c.getMap().broadcastMessage(MaplePacketCreator.showNpcSpecialAction(c.getMap().getNPCById(npc).getObjectId(), a.getNpcAct()));
     }
     }
     // we save forfeits only for logging purposes, they shouldn't matter anymore
     // completion time is set by the constructor

     c.getClient().getSession().write(MaplePacketCreator.showSpecialEffect(9)); // Quest completion
     c.getMap().broadcastMessage(c, MaplePacketCreator.showSpecialEffect(c.getId(), 9), false);

     final int QNX = ServerConstants.CashRate * 50;
     int getDailyQuestBonusStatus = c.getDailyQuestBonus(1);
     int getDailyQuestBonusCount = c.getDailyQuestBonus(2);
     boolean canGainCash = true;
     switch (getId()) {
     case 3069: //잃어버린 네펜데스 즙
     case 3079: //잃어버린 네펜데스 주스
     case 3095: //잃어버린 사진첩
     case 3096: //켄타의 조언(반복)
     case 3450: //잃어버린 자유여행권 조각
     case 3451: //잃어버린 여행권 조각
     case 3609: //제비가 잃어버린 박씨
     case 3637: //도끼 감별
     case 7105: //잃어버린 균열조각
     case 7106: //잃어버린 메달
     case 7107: //잃어버린 균열 조각
     canGainCash = false;
     }
     if (c.getLevel() >= 8 && canGainCash) {
     c.modifyCSPoints(1, QNX, false);
     c.dropMessage(6, "퀘스트 클리어 보상으로 일정량의 캐시를 얻었습니다.");
     if (getDailyQuestBonusCount == 0) {
     if (getDailyQuestBonusStatus == 2) {
     if (c.getInventory(MapleInventoryType.USE).getNumFreeSlot() == 0) {
     if (ServerConstants.specialDailyQuestBonus) {
     c.giveHolySymbol(3600000);
     c.dropMessage(6, "소비 인벤토리 공간이 부족하여 1시간 동안 경험치 1.5배 버프가 적용됩니다.");
     ServerLogger.getInstance().logDailyQuestBonus(LogType.Item.DailyQuestBonus, c.getId(), c.getName(), true, false);
     } else {
     c.giveHolySymbol(1800000);
     c.dropMessage(6, "소비 인벤토리 공간이 부족하여 경험치 1.5배 버프가 즉시 적용됩니다.");
     ServerLogger.getInstance().logDailyQuestBonus(LogType.Item.DailyQuestBonus, c.getId(), c.getName(), false, false);
     }
     } else {
     if (ServerConstants.specialDailyQuestBonus) {
     MapleInventoryManipulator.addById(c.getClient(), 2022033, (short) 2, "", null, 1, "Obtained from Special Daily Bonus on " + FileoutputUtil.CurrentReadable_Date());
     c.dropMessage(6, "'캔디' 아이템을 얻었습니다. 사용하면 15분 동안 경험치 2배 버프가 적용됩니다.");
     ServerLogger.getInstance().logDailyQuestBonus(LogType.Item.DailyQuestBonus, c.getId(), c.getName(), true, true);
     } else {
     MapleInventoryManipulator.addById(c.getClient(), 2022035, (short) 1, "", null, 1, "Obtained from Daily Bonus on " + FileoutputUtil.CurrentReadable_Date());
     c.dropMessage(6, "'펩시콜라' 아이템을 얻었습니다. 사용하면 30분 동안 경험치 1.5배 버프가 적용됩니다.");
     ServerLogger.getInstance().logDailyQuestBonus(LogType.Item.DailyQuestBonus, c.getId(), c.getName(), false, true);
     }
     }
     c.setDailyQuestBonus(1, 1);                        //퀘스트 3번 클리어시 PC방 60분 추가
     c.setPcTime((long) 1000 * 60 * ServerConstants.PC_BONUS_TIME); // 1000 = 1초
     c.setPcDate(GameConstants.getCurrentDate_NoTime()); // 오늘 날짜를 대입
     } else {
     c.setDailyQuestBonus(1, 0);
     }
     }
     }
     }
     }*/
    public void forfeit(MapleCharacter c) {
        if (c.getQuest(this).getStatus() != (byte) 1) {
            return;
        }
        if (timeLimit > 0) {
            c.getClient().getSession().write(MaplePacketCreator.removeQuestTimeLimit(id));
        }
        final MapleQuestStatus oldStatus = c.getQuest(this);
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 0);
        newStatus.setForfeited(oldStatus.getForfeited() + 1);
        newStatus.setCompletionTime(oldStatus.getCompletionTime());
        c.updateQuest(newStatus);
    }

    private static void giveAcceptReward(final MapleCharacter chr, final int questId) {
    
    if (questId == 31117) { //밧줄 돼지 농장
        final int itemId = 2270021;
        final short qty = 20;

        // 인벤 공간 체크 (없으면 메시지로 안내하고 지급 안 함)
        if (!MapleInventoryManipulator.checkSpace(chr.getClient(), itemId, qty, "")) {
            chr.dropMessage(5, "인벤토리 공간이 부족하여 퀘스트 수락 보상을 받을 수 없습니다.");
            return;
        }

        chr.gainItem(itemId, qty);
    }

    // 필요하면 다른 퀘스트도 else-if로 추가
}
    
    public void forceStart(MapleCharacter c, int npc, String customData) {
        final byte oldStatus = c.getQuest(this).getStatus();
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 1, npc);
        newStatus.setForfeited(c.getQuest(this).getForfeited());
        newStatus.setCompletionTime(c.getQuest(this).getCompletionTime());
        newStatus.setCustomData(customData);
        c.updateQuest(newStatus);
        
        if (oldStatus == 0) {
        giveAcceptReward(c, this.id);
        }

        if (timeLimit > 0) {
            newStatus.setExpirationTime(System.currentTimeMillis() + (timeLimit * 1000));
            c.questTimeLimit(this, timeLimit);
        }
        if (timeLimit2 * 1000 > 0) {
            timeLimit2 *= 1000;
        }
        if (timeLimit2 > 0) {
            newStatus.setExpirationTime(System.currentTimeMillis() + timeLimit2);
            c.questTimeLimit2(this, newStatus.getExpirationTime());
        }
    }

    public void forceComplete(MapleCharacter c, int npc) {
        final MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 2, npc);
        if (newStatus.getQuest().getId() == 3083 || newStatus.getQuest().getId() == 3096) {
            for (final MaplePet pet : c.getPets()) {
                pet.setSpeed(1);
                c.getClient().getSession().write(PetPacket.updatePet(pet, c.getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), true));
            }
        }
        if (timeLimit > 0) {
            c.getClient().getSession().write(MaplePacketCreator.removeQuestTimeLimit(id));
        }
        newStatus.setForfeited(c.getQuest(this).getForfeited());
        
        // nextQuest:
        boolean update = false;
        for (MapleQuestAction r : completeActs) {
            if (r.getType() == MapleQuestActionType.nextQuest) {
                update = true;
                break;
            }
        }
        c.updateQuest(newStatus, update);
    }

    public int getId() {
        return id;
    }

    public Map<Integer, Integer> getRelevantMobs() {
        return relevantMobs;
    }

    private boolean checkNPCOnMap(MapleCharacter player, int npcid) {
        //mir = 1013000
        return (GameConstants.isEvan(player.getJob()) && npcid == 1013000) || npcid == 9000040 || npcid == 9000066 || (player.getMap() != null && player.getMap().containsNPC(npcid));
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getMedalItem() {
        return viewMedalItem;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public static enum MedalQuest {

        초보(29005, 29015, 15, new int[]{100000000, 100020400, 100040000, 101000000, 101020300, 101040300, 102000000, 102020500, 102030400, 102040200, 103000000, 103020200, 103030400, 103040000, 104000000, 104020000, 106020100, 120000000, 120020400, 120030000}),
        엘나스산맥(29006, 29012, 50, new int[]{200000000, 200010100, 200010300, 200080000, 200080100, 211000000, 211030000, 211040300, 211041200, 211041800}),
        루더스호수(29007, 29012, 40, new int[]{222000000, 222010400, 222020000, 220000000, 220020300, 220040200, 221020701, 221000000, 221030600, 221040400}),
        아쿠아리움(29008, 29012, 40, new int[]{230000000, 230010400, 230010200, 230010201, 230020000, 230020201, 230030100, 230040000, 230040200, 230040400}),
        무릉(29009, 29012, 50, new int[]{251000000, 251010200, 251010402, 251010500, 250010500, 250010504, 250000000, 250010300, 250010304, 250020300}),
        니할사막(29010, 29012, 70, new int[]{261030000, 261020401, 261020000, 261010100, 261000000, 260020700, 260020300, 260000000, 260010600, 260010300}),
        미나르숲(29011, 29012, 70, new int[]{240000000, 240010200, 240010800, 240020401, 240020101, 240030000, 240040400, 240040511, 240040521, 240050000}),
        슬리피우드(29014, 29015, 50, new int[]{105000000, 105000000, 105010100, 105020100, 105020300, 105030000, 105030100, 105030300, 105030500, 105030500}); //repeated map
        public int questid, level, lquestid;
        public int[] maps;

        private MedalQuest(int questid, int lquestid, int level, int[] maps) {
            this.questid = questid; //infoquest = questid -2005, customdata = questid -1995
            this.level = level;
            this.lquestid = lquestid;
            this.maps = maps; //note # of maps
        }
    }

    public boolean hasStartScript() {
        return scriptedStart;
    }

    public boolean hasEndScript() {
        return customend;
    }

    public void gmQuest(MapleCharacter c, Integer npcid) {
        for (MapleQuestRequirement r : completeReqs) {
            r.gmQuestCheck(c, npcid);
        }
    }
}
