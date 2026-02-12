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

import client.*;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import scripting.EventInstanceManager;
import scripting.EventManager;
import server.*;
import server.MapleCarnivalFactory.MCSkill;
import server.MapleSquad.MapleSquadType;
import server.SpeedRunner.ExpeditionType;
import server.Timer.EtcTimer;
import server.Timer.MapTimer;
import server.events.MapleEvent;
import server.life.*;
import server.maps.MapleNodes.DirectionInfo;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.maps.MapleNodes.MonsterPoint;
import server.marriage.MarriageEventAgent;
import server.marriage.MarriageManager;
import server.quest.MapleQuest;
import tools.*;
import tools.packet.MobPacket;
import tools.packet.PetPacket;
import handling.channel.handler.InventoryHandler;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tools.packet.PartyPacket;
import tools.packet.UIPacket;

public final class MapleMap {

    private int runningOid = 500000;
    private final Lock runningOidLock = new ReentrantLock();
    private final List<Spawns> monsterSpawn = new ArrayList<Spawns>();
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final Map<Integer, MaplePortal> portals = new HashMap<Integer, MaplePortal>();
    private MapleFootholdTree footholds = null;
    private float monsterRate, recoveryRate;
    private MapleMapEffect mapEffect;
    private byte channel;
    private short decHP = 0, createMobInterval = 8000, top = 0, bottom = 0, left = 0, right = 0;
    public int consumeItemCoolTime = 0, protectItem = 0, decHPInterval = 10000, mapid, returnMapId, timeLimit,
            fieldLimit, maxRegularSpawn = 0, fixedMob, forcedReturnMap = 999999999, instanceid = -1,
            lvForceMove = 0, lvLimit = 0, permanentWeather = 0, partyBonusRate = 0;
    private boolean town, clock, personalShop, everlast = false, dropsDisabled = false, gDropsDisabled = false,
            soaring = false, squadTimer = false, isSpawns = true, checkStates = true,
            playerCanTalk = true;
    private String mapName, streetName, onUserEnter, onFirstUserEnter, speedRunLeader, changedMusic = "";
    private String bgm = "";
    private long setCommandTimer = 0;
    private List<Integer> dced = new ArrayList<Integer>();
    private ScheduledFuture<?> squadSchedule;
    private long speedRunStart = 0, lastSpawnTime = 0, lastHurtTime = 0, outMapTime = 0;
    private MapleNodes nodes;
    private MapleSquadType squad;
    private Map<String, Integer> environment = new LinkedHashMap<String, Integer>();
    private List<Integer> blockedMobGen = new LinkedList<Integer>();
    public List<MapleCharacter> seduceOrder = new LinkedList<>();
    private boolean canPetPickup = true;
    private int xy;
    private int fixSpawns;
    private double plusMob = 0;
    private double plusMobSize = 0;
    private double mobRate = 0;
    private AtomicInteger plusMobLastOsize = new AtomicInteger(0);

    private static final long MAP_VACUUM_COOLDOWN_MS = 500L; //자석 쿨
    private static final int MAP_VACUUM_MAX_ITEMS_PER_SWEEP = 60; // 자석

    private long plusMobLastTime = 0;
    EnumMap<MapleMapObjectType, ConcurrentHashMap<Integer, MapleMapObject>> mapobjects = new EnumMap<MapleMapObjectType, ConcurrentHashMap<Integer, MapleMapObject>>(MapleMapObjectType.class);
    private int mapOwnerPrivate = -1;
    private int mapOwnerParty = -1;
    private int mapOwnerExped = -1;
    private String mapOwnerName = "";

    public MapleMap(final int mapid, final int channel, final int returnMapId, final float monsterRate) {
        this.mapid = mapid;
        this.channel = (byte) channel;
        this.returnMapId = returnMapId;
        if (this.returnMapId == 999999999) {
            this.returnMapId = mapid;
        }
//        if (GameConstants.getPartyPlay(mapid) > 0) {
//            this.monsterRate = (monsterRate - 1.0f) * 2.5f + 1.0f;
//        } else {
        this.monsterRate = monsterRate;
//        }

        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            mapobjects.put(type, new ConcurrentHashMap<Integer, MapleMapObject>());
        }
    }

    public final SpawnPoint addMonsterSpawn(final MapleMonster monster, final int mobTime, final byte carnivalTeam, final String msg) {
        final Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        final SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg);
        if (carnivalTeam > -1) {
            monsterSpawn.add(0, sp); //at the beginning
        } else {
            monsterSpawn.add(sp);
            if (maxRegularSpawn > spawnedMonstersOnMap.get()) { //재추가
                sp.spawnMonster(this); //재추가
            }
        }
        return sp;
    }

    public void setBGM(String bgm) {
        this.bgm = (bgm == null) ? "" : bgm;
    }

    public String getBGM() {
        return bgm;
    }

    public String getCurrentBGM() {
        return changedMusic.isEmpty() ? bgm : changedMusic;
    }

    public void setXY(int x) {
        this.xy = x;
    }

    public final void setSpawns(final boolean fm) {
        this.isSpawns = fm;
    }

    public final boolean getSpawns() {
        return isSpawns;
    }

    public final void setFixedMob(int fm) {
        this.fixedMob = fm;
    }

    public final void setForceMove(int fm) {
        this.lvForceMove = fm;
    }

    public final int getForceMove() {
        return lvForceMove;
    }

    public final void setLevelLimit(int fm) {
        this.lvLimit = fm;
    }

    public final int getLevelLimit() {
        return lvLimit;
    }

    public final void setReturnMapId(int rmi) {
        this.returnMapId = rmi;
    }

    public final void setSoaring(boolean b) {
        this.soaring = b;
    }

    public final boolean canSoar() {
        return soaring;
    }

    public void canTalk(boolean b) {
        this.playerCanTalk = b;
    }

    public boolean canTalk() {
        return playerCanTalk;
    }

    public final void toggleDrops() {
        this.dropsDisabled = !dropsDisabled;
    }

    public final void setDrops(final boolean b) {
        this.dropsDisabled = b;
    }

    public final void toggleGDrops() {
        this.gDropsDisabled = !gDropsDisabled;
    }

    public final int getId() {
        return mapid;
    }

    public final MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public final int getReturnMapId() {
        return returnMapId;
    }

    public final int getForcedReturnId() {
        return forcedReturnMap;
    }

    public final MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public final void setForcedReturnMap(final int map) {
        this.forcedReturnMap = map;
    }

    public final float getRecoveryRate() {
        return recoveryRate;
    }

    public final void setRecoveryRate(final float recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public final int getFieldLimit() {
        return fieldLimit;
    }

    public final void setFieldLimit(final int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public final void setCreateMobInterval(final short createMobInterval) {
        this.createMobInterval = createMobInterval;
    }

    public final void setTimeLimit(final int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public final void setMapName(final String mapName) {
        this.mapName = mapName;
    }

    public final String getMapName() {
        return mapName;
    }

    public final String getStreetName() {
        return streetName;
    }

    public final void setFirstUserEnter(final String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public final void setUserEnter(final String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public final String getFirstUserEnter() {
        return onFirstUserEnter;
    }

    public final String getUserEnter() {
        return onUserEnter;
    }

    public final boolean hasClock() {
        return clock;
    }

    public final void setClock(final boolean hasClock) {
        this.clock = hasClock;
    }

    public final boolean isTown() {
        return town;
    }

    public final void setTown(final boolean town) {
        this.town = town;
    }

    public final boolean allowPersonalShop() {
        return personalShop;
    }

    public final void setPersonalShop(final boolean personalShop) {
        this.personalShop = personalShop;
    }

    public final void setStreetName(final String streetName) {
        this.streetName = streetName;
    }

    public final void setEverlast(final boolean everlast) {
        this.everlast = everlast;
    }

    public final boolean getEverlast() {
        return everlast;
    }

    public final int getHPDec() {
        return decHP;
    }

    public final void setHPDec(final int delta) {
        if (delta > 0 || mapid == 749040100) { //pmd
            lastHurtTime = System.currentTimeMillis(); //start it up
        }
        decHP = (short) delta;
    }

    public final int getHPDecInterval() {
        return decHPInterval;
    }

    public final void setHPDecInterval(final int delta) {
        decHPInterval = delta;
    }

    public final int getHPDecProtect() {
        return protectItem;
    }

    public final void setHPDecProtect(final int delta) {
        this.protectItem = delta;
    }

    public final int getCurrentPartyId() {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) obj;
            if (chr.getParty() != null) {
                return chr.getParty().getId();
            }
        }
        return -1;
    }

    public final void addMapObject(final MapleMapObject mapobject) {
        runningOidLock.lock();
        int newOid;
        try {
            newOid = ++runningOid;
        } finally {
            runningOidLock.unlock();
        }

        mapobject.setObjectId(newOid);
        mapobjects.get(mapobject.getType()).put(newOid, mapobject);
    }

    private void spawnAndAddRangedMapObject(final MapleMapObject mapobject, final DelayedPacketCreation packetbakery) {
        addMapObject(mapobject);

        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) obj;
            if ((mapobject.getType() == MapleMapObjectType.MIST || chr.getTruePosition().distanceSq(mapobject.getTruePosition()) <= GameConstants.maxViewRangeSq())) {
                packetbakery.sendPackets(chr.getClient());
                chr.addVisibleMapObject(mapobject);
            }
        }
    }

    public final void removeMapObject(final MapleMapObject obj) {
        mapobjects.get(obj.getType()).remove(Integer.valueOf(obj.getObjectId()));
    }

    public final Point calcPointBelow(final Point initial) {
        final MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            final double s1 = Math.abs(fh.getY2() - fh.getY1());
            final double s2 = Math.abs(fh.getX2() - fh.getX1());
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            } else {
                dropY = fh.getY1() + (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            }
        }
        return new Point(initial.x, dropY);
    }

    public final Point calcDropPos(final Point initial, final Point fallback) {
        final Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(final MapleCharacter chr, final MapleMonster mob, final boolean instanced, final int lastSkill) {
        if (mob.getId() == 8830000 || mob.getId() == 8830007 || mob == null || chr == null || ChannelServer.getInstance(channel) == null || dropsDisabled || mob.dropsDisabled()) { //no drops in pyramid ok? no cash either
            return;
        }

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);
        if (mob.getId() == 9300002) {//커파 커즈아이
            return;
        }
        int mobpos = mob.getTruePosition().x,
                xPosPlus = 12,
                smesorate = RateManager.MESO,
                sdroprate = RateManager.DROP,
                scashrate = RateManager.CASH;
        if (mob.getStats().isExplosiveReward()) {
            xPosPlus = 18;
        }
        Item idrop;
        Point pos = new Point(0, mob.getTruePosition().y);
        double showdown = 0.00;
        final MonsterStatusEffect mse = mob.getBuff(MonsterStatus.SHOWDOWN);
        if (mse != null) {
            showdown += mse.getX();
        } else if (mse == null && (lastSkill == 4121003 || lastSkill == 4221003)) {
            MapleStatEffect skillEffect = SkillFactory.getSkill(lastSkill).getEffect(chr.getTotalSkillLevel(lastSkill));
            if (!mob.getStats().isBoss()) {
                showdown += skillEffect.getX();
            }
        }

        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        final List<MonsterDropEntry> derp = mi.retrieveDrop(mob.getId());
        final List<MonsterDropEntry> dropEntry = new ArrayList<MonsterDropEntry>();
        final List< MonsterDropEntry> questEntry = new ArrayList<>();

        if (getId() >= 925020000 && getId() <= 925033804) {//무릉도장
            if (GameConstants.isMulungBoss(mob.getId())) {
                for (int idd = 2022359; idd <= 2022421; ++idd) {
                    dropEntry.add(new MonsterDropEntry(mob.getId(), idd, 3000, 1, 1, 0));
                }
            } else {
                for (int idd = 2022430; idd <= 2022433; ++idd) {
                    dropEntry.add(new MonsterDropEntry(mob.getId(), idd, 150000, 1, 1, 0));
                }
            }
        } else if (derp == null) { //if no drops, no global drops either <3
            return;
        } else {
            dropEntry.addAll(derp);
        }
        if (chr.checkPcTime()) {
            int chance = 2000;
            if (getId() / 10000000 == 19 || getId() / 100000 == 9501) {
                chance = 5000;
            }
            dropEntry.add(new MonsterDropEntry(mob.getId(), 4000047, chance, 1, 1, 0));
            // 순서대로 아이템, 확률, 최소, 최대, 퀘스트
        }
        Collections.shuffle(dropEntry);

        int nIdx = 1;
        int drop_rate_value = 0;
        boolean drop_rate = false;
        if (chr.getBuffedValue(MapleBuffStat.DROP_RATE) != null) {
            drop_rate = true;
            drop_rate_value = chr.getBuffedValue(MapleBuffStat.DROP_RATE);
        }

        for (final MonsterDropEntry de : dropEntry) {
            if (de.itemId == mob.getStolen()) {
                continue;
            }
            if (de.questid > 0) {
                questEntry.add(new MonsterDropEntry(mob.getId(), de.itemId, de.chance, de.Minimum, de.Maximum, de.questid));
                continue;
            }
            double MDrate = 0.0;
            if (chr.getBuffedValue(MapleBuffStat.MESO_DROP_RATE) != null) {//meso up by item
                MDrate = chr.getBuffedValue(MapleBuffStat.MESO_DROP_RATE) / 100.0D;
            } else {
                MDrate = 1.0;
            }
            double multiplier = 0.00;
            multiplier = sdroprate;
            if (showdown != 0) {
                multiplier += showdown / 100;
            }
            if (drop_rate) {
                multiplier += (drop_rate_value - 100) / 100;
            }
            int chance = (int) (de.chance * multiplier);
            if (de.itemId == 0) {
                chance *= MDrate;
            }
            if (drop_rate) {
                multiplier += (drop_rate_value - 100) / 100;
            }
            if (Randomizer.rand(0, 999999) < chance) {
                pos.x = mobpos + (nIdx % 2 == 0 ? (xPosPlus * nIdx) : -(xPosPlus * (nIdx - 1)));

                if (de.itemId == 0) { // meso
                    int mesos = Randomizer.rand(de.Minimum, de.Maximum);
                    if (mesos > 0) {
                        if (chr.getBuffedValue(MapleBuffStat.MESOUP) != null) {
                            double rate = chr.getBuffedValue(MapleBuffStat.MESOUP) / 100.0D;
                            mesos *= rate;
                        }
                        spawnMobMesoDrop((int) (mesos * (chr.getStat().mesoBuff / 100.0) * smesorate), calcDropPos(pos, mob.getTruePosition()), mob, chr, false, droptype);
                    }
                } else {
                    if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        final int range = Math.abs(de.Maximum - de.Minimum);
                        idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(range <= 0 ? 1 : range) + de.Minimum : 1), (byte) 0);
                    }
                    idrop.setGMLog("Dropped from monster " + mob.getId() + " on " + mapid);
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, de.questid);
                }
                nIdx++;
            }
        }
        final List<MonsterGlobalDropEntry> globalEntry = new ArrayList<MonsterGlobalDropEntry>(mi.getGlobalDrop());
        Collections.shuffle(globalEntry);
        final int cashz = (int) ((mob.getStats().isBoss() && mob.getStats().getHPDisplayType() == 0 ? 20 : 1) * scashrate);
        final int cashModifier = (int) ((mob.getStats().isBoss() ? (mob.getStats().isPartyBonus() ? (mob.getMobExp() / 1000) : 0) : (mob.getMobExp() / 1000 + mob.getMobMaxHp() / 20000))); //no rate

        // Global Drops
        if (chr.getEventInstance() == null) {
            globalEntry.add(new MonsterGlobalDropEntry(4030012, 1000, -1, (byte) 0, 1, 1, 0)); //몬스터 카드
            globalEntry.add(new MonsterGlobalDropEntry(2439990, 7500, -1, (byte) 0, 1, 1, 0)); //미라클 큐브
            globalEntry.add(new MonsterGlobalDropEntry(4039999, 50000, -1, (byte) 0, 1, 5, 0)); //넥슨캐시 100원

            globalEntry.add(new MonsterGlobalDropEntry(2046000, 40, -1, (byte) 0, 1, 1, 0)); //한무공 100%
            globalEntry.add(new MonsterGlobalDropEntry(2046001, 40, -1, (byte) 0, 1, 1, 0)); //한무마 100%
            globalEntry.add(new MonsterGlobalDropEntry(2046100, 40, -1, (byte) 0, 1, 1, 0)); //두무공 100%
            globalEntry.add(new MonsterGlobalDropEntry(2046200, 60, -1, (byte) 0, 1, 1, 0)); //방어구힘
            globalEntry.add(new MonsterGlobalDropEntry(2046201, 60, -1, (byte) 0, 1, 1, 0)); //방어구지력
            globalEntry.add(new MonsterGlobalDropEntry(2046202, 60, -1, (byte) 0, 1, 1, 0)); //방어구민첩
            globalEntry.add(new MonsterGlobalDropEntry(2046203, 30, -1, (byte) 0, 1, 1, 0)); //방어구행운
            globalEntry.add(new MonsterGlobalDropEntry(2430144, 500, -1, (byte) 0, 1, 1, 0)); //비밀의 마스터리 북
            globalEntry.add(new MonsterGlobalDropEntry(2439994, 3000, -1, (byte) 0, 1, 1, 0)); //마스터 미라클 큐브
            globalEntry.add(new MonsterGlobalDropEntry(2430191, 30, -1, (byte) 0, 1, 1, 0)); //프로텍트 실드

            globalEntry.add(new MonsterGlobalDropEntry(4032374, 300000, -1, (byte) 1, 1, 1, 2405));//잃어버린 표창장 (전사)
            globalEntry.add(new MonsterGlobalDropEntry(4032376, 300000, -1, (byte) 1, 1, 1, 2406));//잃어버린 표창장 (마법사)
            globalEntry.add(new MonsterGlobalDropEntry(4032377, 300000, -1, (byte) 1, 1, 1, 2407));//잃어버린 표창장 (궁수)
            globalEntry.add(new MonsterGlobalDropEntry(4032378, 300000, -1, (byte) 1, 1, 1, 2408));//잃어버린 표창장 (도적)
            globalEntry.add(new MonsterGlobalDropEntry(4032379, 300000, -1, (byte) 1, 1, 1, 2409));//잃어버린 표창장 (해적)

            globalEntry.add(new MonsterGlobalDropEntry(4310004, 150000, -1, (byte) 0, 1, 1, 9271)); //아이스박스

            globalEntry.add(new MonsterGlobalDropEntry(2439987, 1, -1, (byte) 0, 1, 1, 0)); // 5000 후캐
        }

        for (final MonsterGlobalDropEntry de : globalEntry) {
            if (de.questid > 0) {
                if ((de.continent < 0 || (de.continent < 10 && mapid / 100000000 == de.continent) || (de.continent < 100 && mapid / 10000000 == de.continent) || (de.continent < 1000 && mapid / 1000000 == de.continent))) {
                    questEntry.add(new MonsterDropEntry(mob.getId(), de.itemId, de.chance, de.Minimum, de.Maximum, de.questid));
                }
                continue;
            }

            int chance = de.chance;

            // 2439987: 몬스터 레벨 기반 확률
            if (de.itemId == 2439987) {
                final int lvl = mob.getStats().getLevel();

                double dyn = lvl * 4.0;
                final double growth = 1.01;
                dyn *= Math.pow(growth, lvl / 10.0);
                chance = (int) Math.max(0, Math.min(999999, Math.round(dyn)));
                chance = Math.min(chance, 5000);
            }

            if (Randomizer.nextInt(999999) < (long) chance * sdroprate && (de.continent < 0 || (de.continent < 10 && mapid / 100000000 == de.continent) || (de.continent < 100 && mapid / 10000000 == de.continent) || (de.continent < 1000 && mapid / 1000000 == de.continent))) {
                pos.x = mobpos + (nIdx % 2 == 0 ? (xPosPlus * nIdx) : -(xPosPlus * (nIdx - 1)));
                if (de.itemId == 0) {
                    //chr.modifyCSPoints(1, (int) ((Randomizer.nextInt(cashz) + cashz + cashModifier) * (chr.getStat().cashBuff / 100.0) * chr.getCashMod()), true);
                } else if (!gDropsDisabled && chr.getEventInstance() == null && chr.getPyramidSubway() == null) {
                    if (de.itemId == 2439990) {
                        //레벨이 72보다 많으면 미라클 큐브 
                        if (mob.getStats().getLevel() < 71) {
                            continue;
                        }
                    } else if (de.itemId == 2046000 || de.itemId == 2046001 || de.itemId == 2046100 || de.itemId == 2046200 || de.itemId == 2046201 || de.itemId == 2046202 || de.itemId == 2046203 || de.itemId == 2430144) {
                        //몹 레벨 120 이상 다양한 아이템
                        if (mob.getStats().getLevel() < 120) {
                            continue;
                        }
                    } else if (de.itemId == 2439994) {
                        //몹 레벨 120이상 마스터 미라클 큐브
                        if (mob.getStats().getLevel() < 120) {
                            continue;
                        }
                    } else if (de.itemId == 2430191) {
                        //몹 레벨 120이상 프로텍트 실드
                        if (mob.getStats().getLevel() < 120) {
                            continue;
                        }
                    }
                    if (mob.getId() / 100000 != 9) {
                        if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                            idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                        } else {
                            idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1), (byte) 0);
                        }
                        idrop.setGMLog("Dropped from monster " + mob.getId() + " on " + mapid + " (Global)");
                        spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, de.onlySelf ? 0 : droptype, de.questid);
                        nIdx++;
                    }
                }
            }
        }
        for (final MonsterDropEntry de : questEntry) {
            if (Randomizer.nextInt(999999) < de.chance * sdroprate) {
                if (de.itemId == 4310004) {
                    if (mob.getStats().getLevel() < 71) {
                        continue;
                    } else if (mob.getStats().getLevel() < 140) {
                        if (Math.abs(mob.getStats().getLevel() - chr.getLevel()) > 30) {
                            continue;
                        }
                    }

                }
                pos.x = mobpos + (nIdx % 2 == 0 ? (xPosPlus * nIdx) : -(xPosPlus * (nIdx - 1)));
                if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                    idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                } else {
                    idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1), (byte) 0);
                }
                idrop.setGMLog("Dropped from monster " + mob.getId() + " on " + mapid);
                spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, de.questid);
                nIdx++;
            }
        }

        int flag = mob.getEventDropFlag();
        if (flag > 0) {
            List<Integer> eventDrops = new ArrayList<>();
            if ((flag & 1) > 0) {
                eventDrops.add(3010007);
            }
            if ((flag & 2) > 0) {
                eventDrops.add(3010008);
            }
            if ((flag & 4) > 0) {
                eventDrops.add(3010009);
            }
            if ((flag & 8) > 0) {
                eventDrops.add(3010000);
            }
            if ((flag & 0x10) > 0) {
                eventDrops.add(2210000);
            }
            if ((flag & 0x20) > 0) {
                eventDrops.add(2210001);
            }
            if ((flag & 0x40) > 0) {
                eventDrops.add(2210002);
            }
            if ((flag & 0x80) > 0) {
                eventDrops.add(5370000);
            }
            for (int itemId : eventDrops) {
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    idrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    idrop = new Item(itemId, (byte) 0, (short) 1, (byte) 0);
                }
                pos.x = mobpos + (nIdx % 2 == 0 ? (xPosPlus * nIdx) : -(xPosPlus * (nIdx - 1)));
                //pos.x = (mobpos + ((nIdx % 2 == 0) ? (22 * (nIdx + 1) / 2) : -(22 * (nIdx / 2))));
                idrop.setGMLog("Dropped from monster " + mob.getId() + " on " + mapid + " (Event)");
                spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, 0);
                nIdx++;
            }
            FileoutputUtil.log("log_event_item.txt", "Found monster"
                    + " at " + StringUtil.getCurrentTime()
                    + " by " + chr.getName()
                    + " monster id : " + mob.getId()
                    + " in " + getStreetName() + " : " + getMapName()
                    + " , flag : " + flag
                    + "\r\n\r\n");
        }
    }

    public void removeMonster(final MapleMonster monster) {
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0));
        removeMapObject(monster);
        monster.killed();
    }

    public void killMonster(final MapleMonster monster) { // For mobs with removeAfter
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        if (monster.getLinkCID() <= 0) {
            monster.spawnRevives(this);
        }
        if (monster.getId() == 9300410) { //훈련로봇 B 하드코딩
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), monster.getStats().getSelfD() < 0 ? 1 : 2));
        } else {
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), monster.getStats().getSelfD() < 0 ? 1 : monster.getStats().getSelfD()));
        }
        removeMapObject(monster);
        monster.killed();
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean second, byte animation) {
        killMonster(monster, chr, withDrops, second, animation, 0);
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean second, byte animation, final int lastSkill) {
        if ((monster.getId() == 8810122 || monster.getId() == 8810018 || monster.getId() == 8810214) && !second) {
            MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    killMonster(monster, chr, true, true, (byte) 1);
                    killAllMonsters(true);
                }
            }, 3000);
            return;
        }
        if (monster.getId() == 8820014) { //pb sponge, kills pb(w) first before dying
            killMonster(8820000);
        } else if (monster.getId() == 9300166) { //ariant pq bomb
            animation = 4; //or is it 3?
        } else if (monster.getId() == 8300007) {
            final Point pos = new Point(-401, -10);
            spawnNpc(2085003, pos); //드래곤 라이더
        } else if (monster.getId() == 9300281) {
            if (getReactorById(2118003) != null) {
                chr.getMap().getReactorById(2118003).forceHitReactor((byte) 0);
            }
        }
        if (monster.getStats().getSelfDHp() > 0 || monster.getId() == 9300410) { //블러드봄 자폭 뎀
            if (monster.getHp() <= 0) {
                animation = 1;
            } else {
                animation = 2;
            }
        }
        spawnedMonstersOnMap.decrementAndGet();
        removeMapObject(monster);
        monster.killed();
        final MapleSquad sqd = getSquadByMap();
        final boolean instanced = sqd != null || monster.getEventInstance() != null || getEMByMap() != null;
        int dropOwner = monster.killBy(chr, lastSkill);
        switch (monster.getId()) {
            case 8145102:
            case 8145202:
                if (getId() == 223030210) {
                    chr.getMap().spawnNpc(2192000, new Point(-336, 4));
                }
                break;
            case 8810122:
            case 8810018:
                if (!second) {
                    MapTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            killMonster(monster, chr, true, true, (byte) 1);
                            killAllMonsters(true);
                        }
                    }, 3000);
                    return;
                }
                break;
            case 8820014:
                killMonster(8820000);
                break;
            case 8840002: // 레드 크로키
                if (getId() == 211060201) {
                    if (chr.getQuestStatus(3139) == 1) {
                        if (getAllMonster().size() <= 1) {
                            chr.dropMessage(-1, "사자왕의 성 첫번째 봉인이 풀렸습니다.");
                            chr.getQuestNAdd(MapleQuest.getInstance(3139)).setCustomData("1");
                        }
                    }
                }
                break;
            case 8210006: // 교도관 보어
                if (getId() == 211060401) {
                    if (chr.getQuestStatus(3140) == 1) {
                        if (getAllMonster().size() <= 1) {
                            chr.dropMessage(-1, "사자왕의 성 두번째 봉인이 풀렸습니다.");
                            chr.getQuestNAdd(MapleQuest.getInstance(3140)).setCustomData("1");
                        }
                    }
                }
            case 8210007: // 교도관 라이노
                if (getId() == 211060601) {
                    if (chr.getQuestStatus(3141) == 1) {
                        if (getAllMonster().size() <= 1) {
                            chr.dropMessage(-1, "사자왕의 성 세번째 봉인이 풀렸습니다.");
                            chr.getQuestNAdd(MapleQuest.getInstance(3141)).setCustomData("1");
                        }
                    }
                }
                break;
            case 9300166:
                animation = 4; //or is it 3?
                break;
            case 9300326:
                if (getId() >= 910520000 && getId() <= 910520004) { // 트리스탄의 무덤
                    chr.getMap().spawnNpc(1061015, new Point(-30, 190));
                }
                break;
        }
        if (animation >= 0) {
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation));
        }

        if (monster.getBuffToGive() > -1) {
            final int buffid = monster.getBuffToGive();
            final MapleStatEffect buff = MapleItemInformationProvider.getInstance().getItemEffect(buffid);

            for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
                MapleCharacter mc = (MapleCharacter) _obj;
                if (mc.isAlive()) {
                    buff.applyTo(mc);

                    switch (monster.getId()) {
                        case 8810018:
                        case 8810214:
                            mc.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(buffid, 13, mc.getLevel(), 1)); // HT nine spirit
                            broadcastMessage(mc, MaplePacketCreator.showBuffeffect(mc.getId(), buffid, 13, mc.getLevel(), 1), false); // HT nine spirit
                            break;
                    }
                }
            }
        }
        if (chr.getBuffedValue(MapleBuffStat.REAPER) != null
                && chr.getBuffSource(MapleBuffStat.REAPER) == 32111006) {
            final MapleStatEffect eff = chr.getStatForBuff(MapleBuffStat.REAPER);
            if (eff.makeChanceResult() && chr.getSummonsSize() <= 5) {
                final MapleSummon summon = new MapleSummon(chr, 32111006, eff.getLevel(),
                        monster.getTruePosition(), SummonMovementType.WALK_STATIONARY);
                this.spawnSummon(summon);
                chr.addSummon(summon);
            }
        }
        if (chr.getEventInstance() == null) {
            //확률이 5%, 레벨이 100이상 몹이 90이상이거나 레벨차가 15이하면 캐시를 지급 사냥 캐쉬 폐쇄
            /*if (Randomizer.rand(0, 100) <= 5 && ((chr.getLevel() >= 100 && monster.getStats().getLevel() >= 90) || Math.abs(monster.getStats().getLevel() - chr.getLevel()) <= 15)) {
             int amount = Randomizer.rand(1, (ServerConstants.CashRate * monster.getStats().getLevel()));//monster.getStats().getLevel()) 다시 1부터
             chr.modifyCSPoints(1, amount, false);
             chr.getClient().getSession().write(MaplePacketCreator.showGainNx(amount));
             //  chr.dropMessage(6,"ㅇㅇ:"+amount);
             }*/
        }

        chr.setLastBattleTime(System.currentTimeMillis());
        final int mobid = monster.getId();
        ExpeditionType type = null;
        if (mobid == 8810018 && mapid == 240060200) { // Horntail
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "수많은 도전 끝에 혼테일을 격파한 원정대여! 그대들이 진정한 리프레의 영웅이다!"));
            //FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Horntail;
            }
            for (MapleCharacter chri : getCharacters()) {
                MapleQuestStatus qs = chri.getQuestNAdd(MapleQuest.getInstance(136000));
                if (qs.getCustomData() == null) {
                    qs.setCustomData("1");
                } else {
                    qs.setCustomData(String.valueOf(Integer.parseInt(qs.getCustomData()) + 1));
                }
            }
            doShrine(true);
        } else if (mobid == 8810214 && mapid == 240060300) {
            //FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
            for (MapleCharacter chri : getCharacters()) {
                MapleQuestStatus qs = chri.getQuestNAdd(MapleQuest.getInstance(136000));
                if (qs.getCustomData() == null) {
                    qs.setCustomData("1");
                } else {
                    qs.setCustomData(String.valueOf(Integer.parseInt(qs.getCustomData()) + 1));
                }
            }
            doShrine(true);
        } else if (mobid == 8800002 && mapid == 280030000) {
//            FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Zakum;
            }
            doShrine(true);
        } else if (mobid == 8800102 && mapid == 280030001) {
//            FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            doShrine(true);
        } else if (mobid == 8810122 && mapid == 240060201) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "수많은 도전 끝에 카오스 혼테일을 격파한 원정대여! 그대들이 진정한 리프레의 영웅이다!"));
            doShrine(true);
        } else if (mobid == 8840000) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "끝없는 투지로 검은 마법사의 군단장인 사자왕 반 레온을 쓰러뜨린 원정대여! 그대들이 메이플 월드를 구할 영웅이다!"));
        } else if (mobid == 8850011) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "어둠으로 물든 에레브를 구한 영웅들이여! 여제와 신수가 그대들을 위한 거대한 축복을 선사하리!"));
        } else if (mobid == 8820001 && mapid == 270050100) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "지치지 않는 열정으로 핑크빈을 물리친 원정대여! 그대들이 진정한 시간의 승리자다!"));

//            FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Zakum;
            }
            for (MapleCharacter chri : getCharacters()) {
                MapleQuestStatus qs = chri.getQuestNAdd(MapleQuest.getInstance(136001));
                if (qs.getCustomData() == null) {
                    qs.setCustomData("1");
                } else {
                    qs.setCustomData(String.valueOf(Integer.parseInt(qs.getCustomData()) + 1));
                }
            }
            doShrine(true);
        } else if (mobid >= 8800003 && mobid <= 8800010) {
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMapObject object : monsters) {
                    final MapleMonster mons = ((MapleMonster) object);
                    if (mons.getId() == 8800000) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        MapleMonster mob = MapleLifeFactory.getMonster(8800000);
                        mob.setHp((long) (mob.getHp() * SystemUtils.getHpModByDay()));
                        spawnMonsterOnGroundBelow(mob, pos);
                        break;
                    }
                }
            }
        } else if (mobid >= 8800103 && mobid <= 8800110) {
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800103 && mons.getId() <= 8800110) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMonster mons : monsters) {
                    if (mons.getId() == 8800100) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        MapleMonster mob = MapleLifeFactory.getMonster(8800100);
                        mob.setHp((long) (mob.getHp() * SystemUtils.getHpModByDay()));
                        spawnMonsterOnGroundBelow(mob, pos);
                        break;
                    }
                }
            }
        } else if (mobid == 8820008) { //wipe out statues and respawn
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 8820010 && mobid <= 8820014) {
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820000 && mons.getId() != 8820001 && mons.getObjectId() != monster.getObjectId() && mons.isAlive() && mons.getLinkOid() == monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid == 9300003) {//킹슬라임 첫번째 동행
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300012) {//알리샤르 차원의 균열
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300182) {//초강화형 포이즌 골렘 독안개의 숲
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 8300007) {//드래곤 라이더
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300119 || mobid == 9300105 || mobid == 9300106 || mobid == 9300107) {//데비존
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300139 || mobid == 9300140) {//프랑켄
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 9300281) {//렉스
            broadcastMessage(MaplePacketCreator.showPQreward(chr.getId()));
        } else if (mobid == 6160003) {//크세르크세스
            startMapEffect("크세르크세를 물리쳤습니다. 왼쪽 포탈을 통해 퇴장해 주세요.", 5120025);
        }
        if (type != null) {
            if (speedRunStart > 0 && speedRunLeader.length() > 0) {
                long endTime = System.currentTimeMillis();
                String time = StringUtil.getReadableMillis(speedRunStart, endTime);
                broadcastMessage(MaplePacketCreator.serverNotice(5, speedRunLeader + "'s squad has taken " + time + " to defeat " + type.name() + "!"));
                getRankAndAdd(speedRunLeader, time, type, (endTime - speedRunStart), (sqd == null ? null : sqd.getMembers()));
                endSpeedRun();
            }

        }
        if (withDrops && dropOwner != 1) {
            MapleCharacter drop;
            if (dropOwner <= 0) {
                drop = chr;
            } else {
                drop = getCharacterById(dropOwner);
                if (drop == null) {
                    drop = chr;
                }
            }
            //dropFromMonster(drop, monster, instanced, lastSkill);
            dropFromMonster(drop, monster, instanced, lastSkill);
            vacuumOwnedDrops(drop);
        }
        /*드라 하드코딩*/
        if (chr.getMapId() == 240080100 || chr.getMapId() == 240080200 || chr.getMapId() == 240080300 || chr.getMapId() == 240080400 || chr.getMapId() == 240080500) {
            if (getMobsSize() == 0) {
                //broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
            } else {
                broadcastMessage(UIPacket.getTopMsg("몬스터가 " + getMobsSize() + "마리 남았습니다."));
            }
        }
        if (chr.getMapId() == 240080600) {
            if (getMobsSize() == 0) {
                chr.getMap().startMapEffect("드래고니카를 물리쳤습니다. 포탈을 통해 이동해 주세요", 5120026);
                //broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
            }
        }
        if (chr.getMapId() == 240080800) {
            if (getMobsSize() == 0) {
                chr.getMap().startMapEffect("못돼처먹은 드래곤 라이더를 물리쳤습니다. 왼쪽 포탈을 통해 이동해 주세요", 5120026);
                //broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
            }
        }
        if (spawnedMonstersOnMap.get() == 0) {
            if (mapid / 1000 == 240080) {
                broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
                broadcastMessage(MaplePacketCreator.playSound("Party1/Clear"));
            }
        }
    }

    public List<MapleReactor> getAllReactor() {
        return getAllReactorsThreadsafe();
    }

    public List<MapleReactor> getAllReactorsThreadsafe() {
        ArrayList<MapleReactor> ret = new ArrayList<MapleReactor>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            ret.add((MapleReactor) mmo);
        }
        return ret;
    }

    public List<MapleSummon> getAllSummonsThreadsafe() {
        ArrayList<MapleSummon> ret = new ArrayList<MapleSummon>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SUMMON).values()) {
            if (mmo instanceof MapleSummon) {
                ret.add((MapleSummon) mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllDoor() {
        return getAllDoorsThreadsafe();
    }

    public List<MapleMapObject> getAllDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
            if (mmo instanceof MapleDoor) {
                ret.add(mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllMechDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
            if (mmo instanceof MechDoor) {
                ret.add(mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllMerchant() {
        return getAllHiredMerchantsThreadsafe();
    }

    public List<MapleMapObject> getAllHiredMerchantsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values()) {
            ret.add(mmo);
        }
        return ret;
    }

    public List<MapleMapObject> getAllShopsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values()) {
            ret.add(mmo);
        }
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SHOP).values()) {
            ret.add(mmo);
        }
        return ret;
    }

    public List<MapleMonster> getAllMonster() {
        return getAllMonstersThreadsafe();
    }

    public List<MapleMonster> getAllMonstersThreadsafe() {
        ArrayList<MapleMonster> ret = new ArrayList<MapleMonster>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            ret.add((MapleMonster) mmo);
        }
        return ret;
    }

    public List<Integer> getAllUniqueMonsters() {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            final int theId = ((MapleMonster) mmo).getId();
            if (!ret.contains(theId)) {
                ret.add(theId);
            }
        }
        return ret;
    }

    public final void killAllMonsters(final boolean animate) {
        for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
            final MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
            removeMapObject(monster);
            monster.killed();
        }
    }

    public final void killMonster(final int monsId) {
        for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
            MapleMonster mob;
            mob = (MapleMonster) mmo;
            //killMonster(mob, c.getPlayer(), true, false, (byte) 1);
            if (((MapleMonster) mmo).getId() == monsId) {
                MapleMonster mons = (MapleMonster) mmo;
                mons.killed();
                spawnedMonstersOnMap.decrementAndGet();
                removeMapObject(mmo);
                killMonster(mob);
                broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1));
                ((MapleMonster) mmo).killed();
                break;
            }
        }
    }

    private String MapDebug_Log() {
        final StringBuilder sb = new StringBuilder("Defeat time : ");
        sb.append(FileoutputUtil.CurrentReadable_Time());

        sb.append(" | Mapid : ").append(this.mapid);

        sb.append(" Users [").append(mapobjects.get(MapleMapObjectType.PLAYER).size()).append("] | ");
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter mc = (MapleCharacter) _obj;
            sb.append(mc.getName()).append(", ");
        }
        return sb.toString();
    }

    public final void limitReactor(final int rid, final int num) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        Map<Integer, Integer> contained = new LinkedHashMap<Integer, Integer>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (contained.containsKey(mr.getReactorId())) {
                if (contained.get(mr.getReactorId()) >= num) {
                    toDestroy.add(mr);
                } else {
                    contained.put(mr.getReactorId(), contained.get(mr.getReactorId()) + 1);
                }
            } else {
                contained.put(mr.getReactorId(), 1);
            }
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactors(final int first, final int last) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                toDestroy.add(mr);
            }
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactor(final int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        if (reactor == null) {
            return;
        }
        broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);

        if (reactor.getDelay() > 0) {
            MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public final void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public final void reloadReactors() {
        List<MapleReactor> toSpawn = new ArrayList<MapleReactor>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            final MapleReactor reactor = (MapleReactor) obj;
            broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
            reactor.setAlive(false);
            reactor.setTimerActive(false);
            toSpawn.add(reactor);
        }
        for (MapleReactor r : toSpawn) {
            removeMapObject(r);
            if (!r.isCustom()) { //guardians cpq
                respawnReactor(r);
            }
        }
    }

    /*
     * command to reset all item-reactors in a map to state 0 for GM/NPC use - not tested (broken reactors get removed
     * from mapobjects when destroyed) Should create instances for multiple copies of non-respawning reactors...
     */
    public final void resetReactors() {
        setReactorState((byte) 0);
    }

    public final void setReactorState() {
        setReactorState((byte) 1);
    }

    public final void setReactorState(final byte state) {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            ((MapleReactor) obj).forceHitReactor((byte) state);
        }
    }

    public final void setReactorDelay(final int state) {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            ((MapleReactor) obj).setDelay(state);
        }
    }

    /*
     * command to shuffle the positions of all reactors in a map for PQ purposes (such as ZPQ/LMPQ)
     */
    public final void shuffleReactors() {
        shuffleReactors(0, 9999999); //all
    }

    public final void shuffleReactors(int first, int last) {
        List<Point> points = new ArrayList<Point>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last && mr.getReactorId() != 2001016) { //hardcode - tower of goddess
                points.add(mr.getPosition());
            }
        }
        Collections.shuffle(points);
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last && mr.getReactorId() != 2001016) {
                mr.setPosition(points.remove(points.size() - 1));
            }
        }
    }

    //soory for poor hard coding
    public final void shuffleReactors_RomeoJuliet() {
        List<Point> points = new ArrayList<Point>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (!mr.getName().contains("out")) {
                points.add(mr.getPosition());
            }
        }
        Collections.shuffle(points);
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (!mr.getName().contains("out")) {
                mr.setPosition(points.remove(points.size() - 1));
            }
        }
    }

    /**
     * Automagically finds a new controller for the given monster from the chars
     * on the map...
     *
     * @param monster
     */
    public final void updateMonsterController(final MapleMonster monster) {
        if (!monster.isAlive() || monster.getLinkCID() > 0 || monster.getStats().isEscort()) {
            return;
        }
        if (monster.getController() != null) {
            if (monster.getController().getMap() != this || monster.getController().isVacFucking()/* || monster.getController().getTruePosition().distanceSq(monster.getTruePosition()) > monster.getRange()*/) {
                monster.getController().stopControllingMonster(monster);
            } else { // Everything is fine :)
                return;
            }
        }
        int mincontrolled = -1;
        MapleCharacter newController = null;
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) _obj;
            if (!chr.isHidden() && !chr.isVacFucking() && (chr.getControlledSize() < mincontrolled || mincontrolled == -1)/* && chr.getTruePosition().distanceSq(monster.getTruePosition()) <= monster.getRange()*/) {
                mincontrolled = chr.getControlledSize();
                newController = chr;
            }
        }

        if (newController != null) {
            if (monster.isFirstAttack()) {
                boolean canHaveAggro = true;
                if (newController.getCarnivalParty() != null) {
                    if (newController.getCarnivalParty().getTeam() == monster.getCarnivalTeam()) {
                        //Carnival Team's Monster do not attack team player.
                        canHaveAggro = false;
                    }
                }
                newController.controlMonster(monster, canHaveAggro);
                monster.setControllerHasAggro(canHaveAggro);
            } else {
                newController.controlMonster(monster, false);
            }
        }
    }

    public final MapleMapObject getMapObject(int oid, MapleMapObjectType type) {
        return mapobjects.get(type).get(oid);
    }

    public final boolean containsNPC(int npcid) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC n = (MapleNPC) itr.next();
            if (n.getId() == npcid) {
                return true;
            }
        }
        return false;
    }

    public MapleNPC getNPCById(int id) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC n = (MapleNPC) itr.next();
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    public MapleMonster getMonsterById(int id) {
        MapleMonster ret = null;
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
        while (itr.hasNext()) {
            MapleMonster n = (MapleMonster) itr.next();
            if (n.getId() == id) {
                ret = n;
                break;
            }
        }
        return ret;
    }

    public int countMonsterById(int id) {
        int ret = 0;
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
        while (itr.hasNext()) {
            MapleMonster n = (MapleMonster) itr.next();
            if (n.getId() == id) {
                ret++;
            }
        }
        return ret;
    }

    public MapleReactor getReactorById(int id) {
        MapleReactor ret = null;
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.REACTOR).values().iterator();
        while (itr.hasNext()) {
            MapleReactor n = (MapleReactor) itr.next();
            if (n.getReactorId() == id) {
                ret = n;
                break;
            }
        }
        return ret;
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public final MapleMonster getMonsterByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.MONSTER);
        if (mmo == null) {
            return null;
        }
        return (MapleMonster) mmo;
    }

    public final MapleSummon getSummonByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.SUMMON);
        if (mmo == null) {
            return null;
        }
        return (MapleSummon) mmo;
    }

    public final MapleNPC getNPCByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.NPC);
        if (mmo == null) {
            return null;
        }
        return (MapleNPC) mmo;
    }

    public final MapleReactor getReactorByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.REACTOR);
        if (mmo == null) {
            return null;
        }
        return (MapleReactor) mmo;
    }

    public final MapleReactor getReactorByName(final String name) {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = ((MapleReactor) obj);
            if (mr.getName().equalsIgnoreCase(name)) {
                return mr;
            }
        }
        return null;
    }

    public final void spawnNpc(final int id, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(id);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos).getId());
        npc.setCustom(true);
        addMapObject(npc);
        broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
    }

    public final void removeNpc(final int npcid) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC npc = (MapleNPC) itr.next();
            if (npc.isCustom() && (npcid == -1 || npc.getId() == npcid)) {
                broadcastMessage(MaplePacketCreator.removeNPCController(npc.getObjectId()));
                broadcastMessage(MaplePacketCreator.removeNPC(npc.getObjectId()));
                itr.remove();
            }
        }
    }

    public final void hideNpc(final int npcid) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC npc = (MapleNPC) itr.next();
            if (npcid == -1 || npc.getId() == npcid) {
                broadcastMessage(MaplePacketCreator.removeNPCController(npc.getObjectId()));
                broadcastMessage(MaplePacketCreator.removeNPC(npc.getObjectId()));
            }
        }
    }

    public final void spawnReactorOnGroundBelow(final MapleReactor mob, final Point pos) {
        mob.setPosition(pos); //reactors dont need FH lol
        mob.setCustom(true);
        spawnReactor(mob);
    }

    public final void spawnMonster_sSack(final MapleMonster mob, final Point pos, final int spawnType) {
        mob.setPosition(calcPointBelow(new Point(pos.x, pos.y - 1)));
        spawnMonster(mob, spawnType);
    }

    public final void spawnMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        spawnMonster_sSack(mob, pos, -2);
    }

    public final int spawnMonsterWithEffectBelow(final MapleMonster mob, final Point pos, final int effect) {
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        return spawnMonsterWithEffect(mob, effect, spos, (short) 0);
    }

    public final void spawnZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800000);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800003, 8800004, 8800005, 8800006, 8800007,
            8800008, 8800009, 8800010};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);
            part.setHp((long) (part.getHp() * SystemUtils.getHpModByDay()));
            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnChaosZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800100);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800103, 8800104, 8800105, 8800106, 8800107,
            8800108, 8800109, 8800110};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);
            part.setHp((long) (part.getHp() * SystemUtils.getHpModByDay()));
            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnFakeMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        spos.y -= 1;
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    private void checkRemoveAfter(final MapleMonster monster) {
        final int ra = monster.getStats().getRemoveAfter();

        if (ra > 0 && monster.getLinkCID() <= 0) {
            monster.registerKill(ra * 1000);
        }
    }

    public final void spawnRevives(final MapleMonster monster, final int oid) {
        monster.setMap(this);
        checkRemoveAfter(monster);
        monster.setLinkOid(oid);
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            @Override
            public final void sendPackets(MapleClient c) {
                if (!c.getPlayer().isGSD()) {
                    //투명 보스
                    c.getSession().write(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() >= 0 ? -3 : monster.getStats().getSummonType(), oid)); // TODO effect 좀 봐야 알듯 셀리노
                }
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnMonster(final MapleMonster monster, final int spawnType) {
        spawnMonster(monster, spawnType, false);
    }

    public final void spawnMonster(final MapleMonster monster, final int spawnType, final boolean overwrite) {
        monster.setMap(this);
        checkRemoveAfter(monster);

        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                if (!c.getPlayer().isGSD()) {
                    c.getSession().write(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 || monster.getStats().getSummonType() == 27 || overwrite ? spawnType : monster.getStats().getSummonType(), 0));
                }
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public final int spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos, short tDelay) {
        try {
            monster.setMap(this);
            monster.setPosition(pos);
            checkRemoveAfter(monster);

            spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
                @Override
                public final void sendPackets(MapleClient c) {
                    if (!c.getPlayer().isGSD()) {
                        c.getSession().write(MobPacket.spawnMonster(monster, effect, tDelay));
                    }
                }
            });
            updateMonsterController(monster);

            spawnedMonstersOnMap.incrementAndGet();
            return monster.getObjectId();
        } catch (Exception e) {
            return -1;
        }
    }

    public final void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);

        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
            @Override
            public final void sendPackets(MapleClient c) {
                if (!c.getPlayer().isGSD()) {
                    c.getSession().write(MobPacket.spawnMonster(monster, -4, 0));
                }
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);

        spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {
            @Override
            public final void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnReactor(reactor));
            }
        });
    }

    public void spawnMessageBox(final MapleMessageBox msgbox) {
        spawnAndAddRangedMapObject(msgbox, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                msgbox.sendSpawnData(c);
            }
        });
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public final void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {
            public final void sendPackets(MapleClient c) {
                door.sendSpawnData(c, true);
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        });
    }

    public final void spawnMechDoor(final MechDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

            public final void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.spawnMechDoor(door, true));
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        });
    }

    public final void spawnSummon(final MapleSummon summon) {
        summon.updateMap(this);
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                if (summon != null && c.getPlayer() != null) {
                    c.getSession().write(MaplePacketCreator.spawnSummon(summon, true));
                }
            }
        });
    }

    public final void spawnMist(final MapleMist mist, final int duration, boolean fake) {
        spawnAndAddRangedMapObject(mist, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                mist.sendSpawnData(c);
            }
        });

        final MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        switch (mist.isPoisonMist()) {
            case 0:
                final MapleCharacter owner = getCharacterById(mist.getOwnerId());
                poisonSchedule = tMan.register(new Runnable() {
                    @Override
                    public void run() {
                        for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                            if (mist.makeChanceResult() && !((MapleMonster) mo).isBuffed(MonsterStatus.POISON)) {
                                ((MapleMonster) mo).applyStatus(owner, new MonsterStatusEffect(MonsterStatus.POISON, 1, mist.getSourceSkill().getId(), null, false), true, duration, true, mist.getSource());
                            }
                        }
                    }
                }, 2000, 2500);
                break;
            case 1:
                poisonSchedule = tMan.register(new Runnable() {
                    @Override
                    public void run() {
                        for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                            if (mist.makeChanceResult()) {
                                final MapleCharacter chr = ((MapleCharacter) mo);
                                chr.giveDebuff(MapleDisease.POISON, MobSkillFactory.getMobSkill(125, 3), (short) 0);
                            }
                        }
                    }
                }, 2000, 2500);
                break;
            case 4:
                poisonSchedule = tMan.register(new Runnable() {
                    @Override
                    public void run() {
                        for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                            if (mist.makeChanceResult()) {
                                final MapleCharacter chr = ((MapleCharacter) mo);
                                if (mist.getChr() == chr.getId()) {
                                    chr.addMP((int) (mist.getSource().getX() * (chr.getStat().getMaxMp() / 100.0)));
                                } else if (chr.getParty() != null && chr.getParty().getMemberById(mist.getChr()) != null) {
                                    chr.addMP((int) (mist.getSource().getX() * (chr.getStat().getMaxMp() / 100.0)));
                                }
                            }
                        }
                    }
                }, 2000, 2500);
                break;
            default:
                poisonSchedule = null;
                break;
        }
        mist.setPoisonSchedule(poisonSchedule);
        mist.setSchedule(tMan.schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(MaplePacketCreator.removeMist(mist.getObjectId(), false));
                removeMapObject(mist);
                if (poisonSchedule != null) {
                    poisonSchedule.cancel(false);
                }
            }
        }, duration));
    }

    public final void disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 3), drop.getTruePosition());
    }

    public final void spawnMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                if (owner.getMapId() / 10000 == 24008 && owner.getMapId() != 240080800) {
                    c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getTruePosition(), dropper.getTruePosition(), (byte) 1));
                } else {
                    c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getTruePosition(), droppos, (byte) 1));
                }
            }
        });
        if (!everlast) {
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }
    }

    public final void spawnMobMesoDrop(final int meso, final Point position, final MapleMonster dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final MapleMapItem mdrop = new MapleMapItem(meso, position, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                // c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getTruePosition(), position, (byte) 1));//북마크

                // if (owner.getMapId() / 10000 == 24008 && owner.getMapId() != 240080800) {
                //     c.getSession().write(MaplePacketCreator.dropItemFromMonster(mdrop, dropper.getTruePosition(), 0, dropper.getTruePosition(), (byte) 1));
                //} else {
                c.getSession().write(MaplePacketCreator.dropItemFromMonster(mdrop, dropper.getTruePosition(), dropper.getId(), position, (byte) 1));
                // }
                //c.getSession().write(MaplePacketCreator.dropItemFromMonster(mdrop, dropper.getTruePosition(), 0, position, (byte) 1));
            }
        });

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
    }

    public final void spawnMobDrop(final Item idrop, final Point dropPos, final MapleMonster mob, final MapleCharacter chr, final byte droptype, final int questid) {
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, droptype, false, questid);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                boolean canShow = questid <= 0 || c.getPlayer().getQuestStatus(questid) == 1;
                Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(idrop.getItemId());
                if (questInfo != null && questid == questInfo.getLeft() && c.getPlayer().getQuestStatus(questid) == 1) {
                    canShow = !c.getPlayer().haveItem(idrop.getItemId(), questInfo.getRight(), true, true);
                }
                if (c != null && c.getPlayer() != null && canShow && mob != null && dropPos != null) {
                    //c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, mob.getTruePosition(), dropPos, (byte) 1));
                    //c.getSession().write(MaplePacketCreator.dropItemFromMonster(mdrop, mob.getTruePosition(), 0, dropPos, (byte) 1));
                    //0으로 안하면 파티시 템이 바로 안먹어지는 듯
                    //if (chr.getMapId() / 10000 == 24008 && chr.getMapId() != 240080800) {
                    //c.getSession().write(MaplePacketCreator.dropItemFromMonster(mdrop, mob.getTruePosition(), 0, mob.getTruePosition(), (byte) 1));
                    //  } else {
                    c.getSession().write(MaplePacketCreator.dropItemFromMonster(mdrop, mob.getTruePosition(), mob.getId(), dropPos, (byte) 1));
                    // }
                }
            }
        });
//	broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mdrop, mob.getTruePosition(), dropPos, (byte) 0));

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnRandDrop() {
        if (mapid != 910000000 || channel != 1) {
            return; //fm, ch1
        }

        for (MapleMapObject o : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            if (((MapleMapItem) o).isRandDrop()) {
                return;
            }
        }
        MapTimer.getInstance().schedule(new Runnable() {
            public void run() {
                final Point pos = new Point(Randomizer.nextInt(800) + 531, -806);
                final int theItem = Randomizer.nextInt(1000);
                int itemid = 0;
                if (theItem < 950) { //0-949 = normal, 950-989 = rare, 990-999 = super
                    itemid = GameConstants.normalDrops[Randomizer.nextInt(GameConstants.normalDrops.length)];
                } else if (theItem < 990) {
                    itemid = GameConstants.rareDrops[Randomizer.nextInt(GameConstants.rareDrops.length)];
                } else {
                    itemid = GameConstants.superDrops[Randomizer.nextInt(GameConstants.superDrops.length)];
                }
                spawnAutoDrop(itemid, pos);
            }
        }, 20000);
    }

    public final void spawnAutoDrop(final int itemid, final Point pos) {
        Item idrop = null;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            idrop = ii.randomizeStats((Equip) ii.getEquipById(itemid));
        } else {
            idrop = new Item(itemid, (byte) 0, (short) 1, (byte) 0);
        }
        idrop.setGMLog("Dropped from auto " + " on " + mapid);
        final MapleMapItem mdrop = new MapleMapItem(pos, idrop);
        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, pos, pos, (byte) 1));
            }
        });
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mdrop, pos, pos, (byte) 0));
        if (itemid == 4001101) {
            mdrop.registerExpire(6000000);
        } else if (itemid / 10000 != 291) {
            mdrop.registerExpire(120000);
        }
    }

    public final void spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, Point pos, final boolean ffaDrop, final boolean playerDrop) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 2, playerDrop);

        final boolean canShow;
        Pair<Integer, Integer> questInfo = MapleItemInformationProvider.getInstance().getQuestItemInfo(item.getItemId());
        if (questInfo != null) {
            canShow = !owner.haveItem(item.getItemId(), questInfo.getRight(), true, true);
            drop.setQuest(questInfo.getLeft());
        } else {
            canShow = true;
        }
        spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                if (canShow) {
                    if (owner.getMapId() / 10000 == 24008 && owner.getMapId() != 240080800) {
                        c.getSession().write(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getTruePosition(), dropper.getTruePosition(), (byte) 1));
                    } else {
                        c.getSession().write(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 1));
                    }
                    c.getSession().write(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 1));
                }
            }
        });
        if (canShow) {
            if (owner.getMapId() / 10000 == 24008 && owner.getMapId() != 240080800) {
                broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getTruePosition(), dropper.getTruePosition(), (byte) 0));
            } else {
                broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 0));
            }
        }

        if (!everlast) {
            if (item.getItemId() == 4001101 || item.getItemId() == 4001454) {
                drop.registerExpire(6000000);
            } else {
                drop.registerExpire(120000);
            }
            activateItemReactors(drop, owner.getClient());
        }
    }

    private void activateItemReactors(final MapleMapItem drop, final MapleClient c) {
        final Item item = drop.getItem();

        for (final MapleMapObject o : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            final MapleReactor react = (MapleReactor) o;
            /*if (react.getReactorId() == 2006000) {
             if (react.getArea().contains(drop.getTruePosition())) {
             if (item.getItemId() == 4001063 && item.getQuantity() == 20) {
             MapTimer.getInstance().schedule(new Runnable() {
             @Override
             public void run() {
             List<MapleMapItem> items = getAllItemsThreadsafe();
             for (MapleMapItem i : items) {
             if (i.getItemId() == 4001063) {
             if (i.getItem().getQuantity() == 20) { // 오르비스 리엑터
             i.expire(c.getPlayer().getMap());
             react.forceStartReactor(c);
             }
             }
             }
             }
             }, 3500);
             }
             }
             }*/
            if (react.getReactorType() == 100) {
                boolean canActivate = false;
                //hardcode cause too lazy (...)
                if (react.getReactorId() == 2008006) { //OrbisPQ
                    canActivate = (item.getItemId() >= 4001056 && item.getItemId() <= 4001062) && react.getReactItem().getRight() == item.getQuantity();
                } else if (react.getReactorId() == 2408002) { //HorntailPQ
                    canActivate = (item.getItemId() >= 4001088 && item.getItemId() <= 4001091) && react.getReactItem().getRight() == item.getQuantity();
                } else { //Default
                    canActivate = item.getItemId() == react.getReactItem().getLeft() && react.getReactItem().getRight() == item.getQuantity();
                }
                if (canActivate) {
                    if (react.getArea().contains(drop.getTruePosition())) {
                        if (!react.isTimerActive()) {
                            MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
                            react.setTimerActive(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    public int getItemsSize() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public int getMessageBoxSize() {
        return mapobjects.get(MapleMapObjectType.MESSAGEBOX).size();
    }

    public int getMobsSize() {
        return mapobjects.get(MapleMapObjectType.MONSTER).size();
    }

    public List<MapleMapItem> getAllItems() {
        return getAllItemsThreadsafe();
    }

    public List<MapleMapItem> getAllItemsThreadsafe() {
        ArrayList<MapleMapItem> ret = new ArrayList<MapleMapItem>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            ret.add((MapleMapItem) mmo);
        }
        return ret;
    }

    private void vacuumOwnedDrops(final MapleCharacter chr) { // 자석
        if (chr == null || chr.getMap() != this || chr.hasBlockedInventory()) {
            return;
        }
        if (!chr.isMagnetEnabled()) { // 온오프
            return;
        }
        if (!chr.tryMagnetVacuum(MAP_VACUUM_COOLDOWN_MS)) {
            return;
        }

        final MapleClient c = chr.getClient();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        int picked = 0;
        for (final MapleMapItem mapitem : getAllItemsThreadsafe()) {
            if (picked >= MAP_VACUUM_MAX_ITEMS_PER_SWEEP) {
                break;
            }
            if (mapitem == null || mapitem.isPickedUp()) {
                continue;
            }
            if (mapitem.getOwner() != chr.getId()) {
                continue;
            }

            final Lock lock = mapitem.getLock();
            lock.lock();
            try {
                if (mapitem.isPickedUp()) {
                    continue;
                }
                if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                    continue;
                }

                // Mesos (실패 가능성 거의 없으니 여기서 선점 후 제거)
                if (mapitem.getMeso() > 0) {
                    mapitem.setPickedUp(true);
                    chr.gainMeso(mapitem.getMeso(), false, false); // 자석 메소 가시성
                    removeMapItemAfterPickup(chr, mapitem);
                    picked++;
                    continue;
                }

                final int itemId = mapitem.getItemId();

                // 자석 금지 목록
                if (chr.isMagnetBanned(itemId)) {
                    continue;
                }

                // Pickup-blocked items (except capture items)
                if (ii.isPickupBlocked(itemId) && itemId / 10000 != 291) {
                    continue;
                }

                // Auto-use / capture / cash coupon 로직
                final boolean used = InventoryHandler.useItem(c, itemId);
                if (used) {
                    mapitem.setPickedUp(true);
                    removeMapItemAfterPickup(chr, mapitem);

                    // capture items
                    if (itemId / 10000 == 291) {
                        chr.getMap().broadcastMessage(MaplePacketCreator.getCapturePosition(chr.getMap()));
                        chr.getMap().broadcastMessage(MaplePacketCreator.resetCapture());
                    }

                    picked++;
                    continue;
                } else if (itemId == 4039999) { // 캐시 쿠폰
                    // 인벤 공간 불필요, 성공 처리만 하면 됨
                    mapitem.setPickedUp(true);

                    final byte rand = (byte) Randomizer.rand(2, 10);
                    chr.modifyCSPoints(1, 100 * rand, false);
                    chr.getClient().getSession().write(MaplePacketCreator.showGainNx(100, rand));
                    removeMapItemAfterPickup(chr, mapitem);
                    chr.getClient().getSession().write(MaplePacketCreator.enableActions());

                    picked++;
                    continue;
                }

                // 일반 아이템: 공간 체크 먼저 (선점은 그 다음)
                final Item item = mapitem.getItem();
                if (item == null) {
                    continue;
                }

                if (!MapleInventoryManipulator.checkSpace(c, itemId, item.getQuantity(), item.getOwner())) {
                    // 인벤 꽉 참 -> 그냥 안 줍고 남겨둠 (나중에 공간 만들면 다시 주울 수 있음)
                    continue;
                }

                // 여기서부터는 거의 성공 확정 -> 선점
                mapitem.setPickedUp(true);

                if (item.getQuantity() >= 50 && itemId == 2340000) {
                    c.setMonitored(true);
                }

                if (MapleInventoryManipulator.addFromDrop(c, item, true, mapitem.getDropper() instanceof MapleMonster)) {
                    removeMapItemAfterPickup(chr, mapitem);
                    picked++;
                } else {
                    // addFromDrop 실패는 드물지만, 혹시 모르니 롤백
                    mapitem.setPickedUp(false);
                }

            } finally {
                lock.unlock();
            }
        }
    }

    private void removeMapItemAfterPickup(final MapleCharacter chr, final MapleMapItem mapitem) {
        mapitem.setPickedUp(true);
        broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
        removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            spawnRandDrop();
        }
    }

    public List<MapleMessageBox> getAllMsgBoxesThreadsafe() {
        ArrayList<MapleMessageBox> ret = new ArrayList<MapleMessageBox>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MESSAGEBOX).values()) {
            ret.add((MapleMessageBox) mmo);
        }
        return ret;
    }

    public Point getPointOfItem(int itemid) {
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            MapleMapItem mm = ((MapleMapItem) mmo);
            if (mm.getItem() != null && mm.getItem().getItemId() == itemid) {
                return mm.getPosition();
            }
        }
        return null;
    }

    public List<MapleMist> getAllMistsThreadsafe() {
        ArrayList<MapleMist> ret = new ArrayList<MapleMist>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MIST).values()) {
            ret.add((MapleMist) mmo);
        }
        return ret;
    }

    public final void returnEverLastItem(final MapleCharacter chr) {
        for (final MapleMapObject o : getAllItemsThreadsafe()) {
            final MapleMapItem item = ((MapleMapItem) o);
            if (item.getOwner() == chr.getId()) {
                item.setPickedUp(true);
                broadcastMessage(MaplePacketCreator.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getTruePosition());
                if (item.getMeso() > 0) {
                    chr.gainMeso(item.getMeso(), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false);
                }
                removeMapObject(item);
            }
        }
        spawnRandDrop();
    }

    public final void talkMonster(final String msg, final int itemId, final int objectid) {
        if (itemId > 0) {
            startMapEffect(msg, itemId, false);
        }
        broadcastMessage(MobPacket.talkMonster(objectid, itemId, msg)); //5120035
        broadcastMessage(MobPacket.removeTalkMonster(objectid));
    }

    public final void startMapEffect(final String msg, final int itemId) {
        startMapEffect(msg, itemId, false);
    }

    public final void startMapEffect(final String msg, final int itemId, final boolean jukebox) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        mapEffect.setJukebox(jukebox);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (mapEffect != null) {
                    broadcastMessage(mapEffect.makeDestroyData());
                    mapEffect = null;
                }
            }
        }, jukebox ? 300000 : 30000);
    }

    public final void startExtendedMapEffect(final String msg, final int itemId) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(MaplePacketCreator.removeMapEffect());
                broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, false));
                //dont remove mapeffect.
            }
        }, 60000);
    }

    public final void startSimpleMapEffect(final String msg, final int itemId) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
    }

    public final void startJukebox(final String msg, final int itemId) {
        startMapEffect(msg, itemId, true);
    }

    private boolean onFirstUserEnterScriptRunned = false;

    public final void addPlayer(final MapleCharacter chr) {
        mapobjects.get(MapleMapObjectType.PLAYER).put(chr.getObjectId(), chr);

        chr.setChangeTime();
        if (GameConstants.isTeamMap(mapid)) {
            chr.setTeam(getAndSwitchTeam() ? 0 : 1);
        }
        final byte[] packet = MaplePacketCreator.spawnPlayerMapobject(chr);
        if (!chr.isHidden()) {
            broadcastMessage(packet);
            if (chr.isIntern() && speedRunStart > 0) {
                endSpeedRun();
                broadcastMessage(MaplePacketCreator.serverNotice(5, "The speed run has ended."));
            }
        } else {
            broadcastGMMessage(chr, packet, false);
        }
        if (!onFirstUserEnter.equals("")) {
            if (!onFirstUserEnterScriptRunned) {
                onFirstUserEnterScriptRunned = true;
                MapScriptMethods.startScript_FirstUser(chr.getClient(), onFirstUserEnter);
            }
        }
        sendObjectPlacement(chr);

        chr.getClient().getSession().write(packet);

        if (!onUserEnter.equals("")) {
            MapScriptMethods.startScript_User(chr.getClient(), onUserEnter);
        }
//        GameConstants.achievementRatio(chr.getClient());
        //chr.getClient().getSession().write(MaplePacketCreator.spawnFlags(nodes.getFlags()));
        if (GameConstants.isTeamMap(mapid)) {
            chr.getClient().getSession().write(MaplePacketCreator.showEquipEffect(chr.getTeam()));
        }
        //final Point pos = chr.getTruePosition();
        for (final MaplePet pet : chr.getPets()) {
            if (pet.getSummoned()) {
                //pet.setPos(pos);
                pet.setFh(0);
                broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), false);
                //chr.getClient().sendPacket(PetPacket.loadPetPickupExceptionList(chr.getId(), pet.getUniqueId(), pet.getPickupExceptionList(), (byte) (pet.getSummonedValue() - 1)));
            }
        }
        if (chr.getParty() != null) {
            chr.silentPartyUpdate();
            chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), chr.getParty(), PartyOperation.SILENT_UPDATE, null));
            chr.updatePartyMemberHP();
            chr.receivePartyMemberHP();
            if (chr.getParty().getExpeditionId() > 0) {
                final MapleExpedition exped = World.Party.getExped(chr.getParty().getExpeditionId());
                if (exped != null) {
                    World.Party.expedPacket(exped.getId(), PartyPacket.expeditionUpdate(exped), null);
                }
            }
        }
        final List<MapleSummon> ss = chr.getSummonsReadLock();
        try {
            for (MapleSummon summon : ss) {
                summon.setPosition(chr.getTruePosition());
                chr.addVisibleMapObject(summon);
                this.spawnSummon(summon);
            }
        } finally {
            chr.unlockSummonsReadLock();
        }

        for (MapleCharacter cS : getCharactersThreadsafe()) { // 서몬! 소환수 팅
            if (chr.getId() != cS.getId()) {
                final List< MapleSummon> css = cS.getSummonsReadLock();
                try {
                    for (MapleSummon summon : css) {
                        chr.getClient().getSession().write(MaplePacketCreator.spawnSummon(summon, false));
                    }
                } finally {
                    cS.unlockSummonsReadLock();
                }
            }
        }

        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (MarriageEventAgent.isWeddingMap(mapid)) {
            MarriageManager.getInstance().getEventAgent(chr.getClient().getChannel()).checkEnterMap(chr);
        }
        if (timeLimit > 0 && getForcedReturnMap() != null) {
            chr.startMapTimeLimitTask(timeLimit, getForcedReturnMap());
        }
        if (chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null && !GameConstants.isResist(chr.getJob())) {
            if (FieldLimitType.Mount.check(fieldLimit)) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            }
        }
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));

        }
        if (hasClock()) {
            final Calendar cal = Calendar.getInstance();
            chr.getClient().getSession().write((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }
        if (!changedMusic.isEmpty()) {
            chr.getClient().getSession().write(MaplePacketCreator.musicChange(changedMusic));
        }
        if (setCommandTimer > System.currentTimeMillis()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) ((setCommandTimer - System.currentTimeMillis()) / 1000)));
        }
        if (/*chr.getCarnivalParty() != null && */chr.getEventInstance() != null) {
            chr.getEventInstance().onMapLoad(chr);
        }
        MapleEvent.mapLoad(chr, channel);
        if (getSquadBegin() != null && getSquadBegin().getTimeLeft() > 0 && getSquadBegin().getStatus() == 1) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (getSquadBegin().getTimeLeft() / 1000)));
        }
        if (mapid / 1000 != 105100 && mapid / 100 != 8020003 && mapid / 100 != 8020008 && mapid != 271040100) { //no boss_balrog/2095/coreblaze/auf/cygnus. but coreblaze/auf/cygnus does AFTER
            final MapleSquad sqd = getSquadByMap(); //for all squads
            final EventManager em = getEMByMap();
            if (!squadTimer && sqd != null && chr.getName().equals(sqd.getLeaderName()) && em != null && em.getProperty("leader") != null && em.getProperty("leader").equals("true") && checkStates) {
                //leader? display
                doShrine(false);
                squadTimer = true;
            }
        }
        if (getNumMonsters() > 0 && (mapid == 280030001 || mapid == 240060201 || mapid == 280030000 || mapid == 240060200 || mapid == 220080001 || mapid == 541020800 || mapid == 541010100 || mapid == 240060300)) {
            String music = "Bgm09/TimeAttack";
            switch (mapid) {
                case 240060200:
                case 240060201:
                case 240060300:
                    music = "Bgm14/HonTale";
                    break;
                case 280030000:
                    music = "Bgm06/FinalFight";
                    break;
                case 280030001:
                    music = "BgmEX/ChaosZakum";
                    break;
            }
            chr.getClient().getSession().write(MaplePacketCreator.musicChange(music));
            //maybe timer too for zak/ht
        }
        if (mapid >= 914000000 && mapid <= 914000500) {
            if (chr.getJob() == 2000 && chr.getLevel() < 10) {
                chr.getClient().getSession().write(MaplePacketCreator.temporaryStats_Aran());
            } else {
                chr.getClient().getSession().write(MaplePacketCreator.temporaryStats_Reset());
            }
        } else if (mapid == 105100300 && chr.getLevel() >= 91) {
            chr.getClient().getSession().write(MaplePacketCreator.temporaryStats_Balrog(chr));
        } else {//if (mapid == 140090000 || mapid == 105100301 || mapid == 105100401 || mapid == 105100100) {
            chr.getClient().getSession().write(MaplePacketCreator.temporaryStats_Reset());
        }
        if (GameConstants.isEvan(chr.getJob()) && chr.getJob() >= 2200) {
            if (chr.getDragon() == null) {
                chr.makeDragon();
            } else {
                chr.getDragon().setPosition(chr.getPosition());
            }
            if (chr.getDragon() != null) {
                broadcastMessage(MaplePacketCreator.spawnDragon(chr.getDragon()));
            }
        }

//        if (mapid == 0 && chr.getJob() == 0) {
//            chr.getClient().getSession().write(MaplePacketCreator.startMapEffect("Welcome to " + chr.getClient().getChannelServer().getServerName() + "!", 5122000, true));
//            chr.dropMessage(1, "Welcome to " + chr.getClient().getChannelServer().getServerName() + ", " + chr.getName() + " ! \r\nUse @npc to collect your Item Of Appreciation once you're level 10! \r\nUse @help for commands. \r\nGood luck and have fun!");
//            chr.dropMessage(5, "Your EXP Rate will be set to " + GameConstants.getExpRate_Below10(chr.getJob()) + "x until you reach level 10.");
//            chr.dropMessage(5, "Use @npc to collect your Item Of Appreciation once you're level 10! Use @help for commands. Good luck and have fun!");
//
//        }
        if (permanentWeather > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.startMapEffect("", permanentWeather, false)); //snow, no msg
        }
        if (getPlatforms().size() > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.getMovingPlatforms(this));
        }
        if (environment.size() > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.getUpdateEnvironment(this));
        }
        if (partyBonusRate > 0) {
            chr.dropMessage(-1, "이곳에서는 파티원 1명 당 " + partyBonusRate + "%의 추가 경험치가 적용됩니다.");
            chr.dropMessage(-1, "파티 플레이 존에 입장하셨습니다.");
        }

        Collection<MapleMonster> mobs = getAllMonster();
        for (MapleMonster mob : mobs) {
            if (mob.getController() == null) {
                updateMonsterController(mob);
            }
        }

        recalcCanSpawnMobs();
    }

    public int getNumItems() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public int getNumMonsters() {
        return mapobjects.get(MapleMapObjectType.MONSTER).size();
    }

    public void doShrine(final boolean spawned) { //false = entering map, true = defeated
        if (squadSchedule != null) {
            cancelSquadSchedule(true);
        }
        final MapleSquad sqd = getSquadByMap();
        if (sqd == null) {
            return;
        }
        final int mode = (mapid == 280030000 ? 1 : (mapid == 280030001 ? 2 : (mapid == 240060200 || mapid == 240060201 || mapid == 240060300 ? 3 : 0)));
        //chaos_horntail message for horntail too because it looks nicer
        final EventManager em = getEMByMap();
        if (sqd != null && em != null && getCharactersSize() > 0) {
            final String leaderName = sqd.getLeaderName();
            final String state = em.getProperty("state");
            final Runnable run;
            MapleMap returnMapa = getForcedReturnMap();
            if (returnMapa == null || returnMapa.getId() == mapid) {
                returnMapa = getReturnMap();
            }
//            if (mode == 1 || mode == 2) { //chaoszakum
//                broadcastMessage(MaplePacketCreator.showChaosZakumShrine(spawned, 5));
//            } else if (mode == 3) { //ht/chaosht
//                broadcastMessage(MaplePacketCreator.showChaosHorntailShrine(spawned, 5));
//            } else {
//                broadcastMessage(MaplePacketCreator.showHorntailShrine(spawned, 5));
//            }
            if (spawned) { //both of these together dont go well
                broadcastMessage(MaplePacketCreator.getClock(300)); //5 min
            }
            final MapleMap returnMapz = returnMapa;
            if (!spawned) { //no monsters yet; inforce timer to spawn it quickly
                final List<MapleMonster> monsterz = getAllMonstersThreadsafe();
                final List<Integer> monsteridz = new ArrayList<Integer>();
                for (MapleMapObject m : monsterz) {
                    monsteridz.add(m.getObjectId());
                }
                run = new Runnable() {
                    public void run() {
                        final MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        if (MapleMap.this.getCharactersSize() > 0 && MapleMap.this.getNumMonsters() == monsterz.size() && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            boolean passed = monsterz.isEmpty();
                            for (MapleMapObject m : MapleMap.this.getAllMonstersThreadsafe()) {
                                for (int i : monsteridz) {
                                    if (m.getObjectId() == i) {
                                        passed = true;
                                        break;
                                    }
                                }
                                if (passed) {
                                    break;
                                } //even one of the monsters is the same
                            }
                            if (passed) {
                                //are we still the same squad? are monsters still == 0?
//                                byte[] packet;
//                                if (mode == 1 || mode == 2) { //chaoszakum
//                                    packet = MaplePacketCreator.showChaosZakumShrine(spawned, 0);
//                                } else {
//                                    packet = MaplePacketCreator.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
//                                }
                                for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
//                                    chr.getClient().getSession().write(packet);
                                    chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                                }
                                checkStates("");
                                resetFully();
                            }
                        }

                    }
                };
            } else { //inforce timer to gtfo
                run = new Runnable() {
                    public void run() {
                        MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        //we dont need to stop clock here because they're getting warped out anyway
                        if (MapleMap.this.getCharactersSize() > 0 && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            //are we still the same squad? monsters however don't count
//                            byte[] packet;
//                            if (mode == 1 || mode == 2) { //chaoszakum
//                                packet = MaplePacketCreator.showChaosZakumShrine(spawned, 0);
//                            } else {
//                                packet = MaplePacketCreator.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
//                            }
                            for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
//                                chr.getClient().getSession().write(packet);
                                chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                            }
                            checkStates("");
                            resetFully();
                        }
                    }
                };
            }
            squadSchedule = MapTimer.getInstance().schedule(run, 300000); //5 mins
            if (!spawned) {
                broadcastMessage(MaplePacketCreator.serverNotice(6, "보스 몬스터를 소환하지 않으면 5분 후 자동 퇴장됩니다."));
            } else {
                broadcastMessage(MaplePacketCreator.serverNotice(6, "5분 후 자동으로 퇴장됩니다."));
            }
        }
    }

    public final MapleSquad getSquadByMap() {
        MapleSquadType zz = null;
        switch (mapid) {
            case 105100400:
            case 105100300:
                zz = MapleSquadType.bossbalrog;
                break;
            case 280030000:
                zz = MapleSquadType.zak;
                break;
            case 280030001:
                zz = MapleSquadType.chaoszak;
                break;
            case 240060200:
                zz = MapleSquadType.horntail;
                break;
            case 240060201:
                zz = MapleSquadType.chaoshorntail;
                break;
            case 240060300:
                zz = MapleSquadType.easyht;
                break;
            case 270050100:
                zz = MapleSquadType.pinkbean;
                break;
            case 802000111:
                zz = MapleSquadType.nmm_squad;
                break;
            case 802000211:
                zz = MapleSquadType.vergamot;
                break;
            case 802000311:
                zz = MapleSquadType.tokyo_2095;
                break;
            case 802000411:
                zz = MapleSquadType.dunas;
                break;
            case 802000611:
                zz = MapleSquadType.nibergen_squad;
                break;
            case 802000711:
                zz = MapleSquadType.dunas2;
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                zz = MapleSquadType.core_blaze;
                break;
            case 802000821:
            case 802000823:
                zz = MapleSquadType.aufheben;
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                zz = MapleSquadType.vonleon;
                break;
            case 800020415:
                zz = MapleSquadType.yasunaga;
                break;
            case 551030200:
                zz = MapleSquadType.scartar;
                break;
            case 271040100:
                zz = MapleSquadType.cygnus;
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getMapleSquad(zz);
    }

    public final MapleSquad getSquadBegin() {
        if (squad != null) {
            return ChannelServer.getInstance(channel).getMapleSquad(squad);
        }
        return null;
    }

    public final EventManager getEMByMap() {
        String em = null;
        switch (mapid) {
            case 105100400:
                em = "BossBalrog_EASY";
                break;
            case 105100300:
                em = "BossBalrog_NORMAL";
                break;
            case 280030000:
                em = "ZakumBattle";
                break;
            case 240060200:
                em = "HorntailBattle";
                break;
            case 280030001:
                em = "ChaosZakum";
                break;
            case 240060201:
                em = "ChaosHorntailBattle";
                break;
            case 240060300:
                em = "EasyHorntail";
                break;
            case 270050100:
                em = "PinkBeanBattle";
                break;
            case 802000111:
                em = "NamelessMagicMonster";
                break;
            case 802000211:
                em = "Vergamot";
                break;
            case 802000311:
                em = "2095_tokyo";
                break;
            case 802000411:
                em = "Dunas";
                break;
            case 802000611:
                em = "Nibergen";
                break;
            case 802000711:
                em = "Dunas2";
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                em = "CoreBlaze";
                break;
            case 802000821:
            case 802000823:
                em = "Aufhaven";
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                em = "VonLeonBattle";
                break;
            case 800020415:
                em = "YasunagaBattle";
                break;
            case 551030200:
                em = "ScarTarBattle";
                break;
            case 271040100:
                em = "CygnusBattle";
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getEventSM().getEventManager(em);
    }

    public final void removePlayer(final MapleCharacter chr) {
        //log.warn("[dc] [level2] Player {} leaves map {}", new Object[] { chr.getName(), mapid });

        if (everlast) {
            returnEverLastItem(chr);
        }

        removeMapObject(chr);
        broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));

        List<MapleSummon> toCancel = new ArrayList<MapleSummon>();
        final List<MapleSummon> ss = chr.getSummonsReadLock();
        try {
            for (final MapleSummon summon : ss) {
                broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
                removeMapObject(summon);
                if (summon.getMovementType() == SummonMovementType.STATIONARY || summon.getMovementType() == SummonMovementType.CIRCLE_STATIONARY || summon.getMovementType() == SummonMovementType.WALK_STATIONARY) {
                    toCancel.add(summon);
                } else {
                    summon.setChangedMap(true);
                }
            }
        } finally {
            chr.unlockSummonsReadLock();
        }
        for (MapleSummon summon : toCancel) {
            chr.removeSummon(summon);
            chr.dispelSkill(summon.getSkill()); //remove the buff
        }
        checkStates(chr.getName());
        if (mapid == 109020001) {
            chr.canTalk(true);
        }
        chr.leaveMap(this);
        recalcCanSpawnMobs();
    }

    public final void broadcastMessage(final byte[] packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getTruePosition());
    }

    /*	public void broadcastMessage(MapleCharacter source, byte[] packet, boolean repeatToSource, boolean ranged) {
     broadcastMessage(repeatToSource ? null : source, packet, ranged ? MapleCharacter.MAX_VIEW_RANGE_SQ : Double.POSITIVE_INFINITY, source.getPosition());
     }*/
    public final void broadcastMessage(final byte[] packet, final Point rangedFrom) {
        broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final Point rangedFrom) {
        broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public void broadcastMessage(final MapleCharacter source, final byte[] packet, final double rangeSq, final Point rangedFrom) {
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) _obj;
            if (chr != source) {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getTruePosition()) <= rangeSq) {
                        chr.getClient().getSession().write(packet);
                    }
                } else {
                    chr.getClient().getSession().write(packet);
                }
            }
        }
    }

    private void sendObjectPlacement(final MapleCharacter c) {
        if (c == null) {
            return;
        }
        for (final MapleMapObject o : getMapObjectsInRange(c.getTruePosition(), c.getRange(), GameConstants.rangedMapobjectTypes)) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (!((MapleReactor) o).isAlive()) {
                    continue;
                }
            }
            o.sendSpawnData(c.getClient());
            c.addVisibleMapObject(o);
        }
    }

    public final List<MaplePortal> getPortalsInRange(final Point from, final double rangeSq) {
        final List<MaplePortal> ret = new ArrayList<MaplePortal>();
        for (MaplePortal type : portals.values()) {
            if (from.distanceSq(type.getPosition()) <= rangeSq && type.getTargetMapId() != mapid && type.getTargetMapId() != 999999999) {
                ret.add(type);
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
            while (itr.hasNext()) {
                MapleMapObject mmo = itr.next();
                if (from.distanceSq(mmo.getTruePosition()) <= rangeSq) {
                    ret.add(mmo);
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Arrays.asList(MapleMapObjectType.ITEM));
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapObject_types) {
            Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
            while (itr.hasNext()) {
                MapleMapObject mmo = itr.next();
                if (from.distanceSq(mmo.getTruePosition()) <= rangeSq) {
                    ret.add(mmo);
                }
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRect(final Rectangle box, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapObject_types) {
            Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
            while (itr.hasNext()) {
                MapleMapObject mmo = itr.next();
                if (box.contains(mmo.getTruePosition())) {
                    ret.add(mmo);
                }
            }
        }
        return ret;
    }

    public final List<MapleCharacter> getCharactersIntersect(final Rectangle box) {
        final List<MapleCharacter> ret = new ArrayList<MapleCharacter>();
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) _obj;
            if (chr.getBounds().intersects(box)) {
                ret.add(chr);
            }
        }
        return ret;
    }

    public final List<MapleCharacter> getPlayersInRectAndInList(final Rectangle box, final List<MapleCharacter> chrList) {
        final List<MapleCharacter> character = new LinkedList<MapleCharacter>();
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter a = (MapleCharacter) _obj;
            if (chrList.contains(a) && box.contains(a.getTruePosition())) {
                character.add(a);
            }
        }
        return character;
    }

    public final void addPortal(final MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public final MaplePortal getPortal(final String portalname) {
        for (final MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public final MaplePortal getPortal(final int portalid) {
        return portals.get(portalid);
    }

    public final void resetPortals() {
        for (final MaplePortal port : portals.values()) {
            port.setPortalState(true);
        }
    }

    public final void setFootholds(final MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public final MapleFootholdTree getFootholds() {
        return footholds;
    }

    public final int getNumSpawnPoints() {
        return monsterSpawn.size();
    }

    public final void loadMonsterRate(final boolean first) {
        final int spawnSize = monsterSpawn.size();
        if (spawnSize >= 20 || partyBonusRate > 0) {
            maxRegularSpawn = Math.round(spawnSize / monsterRate);
        } else {
            maxRegularSpawn = (int) Math.ceil(spawnSize * monsterRate);
        }
        if (fixedMob > 0) {
            maxRegularSpawn = fixedMob;
        } else if (maxRegularSpawn <= 2) {
            maxRegularSpawn = 2;
        } else if (maxRegularSpawn > spawnSize) {
            maxRegularSpawn = Math.max(10, spawnSize);
        }

        Collection<Spawns> newSpawn = new LinkedList<Spawns>();
        Collection<Spawns> newBossSpawn = new LinkedList<Spawns>();
        for (final Spawns s : monsterSpawn) {
            if (s.getCarnivalTeam() >= 2) {
                continue; // Remove carnival spawned mobs
            }
            if (s.getMonster().isBoss()) {
                newBossSpawn.add(s);
            } else {
                newSpawn.add(s);
            }
        }
        monsterSpawn.clear();
        monsterSpawn.addAll(newBossSpawn);
        monsterSpawn.addAll(newSpawn);

        if (first && spawnSize > 0) {
            lastSpawnTime = System.currentTimeMillis();
            if (GameConstants.isForceRespawn(mapid)) {
                createMobInterval = 15000;
            }
            respawn(false); // this should do the trick, we don't need to wait upon entering map        
        }
    }

    public final void addAreaMonsterSpawn(final MapleMonster monster, Point pos1, Point pos2, Point pos3, final int mobTime, final String msg, final boolean shouldSpawn) {
        pos1 = calcPointBelow(pos1);
        pos2 = calcPointBelow(pos2);
        pos3 = calcPointBelow(pos3);
        if (pos1 != null) {
            pos1.y -= 1;
        }
        if (pos2 != null) {
            pos2.y -= 1;
        }
        if (pos3 != null) {
            pos3.y -= 1;
        }
        if (pos1 == null && pos2 == null && pos3 == null) {
            System.out.println("WARNING: mapid " + mapid + ", monster " + monster.getId() + " could not be spawned.");

            return;
        } else if (pos1 != null) {
            if (pos2 == null) {
                pos2 = new Point(pos1);
            }
            if (pos3 == null) {
                pos3 = new Point(pos1);
            }
        } else if (pos2 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos2);
            }
            if (pos3 == null) {
                pos3 = new Point(pos2);
            }
        } else if (pos3 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos3);
            }
            if (pos2 == null) {
                pos2 = new Point(pos3);
            }
        }
        monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg, shouldSpawn));
    }

    public final List<MapleCharacter> getCharacters() {
        return getCharactersThreadsafe();
    }

    public final List<MapleCharacter> getCharactersThreadsafe() {
        final List<MapleCharacter> chars = new ArrayList<MapleCharacter>();

        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter mc = (MapleCharacter) _obj;
            chars.add(mc);
        }
        return chars;
    }

    public final MapleCharacter getCharacterByName(final String id) {
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter mc = (MapleCharacter) _obj;
            if (mc.getName().equalsIgnoreCase(id)) {
                return mc;
            }
        }
        return null;
    }

    public final MapleCharacter getCharacterById_InMap(final int id) {
        return getCharacterById(id);
    }

    public final MapleCharacter getCharacterById(final int id) {
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter mc = (MapleCharacter) _obj;
            if (mc.getId() == id) {
                return mc;
            }
        }
        return null;
    }

    public final void updateMapObjectVisibility(final MapleCharacter chr, final MapleMapObject mo) {
        if (chr == null) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.MIST || mo.getType() == MapleMapObjectType.PLAYER || mo.getType() == MapleMapObjectType.SUMMON || mo instanceof MechDoor || mo.getTruePosition().distanceSq(chr.getTruePosition()) <= mo.getRange()) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else // monster left view range
        if (!(mo instanceof MechDoor) && mo.getType() != MapleMapObjectType.MIST && mo.getType() != MapleMapObjectType.SUMMON && mo.getType() != MapleMapObjectType.PLAYER && mo.getTruePosition().distanceSq(chr.getTruePosition()) > mo.getRange()) {
            chr.removeVisibleMapObject(mo);
            mo.sendDestroyData(chr.getClient());
        } //            else if (mo.getType() == MapleMapObjectType.MONSTER) { //monster didn't leave view range, and is visible        //                if (chr.getTruePosition().distanceSq(mo.getTruePosition()) <= GameConstants.maxViewRangeSq_Half()) {        //                    updateMonsterController((MapleMonster) mo);        //                }        //            }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter mc = (MapleCharacter) _obj;
            updateMapObjectVisibility(mc, monster);
        }
    }

    public void movePlayer(final MapleCharacter player, final Point newPosition) {
        player.setPosition(newPosition);
        if (newPosition.x == 0 || newPosition.y == 0) {
            return;
        }
        try {
            Collection<MapleMapObject> visibleObjects = player.getAndWriteLockVisibleMapObjects();
            ArrayList<MapleMapObject> copy = new ArrayList<MapleMapObject>(visibleObjects);
            Iterator<MapleMapObject> itr = copy.iterator();
            while (itr.hasNext()) {
                MapleMapObject mo = itr.next();
                if (mo != null && getMapObject(mo.getObjectId(), mo.getType()) == mo) {
                    updateMapObjectVisibility(player, mo);
                } else if (mo != null) {
                    visibleObjects.remove(mo);
                }
            }
            for (MapleMapObject mo : getMapObjectsInRange(player.getTruePosition(), player.getRange())) {
                if (mo != null && !visibleObjects.contains(mo)) {
                    mo.sendSpawnData(player.getClient());
                    visibleObjects.add(mo);
                }
            }
        } finally {
            player.unlockWriteVisibleMapObjects();
        }

    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public String spawnDebug() {
        StringBuilder sb = new StringBuilder("Mobs in map : ");
        sb.append(this.getMobsSize());
        sb.append(" spawnedMonstersOnMap: ");
        sb.append(spawnedMonstersOnMap);
        sb.append(" spawnpoints: ");
        sb.append(monsterSpawn.size());
        sb.append(" maxRegularSpawn: ");
        sb.append(maxRegularSpawn);
        sb.append(" actual monsters: ");
        sb.append(getNumMonsters());
        sb.append(" monster rate: ");
        sb.append(monsterRate);
//        sb.append(" fixed: ");
//        sb.append(fixedMob);

        double fix2 = (monsterRate * 1.3) / 1.5;
        double realMax = (maxspawns / fix2) * 2 > maxRegularSpawn ? Math.min(maxRegularSpawn * 1.5, maxspawns) : (maxspawns / fix2) * 1.8;
        int min = (int) (realMax / 2.5);

        sb.append(" min: ").append(min);
        sb.append(" max: ").append(realMax);
        sb.append(" fixedSpawns: ");
        sb.append(fixSpawns);
        sb.append(" plusMob: ").append(plusMob);
        sb.append(" curPlusMobSize: ").append(plusMobSize);
        sb.append(" plusMobSizePerSec(Plusing): ").append(plusMobLastOsize.get());

        return sb.toString();
    }

    public int characterSize() {
        return mapobjects.get(MapleMapObjectType.PLAYER).size();
    }

    public final int getMapObjectSize() {
        return mapobjects.size() + getCharactersSize() - mapobjects.get(MapleMapObjectType.PLAYER).size();
    }

    public final int getCharactersSize() {
        return mapobjects.get(MapleMapObjectType.PLAYER).size();
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    private class ActivateItemReactor implements Runnable {

        private MapleMapItem mapitem;
        private MapleReactor reactor;
        private MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId(), mapitem.getType()) && !mapitem.isPickedUp()) {
                mapitem.expire(MapleMap.this);
                if (c.getPlayer().getEventInstance() != null) {
                    EventInstanceManager eim = c.getPlayer().getEventInstance();
                    eim.setProperty("ActivatedReactorItem", mapitem.getItemId() + "");
                }
                reactor.hitReactor(c);
                reactor.setTimerActive(false);

                if (reactor.getDelay() > 0) {
                    MapTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            reactor.forceHitReactor((byte) 0);
                        }
                    }, reactor.getDelay());
                }
            } else {
                reactor.setTimerActive(false);
            }
        }
    }

    public void setMobGen(int mobid, boolean spawn) {
        Integer value = Integer.valueOf(mobid);
        if (spawn) {
            blockedMobGen.remove(value);
        } else {
            if (blockedMobGen.contains(value)) {
                return;
            }
            blockedMobGen.add(value);
        }
    }

    public final List<Integer> getBlockedMobs() {
        return Collections.unmodifiableList(blockedMobGen);
    }

    double maxspawns = 1;

    public void recalcCanSpawnMobs() {
        double min = (xy * monsterRate * 0.0000078125);
        if (min <= 1) {
            min = 1;
        }
        if (min >= 40) {
            min = 40;
        }
        double max = min * 1.6;
        maxspawns = max;
//        double fix = min + (getCharactersSize() * 1.3 * min);
//        maxspawns = Math.min(min * 1.3, fix);
//        System.out.println(maxspawns);
    }

    public double getCanSpawnMobs() {
        return maxspawns;
    }

    public void respawn(final boolean force) {
        respawn(force, System.currentTimeMillis());
    }

    public void respawn(final boolean force, final long now) {
        lastSpawnTime = now;
        if (force) { //cpq quick hack
            final int numShouldSpawn = monsterSpawn.size() - spawnedMonstersOnMap.get();
            if (numShouldSpawn > 0) {
                int spawned = 0;
                for (Spawns spawnPoint : monsterSpawn) {
                    if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                        continue;
                    }
                    spawnPoint.spawnMonster(this);
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        } else if (getId() / 10000000 == 98) {//카니발
            final List<Spawns> randomSpawn = new ArrayList<Spawns>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            int blue = 0, red = 0, maxblue = 0, maxred = 0;
            for (MapleMonster mob : getAllMonster()) {
                if (mob.getCarnivalTeam() == 0) {
                    blue++;
                } else {
                    red++;
                }
            }
            for (Spawns spawnMax : randomSpawn) {
                if (spawnMax.getCarnivalTeam() == 0) {
                    maxblue++;
                } else {
                    maxred++;
                }
            }
            for (Spawns spawnPoint : randomSpawn) {
                if (spawnPoint.getCarnivalTeam() == 0) {
                    if (blue >= maxblue * 1.2) {
                        continue;
                    }
                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                        continue;
                    }
                    if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                        continue;
                    }
                    blue++;
                    spawnPoint.spawnMonster(this);
                } else {
                    if (red >= maxred * 1.2) {
                        continue;
                    }
                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                        continue;
                    }
                    if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                        continue;
                    }
                    red++;
                    spawnPoint.spawnMonster(this);
                }
            }
        } else {
            if (getId() >= 190000000 && getId() <= 198000000) { //피시방맵 1.5배
                mobRate = 1.5;
            } else if (getId() == 930000200) { //엘린숲
                mobRate = 2;
            } else if (getId() >= 220060000 && getId() <= 220060400 || getId() >= 220070000 && getId() <= 220070400) { //시계탑 버프
                mobRate = 2;
            } else if (getId() >= 551020000 && getId() <= 551030100) { //판타지 테마파크 지역 버프
                mobRate = 1.5;
            } else if (getId() >= 240020000 && getId() <= 240020530) { //전장, 켄타우로스 지역 버프
                mobRate = 1.8;
            } else if (getId() >= 240070100 && getId() <= 240070102) { //네오시티 스케치북 퀘스트지역 버프
                mobRate = 2;
            } else if (getId() >= 271000000 && getId() <= 271030600) { //파괴된 헤네시스 및 에레브 지역 너프
                mobRate = 0.5;
            } else if (getId() >= 803000101 && getId() <= 803000104) { //크림슨우드 산맥 동굴 지역 버프
                mobRate = 1.8;
            } else if (getId() >= 803000300 && getId() <= 803000310) { //크림슨우드 산맥 협곡 지역 버프
                mobRate = 1.8;
            } else if (getId() >= 270000000 && getId() <= 270030500) { //시신전 지역 몹 버프
                mobRate = 2;
            } else if (getId() >= 802000200 && getId() <= 802000300) { //오다이바/공원 몹 버프
                mobRate = 2;
            } else if (getId() >= 802000400 && getId() <= 802000401) { //아키하바라 지역 몹 버프
                mobRate = 2;
            } else if (getId() == 803000400) { //크림슨우드 성채 입구 보스몹 2마리 나오게
                mobRate = 2;
            } else if (getId() >= 240040510 && getId() <= 240040511 || getId() >= 240040800 && getId() <= 240040821) { //남둥 죽둥 부기 젠상승
                mobRate = 1.5;
            } else if (getId() >= 800040400 && getId() <= 800040401 || getId() >= 800040200 && getId() <= 800040209) { //카에데 성 내부 젠 상승
                mobRate = 1.5;
            } else {
                mobRate = 1; //일반
            }
            if (mobRate == 1 && monsterSpawn.size() <= 13) {
                mobRate = 2;
            }

            if (mobRate == 1 && monsterRate > 1) {
                mobRate += (monsterRate - 1);
            }
            maxRegularSpawn = monsterSpawn.size();
            int CalculatedMobRate = (int) Math.round(maxRegularSpawn * mobRate);
            double numShouldSpawn = (GameConstants.isForceRespawn(mapid) ? monsterSpawn.size() : CalculatedMobRate) - spawnedMonstersOnMap.get();

            switch (getId()) {
                case 230040000: //깊협1
                case 230040100: //깊협2
                case 230040200: //위협1
                case 230040300: //위협2
                    numShouldSpawn = (int) Math.ceil(numShouldSpawn * 0.835);
                    break;
                case 240040520: //망둥
                case 240040521: //위둥
                case 240040511: //남둥
                case 240040510: //죽둥
                case 240040310: //레와 둥지
                case 240040210: //블와 둥지
                case 240040400: //와이번의 협곡
                    numShouldSpawn = (int) Math.ceil(numShouldSpawn * 1.666);
                    break;
                case 925100100://임시 데비존
                case 925100400://
                    numShouldSpawn = (int) Math.ceil(numShouldSpawn * 0.300 / 2 - spawnedMonstersOnMap.get());//15마리
                }

            if (numShouldSpawn > 0) {
                int spawned = 0;
                final List<Spawns> randomSpawn = new ArrayList<Spawns>(monsterSpawn);
                Collections.shuffle(randomSpawn);
                for (Spawns spawnPoint : randomSpawn) {
                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                        continue;
                    }
                    if (!blockedMobGen.isEmpty() && blockedMobGen.contains(Integer.valueOf(spawnPoint.getMonster().getId()))) {
                        continue;
                    }
                    if (spawnPoint.shouldSpawn(lastSpawnTime) || GameConstants.isForceRespawn(mapid) || (monsterSpawn.size() < 10 && maxRegularSpawn * mobRate > monsterSpawn.size() && partyBonusRate > 0)) {
                        spawnPoint.spawnMonster(this);
                        spawned++;
                    }
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    private static interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    public String getSnowballPortal() {
        int[] teamss = new int[2];
        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) _obj;
            if (chr.getTruePosition().y > -80) {
                teamss[0]++;
            } else {
                teamss[1]++;
            }
        }
        if (teamss[0] > teamss[1]) {
            return "st01";
        } else {
            return "st00";
        }
    }

    public boolean isDisconnected(int id) {
        return dced.contains(Integer.valueOf(id));
    }

    public void addDisconnected(int id) {
        dced.add(Integer.valueOf(id));
    }

    public void resetDisconnected() {
        dced.clear();
    }

    public void startSpeedRun() {
        final MapleSquad squad = getSquadByMap();
        if (squad != null) {
            for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
                MapleCharacter chr = (MapleCharacter) _obj;
                if (chr.getName().equals(squad.getLeaderName()) && !chr.isIntern()) {
                    startSpeedRun(chr.getName());
                    return;
                }
            }
        }
    }

    public void startSpeedRun(String leader) {
        speedRunStart = System.currentTimeMillis();
        speedRunLeader = leader;
    }

    public void endSpeedRun() {
        speedRunStart = 0;
        speedRunLeader = "";
    }

    public void getRankAndAdd(String leader, String time, ExpeditionType type, long timz, Collection<String> squad) {
        try {
            long lastTime = SpeedRunner.getSpeedRunData(type) == null ? 0 : SpeedRunner.getSpeedRunData(type).right;
            //if(timz > lastTime && lastTime > 0) {
            //return;
            //}
            //Pair<String, Map<Integer, String>>
            StringBuilder rett = new StringBuilder();
            if (squad != null) {
                for (String chr : squad) {
                    rett.append(chr);
                    rett.append(",");
                }
            }
            String z = rett.toString();
            if (squad != null) {
                z = z.substring(0, z.length() - 1);
            }
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("INSERT INTO speedruns(`type`, `leader`, `timestring`, `time`, `members`) VALUES (?,?,?,?,?)");
                ps.setString(1, type.name());
                ps.setString(2, leader);
                ps.setString(3, time);
                ps.setLong(4, timz);
                ps.setString(5, z);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
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

            if (lastTime == 0) { //great, we just add it
                SpeedRunner.addSpeedRunData(type, SpeedRunner.addSpeedRunData(new StringBuilder(SpeedRunner.getPreamble(type)), new HashMap<Integer, String>(), z, leader, 1, time), timz);
            } else {
                //i wish we had a way to get the rank
                //TODO revamp
                SpeedRunner.removeSpeedRunData(type);
                SpeedRunner.loadSpeedRunData(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getSpeedRunStart() {
        return speedRunStart;
    }

    public final void disconnectAll() {
        for (MapleCharacter chr : getCharactersThreadsafe()) {
            if (!chr.isGM()) {
                chr.getClient().sclose();
            }
        }
    }

    public List<MapleNPC> getAllNPCs() {
        return getAllNPCsThreadsafe();
    }

    public List<MapleNPC> getAllNPCsThreadsafe() {
        ArrayList<MapleNPC> ret = new ArrayList<MapleNPC>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.NPC).values()) {
            ret.add((MapleNPC) mmo);
        }
        return ret;
    }

    public final void resetNPCs() {
        removeNpc(-1);
    }

    public final void resetPQ(int level) {
        resetFully();
//        resetSpawnLevel(level);
    }

    public final void resetSpawnLevel(int level) {
        for (Spawns spawn : monsterSpawn) {
            if (spawn instanceof SpawnPoint) {
                ((SpawnPoint) spawn).setLevel(level);
            }
        }
    }

    public final void resetFully() {
        resetFully(true);
    }

    public final void resetFully(final boolean respawn) {
        killAllMonsters(false);
        reloadReactors();
        removeDrops();
        resetNPCs();
        resetSpawns();
        resetDisconnected();
        endSpeedRun();
        cancelSquadSchedule(true);
        resetPortals();
        seduceOrder.clear();
//        for (MapleMapObject ochr : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
//            ((MapleCharacter) ochr).getClient().getSession().close();
//        }
//        mapobjects.get(MapleMapObjectType.PLAYER).clear();
//        mapobjects.get(MapleMapObjectType.MONSTER).clear();
        changedMusic = "";
        environment.clear();
        onFirstUserEnterScriptRunned = false;
        outMapTime = 0;
        blockedMobGen.clear();
        if (respawn) {
            respawn(true);
        }
    }

    public final void cancelSquadSchedule(boolean interrupt) {
        squadTimer = false;
        checkStates = true;
        if (squadSchedule != null) {
            squadSchedule.cancel(interrupt);
            squadSchedule = null;
        }
    }

    public final void removeDrops() {
        List<MapleMapItem> items = this.getAllItemsThreadsafe();
        for (MapleMapItem i : items) {
            i.expire(this);
        }
    }

    public final void resetAllSpawnPoint(int mobid, int mobTime) {
        Collection<Spawns> sss = new LinkedList<Spawns>(monsterSpawn);
        resetFully();
        monsterSpawn.clear();
        for (Spawns s : sss) {
            MapleMonster newMons = MapleLifeFactory.getMonster(mobid);
            newMons.setF(s.getF());
            newMons.setFh(s.getFh());
            newMons.setPosition(s.getPosition());
            addMonsterSpawn(newMons, mobTime, (byte) -1, null);
        }
        loadMonsterRate(true);
    }

    public final void resetSpawns() {
        boolean changed = false;
        Iterator<Spawns> sss = monsterSpawn.iterator();
        while (sss.hasNext()) {
            if (sss.next().getCarnivalId() > -1) {
                sss.remove();
                changed = true;
            }
        }
        setSpawns(true);
        if (changed) {
            loadMonsterRate(true);
        }
    }

    public final boolean makeCarnivalSpawn(final int team, final MapleMonster newMons, final int num) {
        MonsterPoint ret = null;
        for (MonsterPoint mp : nodes.getMonsterPoints()) {
            if (mp.team == team || mp.team == -1) {
                final Point newpos = calcPointBelow(new Point(mp.x, mp.y));
                newpos.y -= 1;
                boolean found = false;
                for (Spawns s : monsterSpawn) {
                    if (s.getCarnivalId() > -1 && (mp.team == -1 || s.getCarnivalTeam() == mp.team) && s.getPosition().x == newpos.x && s.getPosition().y == newpos.y) {
                        found = true;
                        break; //this point has already been used.
                    }
                }
                if (!found) {
                    ret = mp; //this point is safe for use.
                    break;
                }
            }
        }
        if (ret != null) {
            newMons.setCy(ret.cy);
            newMons.setF(0); //always.
            newMons.setFh(ret.fh);
            newMons.setRx0(ret.x + 50);
            newMons.setRx1(ret.x - 50); //does this matter
            newMons.setPosition(new Point(ret.x, ret.y));
            newMons.setHide(false);
            final SpawnPoint sp = addMonsterSpawn(newMons, 1, (byte) team, null);
            sp.setCarnival(num);
        }
        return ret != null;
    }

    public final int makeCarnivalReactor(final int team, final int num) {
        final MapleReactor old = getReactorByName(team + "" + num);
        if (old != null) { //already exists
            return 1;
        }
        Point guardz = null;
        final List<MapleReactor> react = getAllReactorsThreadsafe();
        for (Pair<Point, Integer> guard : nodes.getGuardians()) {
            if (guard.right == team || guard.right == -1) {
                boolean found = false;
                for (MapleReactor r : react) {
                    if (r.getTruePosition().x == guard.left.x && r.getTruePosition().y == guard.left.y && r.getState() < 5) {
                        found = true;
                        break; //already used
                    }
                }
                if (!found) {
                    guardz = guard.left; //this point is safe for use.
                    break;
                }
            }
        }
        if (guardz != null) {
            final MapleReactor my = new MapleReactor(MapleReactorFactory.getReactor(9980000 + team), 9980000 + team);
            //my.setState((byte) 1);
            my.setName(team + "" + num); //lol
            //with num. -> guardians in factory
            spawnReactorOnGroundBelow(my, guardz);
            my.forceHitReactor((byte) 1);
            final MCSkill skil = MapleCarnivalFactory.getInstance().getGuardian(num);
            for (MapleMonster mons : getAllMonstersThreadsafe()) {
                if (mons.getCarnivalTeam() == team) {
                    skil.getSkill().applyEffect(null, mons, false, (short) 0);
                }
            }
            return 0;
        }
        return 2;
    }

    public final void blockAllPortal() {
        for (MaplePortal p : portals.values()) {
            p.setPortalState(false);
        }
    }

    public boolean getAndSwitchTeam() {
        return getCharactersSize() % 2 != 0;
    }

    public void setSquad(MapleSquadType s) {
        this.squad = s;

    }

    public int getChannel() {
        return channel;
    }

    public int getConsumeItemCoolTime() {
        return consumeItemCoolTime;
    }

    public void setConsumeItemCoolTime(int ciit) {
        this.consumeItemCoolTime = ciit;
    }

    public void setPermanentWeather(int pw) {
        this.permanentWeather = pw;
    }

    public int getPermanentWeather() {
        return permanentWeather;
    }

    public void checkStates(final String chr) {
        if (!checkStates) {
            return;
        }
        final MapleSquad sqd = getSquadByMap();
        final EventManager em = getEMByMap();
        final int size = getCharactersSize();
        if (sqd != null && sqd.getStatus() == 2) {
            sqd.removeMember(chr);
            if (em != null) {
                if (sqd.getLeaderName().equalsIgnoreCase(chr)) {
                    em.setProperty("leader", "false");
                }
                if (chr.equals("") || size == 0) {
                    em.setProperty("state", "0");
                    em.setProperty("leader", "true");
                    cancelSquadSchedule(!chr.equals(""));
                    sqd.clear();
                    sqd.copy();
                }
            }
        }
        if (em != null && em.getProperty("state") != null && (sqd == null || sqd.getStatus() == 2) && size == 0) {
            em.setProperty("state", "0");
            if (em.getProperty("leader") != null) {
                em.setProperty("leader", "true");
            }
        }
        if (speedRunStart > 0 && size == 0) {
            endSpeedRun();
        }
        //if (squad != null) {
        //    final MapleSquad sqdd = ChannelServer.getInstance(channel).getMapleSquad(squad);
        //    if (sqdd != null && chr != null && chr.length() > 0 && sqdd.getAllNextPlayer().contains(chr)) {
        //	sqdd.getAllNextPlayer().remove(chr);
        //	broadcastMessage(MaplePacketCreator.serverNotice(5, "The queued player " + chr + " has left the map."));
        //    }
        //}
    }

    public void setCheckStates(boolean b) {
        this.checkStates = b;
    }

    public void setNodes(final MapleNodes mn) {
        this.nodes = mn;
    }

    public final List<MaplePlatform> getPlatforms() {
        return nodes.getPlatforms();
    }

    public Collection<MapleNodeInfo> getNodes() {
        return nodes.getNodes();
    }

    public MapleNodeInfo getNode(final int index) {
        return nodes.getNode(index);
    }

    public boolean isLastNode(final int index) {
        return nodes.isLastNode(index);
    }

    public final List<Rectangle> getAreas() {
        return nodes.getAreas();
    }

    public final Rectangle getArea(final int index) {
        return nodes.getArea(index);
    }

    public final void changeEnvironment(final String ms, final int type) {
        broadcastMessage(MaplePacketCreator.environmentChange(ms, type));
    }

    public final void toggleEnvironment(final String ms) {
        if (environment.containsKey(ms)) {
            moveEnvironment(ms, environment.get(ms) == 1 ? 2 : 1);
        } else {
            moveEnvironment(ms, 1);
        }
    }

    public final void moveEnvironment(final String ms, final int type) {
        broadcastMessage(MaplePacketCreator.environmentMove(ms, type));
        environment.put(ms, type);
    }

    public final Map<String, Integer> getEnvironment() {
        return environment;
    }

    public final int getNumPlayersInArea(final int index) {
        return getNumPlayersInRect(getArea(index));
    }

    public final int getNumPlayersInRect(final Rectangle rect) {
        int ret = 0;

        for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            MapleCharacter chr = (MapleCharacter) _obj;
            if (rect.contains(chr.getTruePosition())) {
                ret++;
            }
        }
        return ret;
    }

    public final int getNumPlayersItemsInArea(final int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public final int getNumPlayersItemsInRect(final Rectangle rect) {
        int ret = getNumPlayersInRect(rect);

        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            if (rect.contains(mmo.getTruePosition())) {
                ret++;
            }
        }
        return ret;
    }

    public void broadcastGMMessage(MapleCharacter source, byte[] packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet);
    }

    private void broadcastGMMessage(MapleCharacter source, byte[] packet) {
        if (source == null) {
            for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
                MapleCharacter chr = (MapleCharacter) _obj;
                if (chr.isStaff()) {
                    chr.getClient().getSession().write(packet);
                }
            }
        } else {
            for (MapleMapObject _obj : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
                MapleCharacter chr = (MapleCharacter) _obj;
                if (chr != source && (chr.getGMLevel() >= source.getGMLevel())) {
                    chr.getClient().getSession().write(packet);
                }
            }
        }
    }

    public final List<Pair<Integer, Integer>> getMobsToSpawn() {
        return nodes.getMobsToSpawn();
    }

    public final List<Integer> getSkillIds() {
        return nodes.getSkillIds();
    }

    public void monsterKilled() {
        plusMobLastOsize.incrementAndGet();
    }

    public final boolean canSpawn(long now) {
        plusMob += plusMobLastOsize.get();
        plusMobLastOsize.set(0);
        if (plusMobLastTime + 60000L < System.currentTimeMillis()) {
            if (plusMobLastTime > 0) {
                plusMobSize = plusMob / 12.5;
                plusMobSize *= 1.65;
                plusMob = 0;
            }
            plusMobLastTime = System.currentTimeMillis();
        }
        return lastSpawnTime > 0 && lastSpawnTime + createMobInterval < now;
    }

    public final boolean canHurt(long now) {
        if (lastHurtTime > 0 && lastHurtTime + decHPInterval < now) {
            lastHurtTime = now;
            return true;
        }
        return false;
    }

    public final boolean togglePetPick() {
        canPetPickup = !canPetPickup;
        return canPetPickup;
    }

    public final boolean canPetPick() {
        return canPetPickup;
    }

    public final void resetShammos(final MapleClient c) {
        killAllMonsters(true);
        broadcastMessage(MaplePacketCreator.serverNotice(5, "A player has moved too far from Shammos. Shammos is going back to the start."));
        EtcTimer.getInstance().schedule(new Runnable() {
            public void run() {
                if (c.getPlayer() != null) {
                    c.getPlayer().changeMap(MapleMap.this, getPortal(0));
                    if (getCharactersThreadsafe().size() > 1) {
                        MapScriptMethods.startScript_FirstUser(c, "shammos_Fenter");
                    }
                }
            }
        }, 500); //avoid dl
    }

    public int getInstanceId() {
        return instanceid;
    }

    public void setInstanceId(int ii) {
        this.instanceid = ii;
    }

    public int getPartyBonusRate() {
        return partyBonusRate;
    }

    public void setPartyBonusRate(int ii) {
        this.partyBonusRate = ii;
    }

    public short getTop() {
        return top;
    }

    public short getBottom() {
        return bottom;
    }

    public short getLeft() {
        return left;
    }

    public short getRight() {
        return right;
    }

    public void setTop(int ii) {
        this.top = (short) ii;
    }

    public void setBottom(int ii) {
        this.bottom = (short) ii;
    }

    public void setLeft(int ii) {
        this.left = (short) ii;
    }

    public void setRight(int ii) {
        this.right = (short) ii;
    }

    public List<Pair<Point, Integer>> getGuardians() {
        return nodes.getGuardians();
    }

    public void changeMusic(String newa) {
        changedMusic = newa;
    }

    public void setCommandTimer(long newtime) {
        setCommandTimer = newtime;
    }

    public DirectionInfo getDirectionInfo(int i) {
        return nodes.getDirection(i);
    }

    public void setOutMapTime(long l) {
        outMapTime = l;
    }

    public long getOutMapTime() {
        return outMapTime;
    }

    public void clearEffect() {
        broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear"));
        broadcastMessage(MaplePacketCreator.playSound("Party1/Clear"));
        broadcastMessage(MaplePacketCreator.environmentChange("gate", 2));
    }

    public void failEffect() {
        broadcastMessage(MaplePacketCreator.showEffect("quest/party/wrong_kor"));
        broadcastMessage(MaplePacketCreator.playSound("Party1/Failed"));
    }

    public final void destroyCarnivalReactor(final int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        if (reactor == null) {
            return;
        }
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);
    }

    public final List<MaplePortal> getPortalSP() {
        List res = new LinkedList();
        for (MaplePortal port : this.portals.values()) {
            if (port.getName().equals("sp")) {
                res.add(port);
            }
        }
        return res;
    }
}
