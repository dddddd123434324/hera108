/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.extract;

import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterStats;
import server.maps.MapleReactorFactory;
import server.quest.MapleQuest;
import server.quest.MapleQuestAction;
import server.quest.MapleQuestAction.QuestItem;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author 티썬
 */
public class MapleDropDataParser {
//쉬프트 f6

    private static void addFrankenroid(List<int[]> datas) {
        // 프랑켄로이드, 화난 프랑켄로이드
        for (int i = 9300139; i <= 9300140; ++i) {
            for (int z = 0; z < 3; ++z) {
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000002, 1, 1, 0, 600000});
                datas.add(new int[]{i, 2000004, 1, 1, 0, 100000});
                datas.add(new int[]{i, 2000005, 1, 1, 0, 100000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});
                datas.add(new int[]{i, 2000006, 1, 1, 0, 300000});

                datas.add(new int[]{i, 2001001, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020013, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020013, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2001001, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020014, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020014, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2001002, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2001002, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020015, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2020015, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2022003, 1, 1, 0, 200000});
                datas.add(new int[]{i, 2022003, 1, 1, 0, 200000});
            }
            for (int iiz = 0; iiz < 8; ++iiz) {
                datas.add(new int[]{i, 0, 30, 49, 0, 999999});
            }
            //

            datas.add(new int[]{i, 2044601, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2040707, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044401, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2040504, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044501, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044001, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043701, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043001, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2040004, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2044701, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043801, 1, 1, 0, 10000});
            datas.add(new int[]{i, 2043301, 1, 1, 0, 10000});

            datas.add(new int[]{i, 1092030, 1, 1, 0, 10000});//35제 메이플
            datas.add(new int[]{i, 1302020, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1382009, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1452016, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1462014, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1472030, 1, 1, 0, 10000});

            datas.add(new int[]{i, 1302030, 1, 1, 0, 10000});//43제
            datas.add(new int[]{i, 1332025, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1382012, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1412011, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1422014, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1432012, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1442024, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1452022, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1462019, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1472032, 1, 1, 0, 10000});

            datas.add(new int[]{i, 1092045, 1, 1, 0, 10000});//64제
            datas.add(new int[]{i, 1092046, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1092047, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1302064, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1312032, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1322054, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1332055, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1332056, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1372034, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1382039, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1402039, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1412027, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1422029, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1432040, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1442051, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1452045, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1462040, 1, 1, 0, 10000});
            datas.add(new int[]{i, 1472055, 1, 1, 0, 10000});

            datas.add(new int[]{i, 5060002, 1, 1, 0, 50000});
            datas.add(new int[]{i, 5060002, 1, 1, 0, 50000});
            datas.add(new int[]{i, 5060002, 1, 1, 0, 50000});

        }
    }

    private static void add(Integer itemid, List<MobDropEntry> newMobDrops, Entry<Integer, List<Integer>> mBookChild, boolean isBoss, boolean isRaidBoss) {
        int mobid = 0;
        if (mBookChild.getKey() == 8220013) {
            mobid = 8220015;
        } else if (mBookChild.getKey() == 9400408) {
            mobid = 9400409;
        } else {
            mobid = mBookChild.getKey();
        }
        if (itemid == 1002390) {
            itemid = 1002357;
        }
        if (itemid / 10000 == 206) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, 8000, 20, 30, 0)); //화살
        } else if (itemid / 10000 == 200) {
            if (mobid != 8810018) {
                for (int i = 0; i < (isBoss ? 5 : 1); ++i) {
                    if (itemid == 2000019) {
                        itemid = 2000005;
                    }
                    if (itemid >= 2000000 && itemid < 2000004 || itemid >= 2000006 && itemid < 2000011) {
                        newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 700000 : isBoss ? 350000 : 100000, 1, 10, 0)); //물약
                    } else {
                        if (itemid == 2000004 || itemid == 2000005) {
                            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 700000 : isBoss ? 350000 : 1000, 1, 10, 0)); //물약
                        } else {
                            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 700000 : isBoss ? 350000 : 50000, 1, 10, 0)); //물약
                        }
                    }
                }
            }
        } else if (itemid / 1000 == 4004) { //크리스탈 원석류
            if (mobid == 9400265 || mobid == 9400270 || mobid == 9400273 || mobid == 9400266) {//베르가모트, 듀나스, 니베룽, 이름없는마수
                newMobDrops.add(new MobDropEntry(mobid, itemid, 100000, 1, 5, 0));
            } else {
                newMobDrops.add(new MobDropEntry(mobid, itemid, 5000, 1, 5, 0));
            }
        } else if (itemid / 1000 == 4005) { //크리스탈 완제품
            if (mobid == 9400265 || mobid == 9400270 || mobid == 9400273 || mobid == 9400266) {//베르가모트, 듀나스, 니베룽, 이름없는마수
                newMobDrops.add(new MobDropEntry(mobid, itemid, 100000, 1, 5, 0));
            } else {
                newMobDrops.add(new MobDropEntry(mobid, itemid, 5000, 1, 5, 0));
            }
        } else if (itemid / 1000 == 4000) {
            if (itemid == 4000451 || itemid == 4000456 || itemid == 4000446 || itemid == 4000448 || itemid == 4000458 || itemid == 4000453) {
                newMobDrops.add(new MobDropEntry(mobid, itemid, 80000, 1, 1, 0)); // 전리품
            } else {
                newMobDrops.add(new MobDropEntry(mobid, itemid, 600000, 1, 1, 0)); // 전리품
            }
        } else if (itemid / 10000 == 401) {
            if (itemid == 4011004) {
                itemid = 4010004;
            }
            newMobDrops.add(new MobDropEntry(mobid, itemid, 4000, 1, 5, 0)); //광석 원석
        } else if (itemid / 1000 == 4020) {
            if (itemid == 4020009) {
                int chance = 8;
                switch (mobid) {
                    case 8200001:
                    case 8200002:
                        chance = 48;
                        break;
                    case 8200003:
                        chance = 180;
                        break;
                    case 8200004:
                        chance = 300;
                        break;
                    case 8200005:
                    case 8200006:
                        chance = 540;
                        break;
                    case 8200007:
                    case 8200008:
                        chance = 660;
                        break;
                    case 8200009:
                    case 8200010:
                        chance = 1040;
                        break;
                    case 8200011:
                        chance = 1500;
                        break;
                    case 8200012:
                        chance = 2400;
                        break;

                }
                newMobDrops.add(new MobDropEntry(mobid, itemid, chance, 1, 1, 0)); //시간 조각
            } else {
                newMobDrops.add(new MobDropEntry(mobid, itemid, 3000, 1, 5, 0)); //보석 원석..
            }
        } else if (itemid == 2049100) {
            int chaosScrollChance = 0;
            if (isRaidBoss) {
                chaosScrollChance = 30000;
            } else if (isBoss) {
                switch (mobid) {
                    case 4220000:
                    case 7220000:
                    case 8180001:
                    case 8220006:
                        chaosScrollChance = 7500;
                        break;
                }
            } else {
                itemid = 2046308;
                chaosScrollChance = 80;
                newMobDrops.add(new MobDropEntry(mobid, 2046313, chaosScrollChance, 1, 1, 0));
            }
            if (mobid != 8220001 && mobid != 8300007 && mobid != 8830000 && mobid != 9500390) {
                newMobDrops.add(new MobDropEntry(mobid, itemid, chaosScrollChance, 1, 1, 0)); //혼돈의 주문서..
            }
        } else if (itemid != 2049100 && itemid / 10000 == 204) {
            if (itemid / 1000 == 2047) {
                if (mobid != 8810018) {
                    newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 100000 : isBoss ? 50000 : 500, 1, 1, 0)); //연성서
                }
            } else {
                if (mobid == 8810018) {
                    switch (itemid) {
                        case 2040317:
                        case 2040418:
                        case 2040421:
                        case 2040512:
                        case 2040515:
                        case 2040625:
                            break;
                        default:
                            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 30000 : isBoss ? 9000 : 70, 1, 1, 0)); //주문서..
                            break;
                    }
                } else if (mobid == 9420544 || mobid == 9420549) {
                    switch (itemid) {
                        case 2040001:
                        case 2040004:
                        case 2040401:
                        case 2040504:
                        case 2040601:
                        case 2040901:
                            break;
                        default:
                            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 30000 : isBoss ? 9000 : 70, 1, 1, 0)); //주문서..
                            break;
                    }
                } else if (mobid != 9600025) {
                    int scrollChance;
                    if (isRaidBoss) {
                        scrollChance = 30000;
                    } else if (isBoss) {
                        scrollChance = 9000;
                    } else {
                        scrollChance = 70;
                    }
                    if (mobid == 9400289) {
                        if (itemid == 2040037) {
                            itemid = 2040099;
                            scrollChance = 5000;
                        } else if (itemid >= 2040033 && itemid <= 2040036) {
                            scrollChance = 1000000;
                        }
                    } else if (mobid == 9400294) {
                        if (itemid >= 2040033 && itemid <= 2040036) {
                            scrollChance = 100000;
                        }
                    }
                    switch (itemid) {
                        default:
                            newMobDrops.add(new MobDropEntry(mobid, itemid, scrollChance, 1, 1, 0)); //주문서..
                            break;
                    }
                }
            }
        } else if (itemid / 10000 == 203 || itemid / 10000 == 201 || itemid / 10000 == 202 || itemid / 10000 == 205) {//귀환 줌서, 특수물약, 등등
            boolean peitem = false;
            if (itemid >= 2022570 && itemid <= 2022584) {
                peitem = true;
            }
            for (int i = 0; i < ((isBoss && !peitem) ? 5 : 1); ++i) {
                newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 700000 : isBoss ? 350000 : 5000, 1, 1, 0));
            }
        } else if (itemid / 1000000 == 1) {
            int f = 20;
            MapleMonsterStats stats = MapleLifeFactory.getMonsterStats(mobid);
            int itemlevel = MapleItemInformationProvider.getInstance().getReqLevel(itemid);
            if (Math.abs(stats.getLevel() - itemlevel) <= 5) {
                f = 10 + (stats.getLevel() - itemlevel);
            } else if (stats.getLevel() - itemlevel < -5) {//아이템레벨이 5보다 더 높은 경우
                f = 5;
            } else if (stats.getLevel() - itemlevel < -10) { //아이템레벨이 10보다 더 높은 경우
                f = 3;
            } else if (stats.getLevel() - itemlevel > 5) { //몹 레벨이 아이템 레벨 보다 5보다 더 높은 경우
                f = 17;
            }
            if (itemid == 1003068) {//라바나 투구
                isBoss = false;
                f = 10000;
            }
            if (itemlevel >= 60 && itemlevel <= 70 && mobid == 9500390 && itemid != 1003068) {//라바나 1단계
                newMobDrops.add(new MobDropEntry(mobid, itemid, 60000, 1, 1, 0)); //장비
                newMobDrops.add(new MobDropEntry(9500391, itemid, 70000, 1, 1, 0)); //2단계 라바나
            } else if (itemlevel >= 95 && itemlevel <= 110 && mobid == 9500390) {
                newMobDrops.add(new MobDropEntry(9500392, itemid, 100000, 1, 1, 0)); //3단계 라바나
            } else if (mobid != 9500390) {
                newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 100000 : isBoss ? 100000 : f * 5, 1, 1, 0)); //장비
            }
        } else if (itemid / 1000 == 4007) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, 1000, 1, 5, 0)); //가루
        } else if (itemid / 1000 == 4006) { //돌류
            if (isBoss) {
                for (int i = 0; i <= 5; ++i) {
                    newMobDrops.add(new MobDropEntry(mobid, itemid, 300000, 1, 5, 0));
                }
            } else {
                newMobDrops.add(new MobDropEntry(mobid, itemid, 1000, 1, 5, 0));
            }
        } else if (itemid / 10000 == 400) {
            if (itemid == 4001107 || itemid >= 4001110 && itemid <= 4001112) {
            } else {
                int chance = 600;
                if (itemid == 4003004) {
                    chance = 100000;
                } else if (itemid == 4003005) {
                    chance = 50000;
                }
                newMobDrops.add(new MobDropEntry(mobid, itemid, chance, 1, 1, 0)); //기타
            }
        } else if (itemid / 10000 == 228) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 300000 : isBoss ? 50000 : 60, 1, 1, 0)); //스북
        } else if (itemid / 10000 == 229) {
            int masteryBookChance;
            if (isRaidBoss) {
                switch (mobid) {
                    case 8800002:
                    case 8810018:
                    case 9420544:
                    case 9420549:
                        masteryBookChance = 100000;
                        break;
                    case 9420522:
                        masteryBookChance = 33000;
                        break;
                    default:
                        masteryBookChance = 110000;
                        break;
                }
            } else if (isBoss) {
                masteryBookChance = 5000;
            } else {
                masteryBookChance = 6;
            }
            if (itemid == 2290125) {
                masteryBookChance = 20000;
            }
            newMobDrops.add(new MobDropEntry(mobid, itemid, masteryBookChance, 1, 1, 0)); //마북
        }/* else if (itemid / 10000 == 416) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 110000 : isBoss ? 50000 : 60, 1, 1, 0)); //스토리북
        }*/ else if (itemid / 10000 == 233 || itemid / 10000 == 207) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 110000 : isBoss ? 5000 : 40, 1, 1, 0)); //표창 불릿
        } else if (itemid / 10000 == 413) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, 1000, 1, 1, 0)); //촉진제
        } else if (itemid / 10000 == 221) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, 999999, 1, 1, 0)); //변신물약
        } else if (itemid / 10000 == 301) {
            newMobDrops.add(new MobDropEntry(mobid, itemid, isRaidBoss ? 110000 : isBoss ? 6000 : 6, 1, 1, 0)); //의자?;
        } else if (itemid / 10000 == 238) {
            System.err.println("몬스터카드: " + itemid);
        } else {
            System.err.println("Undefined code : " + itemid);
        }
    }

    public enum DropType {

        Mob, Reactor;
    }

    public static void main(String[] args) throws Exception {
        DatabaseConnection.init();
        File from = new File("imgs");
        System.setProperty("net.sf.odinms.wzpath", "wz");
        MapleDataProvider pro = MapleDataProviderFactory.getDataProvider(from);
        MapleData root = pro.getData("Reward.img");
        Connection con = DatabaseConnection.getConnection();
        Map<Integer, Integer> quests = new HashMap<Integer, Integer>();

        List<Integer> customMoneyMobAdd = new ArrayList<Integer>();
        List<MobDropEntry> mobdrops = new ArrayList<MobDropEntry>();
        List<ReactorDropEntry> reactordrops = new ArrayList<>();
        Map<Integer, List<Integer>> mBookRewards = new HashMap<Integer, List<Integer>>();

        MapleQuest.initQuests();
        MapleItemInformationProvider.getInstance().runItems();
        for (MapleQuest quest : MapleQuest.getAllInstances()) {
            for (MapleQuestAction act : quest.getCompleteActs()) {
                if (act.getItems() == null) {
                    continue;
                }
                for (QuestItem qitem : act.getItems()) {
                    if (qitem.count < 0 && MapleItemInformationProvider.getInstance().isQuestItem(qitem.itemid)) {
                        if (quests.containsKey(qitem.itemid)) {
                            System.err.println("Warning : Duplicate Quest Required Item. - ItemID : " + qitem.itemid + " Quest1 : " + quests.get(qitem.itemid) + ", Quest2 : " + quest.getId());
                        }
                        quests.put(qitem.itemid, quest.getId());
                    }
                }
            }
        }
        //K:QuestItems   V:QuestID
//        PreparedStatement select = con.prepareStatement("SELECT id,itemid,count FROM wz_questactitemdata");
//        ResultSet rs = select.executeQuery();
//        while (rs.next()) {
//            int itemid = rs.getInt("itemid");
//            int count = rs.getInt("count");
//            int qid = rs.getInt("id");
//            if (itemid / 10000 == 403 && count < 0) {
//                if (quests.containsKey(itemid)) {
//                    System.err.println("Warning : Duplicate Quest Required Item. - ItemID : " + itemid + " Quest1 : " + quests.get(itemid) + ", Quest2 : " + qid);
//                }
//                quests.put(itemid, qid);
//            }
//        }
//        select.close();
//        rs.close();
        System.out.println("Cached Quest Items : " + quests.size());

        long start = System.currentTimeMillis();
        System.out.println("Job Start");

        List<Integer> questdrops = new ArrayList<Integer>();

        PreparedStatement ps = con.prepareStatement("INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, maximum_quantity, questid, chance) VALUES (?, ?, ?, ?, ?, ?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO `reactordrops` (`reactorid`, `itemid`, `chance`, `questid`, `min`, `max`) VALUES (?, ?, ?, ?, ?, ?)");
        PreparedStatement del1 = con.prepareStatement("TRUNCATE `drop_data`");
        PreparedStatement del2 = con.prepareStatement("TRUNCATE `reactordrops`");
        del1.executeUpdate();
        del2.executeUpdate();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ii.runItems();
        ii.runEtc();
        for (MapleData reroot : root.getChildren()) {
            DropType type;
            int id = Integer.parseInt(reroot.getName().substring(1));
            if (reroot.getName().startsWith("m")) {
                if (MapleLifeFactory.getMonster(id) == null) {
                    System.out.println("Monster Id " + id + " is not exists.... Continue...");
                    continue;
                }
                type = DropType.Mob;
            } else {
                try {
                    MapleReactorFactory.getReactor(id);
                } catch (RuntimeException r) {
                    System.out.println("Reactor Id " + id + " is not exists.... Continue...");
                    continue;
                }
                type = DropType.Reactor;
            }

            for (MapleData content : reroot.getChildren()) {
                int id2 = Integer.parseInt(reroot.getName().substring(1));
                int itemid = MapleDataTool.getIntConvert("item", content, 0);

                if (!ii.itemExists(itemid) && itemid != 0) {
                    System.err.println("Item " + itemid + " does not exists.. Continue.");
                    continue;
                }

                if (itemid == 4000047) {
                    continue;
                }

                int money = MapleDataTool.getIntConvert("money", content, 0);
                int prob = (int) Math.round(Double.parseDouble(MapleDataTool.getString("prob", content).substring(4)) * 1000000);
                int min = MapleDataTool.getIntConvert("min", content, 1);
                int max = MapleDataTool.getIntConvert("max", content, 1);
                int quest = 0;
                if (quests.containsKey(itemid)) {
                    quest = quests.get(itemid);
                }
                if (type == DropType.Mob) {
                    if (!questdrops.contains(Integer.valueOf(itemid))) {
                        questdrops.add(Integer.valueOf(itemid));
                    }
                    if (itemid == 1002357
                            || itemid == 4003004
                            || itemid == 4003005
                            || itemid / 10000 == 403
                            || itemid / 1000 == 4000
                            || itemid / 1000 == 4001
                            || itemid == 2210006
                            || itemid >= 2000000 && itemid < 2000004
                            || itemid >= 2000006 && itemid < 2000011
                            || (id2 >= 9200000 && id2 <= 9300136)) {
                        if (itemid == 0) {
                            min = ((int) (money * 0.75));
                            max = (money);
                        }
                        if (itemid == 4031279) {
                            min = 5;
                            max = 10;
                        } else if (itemid == 4031146) { //after B.B 윈스턴의 화석발굴
                            id2 = 2230102;
                        } else if (itemid == 4031147) {
                            id2 = 1140100;
                        } else if (itemid == 4031013) {
                            prob = 800000;
                        } else if (itemid == 4000021) { //동물의 가죽
                            prob = 50000;
                        } else if (itemid == 4003004) { //뻣뻣한 깃털
                            prob = 100000;
                        } else if (itemid == 4003005) { //부드러운 깃털
                            prob = 50000;
                        } else if (itemid == 4031212) { //싸늘한 기운
                            if (id2 == 4230100) {
                                id2 = 6230600;
                            } else if (id2 == 9200013) {
                                id2 = 9500126;
                            }
                        }
                        if (itemid != 4031282) {
                            switch (itemid) {
                                case 4001107:
                                case 4001110:
                                case 4001111:
                                case 4001112:
                                    continue;
                            }
                            switch (id2) {
                                case 6300001:
                                case 6300002:
                                case 6400001:
                                case 6400002:
                                case 6230201:
                                case 6130102: {
                                    continue;
                                }
                                default: {
                                    if (itemid != 4031282) {
                                        if (itemid >= 4001120 && itemid <= 4001122) {
                                            prob = 500000;
                                        }
                                        if (id2 == 8810018 && itemid == 2000006) {
                                        } else {
                                            mobdrops.add(new MobDropEntry(id2, itemid, prob, min, max, quest));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    boolean drop = true;
                    if (id2 == 8810018 //혼테일
                            || id2 == 8510000
                            || id2 == 8520000 //피아누스
                            || id2 == 9400513) {
                        if (itemid == 0) {
                            min = ((int) (money * 0.75));
                            max = (money);
                        }
                        if (id2 == 8810018 && (itemid >= 2000000 && itemid <= 2030010)) {
                            drop = false;
                        }
                        if (drop) {
                            mobdrops.add(new MobDropEntry(id2, itemid, prob, min, max, quest));
                        }
                    }
                } else {
                    if (!questdrops.contains(Integer.valueOf(itemid))) {
                        questdrops.add(Integer.valueOf(itemid));
                    }
                    if (itemid == 0) {
                        min = ((int) (money * 0.75));
                        max = (money);
                    }
                    if (itemid >= 4001095 && itemid <= 4001100) {
                        itemid = 4001453;
                        prob = 400000;
                    }
                    reactordrops.add(new ReactorDropEntry(id, itemid, prob, min, max, quest));
                }
            }
        }

        //Hardcode Drop Data
        List<int[]> datas = new ArrayList<int[]>();
        //mobid, itemid, min, max, quest, prob

        datas.add(new int[]{8800002, 1372049, 1, 1, 0, 1000000});//자쿰
        datas.add(new int[]{8800002, 1372049, 1, 1, 0, 800000});//자쿰
        datas.add(new int[]{8800002, 1372049, 1, 1, 0, 300000});//자쿰
        datas.add(new int[]{8800002, 1372049, 1, 1, 0, 100000});//자쿰

        //datas.add(new int[]{9300001, 4001007, 1, 1, 0, 600000});//리게이터
        //datas.add(new int[]{9300003, 4001008, 1, 1, 0, 1000000});//통행증
        //마노
        datas.add(new int[]{2220000, 2210006, 1, 1, 0, 1000000});
        datas.add(new int[]{2220000, 2210006, 1, 1, 0, 1000000});
        datas.add(new int[]{2220000, 2210006, 1, 1, 0, 1000000});

        //리게이터
        datas.add(new int[]{3110100, 4031405, 1, 1, 4207, 10000});

        //리게이터
        datas.add(new int[]{3110102, 4032734, 1, 1, 3208, 50000});

        //에델슈타인
        //새싹 화분
        datas.add(new int[]{150000, 0, 8, 12, 0, 700000});//메소
        datas.add(new int[]{150000, 4000595, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{150000, 1442006, 1, 1, 0, 350});//아이언볼
        datas.add(new int[]{150000, 1412001, 1, 1, 0, 350});//쇠도끼
        datas.add(new int[]{150000, 1422000, 1, 1, 0, 350});//나무망치
        datas.add(new int[]{150000, 1041064, 1, 1, 0, 350});//블루베리 스퀘이머
        datas.add(new int[]{150000, 1002075, 1, 1, 0, 350});//붉은색 낡은 고깔 모자
        datas.add(new int[]{150000, 1462001, 1, 1, 0, 350});//석궁
        datas.add(new int[]{150000, 1040032, 1, 1, 0, 350});//홍몽 조끼
        datas.add(new int[]{150000, 1061031, 1, 1, 0, 350});//흑몽 바지(여)
        datas.add(new int[]{150000, 2000000, 1, 5, 0, 1000});//빨간 포션
        datas.add(new int[]{150000, 2000001, 1, 5, 0, 1000});//주황 포션
        datas.add(new int[]{150000, 2000003, 1, 5, 0, 1000});//파란 포션
        datas.add(new int[]{150000, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{150000, 2061000, 20, 30, 0, 8000});//석궁전용
        /*datas.add(new int[]{150000, 4003004, 1, 1, 0, 600});
         datas.add(new int[]{150000, 4020002, 1, 1, 0, 150});
         datas.add(new int[]{150000, 4020005, 1, 1, 0, 150});*/

        //나팔꽃 화분
        datas.add(new int[]{150001, 0, 9, 15, 0, 700000});//메소
        datas.add(new int[]{150001, 4000596, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{150001, 1002053, 1, 1, 0, 350});//초록색 가죽 모자
        datas.add(new int[]{150001, 1082002, 1, 1, 0, 350});//노가다 목장갑
        datas.add(new int[]{150001, 1041012, 1, 1, 0, 350});//빨간 줄무늬 티셔츠
        datas.add(new int[]{150001, 1041064, 1, 1, 0, 350});//블루베리 스퀘이머
        datas.add(new int[]{150001, 1322002, 1, 1, 0, 350});//강철 메이스
        datas.add(new int[]{150001, 1452002, 1, 1, 0, 350});//워 보우
        datas.add(new int[]{150001, 1452003, 1, 1, 0, 350});//합금 활
        datas.add(new int[]{150001, 1002125, 1, 1, 0, 350});//검정색 좀도둑 털모자
        datas.add(new int[]{150001, 1002126, 1, 1, 0, 350});//초록색 좀도둑 털모자
        datas.add(new int[]{150001, 1482000, 1, 1, 0, 350});//스틸너클
        datas.add(new int[]{3400006, 2049301, 1, 1, 0, 70});//장비강화주문서
        datas.add(new int[]{3400006, 2049401, 1, 1, 0, 70});//잠재능력부여주문서
        datas.add(new int[]{150001, 2000000, 1, 5, 0, 1000});//빨간 포션
        datas.add(new int[]{150001, 2000001, 1, 5, 0, 1000});//주황 포션
        datas.add(new int[]{150001, 2000003, 1, 5, 0, 1000});//파란 포션
        datas.add(new int[]{150001, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{150001, 2061000, 20, 30, 0, 8000});//석궁전용

        //포도주스병
        datas.add(new int[]{150002, 0, 14, 21, 0, 700000});//메소
        datas.add(new int[]{150002, 4000597, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{150002, 1072042, 1, 1, 0, 350});//검정색 고무신
        datas.add(new int[]{150002, 1002134, 1, 1, 0, 350});//빨간색 물개모자
        datas.add(new int[]{150002, 1002069, 1, 1, 0, 350});//파란색 머리띠
        datas.add(new int[]{150002, 1372006, 1, 1, 0, 350});//하드우드 완드
        datas.add(new int[]{150002, 1402018, 1, 1, 0, 350});//환목검
        datas.add(new int[]{150002, 1062002, 1, 1, 0, 350});//갈색 하드래더 바지
        datas.add(new int[]{150002, 1472002, 1, 1, 0, 350});//미스릴 티탄즈
        datas.add(new int[]{150002, 1492000, 1, 1, 0, 350});//피스톨
        //datas.add(new int[]{150002, 2049100, 1, 1, 0, 30});//혼돈의 주문서
        datas.add(new int[]{150002, 2000000, 1, 5, 0, 1000});//빨간 포션
        datas.add(new int[]{150002, 2000001, 1, 5, 0, 1000});//주황 포션
        datas.add(new int[]{150002, 2000003, 1, 5, 0, 1000});//파란 포션
        datas.add(new int[]{150002, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{150002, 2061000, 20, 30, 0, 8000});//석궁전용

        //순찰로봇
        datas.add(new int[]{1150000, 0, 22, 33, 0, 700000});//메소
        datas.add(new int[]{1150000, 4000598, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{1150000, 1050007, 1, 1, 0, 350});//흰색 검도복
        datas.add(new int[]{1150000, 1302002, 1, 1, 0, 350});//바이킹 소드
        datas.add(new int[]{1150000, 1002058, 1, 1, 0, 350});//미스릴 바이킹 헬름
        datas.add(new int[]{1150000, 1041029, 1, 1, 0, 350});//검은색 가람
        datas.add(new int[]{1150000, 1050025, 1, 1, 0, 350});//흰색 도로스 로브
        datas.add(new int[]{1150000, 1462002, 1, 1, 0, 350});//전투 석궁
        datas.add(new int[]{1150000, 1472004, 1, 1, 0, 350});//브론즈 이고르
        datas.add(new int[]{1150000, 1472005, 1, 1, 0, 350});//스틸 이고르
        datas.add(new int[]{1150000, 1002613, 1, 1, 0, 350});//브라운 래거 캡
        datas.add(new int[]{1150000, 2040901, 1, 1, 0, 30});//방패 방어력 주문서 60
        datas.add(new int[]{1150000, 2048001, 1, 1, 0, 70});//펫장비 이동속도 주문서 60
        datas.add(new int[]{1150000, 2040625, 1, 1, 0, 70});//하의 민첩성 주문서 60
        //datas.add(new int[]{1150000, 2049100, 1, 1, 0, 70});//혼돈의 주문서
        datas.add(new int[]{1150000, 2000000, 1, 5, 0, 1000});//빨간 포션
        datas.add(new int[]{1150000, 2000001, 1, 5, 0, 1000});//주황 포션
        datas.add(new int[]{1150000, 2000003, 1, 5, 0, 1000});//파란 포션
        datas.add(new int[]{1150000, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{1150000, 2061000, 20, 30, 0, 8000});//석궁전용

        //이상한 이정표
        datas.add(new int[]{1150001, 0, 25, 34, 0, 700000});//메소
        datas.add(new int[]{1150001, 4000599, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{1150001, 1050005, 1, 1, 0, 350});//파란색 검도복
        datas.add(new int[]{1150001, 1061016, 1, 1, 0, 350});//레드 라멜 치마
        datas.add(new int[]{1150001, 1302005, 1, 1, 0, 350});//사브르
        datas.add(new int[]{1150001, 1382004, 1, 1, 0, 350});//고목나무 스태프
        datas.add(new int[]{1150001, 1382005, 1, 1, 0, 350});//에메랄드 스태프
        datas.add(new int[]{1150001, 1041062, 1, 1, 0, 350});//옐로우 아벨 래더아머
        datas.add(new int[]{1150001, 1060033, 1, 1, 0, 350});//검정색 파오 바지
        //datas.add(new int[]{1150001, 1060067, 1, 1, 0, 350});//유치원 바지
        datas.add(new int[]{1150001, 1472003, 1, 1, 0, 350});//골드 티탄즈
        datas.add(new int[]{1150001, 2040901, 1, 1, 0, 30});//방패 방어력 주문서 60
        datas.add(new int[]{1150001, 2048001, 1, 1, 0, 70});//펫장비 이동속도 주문서 60
        datas.add(new int[]{1150001, 2040625, 1, 1, 0, 70});//하의 민첩성 주문서 60
        //datas.add(new int[]{1150001, 2049100, 1, 1, 0, 70});//혼돈의 주문서
        datas.add(new int[]{1150001, 2000000, 1, 5, 0, 1000});//빨간 포션
        datas.add(new int[]{1150001, 2000001, 1, 5, 0, 1000});//주황 포션
        datas.add(new int[]{1150001, 2000003, 1, 5, 0, 1000});//파란 포션
        datas.add(new int[]{1150001, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{1150001, 2061000, 20, 30, 0, 8000});//석궁전용     

        //구렁이
        datas.add(new int[]{1150002, 0, 28, 42, 0, 700000});//메소
        datas.add(new int[]{1150002, 4000600, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{1150002, 1062000, 1, 1, 0, 350});//아이스 진
        datas.add(new int[]{1150002, 1442007, 1, 1, 0, 350});//팬텀
        datas.add(new int[]{1150002, 1312001, 1, 1, 0, 350});//전투 도끼
        datas.add(new int[]{1150002, 1002102, 1, 1, 0, 350});//블루문 고깔모자
        datas.add(new int[]{1150002, 1041026, 1, 1, 0, 350});//옐로우 아리안느
        datas.add(new int[]{1150002, 1061025, 1, 1, 0, 350});//레드 쉬버메일 치마
        datas.add(new int[]{1150002, 1040034, 1, 1, 0, 350});//흑야
        datas.add(new int[]{1150002, 1332002, 1, 1, 0, 350});//삼지 자마다르
        datas.add(new int[]{1150002, 1492002, 1, 1, 0, 350});//개런드 피스톨
        datas.add(new int[]{1150002, 2043001, 1, 1, 0, 30});//한손검 공격력 주문서 60
        datas.add(new int[]{1150002, 2044401, 1, 1, 0, 70});//폴암 공격력 60
        datas.add(new int[]{1150002, 2040516, 1, 1, 0, 70});//전신갑옷 행운 60
        datas.add(new int[]{3400006, 2049301, 1, 1, 0, 70});//장비강화주문서
        datas.add(new int[]{3400006, 2049401, 1, 1, 0, 70});//잠재능력부여주문서
        datas.add(new int[]{1150002, 2000000, 1, 5, 0, 1000});//빨간 포션
        datas.add(new int[]{1150002, 2000001, 1, 5, 0, 1000});//주황 포션
        datas.add(new int[]{1150002, 2000003, 1, 5, 0, 1000});//파란 포션
        datas.add(new int[]{1150002, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{1150002, 2061000, 20, 30, 0, 8000});//석궁전용

        datas.add(new int[]{9300414, 4032745, 1, 1, 0, 1000000});//블랙윙 훈장

        datas.add(new int[]{2150002, 4032746, 1, 1, 0, 50000});//가로등의 철제

        datas.add(new int[]{9001031, 4032737, 1, 1, 23023, 1000000});//블랙윙의 보고서
        datas.add(new int[]{9001031, 4032738, 1, 1, 23024, 1000000});//블랙윙의 보고서
        datas.add(new int[]{9001031, 4032739, 1, 1, 23025, 1000000});//블랙윙의 보고서

        datas.add(new int[]{3150000, 4032764, 1, 1, 23941, 600000});//루 건전지
        datas.add(new int[]{3150001, 4032765, 1, 1, 23943, 50000});//돌 방패
        datas.add(new int[]{7150002, 4032770, 1, 1, 23959, 400000});//곰의 고기
        datas.add(new int[]{7150003, 4032771, 1, 1, 23964, 50000});//라쿤의 심장
        datas.add(new int[]{8105000, 4032772, 1, 1, 23965, 50000});//라칸의 심장
        datas.add(new int[]{8105001, 4032782, 1, 1, 23971, 30000});//카드키
        datas.add(new int[]{8105005, 4032776, 1, 1, 23976, 150000});//세포샘플

        datas.add(new int[]{6090000, 4000633, 1, 1, 0, 300000});//리치 

        //PQ
        //킹슬라임(35제 메이플 장비)
        datas.add(new int[]{9300003, 1092030, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1302020, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1382009, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1452016, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1462014, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1472030, 1, 1, 0, 10000});
        datas.add(new int[]{9300003, 1002416, 1, 1, 0, 5000});//슬라임 모자
        datas.add(new int[]{9300003, 1002296, 1, 1, 0, 5000});//슬라임모자
        datas.add(new int[]{9300003, 1072369, 1, 1, 0, 100000});//물신

        //알리샤르(43제 메이플 장비)
        datas.add(new int[]{9300012, 1302030, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1332025, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1382012, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1412011, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1422014, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1432012, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1442024, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1452022, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1462019, 1, 1, 0, 10000});
        datas.add(new int[]{9300012, 1472032, 1, 1, 0, 10000});

        //파파픽시(64제 메이플 장비)
        datas.add(new int[]{9300039, 1092045, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1092046, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1092047, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1302064, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1312032, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1322054, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1332055, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1332056, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1372034, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1382039, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1402039, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1412027, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1422029, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1432040, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1442051, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1452045, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1462040, 1, 1, 0, 20000});
        datas.add(new int[]{9300039, 1472055, 1, 1, 0, 20000});

        //포이즌 골렘
        datas.add(new int[]{9300182, 4001164, 1, 1, 0, 1000000});

        /*데비존 부화기*/
        datas.add(new int[]{9300119, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300119, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300119, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300105, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300105, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300105, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300106, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300106, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300106, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300107, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300107, 5060002, 1, 1, 0, 50000});
        datas.add(new int[]{9300107, 5060002, 1, 1, 0, 50000});

        datas.add(new int[]{9300018, 4000142, 1, 1, 1018, 1000000});

        datas.add(new int[]{9300394, 1012161, 1, 1, 0, 100000});//빛나는 코
        datas.add(new int[]{9300394, 4032529, 1, 1, 0, 100000});//빨간코 선물상자

        /*피라미드*/
        //파라오예티
        datas.add(new int[]{9700019, 2022613, 1, 1, 0, 1000000});//상자
        datas.add(new int[]{9700029, 2022618, 1, 1, 0, 1000000});//상자
        /*지하철*/
        datas.add(new int[]{9700020, 2022615, 1, 1, 0, 1000000});//상자

        /*유령선*/
        //해군 이등병 유령
        datas.add(new int[]{9700030, 2049200, 1, 1, 0, 100});//힘 70
        datas.add(new int[]{9700030, 2049201, 1, 1, 0, 100});//힘 30
        datas.add(new int[]{9700030, 2049202, 1, 1, 0, 100});//민첩성 70
        datas.add(new int[]{9700030, 2049203, 1, 1, 0, 100});//민첩성 30
        datas.add(new int[]{9700030, 2049204, 1, 1, 0, 100});//지력 70
        datas.add(new int[]{9700030, 2049205, 1, 1, 0, 100});//지력 30
        datas.add(new int[]{9700030, 2049206, 1, 1, 0, 100});//행운 70
        datas.add(new int[]{9700030, 2049207, 1, 1, 0, 100});//행운 30
        datas.add(new int[]{9700030, 2049208, 1, 1, 0, 500});//체력 70
        datas.add(new int[]{9700030, 2049209, 1, 1, 0, 500});//체력 30
        datas.add(new int[]{9700030, 2049210, 1, 1, 0, 500});//마나 70
        datas.add(new int[]{9700030, 2049211, 1, 1, 0, 500});//마나 30
        datas.add(new int[]{9700030, 0, 900, 1100, 0, 600000});//메소
        datas.add(new int[]{9700030, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700030, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700030, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700030, 2022003, 1, 1, 0, 100000});//장어구이
        datas.add(new int[]{9700030, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700030, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700030, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700030, 2022003, 1, 1, 0, 100000});//장어구이

        //해군 일등병 유령
        datas.add(new int[]{9700031, 2049200, 1, 1, 0, 100});//힘 70
        datas.add(new int[]{9700031, 2049201, 1, 1, 0, 100});//힘 30
        datas.add(new int[]{9700031, 2049202, 1, 1, 0, 100});//민첩성 70
        datas.add(new int[]{9700031, 2049203, 1, 1, 0, 100});//민첩성 30
        datas.add(new int[]{9700031, 2049204, 1, 1, 0, 100});//지력 70
        datas.add(new int[]{9700031, 2049205, 1, 1, 0, 100});//지력 30
        datas.add(new int[]{9700031, 2049206, 1, 1, 0, 100});//행운 70
        datas.add(new int[]{9700031, 2049207, 1, 1, 0, 100});//행운 30
        datas.add(new int[]{9700031, 2049208, 1, 1, 0, 500});//체력 70
        datas.add(new int[]{9700031, 2049209, 1, 1, 0, 500});//체력 30
        datas.add(new int[]{9700031, 2049210, 1, 1, 0, 500});//마나 70
        datas.add(new int[]{9700031, 2049211, 1, 1, 0, 500});//마나 30
        datas.add(new int[]{9700031, 0, 900, 1100, 0, 600000});//메소
        datas.add(new int[]{9700031, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700031, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700031, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700031, 2022003, 1, 1, 0, 100000});//장어구이
        datas.add(new int[]{9700031, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700031, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700031, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700031, 2022003, 1, 1, 0, 100000});//장어구이

        //해군 상병 유령
        datas.add(new int[]{9700032, 2049200, 1, 1, 0, 100});//힘 70
        datas.add(new int[]{9700032, 2049201, 1, 1, 0, 100});//힘 30
        datas.add(new int[]{9700032, 2049202, 1, 1, 0, 100});//민첩성 70
        datas.add(new int[]{9700032, 2049203, 1, 1, 0, 100});//민첩성 30
        datas.add(new int[]{9700032, 2049204, 1, 1, 0, 100});//지력 70
        datas.add(new int[]{9700032, 2049205, 1, 1, 0, 100});//지력 30
        datas.add(new int[]{9700032, 2049206, 1, 1, 0, 100});//행운 70
        datas.add(new int[]{9700032, 2049207, 1, 1, 0, 100});//행운 30
        datas.add(new int[]{9700032, 2049208, 1, 1, 0, 500});//체력 70
        datas.add(new int[]{9700032, 2049209, 1, 1, 0, 500});//체력 30
        datas.add(new int[]{9700032, 2049210, 1, 1, 0, 500});//마나 70
        datas.add(new int[]{9700032, 2049211, 1, 1, 0, 500});//마나 30
        datas.add(new int[]{9700032, 0, 900, 1100, 0, 600000});//메소
        datas.add(new int[]{9700032, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700032, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700032, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700032, 2022003, 1, 1, 0, 100000});//장어구이
        datas.add(new int[]{9700032, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700032, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700032, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700032, 2022003, 1, 1, 0, 100000});//장어구이

        //해군 병장 유령
        datas.add(new int[]{9700033, 2049200, 1, 1, 0, 100});//힘 70
        datas.add(new int[]{9700033, 2049201, 1, 1, 0, 100});//힘 30
        datas.add(new int[]{9700033, 2049202, 1, 1, 0, 100});//민첩성 70
        datas.add(new int[]{9700033, 2049203, 1, 1, 0, 100});//민첩성 30
        datas.add(new int[]{9700033, 2049204, 1, 1, 0, 100});//지력 70
        datas.add(new int[]{9700033, 2049205, 1, 1, 0, 100});//지력 30
        datas.add(new int[]{9700033, 2049206, 1, 1, 0, 100});//행운 70
        datas.add(new int[]{9700033, 2049207, 1, 1, 0, 100});//행운 30
        datas.add(new int[]{9700033, 2049208, 1, 1, 0, 500});//체력 70
        datas.add(new int[]{9700033, 2049209, 1, 1, 0, 500});//체력 30
        datas.add(new int[]{9700033, 2049210, 1, 1, 0, 500});//마나 70
        datas.add(new int[]{9700033, 2049211, 1, 1, 0, 500});//마나 30
        datas.add(new int[]{9700033, 0, 900, 1100, 0, 600000});//메소
        datas.add(new int[]{9700033, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700033, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700033, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700033, 2022003, 1, 1, 0, 100000});//장어구이
        datas.add(new int[]{9700033, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700033, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700033, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700033, 2022003, 1, 1, 0, 100000});//장어구이

        //해군 하사 유령
        datas.add(new int[]{9700034, 2049200, 1, 1, 0, 100});//힘 70
        datas.add(new int[]{9700034, 2049201, 1, 1, 0, 100});//힘 30
        datas.add(new int[]{9700034, 2049202, 1, 1, 0, 100});//민첩성 70
        datas.add(new int[]{9700034, 2049203, 1, 1, 0, 100});//민첩성 30
        datas.add(new int[]{9700034, 2049204, 1, 1, 0, 100});//지력 70
        datas.add(new int[]{9700034, 2049205, 1, 1, 0, 100});//지력 30
        datas.add(new int[]{9700034, 2049206, 1, 1, 0, 100});//행운 70
        datas.add(new int[]{9700034, 2049207, 1, 1, 0, 100});//행운 30
        datas.add(new int[]{9700034, 2049208, 1, 1, 0, 500});//체력 70
        datas.add(new int[]{9700034, 2049209, 1, 1, 0, 500});//체력 30
        datas.add(new int[]{9700034, 2049210, 1, 1, 0, 500});//마나 70
        datas.add(new int[]{9700034, 2049211, 1, 1, 0, 500});//마나 30
        datas.add(new int[]{9700034, 0, 900, 1100, 0, 600000});//메소
        datas.add(new int[]{9700034, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700034, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700034, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700034, 2022003, 1, 1, 0, 100000});//장어구이
        datas.add(new int[]{9700034, 2000004, 1, 1, 0, 50000});//엘릭서
        datas.add(new int[]{9700034, 2000005, 1, 1, 0, 50000});//파워 엘릭서
        datas.add(new int[]{9700034, 2000006, 1, 1, 0, 100000});//마나엘릭서
        datas.add(new int[]{9700034, 2022003, 1, 1, 0, 100000});//장어구이

        //일등 항해사
        datas.add(new int[]{9700035, 2049200, 1, 1, 0, 10000});//힘 70
        datas.add(new int[]{9700035, 2049201, 1, 1, 0, 10000});//힘 30
        datas.add(new int[]{9700035, 2049202, 1, 1, 0, 10000});//민첩성 70
        datas.add(new int[]{9700035, 2049203, 1, 1, 0, 10000});//민첩성 30
        datas.add(new int[]{9700035, 2049204, 1, 1, 0, 10000});//지력 70
        datas.add(new int[]{9700035, 2049205, 1, 1, 0, 10000});//지력 30
        datas.add(new int[]{9700035, 2049206, 1, 1, 0, 10000});//행운 70
        datas.add(new int[]{9700035, 2049207, 1, 1, 0, 10000});//행운 30
        datas.add(new int[]{9700035, 2049208, 1, 1, 0, 10000});//체력 70
        datas.add(new int[]{9700035, 2049209, 1, 1, 0, 10000});//체력 30
        datas.add(new int[]{9700035, 2049210, 1, 1, 0, 10000});//마나 70
        datas.add(new int[]{9700035, 2049211, 1, 1, 0, 10000});//마나 30
        datas.add(new int[]{9700035, 0, 1500, 2000, 0, 600000});//메소
        datas.add(new int[]{9700035, 0, 1500, 2000, 0, 600000});//메소
        datas.add(new int[]{9700035, 0, 1500, 2000, 0, 600000});//메소
        datas.add(new int[]{9700035, 0, 1500, 2000, 0, 600000});//메소
        datas.add(new int[]{9700035, 0, 1500, 2000, 0, 600000});//메소
        datas.add(new int[]{9700035, 2000004, 1, 1, 0, 300000});//엘릭서
        datas.add(new int[]{9700035, 2000005, 1, 1, 0, 300000});//파워 엘릭서
        datas.add(new int[]{9700035, 2000006, 1, 1, 0, 300000});//마나엘릭서
        datas.add(new int[]{9700035, 2022003, 1, 1, 0, 300000});//장어구이
        datas.add(new int[]{9700035, 2000004, 1, 1, 0, 300000});//엘릭서
        datas.add(new int[]{9700035, 2000005, 1, 1, 0, 300000});//파워 엘릭서
        datas.add(new int[]{9700035, 2000006, 1, 1, 0, 300000});//마나엘릭서
        datas.add(new int[]{9700035, 2022003, 1, 1, 0, 300000});//장어구이
        datas.add(new int[]{9700035, 2000004, 1, 1, 0, 300000});//엘릭서
        datas.add(new int[]{9700035, 2000005, 1, 1, 0, 300000});//파워 엘릭서
        datas.add(new int[]{9700035, 2000006, 1, 1, 0, 300000});//마나엘릭서
        datas.add(new int[]{9700035, 2022003, 1, 1, 0, 300000});//장어구이
        datas.add(new int[]{9700035, 2000004, 1, 1, 0, 300000});//엘릭서
        datas.add(new int[]{9700035, 2000005, 1, 1, 0, 300000});//파워 엘릭서
        datas.add(new int[]{9700035, 2000006, 1, 1, 0, 300000});//마나엘릭서
        datas.add(new int[]{9700035, 2022003, 1, 1, 0, 300000});//장어구이
        datas.add(new int[]{9700035, 2000004, 1, 1, 0, 300000});//엘릭서
        datas.add(new int[]{9700035, 2000005, 1, 1, 0, 300000});//파워 엘릭서
        datas.add(new int[]{9700035, 2000006, 1, 1, 0, 300000});//마나엘릭서
        datas.add(new int[]{9700035, 2022003, 1, 1, 0, 300000});//장어구이
        datas.add(new int[]{9700035, 2000004, 1, 1, 0, 300000});//엘릭서
        datas.add(new int[]{9700035, 2000005, 1, 1, 0, 300000});//파워 엘릭서
        datas.add(new int[]{9700035, 2000006, 1, 1, 0, 300000});//마나엘릭서
        datas.add(new int[]{9700035, 2022003, 1, 1, 0, 300000});//장어구이

        /*황금사원*/
        //사나운 원숭이
        datas.add(new int[]{9500383, 0, 52, 70, 0, 600000});//메소
        datas.add(new int[]{9500383, 4000561, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{9500383, 2046308, 1, 1, 0, 20});//악세서리 공격력 주문서 100%
        datas.add(new int[]{9500383, 2046313, 1, 1, 0, 20});//악세서리 마력 주문서 100%
        datas.add(new int[]{9500383, 2049001, 1, 1, 0, 10});//백의 주문서 3%
        datas.add(new int[]{9500383, 2000003, 1, 1, 0, 5000});//파란포션
        datas.add(new int[]{9500383, 2000001, 1, 1, 0, 5000});//주황포션

        //어미 원숭이
        datas.add(new int[]{9500384, 0, 66, 90, 0, 600000});//메소
        datas.add(new int[]{9500384, 4000562, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{9500384, 2046308, 1, 1, 0, 20});//악세서리 공격력 주문서 100%
        datas.add(new int[]{9500384, 2046313, 1, 1, 0, 20});//악세서리 마력 주문서 100%
        datas.add(new int[]{9500384, 2049001, 1, 1, 0, 10});//백의 주문서 3%
        datas.add(new int[]{9500384, 2000003, 1, 1, 0, 5000});//파란포션
        datas.add(new int[]{9500384, 2000001, 1, 1, 0, 5000});//주황포션
        datas.add(new int[]{9500384, 4032602, 1, 1, 10583, 50000});//파란포션
        datas.add(new int[]{9500384, 4032603, 1, 1, 10583, 50000});//주황포션

        //흰털 아기 원숭이
        datas.add(new int[]{9500385, 0, 90, 120, 0, 600000});//메소
        datas.add(new int[]{9500385, 4000559, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{9500385, 2046308, 1, 1, 0, 20});//악세서리 공격력 주문서 100%
        datas.add(new int[]{9500385, 2046313, 1, 1, 0, 20});//악세서리 마력 주문서 100%
        datas.add(new int[]{9500385, 2049001, 1, 1, 0, 10});//백의 주문서 3% 
        datas.add(new int[]{9500385, 2000003, 1, 1, 0, 5000});//파란포션
        datas.add(new int[]{9500385, 2000002, 1, 1, 0, 5000});//하얀포션
        datas.add(new int[]{9500385, 2000001, 1, 1, 0, 5000});//주황포션
        datas.add(new int[]{9500385, 2000006, 1, 1, 0, 5000});//마나엘릭서

        //흰털 어미 원숭이
        datas.add(new int[]{9500386, 0, 180, 240, 0, 600000});//메소
        datas.add(new int[]{9500386, 4000560, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{9500386, 2046308, 1, 1, 0, 20});//악세서리 공격력 주문서 100%
        datas.add(new int[]{9500386, 2046313, 1, 1, 0, 20});//악세서리 마력 주문서 100%
        datas.add(new int[]{9500386, 2049001, 1, 1, 0, 10});//백의 주문서 3%
        datas.add(new int[]{9500386, 2000003, 1, 1, 0, 5000});//파란포션
        datas.add(new int[]{9500386, 2000002, 1, 1, 0, 5000});//하얀포션
        datas.add(new int[]{9500386, 2000001, 1, 1, 0, 5000});//주황포션
        datas.add(new int[]{9500386, 2000006, 1, 1, 0, 5000});//마나엘릭서
        datas.add(new int[]{9500386, 2290153, 1, 1, 0, 100});//슬래시 스톰

        //파란 도깨비
        datas.add(new int[]{9500387, 0, 240, 360, 0, 600000});//메소
        datas.add(new int[]{9500387, 4000564, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{9500387, 2046308, 1, 1, 0, 20});//악세서리 공격력 주문서 100%
        datas.add(new int[]{9500387, 2046313, 1, 1, 0, 20});//악세서리 마력 주문서 100%
        datas.add(new int[]{9500387, 2049001, 1, 1, 0, 10});//백의 주문서 3%
        datas.add(new int[]{9500387, 2000003, 1, 1, 0, 5000});//파란포션
        datas.add(new int[]{9500387, 2000002, 1, 1, 0, 5000});//하얀포션
        datas.add(new int[]{9500387, 2000001, 1, 1, 0, 5000});//주황포션
        datas.add(new int[]{9500387, 2000006, 1, 1, 0, 5000});//마나엘릭서
        datas.add(new int[]{9500387, 2290154, 1, 1, 0, 100});//토네이도 스핀
        datas.add(new int[]{9500387, 4001433, 1, 1, 0, 100});//태양의 불꽃

        //빨간 도깨비
        datas.add(new int[]{9500388, 0, 360, 500, 0, 600000});//메소
        datas.add(new int[]{9500388, 4000563, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{9500388, 2046308, 1, 1, 0, 20});//악세서리 공격력 주문서 100%
        datas.add(new int[]{9500388, 2046313, 1, 1, 0, 20});//악세서리 마력 주문서 100%
        datas.add(new int[]{9500388, 2049001, 1, 1, 0, 10});//백의 주문서 3%
        datas.add(new int[]{9500388, 2000003, 1, 1, 0, 5000});//파란포션
        datas.add(new int[]{9500388, 2000002, 1, 1, 0, 5000});//하얀포션
        datas.add(new int[]{9500388, 2000001, 1, 1, 0, 5000});//주황포션
        datas.add(new int[]{9500388, 2000006, 1, 1, 0, 5000});//마나엘릭서
        datas.add(new int[]{9500388, 2290155, 1, 1, 0, 100});//미러 이미징
        datas.add(new int[]{9500388, 4001433, 1, 1, 0, 100});//태양의 불꽃

        //힘센 돌도깨비
        datas.add(new int[]{9500389, 0, 500, 650, 0, 600000});//메소
        datas.add(new int[]{9500389, 4000565, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{9500389, 2046308, 1, 1, 0, 20});//악세서리 공격력 주문서 100%
        datas.add(new int[]{9500389, 2046313, 1, 1, 0, 20});//악세서리 마력 주문서 100%
        datas.add(new int[]{9500389, 2049001, 1, 1, 0, 10});//백의 주문서 3%
        datas.add(new int[]{9500389, 2000003, 1, 1, 0, 5000});//파란포션
        datas.add(new int[]{9500389, 2000002, 1, 1, 0, 5000});//하얀포션
        datas.add(new int[]{9500389, 2000001, 1, 1, 0, 5000});//주황포션
        datas.add(new int[]{9500389, 2000006, 1, 1, 0, 5000});//마나엘릭서
        datas.add(new int[]{9500389, 2290156, 1, 1, 0, 100});//플라잉 어썰터
        datas.add(new int[]{9500389, 4001433, 1, 1, 0, 100});//태양의 불꽃

        /*에레브*/
        //티노
        datas.add(new int[]{100120, 0, 4, 5, 0, 600000});//메소
        datas.add(new int[]{100120, 4000482, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{100120, 4020000, 1, 1, 0, 3000});//가넷의 원석
        datas.add(new int[]{100120, 4003004, 1, 1, 0, 100000});//뻣뻣한깃털
        datas.add(new int[]{100120, 2010000, 1, 1, 0, 10000});//사과
        datas.add(new int[]{100120, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100120, 2061000, 20, 30, 0, 8000});//석궁전용

        //티브
        datas.add(new int[]{100121, 0, 9, 13, 0, 600000});//메소
        datas.add(new int[]{100121, 4000483, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{100121, 4020000, 1, 1, 0, 3000});//가넷의 원석
        datas.add(new int[]{100121, 4003004, 1, 1, 0, 100000});//뻣뻣한깃털
        datas.add(new int[]{100121, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100121, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100121, 2061000, 20, 30, 0, 8000});//석궁전용        

        //티무
        datas.add(new int[]{100122, 0, 9, 18, 0, 600000});//메소
        datas.add(new int[]{100122, 4000484, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{100122, 4010002, 1, 1, 0, 3000});//아쿠아마린의 원석
        datas.add(new int[]{100122, 4020002, 1, 1, 0, 3000});//아쿠아마린의 원석
        datas.add(new int[]{100122, 4003004, 1, 1, 0, 100000});//뻣뻣한깃털
        datas.add(new int[]{100122, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100122, 1452002, 1, 1, 0, 350});//워보우
        datas.add(new int[]{100122, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100122, 2061000, 20, 30, 0, 8000});//석궁전용

        //티루
        datas.add(new int[]{100123, 0, 15, 21, 0, 600000});//메소
        datas.add(new int[]{100123, 4000485, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{100123, 4010003, 1, 1, 0, 3000});//아다만티움의 원석
        datas.add(new int[]{100123, 4003004, 1, 1, 0, 100000});//뻣뻣한깃털
        datas.add(new int[]{100123, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100123, 1452002, 1, 1, 0, 350});//워보우
        datas.add(new int[]{100123, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100123, 2061000, 20, 30, 0, 8000});//석궁전용

        //티구루
        datas.add(new int[]{100124, 0, 16, 24, 0, 600000});//메소
        datas.add(new int[]{100124, 4000486, 1, 1, 0, 600000});//기타템
        datas.add(new int[]{100124, 4010004, 1, 1, 0, 3000});//은의 원석
        datas.add(new int[]{100124, 4020004, 1, 1, 0, 3000});//아다만티움의 원석
        datas.add(new int[]{100124, 4003004, 1, 1, 0, 100000});//뻣뻣한깃털
        datas.add(new int[]{100124, 2000000, 1, 1, 0, 10000});//빨간포션
        datas.add(new int[]{100124, 1452003, 1, 1, 0, 350});//워보우
        datas.add(new int[]{100124, 2060000, 20, 30, 0, 8000});//활전용
        datas.add(new int[]{100124, 2061000, 20, 30, 0, 8000});//석궁전용

        //시험의티구루
        datas.add(new int[]{9001011, 4032096, 1, 1, 20201, 800000});//시험의 증표
        datas.add(new int[]{9001011, 4032097, 1, 1, 20202, 800000});//시험의 증표
        datas.add(new int[]{9001011, 4032098, 1, 1, 20203, 800000});//시험의 증표
        datas.add(new int[]{9001011, 4032099, 1, 1, 20204, 800000});//시험의 증표
        datas.add(new int[]{9001011, 4032100, 1, 1, 20205, 800000});//시험의 증표

        //변신술사
        datas.add(new int[]{9001009, 4032101, 1, 1, 20301, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032102, 1, 1, 20302, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032103, 1, 1, 20303, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032104, 1, 1, 20304, 1000000});//시험의 증표
        datas.add(new int[]{9001009, 4032105, 1, 1, 20305, 1000000});//시험의 증표

        //돼지의 반란
        datas.add(new int[]{1210100, 4032130, 1, 1, 20707, 50000});

        //가짜인형
        datas.add(new int[]{1210103, 4032137, 1, 1, 20711, 300000});
        //몽땅 따버리겠어!
        datas.add(new int[]{1210103, 4032139, 1, 1, 20713, 150000});

        //우드 마스크의 이상
        datas.add(new int[]{2230110, 4032146, 1, 1, 20722, 50000});

        //스톤 마스크의 이상
        datas.add(new int[]{2230111, 4032147, 1, 1, 20723, 50000});

        //뿔버섯
        datas.add(new int[]{2110200, 4032390, 1, 1, 2248, 50000});

        //좀비버섯의 신호체계3
        datas.add(new int[]{2230101, 4032399, 1, 1, 2251, 50000});
        datas.add(new int[]{2230131, 4032399, 1, 1, 2251, 50000});

        //거대 네펜데스 등장?
        datas.add(new int[]{9300347, 4032324, 1, 1, 21737, 50000});
        datas.add(new int[]{9300378, 4032324, 1, 1, 21737, 50000});

        //열쇠를 찾아라
        datas.add(new int[]{9001024, 4032326, 1, 1, 21752, 300000});

        //열쇠를 찾아라
        datas.add(new int[]{9001013, 4032339, 1, 1, 21303, 1000000});

        //포이즌 골렘
        datas.add(new int[]{9300182, 4001164, 1, 1, 0, 1000000});

        //수련의 언덕 드롭템
        datas.add(new int[]{9001020, 4000588, 1, 1, 0, 1000000});
        datas.add(new int[]{9001020, 0, 100, 300, 0, 1000000});
        datas.add(new int[]{9001021, 4000589, 1, 1, 0, 1000000});
        datas.add(new int[]{9001021, 0, 100, 300, 0, 1000000});
        datas.add(new int[]{9001022, 4000590, 1, 1, 0, 1000000});
        datas.add(new int[]{9001022, 0, 100, 300, 0, 1000000});
        datas.add(new int[]{9001023, 4000591, 1, 1, 0, 1000000});
        datas.add(new int[]{9001023, 0, 100, 300, 0, 1000000});

        MapleDataProvider mBookPro = MapleDataProviderFactory.getDataProvider(new File("imgs"));
        MapleData mBook = mBookPro.getData("MonsterBook.img");
        MapleData oldmBook = mBookPro.getData("oldMonsterBook.img");

        datas.add(new int[]{130100, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{1110101, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{1130100, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{2130100, 4030009, 1, 1, 0, 3000});
        datas.add(new int[]{1210102, 4030001, 1, 1, 0, 300});
        datas.add(new int[]{210100, 4030000, 1, 1, 0, 200});
        datas.add(new int[]{1210100, 4030011, 1, 1, 0, 600});
        datas.add(new int[]{1120100, 4030010, 1, 1, 0, 600});

        datas.add(new int[]{2100108, 4031568, 1, 1, 0, 11000});

        datas.add(new int[]{9300147, 4001132, 1, 1, 0, 350000});
        datas.add(new int[]{9300148, 4001133, 1, 1, 0, 100000});

        addFrankenroid(datas); //1.2.6에는 마가티아 X
        datas.add(new int[]{9300150, 4031796, 1, 1, 3362, 50000});
        addWorldTripMob(datas);
        addMasteryBook(datas);
        addNeoTokyo(datas);
        addAdditionalScrollDropEntry(datas);

        //중독된 스톤버그 - 독안개의 숲
        datas.add(new int[]{9300173, 4001161, 1, 1, 0, 999999});

        //버섯의 성
        datas.add(new int[]{3300003, 4001317, 1, 1, 2326, 100000});//제임스의 행방(2)
        datas.add(new int[]{3300005, 4032388, 1, 1, 2330, 300000});//결혼식 저지
        datas.add(new int[]{3300006, 4032388, 1, 1, 2330, 300000});//결혼식 저지
        datas.add(new int[]{3300007, 4032388, 1, 1, 2330, 300000});//결혼식 저지

        /*네오시티*/
        //시간여행자 앤디
        datas.add(new int[]{7130002, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{7130003, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{7130004, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{7130500, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{7130501, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{7130600, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{7130601, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{8140110, 4032511, 1, 1, 3718, 50000});
        datas.add(new int[]{8140111, 4032511, 1, 1, 3718, 50000});
        questdrops.add(4032511);

        //소녀의 스케치북
        datas.add(new int[]{7120103, 4032513, 1, 1, 3722, 50000});
        datas.add(new int[]{7120104, 4032513, 1, 1, 3722, 50000});
        datas.add(new int[]{7120105, 4032513, 1, 1, 3722, 50000});
        questdrops.add(4032513);

        //대피소 열쇠
        datas.add(new int[]{7120106, 4032514, 1, 1, 3727, 10000});
        datas.add(new int[]{7120107, 4032514, 1, 1, 3727, 10000});
        questdrops.add(4032514);

        //미사일의 잔해
        datas.add(new int[]{8220010, 4032516, 1, 1, 3735, 1000000});
        questdrops.add(4032516);

        //무언가를 보았다!
        datas.add(new int[]{8220011, 4032517, 1, 1, 3740, 1000000});
        questdrops.add(4032516);

        //진화된 오베론
        datas.add(new int[]{8220012, 4032518, 1, 1, 3743, 1000000});
        questdrops.add(4032516);

        //백초마을의 그림자 귀신 
        datas.add(new int[]{4230505, 4032908, 1, 1, 3851, 50000});//단지
        datas.add(new int[]{4230506, 4032908, 1, 1, 3851, 50000});//삼단지
        questdrops.add(4032908);

        //에반
        datas.add(new int[]{130100, 4032498, 1, 1, 22004, 600000});//스텀프
        questdrops.add(4032498);

        datas.add(new int[]{9300385, 0, 10, 50, 0, 600000});//음흉한 여우
        datas.add(new int[]{9300385, 4001339, 1, 1, 0, 600000});//음흉한 여우

        datas.add(new int[]{1210100, 4032453, 1, 1, 22503, 600000});//돼지 돼지고기
        questdrops.add(4032453);

        datas.add(new int[]{2220100, 4032459, 1, 1, 22524, 50000});//파란버섯 인형
        datas.add(new int[]{2220110, 4032459, 1, 1, 22524, 50000});//파란버섯 인형
        questdrops.add(4032459);

        datas.add(new int[]{2230101, 4032461, 1, 1, 22531, 20000});//파란버섯 인형
        datas.add(new int[]{2230131, 4032461, 1, 1, 22531, 20000});//파란버섯 인형
        questdrops.add(4032461);

        datas.add(new int[]{2230102, 4032462, 1, 1, 22532, 20000});//파란버섯 인형
        datas.add(new int[]{2230112, 4032462, 1, 1, 22532, 20000});//파란버섯 인형
        questdrops.add(4032462);

        datas.add(new int[]{3110100, 4032463, 1, 1, 22548, 10000});//파란버섯 인형
        questdrops.add(4032463);

        datas.add(new int[]{9300387, 4032466, 1, 1, 22559, 1000000});//파란버섯 인형
        questdrops.add(4032466);

        datas.add(new int[]{4220000, 4032474, 1, 1, 22404, 1000000});//세르프의 진주
        datas.add(new int[]{4220000, 4032474, 1, 1, 22405, 1000000});//세르프의 진주
        questdrops.add(4032474);

        datas.add(new int[]{8140000, 4032475, 1, 1, 22407, 250000});//라이칸스로프의 가죽
        questdrops.add(4032475);

        datas.add(new int[]{8140000, 4032504, 1, 1, 22410, 250000});//라이칸스로프의 가죽
        questdrops.add(4032504);

        //시그너스
        datas.add(new int[]{9300287, 4032120, 1, 1, 20601, 1000000});//자격의 스노우맨
        datas.add(new int[]{9300287, 4032121, 1, 1, 20602, 1000000});//자격의 스노우맨
        datas.add(new int[]{9300287, 4032122, 1, 1, 20603, 1000000});//자격의 스노우맨
        datas.add(new int[]{9300287, 4032123, 1, 1, 20604, 1000000});//자격의 스노우맨
        datas.add(new int[]{9300287, 4032124, 1, 1, 20605, 1000000});//자격의 스노우맨

        datas.add(new int[]{9300288, 4032120, 1, 1, 20601, 1000000});//자격의 크림슨 발록
        datas.add(new int[]{9300288, 4032121, 1, 1, 20602, 1000000});//자격의 크림슨 발록        
        datas.add(new int[]{9300288, 4032122, 1, 1, 20603, 1000000});//자격의 크림슨 발록
        datas.add(new int[]{9300288, 4032123, 1, 1, 20604, 1000000});//자격의 크림슨 발록
        datas.add(new int[]{9300288, 4032124, 1, 1, 20605, 1000000});//자격의 크림슨 발록

        datas.add(new int[]{9300289, 4032120, 1, 1, 20601, 1000000});//자격의 도도
        datas.add(new int[]{9300289, 4032121, 1, 1, 20602, 1000000});//자격의 도도
        datas.add(new int[]{9300289, 4032122, 1, 1, 20603, 1000000});//자격의 도도
        datas.add(new int[]{9300289, 4032123, 1, 1, 20604, 1000000});//자격의 도도
        datas.add(new int[]{9300289, 4032124, 1, 1, 20605, 1000000});//자격의 도도

        datas.add(new int[]{9300290, 4032120, 1, 1, 20601, 1000000});//자격의 릴리노흐
        datas.add(new int[]{9300290, 4032121, 1, 1, 20602, 1000000});//자격의 릴리노흐
        datas.add(new int[]{9300290, 4032122, 1, 1, 20603, 1000000});//자격의 릴리노흐
        datas.add(new int[]{9300290, 4032123, 1, 1, 20604, 1000000});//자격의 릴리노흐
        datas.add(new int[]{9300290, 4032124, 1, 1, 20605, 1000000});//자격의 릴리노흐

        datas.add(new int[]{9300291, 4032125, 1, 1, 20611, 1000000});//능력의 마뇽
        datas.add(new int[]{9300291, 4032126, 1, 1, 20612, 1000000});//능력의 마뇽
        datas.add(new int[]{9300291, 4032127, 1, 1, 20613, 1000000});//능력의 마뇽
        datas.add(new int[]{9300291, 4032128, 1, 1, 20614, 1000000});//능력의 마뇽
        datas.add(new int[]{9300291, 4032129, 1, 1, 20615, 1000000});//능력의 마뇽

        datas.add(new int[]{9300292, 4032125, 1, 1, 20611, 1000000});//능력의 그리프
        datas.add(new int[]{9300292, 4032126, 1, 1, 20612, 1000000});//능력의 그리프         
        datas.add(new int[]{9300292, 4032127, 1, 1, 20613, 1000000});//능력의 그리프 
        datas.add(new int[]{9300292, 4032128, 1, 1, 20614, 1000000});//능력의 그리프 
        datas.add(new int[]{9300292, 4032129, 1, 1, 20615, 1000000});//능력의 그리프 

        datas.add(new int[]{9300293, 4032125, 1, 1, 20611, 1000000});//능력의 레비아탄
        datas.add(new int[]{9300293, 4032126, 1, 1, 20612, 1000000});//능력의 레비아탄
        datas.add(new int[]{9300293, 4032127, 1, 1, 20613, 1000000});//능력의 레비아탄
        datas.add(new int[]{9300293, 4032128, 1, 1, 20614, 1000000});//능력의 레비아탄
        datas.add(new int[]{9300293, 4032129, 1, 1, 20615, 1000000});//능력의 레비아탄

        datas.add(new int[]{9300294, 4032125, 1, 1, 20611, 1000000});//능력의 피아누스
        datas.add(new int[]{9300294, 4032126, 1, 1, 20612, 1000000});//능력의 피아누스
        datas.add(new int[]{9300294, 4032127, 1, 1, 20613, 1000000});//능력의 피아누스
        datas.add(new int[]{9300294, 4032128, 1, 1, 20614, 1000000});//능력의 피아누스
        datas.add(new int[]{9300294, 4032129, 1, 1, 20615, 1000000});//능력의 피아누스

        //좀비 사냥
        datas.add(new int[]{5130107, 4001207, 1, 1, 0, 20000}); //검은 비늘

        //커닝스퀘어
        //예티 인형자판기
        datas.add(new int[]{3400003, 0, 86, 124, 0, 600000});
        datas.add(new int[]{3400003, 4020007, 1, 1, 0, 800});//다이아몬드 원석
        datas.add(new int[]{3400003, 4004000, 1, 1, 0, 800});//힘의 크리스탈
        datas.add(new int[]{3400003, 2000002, 1, 1, 0, 1000});//하얀포션
        datas.add(new int[]{3400003, 2000003, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400003, 1092007, 1, 1, 0, 40});//배틀쉴드
        datas.add(new int[]{3400003, 1072116, 1, 1, 0, 40});//골드문슈즈
        datas.add(new int[]{3400003, 1462006, 1, 1, 0, 40});//실버 크로우
        datas.add(new int[]{3400003, 1082069, 1, 1, 0, 40});//Mithril Scaler,
        datas.add(new int[]{3400003, 1061062, 1, 1, 0, 40});//그린 레골리스 팬츠(f)
        datas.add(new int[]{3400003, 1061063, 1, 1, 0, 40});//다크 레골리스 팬츠(f)
        datas.add(new int[]{3400003, 1061060, 1, 1, 0, 40});//레드 레골리스 팬츠(f)
        datas.add(new int[]{3400003, 1040079, 1, 1, 0, 40});//브라운 피에트?
        datas.add(new int[]{3400003, 1040061, 1, 1, 0, 40});//그린 너클베스트
        datas.add(new int[]{3400003, 2041022, 1, 1, 0, 70});//망행 60프로
        datas.add(new int[]{3400003, 2041017, 1, 1, 0, 70});//망지 10프로
        datas.add(new int[]{3400003, 4032508, 1, 1, 2273, 100000});//비밀의 레시피 퀘스트아이템

        //페페 인형자판기
        datas.add(new int[]{3400005, 0, 86, 124, 0, 600000});
        datas.add(new int[]{3400005, 4020007, 1, 1, 0, 800});//다이아몬드 원석
        datas.add(new int[]{3400005, 4004000, 1, 1, 0, 800});//힘의 크리스탈
        datas.add(new int[]{3400005, 2000002, 1, 1, 0, 1000});//하얀포션
        datas.add(new int[]{3400005, 2000003, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400005, 1092008, 1, 1, 0, 40});//배틀쉴드
        datas.add(new int[]{3400005, 1072117, 1, 1, 0, 40});//골드문슈즈
        datas.add(new int[]{3400005, 1462007, 1, 1, 0, 40});//실버 크로우
        datas.add(new int[]{3400005, 1082070, 1, 1, 0, 40});//Mithril Scaler,
        datas.add(new int[]{3400005, 1061063, 1, 1, 0, 40});//그린 레골리스 팬츠(f)
        datas.add(new int[]{3400005, 1061064, 1, 1, 0, 40});//다크 레골리스 팬츠(f)
        datas.add(new int[]{3400005, 1061061, 1, 1, 0, 40});//레드 레골리스 팬츠(f)
        datas.add(new int[]{3400005, 1040080, 1, 1, 0, 40});//브라운 피에트?
        datas.add(new int[]{3400005, 1040063, 1, 1, 0, 40});//그린 너클베스트
        datas.add(new int[]{3400005, 2041022, 1, 1, 0, 70});//망행 60프로
        datas.add(new int[]{3400005, 2041017, 1, 1, 0, 70});//망지 10프로
        datas.add(new int[]{3400005, 4032508, 1, 1, 2273, 100000});//비밀의 레시피 퀘스트아이템

        //페페 인형
        datas.add(new int[]{3400006, 0, 14, 30, 0, 600000});
        datas.add(new int[]{3400006, 4000543, 1, 1, 0, 450000});//주니어페페의 열쇠고리
        datas.add(new int[]{3400006, 4020007, 1, 1, 0, 800});//다이아몬드 원석
        datas.add(new int[]{3400006, 4004000, 1, 1, 0, 800});//힘의 크리스탈
        datas.add(new int[]{3400006, 2000002, 1, 1, 0, 1000});//하얀포션
        datas.add(new int[]{3400006, 2000003, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400006, 2000006, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400006, 2049301, 1, 1, 0, 70});//장비강화주문서
        datas.add(new int[]{3400006, 2049401, 1, 1, 0, 70});//잠재능력부여주문서
        datas.add(new int[]{3400006, 2041022, 1, 1, 0, 70});//망행 60프로
        datas.add(new int[]{3400006, 2041017, 1, 1, 0, 70});//망지 10프로
        datas.add(new int[]{3400006, 1072193, 1, 1, 0, 40});//브라운 섀도우 부츠
        datas.add(new int[]{3400006, 1432003, 1, 1, 0, 40});//나카마키
        datas.add(new int[]{3400006, 1072115, 1, 1, 0, 40});//블루문슈즈
        datas.add(new int[]{3400006, 1002141, 1, 1, 0, 40});//레드 매티
        datas.add(new int[]{3400006, 1002155, 1, 1, 0, 40});//화이트 길티언
        datas.add(new int[]{3400006, 1060062, 1, 1, 0, 40});//블루 레골러 바지
        datas.add(new int[]{3400006, 1472015, 1, 1, 0, 40});//블러드 보닌
        datas.add(new int[]{3400006, 1082076, 1, 1, 0, 40});//골드 클리브

        //예티 인형
        datas.add(new int[]{3400004, 0, 14, 30, 0, 600000});
        datas.add(new int[]{3400004, 4000542, 1, 1, 0, 450000});//주니어예티의 열쇠고리
        datas.add(new int[]{3400004, 4020007, 1, 1, 0, 800});//다이아몬드 원석
        datas.add(new int[]{3400004, 4004000, 1, 1, 0, 800});//힘의 크리스탈
        datas.add(new int[]{3400004, 2000002, 1, 1, 0, 1000});//하얀포션
        datas.add(new int[]{3400004, 2000003, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400004, 2000006, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400004, 2049301, 1, 1, 0, 70});//장비강화주문서
        datas.add(new int[]{3400004, 2049401, 1, 1, 0, 70});//잠재능력부여주문서
        datas.add(new int[]{3400004, 2041019, 1, 1, 0, 70});//망민 60프로
        datas.add(new int[]{3400004, 2041020, 1, 1, 0, 70});//망민 10프로
        datas.add(new int[]{3400004, 2041013, 1, 1, 0, 70});//망힘 60프로
        datas.add(new int[]{3400005, 1092008, 1, 1, 0, 40});//배틀쉴드
        datas.add(new int[]{3400004, 1050022, 1, 1, 0, 40});//다크 크로스 체인메일
        datas.add(new int[]{3400004, 1452006, 1, 1, 0, 40});//레드 바이퍼
        datas.add(new int[]{3400006, 1060065, 1, 1, 0, 40});//브라운 레골러 바지
        datas.add(new int[]{3400006, 1060063, 1, 1, 0, 40});//그린 레골러 바지
        datas.add(new int[]{3400004, 1002625, 1, 1, 0, 40});//블루 데네마린
        datas.add(new int[]{3400004, 1052110, 1, 1, 0, 40});//블루 브레이스룩

        //예티 인형
        datas.add(new int[]{3400008, 0, 96, 125, 0, 600000});
        datas.add(new int[]{3400008, 4000544, 1, 1, 0, 450000});//주황버섯 인형
        datas.add(new int[]{3400008, 4020007, 1, 1, 0, 800});//다이아몬드 원석
        datas.add(new int[]{3400008, 4004000, 1, 1, 0, 800});//힘의 크리스탈
        datas.add(new int[]{3400008, 2000002, 1, 1, 0, 1000});//하얀포션
        datas.add(new int[]{3400008, 2000003, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400008, 2000006, 1, 1, 0, 1000});//파란포션
        datas.add(new int[]{3400008, 2049301, 1, 1, 0, 70});//장비강화주문서
        datas.add(new int[]{3400008, 2049401, 1, 1, 0, 70});//잠재능력부여주문서
        datas.add(new int[]{3400008, 2041019, 1, 1, 0, 70});//망민 60프로
        datas.add(new int[]{3400008, 2041020, 1, 1, 0, 70});//망민 10프로
        datas.add(new int[]{3400008, 2041013, 1, 1, 0, 70});//망힘 60프로
        datas.add(new int[]{3400008, 1412004, 1, 1, 0, 40});//니암
        datas.add(new int[]{3400008, 1050000, 1, 1, 0, 40});//화이트 크로스 체인메일
        datas.add(new int[]{3400008, 1040075, 1, 1, 0, 40});//다크 레골리아
        datas.add(new int[]{3400008, 1041067, 1, 1, 0, 40});//그린 레골레아
        datas.add(new int[]{3400008, 1040079, 1, 1, 0, 40});//브라운 피에뜨
        datas.add(new int[]{3400008, 1332020, 1, 1, 0, 40});//태극부채
        datas.add(new int[]{3400008, 1052110, 1, 1, 0, 40});//Black Blue-Lines Shoes

        //마네킹시리즈 퀘스트드롭
        datas.add(new int[]{4300006, 4032506, 1, 1, 2277, 50000});
        datas.add(new int[]{4300007, 4032506, 1, 1, 2277, 50000});
        datas.add(new int[]{4300008, 4032506, 1, 1, 2277, 50000});

        //싸구려 앰프
        datas.add(new int[]{4300011, 4032509, 1, 1, 2286, 50000});

        //크리세
        //저빌
        datas.add(new int[]{5160000, 4032912, 1, 1, 31001, 30000});
        datas.add(new int[]{5160000, 4000634, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{5160000, 0, 140, 200, 0, 6000000});//메소

        //골든저빌
        datas.add(new int[]{5160001, 4000635, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{5160001, 0, 140, 200, 0, 6000000});//메소

        //스코피
        datas.add(new int[]{5160002, 4000638, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{5160002, 0, 140, 200, 0, 6000000});//메소

        //골든 스코피
        datas.add(new int[]{5160003, 4000639, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{5160003, 0, 140, 200, 0, 6000000});//메소

        //페넥
        datas.add(new int[]{5160004, 4000636, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{5160004, 0, 140, 200, 0, 6000000});//메소

        //골든 페넥
        datas.add(new int[]{6160000, 4000637, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{6160000, 0, 140, 200, 0, 6000000});//메소

//        //트라고스
//        datas.add(new int[]{5160005, 4000639, 1, 1, 0, 6000000});//??
//        datas.add(new int[]{5160005, 0, 140, 200, 0, 6000000});//메소
        //맘무트
        datas.add(new int[]{6160001, 4000640, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{6160001, 0, 140, 200, 0, 6000000});//메소

        //골든 맘무트
        datas.add(new int[]{6160002, 4000641, 1, 1, 0, 6000000});//잡템
        datas.add(new int[]{6160002, 0, 140, 200, 0, 6000000});//메소

        //크세르크세스
        datas.add(new int[]{6160003, 4032911, 1, 1, 31014, 1000000});//퀘템
        datas.add(new int[]{6160003, 2028033, 1, 1, 0, 1000000});//박스
        datas.add(new int[]{6160003, 2028034, 1, 1, 0, 1000000});//박스
        datas.add(new int[]{6160003, 2028035, 1, 1, 0, 1000000});//박스
        datas.add(new int[]{6160003, 2028036, 1, 1, 0, 1000000});//박스
        datas.add(new int[]{6160003, 2028037, 1, 1, 0, 1000000});//박스
        datas.add(new int[]{6160003, 1022114, 1, 1, 0, 50000});//안경1
        datas.add(new int[]{6160003, 1022115, 1, 1, 0, 50000});//안경2

        //기억의 흔적
        datas.add(new int[]{5130104, 4032642, 1, 1, 3117, 100000});
        datas.add(new int[]{5130104, 4032643, 1, 1, 3117, 100000});
        datas.add(new int[]{5130104, 4032644, 1, 1, 3117, 100000});
        datas.add(new int[]{5130104, 4032645, 1, 1, 3117, 100000});
        datas.add(new int[]{5130104, 4032646, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032642, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032643, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032644, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032645, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032646, 1, 1, 3117, 100000});

        //퀘스트아이디 3125 석탄가루(4032651) 추가해야함 
        datas.add(new int[]{5130104, 4032651, 1, 1, 3125, 100000});
        datas.add(new int[]{5140000, 4032651, 1, 1, 3125, 100000});
        //퀘스트아이디 3125 석탄가루(4032651) 추가해야함 
        datas.add(new int[]{5130104, 4032651, 1, 1, 3126, 100000});
        datas.add(new int[]{5140000, 4032651, 1, 1, 3126, 100000});

        //아레크의 잃어버린 비수
        datas.add(new int[]{9300276, 4032647, 1, 1, 3130, 100000});
        datas.add(new int[]{9300277, 4032647, 1, 1, 3130, 100000});
        datas.add(new int[]{9300278, 4032647, 1, 1, 3130, 100000});
        datas.add(new int[]{9300279, 4032647, 1, 1, 3130, 100000});
        datas.add(new int[]{9300280, 4032647, 1, 1, 3130, 100000});

        /*메카닉 암*/
        datas.add(new int[]{3230305, 1622000, 1, 1, 0, 40});

        datas.add(new int[]{4230121, 1622001, 1, 1, 0, 40});

        datas.add(new int[]{3110301, 1622002, 1, 1, 0, 40});
        datas.add(new int[]{5110301, 1622002, 1, 1, 0, 40});
        datas.add(new int[]{5110302, 1622002, 1, 1, 0, 40});

        datas.add(new int[]{8141000, 1622003, 1, 1, 0, 40});
        datas.add(new int[]{8141100, 1622003, 1, 1, 0, 40});
        datas.add(new int[]{7130200, 1622003, 1, 1, 0, 40});
        datas.add(new int[]{8150302, 1622003, 1, 1, 0, 40});

        datas.add(new int[]{7130100, 1622004, 1, 1, 0, 40});
        datas.add(new int[]{8141000, 1622004, 1, 1, 0, 40});
        datas.add(new int[]{8141100, 1622004, 1, 1, 0, 40});
        datas.add(new int[]{8143000, 1622004, 1, 1, 0, 40});
        datas.add(new int[]{8150302, 1622004, 1, 1, 0, 40});

        /*호브파퀘 잡템*/
        datas.add(new int[]{9300276, 4000579, 1, 1, 0, 400000});
        datas.add(new int[]{9300277, 4000580, 1, 1, 0, 400000});
        datas.add(new int[]{9300278, 4000581, 1, 1, 0, 400000});
        datas.add(new int[]{9300279, 4000582, 1, 1, 0, 400000});
        datas.add(new int[]{9300280, 4000583, 1, 1, 0, 400000});
        //호브의 메이스
        datas.add(new int[]{9300280, 4032648, 1, 1, 3128, 100000});

        datas.add(new int[]{5130104, 4032645, 1, 1, 3117, 100000});
        datas.add(new int[]{5130104, 4032646, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032642, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032643, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032644, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032645, 1, 1, 3117, 100000});
        datas.add(new int[]{5140000, 4032646, 1, 1, 3117, 100000});

        //락 스피릿
        datas.add(new int[]{4300013, 4000538, 1, 1, 2288, 1000000});

        //초보 모험가 지미 돕기
        datas.add(new int[]{1130100, 4032460, 1, 1, 22529, 50000});
        datas.add(new int[]{1110101, 4032460, 1, 1, 22529, 50000});
        datas.add(new int[]{2130100, 4032460, 1, 1, 22529, 50000});
        questdrops.add(4032460);

        //해적의시험장 - 강력한 결정
        datas.add(new int[]{9001005, 4031857, 1, 1, 0, 600000});
        datas.add(new int[]{9001006, 4031856, 1, 1, 0, 600000});
        questdrops.add(4031857);
        questdrops.add(4031856);

        //주황버섯인형
        datas.add(new int[]{9300274, 4032190, 1, 1, 20705, 30000});
        datas.add(new int[]{9300274, 4032315, 1, 1, 21711, 30000});
        questdrops.add(4032190);

        //카이린의 분신 - 검은 부적
        datas.add(new int[]{9001004, 4031059, 1, 1, 0, 1099999});

        //영웅의 별, 영웅의 펜타곤 - 해적
        datas.add(new int[]{8180001, 4031861, 1, 1, 6944, 1009999});
        datas.add(new int[]{8180000, 4031860, 1, 1, 6944, 1009999});
        questdrops.add(4031861);
        questdrops.add(4031860);

        //영웅의 별, 영웅의 펜타곤 - 듀얼블레이드
        datas.add(new int[]{8180001, 4031520, 1, 1, 50636, 1009999});
        datas.add(new int[]{8180000, 4031519, 1, 1, 50636, 1009999});
        questdrops.add(4031520);
        questdrops.add(4031519);

        //루모의 잎사귀
        //3110302, 3110303 -> 4031694 (qid 3312)
        datas.add(new int[]{3110302, 4031694, 1, 1, 3312, 100000});
        datas.add(new int[]{3110303, 4031694, 1, 1, 3312, 300000});
        questdrops.add(4031694);

        //돌의 심장
        //8140701 -> 4031872
        datas.add(new int[]{8140701, 4031872, 1, 1, 6340, 200000});
        questdrops.add(4031872);

        //단단한 가죽
        //8140700 -> 4031871
        datas.add(new int[]{8140700, 4031871, 1, 1, 6350, 200000});
        questdrops.add(4031871);

        //바이킹의 깃발
        //8141000 -> 4031873
        datas.add(new int[]{8141000, 4031873, 1, 1, 6380, 200000});
        questdrops.add(4031873);

        //바이킹의 증표
        //8141100 -> 4031874
        datas.add(new int[]{8141100, 4031874, 1, 1, 6390, 200000});
        questdrops.add(4031874);

        //4031869 파풀라투스의 열쇠
        datas.add(new int[]{8500002, 4031869, 1, 1, 6360, 999999});
        questdrops.add(4031869);

        //4031773 바짝 마른 나뭇가지
        datas.add(new int[]{130100, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{1110101, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{1130100, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{1140100, 4031773, 1, 1, 2145, 199999});
        datas.add(new int[]{2130100, 4031773, 1, 1, 2145, 199999});
        questdrops.add(4031773);

        //요괴선사 퇴치
        datas.add(new int[]{7220002, 4031789, 1, 1, 3844, 999999});
        questdrops.add(4031789);

        datas.add(new int[]{5300100, 4031925, 1, 1, 2223, 60000});
        questdrops.add(4031925);

        //카슨의 시험 퀘스트
        datas.add(new int[]{9300141, 4031698, 1, 1, 3310, 199999});
        questdrops.add(4031698);

        //파웬의 출입증 3358 6110301 4031745
        datas.add(new int[]{6110301, 4031745, 1, 1, 3358, 50000});
        questdrops.add(4031745);

        //감춰진 진실 8110300 4031737 3343
        datas.add(new int[]{8110300, 4031737, 1, 1, 3343, 1000000});
        questdrops.add(4031737);

        //검은 마법사의 마법진 3345
        datas.add(new int[]{8110300, 4031740, 1, 1, 3345, 1000000});
        questdrops.add(4031740);
        datas.add(new int[]{7110300, 4031741, 1, 1, 3345, 1000000});
        questdrops.add(4031741);

        //시약 만들기 3366 9300154 4031780 ~ 4031784
        datas.add(new int[]{9300154, 4031780, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031781, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031782, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031783, 1, 1, 3366, 200000});
        datas.add(new int[]{9300154, 4031784, 1, 1, 3366, 200000});

        //3454, 4031926 모든 그레이
        datas.add(new int[]{4230116, 4031926, 1, 1, 3454, 100000});
        datas.add(new int[]{4230117, 4031926, 1, 1, 3454, 120000});
        datas.add(new int[]{4230118, 4031926, 1, 1, 3454, 130000});
        datas.add(new int[]{4240000, 4031926, 1, 1, 3454, 190000});
        questdrops.add(4031926);

        datas.add(new int[]{4230113, 2022354, 1, 1, 3248, 200000});
        datas.add(new int[]{3230306, 2022355, 1, 1, 3248, 200000});
        questdrops.add(4031991);
        questdrops.add(2022354);
        questdrops.add(2022355);
        questdrops.add(4031992);

        //220040000 ~ 220080000 : 4031992 드롭
        datas.add(new int[]{4230114, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{3230306, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{3210207, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{4230113, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{4230115, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6130200, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6230300, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6300100, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6400100, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7140000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7160000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8141000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8141100, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8160000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6230400, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{6230500, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8140200, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8140300, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7130010, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{7130300, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8142000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8143000, 4031992, 1, 1, 0, 30000});
        datas.add(new int[]{8170000, 4031992, 1, 1, 0, 30000});

        //연우의 걱정거리
        datas.add(new int[]{3210100, 4032629, 1, 1, 2371, 250000});
        questdrops.add(4032629);

        //5100004 4031790 3642
        questdrops.add(4031790);
        datas.add(new int[]{5100004, 4031790, 1, 1, 3642, 660000});

        //7220001 4031793 3647
        questdrops.add(4031790);
        datas.add(new int[]{7220001, 4031793, 1, 1, 3647, 1000000});
        datas.add(new int[]{7220001, 4031793, 1, 1, 3647, 1000000});
        datas.add(new int[]{7220001, 4031793, 1, 1, 3647, 1000000});

        //주황버섯의 이상 조사
        datas.add(new int[]{1210102, 4032314, 1, 1, 21709, 250000});
        datas.add(new int[]{9300274, 4032314, 1, 1, 21709, 250000});
        questdrops.add(4032314);

        //몬스터 전쟁?
        datas.add(new int[]{1210100, 4032340, 1, 1, 21710, 400000});
        datas.add(new int[]{1210101, 4032340, 1, 1, 21710, 400000});
        questdrops.add(4032340);

        //좀비버섯 조사
        datas.add(new int[]{2230101, 4032321, 1, 1, 21727, 30000});
        datas.add(new int[]{2230131, 4032321, 1, 1, 21727, 30000});
        questdrops.add(4032321);

        //인형사를 퇴치하라!
        datas.add(new int[]{9300344, 4032322, 1, 1, 21731, 1000000});
        datas.add(new int[]{9300344, 0, 5000, 6000, 0, 1000000});
        questdrops.add(4032322);

        //2차전직 최고의 무기를 찾아
        datas.add(new int[]{9001012, 4032311, 1, 1, 21202, 300000});
        questdrops.add(4032311);

        datas.add(new int[]{1110130, 4032316, 1, 1, 21714, 100000});
        questdrops.add(4032316);

        datas.add(new int[]{1110130, 4032317, 1, 1, 21717, 30000});
        questdrops.add(4032317);

        datas.add(new int[]{1110130, 4032318, 1, 1, 21718, 30000});
        questdrops.add(4032318);

        datas.add(new int[]{2300100, 4032620, 1, 1, 2357, 600000});
        datas.add(new int[]{3230101, 4032620, 1, 1, 2357, 600000});
        questdrops.add(4032620);

        datas.add(new int[]{1120100, 4032622, 1, 1, 2359, 100000});
        questdrops.add(4032622);

        datas.add(new int[]{1210103, 4032621, 1, 1, 2378, 100000});
        questdrops.add(4032621);

        datas.add(new int[]{2300100, 4032623, 1, 1, 2379, 100000});
        questdrops.add(4032623);

        datas.add(new int[]{1110101, 4032624, 1, 1, 2380, 100000});
        questdrops.add(4032624);

        datas.add(new int[]{8150200, 4001402, 1, 1, 3758, 50000});//드래곤의 정수
        datas.add(new int[]{8150201, 4001402, 1, 1, 3758, 50000});
        datas.add(new int[]{7130002, 4001402, 1, 1, 3758, 50000});
        datas.add(new int[]{7130003, 4001402, 1, 1, 3758, 50000});
        datas.add(new int[]{7130500, 4001402, 1, 1, 3758, 50000});
        datas.add(new int[]{7130501, 4001402, 1, 1, 3758, 50000});
        datas.add(new int[]{8140001, 4001402, 1, 1, 3758, 50000});
        datas.add(new int[]{8140002, 4001402, 1, 1, 3758, 50000});
        questdrops.add(4001402);

        //4031846 130101 1210100 2173
        questdrops.add(4031846);
        datas.add(new int[]{130101, 4031846, 1, 1, 2173, 100000});
        datas.add(new int[]{1210100, 4031846, 1, 1, 2173, 100000});

        /*시간조각*/
        datas.add(new int[]{8220004, 4020009, 1, 1, 0, 600000});
        datas.add(new int[]{8220005, 4020009, 1, 1, 0, 1000000});
        datas.add(new int[]{8220005, 4020009, 1, 1, 0, 800000});
        datas.add(new int[]{8220006, 4020009, 1, 1, 0, 1000000});
        datas.add(new int[]{8220006, 4020009, 1, 1, 0, 800000});
        datas.add(new int[]{8220006, 4020009, 1, 1, 0, 600000});

        datas.add(new int[]{9500391, 4020009, 1, 1, 0, 500000});//라바나
        datas.add(new int[]{9500391, 4020009, 1, 1, 0, 500000});

        datas.add(new int[]{9500392, 4020009, 1, 1, 0, 1000000});
        datas.add(new int[]{9500392, 4020009, 1, 1, 0, 500000});
        datas.add(new int[]{9500392, 4020009, 1, 1, 0, 200000});


        /*라바나 투구*/
        datas.add(new int[]{9500391, 1003068, 1, 1, 0, 300000});
        datas.add(new int[]{9500391, 1003068, 1, 1, 0, 50000});

        datas.add(new int[]{9500392, 1003068, 1, 1, 0, 1000000});
        datas.add(new int[]{9500392, 1003068, 1, 1, 0, 300000});
        datas.add(new int[]{9500392, 1003068, 1, 1, 0, 50000});

        //루파 7단계
        datas.add(new int[]{9300169, 4001022, 1, 1, 0, 1000000});
        datas.add(new int[]{9300170, 4001022, 1, 1, 0, 1000000});
        datas.add(new int[]{9300171, 4001022, 1, 1, 0, 1000000});

        PreparedStatement psdd = con.prepareStatement("SELECT * FROM `drop_data_p`");
        ResultSet rsdd = psdd.executeQuery();
        while (rsdd.next()) {
            int itemid = rsdd.getInt("itemid");
            if (ii.itemExists(itemid)) {
                datas.add(new int[]{rsdd.getInt("dropperid"), itemid, 1, 1, rsdd.getInt("questid"), rsdd.getInt("chance")});
            } else {
                System.err.println("Pinkbeen item not exists : " + itemid + "(" + ii.getName(itemid) + ")");
            }
        }
        rsdd.close();
        psdd.close();

        // 해적 마북들
        //속성강화 20
        datas.add(new int[]{8510000, 2290112, 1, 1, 0, 50000});
        datas.add(new int[]{8140702, 2290112, 1, 1, 0, 6});

        //서포트 옥토퍼스 20
        datas.add(new int[]{8520000, 2290114, 1, 1, 0, 30000});
        datas.add(new int[]{8142100, 2290114, 1, 1, 0, 4});

        //어드밴스드 호밍
        datas.add(new int[]{8190005, 2290124, 1, 1, 0, 5});
        datas.add(new int[]{8510000, 2290124, 1, 1, 0, 20000});

        //래피드 파이어 20
        datas.add(new int[]{8810023, 2290117, 1, 1, 0, 7});
        datas.add(new int[]{8180000, 2290117, 1, 1, 0, 9000});

        //래피드 파이어 30
        datas.add(new int[]{8150100, 2290118, 1, 1, 0, 5});

        //에어스트라이크 20
        datas.add(new int[]{8800002, 2290115, 1, 1, 0, 90000});

        //에어스트라이크 30
        datas.add(new int[]{8810018, 2290116, 1, 1, 0, 230000});

        //마인드 컨트롤 20
        datas.add(new int[]{8500002, 2290123, 1, 1, 0, 11200});

        //배틀쉽 캐논 20
        datas.add(new int[]{8180001, 2290119, 1, 1, 0, 9000 * 2});
        datas.add(new int[]{8150302, 2290119, 1, 1, 0, 5});

        //배틀쉽 캐논 30
        datas.add(new int[]{8150300, 2290120, 1, 1, 0, 5});

        //배틀쉽 토르페도 20  2290121
        datas.add(new int[]{8500002, 2290121, 1, 1, 0, 7800});
        datas.add(new int[]{8190004, 2290121, 1, 1, 0, 6});

        //배틀쉽 토르페도 30  2290122
        datas.add(new int[]{8520000, 2290122, 1, 1, 0, 132000});
        datas.add(new int[]{8140701, 2290122, 1, 1, 0, 4});

        //마뇽 마북들
        datas.add(new int[]{8180000, 2290003, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290005, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290015, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290030, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290035, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290036, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290063, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290080, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290098, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290101, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290117, 1, 1, 0, 20000});
        datas.add(new int[]{8180000, 2290130, 1, 1, 0, 20000});

        //그리프 마북들
        datas.add(new int[]{8180001, 2290018, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290032, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290042, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290059, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290069, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290072, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290092, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290100, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290102, 1, 1, 0, 20000});
        datas.add(new int[]{8180001, 2290128, 1, 1, 0, 20000});

        //단일 보스만 드롭하는 책들 추가몹들
        datas.add(new int[]{7140000, 2290101, 1, 1, 0, 100}); //슈퍼트랜스폼20 파이렛
        datas.add(new int[]{8141100, 2290102, 1, 1, 0, 100}); //데몰리션20 기간틱바이킹
        datas.add(new int[]{5130107, 2290030, 1, 1, 0, 100}); //페럴라이즈20 쿨리좀비
        datas.add(new int[]{8142000, 2290128, 1, 1, 0, 100}); //하이 마스터리20 팬텀워치
        
        //샤프아이즈 개선
        datas.add(new int[]{8520000, 2290053, 1, 1, 0, 20000}); //샤프아이즈30 좌붕
        datas.add(new int[]{9420519, 2290053, 1, 1, 0, 80}); //샤프아이즈30 두쿠
        datas.add(new int[]{9420517, 2290014, 1, 1, 0, 150}); //가디언 스피릿20 페트리파이터

        //아인크라드
        //라구 래빗
        datas.add(new int[]{9390706, 2022163, 1, 1, 0, 1000000});
        datas.add(new int[]{9390706, 2022162, 1, 1, 0, 125000});
        datas.add(new int[]{9390706, 2022175, 1, 1, 0, 250000});
        datas.add(new int[]{9390706, 2022177, 1, 1, 0, 250000});

        //보스들
        datas.add(new int[]{9401011, 4310096, 1, 1, 0, 1000000});
        datas.add(new int[]{9401012, 4310096, 1, 1, 0, 1000000});
        datas.add(new int[]{9401012, 4310097, 1, 1, 0, 100000});
        datas.add(new int[]{9401013, 4310096, 1, 1, 0, 1000000});
        datas.add(new int[]{9401013, 4310097, 1, 1, 0, 250000});

        //헌티드 맨션
        datas.add(new int[]{9500195, 1112400, 1, 1, 0, 750});
        datas.add(new int[]{9500195, 0, 800, 1200, 0, 650000});
        datas.add(new int[]{9500196, 1112400, 1, 1, 0, 600});
        datas.add(new int[]{9500196, 0, 800, 1200, 0, 650000});

        //골드리치 황금돼지
        datas.add(new int[]{9302000, 4001255, 1, 1, 0, 40000});//단단한 알
        datas.add(new int[]{9302001, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302002, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302003, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302004, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302005, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302006, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302007, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302008, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302009, 4001255, 1, 1, 0, 40000});
        datas.add(new int[]{9302010, 4001255, 1, 1, 0, 40000});

        datas.add(new int[]{9302000, 2022503, 1, 1, 0, 200000});//빛나는 알
        datas.add(new int[]{9302001, 2022504, 1, 1, 0, 200000});
        datas.add(new int[]{9302002, 2022505, 1, 1, 0, 200000});
        datas.add(new int[]{9302003, 2022506, 1, 1, 0, 200000});
        datas.add(new int[]{9302004, 2022507, 1, 1, 0, 200000});
        datas.add(new int[]{9302005, 2022508, 1, 1, 0, 200000});
        datas.add(new int[]{9302006, 2022509, 1, 1, 0, 200000});
        datas.add(new int[]{9302007, 2022510, 1, 1, 0, 200000});
        datas.add(new int[]{9302008, 2022511, 1, 1, 0, 200000});
        datas.add(new int[]{9302009, 2022512, 1, 1, 0, 200000});
        datas.add(new int[]{9302010, 2022513, 1, 1, 0, 200000});

        datas.add(new int[]{9302000, 2022514, 1, 1, 0, 50000});//찬란한 알
        datas.add(new int[]{9302001, 2022515, 1, 1, 0, 50000});
        datas.add(new int[]{9302002, 2022516, 1, 1, 0, 50000});
        datas.add(new int[]{9302003, 2022517, 1, 1, 0, 50000});
        datas.add(new int[]{9302004, 2022518, 1, 1, 0, 50000});
        datas.add(new int[]{9302005, 2022519, 1, 1, 0, 50000});
        datas.add(new int[]{9302006, 2022520, 1, 1, 0, 50000});
        datas.add(new int[]{9302007, 2022521, 1, 1, 0, 50000});
        datas.add(new int[]{9302008, 2022522, 1, 1, 0, 50000});
        datas.add(new int[]{9302009, 2022523, 1, 1, 0, 50000});
        datas.add(new int[]{9302010, 2022524, 1, 1, 0, 50000});

        //자쿰 추가 드랍 목록
        //응축된 힘의 결정석
        datas.add(new int[]{8800002, 1012478, 1, 1, 0, 150000});
        datas.add(new int[]{8800002, 1012478, 1, 1, 0, 50000});
        //아쿠아틱 레터 눈장식
        datas.add(new int[]{8800002, 1022231, 1, 1, 0, 150000});
        datas.add(new int[]{8800002, 1022231, 1, 1, 0, 50000});

        //무한의 수리검
        datas.add(new int[]{8800002, 2070024, 1, 1, 0, 20000});

        //카오스자쿰 추가 드랍 목록
        datas.add(new int[]{8800102, 1132296, 1, 1, 0, 100000}); //분노한 자쿰의 벨트
        datas.add(new int[]{8800102, 1102871, 1, 1, 0, 200000}); //분노한 자쿰의 망토
        datas.add(new int[]{8800102, 1142503, 1, 1, 0, 200000}); //카오스 자쿰 슬레이어
        /*레드무기*/
        datas.add(new int[]{8800102, 1492194, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1462208, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1332242, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1382226, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1432182, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1482183, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1402214, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1452220, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1422156, 1, 1, 0, 120000});
        datas.add(new int[]{8800102, 1472230, 1, 1, 0, 120000});

        //혼테일 추가 드랍 목록
        //응축된 힘의 결정석
        datas.add(new int[]{8810018, 1012478, 1, 1, 0, 150000});
        datas.add(new int[]{8810018, 1012478, 1, 1, 0, 70000});

        //아쿠아틱 레터 눈장식
        datas.add(new int[]{8810018, 1022231, 1, 1, 0, 100000});

        //데아 시두스 이어링
        datas.add(new int[]{8810018, 1032241, 1, 1, 0, 80000});
        datas.add(new int[]{8810018, 1032241, 1, 1, 0, 8000});

        //물약류 수동
        datas.add(new int[]{8810018, 2020013, 30, 80, 0, 1000000}); //순록
        datas.add(new int[]{8810018, 2020015, 30, 80, 0, 1000000}); //황혼
        datas.add(new int[]{8810018, 2000004, 10, 60, 0, 1000000}); //엘릭서
        datas.add(new int[]{8810018, 2000005, 10, 50, 0, 1000000}); //파워엘릭서

        //메용30
        datas.add(new int[]{8810018, 2290125, 1, 1, 0, 10000});

        //무한의 수리검
        datas.add(new int[]{8810018, 2070024, 1, 1, 0, 100000});

        //캡틴 라타니카 추가 드랍 목록
        datas.add(new int[]{9420513, 1372035, 1, 1, 0, 30000});
        datas.add(new int[]{9420513, 1372037, 1, 1, 0, 30000});
        datas.add(new int[]{9420513, 2331000, 1, 1, 0, 10000});
        datas.add(new int[]{9420513, 2332000, 1, 1, 0, 10000});
        datas.add(new int[]{9420513, 4000384, 1, 1, 0, 1000000});

        //크렉셀 추가 드랍 목록
        datas.add(new int[]{9420522, 1152134, 1, 1, 0, 60000});
        datas.add(new int[]{9420522, 1482046, 1, 1, 0, 20000});
        datas.add(new int[]{9420522, 1492048, 1, 1, 0, 20000});
        datas.add(new int[]{9420522, 2049100, 1, 1, 0, 20000});
        datas.add(new int[]{9420522, 2070024, 1, 1, 0, 25000});
        datas.add(new int[]{9420522, 2280007, 1, 1, 0, 50000});
        datas.add(new int[]{9420522, 2280008, 1, 1, 0, 50000});
        datas.add(new int[]{9420522, 2280009, 1, 1, 0, 50000});
        datas.add(new int[]{9420522, 2280010, 1, 1, 0, 50000});
        datas.add(new int[]{9420522, 2280030, 1, 1, 0, 50000});
        datas.add(new int[]{9420522, 2290122, 1, 1, 0, 33000});
        datas.add(new int[]{9420522, 2331000, 1, 1, 0, 20000});
        datas.add(new int[]{9420522, 2332000, 1, 1, 0, 20000});
        datas.add(new int[]{9420522, 1382016, 1, 1, 0, 20000});
        datas.add(new int[]{9420522, 1382011, 1, 1, 0, 20000});

        //피아누스 추가 드랍 목록
        datas.add(new int[]{8510000, 2290122, 1, 1, 0, 35000});

        //블레이드 공격력 주문서 60%
        datas.add(new int[]{7130102, 5530015, 1, 1, 0, 50});
        datas.add(new int[]{8140103, 5530015, 1, 1, 0, 50});
        datas.add(new int[]{9400003, 5530015, 1, 1, 0, 50});
        datas.add(new int[]{9420535, 5530015, 1, 1, 0, 50});
        datas.add(new int[]{9420539, 5530015, 1, 1, 0, 50});

        //블레이드 제작의 촉진제
        datas.add(new int[]{2100103, 4130023, 1, 1, 0, 500});
        datas.add(new int[]{5120502, 4130023, 1, 1, 0, 500});
        datas.add(new int[]{5130100, 4130023, 1, 1, 0, 500});
        datas.add(new int[]{6120000, 4130023, 1, 1, 0, 500});
        datas.add(new int[]{9400000, 4130023, 1, 1, 0, 500});
        datas.add(new int[]{9420519, 4130023, 1, 1, 0, 500});

        //타르가/스카라이온 보스 추가 드랍 목록
        datas.add(new int[]{9420544, 1002926, 1, 1, 0, 1000000});
        datas.add(new int[]{9420544, 1002926, 1, 1, 0, 200000});
        datas.add(new int[]{9420544, 2040815, 1, 1, 0, 30000});
        datas.add(new int[]{9420549, 1002927, 1, 1, 0, 1000000});
        datas.add(new int[]{9420549, 1002927, 1, 1, 0, 200000});
        datas.add(new int[]{9420549, 2040815, 1, 1, 0, 30000});

        //여두목 추가 드랍 목록
        datas.add(new int[]{9400121, 1332029, 1, 1, 0, 20000});
        datas.add(new int[]{9400121, 1382048, 1, 1, 0, 20000});
        datas.add(new int[]{9400121, 1032026, 1, 1, 0, 50000});
        datas.add(new int[]{9400121, 1452026, 1, 1, 0, 20000});
        datas.add(new int[]{9400121, 1302026, 1, 1, 0, 20000});
        datas.add(new int[]{9400121, 2046309, 1, 1, 0, 10000});

        //대두목 추가 드랍 목록
        datas.add(new int[]{9400300, 1442030, 1, 1, 0, 70000});
        datas.add(new int[]{9400300, 1432018, 1, 1, 0, 70000});
        datas.add(new int[]{9400300, 1402037, 1, 1, 0, 70000});
        datas.add(new int[]{9400300, 1302026, 1, 1, 0, 70000});
        datas.add(new int[]{9400300, 2046309, 1, 1, 0, 1000000});
        datas.add(new int[]{9400300, 2046310, 1, 1, 0, 60000});
        datas.add(new int[]{9400300, 2070024, 1, 1, 0, 100000});
        datas.add(new int[]{9400300, 1129999, 1, 1, 0, 1000000});

        //무림요승 추가 드랍 목록
        datas.add(new int[]{9600025, 1402037, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1432018, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1442057, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1332029, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1302026, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1382046, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1382048, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1452026, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1462022, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1472054, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1492048, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 1482046, 1, 1, 0, 110000});
        datas.add(new int[]{9600025, 2290125, 1, 1, 0, 3334});

        //마스터 미라클 큐브 교환권 
        datas.add(new int[]{9420544, 2439994, 4, 4, 0, 300000});
        datas.add(new int[]{9420549, 2439994, 4, 4, 0, 300000});
        datas.add(new int[]{8800102, 2439994, 8, 8, 0, 1000000});
        datas.add(new int[]{8810018, 2439994, 6, 6, 0, 500000});
        datas.add(new int[]{8810118, 2439994, 10, 10, 0, 1000000});

        for (int[] i : datas) {
            mobdrops.add(new MobDropEntry(i[0], i[1], i[5], i[2], i[3], i[4]));
        }

        //리액터 하드코딩
        List<int[]> datas_r = new ArrayList<int[]>();

        datas_r.add(new int[]{2612004, 4031703, 999999, 3302, 1, 1});
        datas_r.add(new int[]{8042000, 4039997, 1000000, 50604, 1, 1});
        datas_r.add(new int[]{8042000, 4039998, 1000000, 50604, 1, 1});

        int[] normal_scrolls = new int[]{2040001, 2040002, 2040004, 2040005, 2040025, 2040026, 2040029, 2040031, 2040301, 2040302,
            2040317, 2040318, 2040321, 2040323, 2040326, 2040328, 2040401, 2040402, 2040418, 2040419,
            2040421, 2040422, 2040425, 2040427, 2040501, 2040502, 2040504, 2040505, 2040513, 2040514,
            2040516, 2040517, 2040532, 2040534, 2040601, 2040602, 2040618, 2040619, 2040621, 2040622,
            2040625, 2040627, 2040701, 2040702, 2040704, 2040705, 2040707, 2040708, 2040801, 2040802,
            2040804, 2040805, 2040824, 2040825, 2040901, 2040902, 2040924, 2040925, 2040927, 2040928,
            2040931, 2040933, 2041001, 2041002, 2041004, 2041005, 2041007, 2041008, 2041010, 2041011,
            2041013, 2041014, 2041016, 2041017, 2041019, 2041020, 2041022, 2041023, 2043001, 2043002,
            2043017, 2043019, 2043101, 2043102, 2043112, 2043114, 2043201, 2043202, 2043212, 2043214,
            2043301, 2043302, 2043701, 2043702, 2043801, 2043802, 2044001, 2044002, 2044012, 2044014,
            2044101, 2044102, 2044112, 2044114, 2044201, 2044202, 2044212, 2044214, 2044301, 2044302,
            2044312, 2044314, 2044401, 2044402, 2044412, 2044414, 2044501, 2044502, 2044601, 2044602,
            2044701, 2044702, 2044801, 2044802, 2044807, 2044809, 2044901, 2044902, 2048001, 2048002,
            2048004, 2048005, 2049100};

        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802000, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});
        datas_r.add(new int[]{6802001, 0, 999999, 0, 10, 49});

        for (int zii : normal_scrolls) {
            datas_r.add(new int[]{6802001, zii, 1300, 0, 1, 1});
            datas_r.add(new int[]{6802000, zii, 1600, 0, 1, 1});
        }
        //크리스탈 원석
        for (int iii = 4004000; iii <= 4004003; ++iii) {
            datas_r.add(new int[]{6802000, iii, 20000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 20000, 0, 1, 1});
        }
        //광물 원석
        for (int iii = 4010000; iii <= 4010007; ++iii) {
            datas_r.add(new int[]{6802000, iii, 100000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 100000, 0, 1, 1});
        }
        //보석 원석
        for (int iii = 4020000; iii <= 4020008; ++iii) {
            datas_r.add(new int[]{6802000, iii, 50000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 50000, 0, 1, 1});
        }
        //만병 통치약
        datas_r.add(new int[]{6802000, 2050004, 250000, 0, 1, 1});
        datas_r.add(new int[]{6802001, 2050004, 250000, 0, 1, 1});
        //안약 보약 성수
        for (int iii = 2050001; iii <= 2050003; ++iii) {
            datas_r.add(new int[]{6802000, iii, 90000, 0, 1, 1});
            datas_r.add(new int[]{6802001, iii, 90000, 0, 1, 1});
        }

        //독안개의 숲 파퀘
        datas_r.add(new int[]{3002000, 4001162, 999999, 0, 1, 1});
        datas_r.add(new int[]{3002001, 4001163, 999999, 0, 1, 1});

        //속성강화 30
        datas_r.add(new int[]{9202012, 2290113, 3600, 0, 1, 1});

        //래피드 파이어 30
        datas_r.add(new int[]{9202012, 2290118, 4400, 0, 1, 1});

        //배틀쉽 캐논 30
        datas_r.add(new int[]{9202012, 2290120, 5400, 0, 1, 1});

        //커다란 진주 퀘스트
        datas_r.add(new int[]{1202002, 4031843, 999999, 2169, 1, 1});
        questdrops.add(4031843);

        //
        datas_r.add(new int[]{2612005, 4031798, 999999, 3366, 1, 1});
        questdrops.add(4031798);

        datas_r.add(new int[]{2502002, 2022252, 999999, 3839, 1, 1});
        questdrops.add(4031798);

        datas_r.add(new int[]{1012000, 4032143, 999999, 20717, 1, 1});
        questdrops.add(4032143);

        datas_r.add(new int[]{2402007, 4032512, 999999, 3720, 1, 1});
        datas_r.add(new int[]{2402008, 4032512, 999999, 3720, 1, 1});
        questdrops.add(4032512);

        datas_r.add(new int[]{5130104, 4031218, 10000, 3071, 1, 1});
        questdrops.add(4031218);

        /*마발 리액터*/
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});

        int[] balrog_weapons = new int[]{
            2330005,//이터널 불릿
            1072375,//발록의 가죽 신발
            1072376,//발록의 털가죽 신발
            1302112,//발록의 아츠
            1302113,//<string name="name" value="발록의 프라우테"/>
            1312042,//<string name="name" value="발록의 리프 엑스"/>
            1312043,//string name="name" value="발록의 비펜니스"/>
            1322068,//<string name="name" value="발록의 골든해머"/>
            1322069,//<string name="name" value="발록의 루인해머"/>
            1332084,//발록의 카타르
            1332085,//발록의 용천권
            1332086,//발록의 커세어
            1332087,//발록의 바키트
            1342019,//발록의 용화도
            1342020,//발록의 만혈도
            1372050,//발록의 마기코라스
            1372051,//발록의 피닉스 완드
            1382066,//발록의 케이그
            1382067,//발록의 블루마린
            1402056,//발록의 청운검
            1402057,//발록의 그레이트 로헨
            1402058,//발록의 발록의 참마도
            1402059,//발록의 라 투핸더
            1412038,//발록의 헬리오스
            1412039,//발록의 클로니안 엑스
            1422042,//발록의 크롬
            1422043,//발록의 레오마이트
            1432054,//발록의 호진공창
            1432055,//발록의 페어프로즌
            1442074,//발록의 월아산
            1442075,//발록의 핼버드
            1452066,//발록의 마린 아룬드
            1452067,//발록의 파이어 아룬드
            1452068,//발록의 골든 아룬드
            1452069,//발록의 다크 아룬드
            1452070,//발록의 메투스
            1462059,//발록의 마린 샬리트
            1462060,//발록의 파이어 샬리트
            1462061,//발록의 골든 샬리트
            1462062,//발록의 다크 샬리트
            1462063,//발록의 카사 크로우
            1472083,//발록의 코브라스티어
            1472084,//발록의 캐스터스
            1482031,//발록의 스틸르노
            1482032,//발록의 백혈귀
            1492035,//발록의 인피티니
            1492036,//발록의 피스메이커
            1382068,//베인윙즈
            1402062,//베인스워드
            1442078,//베인 폴암
            1452071,//베인 롱보우
            1472086,//베인바이터
            1492037//베인 슈터
        };
        for (int zii : balrog_weapons) {
            datas_r.add(new int[]{1052001, zii, 100000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, zii, 50000, 0, 1, 1});//이지
        }
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 5});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 5});//이지
        }
        //혼줌
        datas_r.add(new int[]{1052001, 2049100, 1000, 0, 1, 1});//노말 
        datas_r.add(new int[]{1052002, 2049100, 500, 0, 1, 1});//이지        
        //발록의 가죽
        datas_r.add(new int[]{1052001, 4001261, 1000000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 4001261, 250000, 0, 1, 1});//이지
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
        //백줌
        datas_r.add(new int[]{1052001, 2049000, 1000, 0, 1, 1});//노말 
        datas_r.add(new int[]{1052002, 2049000, 500, 0, 1, 1});//이지
        //발록의 가죽
        datas_r.add(new int[]{1052001, 4001261, 1000000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 4001261, 250000, 0, 1, 1});//이지
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
        //만병 통치약
        datas_r.add(new int[]{1052001, 2050004, 250000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 2050004, 250000, 0, 1, 1});//이지
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
        for (int zii : balrog_weapons) {
            datas_r.add(new int[]{1052001, zii, 5000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, zii, 1000, 0, 1, 1});//이지
        }
        //만병 통치약
        datas_r.add(new int[]{1052001, 2050004, 250000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 2050004, 250000, 0, 1, 1});//이지
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        datas_r.add(new int[]{1052001, 2050004, 250000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 2050004, 250000, 0, 1, 1});//이지
        //발록의 가죽
        datas_r.add(new int[]{1052001, 4001261, 1000000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 4001261, 250000, 0, 1, 1});//이지
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
        datas_r.add(new int[]{1052001, 2050004, 250000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 2050004, 250000, 0, 1, 1});//이지
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        datas_r.add(new int[]{1052001, 2050004, 250000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 2050004, 250000, 0, 1, 1});//이지
        /*//파워 엘릭서
         datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
         datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
         datas_r.add(new int[]{1052001, 2000005, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000005, 250000, 0, 1, 1});//이지  
         datas_r.add(new int[]{1052001, 2000005, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000005, 250000, 0, 1, 1});//이지  
         datas_r.add(new int[]{1052001, 2000005, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000005, 250000, 0, 1, 1});//이지  
         datas_r.add(new int[]{1052001, 2000005, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000005, 250000, 0, 1, 1});//이지  
         //엘릭서
         datas_r.add(new int[]{1052001, 2000004, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000004, 250000, 0, 1, 1});//이지 
         datas_r.add(new int[]{1052001, 2000004, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000004, 250000, 0, 1, 1});//이지 
         datas_r.add(new int[]{1052001, 2000004, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000004, 250000, 0, 1, 1});//이지 
         datas_r.add(new int[]{1052001, 2000004, 250000, 0, 1, 1});//노말 
         datas_r.add(new int[]{1052002, 2000004, 250000, 0, 1, 1});//이지 */
        for (int iii = 2000004; iii <= 2000006; ++iii) {//엘릭서류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int zii : balrog_weapons) {
            datas_r.add(new int[]{1052001, zii, 5000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, zii, 1000, 0, 1, 1});//이지
        }
        for (int iii = 2020000; iii <= 2020010; ++iii) {//엘릭서류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int iii = 2001000; iii <= 2001002; ++iii) {//수박부터 팥빙수
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        //발록의 가죽
        datas_r.add(new int[]{1052001, 4001261, 1000000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 4001261, 250000, 0, 1, 1});//이지
        //발록의주문서
        for (int iii = 2040728; iii <= 2040739; ++iii) {
            datas_r.add(new int[]{1052001, iii, 2000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 1000, 0, 1, 1});//이지
        }
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        datas_r.add(new int[]{1052001, 4001399, 100000, 0, 1, 1});//노말 봉인석
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
        for (int iii = 2020000; iii <= 2020010; ++iii) {//엘릭서류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int iii = 2001000; iii <= 2001002; ++iii) {//수박부터 팥빙수
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int iii = 2000004; iii <= 2000006; ++iii) {//엘릭서류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
        for (int iii = 2020000; iii <= 2020010; ++iii) {//엘릭서류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int iii = 2001000; iii <= 2001002; ++iii) {//수박부터 팥빙수
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int zii : balrog_weapons) {
            datas_r.add(new int[]{1052001, zii, 100000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, zii, 50000, 0, 1, 1});//이지
        }
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        //발록의 가죽
        datas_r.add(new int[]{1052001, 4001261, 1000000, 0, 1, 1});//노말
        datas_r.add(new int[]{1052002, 4001261, 250000, 0, 1, 1});//이지
        datas_r.add(new int[]{1052001, 0, 999999, 0, 20000, 50000});
        datas_r.add(new int[]{1052002, 0, 999999, 0, 5000, 10000});
        for (int iii = 2000004; iii <= 2000006; ++iii) {//엘릭서류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int iii = 2020000; iii <= 2020010; ++iii) {//엘릭서류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        for (int zii : balrog_weapons) {
            datas_r.add(new int[]{1052001, zii, 100000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, zii, 50000, 0, 1, 1});//이지
        }
        for (int iii = 2020012; iii <= 2020015; ++iii) {//고급류
            datas_r.add(new int[]{1052001, iii, 250000, 0, 1, 1});//노말
            datas_r.add(new int[]{1052002, iii, 250000, 0, 1, 1});//이지
        }
        //맵코드 105100301(1052001)노말, 105100401(1052002)이지

        //리액터 하드코딩
        for (int[] i : datas_r) {
            reactordrops.add(new ReactorDropEntry(i[0], i[1], i[2], i[4], i[5], i[3]));
        }
        reactordrops.add(new ReactorDropEntry(2222001, 1002309, 5000, 1, 1, 0));//수박 모자
        reactordrops.add(new ReactorDropEntry(2222001, 1002312, 5000, 1, 1, 0));//귀신수박 모자
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 5000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 5060002, 10000, 1, 1, 0));//부화기
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박
        reactordrops.add(new ReactorDropEntry(2222001, 2001000, 1000000, 1, 5, 0));//소비템 수박

        /*건초*/
        reactordrops.add(new ReactorDropEntry(1002008, 4032452, 1000000, 1, 1, 22502));//건초더미
        reactordrops.add(new ReactorDropEntry(1002008, 2000000, 300000, 1, 1, 0));//빨간포션
        reactordrops.add(new ReactorDropEntry(1002008, 2000001, 300000, 1, 1, 0));//파란포션
        reactordrops.add(new ReactorDropEntry(1002008, 0, 300000, 10, 50, 0));//메소

        /*피오의 상자*/
        reactordrops.add(new ReactorDropEntry(1002009, 4031161, 1000000, 1, 1, 1008));//나사
        reactordrops.add(new ReactorDropEntry(1002009, 4031162, 1000000, 1, 1, 1008));//나무
        reactordrops.add(new ReactorDropEntry(1002009, 2000000, 300000, 1, 1, 0));//빨간포션
        reactordrops.add(new ReactorDropEntry(1002009, 2000001, 300000, 1, 1, 0));//파란포션
        reactordrops.add(new ReactorDropEntry(1002009, 0, 300000, 10, 50, 0));//메소

        /*아이템이 들어있는 상자가 있어*/
        reactordrops.add(new ReactorDropEntry(1302000, 4032267, 1000000, 1, 1, 20013));//석재
        reactordrops.add(new ReactorDropEntry(1302000, 4032268, 1000000, 1, 1, 20013));//휘장
        reactordrops.add(new ReactorDropEntry(1302000, 2000000, 300000, 1, 1, 0));//빨간포션
        reactordrops.add(new ReactorDropEntry(1302000, 2000001, 300000, 1, 1, 0));//파란포션
        reactordrops.add(new ReactorDropEntry(1302000, 0, 300000, 10, 50, 0));//메소

        /*도토리나무*/
        reactordrops.add(new ReactorDropEntry(3102000, 2022712, 1000000, 1, 1, 0));//도토리
        reactordrops.add(new ReactorDropEntry(3102000, 2022712, 1000000, 1, 1, 0));//도토리
        reactordrops.add(new ReactorDropEntry(3102000, 2022712, 1000000, 1, 1, 0));//도토리
        reactordrops.add(new ReactorDropEntry(3102000, 2022712, 1000000, 1, 1, 0));//도토리
        reactordrops.add(new ReactorDropEntry(3102000, 2022712, 1000000, 1, 1, 0));//도토리
        reactordrops.add(new ReactorDropEntry(3102000, 2022712, 1000000, 1, 1, 0));//도토리
        reactordrops.add(new ReactorDropEntry(3102000, 2000000, 300000, 1, 1, 0));//빨간포션
        reactordrops.add(new ReactorDropEntry(3102000, 2000001, 300000, 1, 1, 0));//파란포션
        reactordrops.add(new ReactorDropEntry(3102000, 0, 300000, 10, 50, 0));//메소

        reactordrops.add(new ReactorDropEntry(3102003, 4032775, 1000000, 1, 1, 0));//새끼 광석 이터

        /*영웅을 위한 선물*/
        reactordrops.add(new ReactorDropEntry(1402000, 4032309, 1000000, 1, 1, 21013));//대나무 한 단
        reactordrops.add(new ReactorDropEntry(1402000, 4032310, 1000000, 1, 1, 21013));//나무
        reactordrops.add(new ReactorDropEntry(1402000, 2000000, 300000, 1, 1, 0));//빨간포션
        reactordrops.add(new ReactorDropEntry(1402000, 2000001, 300000, 1, 1, 0));//파란포션
        reactordrops.add(new ReactorDropEntry(1402000, 0, 300000, 10, 50, 0));//메소

        /*유적더미*/
        reactordrops.add(new ReactorDropEntry(1022001, 4032319, 1000000, 1, 1, 21723));//히죽대는
        reactordrops.add(new ReactorDropEntry(1022001, 2000000, 300000, 1, 1, 0));//빨간포션
        reactordrops.add(new ReactorDropEntry(1022001, 2000001, 300000, 1, 1, 0));//파란포션
        reactordrops.add(new ReactorDropEntry(1022001, 0, 300000, 10, 50, 0));//메소

        /*각성의 시간*/
        reactordrops.add(new ReactorDropEntry(1032001, 2430071, 1000000, 1, 1, 2363));//거울

        /*더 큰 안장 만들기*/
        reactordrops.add(new ReactorDropEntry(2302006, 4032476, 1000000, 1, 1, 22407));//캡틴 알파의 버클

        Map<Integer, List<MobDropEntry>> mdrop_final = new HashMap<Integer, List<MobDropEntry>>();
        Map<Integer, List<MobDropEntry>> mdrop_finalPia = new HashMap<Integer, List<MobDropEntry>>();
        Map<Integer, List<MobDropEntry>> mdrop_final2 = new HashMap<Integer, List<MobDropEntry>>();
        Map<Integer, List<ReactorDropEntry>> rdrop_final = new HashMap<Integer, List<ReactorDropEntry>>();

        for (MobDropEntry mde : mobdrops) {
            if (!mdrop_final.containsKey(mde.mobid)) {
                mdrop_final.put(mde.mobid, new ArrayList<MobDropEntry>());
            }
            List<MobDropEntry> dd = mdrop_final.get(mde.mobid);
            dd.add(mde);
        }
        for (ReactorDropEntry rde : reactordrops) {
            if (!rdrop_final.containsKey(rde.reactorid)) {
                rdrop_final.put(rde.reactorid, new ArrayList<ReactorDropEntry>());
            }
            List<ReactorDropEntry> dd = rdrop_final.get(rde.reactorid);
            dd.add(rde);
        }

        for (MapleData mD : mBook) {
            int mobid = Integer.parseInt(mD.getName());
            switch (mobid) {
                case 9400265://베르가모트
                case 9400289://아우프헤벤
                    continue;
            }
            List<Integer> d = mBookRewards.get(mobid);
            if (d == null) {
                d = new LinkedList<Integer>();
                mBookRewards.put(mobid, d);
            }
            for (MapleData mDR : mD.getChildByPath("reward")) {
                int itemid = MapleDataTool.getInt(mDR);
                if (!ii.itemExists(itemid) && itemid != 0) {
                    System.err.println("Item " + itemid + " does not exists.. Continue.");
                } else {
                    d.add(itemid);
                }
            }
        }

        for (MapleData mD : oldmBook) { //빅뱅전 데이터 가져오는 부분
            int mobid = Integer.parseInt(mD.getName());
            /*if (mobid >= 7120103 && mobid <= 7120109
                    || mobid >= 7220003 && mobid <= 7220005
                    || mobid >= 8120100 && mobid <= 8120107
                    || mobid >= 8140510 && mobid <= 8140512
                    || mobid >= 8220010 && mobid <= 8220015
                    //|| mobid == 8810018 //|| mobid == 8510000 || mobid == 8520000 //피아누스 옛날 마북
                    || mobid == 9500390) {
                List<Integer> d = mBookRewards.get(mobid);
                if (d == null) {
                    d = new LinkedList<Integer>();
                    mBookRewards.put(mobid, d);
                }
                for (MapleData mDR : mD.getChildByPath("reward")) {
                    int itemid = MapleDataTool.getInt(mDR);
                    d.add(itemid);
                }
            }*/
            if (!mBookRewards.containsKey(mobid)) {
                List<Integer> d = mBookRewards.get(mobid);
                if (d == null) {
                    d = new LinkedList<Integer>();
                    mBookRewards.put(mobid, d);
                }
                for (MapleData mDR : mD.getChildByPath("reward")) {
                    int itemid = MapleDataTool.getInt(mDR);
                    d.add(itemid);
                }
            }
        }

        for (Entry<Integer, List<Integer>> mBookChild : mBookRewards.entrySet()) { //1.2.6은 몬스터북 X
            int mobid = 0;
            if (mBookChild.getKey() == 8220013) {
                mobid = 8220015;
            } else if (mBookChild.getKey() == 9400408) {
                mobid = 9400409;
            } else {
                mobid = mBookChild.getKey();
            }
            if (mdrop_final.containsKey(mobid)) {
                //                List<MobDropEntry> missingMBookDrops = new ArrayList<MobDropEntry>();
                List<MobDropEntry> mdes = mdrop_final.get(mobid);
                List<MobDropEntry> newMobDrops = new ArrayList<MobDropEntry>(mdes);
                List<MobDropEntry> newMobDropsPia = new ArrayList<MobDropEntry>(mdes);
                List<Integer> mBookRewardIds = mBookChild.getValue();
                boolean isBoss = MapleLifeFactory.getMonsterStats(mobid).isBoss();
                boolean isRaidBoss = false;

                for (Integer itemid : mBookRewardIds) {
                    boolean found = false;
                    for (MobDropEntry mde : mdes) {
                        if (mde.itemid == itemid) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        switch (mobid) {
                            case 8810018: //혼테일
                            case 8800002: //자쿰
                            case 8520000: //좌붕
                            case 8510000: //우붕
                            case 8500002: //파풀라투스
                            case 8820001: //핑크빈
                            case 9500392: //라바나
                            case 9420522: //크렉셀
                            case 9400409: //대두꺼비 가엘
                            case 9400265: //베르가모트
                            case 9400270: //듀나스 1차전
                            case 9420544: //분노한 타르가
                            case 9420549: //분노한 스카라이온 보스
                            case 9400266: //이름 없는 마수
                            case 9400273: //니베룽
                            case 9400294: //듀나스 2차전
                            case 9400300: //대두목
                            case 9600025: //무림요승
                                isRaidBoss = true;
                                break;
                        }
                        switch (itemid) {
                            case 2022053:
                            case 2022054:
                            case 2022055:
                            case 2022056:
                            case 2022057:
                            case 2022058:
                            case 4001431:
                            case 4001473:
                                break;
                            default:
                                add(itemid, newMobDrops, mBookChild, isBoss, isRaidBoss);
                                break;
                        }
                        //                        System.out.println("드롭 테이블에 없는 아이템 - 몬스터 : " + mBookChild.getKey() + " / 아이템 : " + itemid + " (" + ii.getName(itemid) + ")");
                    }
                }

                boolean hasMoney = false;
                for (MobDropEntry mde : mdes) {
                    if (mde.itemid == 0) {
                        hasMoney = true;
                        break;
                    }
                }

                int repeat = 1;
                int mesoMultiplier;
                switch (mobid) {
                    case 9400265:
                    case 9400270:
                    case 9400266:
                    case 9400273:
                    case 9400294:
                    case 9400289:
                        mesoMultiplier = 5;
                        break;
                    default:
                        mesoMultiplier = 1;
                        break;
                }
                if (isBoss) {
                    switch (mobid) {
                        case 9400265:
                        case 9400270:
                        case 9400266:
                        case 9400273:
                        case 9400294:
                        case 9400289:
                            repeat = 1;
                            break;
                        default:
                            repeat = 5;
                            break;
                    }
                }
                for (int i = 0; i < repeat && !hasMoney && mobid != 8810018; ++i) {
                    MapleMonsterStats mobstat = MapleLifeFactory.getMonsterStats(mobid);
                    double mesoDecrease = Math.pow(0.93, mobstat.getExp() / (isBoss ? 2000.0 : 300.0));
                    if (mesoDecrease > 1.0) {
                        mesoDecrease = 1.0;
                    } else if (mesoDecrease < 0.001) {
                        mesoDecrease = 0.005;
                    }
                    int tempmeso = Math.min(30000, (int) (mesoDecrease * (mobstat.getExp() * 5.7) / 10.0));

                    byte value = 1;
                    if (mobstat.getLevel() >= 1 && mobstat.getLevel() < 70) {
                        value = 3;
                    } else if (mobstat.getLevel() >= 70 && mobstat.getLevel() < 120) {
                        value = 3;
                    } else {
                        value = 2;
                    }
                    final int meso = tempmeso * value * mesoMultiplier;
                    newMobDrops.add(new MobDropEntry(mobid, 0, 700000, (int) (meso * 0.75), meso, 0));
                }

                mdrop_final.put(mobid, newMobDrops);
            } else {
                if (!mBookChild.getValue().isEmpty() && MapleLifeFactory.getMonsterStats(mobid) != null) { //존재하지 않는 몹 부분
                    //                    System.out.println("드롭에 없는 몹 : " + mBookChild.getKey() + " (" + getMobName(mBookChild.getKey()) + ")");

                    boolean isBoss = MapleLifeFactory.getMonsterStats(mobid).isBoss();
                    boolean isRaidBoss = false;
                    switch (mobid) {
                        case 8800002: //자쿰
                        case 8520000: //좌붕
                        case 8510000: //우붕
                        case 8500002: //파풀라투스
                        case 8820001: //핑크빈
                        case 9500392: //라바나
                        case 9420522: //크렉셀
                        case 9400409: //대두꺼비 가엘
                        case 9400265: //베르가모트
                        case 9400270: //듀나스 1차전
                        case 9420544: //분노한 타르가
                        case 9420549: //분노한 스카라이온 보스
                        case 9400266: //이름 없는 마수
                        case 9400273: //니베룽
                        case 9400294: //듀나스 2차전
                        case 9400300: //대두목
                        case 9600025: //무림요승
                            isRaidBoss = true;
                            break;
                    }

                    List<MobDropEntry> newMobDrops = new ArrayList<MobDropEntry>();
                    if (isRaidBoss) {
                        // ..
                    } else {
                        for (int i = 0; i < (isBoss ? 5 : 1); ++i) {
                            Random r = new Random();
                            MapleMonsterStats mobstat = MapleLifeFactory.getMonsterStats(mobid);
                            double mesoDecrease = Math.pow(0.93, mobstat.getExp() / (isBoss ? 2000.0 : 300.0));
                            if (mesoDecrease > 1.0) {
                                mesoDecrease = 1.0;
                            } else if (mesoDecrease < 0.001) {
                                mesoDecrease = 0.005;
                            }
                            int tempmeso = Math.min(30000, (int) (mesoDecrease * (mobstat.getExp() * 5.7) / 10.0));
                            byte value = 1;
                            if (mobstat.getLevel() >= 1 && mobstat.getLevel() < 70) {
                                value = 3;
                            } else if (mobstat.getLevel() >= 70 && mobstat.getLevel() < 120) {
                                value = 3;
                            } else {
                                value = 2;
                            }
                            final int meso = tempmeso * value;
                            newMobDrops.add(new MobDropEntry(mobid, 0, 700000, (int) (meso * 0.75), meso, 0));
                        }
                    }
                    for (Integer itemid : mBookChild.getValue()) {
                        switch (itemid) {
                            case 2022053:
                            case 2022054:
                            case 2022055:
                            case 2022056:
                            case 2022057:
                            case 2022058:
                            case 4001431:
                            case 4001473:
                                break;
                            default:
                                add(itemid, newMobDrops, mBookChild, isBoss, isRaidBoss);
                                break;
                        }
                    }
                    mdrop_final.put(mobid, newMobDrops);
                }
            }
        }
        getMobName(100100);

        for (List<MobDropEntry> mdes : mdrop_final.values()) {
            for (MobDropEntry mde : mdes) {
                if (mde.itemid != 4000047) {
                    if (mde.itemid == 0 && mde.max == 0) {
                        continue;
                    }
                    ps.setInt(1, mde.mobid);
                    ps.setInt(2, mde.itemid);
                    ps.setInt(3, mde.min);
                    ps.setInt(4, mde.max);
                    ps.setInt(5, mde.questid);
                    if (mde.itemid >= 2022570 && mde.itemid <= 2022584 || mde.itemid == 4001318) { //페페킹 아이템
                        ps.setInt(6, 1000000);
                    } else {
                        ps.setInt(6, mde.chance);
                    }
                    ps.addBatch();
                }
            }
        }

        //훈련용 시리즈
        copyDropEntry(210100, 9300341, mdrop_final);
        copyDropEntry(1210102, 9300342, mdrop_final);
        copyDropEntry(1210100, 9300343, mdrop_final);

        copyDropEntry(6110300, 9300141, mdrop_final);

        for (int i = 9300315; i <= 9300324; ++i) { //몹들 
            copyDropEntry(9300130, i, mdrop_final);
        }

        copyDropEntry(9500390, 9500391, mdrop_final); //라바나 드롭템이 없음
        copyDropEntry(9500390, 9500392, mdrop_final);
        /*메이플 아일랜드*/
        copyDropEntry(100100, 100000, mdrop_final);//달팽이
        copyDropEntry(100101, 100001, mdrop_final);//파란 달팽이
        copyDropEntry(130101, 100002, mdrop_final);//빨간 달팽이
        copyDropEntry(120100, 100003, mdrop_final);//스포아
        copyDropEntry(1210102, 100004, mdrop_final);//주황버섯
        copyDropEntry(130100, 100005, mdrop_final);//스텀프
        copyDropEntry(210100, 100006, mdrop_final);//슬라임
        copyDropEntry(1210100, 100007, mdrop_final);//돼지

        copyDropEntry(6300000, 6300001, mdrop_final);//예티
        copyDropEntry(6300000, 6300002, mdrop_final);//예티

        copyDropEntry(6400000, 6400001, mdrop_final);//다크 예티
        copyDropEntry(6400000, 6400002, mdrop_final);//다크 예티

        copyDropEntry(6230200, 6230201, mdrop_final);//다크 페페

        copyDropEntry(6130103, 6130102, mdrop_final);//페페

        copyDropEntry(4130102, 4130104, mdrop_final);//다크 네펜데스 
        copyDropEntry(4230105, 4230122, mdrop_final);//네펜데스

        copyDropEntryZ(8800002, 8800102, mdrop_final);//카오스자쿰

        copyDropEntryZ(8810018, 8810118, mdrop_final);//카오스혼테일

        for (List<MobDropEntry> mdes : mdrop_final2.values()) {
            for (MobDropEntry mde : mdes) {
                if (mde.itemid == 0 && mde.max == 0) {
                    continue;
                }
                ps.setInt(1, mde.mobid);
                ps.setInt(2, mde.itemid);
                ps.setInt(3, mde.min);
                ps.setInt(4, mde.max);
                ps.setInt(5, mde.questid);
                ps.setInt(6, mde.chance);
                ps.addBatch();
            }
        }

        for (List<ReactorDropEntry> mdes : rdrop_final.values()) {
            for (ReactorDropEntry mde : mdes) {
                if (mde.itemid == 0 && mde.max == 0) {
                    continue;
                }
                ps2.setInt(1, mde.reactorid);
                ps2.setInt(2, mde.itemid);
                ps2.setInt(3, mde.chance);
                ps2.setInt(4, mde.questid);
                ps2.setInt(5, mde.min);
                ps2.setInt(6, mde.max);
                ps2.addBatch();
            }
        }

        for (MapleQuest quest : MapleQuest.getAllInstances()) {
            for (MapleQuestAction act : quest.getCompleteActs()) {
                if (act.getItems() == null) {
                    continue;
                }
                for (QuestItem qitem : act.getItems()) {
                    if (qitem.count < 0 && MapleItemInformationProvider.getInstance().isQuestItem(qitem.itemid)) {
                        if (!questdrops.contains(Integer.valueOf(qitem.itemid))) {
                            System.out.println(qitem.itemid + " : " + (ii.getName(qitem.itemid)) + " (" + quest.getId() + " - " + quest.getName() + ")");
                        }
                    }
                }
            }
        }

        ps.executeBatch();
        ps2.executeBatch();
        ps.close();
        ps2.close();
        System.out.println("Job Done. Elapsed Time : " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void copyDropEntry(int from, int to, Map<Integer, List<MobDropEntry>> mdrop_final) throws Exception {
        List<MobDropEntry> temp = mdrop_final.get(from);//6110300
        Map<Integer, List<MobDropEntry>> mdrop_final2 = new HashMap<Integer, List<MobDropEntry>>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, maximum_quantity, questid, chance) VALUES (?, ?, ?, ?, ?, ?)");
        mdrop_final2.put(to, temp);
        try {
            for (List<MobDropEntry> mdes : mdrop_final2.values()) {
                for (MobDropEntry mde : mdes) {
                    mde.mobid = to;
                    int itemlevel = MapleItemInformationProvider.getInstance().getReqLevel((mde.itemid));
                    if (mde.itemid == 4001129) { //그냥 코인
                        mde.itemid = 4001254; //반짝이는 코인
                        mde.chance /= 1.4;
                    } else if (mde.mobid == 9500391 && (mde.itemid / 1000000 == 1)) {
                        continue;
                    } else if (mde.mobid == 9500392 && (mde.itemid / 1000000 == 1)) {
                        continue;
                    } else {
                        if (mde.itemid != 4000047
                                && mde.itemid != 2382032
                                && mde.itemid != 4000058 //네펜데스 씨앗
                                && mde.itemid != 4000062 //다크 네펜데스씨 앗
                                ) {
                            if (mde.itemid == 0 && mde.max == 0) {
                                continue;
                            }
                            if (mde.itemid == 0 && mde.min == 0) {
                                mde.min = 1;
                            }
                            ps.setInt(1, mde.mobid);
                            ps.setInt(2, mde.itemid);
                            ps.setInt(3, mde.min);
                            ps.setInt(4, mde.max);
                            ps.setInt(5, mde.questid);
                            ps.setInt(6, mde.chance);
                            ps.addBatch();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        ps.executeBatch();
        con.close();
        ps.close();

    }

    public static void copyDropEntryZ(int from, int to, Map<Integer, List<MobDropEntry>> mdrop_final) throws Exception {
        List<MobDropEntry> temp = mdrop_final.get(from);//6110300
        Map<Integer, List<MobDropEntry>> mdrop_final2 = new HashMap<Integer, List<MobDropEntry>>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("INSERT INTO `drop_data` (`dropperid`, `itemid`, `minimum_quantity`, maximum_quantity, questid, chance) VALUES (?, ?, ?, ?, ?, ?)");
        for (MobDropEntry mde : temp) {
            mde.mobid = to;
            if (mde.itemid == 1002357) { //그냥 자투
                mde.itemid = 1003112; //카오스 자투
            } else if (mde.itemid == 1372049) { //그냥 나뭇가지
                mde.itemid = 1372073; //카오스 자쿰의 나뭇가지
            } else if (mde.itemid == 1122000) { //그냥 목걸이
                mde.itemid = 1122076; //카오스 혼테일의 목걸이
            }
        }
        mdrop_final2.put(to, temp);
        byte index1 = 0;
        byte index2 = 0;
        byte index3 = 0;
        try {
            for (List<MobDropEntry> mdes : mdrop_final2.values()) {
                for (MobDropEntry mde : mdes) {
                    boolean ok = true;
                    int itemlevel = MapleItemInformationProvider.getInstance().getReqLevel((mde.itemid));
                    if (mde.itemid == 0 && mde.max == 0) {
                        continue;
                    }
                    if (to == 8800102) { //카오스자쿰
                        if (mde.itemid == 1003112) {
                            switch (index1) {
                                case 0:
                                    mde.chance = 500000;
                                    break;
                                case 1:
                                    mde.chance = 350000;
                                    break;
                                case 2:
                                    mde.chance = 200000;
                                    break;
                                case 3:
                                    mde.chance = 125000;
                                    break;
                                case 4:
                                    mde.chance = 50000;
                                    break;
                                case 5:
                                    ok = false;
                                    break;
                            }
                            index1++;
                        } else if (mde.itemid == 1012478) {
                            switch (index2) {
                                case 0:
                                    mde.chance = 110000;
                                    break;
                                case 1:
                                    mde.chance = 50000;
                                    break;
                            }
                            index2++;
                        } else if (mde.itemid == 1022231) {
                            switch (index3) {
                                case 0:
                                    mde.chance = 110000;
                                    break;
                                case 1:
                                    mde.chance = 50000;
                                    break;
                            }
                            index3++;
                        }
                    } else if (to == 8810118) { //카오스혼테일
                        if (mde.itemid == 2020013) { //순록의 우유
                            mde.min = 50;
                            mde.max = 120;
                        } else if (mde.itemid == 2020015) { //황혼의 이슬
                            mde.min = 50;
                            mde.max = 120;
                        } else if (mde.itemid == 2000004) { //엘릭서 
                            mde.min = 20;
                            mde.max = 80;
                        } else if (mde.itemid == 2000005) { //파워 엘릭서 
                            mde.min = 30;
                            mde.max = 70;
                        } else if (mde.itemid == 1032241) { //데아시두스
                            mde.chance = 75000;
                        }
                    }
                    if (ok) {
                        ps.setInt(1, mde.mobid);
                        ps.setInt(2, mde.itemid);
                        ps.setInt(3, mde.min);
                        ps.setInt(4, mde.max);
                        ps.setInt(5, mde.questid);
                        mde.chance *= 2;
                        if (mde.chance >= 1000000) {
                            mde.chance = 1000000;
                        }
                        ps.setInt(6, mde.chance);
                        ps.addBatch();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        ps.executeBatch();
        con.close();
        ps.close();

    }

    public static class MobDropEntry {

        public MobDropEntry(int mobid, int itemid, int chance, int min, int max, int questid) {
            this.mobid = mobid;
            this.itemid = itemid;
            this.chance = chance;
            this.min = min;
            this.max = max;
            this.questid = questid;
        }

        public int mobid;
        public int itemid;
        public int chance;
        public int min;
        public int max;
        public int questid;
    }

    public static class ReactorDropEntry {

        public ReactorDropEntry(int mobid, int itemid, int chance, int min, int max, int questid) {
            this.reactorid = mobid;
            this.itemid = itemid;
            this.chance = chance;
            this.min = min;
            this.max = max;
            this.questid = questid;
        }

        public int reactorid;
        public int itemid;
        public int chance;
        public int min;
        public int max;
        public int questid;
    }

    static final Map<Integer, String> mobnames = new HashMap<>();

    public static String getMobName(int mid) {
        if (mobnames.isEmpty()) {
            MapleDataProvider p = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
            MapleData d = p.getData("Mob.img");
            for (MapleData dd : d) {
                mobnames.put(Integer.parseInt(dd.getName()), MapleDataTool.getString("name", dd, "null"));
            }
        }
        return mobnames.get(mid);
    }

    public static boolean isNeedDropDataMob(final int mid) {
        switch (mid) {
            case 4110300://아이언 뮤테
            case 4110301://강화된 아이언 뮤테
            case 4110302://미스릴 뮤테
            case 4230600://모래거인
            case 5110301://로이드
            case 5110300://강화된 미스릴 뮤테
            case 5110302://네오 휴로이드
            case 6090000://리치
            //case 6110300://호문
            case 6110301://사이티
            case 7110300://D.로이
            case 7110301://호문쿨루
            case 8110300://호문스큘러
                return true;
        }
        if (mid >= 3100101 && mid <= 3110303) { //모래 난쟁이부터
            return true;
        }
        return false;
    }

    public static boolean isDonNeedDropDataMob(final int mid) {
        switch (mid) {
            case 2110200://뿔버섯
            case 3110100://리게이터
            case 6090000://리치
                return true;
        }
        return false;
    }

    private static void addWorldTripMob(List<int[]> datas) {
        //WorldTrip
        //천구
        datas.add(new int[]{9400014, 1032026, 1, 1, 0, 30000});
        datas.add(new int[]{9400014, 1332029, 1, 1, 0, 20000});
        datas.add(new int[]{9400014, 1402037, 1, 1, 0, 20000});
        datas.add(new int[]{9400014, 1432018, 1, 1, 0, 20000});
        datas.add(new int[]{9400014, 1462022, 1, 1, 0, 20000});
        datas.add(new int[]{9400014, 2290149, 1, 1, 0, 50000}); //다크포그 30
        datas.add(new int[]{9400014, 2290010, 1, 1, 0, 50000}); //브레이브 슬래시 20
        datas.add(new int[]{9400014, 1302106, 1, 1, 0, 20000}); //푸른 화염의 카타나

        //몽롱귀신
        datas.add(new int[]{9400013, 2290149, 1, 1, 0, 150}); //다크포그 30
        datas.add(new int[]{8150201, 2290092, 1, 1, 0, 150}); //암살 20

        //갑옷무사
        datas.add(new int[]{9400405, 2290145, 1, 1, 0, 10000}); //매직 마스터리 30
        datas.add(new int[]{9400405, 2290060, 1, 1, 0, 10000}); //폭풍의 시 20
        datas.add(new int[]{9400405, 2290009, 1, 1, 0, 10000}); //어드밴스드 콤보 30
        datas.add(new int[]{9400405, 2290033, 1, 1, 0, 10000}); //체인 라이트닝 30
        datas.add(new int[]{9400405, 4001998, 1, 1, 0, 800000}); //투구의 파편
        datas.add(new int[]{9400405, 1003626, 1, 1, 0, 120000}); //갑옷무사의 투구
        datas.add(new int[]{9400405, 1052509, 1, 1, 0, 80000}); //갑옷무사의 갑주
        datas.add(new int[]{9400405, 1113180, 1, 1, 0, 10000}); //갑옷의 반지
        datas.add(new int[]{9400405, 1302106, 1, 1, 0, 10000}); //푸른 화염의 카타나
        datas.add(new int[]{9400405, 1382011, 1, 1, 0, 10000}); //황금 링 지팡이

        //상급닌자
        datas.add(new int[]{9400402, 2290092, 1, 1, 0, 150}); //암살 20
        datas.add(new int[]{9400402, 2290127, 1, 1, 0, 150}); //오버 스윙 30
        datas.add(new int[]{9400402, 2290070, 1, 1, 0, 150}); //피어싱 20

        //우두머리닌자
        datas.add(new int[]{9400403, 2290145, 1, 1, 0, 150}); //매직 마스터리 30
        datas.add(new int[]{9400403, 2290139, 1, 1, 0, 150}); //콤보 배리어 30
        datas.add(new int[]{9400403, 2290117, 1, 1, 0, 150}); //래피드 파이어 20
        datas.add(new int[]{9400403, 4001999, 1, 1, 0, 5000}); //닌자의 비술

        //버서키
        datas.add(new int[]{9420514, 2290050, 1, 1, 0, 150}); //엔젤레이 30
        datas.add(new int[]{9420514, 2290231, 1, 1, 0, 150}); //피니쉬 블로우 30

        //비트론
        datas.add(new int[]{9420515, 2290010, 1, 1, 0, 150}); //브레이브 슬래시 20
        datas.add(new int[]{9420515, 2290134, 1, 1, 0, 150}); //하이 디펜스 20
        datas.add(new int[]{9420515, 2290012, 1, 1, 0, 150}); //블래스트 20

        //슬라이지
        datas.add(new int[]{9420516, 2290052, 1, 1, 0, 150}); //샤프 아이즈 20
        datas.add(new int[]{9420516, 2290135, 1, 1, 0, 150}); //하이 디펜스 30
        
        //윈드 라이더
        datas.add(new int[]{9400576, 0, 1221, 1548, 0, 700000}); //메소
        datas.add(new int[]{9400576, 4032007, 1, 1, 50106, 200000}); //조커의 보물
        datas.add(new int[]{9400576, 4032015, 1, 1, 0, 6000}); //조화의 곡옥
        datas.add(new int[]{9400576, 4032016, 1, 1, 0, 6000}); //풍경의 곡옥
        datas.add(new int[]{9400576, 4032017, 1, 1, 0, 6000}); //환영의 곡옥
        datas.add(new int[]{9400576, 2002020, 1, 1, 0, 20000}); //마나불
        datas.add(new int[]{9400576, 1452025, 1, 1, 0, 1600}); //메투스 블루
        datas.add(new int[]{9400576, 2290052, 1, 1, 0, 150}); //샤프20
        datas.add(new int[]{9400576, 2290060, 1, 1, 0, 150}); //폭시20
        datas.add(new int[]{9400576, 2290070, 1, 1, 0, 150}); //피어싱20
        datas.add(new int[]{9400576, 2290056, 1, 1, 0, 150}); //보엑20
        datas.add(new int[]{9400576, 2290066, 1, 1, 0, 150}); //크보엑20
        
        //파이어 브랜드
        datas.add(new int[]{9400577, 0, 971, 1248, 0, 700000}); //메소
        datas.add(new int[]{9400577, 4032008, 1, 1, 50107, 200000}); //퀘템
        datas.add(new int[]{9400577, 4032015, 1, 1, 0, 6000}); //조화의 곡옥
        datas.add(new int[]{9400577, 4032016, 1, 1, 0, 6000}); //풍경의 곡옥
        datas.add(new int[]{9400577, 4032017, 1, 1, 0, 6000}); //환영의 곡옥
        datas.add(new int[]{9400577, 2002020, 1, 1, 0, 20000}); //마나불
        datas.add(new int[]{9400577, 2290036, 1, 1, 0, 150}); //파이어 데몬 20
        datas.add(new int[]{9400577, 2290042, 1, 1, 0, 150}); //아이스 데몬 20
        datas.add(new int[]{9400577, 2290026, 1, 1, 0, 150}); //빅뱅 20
        datas.add(new int[]{9400577, 2290044, 1, 1, 0, 150}); //엘퀴네스 20
        datas.add(new int[]{9400577, 2290038, 1, 1, 0, 150}); //이프리트 20
        datas.add(new int[]{9400577, 2290142, 1, 1, 0, 150}); //플레임 휠 20
        datas.add(new int[]{9400577, 2290144, 1, 1, 0, 150}); //매직 마스터리 20
        datas.add(new int[]{9400577, 2290140, 1, 1, 0, 150}); //일루전 20
        datas.add(new int[]{9400577, 2290146, 1, 1, 0, 150}); //블레이즈 20
        
        //파이어 브랜드 변신후
        datas.add(new int[]{9400578, 0, 571, 748, 0, 700000}); //메소
        
        //나이트 섀도우
        datas.add(new int[]{9400579, 0, 1221, 1648, 0, 700000}); //메소
        datas.add(new int[]{9400579, 4032009, 1, 1, 50107, 200000}); //퀘템
        datas.add(new int[]{9400579, 4032015, 1, 1, 0, 6000}); //조화의 곡옥
        datas.add(new int[]{9400579, 4032016, 1, 1, 0, 6000}); //풍경의 곡옥
        datas.add(new int[]{9400579, 4032017, 1, 1, 0, 6000}); //환영의 곡옥
        datas.add(new int[]{9400579, 2002021, 1, 1, 0, 10000}); //혼스터
        datas.add(new int[]{9400579, 2290076, 1, 1, 0, 150}); //페이크 20
        datas.add(new int[]{9400579, 2290078, 1, 1, 0, 150}); //베놈 20
        datas.add(new int[]{9400579, 2290090, 1, 1, 0, 150}); //써든 레이드 30
        datas.add(new int[]{9400579, 2290159, 1, 1, 0, 150}); //부메랑 스텝 20
        
        //엘더 레이스
        datas.add(new int[]{9400580, 0, 1121, 1448, 0, 700000}); //메소
        datas.add(new int[]{9400580, 4032010, 1, 1, 50102, 300000}); //오래된 재
        datas.add(new int[]{9400580, 4032011, 1, 1, 0, 700000}); //오래된 천
        datas.add(new int[]{9400580, 2290097, 1, 1, 0, 150}); //드래곤 스트라이크 20
        datas.add(new int[]{9400580, 2290106, 1, 1, 0, 150}); //피스트 20
        datas.add(new int[]{9400580, 2290108, 1, 1, 0, 150}); //윈드 부스터 20
        datas.add(new int[]{9400580, 2290117, 1, 1, 0, 150}); //래피드 파이어 20
        
        //스톰 브레이커
        datas.add(new int[]{9400581, 0, 1221, 1648, 0, 700000}); //메소
        datas.add(new int[]{9400581, 4032015, 1, 1, 0, 6000}); //조화의 곡옥
        datas.add(new int[]{9400581, 4032016, 1, 1, 0, 6000}); //풍경의 곡옥
        datas.add(new int[]{9400581, 4032017, 1, 1, 0, 6000}); //환영의 곡옥
        datas.add(new int[]{9400581, 2002021, 1, 1, 0, 10000}); //혼스터
        datas.add(new int[]{9400581, 2290006, 1, 1, 0, 150}); //스탠스 20
        datas.add(new int[]{9400581, 2290205, 1, 1, 0, 150}); //비홀더스 버프 20
        datas.add(new int[]{9400581, 2290018, 1, 1, 0, 150}); //디바인 차지 20
        datas.add(new int[]{9400581, 2290008, 1, 1, 0, 150}); //어드밴스드 콤보 20
        datas.add(new int[]{9400581, 2290128, 1, 1, 0, 150}); //하이 마스터리 20
        
        //스칼렛 피닉스, 아주르 오셀롯
        datas.add(new int[]{9400596, 0, 2221, 2648, 0, 700000}); //메소
        datas.add(new int[]{9400597, 0, 2221, 2648, 0, 700000}); //메소
        datas.add(new int[]{9400596, 4032003, 1, 1, 0, 1000000}); 
        datas.add(new int[]{9400597, 4032003, 1, 1, 0, 1000000}); 
        
        //이지 모드 보스 스킬북 드롭
        datas.add(new int[]{9400999, 2280007, 1, 1, 0, 200000}); //어드밴스드 콤보
        datas.add(new int[]{9400999, 2280008, 1, 1, 0, 200000}); //어드밴스드 차지
        datas.add(new int[]{9400999, 2280014, 1, 1, 0, 200000}); //하이 디펜스
        datas.add(new int[]{9400999, 2280015, 1, 1, 0, 200000}); //콤보 템페스트
        datas.add(new int[]{9400999, 2280013, 1, 1, 0, 200000}); //파이널 블로우
        datas.add(new int[]{9400999, 2280016, 1, 1, 0, 200000}); //콤보 배리어
        datas.add(new int[]{9400998, 2280009, 1, 1, 0, 200000}); //엔젤레이
        datas.add(new int[]{9400998, 2280026, 1, 1, 0, 200000}); //매직 마스터리
        datas.add(new int[]{9400998, 2280027, 1, 1, 0, 200000}); //플레임 휠
        datas.add(new int[]{9400998, 2280028, 1, 1, 0, 200000}); //다크 포그
        datas.add(new int[]{9400998, 2280029, 1, 1, 0, 200000}); //소울 스톤
        datas.add(new int[]{9400996, 2280010, 1, 1, 0, 200000}); //트리플 스로우
        datas.add(new int[]{9400996, 2280031, 1, 1, 0, 200000}); //사슬 지옥
        datas.add(new int[]{9400996, 2280030, 1, 1, 0, 200000}); //파이널 컷
        
        //빅풋
        datas.add(new int[]{9400575, 0, 12221, 22648, 0, 700000}); //메소
        datas.add(new int[]{9400575, 0, 12221, 22648, 0, 700000}); //메소
        datas.add(new int[]{9400575, 1402037, 1, 1, 0, 20000}); //스톤투스
        datas.add(new int[]{9400575, 1462022, 1, 1, 0, 20000}); //옐로우 크로우
        datas.add(new int[]{9400575, 1332029, 1, 1, 0, 20000}); //쌍고검
        datas.add(new int[]{9400575, 1442057, 1, 1, 0, 20000}); //보라색 서핑보드
        
        //들개
        datas.add(new int[]{9410000, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9410000, 1002096, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1050011, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1072011, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1072034, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1082020, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1092001, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 1452005, 1, 1, 0, 350});
        datas.add(new int[]{9410000, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9410000, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410000, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410000, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410000, 4000198, 1, 1, 0, 600000});
        datas.add(new int[]{9410000, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9410000, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9410000, 4020005, 1, 1, 0, 150});

        //멋쟁이 들개
        datas.add(new int[]{9410001, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9410001, 1002049, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1050003, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1072035, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1082017, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 1472011, 1, 1, 0, 350});
        datas.add(new int[]{9410001, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9410001, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410001, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410001, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410001, 4000199, 1, 1, 0, 600000});
        datas.add(new int[]{9410001, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9410001, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9410001, 4010005, 1, 1, 0, 1000});

        //험악한 들개
        datas.add(new int[]{9410002, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410002, 1002153, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1051039, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1072000, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1082066, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 1432005, 1, 1, 0, 350});
        datas.add(new int[]{9410002, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410002, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410002, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410002, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410002, 4000200, 1, 1, 0, 600000});
        datas.add(new int[]{9410002, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9410002, 4020000, 1, 1, 0, 150});
        datas.add(new int[]{9410002, 4020006, 1, 1, 0, 150});

        //광대 원숭이
        datas.add(new int[]{9410003, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410003, 1002170, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1040083, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1041075, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1060072, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1061070, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1072143, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1082024, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1332020, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 1442014, 1, 1, 0, 350});
        datas.add(new int[]{9410003, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410003, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410003, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410003, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410003, 4000201, 1, 1, 0, 600000});
        datas.add(new int[]{9410003, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9410003, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9410003, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9410003, 4031296, 1, 1, 4010, 50000});

        //폭주족 원숭이
        datas.add(new int[]{9410004, 0, 125, 185, 0, 700000});
        datas.add(new int[]{9410004, 1002208, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1041088, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1061087, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1072124, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1082080, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 1382006, 1, 1, 0, 350});
        datas.add(new int[]{9410004, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410004, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410004, 4000202, 1, 1, 0, 600000});
        datas.add(new int[]{9410004, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9410004, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 4010002, 1, 1, 0, 1000});
        datas.add(new int[]{9410004, 4031296, 1, 1, 4010, 50000});

        //레드 버블티
        datas.add(new int[]{9410005, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9410005, 1002151, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1032012, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1041076, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1061071, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1302004, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1302010, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1302016, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 1312005, 1, 1, 0, 350});
        datas.add(new int[]{9410005, 2040004, 1, 1, 0, 70});
        datas.add(new int[]{9410005, 2040501, 1, 1, 0, 70});
        datas.add(new int[]{9410005, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410005, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410005, 4000254, 1, 1, 0, 600000});
        datas.add(new int[]{9410005, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9410005, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9410005, 4020000, 1, 1, 0, 150});

        //옐로우 버블티
        datas.add(new int[]{9410006, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9410006, 1050011, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1072127, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1302017, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1332020, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 1402010, 1, 1, 0, 350});
        datas.add(new int[]{9410006, 2040401, 1, 1, 0, 70});
        datas.add(new int[]{9410006, 2044701, 1, 1, 0, 70});
        datas.add(new int[]{9410006, 2048004, 1, 1, 0, 70});
        datas.add(new int[]{9410006, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410006, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410006, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9410006, 4000255, 1, 1, 0, 600000});
        datas.add(new int[]{9410006, 4006001, 1, 1, 0, 600});
        datas.add(new int[]{9410006, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9410006, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9410006, 4020004, 1, 1, 0, 150});

        //그린 버블티
        datas.add(new int[]{9410007, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9410007, 1002038, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002098, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002136, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002172, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1002182, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1040083, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1041050, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1060072, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1061046, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1072103, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 1472015, 1, 1, 0, 350});
        datas.add(new int[]{9410007, 2040704, 1, 1, 0, 70});
        datas.add(new int[]{9410007, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9410007, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9410007, 2070008, 1, 1, 0, 40});
        datas.add(new int[]{9410007, 4000256, 1, 1, 0, 600000});
        datas.add(new int[]{9410007, 4010002, 1, 1, 0, 1000});

        //예티 인형자판기
        datas.add(new int[]{9410008, 0, 135, 200, 0, 700000});
        datas.add(new int[]{9410008, 1002135, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1002141, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1002169, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1032018, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1041086, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1061085, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1082084, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1382001, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1402002, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 1402003, 1, 1, 0, 350});
        datas.add(new int[]{9410008, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410008, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410008, 2040804, 1, 1, 0, 70});
        datas.add(new int[]{9410008, 2043201, 1, 1, 0, 70});
        datas.add(new int[]{9410008, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9410008, 4031352, 1, 1, 0, 80000});

        //예티 인형
        datas.add(new int[]{9410009, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410009, 4000257, 1, 1, 0, 600000});

        //주니어페페 인형자판기
        datas.add(new int[]{9410010, 0, 135, 200, 0, 700000});
        datas.add(new int[]{9410010, 1032008, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1032018, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1040085, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1072090, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1332016, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1372007, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1412004, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1432004, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1462004, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1462006, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 1472013, 1, 1, 0, 350});
        datas.add(new int[]{9410010, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9410010, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9410010, 2040516, 1, 1, 0, 70});

        //주니어페페 인형
        datas.add(new int[]{9410011, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9410011, 4000258, 1, 1, 0, 600000});

        //인형뽑기 기계
        datas.add(new int[]{9410013, 0, 175, 250, 0, 700000});
        datas.add(new int[]{9410013, 1032021, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1051044, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1051045, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1092002, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1102016, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1322016, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1332003, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1332019, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1412007, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 1472022, 1, 1, 0, 350});
        datas.add(new int[]{9410013, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9410013, 2002003, 1, 1, 0, 1000});
        datas.add(new int[]{9410013, 2043801, 1, 1, 0, 70});
        datas.add(new int[]{9410013, 2044601, 1, 1, 0, 70});
        datas.add(new int[]{9410013, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9410013, 4000259, 1, 1, 0, 600000});
        datas.add(new int[]{9410013, 4006001, 1, 1, 0, 600});

        //포장마차
        datas.add(new int[]{9410015, 0, 3500, 5000, 0, 700000});
        datas.add(new int[]{9410015, 2000004, 3, 6, 0, 350000});
        datas.add(new int[]{9410015, 2000005, 1, 5, 0, 350000});
        datas.add(new int[]{9410015, 4031354, 1, 1, 4013, 1000000});

        //두꺼비
        datas.add(new int[]{9420000, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9420000, 1002164, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1032010, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1040062, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1082042, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1092018, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1302004, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1432002, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 1462000, 1, 1, 0, 350});
        datas.add(new int[]{9420000, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 2040601, 1, 1, 0, 70});
        datas.add(new int[]{9420000, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420000, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420000, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9420000, 4000246, 1, 1, 0, 600000});
        datas.add(new int[]{9420000, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 4010002, 1, 1, 0, 1000});
        datas.add(new int[]{9420000, 4131010, 1, 1, 0, 100});

        //개구리
        datas.add(new int[]{9420001, 0, 30, 50, 0, 700000});
        datas.add(new int[]{9420001, 1002019, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1060002, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1061014, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1072023, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1332000, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1402001, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1412001, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1432001, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 1442012, 1, 1, 0, 350});
        datas.add(new int[]{9420001, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9420001, 2040601, 1, 1, 0, 70});
        datas.add(new int[]{9420001, 2041010, 1, 1, 0, 70});
        datas.add(new int[]{9420001, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420001, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420001, 4000247, 1, 1, 0, 600000});
        datas.add(new int[]{9420001, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9420001, 4020005, 1, 1, 0, 150});

        //구렁이
        datas.add(new int[]{9420002, 0, 150, 200, 0, 700000});
        datas.add(new int[]{9420002, 1002100, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1002218, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1002246, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1041080, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1041094, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1050056, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1051052, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1061079, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1061093, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1072155, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1072161, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1072164, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1082067, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1082087, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1082088, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1092011, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1102018, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1332017, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1472020, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1472023, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 1472025, 1, 1, 0, 350});
        datas.add(new int[]{9420002, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 2000006, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 4000248, 1, 1, 0, 600000});
        datas.add(new int[]{9420002, 4000249, 1, 1, 0, 600000});
        datas.add(new int[]{9420002, 4010004, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9420002, 4020000, 1, 1, 0, 150});

        //빨간 도마뱀
        datas.add(new int[]{9420003, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9420003, 1002025, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1002093, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1002185, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1050038, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072108, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072116, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072120, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1072126, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1082072, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1092014, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 1302009, 1, 1, 0, 350});
        datas.add(new int[]{9420003, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9420003, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420003, 2040001, 1, 1, 0, 70});
        datas.add(new int[]{9420003, 2040704, 1, 1, 0, 70});
        datas.add(new int[]{9420003, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420003, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420003, 4000251, 1, 1, 0, 600000});
        datas.add(new int[]{9420003, 4010003, 1, 1, 0, 1000});

        //노란 도마뱀
        datas.add(new int[]{9420004, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9420004, 1002034, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1002151, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1032000, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1040059, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1040079, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1060045, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1060069, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1061054, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 1382017, 1, 1, 0, 350});
        datas.add(new int[]{9420004, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420004, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420004, 4000021, 1, 1, 0, 600000});
        datas.add(new int[]{9420004, 4000250, 1, 1, 0, 600000});
        datas.add(new int[]{9420004, 4010002, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9420004, 4031388, 1, 1, 0, 500000});

        //흰 닭
        datas.add(new int[]{9420005, 0, 40, 65, 0, 700000});
        datas.add(new int[]{9420005, 1040022, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1040074, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1041032, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1060031, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1060063, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1092007, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1322000, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1372001, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1402009, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 1432000, 1, 1, 0, 350});
        datas.add(new int[]{9420005, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9420005, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9420005, 2043101, 1, 1, 0, 70});
        datas.add(new int[]{9420005, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9420005, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9420005, 4000252, 1, 1, 0, 600000});
        datas.add(new int[]{9420005, 4000253, 1, 1, 0, 600000});
        datas.add(new int[]{9420005, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9420005, 4020004, 1, 1, 0, 150});

        //닭
        datas.add(new int[]{9600001, 0, 35, 50, 0, 700000});
        datas.add(new int[]{9600001, 1002051, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1041030, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1061027, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1072062, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1082016, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 1312003, 1, 1, 0, 350});
        datas.add(new int[]{9600001, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9600001, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600001, 2002002, 1, 1, 0, 1000});
        datas.add(new int[]{9600001, 2040401, 1, 1, 0, 70});
        datas.add(new int[]{9600001, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600001, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600001, 4000187, 1, 1, 0, 600000});
        datas.add(new int[]{9600001, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600001, 4020000, 1, 1, 0, 150});
        datas.add(new int[]{9600001, 4020001, 1, 1, 0, 150});

        //오리
        datas.add(new int[]{9600002, 0, 40, 55, 0, 700000});
        datas.add(new int[]{9600002, 1002104, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1041028, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1061026, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1072051, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1082037, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1302002, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 1442001, 1, 1, 0, 350});
        datas.add(new int[]{9600002, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 2002003, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 2040601, 1, 1, 0, 70});
        datas.add(new int[]{9600002, 2040801, 1, 1, 0, 70});
        datas.add(new int[]{9600002, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600002, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600002, 4000188, 1, 1, 0, 600000});
        datas.add(new int[]{9600002, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600002, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9600002, 4020002, 1, 1, 0, 150});

        //양
        datas.add(new int[]{9600003, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9600003, 1002119, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1040050, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1060039, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1072073, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1082006, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1322003, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 1432002, 1, 1, 0, 350});
        datas.add(new int[]{9600003, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9600003, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600003, 2041007, 1, 1, 0, 70});
        datas.add(new int[]{9600003, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600003, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600003, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9600003, 4000021, 1, 1, 0, 600000});
        datas.add(new int[]{9600003, 4000189, 1, 1, 0, 600000});
        datas.add(new int[]{9600003, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600003, 4020005, 1, 1, 0, 150});
        datas.add(new int[]{9600003, 4020006, 1, 1, 0, 150});

        //염소
        datas.add(new int[]{9600004, 0, 65, 85, 0, 700000});
        datas.add(new int[]{9600004, 1002150, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1051012, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1072102, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1082051, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1372001, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 1422008, 1, 1, 0, 350});
        datas.add(new int[]{9600004, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 2002004, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 2041022, 1, 1, 0, 70});
        datas.add(new int[]{9600004, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600004, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600004, 4000021, 1, 1, 0, 600000});
        datas.add(new int[]{9600004, 4000190, 1, 1, 0, 600000});
        datas.add(new int[]{9600004, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600004, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9600004, 4010002, 1, 1, 0, 1000});

        //흑염소
        datas.add(new int[]{9600005, 0, 85, 110, 0, 700000});
        datas.add(new int[]{9600005, 1002023, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1032004, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1051025, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1072108, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1082071, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 1412005, 1, 1, 0, 350});
        datas.add(new int[]{9600005, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600005, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600005, 2041001, 1, 1, 0, 70});
        datas.add(new int[]{9600005, 2043201, 1, 1, 0, 70});
        datas.add(new int[]{9600005, 2044501, 1, 1, 0, 70});
        datas.add(new int[]{9600005, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600005, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600005, 4000021, 1, 1, 0, 600000});
        datas.add(new int[]{9600005, 4000191, 1, 1, 0, 600000});
        datas.add(new int[]{9600005, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600005, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9600005, 4020003, 1, 1, 0, 150});

        //소
        datas.add(new int[]{9600006, 0, 70, 95, 0, 700000});
        datas.add(new int[]{9600006, 1002037, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1041068, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1061063, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1072064, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1082075, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1382018, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 1402010, 1, 1, 0, 350});
        datas.add(new int[]{9600006, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600006, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600006, 2002005, 1, 1, 0, 1000});
        datas.add(new int[]{9600006, 2040901, 1, 1, 0, 70});
        datas.add(new int[]{9600006, 2044301, 1, 1, 0, 70});
        datas.add(new int[]{9600006, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600006, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600006, 4000021, 1, 1, 0, 600000});
        datas.add(new int[]{9600006, 4000192, 1, 1, 0, 600000});
        datas.add(new int[]{9600006, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600006, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9600006, 4020004, 1, 1, 0, 150});

        //쟁기소
        datas.add(new int[]{9600007, 0, 85, 110, 0, 700000});
        datas.add(new int[]{9600007, 1002163, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1032008, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1051009, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1072116, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1082023, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1332014, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1442015, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 1452007, 1, 1, 0, 350});
        datas.add(new int[]{9600007, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600007, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600007, 2040704, 1, 1, 0, 70});
        datas.add(new int[]{9600007, 2043801, 1, 1, 0, 70});
        datas.add(new int[]{9600007, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600007, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600007, 4000021, 1, 1, 0, 600000});
        datas.add(new int[]{9600007, 4000193, 1, 1, 0, 600000});
        datas.add(new int[]{9600007, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600007, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9600007, 4020007, 1, 1, 0, 150});

        //검은 양
        datas.add(new int[]{9600008, 0, 80, 105, 0, 700000});
        datas.add(new int[]{9600008, 1002175, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1041086, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1061085, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1072120, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1082062, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1312006, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 1472016, 1, 1, 0, 350});
        datas.add(new int[]{9600008, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9600008, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9600008, 2040707, 1, 1, 0, 70});
        datas.add(new int[]{9600008, 2043301, 1, 1, 0, 70});
        datas.add(new int[]{9600008, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9600008, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9600008, 2070002, 1, 1, 0, 40});
        datas.add(new int[]{9600008, 4000021, 1, 1, 0, 600000});
        datas.add(new int[]{9600008, 4000194, 1, 1, 0, 600000});
        datas.add(new int[]{9600008, 4003004, 1, 1, 0, 100000});
        datas.add(new int[]{9600008, 4010004, 1, 1, 0, 1000});
        datas.add(new int[]{9600008, 4020000, 1, 1, 0, 150});

        //대왕지네
        datas.add(new int[]{9600009, 0, 1000, 1500, 0, 700000});
        datas.add(new int[]{9600009, 4031227, 1, 1, 4103, 1000000});
        datas.add(new int[]{9600010, 0, 1000, 1500, 0, 700000});
        datas.add(new int[]{9600010, 4031227, 1, 1, 4103, 1000000});

        //CokeTown
        //코-크 돼지
        datas.add(new int[]{9500143, 0, 75, 100, 0, 700000});
        datas.add(new int[]{9500143, 1002152, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1002164, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1002183, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1040029, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1040072, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1051011, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1060020, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1060061, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 1092008, 1, 1, 0, 350});
        datas.add(new int[]{9500143, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500143, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500143, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500143, 2043200, 1, 1, 0, 70});
        datas.add(new int[]{9500143, 2043300, 1, 1, 0, 70});
        datas.add(new int[]{9500143, 2043700, 1, 1, 0, 70});
        datas.add(new int[]{9500143, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500143, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500143, 4000210, 1, 1, 0, 600000});
        datas.add(new int[]{9500143, 4006001, 1, 1, 0, 600});
        datas.add(new int[]{9500143, 4010000, 1, 1, 0, 1000});
        datas.add(new int[]{9500143, 4020006, 1, 1, 0, 150});
        datas.add(new int[]{9500143, 4030012, 1, 1, 0, 15000});

        //코-크 달팽이
        datas.add(new int[]{9500144, 0, 20, 30, 0, 700000});
        datas.add(new int[]{9500144, 1002001, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1002043, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1002132, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1041018, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1061013, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 1092003, 1, 1, 0, 350});
        datas.add(new int[]{9500144, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9500144, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500144, 2040501, 1, 1, 0, 70});
        datas.add(new int[]{9500144, 2040705, 1, 1, 0, 70});
        datas.add(new int[]{9500144, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500144, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500144, 4000214, 1, 1, 0, 600000});
        datas.add(new int[]{9500144, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9500144, 4010003, 1, 1, 0, 1000});
        datas.add(new int[]{9500144, 4030009, 1, 1, 0, 15000});
        datas.add(new int[]{9500144, 4030012, 1, 1, 0, 15000});

        //코-크 씰
        datas.add(new int[]{9500145, 0, 115, 165, 0, 700000});
        datas.add(new int[]{9500145, 1002048, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1002176, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1040082, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1041078, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1060071, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1061077, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 1372012, 1, 1, 0, 350});
        datas.add(new int[]{9500145, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 2002005, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500145, 2041023, 1, 1, 0, 70});
        datas.add(new int[]{9500145, 2043002, 1, 1, 0, 70});
        datas.add(new int[]{9500145, 2043102, 1, 1, 0, 70});
        datas.add(new int[]{9500145, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500145, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500145, 4000211, 1, 1, 0, 600000});
        datas.add(new int[]{9500145, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9500145, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500145, 4030012, 1, 1, 0, 15000});
        datas.add(new int[]{9500145, 4131005, 1, 1, 0, 100});

        //플레이 씰
        datas.add(new int[]{9500146, 0, 100, 150, 0, 700000});
        datas.add(new int[]{9500146, 1041068, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1061063, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1072109, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1072116, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1082066, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1082082, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1372000, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 1402007, 1, 1, 0, 350});
        datas.add(new int[]{9500146, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500146, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500146, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500146, 2041008, 1, 1, 0, 70});
        datas.add(new int[]{9500146, 2041017, 1, 1, 0, 70});
        datas.add(new int[]{9500146, 2041020, 1, 1, 0, 70});
        datas.add(new int[]{9500146, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500146, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500146, 4000212, 1, 1, 0, 600000});
        datas.add(new int[]{9500146, 4010005, 1, 1, 0, 1000});
        datas.add(new int[]{9500146, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9500146, 4030012, 1, 1, 0, 15000});
        datas.add(new int[]{9500146, 4130010, 1, 1, 0, 100});

        //예티와 코-크텀프
        datas.add(new int[]{9500147, 0, 200, 300, 0, 700000});
        datas.add(new int[]{9500147, 1002271, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1002275, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072137, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072145, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072148, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1072151, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082095, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082099, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082104, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1082107, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1372014, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1382006, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 1402012, 1, 1, 0, 350});
        datas.add(new int[]{9500147, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9500147, 2000006, 1, 1, 0, 1000});
        datas.add(new int[]{9500147, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500147, 2040302, 1, 1, 0, 70});
        datas.add(new int[]{9500147, 2040502, 1, 1, 0, 70});
        datas.add(new int[]{9500147, 2040505, 1, 1, 0, 70});
        datas.add(new int[]{9500147, 2050000, 1, 1, 0, 35000});
        datas.add(new int[]{9500147, 4000213, 1, 1, 0, 600000});
        datas.add(new int[]{9500147, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9500147, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9500147, 4020008, 1, 1, 0, 150});
        datas.add(new int[]{9500147, 4030012, 1, 1, 0, 15000});

        //이글루 터틀
        datas.add(new int[]{9500148, 0, 130, 185, 0, 700000});
        datas.add(new int[]{9500148, 1002004, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1002155, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1002161, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1002183, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1040095, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1041089, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1060084, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1061088, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1082087, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 1082089, 1, 1, 0, 350});
        datas.add(new int[]{9500148, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500148, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500148, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500148, 2040802, 1, 1, 0, 70});
        datas.add(new int[]{9500148, 2040805, 1, 1, 0, 70});
        datas.add(new int[]{9500148, 2040900, 1, 1, 0, 70});
        datas.add(new int[]{9500148, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500148, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500148, 4000218, 1, 1, 0, 600000});
        datas.add(new int[]{9500148, 4006000, 1, 1, 0, 600});
        datas.add(new int[]{9500148, 4010001, 1, 1, 0, 1000});
        datas.add(new int[]{9500148, 4020001, 1, 1, 0, 150});
        datas.add(new int[]{9500148, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9500148, 4030012, 1, 1, 0, 15000});

        //코-크 골렘
        datas.add(new int[]{9500149, 0, 150, 200, 0, 700000});
        datas.add(new int[]{9500149, 1041084, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1051025, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1061083, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1072123, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1072136, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1082010, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1082066, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1452004, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 1462008, 1, 1, 0, 350});
        datas.add(new int[]{9500149, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2002003, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 2040708, 1, 1, 0, 5000});
        datas.add(new int[]{9500149, 2040706, 1, 1, 0, 70});
        datas.add(new int[]{9500149, 2040702, 1, 1, 0, 70});
        datas.add(new int[]{9500149, 2040705, 1, 1, 0, 70});
        datas.add(new int[]{9500149, 4000219, 1, 1, 0, 600000});
        datas.add(new int[]{9500149, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9500149, 4020004, 1, 1, 0, 150});
        datas.add(new int[]{9500149, 4030012, 1, 1, 0, 15000});

        //아이스 골렘
        datas.add(new int[]{9500150, 0, 185, 250, 0, 700000});
        datas.add(new int[]{9500150, 1002242, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1002247, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1002267, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1040090, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1041094, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1060079, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1061093, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1082095, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1082098, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1082106, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1302010, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1312018, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 1332016, 1, 1, 0, 350});
        datas.add(new int[]{9500150, 2000004, 1, 1, 0, 1000});
        datas.add(new int[]{9500150, 2000006, 1, 1, 0, 1000});
        datas.add(new int[]{9500150, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500150, 2040514, 1, 1, 0, 70});
        datas.add(new int[]{9500150, 2040515, 1, 1, 0, 70});
        datas.add(new int[]{9500150, 2040602, 1, 1, 0, 70});
        datas.add(new int[]{9500150, 4000220, 1, 1, 0, 600000});
        datas.add(new int[]{9500150, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500150, 4020008, 1, 1, 0, 150});
        datas.add(new int[]{9500150, 4030012, 1, 1, 0, 15000});

        //코-크 슬라임
        datas.add(new int[]{9500151, 0, 30, 50, 0, 700000});
        datas.add(new int[]{9500151, 1040012, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1041063, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1051000, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1060010, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1082000, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1082029, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1402018, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1432008, 1, 1, 0, 350});
        datas.add(new int[]{9500151, 1501000, 1, 1, 0, 350}); //해적템
        datas.add(new int[]{9500151, 2000000, 1, 1, 0, 1000});
        datas.add(new int[]{9500151, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500151, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500151, 2040703, 1, 1, 0, 70});
        datas.add(new int[]{9500151, 2040800, 1, 1, 0, 70});
        datas.add(new int[]{9500151, 2041008, 1, 1, 0, 70});
        datas.add(new int[]{9500151, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500151, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500151, 4000209, 1, 1, 0, 600000});
        datas.add(new int[]{9500151, 4006000, 1, 1, 0, 600});
        datas.add(new int[]{9500151, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500151, 4020005, 1, 1, 0, 150});
        datas.add(new int[]{9500151, 4030012, 1, 1, 0, 15000});

        //코-크 버섯
        datas.add(new int[]{9500152, 0, 40, 65, 0, 700000});
        datas.add(new int[]{9500152, 1002146, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1040020, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1060015, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1072019, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1072025, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1302003, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 1312005, 1, 1, 0, 350});
        datas.add(new int[]{9500152, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9500152, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500152, 2002002, 1, 1, 0, 1000});
        datas.add(new int[]{9500152, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500152, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500152, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500152, 4000221, 1, 1, 0, 600000});
        datas.add(new int[]{9500152, 4020006, 1, 1, 0, 150});
        datas.add(new int[]{9500152, 4020007, 1, 1, 0, 150});
        datas.add(new int[]{9500152, 4030012, 1, 1, 0, 15000});

        //코-크텀프
        datas.add(new int[]{9500153, 0, 50, 75, 0, 700000});
        datas.add(new int[]{9500153, 1040022, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1040026, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1041032, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1050001, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1050005, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1051012, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1060019, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1062006, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 1082026, 1, 1, 0, 350});
        datas.add(new int[]{9500153, 2000001, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2002001, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2002005, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500153, 2044202, 1, 1, 0, 70});
        datas.add(new int[]{9500153, 2044302, 1, 1, 0, 70});
        datas.add(new int[]{9500153, 2044602, 1, 1, 0, 70});
        datas.add(new int[]{9500153, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500153, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500153, 4000216, 1, 1, 0, 600000});
        datas.add(new int[]{9500153, 4010006, 1, 1, 0, 1000});
        datas.add(new int[]{9500153, 4020001, 1, 1, 0, 150});
        datas.add(new int[]{9500153, 4020002, 1, 1, 0, 150});
        datas.add(new int[]{9500153, 4030012, 1, 1, 0, 15000});

        //코-크텀프 라이트
        datas.add(new int[]{9500154, 0, 60, 90, 0, 700000});
        datas.add(new int[]{9500154, 1072003, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1072034, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1072072, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1082045, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1082068, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1322014, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 1332009, 1, 1, 0, 350});
        datas.add(new int[]{9500154, 2000002, 1, 1, 0, 1000});
        datas.add(new int[]{9500154, 2000003, 1, 1, 0, 1000});
        datas.add(new int[]{9500154, 2000002, 1, 1, 0, 35000}); //41에 없는템
        datas.add(new int[]{9500154, 2022075, 1, 1, 0, 5000});
        datas.add(new int[]{9500154, 2043800, 1, 1, 0, 70});
        datas.add(new int[]{9500154, 2044001, 1, 1, 0, 70});
        datas.add(new int[]{9500154, 2044100, 1, 1, 0, 70});
        datas.add(new int[]{9500154, 2060000, 20, 30, 0, 8000});
        datas.add(new int[]{9500154, 2061000, 20, 30, 0, 8000});
        datas.add(new int[]{9500154, 4000217, 1, 1, 0, 350000});
        datas.add(new int[]{9500154, 4010004, 1, 1, 0, 1000});
        datas.add(new int[]{9500154, 4020003, 1, 1, 0, 150});
        datas.add(new int[]{9500154, 4030012, 1, 1, 0, 15000});
        datas.add(new int[]{9500154, 4131003, 1, 1, 0, 100});

        //미니 두꺼비
        datas.add(new int[]{9400407, 4000343, 1, 1, 0, 1000000});

        //대두꺼비 가엘
        datas.add(new int[]{9400409, 2049100, 1, 1, 0, 30000});
        datas.add(new int[]{9400409, 1442057, 1, 1, 0, 80000});
        datas.add(new int[]{9400409, 1472054, 1, 1, 0, 80000});
        datas.add(new int[]{9400409, 1382046, 1, 1, 0, 80000});
        datas.add(new int[]{9400409, 1382048, 1, 1, 0, 80000});
        datas.add(new int[]{9400409, 1452026, 1, 1, 0, 80000});
        datas.add(new int[]{9400409, 1302106, 1, 1, 0, 80000});
        datas.add(new int[]{9400409, 1113180, 1, 1, 0, 50000});
        datas.add(new int[]{9400409, 1003976, 1, 1, 0, 20000});
        datas.add(new int[]{9400409, 2046309, 1, 1, 0, 30000});
        datas.add(new int[]{9400409, 2046314, 1, 1, 0, 30000});
        datas.add(new int[]{9400409, 2046310, 1, 1, 0, 15000});
        datas.add(new int[]{9400409, 2046315, 1, 1, 0, 15000});
        datas.add(new int[]{9400409, 2430144, 1, 1, 0, 40000});
        datas.add(new int[]{9400409, 2049116, 1, 1, 0, 3334});

        //거리의 슬라임
        datas.add(new int[]{9400538, 0, 54, 61, 0, 700000});

        //도심의 균상종
        datas.add(new int[]{9400539, 0, 56, 64, 0, 700000});

        //킬라비
        datas.add(new int[]{9400540, 0, 68, 74, 0, 700000});

        //파이어 터스크
        datas.add(new int[]{9400542, 0, 78, 85, 0, 700000});

        //일렉트로펀트
        datas.add(new int[]{9400543, 0, 91, 94, 0, 700000});
        datas.add(new int[]{9400543, 4031680, 1, 1, 0, 1000000});
        datas.add(new int[]{9400543, 4031675, 1, 1, 4917, 1000000});

        //그리폰
        datas.add(new int[]{9400544, 0, 121, 135, 0, 700000});

        //늑대거미
        datas.add(new int[]{9400545, 0, 330, 370, 0, 700000});

        //I.AM.ROBOT
        datas.add(new int[]{9400546, 0, 81, 85, 0, 700000});
        datas.add(new int[]{9400546, 4031681, 1, 1, 0, 1000000});
        datas.add(new int[]{9400546, 4031674, 1, 1, 4916, 1000000});

        //부머
        datas.add(new int[]{9400547, 0, 57, 71, 0, 700000});
        datas.add(new int[]{9400547, 4000391, 1, 1, 0, 700000});

        //마이티 메이플 이터
        datas.add(new int[]{9400548, 0, 60, 64, 0, 700000});

        //생일 초
        datas.add(new int[]{9400601, 0, 55, 66, 0, 700000});
        datas.add(new int[]{9400601, 2070001, 1, 1, 0, 500});
        datas.add(new int[]{9400601, 2070009, 1, 1, 0, 500});

        //딸기 케이크
        datas.add(new int[]{9400602, 0, 58, 70, 0, 700000});
        datas.add(new int[]{9400602, 2070002, 1, 1, 0, 500});
        datas.add(new int[]{9400602, 2022279, 1, 1, 0, 1000});

        //화난 딸기 케이크
        datas.add(new int[]{9400603, 0, 92, 96, 0, 700000});
        datas.add(new int[]{9400603, 2070003, 1, 1, 0, 500});
        datas.add(new int[]{9400603, 2022279, 1, 2, 0, 1000});

        //화려한 초
        datas.add(new int[]{9400604, 0, 96, 100, 0, 700000});
        datas.add(new int[]{9400604, 2070010, 1, 1, 0, 500});
        datas.add(new int[]{9400604, 2330001, 1, 1, 0, 500});
        datas.add(new int[]{9400604, 2022279, 1, 1, 0, 1000});

        //초콜릿 케이크
        datas.add(new int[]{9400605, 0, 140, 190, 0, 700000});
        datas.add(new int[]{9400605, 2070004, 1, 1, 0, 500});
        datas.add(new int[]{9400605, 2330002, 1, 1, 0, 500});
        datas.add(new int[]{9400605, 2022279, 2, 4, 0, 1000});

        //커다란 케이크
        datas.add(new int[]{9400606, 0, 180, 220, 0, 700000});
        datas.add(new int[]{9400606, 2070007, 1, 1, 0, 200});
        datas.add(new int[]{9400606, 2330003, 1, 1, 0, 200});
        datas.add(new int[]{9400606, 2020016, 1, 2, 0, 1000});
    }

    private static void addMasteryBook(List<int[]> datas) {
        //블레이드 제작의 촉진제
        datas.add(new int[]{5100004, 4130023, 1, 1, 0, 1000});//삼미호 

        /*공통 북*/
        //메이플용사 20
        //datas.add(new int[]{8140701, 2290096, 1, 1, 0, 60});//레드 드래곤터틀
        //datas.add(new int[]{8220011, 2290096, 1, 1, 0, 50000});//아우프헤벤
        //크로스보우 엑스퍼트 20
        datas.add(new int[]{7130001, 2290066, 1, 1, 0, 60});//불독
        datas.add(new int[]{8140002, 2290066, 1, 1, 0, 60});//블러드 하프
        datas.add(new int[]{8200010, 2290066, 1, 1, 0, 60});//망각의 신관
        datas.add(new int[]{8180001, 2290066, 1, 1, 0, 100000});//그리프

        //크로스보우 엑스퍼트 30
        datas.add(new int[]{6130207, 2290067, 1, 1, 0, 60});//원공
        datas.add(new int[]{7130200, 2290067, 1, 1, 0, 60});//웨어울프
        datas.add(new int[]{7130003, 2290067, 1, 1, 0, 60});//듀얼 비틀
        datas.add(new int[]{8200010, 2290067, 1, 1, 0, 60});//망각의 신관

        /*전사 북*/
        //돌진 20
        datas.add(new int[]{8140700, 2290004, 1, 1, 0, 60});//블루 드래곤터틀
        datas.add(new int[]{8150101, 2290004, 1, 1, 0, 60});//콜드샤크
        datas.add(new int[]{8220015, 2290004, 1, 1, 0, 50000});//니벨룽겐 전함
        datas.add(new int[]{8150000, 2290004, 1, 1, 0, 50000});//크림슨발록

        //돌진 30
        datas.add(new int[]{7120103, 2290005, 1, 1, 0, 60});//레드 슬라임
        datas.add(new int[]{8200003, 2290005, 1, 1, 0, 60});//추억의 수호병
        datas.add(new int[]{8180000, 2290005, 1, 1, 0, 50000});//마뇽
        datas.add(new int[]{5220004, 2290005, 1, 1, 0, 50000});//대왕지네

        //스탠스 20
        datas.add(new int[]{7110301, 2290006, 1, 1, 0, 60});//호문쿨루
        datas.add(new int[]{8190000, 2290006, 1, 1, 0, 60});//뉴트주니어
        datas.add(new int[]{6130208, 2290006, 1, 1, 0, 60});//크루
        datas.add(new int[]{7130104, 2290006, 1, 1, 0, 60});//캡틴
        datas.add(new int[]{8220004, 2290006, 1, 1, 0, 50000});//도도

        //스탠스 30
        datas.add(new int[]{8140500, 2290007, 1, 1, 0, 60});//파이어독
        datas.add(new int[]{8105003, 2290007, 1, 1, 0, 60});//AF형 안드로이드
        datas.add(new int[]{7130004, 2290007, 1, 1, 0, 60});//헹키
        datas.add(new int[]{8220015, 2290007, 1, 1, 0, 50000});//니벨룽겐 전함

        //비홀더스 버프 20
        datas.add(new int[]{8140101, 2290205, 1, 1, 0, 60});//검은 켄타우로스

        //도적 북
        //페이크20
        datas.add(new int[]{4230108, 2290076, 1, 1, 0, 60});//주니어 불독
        datas.add(new int[]{7140000, 2290076, 1, 1, 0, 60});//파이렛
        datas.add(new int[]{6130207, 2290076, 1, 1, 0, 60});//원공
        datas.add(new int[]{8220005, 2290076, 1, 1, 0, 110000});//릴리노흐

        //레지스탕스 마스터리 북
        //배틀메이지
        datas.add(new int[]{6300100, 2290226, 1, 1, 0, 60});//어드밴스드 다크오라 20 버푼
        datas.add(new int[]{7130600, 2290226, 1, 1, 0, 60});//어드밴스드 다크오라 20 호브
        datas.add(new int[]{8140103, 2290226, 1, 1, 0, 60});//어드밴스드 다크오라 20 푸른 켄타우로스
        datas.add(new int[]{8141100, 2290226, 1, 1, 0, 60});//어드밴스드 다크오라 20 기간틱 바이킹
        datas.add(new int[]{8500002, 2290226, 1, 1, 0, 110000});//어드밴스드 다크오라 20 파풀라투스

        //datas.add(new int[]{8105005, 2290227, 1, 1, 0, 60});//어드밴스드 다크오라 30 광석 이터
        //datas.add(new int[]{7140000, 2290227, 1, 1, 0, 60});//어드밴스드 다크오라 30 파이렛
        //datas.add(new int[]{8142000, 2290227, 1, 1, 0, 60});//어드밴스드 다크오라 30 팬텀워치
        datas.add(new int[]{8810018, 2290227, 1, 1, 0, 80000});//어드밴스드 다크오라 30 혼테일8%/카텔16%

        datas.add(new int[]{8141000, 2290228, 1, 1, 0, 60});//어드밴스드 옐로우오라 20 바이킹
        datas.add(new int[]{8140600, 2290228, 1, 1, 0, 60});//어드밴스드 옐로우오라 20 본피쉬
        datas.add(new int[]{6110300, 2290228, 1, 1, 0, 60});//어드밴스드 옐로우오라 20 호문
        datas.add(new int[]{8200012, 2290228, 1, 1, 0, 60});//어드밴스드 옐로우오라 20 망각의 수호대장

        datas.add(new int[]{6400000, 2290229, 1, 1, 0, 60});//어드밴스드 옐로우오라 30 다크 예티
        datas.add(new int[]{7110301, 2290229, 1, 1, 0, 60});//어드밴스드 옐로우오라 30 호문쿨루
        datas.add(new int[]{8140600, 2290229, 1, 1, 0, 60});//어드밴스드 옐로우오라 30 본피쉬

        datas.add(new int[]{8105003, 2290230, 1, 1, 0, 60});//피니쉬 블로우 20 AF형 안드로이드
        datas.add(new int[]{8140600, 2290230, 1, 1, 0, 60});//피니쉬 블로우 20 본피쉬
        datas.add(new int[]{8105005, 2290230, 1, 1, 0, 60});//피니쉬 블로우 20 광석 이터

        datas.add(new int[]{8140101, 2290231, 1, 1, 0, 60});//피니쉬 블로우 30 검은 켄타우로스
        datas.add(new int[]{8140103, 2290231, 1, 1, 0, 60});//피니쉬 블로우 30 푸른 켄타우로스
        datas.add(new int[]{6130200, 2290231, 1, 1, 0, 60});//피니쉬 블로우 30 버피

        datas.add(new int[]{7120104, 2290232, 1, 1, 0, 60});//싸이클론 20 실버 슬라임
        datas.add(new int[]{7130001, 2290232, 1, 1, 0, 60});//싸이클론 20 불독

        datas.add(new int[]{8150301, 2290233, 1, 1, 0, 60});//싸이클론 30 블루 와이번

        datas.add(new int[]{8140701, 2290234, 1, 1, 0, 60});//다크 제네시스 20 레드 드래곤터틀
        datas.add(new int[]{7130003, 2290234, 1, 1, 0, 60});//다크 제네시스 20 듀얼 비틀
        datas.add(new int[]{8105004, 2290234, 1, 1, 0, 60});//다크 제네시스 20 고장난 DF형 안드로이드

        datas.add(new int[]{7120103, 2290235, 1, 1, 0, 60});//다크 제네시스 30 레드 슬라임
        datas.add(new int[]{8200003, 2290235, 1, 1, 0, 60});//다크 제네시스 30 추억의 수호병

        datas.add(new int[]{8810018, 2290236, 1, 1, 0, 100000});//쉘터 20 혼테일

        //해적
        //윈드 부스터 20
        datas.add(new int[]{8140111, 2290108, 1, 1, 0, 60});//듀얼 버크 

        //에반
        datas.add(new int[]{7110301, 2290144, 1, 1, 0, 60});//호문쿨루
        datas.add(new int[]{8200004, 2290148, 1, 1, 0, 60});//추억의 수호대장
        datas.add(new int[]{8220004, 2290148, 1, 1, 0, 50000});//도도

        //와일드헌터
        //플래쉬 레인 20
        datas.add(new int[]{6130204, 2290238, 1, 1, 0, 60});//게비알
        datas.add(new int[]{8200003, 2290238, 1, 1, 0, 60});//추억의 수호병

        //플래쉬 레인 30
        //2290239
        datas.add(new int[]{8141100, 2290239, 1, 1, 0, 60});//플래쉬 레인 30 기간틱 바이킹
        //와일드 발칸 20
        datas.add(new int[]{6230400, 2290240, 1, 1, 0, 60});//소울테니
        datas.add(new int[]{7110301, 2290240, 1, 1, 0, 60});//호문클루
        datas.add(new int[]{7130004, 2290240, 1, 1, 0, 60});//헹키
        datas.add(new int[]{6110300, 2290240, 1, 1, 0, 60});//호문
        datas.add(new int[]{6110301, 2290240, 1, 1, 0, 60});//사이티
        datas.add(new int[]{8110300, 2290240, 1, 1, 0, 60});//호문스큘러
        datas.add(new int[]{8140600, 2290240, 1, 1, 0, 60});//본피쉬

        //와일드 발칸 30
        datas.add(new int[]{7130020, 2290241, 1, 1, 0, 60});//망둥이
        datas.add(new int[]{6130208, 2290241, 1, 1, 0, 60});//크루
        datas.add(new int[]{7130104, 2290241, 1, 1, 0, 60});//캡틴

        //소닉붐 20 
        datas.add(new int[]{8200004, 2290242, 1, 1, 0, 60});//추억의 수호대장

        //소닉붐 30 
        datas.add(new int[]{6400001, 2290243, 1, 1, 0, 60});//다크예티
        datas.add(new int[]{6400002, 2290243, 1, 1, 0, 60});//다크예티
        datas.add(new int[]{7130300, 2290243, 1, 1, 0, 60});//마스터 데스테니

        //캐미컬 쉘 20
        datas.add(new int[]{8140702, 2290244, 1, 1, 0, 60});//리스튼
        datas.add(new int[]{8200012, 2290244, 1, 1, 0, 60});//망각의 수호대장

        //비스트 폼 20
        datas.add(new int[]{8140002, 2290246, 1, 1, 0, 60});//하프
        datas.add(new int[]{8220011, 2290246, 1, 1, 0, 100000});//아우프헤벤

        //비스트 폼 30
        datas.add(new int[]{8810018, 2290247, 1, 1, 0, 100000});//혼테일
        datas.add(new int[]{8810118, 2290247, 1, 1, 0, 200000});//카오스 혼테일

        //bb전 기준 듀블
        datas.add(new int[]{8800002, 2280030, 1, 1, 0, 100000});//파이널 컷
        datas.add(new int[]{8800002, 2280031, 1, 1, 0, 100000});//사슬지옥

        datas.add(new int[]{3300008, 2290153, 1, 1, 0, 1000000});//슬래시 스톰20 총리대신
        datas.add(new int[]{4300013, 2290154, 1, 1, 0, 1000000});//토네이도 스핀20 락 스피릿
        datas.add(new int[]{6160003, 2290155, 1, 1, 0, 1000000});//미러 이미징 30 크세르크세스
        datas.add(new int[]{8200001, 2290155, 1, 1, 0, 60});//미러 이미징 30 추억의 사제 
        datas.add(new int[]{8200012, 2290155, 1, 1, 0, 60});//미러 이미징 30 망각의 수호대장
        datas.add(new int[]{9420513, 2290156, 1, 1, 0, 1000000});//플라잉 어썰터 20 캡틴 라타니카
        datas.add(new int[]{8200002, 2290156, 1, 1, 0, 60});//플라잉 어썰터 20 추억의 신관
        datas.add(new int[]{8150301, 2290156, 1, 1, 0, 60});//플라잉 어썰터 20 블루 와이번
        datas.add(new int[]{8180000, 2290156, 1, 1, 0, 50000});//플라잉 어썰터 20 마뇽
        datas.add(new int[]{8800002, 2290157, 1, 1, 0, 100000});//베놈 30 자쿰
        datas.add(new int[]{8200005, 2290157, 1, 1, 0, 60});//베놈 30 후회의 사제
        datas.add(new int[]{8200008, 2290158, 1, 1, 0, 60});//몬스터봄 30 후회의 수호대장
        datas.add(new int[]{8810018, 2290158, 1, 1, 0, 100000});//몬스터봄 30 혼테일 
        datas.add(new int[]{8220006, 2290159, 1, 1, 0, 50000});//써든레이드 30 라이카 
        datas.add(new int[]{8200017, 2290160, 1, 1, 0, 60});//더미이펙트 30 망각의 수호병
        datas.add(new int[]{8200018, 2290160, 1, 1, 0, 60});//더미이펙트 30 망각의 수호대장
        datas.add(new int[]{8810018, 2290160, 1, 1, 0, 100000});//더미이펙트 30 혼테일
        datas.add(new int[]{8810018, 2290161, 1, 1, 0, 100000});//쏜즈이펙트 30 혼테일

        //메카닉
        datas.add(new int[]{8500002, 2290283, 1, 1, 0, 50000});//파풀라투스레이저 블래스트 30
        datas.add(new int[]{8140102, 2290282, 1, 1, 0, 150});//붉은 켄타우로스레이저 블래스트 20
        datas.add(new int[]{8140110, 2290281, 1, 1, 0, 150});//듀얼 버크앰플 20
        datas.add(new int[]{8141100, 2290279, 1, 1, 0, 150});//기간틱 바이킹팩토리 20
        datas.add(new int[]{8150300, 2290280, 1, 1, 0, 150});//레드 와이번팩토리 30
        datas.add(new int[]{8200002, 2290276, 1, 1, 0, 150});//추억의 신관타이탄 30
        datas.add(new int[]{8105005, 2290275, 1, 1, 0, 150});//광석 이터타이탄20
        datas.add(new int[]{8190003, 2290284, 1, 1, 0, 150});//스켈레곤 세이프티 25
        datas.add(new int[]{8150301, 2290278, 1, 1, 0, 150});//블루 와이번미사일탱크 30
    }

    private static void addNeoTokyo(List<int[]> datas) {
        //애프터로드
        datas.add(new int[]{9400253, 4032155, 1, 1, 0, 1000000});
        datas.add(new int[]{9400253, 4032151, 1, 1, 4684, 10000});
        datas.add(new int[]{9400253, 4032152, 1, 1, 4685, 10000});
        datas.add(new int[]{9400253, 4032153, 1, 1, 4685, 10000});
        datas.add(new int[]{9400253, 4032154, 1, 1, 4685, 10000});
        datas.add(new int[]{9400253, 4032166, 1, 1, 0, 25});
        datas.add(new int[]{9400253, 4032181, 1, 1, 0, 5000});

        //오버로드
        datas.add(new int[]{9400254, 4032156, 1, 1, 0, 1000000});
        datas.add(new int[]{9400254, 4032151, 1, 1, 4684, 10000});
        datas.add(new int[]{9400254, 4032152, 1, 1, 4685, 10000});
        datas.add(new int[]{9400254, 4032153, 1, 1, 4685, 10000});
        datas.add(new int[]{9400254, 4032154, 1, 1, 4685, 10000});
        datas.add(new int[]{9400254, 4032181, 1, 1, 0, 5000});

        //프로토타입 로드
        datas.add(new int[]{9400255, 4032159, 1, 1, 0, 1000000});
        datas.add(new int[]{9400255, 4032158, 1, 1, 4688, 10000});
        datas.add(new int[]{9400255, 4032166, 1, 1, 0, 25});
        datas.add(new int[]{9400255, 4032181, 1, 1, 0, 5000});

        //마베리크 Typeα
        datas.add(new int[]{9400256, 4032160, 1, 1, 0, 1000000});
        datas.add(new int[]{9400256, 4032192, 1, 1, 4689, 50000});
        datas.add(new int[]{9400256, 4032164, 1, 1, 4695, 10000});
        datas.add(new int[]{9400256, 4032167, 1, 1, 0, 25});
        datas.add(new int[]{9400256, 4032181, 1, 1, 0, 5000});

        //마베리크 Typeγ
        datas.add(new int[]{9400257, 4032163, 1, 1, 0, 1000000});
        datas.add(new int[]{9400257, 4032192, 1, 1, 4689, 50000});
        datas.add(new int[]{9400257, 4032164, 1, 1, 4695, 10000});
        datas.add(new int[]{9400257, 4032167, 1, 1, 0, 25});
        datas.add(new int[]{9400257, 4032181, 1, 1, 0, 5000});

        //마베리크 Typeβ
        datas.add(new int[]{9400258, 4032163, 1, 1, 0, 1000000});
        datas.add(new int[]{9400258, 4032192, 1, 1, 4689, 50000});
        datas.add(new int[]{9400258, 4032167, 1, 1, 0, 25});
        datas.add(new int[]{9400258, 4032181, 1, 1, 0, 5000});

        //마베리크 Typeν
        datas.add(new int[]{9400259, 4032163, 1, 1, 0, 1000000});
        datas.add(new int[]{9400259, 4032181, 1, 1, 0, 5000});
        datas.add(new int[]{9400259, 4032164, 1, 1, 4695, 10000});

        //이르바타
        datas.add(new int[]{9400260, 4032161, 1, 1, 0, 1000000});
        datas.add(new int[]{9400260, 4032180, 1, 1, 4692, 10000});
        datas.add(new int[]{9400260, 4032168, 1, 1, 0, 25});
        datas.add(new int[]{9400260, 4032181, 1, 1, 0, 5000});

        //베르가모트
        datas.add(new int[]{9400265, 1003976, 1, 1, 0, 70000});
        datas.add(new int[]{9400265, 1052669, 1, 1, 0, 70000});
        datas.add(new int[]{9400265, 1102623, 1, 1, 0, 100000});
        datas.add(new int[]{9400265, 1012438, 1, 1, 0, 40000});
        datas.add(new int[]{9400265, 1092050, 1, 1, 0, 40000});
        datas.add(new int[]{9400265, 2330007, 1, 1, 0, 20000});

        datas.add(new int[]{9400265, 2022345, 1, 5, 0, 1000000});
        datas.add(new int[]{9400265, 2000004, 10, 30, 0, 1000000});
        datas.add(new int[]{9400265, 2000005, 5, 20, 0, 1000000});

        datas.add(new int[]{9400265, 4020011, 1, 1, 0, 20000});
        datas.add(new int[]{9400265, 4020012, 1, 1, 0, 20000});

        datas.add(new int[]{9400265, 4032157, 1, 1, 0, 1000000});
        datas.add(new int[]{9400265, 4032167, 1, 1, 0, 20000});
        datas.add(new int[]{9400265, 4032168, 1, 1, 0, 20000});
        datas.add(new int[]{9400265, 4032181, 5, 10, 0, 1000000});
        datas.add(new int[]{9400265, 2046309, 1, 1, 0, 10000});
        datas.add(new int[]{9400265, 2046314, 1, 1, 0, 10000});
        datas.add(new int[]{9400265, 2046310, 1, 1, 0, 7000});
        datas.add(new int[]{9400265, 2046315, 1, 1, 0, 7000});
        datas.add(new int[]{9400265, 2043403, 1, 1, 0, 50000});
        
        datas.add(new int[]{9400265, 2430144, 1, 1, 0, 30000});

        //이름 없는 마수
        datas.add(new int[]{9400266, 4032150, 1, 1, 0, 1000000});
        datas.add(new int[]{9400266, 1082556, 1, 1, 0, 130000});
        datas.add(new int[]{9400266, 1072870, 1, 1, 0, 72000});
        datas.add(new int[]{9400266, 1102623, 1, 1, 0, 150000});
        datas.add(new int[]{9400266, 1109999, 1, 1, 0, 80000});
        datas.add(new int[]{9400266, 2046309, 1, 1, 0, 180000});
        datas.add(new int[]{9400266, 2046310, 1, 1, 0, 35000});
        datas.add(new int[]{9400266, 2046314, 1, 1, 0, 180000});
        datas.add(new int[]{9400266, 2046315, 1, 1, 0, 35000});
        datas.add(new int[]{9400266, 2070019, 1, 1, 0, 20000});
        datas.add(new int[]{9400266, 2330007, 1, 1, 0, 20000});
        datas.add(new int[]{9400266, 4020010, 1, 1, 0, 20000});
        datas.add(new int[]{9400266, 4020011, 1, 1, 0, 20000});
        datas.add(new int[]{9400266, 4020012, 1, 1, 0, 20000});
        datas.add(new int[]{9400270, 2022345, 1, 5, 0, 1000000});

        //듀나스
        datas.add(new int[]{9400270, 2022345, 1, 5, 0, 1000000});
        datas.add(new int[]{9400270, 2000004, 10, 30, 0, 1000000});
        datas.add(new int[]{9400270, 2000005, 5, 20, 0, 1000000});

        datas.add(new int[]{9400270, 2070019, 1, 1, 0, 10000});

        datas.add(new int[]{9400270, 4020011, 1, 1, 0, 20000});
        datas.add(new int[]{9400270, 4020012, 1, 1, 0, 20000});

        datas.add(new int[]{9400270, 1082556, 1, 1, 0, 60000});
        datas.add(new int[]{9400270, 1032062, 1, 1, 0, 80000});
        datas.add(new int[]{9400270, 1102623, 1, 1, 0, 100000});
        datas.add(new int[]{9400270, 1109999, 1, 1, 0, 40000});
        datas.add(new int[]{9400270, 4032162, 1, 1, 0, 1000000});
        datas.add(new int[]{9400270, 4032166, 1, 1, 0, 20000});
        datas.add(new int[]{9400270, 4032167, 1, 1, 0, 20000});
        datas.add(new int[]{9400270, 4032181, 5, 10, 0, 1000000});
        datas.add(new int[]{9400270, 2046309, 1, 1, 0, 10000});
        datas.add(new int[]{9400270, 2046314, 1, 1, 0, 10000});
        datas.add(new int[]{9400270, 2046310, 1, 1, 0, 7000});
        datas.add(new int[]{9400270, 2046315, 1, 1, 0, 7000});
        datas.add(new int[]{9400270, 2043403, 1, 1, 0, 50000});
        
        datas.add(new int[]{9400270, 2430144, 1, 1, 0, 30000});

        //니베룽
        datas.add(new int[]{9400273, 4032165, 1, 1, 0, 1000000});
        datas.add(new int[]{9400273, 2330007, 1, 1, 0, 8000});
        datas.add(new int[]{9400273, 4032166, 1, 1, 0, 15000});
        datas.add(new int[]{9400273, 4032167, 1, 1, 0, 15000});
        datas.add(new int[]{9400273, 4032168, 1, 1, 0, 15000});
        datas.add(new int[]{9400273, 2040815, 1, 1, 0, 30000});
        datas.add(new int[]{9400273, 1003976, 1, 1, 0, 100000});
        datas.add(new int[]{9400273, 1052669, 1, 1, 0, 80000});
        datas.add(new int[]{9400273, 1102623, 1, 1, 0, 100000});
        datas.add(new int[]{9400273, 2046309, 1, 1, 0, 1000000});
        datas.add(new int[]{9400273, 2046310, 1, 1, 0, 36000});
        datas.add(new int[]{9400273, 2046314, 1, 1, 0, 1000000});
        datas.add(new int[]{9400273, 2046315, 1, 1, 0, 36000});
        datas.add(new int[]{9400273, 1012438, 1, 1, 0, 70000});
        datas.add(new int[]{9400273, 4020011, 1, 1, 0, 30000});
        datas.add(new int[]{9400273, 4020012, 1, 1, 0, 30000});
        datas.add(new int[]{9400273, 2022345, 1, 5, 0, 1000000});
        datas.add(new int[]{9400273, 2430144, 1, 1, 0, 80000});

        //임페리얼 가드
        datas.add(new int[]{9400287, 4032355, 1, 1, 0, 1000000});

        //듀나스 2차전
        datas.add(new int[]{9400294, 2070019, 1, 1, 0, 20000});
        datas.add(new int[]{9400294, 2330007, 1, 1, 0, 20000});
        datas.add(new int[]{9400294, 1003976, 1, 1, 0, 180000});
        datas.add(new int[]{9400294, 1052669, 1, 1, 0, 180000});
        datas.add(new int[]{9400294, 1102623, 1, 1, 0, 180000});
        datas.add(new int[]{9400294, 1082556, 1, 1, 0, 130000});
        datas.add(new int[]{9400294, 1072870, 1, 1, 0, 30000});
        datas.add(new int[]{9400294, 1109999, 1, 1, 0, 80000});
        datas.add(new int[]{9400294, 1012438, 1, 1, 0, 85000});
        datas.add(new int[]{9400294, 1042191, 1, 1, 0, 45000});
        datas.add(new int[]{9400294, 1062125, 1, 1, 0, 45000});
        datas.add(new int[]{9400265, 2022345, 1, 5, 0, 1000000});
        datas.add(new int[]{9400294, 2046309, 1, 1, 0, 1000000});
        datas.add(new int[]{9400294, 2046310, 1, 1, 0, 100000});
        datas.add(new int[]{9400294, 2046311, 1, 1, 0, 10000});
        datas.add(new int[]{9400294, 2046314, 1, 1, 0, 1000000});
        datas.add(new int[]{9400294, 2046315, 1, 1, 0, 100000});
        datas.add(new int[]{9400294, 2043403, 1, 1, 0, 50000});

        //로얄 가드
        datas.add(new int[]{9400288, 4032356, 1, 1, 0, 1000000});

        //코어 블레이즈
        datas.add(new int[]{9400296, 4032358, 1, 1, 0, 1000000});
        datas.add(new int[]{9400296, 2022345, 2, 8, 0, 1000000});

        //아우프헤벤
        datas.add(new int[]{9400289, 2000004, 10, 50, 0, 1000000});
        datas.add(new int[]{9400289, 2000005, 5, 30, 0, 1000000});
        datas.add(new int[]{9400289, 4032357, 1, 1, 0, 1000000});
        datas.add(new int[]{9400289, 1002972, 1, 1, 0, 1000000});
        datas.add(new int[]{9400289, 1002972, 1, 1, 0, 200000});
        datas.add(new int[]{9400289, 1002972, 1, 1, 0, 10000});
        datas.add(new int[]{9400289, 1012438, 1, 1, 0, 85000});
        datas.add(new int[]{9400289, 1092090, 1, 1, 0, 80000});
        datas.add(new int[]{9400289, 1302297, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1312173, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1322223, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1332247, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1342090, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1372195, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1382231, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1402220, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1412152, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1422158, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1432187, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1442242, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1452226, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1462213, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1472235, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1482189, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1492199, 1, 1, 0, 150000});
        datas.add(new int[]{9400289, 1113070, 1, 1, 0, 50000});
        datas.add(new int[]{9400289, 1119998, 1, 1, 0, 90000});
        datas.add(new int[]{9400289, 2046310, 1, 1, 0, 100000});
        datas.add(new int[]{9400289, 2046311, 1, 1, 0, 17500});
        datas.add(new int[]{9400289, 2046315, 1, 1, 0, 100000});
        datas.add(new int[]{9400289, 2046316, 1, 1, 0, 40000});
        datas.add(new int[]{9400289, 2290125, 1, 1, 0, 3334});
    }

    private static void addAdditionalScrollDropEntry(List<int[]> datas) {
        //혼돈의 주문서
        datas.add(new int[]{9400014, 2049100, 1, 1, 0, 20000}); //천구
        datas.add(new int[]{9400405, 2049100, 1, 1, 0, 10000}); //갑옷 무사
        datas.add(new int[]{9400575, 2049100, 1, 1, 0, 15000}); //빅풋
        datas.add(new int[]{8220003, 2049100, 1, 1, 0, 7500}); //레비아탄
        datas.add(new int[]{8220004, 2049100, 1, 1, 0, 7500}); //도도
        datas.add(new int[]{8220005, 2049100, 1, 1, 0, 7500}); //릴리노흐

        //악세서리 공격력 주문서 60%
        datas.add(new int[]{8200012, 2046309, 1, 1, 0, 30}); //망각의 수호대장
        datas.add(new int[]{9400405, 2046309, 1, 1, 0, 10000}); //갑옷 무사
        datas.add(new int[]{8800002, 2046309, 1, 1, 0, 30000}); //자쿰
        datas.add(new int[]{8810018, 2046309, 1, 1, 0, 30000}); //혼테일
        datas.add(new int[]{8500002, 2046309, 1, 1, 0, 30000}); //파풀라투스
        datas.add(new int[]{9400014, 2046309, 1, 1, 0, 20000}); //천구
        datas.add(new int[]{9420544, 2046309, 1, 1, 0, 1000000}); //타르가
        datas.add(new int[]{9420549, 2046309, 1, 1, 0, 1000000}); //스카라이온 보스

        //악세서리 마력 주문서 70%
        datas.add(new int[]{8200012, 2046314, 1, 1, 0, 30}); //망각의 수호대장
        datas.add(new int[]{9400405, 2046314, 1, 1, 0, 10000}); //갑옷 무사
        datas.add(new int[]{8800002, 2046314, 1, 1, 0, 30000}); //자쿰
        datas.add(new int[]{8810018, 2046314, 1, 1, 0, 30000}); //혼테일
        datas.add(new int[]{8500002, 2046314, 1, 1, 0, 30000}); //파풀라투스
        datas.add(new int[]{9400014, 2046314, 1, 1, 0, 20000}); //천구
        datas.add(new int[]{9420544, 2046314, 1, 1, 0, 1000000}); //타르가
        datas.add(new int[]{9420549, 2046314, 1, 1, 0, 1000000}); //스카라이온 보스

        //악세서리 공격력 주문서 40%
        datas.add(new int[]{8800002, 2046310, 1, 1, 0, 20000}); //자쿰
        datas.add(new int[]{8810018, 2046310, 1, 1, 0, 20000}); //혼테일
        datas.add(new int[]{9420544, 2046310, 1, 1, 0, 50000}); //타르가
        datas.add(new int[]{9420549, 2046310, 1, 1, 0, 50000}); //스카라이온 보스

        //악세서리 마력 주문서 45%
        datas.add(new int[]{8800002, 2046315, 1, 1, 0, 20000}); //자쿰
        datas.add(new int[]{8810018, 2046315, 1, 1, 0, 20000}); //혼테일
        datas.add(new int[]{9420544, 2046315, 1, 1, 0, 50000}); //타르가
        datas.add(new int[]{9420549, 2046315, 1, 1, 0, 50000}); //스카라이온 보스
    }

    //customMoneyMobAdd.add(8820001); // 핑크빈 안쓰는중
    /*for (int cs : customMoneyMobAdd) {

            List<MobDropEntry> mdes = mdrop_final.get(cs);
            if (mdes == null) {
                mdes = new ArrayList<MobDropEntry>();
            }
            List<MobDropEntry> newMobDrops = new ArrayList<MobDropEntry>(mdes);
            boolean hasMoney = false;
            for (MobDropEntry mde : mdes) {
                if (mde.itemid == 0) {
                    hasMoney = true;
                    break;
                }
            }

            boolean isBoss = MapleLifeFactory.getMonsterStats(cs).isBoss();
            boolean isRaidBoss = false;
            switch (cs) {
                case 8810018:
                case 8800002:
                case 8800102: //카오스 자쿰
                case 8520000:
                case 8510000:
                case 8500002:
                case 8820001:
                    isRaidBoss = true;
                    break;
            }
            for (int i = 0; i < (isRaidBoss ? 15 : isBoss ? 5 : 1) && !hasMoney; ++i) {
                Random r = new Random();
                MapleMonsterStats mobstat = MapleLifeFactory.getMonsterStats(cs);
                double mesoDecrease = Math.pow(0.93, mobstat.getExp() / (isBoss ? 200.0 : 30.0));
                if (mesoDecrease > 1.0) {
                    mesoDecrease = 1.0;
                } else if (mesoDecrease < 0.001) {
                    mesoDecrease = 0.005;
                }
                int tempmeso = Math.min(30000, (int) (mesoDecrease * (mobstat.getExp() * 57) / 10.0));

                final int meso = tempmeso;
                newMobDrops.add(new MobDropEntry(cs, 0, 700000, (int) (meso * 0.75), meso, 0)); //화살
            }
            mdrop_final.put(cs, newMobDrops);
        }*/
}
