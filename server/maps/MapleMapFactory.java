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
package server.maps;

import constants.GameConstants;
import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MaplePortal;
import server.life.AbstractLoadedMapleLife;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleNodes.DirectionInfo;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import tools.Pair;
import tools.StringUtil;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MapleMapFactory {

    private final MapleDataProvider source = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Map.wz"));
    private final MapleData nameData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")).getData("Map.img");
    private final HashMap<Integer, MapleMap> maps = new HashMap<Integer, MapleMap>();
    private final HashMap<Integer, MapleMap> instanceMap = new HashMap<Integer, MapleMap>();
    private final ReentrantLock lock = new ReentrantLock();
    private int channel;

    public MapleMapFactory(int channel) {
        this.channel = channel;
    }

    public final MapleMap getMap(final int mapid) {
        return getMap(mapid, true, true, true);
    }

    public final MapleMap getMap(final int mapid, final boolean respawns, final boolean npcs, final boolean reactors) {
        Integer omapid = Integer.valueOf(mapid);
        MapleMap map = maps.get(omapid);
        if (map == null) {
            lock.lock();
            try {
                map = maps.get(omapid);
                if (map != null) {
                    return map;
                }
                MapleData mapData = null;
                try {
                    mapData = source.getData(getMapName(mapid));
                } catch (Exception e) {
                    return null;
                }
                if (mapData == null) {
                    return null;
                }
                MapleData link = mapData.getChildByPath("info/link");
                if (link != null) {
                    mapData = source.getData(getMapName(MapleDataTool.getIntConvert("info/link", mapData)));
                }

                float monsterRate = 0;
                if (respawns) {
                    MapleData mobRate = mapData.getChildByPath("info/mobRate");
                    if (mobRate != null) {
                        monsterRate = ((Float) mobRate.getData()).floatValue();
                    }
                }
                map = new MapleMap(mapid, channel, MapleDataTool.getInt("info/returnMap", mapData), monsterRate);

                loadPortals(map, mapData.getChildByPath("portal"));
                map.setTop(MapleDataTool.getInt(mapData.getChildByPath("info/VRTop"), 0));
                map.setLeft(MapleDataTool.getInt(mapData.getChildByPath("info/VRLeft"), 0));
                map.setBottom(MapleDataTool.getInt(mapData.getChildByPath("info/VRBottom"), 0));
                map.setRight(MapleDataTool.getInt(mapData.getChildByPath("info/VRRight"), 0));
                int x = MapleDataTool.getInt(mapData.getChildByPath("miniMap/width"), 1200);
                int y = MapleDataTool.getInt(mapData.getChildByPath("miniMap/height"), 1000);
                if (x < 800) {
                    x = 800;
                }
                if (y - 450 > 600) {
                    y = y - 450;
                }
                map.setXY(x * y);

                List<MapleFoothold> allFootholds = new LinkedList<MapleFoothold>();
                Point lBound = new Point();
                Point uBound = new Point();
                MapleFoothold fh;

                for (MapleData footRoot : mapData.getChildByPath("foothold")) {
                    for (MapleData footCat : footRoot) {
                        for (MapleData footHold : footCat) {
                            fh = new MapleFoothold(new Point(
                                    MapleDataTool.getInt(footHold.getChildByPath("x1"), 0), MapleDataTool.getInt(footHold.getChildByPath("y1"), 0)), new Point(
                                            MapleDataTool.getInt(footHold.getChildByPath("x2"), 0), MapleDataTool.getInt(footHold.getChildByPath("y2"), 0)), Integer.parseInt(footHold.getName()));
                            fh.setPrev((short) MapleDataTool.getInt(footHold.getChildByPath("prev"), 0));
                            fh.setNext((short) MapleDataTool.getInt(footHold.getChildByPath("next"), 0));

                            if (fh.getX1() < lBound.x) {
                                lBound.x = fh.getX1();
                            }
                            if (fh.getX2() > uBound.x) {
                                uBound.x = fh.getX2();
                            }
                            if (fh.getY1() < lBound.y) {
                                lBound.y = fh.getY1();
                            }
                            if (fh.getY2() > uBound.y) {
                                uBound.y = fh.getY2();
                            }
                            allFootholds.add(fh);
                        }
                    }
                }
                MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
                for (MapleFoothold foothold : allFootholds) {
                    fTree.insert(foothold);
                }
                map.setFootholds(fTree);
                if (map.getTop() == 0) {
                    map.setTop(lBound.y);
                }
                if (map.getBottom() == 0) {
                    map.setBottom(uBound.y);
                }
                if (map.getLeft() == 0) {
                    map.setLeft(lBound.x);
                }
                if (map.getRight() == 0) {
                    map.setRight(uBound.x);
                }
                int bossid = -1;
                String msg = null;
                if (mapData.getChildByPath("info/timeMob") != null) {
                    bossid = MapleDataTool.getInt(mapData.getChildByPath("info/timeMob/id"), 0);
                    msg = MapleDataTool.getString(mapData.getChildByPath("info/timeMob/message"), null);
                }

                // load life data (npc, monsters)
                List<Point> herbRocks = new ArrayList<Point>();
                int lowestLevel = 200, highestLevel = 0;
                String type, limited;
                AbstractLoadedMapleLife myLife;

                for (MapleData life : mapData.getChildByPath("life")) {
                    type = MapleDataTool.getString(life.getChildByPath("type"));
                    limited = MapleDataTool.getString("limitedname", life, "");
                    if ((npcs || !type.equals("n")) && !limited.equals("Stage0")) { //alien pq stuff
                        myLife = loadLife(life, MapleDataTool.getString(life.getChildByPath("id")), type);

                        if (myLife instanceof MapleMonster && !GameConstants.isNoSpawn(mapid)) {
                            final MapleMonster mob = (MapleMonster) myLife;

                            herbRocks.add(map.addMonsterSpawn(mob,
                                    MapleDataTool.getInt("mobTime", life, 0),
                                    (byte) MapleDataTool.getInt("team", life, -1),
                                    mob.getId() == bossid ? msg : null).getPosition());
                            if (mob.getStats().getLevel() > highestLevel && !mob.getStats().isBoss()) {
                                highestLevel = mob.getStats().getLevel();
                            }
                            if (mob.getStats().getLevel() < lowestLevel && !mob.getStats().isBoss()) {
                                lowestLevel = mob.getStats().getLevel();
                            }
                        } else if (myLife instanceof MapleNPC) {
                            map.addMapObject(myLife);
                        }
                    }
                }
                addAreaBossSpawn(map); //마스터몹 스폰 금지 
                map.setCreateMobInterval((short) MapleDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 9000));
                map.setFixedMob(MapleDataTool.getInt(mapData.getChildByPath("info/fixedMobCapacity"), 0));
                map.setPartyBonusRate(GameConstants.getPartyPlay(mapid, MapleDataTool.getInt(mapData.getChildByPath("info/partyBonusR"), 0))); //파티보너스 부분
                map.loadMonsterRate(true);
                map.setNodes(loadNodes(mapid, mapData));

                //load reactor data
                String id;
                if (reactors && mapData.getChildByPath("reactor") != null) {
                    for (MapleData reactor : mapData.getChildByPath("reactor")) {
                        id = MapleDataTool.getString(reactor.getChildByPath("id"));
                        if (id != null) {
                            map.spawnReactor(loadReactor(reactor, id, (byte) MapleDataTool.getInt(reactor.getChildByPath("f"), 0)));
                        }
                    }
                }
                map.setFirstUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
                map.setUserEnter(mapid == GameConstants.JAIL ? "jail" : MapleDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));

                try {
                    map.setMapName(MapleDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(omapid)), ""));
                    map.setStreetName(MapleDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(omapid)), ""));
                } catch (Exception e) {
                    map.setMapName("");
                    map.setStreetName("");
                }
                map.setClock(mapData.getChildByPath("clock") != null); //clock was changed in wz to have x,y,width,height
                map.setEverlast(MapleDataTool.getInt(mapData.getChildByPath("info/everlast"), 0) > 0);
                map.setTown(MapleDataTool.getInt(mapData.getChildByPath("info/town"), 0) > 0);
                map.setSoaring(MapleDataTool.getInt(mapData.getChildByPath("info/needSkillForFly"), 0) > 0);
                map.setPersonalShop(MapleDataTool.getInt(mapData.getChildByPath("info/personalShop"), 0) > 0);
                map.setForceMove(MapleDataTool.getInt(mapData.getChildByPath("info/lvForceMove"), 0));
                map.setHPDec(MapleDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
                map.setHPDecInterval(MapleDataTool.getInt(mapData.getChildByPath("info/decHPInterval"), 10000));
                map.setHPDecProtect(MapleDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
                map.setForcedReturnMap(mapid == 0 ? 999999999 : MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
                map.setTimeLimit(MapleDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));
                map.setFieldLimit(MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
                map.setBGM(MapleDataTool.getString(mapData.getChildByPath("info/bgm"), "")); //BGM 불러들이기
                map.setRecoveryRate(MapleDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));
                map.setFixedMob(MapleDataTool.getInt(mapData.getChildByPath("info/fixedMobCapacity"), 0));
                //map.setPartyBonusRate(GameConstants.getPartyPlay(mapid, MapleDataTool.getInt(mapData.getChildByPath("info/partyBonusR"), 0)));
                map.setConsumeItemCoolTime(MapleDataTool.getInt(mapData.getChildByPath("info/consumeItemCoolTime"), 0));

                maps.put(omapid, map);
                getFixNpc(map, mapid);//고정엔피시
            } finally {
                lock.unlock();
            }
        }
        return map;
    }

    public MapleMap getInstanceMap(final int instanceid) {
        return instanceMap.get(instanceid);
    }

    public void removeInstanceMap(final int instanceid) {
        lock.lock();
        try {
            if (isInstanceMapLoaded(instanceid)) {
                getInstanceMap(instanceid).checkStates("");
                instanceMap.remove(instanceid);
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeMap(final int instanceid) {
        lock.lock();
        try {
            if (isMapLoaded(instanceid)) {
                getMap(instanceid).checkStates("");
                maps.remove(instanceid);
            }
        } finally {
            lock.unlock();
        }
    }

    public int getLoadedMaps() {
        return maps.size();
    }

    public boolean isMapLoaded(int mapId) {
        return maps.containsKey(mapId);
    }

    public boolean isInstanceMapLoaded(int instanceid) {
        return instanceMap.containsKey(instanceid);
    }

    public void clearLoadedMap() {
        lock.lock();
        try {
            maps.clear();
        } finally {
            lock.unlock();
        }
    }

    public List<MapleMap> getAllLoadedMaps() {
        List<MapleMap> ret = new ArrayList<MapleMap>();
        lock.lock();
        try {
            ret.addAll(maps.values());
            ret.addAll(instanceMap.values());
        } finally {
            lock.unlock();
        }
        return ret;
    }

    public Collection<MapleMap> getAllMaps() {
        return maps.values();
    }

    private AbstractLoadedMapleLife loadLife(MapleData life, String id, String type) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(Integer.parseInt(id), type);
        if (myLife == null) {
            return null;
        }
        myLife.setCy(MapleDataTool.getInt(life.getChildByPath("cy")));
        MapleData dF = life.getChildByPath("f");
        if (dF != null) {
            myLife.setF(MapleDataTool.getInt(dF));
        }
        myLife.setFh(MapleDataTool.getInt(life.getChildByPath("fh")));
        myLife.setRx0(MapleDataTool.getInt(life.getChildByPath("rx0")));
        myLife.setRx1(MapleDataTool.getInt(life.getChildByPath("rx1")));
        myLife.setPosition(new Point(MapleDataTool.getInt(life.getChildByPath("x")), MapleDataTool.getInt(life.getChildByPath("y"))));

        if (MapleDataTool.getInt("hide", life, 0) == 1 && myLife instanceof MapleNPC) {
            myLife.setHide(true);
//		} else if (hide > 1) {
//			System.err.println("Hide > 1 ("+ hide +")");
        }
        return myLife;
    }

    private final MapleReactor loadReactor(final MapleData reactor, final String id, final byte FacingDirection) {
        final MapleReactor myReactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id));

        myReactor.setFacingDirection(FacingDirection);
        myReactor.setPosition(new Point(MapleDataTool.getInt(reactor.getChildByPath("x")), MapleDataTool.getInt(reactor.getChildByPath("y"))));
        myReactor.setDelay(MapleDataTool.getInt(reactor.getChildByPath("reactorTime")) * 1000);
        myReactor.setName(MapleDataTool.getString(reactor.getChildByPath("name"), ""));

        return myReactor;
    }

    private static String getMapName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        StringBuilder builder = new StringBuilder("Map/Map");
        builder.append(mapid / 100000000);
        builder.append("/");
        builder.append(mapName);
        builder.append(".img");

        mapName = builder.toString();
        return mapName;
    }

    public static String getMapStringName(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) {
            builder.append("maple");
        } else if ((mapid >= 100000000 && mapid < 200000000) || mapid / 100000 == 5540) {
            builder.append("victoria");
        } else if (mapid >= 200000000 && mapid < 300000000) {
            builder.append("ossyria");
        } else if (mapid >= 300000000 && mapid < 400000000) {
            builder.append("3rd");
        } else if (mapid >= 500000000 && mapid < 510000000) {
            builder.append("thai");
        } else if (mapid >= 555000000 && mapid < 556000000) {
            builder.append("SG");
        } else if (mapid >= 540000000 && mapid < 600000000) {
            builder.append("singapore");
        } else if (mapid >= 682000000 && mapid < 683000000) {
            builder.append("HalloweenGL");
        } else if (mapid >= 600000000 && mapid < 670000000) {
            builder.append("MasteriaGL");
        } else if (mapid >= 677000000 && mapid < 678000000) {
            builder.append("Episode1GL");
        } else if (mapid >= 670000000 && mapid < 682000000) {
            builder.append("weddingGL");
        } else if (mapid >= 687000000 && mapid < 688000000) {
            builder.append("Gacha_GL");
        } else if (mapid >= 689000000 && mapid < 690000000) {
            builder.append("CTF_GL");
        } else if (mapid >= 683000000 && mapid < 684000000) {
            builder.append("event");
        } else if (mapid >= 684000000 && mapid < 685000000) {
            builder.append("event_5th");
        } else if (mapid >= 700000000 && mapid < 700000300) {
            builder.append("wedding");
        } else if (mapid >= 701000000 && mapid < 701020000) {
            builder.append("china");
        } else if ((mapid >= 702090000 && mapid <= 702100000) || (mapid >= 740000000 && mapid < 741000000)) {
            builder.append("taiwan");
        } else if (mapid >= 702000000 && mapid < 742000000) {
            builder.append("china");
        } else if (mapid >= 800000000 && mapid < 900000000) {
            builder.append("japan");
        } else {
            builder.append("etc");
        }
        builder.append("/");
        builder.append(mapid);

        return builder.toString();
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    private void addAreaBossSpawn(final MapleMap map) {
        int monsterid = -1;
        int mobtime = -1;
        String msg = null;
        boolean shouldSpawn = true;
        Point pos1 = null, pos2 = null, pos3 = null;

        switch (map.getId()) {
            case 104010200: // 마노
                mobtime = 1200;
                monsterid = 2220000;
                msg = "서늘한 기운이 감돌면서 마노가 나타났습니다.";
                pos1 = new Point(189, 2);
                pos2 = new Point(478, 250);
                pos3 = new Point(611, 489);
                break;
            case 102020500: // 스텀피
                mobtime = 1200;
                monsterid = 3220000;
                msg = "바위산을 울리는 발걸음 소리와 함께 스텀피가 나타났습니다.";
                pos1 = new Point(1121, 2130);
                pos2 = new Point(483, 2171);
                pos3 = new Point(1474, 1706);
                break;
            case 100020101: // 머쉬맘
                mobtime = 1200;
                monsterid = 6130101;
                msg = "어디선가 커다란 버섯이 나타났습니다.";
                pos1 = new Point(-311, 201);
                pos2 = new Point(-903, 197);
                pos3 = new Point(-568, 196);
                break;
            case 100020301: //Blue Mushmom
                mobtime = 1200;
                monsterid = 8220007;
                msg = "어디선가 커다란 파란 버섯이 나타났습니다1.";
                pos1 = new Point(-188, -657);
                pos2 = new Point(625, -660);
                pos3 = new Point(508, -648);
                break;
            case 100020401: //Zombie Mushmom
                mobtime = 1200;
                monsterid = 6300005;
                msg = "어디선가 음산한 기운을 풍기는 커다란 버섯이 나타났습니다.";
                pos1 = new Point(-130, -773);
                pos2 = new Point(504, -760);
                pos3 = new Point(608, -641);
                break;
            case 120030500: // King Clang
                mobtime = 1200;
                monsterid = 5220001;
                msg = "모래사장에 수상한 소라껍질이 나타났습니다.";
                pos1 = new Point(-355, 179);
                pos2 = new Point(-1283, -113);
                pos3 = new Point(-571, -593);
                break;
            case 250010304: // Tae Roon
                mobtime = 2100;
                monsterid = 7220000;
                msg = "나즈막한 휘파람 소리와 함께 태륜이 나타났습니다.";
                pos1 = new Point(-210, 33);
                pos2 = new Point(-234, 393);
                pos3 = new Point(-654, 33);
                break;
            case 200010302: // Eliza
                mobtime = 1200;
                monsterid = 8220000;
                msg = "검은 돌개바람과 함께 엘리쟈가 나타났습니다.";
                pos1 = new Point(-58, 83);
                pos2 = new Point(-58, 83);
                pos3 = new Point(-58, 83);
                break;
            case 250010503: // 요괴선사
                mobtime = 1800;
                monsterid = 7220002;
                msg = "주변을 흐르는 요기가 강해졌습니다. 기분 나쁜 고양이 울음소리가 들립니다.";
                pos1 = new Point(-303, 543);
                pos2 = new Point(227, 543);
                pos3 = new Point(719, 543);
                break;
            case 222010310: // 구미호
                mobtime = 2700;
                monsterid = 7220001;
                msg = "달빛이 흐려지면서 긴 여우 울음소리와 함께 구미호의 기운이 느껴집니다.";
                pos1 = new Point(-169, -147);
                pos2 = new Point(-517, 93);
                pos3 = new Point(247, 93);
                break;
            case 103030400: // 다일
                mobtime = 1800;
                monsterid = 6220000;
                msg = "늪 속에서 거대한 악어 다일이 올라왔습니다.";
                pos1 = new Point(-831, 109);
                pos2 = new Point(-831, 109);
                pos3 = new Point(-511, 107);
                break;
            case 101040300: // 파우스트
                mobtime = 1800;
                monsterid = 5220002;
                msg = "푸른안개가 짙어지면서 파우스트가 나타났습니다.";
                pos1 = new Point(600, -600);
                pos2 = new Point(600, -800);
                pos3 = new Point(600, -300);
                break;
            case 220050200: // 타이머
                mobtime = 1500;
                monsterid = 5220003;
                msg = "짹각짹각! 불규칙한 시계 소리와 함께 타이머가 나타났습니다.";
                pos1 = new Point(-90, 1032);
                pos2 = new Point(-432, 732);
                pos3 = new Point(431, 492);
                break;
            case 221040301: // 제노
                mobtime = 2400;
                monsterid = 6220001;
                msg = "묵직한 기계음을 울리며 제노가 나타났습니다.";
                pos1 = new Point(-4134, 416);
                pos2 = new Point(-4283, 776);
                pos3 = new Point(-3292, 776);
                break;
            case 240040401: // 레비아탄
                mobtime = 7200;
                monsterid = 8220003;
                msg = "차가운 바람과 함께 협곡 너머에서 레비아탄이 모습을 드러냈습니다.";
                pos1 = new Point(-15, 2481);
                pos2 = new Point(127, 1634);
                pos3 = new Point(159, 1142);
                break;
            case 260010201: // 데우
                mobtime = 3600;
                monsterid = 3220001;
                msg = "모래먼지 속에서 데우가 천천히 모습을 드러냈습니다.";
                pos1 = new Point(-215, 275);
                pos2 = new Point(298, 275);
                pos3 = new Point(592, 275);
                break;
            case 251010102: // 대왕지네
                mobtime = 3600;
                monsterid = 5220004;
                msg = "물밑에서 스멀스멀 대왕지네가 나타났습니다.";
                pos1 = new Point(-41, 124);
                pos2 = new Point(-173, 126);
                pos3 = new Point(79, 118);
                break;
            case 261030000: // 키메라
                mobtime = 2700;
                monsterid = 8220002;
                msg = "지하의 어둠 속에서 안광을 빛내며 키메라가 모습을 드러냈습니다.";
                pos1 = new Point(-1094, -405);
                pos2 = new Point(-772, -116);
                pos3 = new Point(-108, 181);
                break;
            case 230020100: // 셰르프
                mobtime = 2700;
                monsterid = 4220000;
                msg = "해초 수풀 사이로 이상한 조개가 나타났습니다.";
                pos1 = new Point(-291, -20);
                pos2 = new Point(-272, -500);
                pos3 = new Point(-462, 640);
                break;
            case 103020320: // 셰이드
                mobtime = 1800;
                monsterid = 5090000;
                msg = "지하철의 어두운 그림자 속에서 무언가 나타났습니다.";
                pos1 = new Point(79, 174);
                pos2 = new Point(-223, 296);
                pos3 = new Point(80, 275);
                break;
            case 103020420: // 셰이드
                mobtime = 1800;
                monsterid = 5090000;
                msg = "지하철의 어두운 그림자 속에서 무언가 나타났습니다.";
                pos1 = new Point(2241, 301);
                pos2 = new Point(1990, 301);
                pos3 = new Point(1684, 307);
                break;
            case 261020300: // 자동경비시스템
                mobtime = 2700;
                monsterid = 7090000;
                msg = "자동경비시스템이 발동했습니다.";
                pos1 = new Point(312, 157);
                pos2 = new Point(539, 136);
                pos3 = new Point(760, 141);
                break;
            case 261020401: // Deet and Roi
                mobtime = 2700;
                monsterid = 8090000;
                msg = "알케미스트연구소를 순찰하는 디트와 로이가 나타났습니다.";
                pos1 = new Point(-263, 155);
                pos2 = new Point(-436, 122);
                pos3 = new Point(22, 144);
                break;
            case 250020300: // Master Dummy
                mobtime = 2700;
                monsterid = 5090001;
                msg = "어디선가 선인인형이 나타났습니다.";
                pos1 = new Point(1208, 27);
                pos2 = new Point(1654, 40);
                pos3 = new Point(927, -502);
                break;
            case 211050000: // Snow Witch
                mobtime = 2700;
                monsterid = 6090001;
                msg = "얼음속에 구속된 마녀가 눈을 떴습니다.";
                pos1 = new Point(-233, -431);
                pos2 = new Point(-370, -426);
                pos3 = new Point(-526, -420);
                break;
            case 261010003: // Rurumo
                mobtime = 2700;
                monsterid = 6090004;
                msg = "수상쩍은 소근거림과 함께 루루모가 모습을 드러냈습니다.";
                pos1 = new Point(-861, 301);
                pos2 = new Point(-703, 301);
                pos3 = new Point(-426, 287);
                break;
            case 222010300: // 선비귀신
                mobtime = 2700;
                monsterid = 6090003;
                msg = "원혼에 맺힌 울음소리와 함께 선비귀신이 나타났습니다.";
                pos1 = new Point(1300, -400);
                pos2 = new Point(1100, -100);
                pos3 = new Point(1100, 100);
                break;
            case 251010101: // Bamboo Warrior
                mobtime = 2700;
                monsterid = 6090002;
                msg = "두루마기에 몸을 숨겼던 대나무 무사가 모습을 드러냈습니다.";
                pos1 = new Point(-15, -449);
                pos2 = new Point(-114, -442);
                pos3 = new Point(-255, -446);
                break;
            case 211041400: // Riche
                mobtime = 2700;
                monsterid = 6090000;
                msg = "눈보다 차가운 숨결을 지닌 리치가 나타났습니다.";
                pos1 = new Point(1672, 82);
                pos2 = new Point(2071, 10);
                pos3 = new Point(1417, 57);
                break;
            case 105030500: // Rog
                mobtime = 2700;
                monsterid = 8130100;
                msg = "그르렁거리는 소리가 들립니다. 사악한 존재가 모습을 드러냈습니다.";
                pos1 = new Point(1275, -399);
                pos2 = new Point(1254, -412);
                pos3 = new Point(1058, -427);
                break;
            case 105020400: // Snack Bar
                mobtime = 2700;
                monsterid = 8220008;
                msg = "정체불명의 포장마차가 나타났습니다.";
                pos1 = new Point(-163, 82);
                pos2 = new Point(958, 107);
                pos3 = new Point(706, -206);
                break;
            case 211040101: // Snowman
                mobtime = 3600;
                monsterid = 8220001;
                msg = "눈 속에서 스키를 신은 유쾌한 스노우맨이 나타났습니다.";
                pos1 = new Point(485, 244);
                pos2 = new Point(-60, 249);
                pos3 = new Point(208, 255);
                break;
            case 209000000: // Happyvile
                mobtime = 300;
                monsterid = 9500317;
                msg = "어디선가 꼬마 눈사람이 나타났습니다.";
                pos1 = new Point(-115, 154);
                pos2 = new Point(-115, 154);
                pos3 = new Point(-115, 154);
                break;
            case 931000500:
                mobtime = 3 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000501:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000502:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000503:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000504:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000505:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000506:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000507:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000508:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
            case 931000509:
                mobtime = 24 * 3600; //3 hours
                monsterid = 9304005;
                msg = "바위 돌 속에서 제이라가 나타났습니다.";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                break;
        }
        if (monsterid > 0) {
            map.addAreaMonsterSpawn(MapleLifeFactory.getMonster(monsterid), pos1, pos2, pos3, mobtime, msg, shouldSpawn);
        }
    }

    private void loadPortals(MapleMap map, MapleData port) {
        if (port == null) {
            return;
        }
        int nextDoorPortal = 0x80;
        for (MapleData portal : port.getChildren()) {
            MaplePortal myPortal = new MaplePortal(MapleDataTool.getInt(portal.getChildByPath("pt")));
            myPortal.setName(MapleDataTool.getString(portal.getChildByPath("pn")));
            myPortal.setTarget(MapleDataTool.getString(portal.getChildByPath("tn")));
            myPortal.setTargetMapId(MapleDataTool.getInt(portal.getChildByPath("tm")));
            myPortal.setPosition(new Point(MapleDataTool.getInt(portal.getChildByPath("x")), MapleDataTool.getInt(portal.getChildByPath("y"))));
            String script = MapleDataTool.getString("script", portal, null);
            if (script != null && script.equals("")) {
                script = null;
            }
            myPortal.setScriptName(script);

            if (myPortal.getType() == MaplePortal.DOOR_PORTAL) {
                myPortal.setId(nextDoorPortal);
                nextDoorPortal++;
            } else {
                myPortal.setId(Integer.parseInt(portal.getName()));
            }
            map.addPortal(myPortal);
        }
    }

    private MapleNodes loadNodes(final int mapid, final MapleData mapData) {
        MapleNodes nodeInfo = new MapleNodes(mapid);
        if (mapData.getChildByPath("nodeInfo") != null) {
            for (MapleData node : mapData.getChildByPath("nodeInfo")) {
                try {
                    if (node.getName().equals("start")) {
                        nodeInfo.setNodeStart(MapleDataTool.getInt(node, 0));
                        continue;
                    }
                    List<Integer> edges = new ArrayList<Integer>();
                    if (node.getChildByPath("edge") != null) {
                        for (MapleData edge : node.getChildByPath("edge")) {
                            edges.add(MapleDataTool.getInt(edge, -1));
                        }
                    }
                    final MapleNodeInfo mni = new MapleNodeInfo(
                            Integer.parseInt(node.getName()),
                            MapleDataTool.getIntConvert("key", node, 0),
                            MapleDataTool.getIntConvert("x", node, 0),
                            MapleDataTool.getIntConvert("y", node, 0),
                            MapleDataTool.getIntConvert("attr", node, 0), edges);
                    nodeInfo.addNode(mni);
                } catch (NumberFormatException e) {
                } //start, end, edgeInfo = we dont need it
            }
            nodeInfo.sortNodes();
        }
        for (int i = 1; i <= 7; i++) {
            if (mapData.getChildByPath(String.valueOf(i)) != null && mapData.getChildByPath(i + "/obj") != null) {
                for (MapleData node : mapData.getChildByPath(i + "/obj")) {
                    if (node.getChildByPath("SN_count") != null && node.getChildByPath("speed") != null) {
                        int sn_count = MapleDataTool.getIntConvert("SN_count", node, 0);
                        String name = MapleDataTool.getString("name", node, "");
                        int speed = MapleDataTool.getIntConvert("speed", node, 0);
                        if (sn_count <= 0 || speed <= 0 || name.equals("")) {
                            continue;
                        }
                        final List<Integer> SN = new ArrayList<Integer>();
                        for (int x = 0; x < sn_count; x++) {
                            SN.add(MapleDataTool.getIntConvert("SN" + x, node, 0));
                        }
                        final MaplePlatform mni = new MaplePlatform(
                                name, MapleDataTool.getIntConvert("start", node, 2), speed,
                                MapleDataTool.getIntConvert("x1", node, 0),
                                MapleDataTool.getIntConvert("y1", node, 0),
                                MapleDataTool.getIntConvert("x2", node, 0),
                                MapleDataTool.getIntConvert("y2", node, 0),
                                MapleDataTool.getIntConvert("r", node, 0), SN);
                        nodeInfo.addPlatform(mni);
                    } else if (node.getChildByPath("tags") != null) {
                        String name = MapleDataTool.getString("tags", node, "");
                        nodeInfo.addFlag(new Pair<String, Integer>(name, name.endsWith("3") ? 1 : 0)); //idk, no indication in wz
                    }
                }
            }
        }
        // load areas (EG PQ platforms)
        if (mapData.getChildByPath("area") != null) {
            int x1, y1, x2, y2;
            Rectangle mapArea;
            for (MapleData area : mapData.getChildByPath("area")) {
                x1 = MapleDataTool.getInt(area.getChildByPath("x1"));
                y1 = MapleDataTool.getInt(area.getChildByPath("y1"));
                x2 = MapleDataTool.getInt(area.getChildByPath("x2"));
                y2 = MapleDataTool.getInt(area.getChildByPath("y2"));
                mapArea = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
                nodeInfo.addMapleArea(mapArea);
            }
        }
        if (mapData.getChildByPath("CaptureTheFlag") != null) {
            final MapleData mc = mapData.getChildByPath("CaptureTheFlag");
            for (MapleData area : mc) {
                nodeInfo.addGuardianSpawn(new Point(MapleDataTool.getInt(area.getChildByPath("FlagPositionX")), MapleDataTool.getInt(area.getChildByPath("FlagPositionY"))), area.getName().startsWith("Red") ? 0 : 1);
            }
        }
        if (mapData.getChildByPath("directionInfo") != null) {
            final MapleData mc = mapData.getChildByPath("directionInfo");
            for (MapleData area : mc) {
                DirectionInfo di = new DirectionInfo(Integer.parseInt(area.getName()), MapleDataTool.getInt("x", area, 0), MapleDataTool.getInt("y", area, 0), MapleDataTool.getInt("forcedInput", area, 0) > 0);
                for (MapleData event : area.getChildByPath("eventQ")) {
                    di.eventQ.add(MapleDataTool.getString(event));
                }
                nodeInfo.addDirection(Integer.parseInt(area.getName()), di);
            }
        }
        if (mapData.getChildByPath("monsterCarnival") != null) {
            final MapleData mc = mapData.getChildByPath("monsterCarnival");
            if (mc.getChildByPath("mobGenPos") != null) {
                for (MapleData area : mc.getChildByPath("mobGenPos")) {
                    nodeInfo.addMonsterPoint(MapleDataTool.getInt(area.getChildByPath("x")),
                            MapleDataTool.getInt(area.getChildByPath("y")),
                            MapleDataTool.getInt(area.getChildByPath("fh")),
                            MapleDataTool.getInt(area.getChildByPath("cy")),
                            MapleDataTool.getInt("team", area, -1));
                }
            }
            if (mc.getChildByPath("mob") != null) {
                for (MapleData area : mc.getChildByPath("mob")) {
                    nodeInfo.addMobSpawn(MapleDataTool.getInt(area.getChildByPath("id")), MapleDataTool.getInt(area.getChildByPath("spendCP")));
                }
            }
            if (mc.getChildByPath("guardianGenPos") != null) {
                for (MapleData area : mc.getChildByPath("guardianGenPos")) {
                    nodeInfo.addGuardianSpawn(new Point(MapleDataTool.getInt(area.getChildByPath("x")), MapleDataTool.getInt(area.getChildByPath("y"))), MapleDataTool.getInt("team", area, -1));
                }
            }
            if (mc.getChildByPath("skill") != null) {
                for (MapleData area : mc.getChildByPath("skill")) {
                    nodeInfo.addSkillId(MapleDataTool.getInt(area));
                }
            }
        }
        return nodeInfo;
    }

    public void getFixNpc(MapleMap map, int mapid) {//고정엔피시
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM spawnnpc WHERE mapid = ?");
            ps.setInt(1, mapid);
            rs = ps.executeQuery();

            while (rs.next()) {
                MapleNPC npc = MapleLifeFactory.getNPC(rs.getInt("npcid"));
                npc.setCy(rs.getInt("cy"));
                npc.setRx0(rs.getInt("rx0"));
                npc.setRx1(rs.getInt("rx1"));
                npc.setFh(rs.getInt("fh"));
                npc.setPosition(new Point(npc.getRx0() + 50, npc.getCy()));
//                if (npc.getId() == 2131008) {
//                    npc.setF(1);
//                }
                map.addMapObject(npc);
            }

            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println("fix npc error ");
            ex.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                }
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    public MapleMap CreateInstanceMap(int mapid, boolean respawns, boolean npcs, boolean reactors, int instanceid) {
        lock.lock();
        try {
            if (isInstanceMapLoaded(instanceid)) {
                return getInstanceMap(instanceid);
            }
        } finally {
            lock.unlock();
        }
        MapleData mapData = null;
        try {
            mapData = source.getData(getMapName(mapid));
        } catch (Exception e) {
            return null;
        }
        if (mapData == null) {
            return null;
        }
        MapleData link = mapData.getChildByPath("info/link");
        if (link != null) {
            mapData = source.getData(getMapName(MapleDataTool.getIntConvert("info/link", mapData)));
        }

        float monsterRate = 0;
        if (respawns) {
            MapleData mobRate = mapData.getChildByPath("info/mobRate");
            if (mobRate != null) {
                monsterRate = ((Float) mobRate.getData()).floatValue();
            }
        }
        MapleMap map = new MapleMap(mapid, channel, MapleDataTool.getInt("info/returnMap", mapData), monsterRate);
        loadPortals(map, mapData.getChildByPath("portal"));
        map.setTop(MapleDataTool.getInt(mapData.getChildByPath("info/VRTop"), 0));
        map.setLeft(MapleDataTool.getInt(mapData.getChildByPath("info/VRLeft"), 0));
        map.setBottom(MapleDataTool.getInt(mapData.getChildByPath("info/VRBottom"), 0));
        map.setRight(MapleDataTool.getInt(mapData.getChildByPath("info/VRRight"), 0));
        List<MapleFoothold> allFootholds = new LinkedList<MapleFoothold>();
        Point lBound = new Point();
        Point uBound = new Point();
        for (MapleData footRoot : mapData.getChildByPath("foothold")) {
            for (MapleData footCat : footRoot) {
                for (MapleData footHold : footCat) {
                    MapleFoothold fh = new MapleFoothold(new Point(
                            MapleDataTool.getInt(footHold.getChildByPath("x1")), MapleDataTool.getInt(footHold.getChildByPath("y1"))), new Point(
                            MapleDataTool.getInt(footHold.getChildByPath("x2")), MapleDataTool.getInt(footHold.getChildByPath("y2"))), Integer.parseInt(footHold.getName()));
                    fh.setPrev((short) MapleDataTool.getInt(footHold.getChildByPath("prev")));
                    fh.setNext((short) MapleDataTool.getInt(footHold.getChildByPath("next")));

                    if (fh.getX1() < lBound.x) {
                        lBound.x = fh.getX1();
                    }
                    if (fh.getX2() > uBound.x) {
                        uBound.x = fh.getX2();
                    }
                    if (fh.getY1() < lBound.y) {
                        lBound.y = fh.getY1();
                    }
                    if (fh.getY2() > uBound.y) {
                        uBound.y = fh.getY2();
                    }
                    allFootholds.add(fh);
                }
            }
        }
        MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
        for (MapleFoothold fh : allFootholds) {
            fTree.insert(fh);
        }
        map.setFootholds(fTree);
        if (map.getTop() == 0) {
            map.setTop(lBound.y);
        }
        if (map.getBottom() == 0) {
            map.setBottom(uBound.y);
        }
        if (map.getLeft() == 0) {
            map.setLeft(lBound.x);
        }
        if (map.getRight() == 0) {
            map.setRight(uBound.x);
        }
        int bossid = -1;
        String msg = null;
        if (mapData.getChildByPath("info/timeMob") != null) {
            bossid = MapleDataTool.getInt(mapData.getChildByPath("info/timeMob/id"), 0);
            msg = MapleDataTool.getString(mapData.getChildByPath("info/timeMob/message"), null);
        }

        // load life data (npc, monsters)
        String type, limited;
        AbstractLoadedMapleLife myLife;

        for (MapleData life : mapData.getChildByPath("life")) {
            type = MapleDataTool.getString(life.getChildByPath("type"));
            limited = MapleDataTool.getString("limitedname", life, "");
            if ((npcs || !type.equals("n")) && limited.equals("")) {
                myLife = loadLife(life, MapleDataTool.getString(life.getChildByPath("id")), type);

                if (myLife instanceof MapleMonster && !GameConstants.isNoSpawn(mapid)) {
                    final MapleMonster mob = (MapleMonster) myLife;

                    map.addMonsterSpawn(mob,
                            MapleDataTool.getInt("mobTime", life, 0),
                            (byte) MapleDataTool.getInt("team", life, -1),
                            mob.getId() == bossid ? msg : null);

                } else if (myLife instanceof MapleNPC) {
                    map.addMapObject(myLife);
                }
            }
        }
        addAreaBossSpawn(map);
        map.setCreateMobInterval((short) MapleDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), 9000));
        map.setFixedMob(MapleDataTool.getInt(mapData.getChildByPath("info/fixedMobCapacity"), 0));
        map.setPartyBonusRate(GameConstants.getPartyPlay(mapid, MapleDataTool.getInt(mapData.getChildByPath("info/partyBonusR"), 0)));
        map.loadMonsterRate(true);
        map.setNodes(loadNodes(mapid, mapData));

        //load reactor data
        String id;
        if (reactors && mapData.getChildByPath("reactor") != null) {
            for (MapleData reactor : mapData.getChildByPath("reactor")) {
                id = MapleDataTool.getString(reactor.getChildByPath("id"));
                if (id != null) {
                    map.spawnReactor(loadReactor(reactor, id, (byte) MapleDataTool.getInt(reactor.getChildByPath("f"), 0)));
                }
            }
        }
        try {
            map.setMapName(MapleDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(mapid)), ""));
            map.setStreetName(MapleDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(mapid)), ""));
        } catch (Exception e) {
            map.setMapName("");
            map.setStreetName("");
        }
        map.setClock(MapleDataTool.getInt(mapData.getChildByPath("info/clock"), 0) > 0);
        map.setEverlast(MapleDataTool.getInt(mapData.getChildByPath("info/everlast"), 0) > 0);
        map.setTown(MapleDataTool.getInt(mapData.getChildByPath("info/town"), 0) > 0);
        map.setSoaring(MapleDataTool.getInt(mapData.getChildByPath("info/needSkillForFly"), 0) > 0);
        map.setForceMove(MapleDataTool.getInt(mapData.getChildByPath("info/lvForceMove"), 0));
        map.setHPDec(MapleDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
        map.setHPDecInterval(MapleDataTool.getInt(mapData.getChildByPath("info/decHPInterval"), 10000));
        map.setHPDecProtect(MapleDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
        map.setForcedReturnMap(MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
        map.setTimeLimit(MapleDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));
        map.setFieldLimit(MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
        map.setFirstUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
        map.setUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));
        map.setRecoveryRate(MapleDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));
        map.setConsumeItemCoolTime(MapleDataTool.getInt(mapData.getChildByPath("info/consumeItemCoolTime"), 0));
        map.setInstanceId(instanceid);
        lock.lock();
        try {
            instanceMap.put(instanceid, map);
        } finally {
            lock.unlock();
        }
        return map;
    }
}
