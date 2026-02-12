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
package client;

import client.anticheat.CheatTracker;
import client.anticheat.ReportType;
import client.inventory.*;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import database.DatabaseException;
import handling.channel.ChannelServer;
import handling.channel.handler.DueyHandler;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import scripting.EventInstanceManager;
import scripting.NPCScriptManager;
import server.*;
import server.Timer;
import server.Timer.MapTimer;
import server.life.*;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.*;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageEventAgent;
import server.marriage.MarriageManager;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.IMaplePlayerShop;
import server.shops.MapleMiniGame;
import tools.*;
import tools.MaplePacketCreator.GainExpPacket;
import tools.packet.*;

import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;
import server.Timer.EventTimer;
import server.events.OnTimeGiver;
import tools.packet.FamilyPacket;
import java.util.concurrent.atomic.AtomicLong;

public class MapleCharacter extends AnimatedMapleMapObject implements Serializable {

    private static final long serialVersionUID = 845748950829L;
    private String name, chalktext, teleportname, BlessOfFairy_Origin, BlessOfEmpress_Origin;
    private long lastCombo, lastfametime, keydown_skill, nextConsume, pqStartTime, lastDragonBloodTime, lastBeholderHealTime, lastBeholderBuffTime, lastBattleTime,
            lastBerserkTime, lastRecoveryTime, lastSummonTime, mapChangeTime,
            lastHPTime, lastMPTime, lastDOTTime, firstLoginTime, lastPotentialRecoveryTime;
    private byte gmLevel, gender, initialSpawnPoint, skinColor, guildrank = 5, allianceRank = 5,
            world, subcategory, SpawnPoint, mobKilledNo, hoursFromLogin;
    private short level, mulung_energy, combo, availableCP, totalCP, hpApUsed, job, remainingAp, scrolledPosition;
    private int accountid, id, meso, exp, hair, face, mapid, fame,
            guildid = 0, fallcounter, maplepoints, acash, chair, itemEffect, points, vpoints,
            rank = 1, rankMove = 0, jobRank = 1, jobRankMove = 0, marriageId, engageId, marriageItemId, dotHP, fh, extraDamage,
            finalcut, battleshipHP, coconutteam, currentrep, totalrep, challenge, donatecash, bookCover, followid, weddingGiftGive, guildContribution = 0;
    private Point old;
    private int[] wishlist, rocks, savedLocations, regrocks, hyperrocks, remainingSp = new int[10];
    private transient AtomicInteger inst, insd;
    private transient List<LifeMovementFragment> lastres;
    private List<Integer> lastmonthfameids, lastmonthbattleids;
    private List<MapleDoor> doors;
    private List<MechDoor> mechDoors;
    private List<MaplePet> pets;
    private MaplePet[] petz = new MaplePet[3];
    private transient Set<MapleMonster> controlled;
    private transient Set<MapleMapObject> visibleMapObjects;
    private transient ReentrantReadWriteLock visibleMapObjectsLock;
    private transient ReentrantReadWriteLock summonsLock;
    private transient ReentrantReadWriteLock controlledLock;
    private Map<MapleQuest, MapleQuestStatus> quests;
    private transient Map<Integer, Integer> linkMobs;
    private Map<Integer, String> questinfo;
    private Map<Skill, SkillEntry> skills;
    private transient Map<MapleBuffStat, MapleBuffStatValueHolder> effects;
    private transient List<MapleSummon> summons;
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns;
    private transient Map<MapleDisease, MapleDiseaseValueHolder> diseases;
    private Map<ReportType, Integer> reports;
    private CashShop cs;
    private transient Deque<MapleCarnivalChallenge> pendingCarnivalRequests;
    private transient MapleCarnivalParty carnivalParty;
    private BuddyList buddylist;
    private transient CheatTracker anticheat;
    private MapleClient client;
    private transient MapleParty party;
    private PlayerStats stats;
    private transient MapleMap map;
    private transient MapleShop shop;
    private transient MapleDragon dragon;
    private transient RockPaperScissors rps;
    private MonsterBook monsterbook;
    private MapleStorage storage;
    private transient boolean usingStrongBuff = false;
    private transient MapleTrade trade;
    private MapleMount mount;
    private List<Integer> finishedAchievements;
    private MapleMessenger messenger;
    private byte[] petStore;
    private transient IMaplePlayerShop playerShop;
    private boolean invincible, canTalk, smega, hasSummon, followinitiator, followon;
    private MapleGuildCharacter mgc;
    private MapleFamilyCharacter mfc;
    private transient EventInstanceManager eventInstance;
    private MapleInventory[] inventory;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    //private EnumMap<MapleTraitType, MapleTrait> traits;
    private MapleKeyLayout keylayout;
    private transient ScheduledFuture<?> mapTimeLimitTask;
    private transient List<Integer> pendingExpiration = null, pendingSkills = null;
    private boolean changed_wishlist, changed_trocklocations, changed_regrocklocations, changed_hyperrocklocations, changed_skillmacros, changed_achievements,
            changed_savedlocations, changed_questinfo, changed_skills, changed_reports;
    private boolean goDonateCashShop = false;
    private boolean searchingParty = false;
    private List<Integer> psearch_jobs;
    private int psearch_maxLevel;
    private int psearch_minLevel;
    private int psearch_membersNeeded;
    private int canGainNoteFame = 0;
    public CRand32 rndGenForCharacter;
    public CRand32 rndForCheckDamageMiss;
    public CRand32 rndGenForMob;
    public CalcDamage calcDamage;
    private int bonusExpR = 0;
    private boolean updateAccepted = false;
    private int possibleReports = 12;
    public EnumMap<MapleBuffStat, Integer> holySymbol;
    public int LastSkill, swallowedMobId = 0;
    private transient Event_PyramidSubway pyramidSubway = null;
    private int hairCoupon = 123456;
    public int cubeitemid = -1;

    private volatile boolean oneClickCubeLoaded = false;
    private volatile boolean oneClickCubeEnabled = false;
    private final int[] oneClickCubeDesired = new int[]{0, 0, 0};

    private volatile boolean oneClickCubeDupMajorEnabled = false;

    private volatile boolean pendingOneClickCube = false;
    private volatile int pendingOneClickCubeItemId = 0;
    private volatile String pendingOneClickCubeReason = null;

    private volatile byte pendingOneClickCubeSlot = -1;

    public volatile boolean oneClickCubeIgnoreBreakOnce = false;

    // 자석 온오프, 템제한 클래스 추가
    private volatile boolean magnetLoaded = false;
    private volatile boolean magnetEnabled = false; // DB: banitemid=0 row
    private final java.util.Set<Integer> magnetBanItems = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>());

    private transient Equip ReturnScrollBefore;
    private transient Equip ReturnScrollAfter;
    private transient MapleInventoryType ReturnScrollInvType;
    private transient short ReturnScrollSlot;

    public void setReturnScrollData(Equip before, Equip after, MapleInventoryType invType, short slot) { // 리턴 스크롤
        this.ReturnScrollBefore = before;
        this.ReturnScrollAfter = after;
        this.ReturnScrollInvType = invType;
        this.ReturnScrollSlot = slot;
    }

    public Equip getReturnScrollBefore() {
        return ReturnScrollBefore;
    }

    public Equip getReturnScrollAfter() {
        return ReturnScrollAfter;
    }

    public MapleInventoryType getReturnScrollInvType() {
        return ReturnScrollInvType;
    }

    public short getReturnScrollSlot() {
        return ReturnScrollSlot;
    }

    public void clearReturnScrollData() {
        this.ReturnScrollBefore = null;
        this.ReturnScrollAfter = null;
        this.ReturnScrollInvType = null;
        this.ReturnScrollSlot = 0;
    }

    public void applyReturnScroll(boolean keepNew) { //리턴 스크롤
        try {
            if (ReturnScrollBefore == null || ReturnScrollAfter == null || ReturnScrollInvType == null) {
                clearReturnScrollData();
                return;
            }
            if (keepNew) {
                clearReturnScrollData();
                return;
            }

            MapleInventory inv = getInventory(ReturnScrollInvType);
            if (inv == null) {
                clearReturnScrollData();
                return;
            }

            inv.removeSlot(ReturnScrollSlot);

            Equip restore = (Equip) ReturnScrollBefore.copy();
            restore.setPosition(ReturnScrollSlot);
            inv.addFromDB(restore);

            // 인벤토리 갱신 패킷
            if (getClient() != null) {
                final byte packetInvType = MapleInventoryType.EQUIP.getType(); // ★ 무조건 EQUIP(1)

                getClient().getSession().write(
                        MaplePacketCreator.updateSpecialItemUse(
                                restore,
                                packetInvType,
                                this
                        )
                );

                getClient().getSession().write(MaplePacketCreator.getInventoryFull());
                getClient().getSession().write(MaplePacketCreator.enableActions());
            }

            if (ReturnScrollInvType == MapleInventoryType.EQUIPPED) {
                equipChanged();
            }

        } finally {
            clearReturnScrollData();
        }
    }

    private final AtomicLong lastMagnetVacuumTime = new AtomicLong(0L);

    public boolean shouldBreakOneClickCube(final client.inventory.Equip eq) {
        ensureOneClickCubeLoaded();
        if (!oneClickCubeEnabled) {
            return false;
        }
        if (eq == null) {
            return false;
        }

        // 희망 슬롯들(중복 포함)을 리스트로 구성
        final java.util.List<Integer> desired = new java.util.ArrayList<Integer>(3);
        for (int i = 0; i < 3; i++) {
            if (oneClickCubeDesired[i] != 0) {
                desired.add(oneClickCubeDesired[i]);
            }
        }
        if (desired.isEmpty()) {
            return false;
        }

        final int[] cur = new int[]{eq.getPotential1(), eq.getPotential2(), eq.getPotential3()};

        // k<=3 이라 완전탐색(줄 배정)으로 “중복 슬롯”까지 정확히 처리
        final int k = desired.size();
        final int[] idx = new int[]{0, 1, 2};

        // 순열 3! = 6만 보면 됨
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                if (b == a) {
                    continue;
                }
                for (int c = 0; c < 3; c++) {
                    if (c == a || c == b) {
                        continue;
                    }

                    final int[] pick = new int[]{idx[a], idx[b], idx[c]};
                    boolean ok = true;

                    for (int j = 0; j < k; j++) {
                        final int optionKey = desired.get(j);
                        final int potId = cur[pick[j]];
                        if (!constants.GameConstants.oneClickCubeOptionMatches(optionKey, potId)) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        return true; // 브레이크 조건 충족
                    }
                }
            }
        }
        return false;
    }

    // (추가) 원클릭큐브 중단 사유를 문자열로 리턴 (없으면 null)
    public String getOneClickCubeBreakReason(final client.inventory.Equip eq, final int oneClickItemId) {
        ensureOneClickCubeLoaded();
        if (eq == null) {
            return null;
        }

        // 1) 희망 잠재 감지
        if (oneClickCubeEnabled && shouldBreakOneClickCube(eq)) {
            return "희망 잠재능력이 감지되었습니다.";
        }

        // 2) 중복 주요 옵션 감지(ON일 때)
        if (isOneClickCubeDupMajorEnabled()) {
            final String dup = getDupMajorDetected(eq);
            if (dup != null) {
                return "중복 주요 옵션이 감지되었습니다: " + dup;
            }
        }

        return null;
    }

    // (추가) ON 상태에서 주요 옵션군이 2줄 이상이면 감지
    private String getDupMajorDetected(final client.inventory.Equip eq) {
        final int p1 = eq.getPotential1();
        final int p2 = eq.getPotential2();
        final int p3 = eq.getPotential3();

        int str = 0, dex = 0, intl = 0, luk = 0, atk = 0, matk = 0, ied = 0;

        final int[] pots = new int[]{p1, p2, p3};
        for (int p : pots) {
            if (p == 0) {
                continue;
            }

            // 힘%: 10041/20041/30041
            if (p == 10041 || p == 20041 || p == 30041) {
                str++;
            } // 덱스%: 10042/20042/30042
            else if (p == 10042 || p == 20042 || p == 30042) {
                dex++;
            } // 인트%: 10043/20043/30043
            else if (p == 10043 || p == 20043 || p == 30043) {
                intl++;
            } // 럭%: 10044/20044/30044
            else if (p == 10044 || p == 20044 || p == 30044) {
                luk++;
            } // 공격력%: 20051/30051
            else if (p == 20051 || p == 30051) {
                atk++;
            } // 마력%: 20052/30052
            else if (p == 20052 || p == 30052) {
                matk++;
            } // 방무%: 10291/20291/30291
            else if (p == 10291 || p == 20291 || p == 30291) {
                ied++;
            }
        }

        final java.util.List<String> hit = new java.util.ArrayList<String>();
        if (str >= 2) {
            hit.add("힘%");
        }
        if (dex >= 2) {
            hit.add("덱스%");
        }
        if (intl >= 2) {
            hit.add("인트%");
        }
        if (luk >= 2) {
            hit.add("럭%");
        }
        if (atk >= 2) {
            hit.add("공격력%");
        }
        if (matk >= 2) {
            hit.add("마력%");
        }
        if (ied >= 2) {
            hit.add("방무%");
        }

        if (hit.isEmpty()) {
            return null;
        }
        return String.join(", ", hit);
    }

    private MapleCharacter(final boolean ChannelServer) {
        setStance(0);
        setPosition(new Point(0, 0));

        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        quests = new LinkedHashMap<MapleQuest, MapleQuestStatus>(); // Stupid erev quest.
        skills = new LinkedHashMap<Skill, SkillEntry>(); //Stupid UAs.
        stats = new PlayerStats();
        for (int i = 0; i < remainingSp.length; i++) {
            remainingSp[i] = 0;
        }
//        traits = new EnumMap<MapleTraitType, MapleTrait>(MapleTraitType.class);
//        for (MapleTraitType t : MapleTraitType.values()) {
//            traits.put(t, new MapleTrait(t));
//        }
        if (ChannelServer) {
            changed_reports = false;
            changed_skills = false;
            changed_achievements = false;
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_questinfo = false;
            scrolledPosition = 0;
            lastCombo = 0;
            mulung_energy = 0;
            combo = 0;
            keydown_skill = 0;
            nextConsume = 0;
            pqStartTime = 0;
            mapChangeTime = 0;
            lastRecoveryTime = 0;
            lastPotentialRecoveryTime = 0;
            battleshipHP = 0;
            lastDragonBloodTime = 0;
            lastBeholderBuffTime = 0;
            lastBeholderHealTime = 0;
            lastBerserkTime = 0;
            lastHPTime = 0;
            lastMPTime = 0;
            old = new Point(0, 0);
            coconutteam = 0;
            followid = 0;
            marriageItemId = 0;
            fallcounter = 0;
            challenge = 0;
            dotHP = 0;
            engageId = 0;
            marriageId = 0;
            lastSummonTime = 0;
            hasSummon = false;
            invincible = false;
            canTalk = true;
            followinitiator = false;
            followon = false;
            searchingParty = false;
            finishedAchievements = new ArrayList<Integer>();
            reports = new EnumMap<ReportType, Integer>(ReportType.class);
            teleportname = "";
            smega = true;
            petStore = new byte[3];
            for (int i = 0; i < petStore.length; i++) {
                petStore[i] = (byte) -1;
            }
            wishlist = new int[10];
            rocks = new int[10];
            regrocks = new int[5];
            hyperrocks = new int[13];
            effects = new ConcurrentEnumMap<MapleBuffStat, MapleBuffStatValueHolder>(MapleBuffStat.class);
            coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>();
            diseases = new ConcurrentEnumMap<MapleDisease, MapleDiseaseValueHolder>(MapleDisease.class);
            inst = new AtomicInteger(0);// 1 = NPC/ Quest, 2 = Duey, 3 = Hired Merch store, 4 = Storage
            insd = new AtomicInteger(-1);
            keylayout = new MapleKeyLayout();
            doors = new ArrayList<MapleDoor>();
            mechDoors = new ArrayList<MechDoor>();
            linkMobs = new HashMap<Integer, Integer>();
            controlled = new LinkedHashSet<MapleMonster>();
            controlledLock = new ReentrantReadWriteLock();
            summons = new LinkedList<MapleSummon>();
            summonsLock = new ReentrantReadWriteLock();
            visibleMapObjects = new LinkedHashSet<MapleMapObject>();
            visibleMapObjectsLock = new ReentrantReadWriteLock();
            pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();

            savedLocations = new int[SavedLocationType.values().length];
            for (int i = 0; i < SavedLocationType.values().length; i++) {
                savedLocations[i] = -1;
            }
            questinfo = new LinkedHashMap<Integer, String>();
            pets = new ArrayList<MaplePet>();
        }
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (MapleCharacter char2 : getMap().getCharacters()) {
            if (char2.getParty() == getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    public void gainItemPotential(int code, short quantity) {
        MapleInventoryManipulator.addByIdPotential(client, code, (short) quantity, "", null, "", true);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ServerLogger.getInstance().logItem(LogType.Item.FromScript, getId(), getName(), code, quantity, ii.getName(code), 0, "");
    }

    public void gainItem(int code, short quantity) {
        MapleInventoryManipulator.addById(client, code, (short) quantity, "");
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ServerLogger.getInstance().logItem(LogType.Item.FromScript, getId(), getName(), code, quantity, ii.getName(code), 0, "");
    }

    public void gainItem(int code, short quantity, boolean show) {
        MapleInventoryManipulator.addById(client, code, (short) quantity, "");
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ServerLogger.getInstance().logItem(LogType.Item.FromScript, getId(), getName(), code, quantity, ii.getName(code), 0, "");
        if (show) {
            getClient().getSession().write(MaplePacketCreator.getShowItemGain(code, quantity, true));
        }
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner, final MapleClient cg) {
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(id);

            if (!MapleInventoryManipulator.checkSpace(cg, id, quantity, "")) {
                return;
            }
            if (type.equals(MapleInventoryType.EQUIP) && !GameConstants.isThrowingStar(id) && !GameConstants.isBullet(id)) {
                final Equip item = (Equip) (randomStats ? ii.randomizeStats((Equip) ii.getEquipById(id)) : ii.getEquipById(id));
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (slots > 0) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() + slots));
                }
                if (owner != null) {
                    item.setOwner(owner);
                }
                item.setGMLog("Received from interaction " + this.id + " on " + FileoutputUtil.CurrentReadable_Time());
                final String name = ii.getName(id);
                if (id / 10000 == 114 && name != null && name.length() > 0) { //medal
                    final String msg = "<" + name + "> 칭호를 얻었습니다.";
                    cg.getPlayer().dropMessage(-1, msg);
                    cg.getPlayer().dropMessage(5, msg);
                }
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, null, period, "Received from interaction " + this.id + " on " + FileoutputUtil.CurrentReadable_Date());
            }
        } else {
            MapleInventoryManipulator.removeById(cg, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        cg.getSession().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    private transient ScheduledFuture<?> timemove;

    public void timeMove() {
        if (timemove != null) {
            timemove.cancel(true);
        }
        timemove = null;
    }

    public final void warp(int map) {
        MapleMap mapz = getWarpMap(map);
        changeMap(mapz, (MaplePortal) mapz.getPortalSP().get(Randomizer.nextInt(mapz.getPortalSP().size())));
    }

    private final MapleMap getWarpMap(int map) {
        if (getEventInstance() != null) {
            return getEventInstance().getMapFactory().getMap(map);
        }
        return ChannelServer.getInstance(this.client.getChannel()).getMapFactory().getMap(map);
    }

    public final void timeMoveMap(final int destination, final int movemap, final int time) {
        warp(movemap);
        getClient().getSession().write(MaplePacketCreator.getClock(time));
        timemove = Timer.EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (this != null) {
                    if (getMapId() == movemap) {
                        warp(destination);
                    }
                }
            }
        }, time * 1000);
    }

    public final boolean canHold(final int itemid, final int quantity) {
        return MapleInventoryManipulator.checkSpace(getClient(), itemid, quantity, "");
    }

    public static MapleCharacter getDefault(final MapleClient client, final JobType type) {
        MapleCharacter ret = new MapleCharacter(false);
        ret.client = client;
        ret.map = null;
        ret.exp = 0;
        ret.gmLevel = 0;
        ret.job = (short) type.id;
        ret.meso = 0;
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList((byte) 20);

        ret.stats.str = 12;
        ret.stats.dex = 5;
        ret.stats.int_ = 4;
        ret.stats.luk = 4;
        ret.stats.maxhp = 50;
        ret.stats.hp = 50;
        ret.stats.maxmp = 5;
        ret.stats.mp = 5;
        String getDailyQuestBonusStatus = ret.getDailyQuestBonus();
        if (getDailyQuestBonusStatus == null) {
            ret.setDailyQuestBonus(0, 0);
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.acash = rs.getInt("ACash");
                ret.maplepoints = rs.getInt("mPoints");
                ret.points = rs.getInt("points");
                ret.vpoints = rs.getInt("vpoints");
            }
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
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
        return ret;
    }

    private static final void respawnPlayer(final MapleCharacter mc) {
        if (mc.getMap() == null) {
            return;
        }
        mc.getMap().broadcastMessage(MaplePacketCreator.loadGuildName(mc));
        mc.getMap().broadcastMessage(MaplePacketCreator.loadGuildIcon(mc));
    }

    public void addGuildMember(int gid) {
        if (getGuildId() > 0) {
            return; // 있으면 사라져
        } else {
            int guildId = gid; // 외부에서 받아온 길드 아이디 인자를 내부 변수 guildId에 넣음
            int cid = getId();

            if (cid != getId()) {
                return;
            } else {
                setGuildId(guildId); // gulidId에 지정된 길드아이디로 플레이어를 가입시킴
                setGuildRank((byte) 5);
                int s = World.Guild.addGuildMember(getMGC());
                if (s == 0) {
                    dropMessage(1, "가입하려는 길드는 이미 최대 인원으로 가득 찼습니다."); // 사라져..
                    setGuildId(0);
                    return; // ㅂㅂ
                }
                getClient().getSession().write(MaplePacketCreator.showGuildInfo(this));
                final MapleGuild gs = World.Guild.getGuild(guildId);
                for (byte[] pack : World.Alliance.getAllianceInfo(gs.getAllianceId(), true)) {
                    if (pack != null) {
                        getClient().getSession().write(pack);
                    }
                }
                saveGuildStatus();
                respawnPlayer(this);
            }
        }
    }

    public final static MapleCharacter ReconstructChr(final CharacterTransfer ct, final MapleClient client, final boolean isChannel) {
        final MapleCharacter ret = new MapleCharacter(true); // Always true, it's change channel
        ret.client = client;
        if (!isChannel) {
            ret.client.setChannel(ct.channel);
        }
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.fame = ct.fame;

        ret.stats.str = ct.str;
        ret.stats.dex = ct.dex;
        ret.stats.int_ = ct.int_;
        ret.stats.luk = ct.luk;
        ret.stats.maxhp = ct.maxhp;
        ret.stats.maxmp = ct.maxmp;
        ret.stats.hp = ct.hp;
        ret.stats.mp = ct.mp;
        //System.out.println("ret.stats.hp" + ret.stats.hp);
        //System.out.println("ct.hp" + ct.hp);

        ret.chalktext = ct.chalkboard;
        ret.gmLevel = ct.gmLevel;
        ret.hide = ret.isGM();
        ret.exp = (ret.level == 200 || (GameConstants.isKOC(ret.job) && ret.level >= 120)) && !ret.isIntern() ? 0 : ct.exp;
        ret.hpApUsed = ct.hpApUsed;
        ret.remainingSp = ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso = ct.meso;
        ret.skinColor = ct.skinColor;
        ret.gender = ct.gender;
        ret.job = ct.job;
        ret.hair = ct.hair;
        ret.face = ct.face;
        ret.accountid = ct.accountid;
        client.setAccID(ct.accountid);
        ret.mapid = ct.mapid;

        //PC방 처리
        ret.pctime = ct.pctime;
        ret.pcdate = ct.pcdate;

        ret.SpawnPoint = ct.SpawnPoint;
        ret.fh = ct.fh;
        ret.world = ct.world;
        ret.guildid = ct.guildid;
        ret.guildrank = ct.guildrank;
        ret.guildContribution = ct.guildContribution;
        ret.allianceRank = ct.alliancerank;
        ret.points = ct.points;
        ret.vpoints = ct.vpoints;
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        ret.buddylist = new BuddyList(ct.buddysize);
        ret.subcategory = ct.subcategory;
        ret.goDonateCashShop = ct.donateshop > 0;
        ret.firstLoginTime = ct.firstLoginTime;
        ret.makeMFC(ct.familyid, ct.seniorid, ct.junior1, ct.junior2);
        ret.totalrep = ct.totalrep;
        ret.currentrep = ct.currentrep;
        ret.marriageId = ct.marriageId;
        ret.engageId = ct.engageId;
        ret.itemEffect = ct.itemEffect;

        if (isChannel) {
            final MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                ret.map = mapFactory.getMap(100000000);
            } else {
                if (ret.map.getForcedReturnId() != 999999999 && ret.map.getForcedReturnMap() != null) {
                    ret.map = ret.map.getForcedReturnMap();
                } else if (ret.mapid >= 925010000 && ret.mapid <= 925010300) {
                    ret.map = mapFactory.getMap(120000104);
                }
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());

            final int messengerid = ct.messengerid;
            if (messengerid > 0) {
                ret.messenger = World.Messenger.getMessenger(messengerid);
            }
        } else {

            ret.messenger = null;
        }
        int partyid = ct.partyid;
        if (partyid >= 0) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null && party.getMemberById(ret.id) != null) {
                ret.party = party;
            }
        }

        MapleQuestStatus queststatus_from;
        for (final Map.Entry<Integer, Object> qs : ct.Quest.entrySet()) {
            queststatus_from = (MapleQuestStatus) qs.getValue();
            queststatus_from.setQuest(qs.getKey());
            ret.quests.put(queststatus_from.getQuest(), queststatus_from);
        }
        for (final Map.Entry<Integer, SkillEntry> qs : ct.Skills.entrySet()) {
            ret.skills.put(SkillFactory.getSkill(qs.getKey()), qs.getValue());
        }
        for (final Integer zz : ct.finishedAchievements) {
            ret.finishedAchievements.add(zz);
        }
//        for (Entry<MapleTraitType, Integer> t : ct.traits.entrySet()) {
//            ret.traits.get(t.getKey()).setExp(t.getValue());
//        }
        for (final Map.Entry<Byte, Integer> qs : ct.reports.entrySet()) {
            ret.reports.put(ReportType.getById(qs.getKey()), qs.getValue());
        }
        ret.monsterbook = (MonsterBook) ct.monsterbook;
        ret.bookCover = ct.mbookcover;
        ret.inventory = (MapleInventory[]) ct.inventorys;
        ret.BlessOfFairy_Origin = ct.BlessOfFairy;
        ret.BlessOfEmpress_Origin = ct.BlessOfEmpress;
        ret.skillMacros = (SkillMacro[]) ct.skillmacro;
        //ret.petz = ct.petz;
        ret.petStore = ct.petStore;
        ret.keylayout = new MapleKeyLayout(ct.keymap);
        ret.questinfo = ct.InfoQuest;
        ret.savedLocations = ct.savedlocation;
        ret.wishlist = ct.wishlist;
        ret.rocks = ct.rocks;
        ret.regrocks = ct.regrocks;
        ret.hyperrocks = ct.hyperrocks;
        ret.buddylist.loadFromTransfer(ct.buddies);
        // ret.lastfametime
        // ret.lastmonthfameids
        ret.keydown_skill = 0; // Keydown skill can't be brought over
        ret.lastfametime = ct.lastfametime;
        ret.lastmonthfameids = ct.famedcharacters;
        ret.lastmonthbattleids = ct.battledaccs;
        ret.storage = (MapleStorage) ct.storage;
        ret.cs = (CashShop) ct.cs;
        client.setAccountName(ct.accountname);
        ret.acash = ct.ACash;
        ret.donatecash = ct.DonateCash;
        ret.maplepoints = ct.MaplePoints;
        ret.anticheat = (CheatTracker) ct.anticheat;

        ret.anticheat.start(ret);
        ret.mount = new MapleMount(ret, ct.mount_itemid, ret.stats.getSkillByJob(1004, ret.job), ct.mount_Fatigue, ct.mount_level, ct.mount_exp);
        ret.expirationTask(false, false);
        //ret.stats.recalcLocalStats(true, ret);
        client.setTempIP(ct.tempIP);

        ret.rndGenForCharacter = new CRand32();
        ret.rndForCheckDamageMiss = new CRand32();
        ret.rndGenForMob = new CRand32();

        ret.calcDamage = new CalcDamage();
        ret.antiMacro = new MapleLieDetector(ret);
        return ret;
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) {
        final MapleCharacter ret = new MapleCharacter(channelserver);
        ret.calcDamage = new CalcDamage();
        ret.antiMacro = new MapleLieDetector(ret);
        ret.client = client;
        ret.id = charid;

        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading the Char Failed (char not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getShort("level");
            ret.fame = rs.getInt("fame");

            ret.stats.str = rs.getShort("str");
            ret.stats.dex = rs.getShort("dex");
            ret.stats.int_ = rs.getShort("int");
            ret.stats.luk = rs.getShort("luk");
            ret.stats.maxhp = rs.getInt("maxhp");
            ret.stats.maxmp = rs.getInt("maxmp");
            ret.stats.hp = rs.getInt("hp");
            ret.stats.mp = rs.getInt("mp");
            ret.job = rs.getShort("job");
            ret.gmLevel = rs.getByte("gm");
            ret.hide = ret.isGM();
            ret.exp = (ret.level == 200 || (GameConstants.isKOC(ret.job) && ret.level >= 120)) && !ret.isIntern() ? 0 : rs.getInt("exp");
            ret.hpApUsed = rs.getShort("hpApUsed");
            final String[] sp = rs.getString("sp").split(",");
            for (int i = 0; i < ret.remainingSp.length; i++) {
                ret.remainingSp[i] = Integer.parseInt(sp[i]);
            }
            ret.remainingAp = rs.getShort("ap");
            ret.meso = rs.getInt("meso");
            ret.skinColor = rs.getByte("skincolor");
            ret.gender = rs.getByte("gender");

            ret.hair = rs.getInt("hair");
            ret.face = rs.getInt("face");
            ret.accountid = rs.getInt("accountid");
            client.setAccID(ret.accountid);
            ret.mapid = rs.getInt("map");
            ret.SpawnPoint = rs.getByte("spawnpoint");
            ret.fh = rs.getInt("fh");
            //System.out.println(ret.SpawnPoint + "doe this come when cc?12");
            ret.world = rs.getByte("world");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getByte("guildrank");
            ret.allianceRank = rs.getByte("allianceRank");
            ret.guildContribution = rs.getInt("guildContribution");
            ret.currentrep = rs.getInt("currentrep");
            ret.totalrep = rs.getInt("totalrep");
            ret.firstLoginTime = System.currentTimeMillis();
            ret.makeMFC(rs.getInt("familyid"), rs.getInt("seniorid"), rs.getInt("junior1"), rs.getInt("junior2"));
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.buddylist = new BuddyList(rs.getByte("buddyCapacity"));
            ret.subcategory = rs.getByte("subcategory");
            ret.mount = new MapleMount(ret, 0, ret.stats.getSkillByJob(1004, ret.job), (byte) 0, (byte) 1, 0);
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            ret.marriageId = rs.getInt("marriageId");
            ret.engageId = 0;
            ret.bookCover = rs.getInt("mbookcover");

            if (ret.getDailyQuestBonus() == null) {
                ret.setDailyQuestBonus(0, 0);
            }
//            for (MapleTrait t : ret.traits.values()) {
//                t.setExp(rs.getInt(t.getType().name()));
//            }
            if (channelserver) {
                ret.anticheat = new CheatTracker(ret);
                MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                    ret.map = mapFactory.getMap(100000000);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    MapleParty party = World.Party.getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.party = party;
                    }
                }
                final String[] pets = rs.getString("pets").split(",");
                for (int i = 0; i < ret.petStore.length; i++) {
                    ret.petStore[i] = Byte.parseByte(pets[i]);
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM achievements WHERE accountid = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.finishedAchievements.add(rs.getInt("achievementid"));
                }
                ps.close();
                rs.close();

                ret.rndGenForCharacter = new CRand32();
                ret.rndForCheckDamageMiss = new CRand32();
                ret.rndGenForMob = new CRand32();

                /*ps = con.prepareStatement("SELECT * FROM reports WHERE characterid = ?");
                 ps.setInt(1, charid);
                 rs = ps.executeQuery();
                 while (rs.next()) {
                 if (ReportType.getById(rs.getByte("type")) != null) {
                 ret.reports.put(ReportType.getById(rs.getByte("type")), rs.getInt("count"));
                 }
                 }*/
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT * FROM c_pctime WHERE acc = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            if (rs.next()) { // 캐릭터를 불러올 때 시간이 같으면 가져오고 아니면 리셋
                //if (rs.getInt(4) == GameConstants.getCurrentDate_NoTime()) {
                ret.pctime = rs.getLong(3);
                ret.pcdate = rs.getInt(4);
                //}
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");

            while (rs.next()) {
                final int id = rs.getInt("quest");
                final MapleQuest q = MapleQuest.getInstance(id);
                final byte stat = rs.getByte("status");
                if ((stat == 1 || stat == 2) && channelserver && (q == null || q.isBlocked())) { //bigbang
                    continue;
                }
//                if (stat == 1 && channelserver && !q.canStart(ret, null)) { //bigbang
//                    continue;
//                }
                final MapleQuestStatus status = new MapleQuestStatus(q, stat);
                final long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                status.setCustomData(rs.getString("customData"));
                ret.quests.put(q, status);
                pse.setInt(1, rs.getInt("queststatusid"));
                final ResultSet rsMobs = pse.executeQuery();

                while (rsMobs.next()) {
                    status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                }
                rsMobs.close();
            }
            rs.close();
            ps.close();
            pse.close();

            if (channelserver) {

                ret.monsterbook = new MonsterBook();
                ret.monsterbook.loadCards(charid);

                ps = con.prepareStatement("SELECT * FROM inventoryslot where characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit((byte) 24);
                    ret.getInventory(MapleInventoryType.USE).setSlotLimit((byte) 24);
                    ret.getInventory(MapleInventoryType.SETUP).setSlotLimit((byte) 24);
                    ret.getInventory(MapleInventoryType.ETC).setSlotLimit((byte) 24);
                    ret.getInventory(MapleInventoryType.CASH).setSlotLimit((byte) 96);
//                    throw new RuntimeException("No Inventory slot column found in SQL. [inventoryslot]");
                } else {
                    ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(rs.getByte("equip"));
                    ret.getInventory(MapleInventoryType.USE).setSlotLimit(rs.getByte("use"));
                    ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(rs.getByte("setup"));
                    ret.getInventory(MapleInventoryType.ETC).setSlotLimit(rs.getByte("etc"));
                    ret.getInventory(MapleInventoryType.CASH).setSlotLimit(rs.getByte("cash"));
                }
                ps.close();
                rs.close();

                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(false, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                    if (mit.getLeft().getPet() != null) {
                        ret.pets.add(mit.getLeft().getPet());
                    }
                }

                ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.getClient().setAccountName(rs.getString("name"));
                    ret.acash = rs.getInt("ACash");
                    ret.donatecash = rs.getInt("DonateCash");
                    ret.maplepoints = rs.getInt("mPoints");
                    ret.points = rs.getInt("points");
                    ret.vpoints = rs.getInt("vpoints");

//                    if (rs.getTimestamp("lastlogon") != null) {
//                        final Calendar cal = Calendar.getInstance();
//                        cal.setTimeInMillis(rs.getTimestamp("lastlogon").getTime());
//                        if (cal.get(Calendar.DAY_OF_WEEK) + 1 == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
//                            ret.acash += 500;
//                        }
//                    }
                    if (rs.getInt("banned") > 0) {
                        rs.close();
                        ps.close();
                        ret.getClient().getSession().close(true);
                        throw new RuntimeException("Loading a banned character");
                    }
                    rs.close();
                    ps.close();

                    ps = con.prepareStatement("UPDATE accounts SET lastlogon = CURRENT_TIMESTAMP() WHERE id = ?");
                    ps.setInt(1, ret.accountid);
                    ps.executeUpdate();
                } else {
                    rs.close();
                }
                ps.close();

                ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.questinfo.put(rs.getInt("quest"), rs.getString("customData"));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel, expiration FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Skill skil;
                while (rs.next()) {
                    final int skid = rs.getInt("skillid");
                    skil = SkillFactory.getSkill(skid);
                    int skl = rs.getInt("skilllevel");
                    byte msl = rs.getByte("masterlevel");
                    if (skil != null && GameConstants.isApplicableSkill(skid)) {
                        if (skl > skil.getMaxLevel() && skid < 92000000) {
                            if (!skil.isBeginnerSkill() && skil.canBeLearnedBy(ret.job) && !skil.isSpecialSkill()) {
                                ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += (skl - skil.getMaxLevel());
                            }
                            skl = (byte) skil.getMaxLevel();
                        }
                        if (msl > skil.getMaxLevel()) {
                            msl = (byte) skil.getMaxLevel();
                        }
                        ret.skills.put(skil, new SkillEntry(skl, msl, rs.getLong("expiration")));
                    } else if (skil == null) { //doesnt. exist. e.g. bb
                        if (!GameConstants.isBeginnerJob(skid / 10000) && skid / 10000 != 900 && skid / 10000 != 800 && skid / 10000 != 9000) {
                            ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += skl;
                        }
                    }
                }
                rs.close();
                ps.close();

                ret.expirationTask(false, true); //do it now

                // Bless of Fairy handling
                try {
                    ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                    ps.setInt(1, ret.accountid);
                    rs = ps.executeQuery();
                    int maxlevel_ = 0, maxlevel_2 = 0;
                    while (rs.next()) {
                        if (rs.getInt("id") != charid) { // Not this character
                            if (GameConstants.isKOC(rs.getShort("job"))) {
                                int maxlevel = (rs.getShort("level") / 5);

                                if (maxlevel > 24) {
                                    maxlevel = 24;
                                }
                                if (maxlevel > maxlevel_2 || maxlevel_2 == 0) {
                                    maxlevel_2 = maxlevel;
                                    ret.BlessOfEmpress_Origin = rs.getString("name");
                                }
                            }
                            int maxlevel = (rs.getShort("level") / 10);

                            if (maxlevel > 20) {
                                maxlevel = 20;
                            }
                            if (maxlevel > maxlevel_ || maxlevel_ == 0) {
                                maxlevel_ = maxlevel;
                                ret.BlessOfFairy_Origin = rs.getString("name");
                            }
                        }
                    }
                    if (ret.BlessOfFairy_Origin == null) {
                        ret.BlessOfFairy_Origin = ret.name;
                    }
                    ret.skills.put(SkillFactory.getSkill(GameConstants.getBOF_ForJob(ret.job)), new SkillEntry(maxlevel_, (byte) 0, -1));
                    if (SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)) != null) {
                        if (ret.BlessOfEmpress_Origin == null) {
                            ret.BlessOfEmpress_Origin = ret.BlessOfFairy_Origin;
                        }
                        ret.skills.put(SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)), new SkillEntry(maxlevel_2, (byte) 0, -1));
                    }
                    ps.close();
                    rs.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                /*if (!compensate_previousSP) {
                 for (Entry<Skill, SkillEntry> skill : ret.skills.entrySet()) {
                 if (!skill.getKey().isBeginnerSkill() && !skill.getKey().isSpecialSkill()) {
                 ret.remainingSp[GameConstants.getSkillBookForSkill(skill.getKey().getId())] += skill.getValue().skillevel;
                 skill.getValue().skillevel = 0;
                 }
                 }
                 ret.setQuestAdd(MapleQuest.getInstance(170000), (byte) 0, null); //set it so never again
                 }*/
                // END
                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int position;
                while (rs.next()) {
                    position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                final Map<Integer, Pair<Byte, Integer>> keyb = ret.keylayout.Layout();
                while (rs.next()) {
                    keyb.put(Integer.valueOf(rs.getInt("key")), new Pair<Byte, Integer>(rs.getByte("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();
                ret.keylayout.unchanged();

                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<Integer>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();

                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadStorage(ret.accountid);
                ret.cs = new CashShop(ret.accountid, charid, ret.getJob());

                ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    ret.wishlist[i] = rs.getInt("sn");
                    i++;
                }
                while (i < 10) {
                    ret.wishlist[i] = 0;
                    i++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int r = 0;
                while (rs.next()) {
                    ret.rocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 10) {
                    ret.rocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM regrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.regrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 5) {
                    ret.regrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM hyperrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.hyperrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 13) {
                    ret.hyperrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    PreparedStatement dps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
                    dps.setInt(1, ret.id);
                    dps.setByte(2, (byte) 1);
                    dps.setInt(3, 0);
                    dps.setByte(4, (byte) 0);
                    dps.execute();
                    dps.close();
                    ps.close();
                    rs.close();
                    ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();
                    rs.next();
//                    throw new RuntimeException("No mount data found on SQL column");
                }
                final Item mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -23);
                ret.mount = new MapleMount(ret, mount != null ? mount.getItemId() : 0, 1004, rs.getByte("Fatigue"), rs.getByte("Level"), rs.getInt("Exp"));
                ps.close();
                rs.close();

                ret.stats.recalcLocalStats(true, ret, false);
            } else { // Not channel server
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(true, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                }
            }
        } catch (Exception ess) {
            ess.printStackTrace();
            System.out.println("Failed to load character..");
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ess);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (pse != null) {
                try {
                    pse.close();
                } catch (Exception e) {
                }
            }
        }
        ret.loadMagnetSettings();
        return ret;
    }

    public static void saveNewCharToDB(final MapleCharacter chr, final JobType type, short db) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("INSERT INTO characters (level, str, dex, luk, `int`, hp, mp, maxhp, maxmp, sp, ap, skincolor, gender, job, hair, face, map, meso, party, buddyCapacity, pets, subcategory, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, chr.level); // Level
            final PlayerStats stat = chr.stats;
            ps.setShort(2, stat.getStr()); // Str
            ps.setShort(3, stat.getDex()); // Dex
            ps.setShort(4, stat.getLuk()); // Luk
            ps.setShort(5, stat.getInt()); // Int
            ps.setInt(6, stat.getHp()); // HP
            ps.setInt(7, stat.getMp());
            ps.setInt(8, stat.getMaxHp()); // MP
            ps.setInt(9, stat.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < chr.remainingSp.length; i++) {
                sps.append(chr.remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(10, sp.substring(0, sp.length() - 1));
            ps.setShort(11, (short) chr.remainingAp); // Remaining AP
            ps.setByte(12, chr.skinColor);
            ps.setByte(13, chr.gender);
            ps.setShort(14, chr.job);
            ps.setInt(15, chr.hair);
            ps.setInt(16, chr.face);
            ps.setInt(17, type.map);
            ps.setInt(18, chr.meso); // Meso
            ps.setInt(19, -1); // Party
            ps.setByte(20, chr.buddylist.getCapacity()); // Buddylist
            ps.setString(21, "-1,-1,-1");
            ps.setInt(22, db); //for now
            ps.setInt(23, chr.getAccountID());
            ps.setString(24, chr.name);
            ps.setByte(25, chr.world);
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                ps.close();
                rs.close();
                throw new DatabaseException("Inserting char failed.");
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (final MapleQuestStatus q : chr.quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);

            for (final Entry<Skill, SkillEntry> skill : chr.skills.entrySet()) {
                if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                    ps.setInt(2, skill.getKey().getId());
                    ps.setInt(3, skill.getValue().skillevel);
                    ps.setByte(4, skill.getValue().masterlevel);
                    ps.setLong(5, skill.getValue().expiration);
                    ps.execute();
                }
            }
            ps.close();

            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 32); // Eq
            ps.setByte(3, (byte) 32); // Use
            ps.setByte(4, (byte) 32); // Setup
            ps.setByte(5, (byte) 32); // ETC
            ps.setByte(6, (byte) 96); // Cash
            ps.execute();
            ps.close();

            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 1);
            ps.setInt(3, 0);
            ps.setByte(4, (byte) 0);
            ps.execute();
            ps.close();

            List<Pair<Item, MapleInventoryType>> listing = new ArrayList<Pair<Item, MapleInventoryType>>();
            for (final MapleInventory iv : chr.inventory) {
                for (final Item item : iv.list()) {
                    listing.add(new Pair<Item, MapleInventoryType>(item, iv.getType()));
                }
            }
            ItemLoader.INVENTORY.saveItems(listing, con, chr.id);

            con.commit();
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            System.err.println("[charsave] Error saving character data");
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                ex.printStackTrace();
                System.err.println("[charsave] Error Rolling Back");
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                e.printStackTrace();
                System.err.println("[charsave] Error going back to autocommit mode");
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (pse != null) {
                try {
                    pse.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void saveToDB(boolean dc, boolean fromcs) {
        Connection con = null;

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, pets = ?, subcategory = ?, marriageId = ?, currentrep = ?, totalrep = ?, mbookcover = ?, fh = ?, name = ? WHERE id = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, level);
            ps.setInt(2, fame);
            ps.setShort(3, stats.getStr());
            ps.setShort(4, stats.getDex());
            ps.setShort(5, stats.getLuk());
            ps.setShort(6, stats.getInt());
            ps.setInt(7, (level == 200 || (GameConstants.isKOC(job) && level >= 120)) && !isIntern() ? 0 : exp);
            ps.setInt(8, stats.getHp() < 1 ? 50 : stats.getHp());
            ps.setInt(9, stats.getMp());
            ps.setInt(10, stats.getMaxHp());
            ps.setInt(11, stats.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < remainingSp.length; i++) {
                sps.append(remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(12, sp.substring(0, sp.length() - 1));
            ps.setShort(13, remainingAp);
            ps.setByte(14, gmLevel);
            ps.setByte(15, skinColor);
            ps.setByte(16, gender);
            ps.setShort(17, job);
            ps.setInt(18, hair);
            ps.setInt(19, face);
            if (!fromcs && map != null) {
                if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                    ps.setInt(20, map.getForcedReturnId());
                } else if (mapid >= 925010000 && mapid <= 925010300) {
                    ps.setInt(20, 120000104);
                } else {
                    ps.setInt(20, stats.getHp() < 1 ? map.getReturnMapId() : map.getId());
                }
            } else {
                ps.setInt(20, mapid);
            }
            ps.setInt(21, meso);
            ps.setShort(22, hpApUsed);
            if (map == null) {
                if (fromcs) {
                    return;
                } else {
                    ps.setByte(23, (byte) 0);
                }
            } else {
                final MaplePortal closest = map.findClosestSpawnpoint(getTruePosition());
                setSpawnpoint((byte) closest.getId());
                ps.setByte(23, (byte) (closest != null ? closest.getId() : 0));
            }
            ps.setInt(24, party == null ? -1 : party.getId());
            ps.setShort(25, buddylist.getCapacity());
            final StringBuilder petz = new StringBuilder();
            int petLength = 0;
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    pet.saveToDb();
                    petz.append(pet.getInventoryPosition());
                    petz.append(",");
                    petLength++;
                }
            }
            while (petLength < 3) {
                petz.append("-1,");
                petLength++;
            }
            final String petstring = petz.toString();
            ps.setString(26, petstring.substring(0, petstring.length() - 1));
            ps.setByte(27, subcategory);
            ps.setInt(28, marriageId);
            ps.setInt(29, currentrep);
            ps.setInt(30, totalrep);
            ps.setInt(31, bookCover);
            ps.setInt(32, fh);
            ps.setString(33, name);
            ps.setInt(34, id);
            if (ps.executeUpdate() < 1) {
                ps.close();
                throw new DatabaseException("Character not in database (" + id + ")");
            }
            ps.close();
            if (changed_skillmacros) {
                deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                for (int i = 0; i < 5; i++) {
                    final SkillMacro macro = skillMacros[i];
                    if (macro.getSkill1() == 0 && macro.getSkill2() == 0 && macro.getSkill3() == 0) {
                        //매크로 스킬이 없으면 컨티뉴
                        continue;
                    }
                    if (macro != null) {
                        ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                        ps.setInt(1, id);
                        ps.setInt(2, macro.getSkill1());
                        ps.setInt(3, macro.getSkill2());
                        ps.setInt(4, macro.getSkill3());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getShout());
                        ps.setInt(7, i);
                        ps.execute();
                        ps.close();
                    }
                }
            }

            deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setByte(2, getInventory(MapleInventoryType.EQUIP).getSlotLimit());
            ps.setByte(3, getInventory(MapleInventoryType.USE).getSlotLimit());
            ps.setByte(4, getInventory(MapleInventoryType.SETUP).getSlotLimit());
            ps.setByte(5, getInventory(MapleInventoryType.ETC).getSlotLimit());
            ps.setByte(6, getInventory(MapleInventoryType.CASH).getSlotLimit());
            ps.execute();
            ps.close();

            saveInventory(con);

            if (changed_questinfo) {
                deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final Entry<Integer, String> q : questinfo.entrySet()) {
                    ps.setInt(2, q.getKey());
                    ps.setString(3, q.getValue());
                    ps.execute();
                }
                ps.close();
            }

            deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, id);
            for (final MapleQuestStatus q : quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

            if (changed_skills) {
                deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, id);

                for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                    if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                        ps.setInt(2, skill.getKey().getId());
                        ps.setInt(3, skill.getValue().skillevel);
                        ps.setByte(4, skill.getValue().masterlevel);
                        ps.setLong(5, skill.getValue().expiration);
                        ps.execute();
                    }
                }
                ps.close();
            }

            List<MapleCoolDownValueHolder> cd = getCooldowns();
            if (dc && cd.size() > 0) {
                ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (final MapleCoolDownValueHolder cooling : cd) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.execute();
                }
                ps.close();
            }

            if (changed_savedlocations) {
                deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                    if (savedLocations[savedLocationType.getValue()] != -1) {
                        ps.setInt(2, savedLocationType.getValue());
                        ps.setInt(3, savedLocations[savedLocationType.getValue()]);
                        ps.execute();
                    }
                }
                ps.close();
            }

            if (changed_achievements) {
                ps = con.prepareStatement("DELETE FROM achievements WHERE accountid = ?");
                ps.setInt(1, accountid);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("INSERT INTO achievements(charid, achievementid, accountid) VALUES(?, ?, ?)");
                for (Integer achid : finishedAchievements) {
                    ps.setInt(1, id);
                    ps.setInt(2, achid);
                    ps.setInt(3, accountid);
                    ps.execute();
                }
                ps.close();
            }

            if (changed_reports) {
                deleteWhereCharacterId(con, "DELETE FROM reports WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO reports VALUES(DEFAULT, ?, ?, ?)");
                for (Entry<ReportType, Integer> achid : reports.entrySet()) {
                    ps.setInt(1, id);
                    ps.setByte(2, achid.getKey().i);
                    ps.setInt(3, achid.getValue());
                    ps.execute();
                }
                ps.close();
            }

            if (buddylist.changed()) {
                deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `groupname`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, id);
                for (BuddylistEntry entry : buddylist.getBuddies()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.setInt(3, entry.isVisible() ? 0 : 1);
                    ps.setString(4, entry.getGroup());
                    ps.execute();
                }
                ps.close();
                buddylist.setChanged(false);
            }

            ps = con.prepareStatement("UPDATE accounts SET `DonateCash` = ?, `ACash` = ?, `mPoints` = ?, `points` = ?, `vpoints` = ? WHERE id = ?");
            ps.setInt(1, donatecash);
            ps.setInt(2, acash);
            ps.setInt(3, maplepoints);
            ps.setInt(4, points);
            ps.setInt(5, vpoints);
            ps.setInt(6, client.getAccID());
            ps.executeUpdate();
            ps.close();

            if (storage != null) {
                storage.saveToDB(con);
            }
            if (cs != null) {
                cs.save(con);
            }
            //PlayerNPC.updateByCharId(this);
            keylayout.saveKeys(id, con);
            mount.saveMount(id, con);
            monsterbook.saveCards(id, con);

            if (changed_wishlist) {
                deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
                for (int i = 0; i < getWishlistSize(); i++) {
                    ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ");
                    ps.setInt(1, getId());
                    ps.setInt(2, wishlist[i]);
                    ps.execute();
                    ps.close();
                }
            }
            if (changed_trocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
                for (int i = 0; i < rocks.length; i++) {
                    if (rocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, rocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }

            if (changed_regrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM regrocklocations WHERE characterid = ?");
                for (int i = 0; i < regrocks.length; i++) {
                    if (regrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO regrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, regrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_hyperrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM hyperrocklocations WHERE characterid = ?");
                for (int i = 0; i < hyperrocks.length; i++) {
                    if (hyperrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO hyperrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, hyperrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_questinfo = false;
            changed_achievements = false;
            changed_skills = false;
            changed_reports = false;
            con.commit();
            saveToPC();//피시방 저장
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            System.err.println(MapleClient.getLogMessage(this, "[charsave] Error saving character data") + e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error Rolling Back") + e);
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error going back to autocommit mode") + e);
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (pse != null) {
                try {
                    pse.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        deleteWhereCharacterId(con, sql, id);
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    public static void deleteWhereCharacterId_NoLock(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.execute();
        ps.close();
    }

    public void saveInventory(Connection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> listing = new ArrayList<Pair<Item, MapleInventoryType>>();
        for (final MapleInventory iv : inventory) {
            for (final Item item : iv.list()) {
                listing.add(new Pair<Item, MapleInventoryType>(item, iv.getType()));
            }
        }
        if (con != null) {
            ItemLoader.INVENTORY.saveItems(listing, con, id);
        } else {
            ItemLoader.INVENTORY.saveItems(listing, id);
        }
    }

    public final PlayerStats getStat() {
        return stats;
    }

    public final void QuestInfoPacket(final tools.data.MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(questinfo.size());

        for (final Entry<Integer, String> q : questinfo.entrySet()) {
            mplew.writeShort(q.getKey());
            mplew.writeMapleAsciiString(q.getValue() == null ? "" : q.getValue());
        }
    }

    public final void updateInfoQuest(final int questid, final String data) {
        questinfo.put(questid, data);
        changed_questinfo = true;
//        if (questid == 29003) { //fucking hard coding..
//        } 
//        else {
        client.getSession().write(MaplePacketCreator.updateInfoQuest(questid, data));
//        }
    }

    public final String getInfoQuest(final int questid) {
        if (questinfo.containsKey(questid)) {
            return questinfo.get(questid);
        }
        return "";
    }

    public final int getNumQuest() {
        int i = 0;
        for (final MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !(q.isCustom())) {
                i++;
            }
        }
        return i;
    }

    public final byte getQuestStatus(final int quest) {
        final MapleQuest qq = MapleQuest.getInstance(quest);
        if (getQuestNoAdd(qq) == null) {
            return 0;
        }
        return getQuestNoAdd(qq).getStatus();
    }

    public final MapleQuestStatus getQuest(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, (byte) 0);
        }
        return quests.get(quest);
    }

    public final void setQuestAdd(final MapleQuest quest, final byte status, final String customData) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus stat = new MapleQuestStatus(quest, status);
            stat.setCustomData(customData);
            quests.put(quest, stat);
        }
    }

    public final MapleQuestStatus getQuestNAdd(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus status = new MapleQuestStatus(quest, (byte) 0);
            quests.put(quest, status);
            return status;
        }
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestNoAdd(final MapleQuest quest) {
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestRemove(final MapleQuest quest) {
        return quests.remove(quest);
    }

    public final void updateQuest(final MapleQuestStatus quest) {
        updateQuest(quest, false);
    }

    public final void updateQuest(final MapleQuestStatus quest, final boolean update) {

        quests.put(quest.getQuest(), quest);
        if (!(quest.isCustom())) {
            client.getSession().write(MaplePacketCreator.updateQuest(quest));
            if (quest.getStatus() >= 1 && !update && quest.getNpc() != 0) {
                client.getSession().write(MaplePacketCreator.updateQuestInfo(this, quest.getQuest().getId(), quest.getNpc(), (byte) 10));
            }
        }
    }

    public final Map<Integer, String> getInfoQuest_Map() {
        return questinfo;
    }

    public final Map<MapleQuest, MapleQuestStatus> getQuest_Map() {
        return quests;
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Integer.valueOf(mbsvh.value);
    }

    public final Integer getBuffedSkill_X(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getX();
    }

    public final Integer getBuffedSkill_Y(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getY();
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null || mbsvh.effect == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public int getBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : mbsvh.effect.getSourceId();
    }

    public int getTrueBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : (mbsvh.effect.isSkill() ? mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId());
    }

    public MapleBuffStatValueHolder getBSVH(MapleBuffStat stat) {
        return effects.get(stat);
    }

    private MapleBuffStatValueHolder saveBSVH;

    public MapleBuffStatValueHolder getSaveBSVH() {
        return saveBSVH;
    }

    public void setSaveBSVH(MapleBuffStatValueHolder saveBSVH) {
        this.saveBSVH = saveBSVH;
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public int getRemainingExpirationDay(int itemid) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].expirationById(itemid);
        return possesed;
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setSchedule(MapleBuffStat effect, ScheduledFuture<?> sched) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.schedule.cancel(false);
        mbsvh.schedule = sched;
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Long.valueOf(mbsvh.startTime);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : mbsvh.effect;
    }

    public void doDragonBlood() {
        final MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.DRAGONBLOOD);
        if (bloodEffect == null) {
            lastDragonBloodTime = 0;
            return;
        }
        prepareDragonBlood();
        if (stats.getHp() - bloodEffect.getX() <= 1) {
            cancelBuffStats(true, MapleBuffStat.DRAGONBLOOD);
            cancelBuffStats(true, MapleBuffStat.WATK);
        } else {
            addHP(-bloodEffect.getX());
            client.getSession().write(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()));
            map.broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()), false);
        }
    }

    public void doBeholderHealing() {
        if (getBuffSource(MapleBuffStat.SUMMON) != 1321007) {//beholder
            return;
        }
        Skill bHealing = SkillFactory.getSkill(1320008);
        int bHealingLvl = getSkillLevel(bHealing);
        if (bHealingLvl > 0) {
            final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
            int healInterval = healEffect.getX() * 1000;
            // if (lastBeholderHealTime + healInterval < System.currentTimeMillis()
            //       && (getStat().getHp() + healEffect.getHp()) * 100 / getStat().getCurrentMaxHp() <= SkillFactory.getSkill(1320006).getEffect(getTotalSkillLevel(SkillFactory.getSkill(1320006))).getX()) {
            addHP(healEffect.getHp());
            dropMessage(6, "healEffect.getHp() : " + healEffect.getHp());
            getMap().broadcastMessage(this, MaplePacketCreator.summonSkill(id, 1321007, 5), getTruePosition());
            getMap().broadcastMessage(this, MaplePacketCreator.showBuffeffect(id, 1321007, 2, level, bHealingLvl), false);//forOthers
            getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(1321007, 2, level, bHealingLvl));//forClient
            getClient().getSession().write(MaplePacketCreator.summonSkill(id, 1321007, 5));
            lastBeholderHealTime = System.currentTimeMillis();
            //   }
            Skill bBuff = SkillFactory.getSkill(1320009);
            int bBuffLvl = getSkillLevel(bBuff);
            byte buffEff = (byte) Randomizer.rand(0, 4);
            if (bBuffLvl > 0) {

                final MapleStatEffect buffEffect = bBuff.getEffect(bBuffLvl);
                buffEffect.applyTo(MapleCharacter.this);
                getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(id, 2, level, bBuffLvl));
                getMap().broadcastMessage(MaplePacketCreator.summonSkill(getId(), id, buffEff + 6));
                getMap().broadcastMessage(this, MaplePacketCreator.showBuffeffect(id, 1320008, 2, level, bBuffLvl), false);

            }
        }
    }

    public boolean canBeholderBuff() {
        if (lastBeholderBuffTime + 4000 < System.currentTimeMillis()) { //time safe
            lastBeholderBuffTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public final boolean canBlood(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    public final boolean canBeHeal(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    private void prepareDragonBlood() {
        lastDragonBloodTime = System.currentTimeMillis();
    }

    public void doRecovery() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.RECOVERY);
        if (bloodEffect != null) {
            prepareRecovery();
            if (stats.getHp() >= stats.getCurrentMaxHp()) {
                cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
            } else {
                healHP(bloodEffect.getX());
            }
        }
    }

    public final boolean canRecover(long now) {
        return lastRecoveryTime > 0 && lastRecoveryTime + 5000 < now;
    }

    private void prepareRecovery() {
        lastRecoveryTime = System.currentTimeMillis();
    }

    public void doPotentialRecovery() {
        if (getStat().recoverHP > 0) {
            prepareRecovery();
            //healHP(getStat().recoverHP);
            addHP(getStat().recoverHP);
            //dropMessage(6, "잠재로HP힐함" + getStat().recoverHP);
        }
        if (getStat().recoverMP > 0) {
            prepareRecovery();
            //healHP(getStat().recoverHP);
            addMP(getStat().recoverMP);
            //dropMessage(6, "잠재로MP힐함" + getStat().recoverMP);
        }
    }

    public final boolean canPotentialRecover(long now) {
        if (4000 > 0 && lastPotentialRecoveryTime + 4000 < now) {
            lastPotentialRecoveryTime = now;
            return true;
        }
        return false;
    }

    private void preparePotentialRecovery() {
        lastPotentialRecoveryTime = System.currentTimeMillis();
    }

    public void startMapTimeLimitTask(int time, final MapleMap to) {
        if (time <= 0) { //jail
            time = 1;
        }
        client.getSession().write(MaplePacketCreator.getClock(time));
        final MapleMap ourMap = getMap();
        time *= 1000;
        mapTimeLimitTask = MapTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                if (ourMap.getId() == GameConstants.JAIL) {
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData("0"); //release them!
                }
                changeMap(to, to.getPortal(0));
            }
        }, time, time);
    }

    public boolean canDOT(long now) {
        return lastDOTTime > 0 && lastDOTTime + 8000 < now;
    }

    public boolean hasDOT() {
        return dotHP > 0;
    }

    public void doDOT() {
        addHP(-(dotHP * 4));
        dotHP = 0;
        lastDOTTime = 0;
    }

    public void setDOT(int d, int source, int sourceLevel) {
        this.dotHP = d;
        addHP(-(dotHP * 4));
        map.broadcastMessage(MaplePacketCreator.getPVPMist(id, source, sourceLevel, d));
        lastDOTTime = System.currentTimeMillis();
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
            mapTimeLimitTask = null;
        }
    }

    public int getNeededExp() {
        return GameConstants.getExpNeededForLevel(level);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, int from) {
        registerEffect(effect, starttime, schedule, effect.getStatups(), false, effect.getDuration(), from);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, Map<MapleBuffStat, Integer> statups, boolean silent, final int localDuration, final int cid) {
        if (effect.isHide()) {
            map.broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood();
        } else if (effect.isRecovery()) {
            prepareRecovery();
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isMonsterRiding_()) {
            getMount().startSchedule();
        }
        for (Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            int value = statup.getValue().intValue();
            if (statup.getKey() == MapleBuffStat.MONSTER_RIDING) {
                if (effect.getSourceId() == 5221006 && battleshipHP <= 0) {
                    battleshipHP = maxBattleshipHP(effect.getSourceId()); //copy this as well
                }
            }
            effects.put(statup.getKey(), new MapleBuffStatValueHolder(effect, starttime, schedule, value, localDuration, cid));

        }
        if (!silent) {
            stats.recalcLocalStats(this);
        }
    }

    public List<MapleBuffStat> getBuffStats(final MapleStatEffect effect, final long startTime) {
        final List<MapleBuffStat> bstats = new ArrayList<MapleBuffStat>();
        final Map<MapleBuffStat, MapleBuffStatValueHolder> allBuffs = new EnumMap<MapleBuffStat, MapleBuffStatValueHolder>(effects);
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : allBuffs.entrySet()) {
            final MapleBuffStatValueHolder mbsvh = stateffect.getValue();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime)) {
                bstats.add(stateffect.getKey());
            }
        }
        return bstats;
    }

    private boolean deregisterBuffStats(List<MapleBuffStat> stats) {
        boolean clonez = false;
        List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<MapleBuffStatValueHolder>(stats.size());
        for (MapleBuffStat stat : stats) {
            final MapleBuffStatValueHolder mbsvh = effects.remove(stat);
            if (mbsvh != null) {
                boolean addMbsvh = true;
                for (MapleBuffStatValueHolder contained : effectsToCancel) {
                    if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                        addMbsvh = false;
                    }
                }
                if (addMbsvh) {
                    effectsToCancel.add(mbsvh);
                }
                if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.SUMMON2 || stat == MapleBuffStat.PUPPET || stat == MapleBuffStat.RAINING_MINES || stat == MapleBuffStat.REAPER) {
                    final int summonId = mbsvh.effect.getSourceId();
                    final List<MapleSummon> toRemove = new ArrayList<MapleSummon>();
                    visibleMapObjectsLock.writeLock().lock(); //We need to lock this later on anyway so do it now to prevent deadlocks.
                    summonsLock.writeLock().lock();
                    try {
                        /*byte i = 0;
                         for (MapleSummon summon : summons) {
                         if (summon.getObjectId() == mbsvh.effect.octoOID.get(i)) { //removes bots n tots
                         map.broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
                         map.removeMapObject(summon);
                         visibleMapObjects.remove(summon);
                         toRemove.add(summon);
                         mbsvh.effect.octoOID.remove(i);
                         i++;
                         }
                         }*/
                        for (MapleSummon summon : summons) {
                            if (summon.getSkill() == summonId) { //removes bots n tots
                                map.broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
                                map.removeMapObject(summon);
                                visibleMapObjects.remove(summon);
                                toRemove.add(summon);
                            }
                        }
                        for (MapleSummon s : toRemove) {
                            summons.remove(s);
                        }
                    } finally {
                        summonsLock.writeLock().unlock();
                        visibleMapObjectsLock.writeLock().unlock(); //lolwut
                    }
                } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                    lastDragonBloodTime = 0;
                } else if (stat == MapleBuffStat.RECOVERY) {
                    lastRecoveryTime = 0;
                }
            }
        }
        for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
            if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).size() == 0) {
                if (cancelEffectCancelTasks.schedule != null) {
                    cancelEffectCancelTasks.schedule.cancel(false);
                }
            }
        }
        return clonez;
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the
     * Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(final MapleStatEffect effect, final long startTime, boolean again) {
        if (effect == null) {
            return;
        }
        cancelEffect(effect, startTime, effect.getStatups(), true, again);
    }

    public void cancelEffect(final MapleStatEffect effect, final long startTime) {
        if (effect == null) {
            return;
        }
        cancelEffect(effect, startTime, effect.getStatups(), true, false);
    }

    public void cancelEffect(final MapleStatEffect effect, final long startTime, Map<MapleBuffStat, Integer> statups, boolean recalcStat, boolean again) {
        if (effect == null) {
            return;
        }
        List<MapleBuffStat> buffstats = new ArrayList<MapleBuffStat>(statups.keySet());
        if (effect.isMonsterRiding()) {
            buffstats = getBuffStats(effect, startTime);
        }
        if (effect.getSourceId() == 35121013) { //when siege 2 deactivates, missile re-activates
            SkillFactory.getSkill(35121005).getEffect(getTotalSkillLevel(35121005)).applyTo(this);
        }
        if (effect.getSourceId() == 35111004) {
            SkillFactory.getSkill(35001002).getEffect(getTotalSkillLevel(35001002)).applyTo(this);
        }

        try {
            //버프캔슬
            for (MapleBuffStat stat : statups.keySet()) {
                if (getBuffSource(stat) != effect.getSourceId()) {
                    if (stat == MapleBuffStat.PUPPET || stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.SUMMON2 || stat == MapleBuffStat.DASH_SPEED || stat == MapleBuffStat.DASH_JUMP
                            || stat == MapleBuffStat.BLUE_AURA || stat == MapleBuffStat.DARK_AURA || stat == MapleBuffStat.YELLOW_AURA || stat == MapleBuffStat.AURA
                            || stat == MapleBuffStat.EXPRATE || stat == MapleBuffStat.DROP_RATE || stat == MapleBuffStat.GHOST_MORPH) {
                        continue;
                    }
                    buffstats.remove(stat);
                }
            }
        } catch (Exception e) {
            System.err.println("buff err");
        }
        if (effect.isInfinity() && getBuffedValue(MapleBuffStat.INFINITY) != null) { //before
            int duration = Math.max(effect.getDuration(), effect.alchemistModifyVal2(this, effect.getDuration(), false));
            final long start = getBuffedStarttime(MapleBuffStat.INFINITY);
            duration += (int) ((start - System.currentTimeMillis()));
            if (duration > 0) {
                final int neworbcount = getBuffedValue(MapleBuffStat.INFINITY) + effect.getDamage();
                final Map<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.INFINITY, neworbcount);
                setBuffedValue(MapleBuffStat.INFINITY, neworbcount);
                client.getSession().write(TemporaryStatsPacket.giveBuff(effect.getSourceId(), duration, stat, effect));
                addHP((int) (effect.getHpR() * this.stats.getCurrentMaxHp()));
                addMP((int) (effect.getMpR() * this.stats.getCurrentMaxMp()));
                setSchedule(MapleBuffStat.INFINITY, BuffTimer.getInstance().schedule(new CancelEffectAction(this, effect, start, stat), effect.alchemistModifyVal2(this, 4000, false)));
                return;
            }
        }
        deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            // remove for all on maps
            if (!getDoors().isEmpty()) {
                removeDoor();
                silentPartyUpdate();
            }
        } else if (effect.isMechDoor()) {
            if (!getMechDoors().isEmpty()) {
                removeMechDoor();
            }
        } else if (effect.isMonsterRiding_()) {
            getMount().cancelSchedule();
        } else if (effect.isMonsterRiding()) {
            cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (effect.isAranCombo()) {
            combo = 0;
        }

        if (effect.getSourceId() == 35111004) { //헤비 머신전 3차 시절
            SkillFactory.getSkill(35001002).getEffect(getTotalSkillLevel(35001002)).applyTo(this);
        }

        if (effect.getSourceId() == 35121003) { //강제로 타이탄 캔슬 막아서 작동되게
            return;
        }
        switch (effect.getSourceId()) {
            case 2450018:
            case 2022694:
            case 2000000:
                getClient().getSession().write(FamilyPacket.cancelFamilyBuff());
                break;
        }
        // check if we are still logged in o.o
        cancelPlayerBuffs(buffstats, recalcStat);
        if (effect.isHide() && client.getChannelServer().getPlayerStorage().getCharacterById(this.getId()) != null) { //Wow this is so fking hacky...
            map.broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);

            sendTemporaryStats();
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    map.broadcastMessage(this, PetPacket.showPet(this, pet, false, false), false);
                }
            }
        }
    }

    public void cancelBuffStats(boolean recalcStat, MapleBuffStat... stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList, recalcStat);
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        if (effects.get(stat) != null) {
            cancelEffect(effects.get(stat).effect, -1);
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat, boolean again) {
        if (effects.get(stat) != null) {
            cancelEffect(effects.get(stat).effect, -1, again);
        }
    }

    public void cancelSummonEffectFromSkillID(int summonSkillID) {
        if (getBuffSource(MapleBuffStat.SUMMON) == summonSkillID) {
            cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
        }
        if (getBuffSource(MapleBuffStat.SUMMON2) == summonSkillID) {
            cancelEffectFromBuffStat(MapleBuffStat.SUMMON2);
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat, int from) {
        if (effects.get(stat) != null && effects.get(stat).cid == from) {
            cancelEffect(effects.get(stat).effect, -1);
        }
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats, boolean recalcStat) {
        //this.dropMessage(6, "buffstats" + buffstats);
        boolean write = client != null && client.getChannelServer() != null && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
        if (write && recalcStat) {
            stats.recalcLocalStats(this);
        }
        client.getSession().write(TemporaryStatsPacket.cancelBuff(buffstats));
        map.broadcastMessage(this, TemporaryStatsPacket.cancelForeignBuff(getId(), buffstats), false);
    }

    public void dispel() {
        if (!isHidden()) {
            final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
            for (MapleBuffStatValueHolder mbsvh : allBuffs) {
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph() && !mbsvh.effect.isGmBuff() && !mbsvh.effect.isMonsterRiding() && !mbsvh.effect.isMechChange() && !mbsvh.effect.isDispelImmuneBuff()) {
                    cancelEffect(mbsvh.effect, mbsvh.startTime);
                }
            }
        }
    }

    public void dispelSkill(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
                break;
            }
        }
    }

    public void dispelSummons() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSummonMovementType() != null) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
            }
        }
    }

    public void dispelBuff(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
                break;
            }
        }
    }

    public void cancelAllBuffs_() {
        effects.clear();
    }

    public void cancelAllBuffs_Dead() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceId() != 22181003) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
            }
        }
    }

    public void cancelAllBuffs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, mbsvh.startTime);
        }
    }

    public void cancelMorphs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            switch (mbsvh.effect.getSourceId()) {
                case 5111005:
                case 5121003:
                case 15111002:
                case 13111005:
                    return; // Since we can't have more than 1, save up on loops
                default:
                    if (mbsvh.effect.isMorph()) {
                        cancelEffect(mbsvh.effect, mbsvh.startTime);
                        continue;
                    }
            }
        }
    }

    public int getMorphState() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMorph()) {
                return mbsvh.effect.getSourceId();
            }
        }
        return -1;
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs == null) {
            return;
        }
        for (PlayerBuffValueHolder mbsvh : buffs) {
            if (System.currentTimeMillis() - mbsvh.startTime > mbsvh.localDuration) {
                // 버프 시간이 지나면 강제로 0.3초로 조정
                mbsvh.effect.silentApplyBuff(this, System.currentTimeMillis(), 300, mbsvh.statup, mbsvh.cid);
                continue;
            }
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime, mbsvh.localDuration, mbsvh.statup, mbsvh.cid);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        final List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
        final Map<Pair<Integer, Byte>, Integer> alreadyDone = new HashMap<Pair<Integer, Byte>, Integer>();
        final LinkedList<Entry<MapleBuffStat, MapleBuffStatValueHolder>> allBuffs = new LinkedList<Entry<MapleBuffStat, MapleBuffStatValueHolder>>(effects.entrySet());
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : allBuffs) {
            final Pair<Integer, Byte> key = new Pair<Integer, Byte>(mbsvh.getValue().effect.getSourceId(), mbsvh.getValue().effect.getLevel());
            if (alreadyDone.containsKey(key)) {
                ret.get(alreadyDone.get(key)).statup.put(mbsvh.getKey(), mbsvh.getValue().value);
            } else {
                alreadyDone.put(key, ret.size());
                final EnumMap<MapleBuffStat, Integer> list = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                list.put(mbsvh.getKey(), mbsvh.getValue().value);
                ret.add(new PlayerBuffValueHolder(mbsvh.getValue().startTime, mbsvh.getValue().effect, list, mbsvh.getValue().localDuration, mbsvh.getValue().cid));
            }
        }
        return ret;
    }

    public void cancelMagicDoor() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, mbsvh.startTime);
                break;
            }
        }
    }

    public int getSkillLevel(int skillid) {
        return getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getTotalSkillLevel(int skillid) {
        return getTotalSkillLevel(SkillFactory.getSkill(skillid));
    }

    public final void handleBattleshipHP(int damage) {
        if (damage < 0) {
            final MapleStatEffect effect = getStatForBuff(MapleBuffStat.MONSTER_RIDING);
            if (effect != null && effect.getSourceId() == 5221006) {
                battleshipHP += damage;
                client.getSession().write(MaplePacketCreator.skillCooldown(5221999, battleshipHP / 10));
                if (battleshipHP <= 0) {
                    battleshipHP = 0;
                    client.getSession().write(MaplePacketCreator.skillCooldown(5221006, effect.getCooldown()));
                    addCooldown(5221006, System.currentTimeMillis(), effect.getCooldown() * 1000);
                    cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                }
            }
        }
    }

    public final void handleOrbgain() {
        int orbcount = getBuffedValue(MapleBuffStat.COMBO);
        Skill combo;
        Skill advcombo;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                combo = SkillFactory.getSkill(11111001);
                advcombo = SkillFactory.getSkill(11110005);
                break;
            default:
                combo = SkillFactory.getSkill(1111002);
                advcombo = SkillFactory.getSkill(1120003);
                break;
        }

        MapleStatEffect ceffect = null;
        int advComboSkillLevel = getTotalSkillLevel(advcombo);
        if (advComboSkillLevel > 0) {
            ceffect = advcombo.getEffect(advComboSkillLevel);
        } else if (getSkillLevel(combo) > 0) {
            ceffect = combo.getEffect(getTotalSkillLevel(combo));
        } else {
            return;
        }

        if (orbcount < ceffect.getX() + 1) {
            int neworbcount = orbcount + 1;
            if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                if (neworbcount < ceffect.getX() + 1) {
                    neworbcount++;
                }
            }
            EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
            stat.put(MapleBuffStat.COMBO, neworbcount);
            setBuffedValue(MapleBuffStat.COMBO, neworbcount);
            int duration = ceffect.getDuration();
            duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

            client.getSession().write(TemporaryStatsPacket.giveBuff(combo.getId(), duration, stat, ceffect));
            map.broadcastMessage(this, TemporaryStatsPacket.giveForeignBuff(getId(), stat, ceffect), false);
        }
    }

    public void handleOrbconsume(int howmany) {
        Skill combo;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                combo = SkillFactory.getSkill(11111001);
                break;
            default:
                combo = SkillFactory.getSkill(1111002);
                break;
        }
        if (getSkillLevel(combo) <= 0) {
            return;
        }
        MapleStatEffect ceffect = getStatForBuff(MapleBuffStat.COMBO);
        if (ceffect == null) {
            return;
        }
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        stat.put(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        setBuffedValue(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        int duration = ceffect.getDuration();
        duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

        client.getSession().write(TemporaryStatsPacket.giveBuff(combo.getId(), duration, stat, ceffect));
        map.broadcastMessage(this, TemporaryStatsPacket.giveForeignBuff(getId(), stat, ceffect), false);
    }

    public void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp(), this);
        stats.setHp(stats.getHp(), true, this);
    }

    public void enforceMaxHpMp() {
        Map<MapleStat, Integer> statups = new EnumMap<MapleStat, Integer>(MapleStat.class);
        if (stats.getMp() > stats.getCurrentMaxMp()) {
            stats.setMp(stats.getMp(), this);
            statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp(), this);
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if (statups.size() > 0) {
            client.getSession().write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public byte getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public byte getSpawnpoint() {
        return SpawnPoint;
    }

    public void setSpawnpoint(byte i) {
        SpawnPoint = i;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final String getBlessOfFairyOrigin() {
        return this.BlessOfFairy_Origin;
    }

    public final String getBlessOfEmpressOrigin() {
        return this.BlessOfEmpress_Origin;
    }

    public final short getLevel() {
        return level;
    }

    public final int getFame() {
        return fame;
    }

    public final int getFallCounter() {
        return fallcounter;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final void setClient(final MapleClient client) {
        this.client = client;
    }

    public int getExp() {
        return exp;
    }

    public short getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp[GameConstants.getSkillBook(job)]; //default
    }

    public int getRemainingSp(final int skillbook) {
        return remainingSp[skillbook];
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSpSize() {
        int ret = 0;
        for (int i = 0; i < remainingSp.length; i++) {
            if (remainingSp[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public short getHpApUsed() {
        return hpApUsed;
    }

    public void setHidden(boolean f) {
        hide = f;
        client.getSession().write(MaplePacketCreator.GmHide(hide));
        if (hide) {
            map.broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else {
            map.broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
            sendTemporaryStats();

            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    map.broadcastMessage(this, PetPacket.showPet(this, pet, false, false), false);
                }
            }
        }
    }

    boolean hide = false;

    public boolean isVacFucking() {
        if (getQuestNoAdd(MapleQuest.getInstance(170980)) == null) {
            return false;
        }
        return getQuestNAdd(MapleQuest.getInstance(170980)).getCustomData().equals("1");
    }

    public boolean isMovePlayerFucking() {
        if (getQuestNoAdd(MapleQuest.getInstance(170981)) == null) {
            return false;
        }
        return getQuestNAdd(MapleQuest.getInstance(170981)).getCustomData().equals("1");
    }

    public boolean isApplyDamageFucking() {
        if (getQuestNoAdd(MapleQuest.getInstance(170982)) == null) {
            return false;
        }
        return getQuestNAdd(MapleQuest.getInstance(170982)).getCustomData().equals("1");
    }

    public boolean isGSD() {
        if (getQuestNoAdd(MapleQuest.getInstance(170983)) == null) {
            return false;
        }
        return getQuestNAdd(MapleQuest.getInstance(170983)).getCustomData().equals("1");
    }

    public boolean isHidden() {
        return hide;
//        return getBuffSource(MapleBuffStat.DARKSIGHT) / 1000000 == 9;
    }

    public void setHpApUsed(short hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public byte getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(byte skinColor) {
        this.skinColor = skinColor;
    }

    public short getJob() {
        return job;
    }

    public byte getGender() {
        return gender;
    }

    public int getHair() {
        return hair;
    }

    public int getFace() {
        return face;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public void setHair(int hair) {
        this.hair = hair;
        if (getQuestStatus(29020) == 1) {
            MapleQuestStatus quest = getQuestNoAdd(MapleQuest.getInstance(29020));
            if (quest != null) { // 버라이어티 훈장
                int value = 0;
                if (quest.getCustomData() == null) {
                    MapleQuest.getInstance(29020).forceStart(this, 0, String.valueOf(1 + value));
                    return;
                }
                try {
                    value = Integer.parseInt(quest.getCustomData());
                } catch (Exception e) {
                }
                MapleQuest.getInstance(29020).forceStart(this, 0, String.valueOf(1 + value));
            }
        }
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public Point getOldPosition() {
        return old;
    }

    public void setOldPosition(Point x) {
        this.old = x;
    }

    public void setRemainingAp(short remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[GameConstants.getSkillBook(job)] = remainingSp; //default
    }

    public void setRemainingSp(int remainingSp, final int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void setInvincible(boolean invinc) {
        invincible = invinc;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
        if (this.fame >= 10) {
            finishAchievement(7);
        }
        if (this.fame >= 20) {
            finishAchievement(8);
        }
    }

    public void updateFame() {
        updateSingleStat(MapleStat.FAME, this.fame);
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        dropMessage(5, msg);
        final MapleMap map = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map, map.getPortal(portal));
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, MaplePacketCreator.getWarpToMap(to, 0x80, this), null);
    }

    public void changeMap(final MapleMap to) {
        changeMapInternal(to, to.getPortal(0).getPosition(), MaplePacketCreator.getWarpToMap(to, 0, this), to.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId(), this), null);
    }

    public void changeMapPortal(final MapleMap to, final MaplePortal pto, final byte orange) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId() - (byte) orange, this), pto);
    }

    private void changeMapInternal(final MapleMap to, final Point pos, byte[] warpPacket, final MaplePortal pto) {
        if (to == null) {
            return;
        }
        final int nowmapid = map.getId();

        if (to.getId() == 240060200 || getMapId() == 240060201) {
            if (!to.seduceOrder.contains(this)) {
                to.seduceOrder.add(this);
            }
            //System.out.println("seduceOrder" + to.seduceOrder);
        } else if (to.getId() == 702000000 || to.getId() == 500000000 || to.getId() == 683000000 || to.getId() == 740000000 || to.getId() == 701010000 || to.getId() == 701010000) {
            dropMessage(5, "지금은 입장할 수 없는 맵입니다. 다음 패치를 기대해 주세요.");
            return;
        }
        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }
        if (!to.canSoar()) {
            cancelEffectFromBuffStat(MapleBuffStat.SOARING);
        }
        if (map.getId() == nowmapid) {
            client.getSession().write(warpPacket);
            if (isHidden()) {
                client.getSession().write(MaplePacketCreator.GmHide(isHidden()));
            }
            final boolean shouldChange = client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
            final boolean shouldState = map.getId() == to.getId();
            if (shouldChange && shouldState) {
                to.setCheckStates(false);
            }
            MapleMap oldMap = map;
            if (shouldChange) {
                map = to;
                oldMap.removePlayer(this);
                setPosition(pos);
                if (getPets() != null) {
                    for (final MaplePet pets : getPets()) {
                        if (pets.getSummoned()) {
                            pets.setPos(pos);;
                        }
                    }
                }
                setFh(0);
                setStance(0);
                to.addPlayer(this);
                stats.relocHeal(this);
                if (shouldState) {
                    to.setCheckStates(true);
                }
            } else {
                map.removePlayer(this);
            }
        }
        if (pyramidSubway != null) { //checks if they had pyramid before AND after changing
            pyramidSubway.onChangeMap(this, to.getId());
        }
    }

    public void cancelChallenge() {
        if (challenge != 0 && client.getChannelServer() != null) {
            final MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(challenge);
            if (chr != null) {
                chr.dropMessage(6, getName() + " 님이 요청을 거절하였습니다.");
                chr.setChallenge(0);
            }
            dropMessage(6, "요청을 거절하였습니다.");
            challenge = 0;
        }
    }

    public void leaveMap(MapleMap map) {
        controlledLock.writeLock().lock();
        visibleMapObjectsLock.writeLock().lock();
        try {
            for (MapleMonster mons : controlled) {
                if (mons != null) {
                    mons.setController(null);
                    mons.setControllerHasAggro(false);
                    map.updateMonsterController(mons);
                }
            }
            controlled.clear();
            visibleMapObjects.clear();
        } finally {
            controlledLock.writeLock().unlock();
            visibleMapObjectsLock.writeLock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
        cancelChallenge();
        clearLinkMid();
        cancelMapTimeLimitTask();
        if (!getMechDoors().isEmpty()) {
            removeMechDoor(map);
        }
        searchingParty = false;
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
        if (getPlayerShop() != null) {
            if (getPlayerShop().isOwner(this) && getPlayerShop().getShopType() != 1) {
                getPlayerShop().closeShop(false, getPlayerShop().isAvailable(), false); //how to return the items?
            } else {
                final IMaplePlayerShop ips = getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (!game.isOpen()) {
                        game.nextLoser();
                        try {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 0, game.getVisitorSlot(this)));
                        } catch (Exception ex) {
                        }
                        game.setOpen(true);
                        game.update();
                        game.setExitAfter(this);
                        game.checkExitAfterGame();
                    }
                }
                //getPlayerShop().removeVisitor(this, true);
            }
            setPlayerShop(null);
        }
        if (MarriageEventAgent.isWeddingMap(map.getId())) {
            MarriageManager.getInstance().getEventAgent(getClient().getChannel()).checkLeaveMap(this, getMapId());
        }
    }

    public void changeJob(int newJob) {
        try {
            cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
            this.job = (short) newJob;
            updateSingleStat(MapleStat.JOB, newJob);
            if (!GameConstants.isBeginnerJob(newJob)) {
                if (GameConstants.isEvan(newJob) || GameConstants.isResist(newJob) || GameConstants.isMercedes(newJob)) {
                    int changeSp = (newJob == 2200 || newJob == 2210 || newJob == 2211 || newJob == 2213 ? 3 : 5);
                    if (GameConstants.isResist(job) && newJob != 3100 && newJob != 3200 && newJob != 3300 && newJob != 3500) {
                        changeSp = 3;
                    }
                    remainingSp[GameConstants.getSkillBook(newJob)] += changeSp;
                    client.getSession().write(UIPacket.getSPMsg((byte) changeSp, (short) newJob));
                } else {
                    remainingSp[GameConstants.getSkillBook(newJob)]++;
                    if (newJob % 10 >= 2) {
                        remainingSp[GameConstants.getSkillBook(newJob)] += 2;
                    }
                }

                if (newJob % 10 >= 1 && level >= 70) { //3rd job or higher. lucky for evans who get 80, 100, 120, 160 ap...
                    remainingAp += 5;
                    updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
                }
//                if (!isGM()) {
                resetStatsByJob(true);
                if (getLevel() > (newJob == 200 ? 8 : 10) && newJob % 100 == 0) { //first job
                    remainingSp[GameConstants.getSkillBook(newJob)] += 3 * (getLevel() - (newJob == 200 ? 8 : 10));
                } else if (newJob == 2200) {
                    client.getSession().write(MaplePacketCreator.getEvanTutorial("UI/tutorial/evan/14/0"));
                    dropMessage(5, "아기 드래곤이 뭔가 하고 싶은 말이 있는 것 같다. 아기 드래곤을 클릭해 말을 걸어 보자.");
                }
//                }
                client.getSession().write(MaplePacketCreator.updateSp(this, false, false));
                if (job % 100 == 0) { //1차전직
                    if (job == 200 || job == 1200 || job == 2200 || job == 3200) {
                        DueyHandler.addNewItemToDb(2020009, 150, getId(), "[헤라]", "1차전직 지원 아이템입니다.", true);
                        DueyHandler.addNewItemToDb(2000016, 150, getId(), "[헤라]", "1차전직 지원 아이템입니다.", true);
                    } else {
                        DueyHandler.addNewItemToDb(2020007, 150, getId(), "[헤라]", "1차전직 지원 아이템입니다.", true);
                        DueyHandler.addNewItemToDb(2000018, 150, getId(), "[헤라]", "1차전직 지원 아이템입니다.", true);
                    }
                    getClient().sendPacket(MaplePacketCreator.receiveParcel("1차전직 지원", true));

                }
            }

            if (job < 3000) {
                expandInventory((byte) 1, 4);
                expandInventory((byte) 2, 4);
                expandInventory((byte) 3, 4);
                expandInventory((byte) 4, 4);
            }
            int maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

            switch (job) {

                case 100: // Warrior
                case 1100: // Soul Master
                case 2100: // Aran
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3200:
                    maxhp += Randomizer.rand(160, 180);
                    break;
                case 3100:
                case 3300:
                case 3500:
                    maxhp += Randomizer.rand(200, 250);
                    maxmp = 70;
                    break;
                case 3110:
                    maxhp += Randomizer.rand(300, 350);
                    maxmp = 50;
                    break;
                case 3111:
                    maxmp = 100;
                    break;
                case 3112:
                    maxmp = 120;
                    break;
                case 200: // Magician
                case 2200: //evan
                case 2210: //evan
                    maxmp += Randomizer.rand(100, 150);
                    break;
                case 300: // Bowman
                case 400: // Thief
                case 500: // Pirate
                case 2300:
                    maxhp += Randomizer.rand(100, 150);
                    maxmp += Randomizer.rand(25, 50);
                    break;
                case 110: // Fighter
                case 120: // Page
                case 130: // Spearman
                case 1110: // Soul Master
                case 2110: // Aran
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 210: // FP
                case 220: // IL
                case 230: // Cleric
                    maxmp += Randomizer.rand(400, 450);
                    break;
                case 310: // Bowman
                case 320: // Crossbowman
                case 410: // Assasin
                case 420: // Bandit
                case 430: // Semi Dualer
                case 510:
                case 520:
                case 530:
                case 2310:
                case 1310: // Wind Breaker
                case 1410: // Night Walker
                    maxhp += Randomizer.rand(200, 250);
                    maxhp += Randomizer.rand(150, 200);
                    break;
                case 900: // GM
                case 800: // Manager
                    maxhp += Randomizer.rand(200, 250);
                    maxhp += Randomizer.rand(150, 200);
                    break;
            }

            if (maxhp >= 99999) {
                maxhp = 99999;
            }
            if (maxmp >= 99999) {
                maxmp = 99999;
            }
            stats.setInfo(maxhp, maxmp, maxhp, maxmp);
            Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
            statup.put(MapleStat.MAXHP, Integer.valueOf(maxhp));
            statup.put(MapleStat.MAXMP, Integer.valueOf(maxmp));
            statup.put(MapleStat.HP, Integer.valueOf(maxhp));
            statup.put(MapleStat.MP, Integer.valueOf(maxmp));
            stats.recalcLocalStats(this);
            client.getSession().write(MaplePacketCreator.updatePlayerStats(statup, getJob()));
            map.broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 10), false);
            silentPartyUpdate();
            guildUpdate();
            familyUpdate();
            baseSkills();
            if (newJob >= 2200 && newJob <= 2218) { //make new
                if (getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                }
                makeDragon();
            }
            if (newJob >= 3300 && newJob <= 3312) { //make new
                teachSkill(30001061, (byte) 1, (byte) 1);
                teachSkill(30001062, (byte) 1, (byte) 1);
            }
            healMaxHPMP();
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }

    public void makeDragon() {
        dragon = new MapleDragon(this);
        map.broadcastMessage(MaplePacketCreator.spawnDragon(dragon));
    }

    public MapleDragon getDragon() {
        return dragon;
    }

    public boolean checkMasterSkill(int id) {
        switch (id) {
            case 21110007:
            case 21110008:
            case 21120009:
            case 21120010:
                return false;
        }
        return true;
    }

    public final void teachSkill(final int id, final byte level, final byte masterlevel) { //스킬지급 메소드
        changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public void baseSkills() {
        if (GameConstants.getJobNumber(job) >= 3) { //third job.
            List<Integer> skills = SkillFactory.getSkillsByJob(job);
            if (skills != null) {
                for (int i : skills) {
                    final Skill skil = SkillFactory.getSkill(i);
                    if (skil != null && !skil.isInvisible() && skil.isFourthJob() && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0 && skil.getMasterLevel() > 0) {
                        changeSkillLevel(skil, (byte) 0, (byte) skil.getMasterLevel()); //usually 10 master
                    }
                }
            }
            //redemption for completed quests. holy fk. ex
            /*List<MapleQuestStatus> cq = getCompletedQuests();
             for (MapleQuestStatus q : cq) {
             for (MapleQuestAction qs : q.getQuest().getCompleteActs()) {
             if (qs.getType() == MapleQuestActionType.skill) {
             for (Pair<Integer, Pair<Integer, Integer>> skill : qs.getSkills()) {
             final Skill skil = SkillFactory.getSkill(skill.left);
             if (skil != null && getSkillLevel(skil) <= skill.right.left && getMasterLevel(skil) <= skill.right.right) {
             changeSkillLevel(skil, (byte) (int)skill.right.left, (byte) (int)skill.right.right);
             }
             }
             } else if (qs.getType() == MapleQuestActionType.item) { //skillbooks
             for (MapleQuestAction.QuestItem item : qs.getItems()) {
             if (item.itemid / 10000 == 228 && !haveItem(item.itemid,1)) { //skillbook
             //check if we have the skill
             final Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getSkillStats(item.itemid);
             if (skilldata != null) {
             byte i = 0;
             Skill finalSkill = null;
             Integer skillID = 0;
             while (finalSkill == null) {
             skillID = skilldata.get("skillid" + i);
             i++;
             if (skillID == null) {
             break;
             }
             final Skill CurrSkill = SkillFactory.getSkill(skillID);
             if (CurrSkill != null && CurrSkill.canBeLearnedBy(job) && getSkillLevel(CurrSkill) <= 0 && getMasterLevel(CurrSkill) <= 0) {
             finalSkill = CurrSkill;
             }
             }
             if (finalSkill != null) {
             //may as well give the skill
             changeSkillLevel(finalSkill, (byte) 0, (byte)10);
             //MapleInventoryManipulator.addById(client, item.itemid, item.count);
             }
             }
             }
             }
             }
             }
             }*/
        }
    }

    public void gainAp(short ap) {
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[GameConstants.getSkillBook(job)] += sp; //default
        client.getSession().write(MaplePacketCreator.updateSp(this, false));
        client.getSession().write(UIPacket.getSPMsg((byte) sp, (short) job));
    }

    public void gainSP(int sp, final int skillbook) {
        this.remainingSp[skillbook] += sp; //default
        client.getSession().write(MaplePacketCreator.updateSp(this, false));
        client.getSession().write(UIPacket.getSPMsg((byte) sp, (short) 0));
    }

    public void resetSP(int sp) {
        for (int i = 0; i < remainingSp.length; i++) {
            this.remainingSp[i] = sp;
        }
        client.getSession().write(MaplePacketCreator.updateSp(this, false));
    }

    public void resetAPSP() {
        resetSP(0);
        gainAp((short) -this.remainingAp);
    }

    public void changeSkillLevel(final Skill skill, int newLevel, byte newMasterlevel) { //1 month
        if (skill == null) {
            return;
        }
        changeSkillLevel(skill, newLevel, newMasterlevel, skill.isTimeLimited() ? (System.currentTimeMillis() + (long) (30L * 24L * 60L * 60L * 1000L)) : -1);
    }

    public void changeSkillLevel(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        if (skill == null || (!GameConstants.isApplicableSkill(skill.getId()) && !GameConstants.isApplicableSkill_(skill.getId()))) {
            return;
        }
        client.getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, expiration));
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return; //nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
        }
        changed_skills = true;
        if (GameConstants.isRecoveryIncSkill(skill.getId())) {
            stats.relocHeal(this);
        }
        if (skill.getId() < 80000000) {
            stats.recalcLocalStats(this);
        }
    }

    public void changeSkillLevel_Skip(final Skill skill, int newLevel, byte newMasterlevel) {
        changeSkillLevel_Skip(skill, newLevel, newMasterlevel, false);
    }

    public void changeSkillLevel_Skip(final Skill skill, int newLevel, byte newMasterlevel, boolean write) {
        if (skill == null) {
            return;
        }
        if (write) {
            client.getSession().write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, -1L));
        }
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return; //nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, -1L));
        }

    }

    public void playerDead() {
        if (getMapId() == 240060200 || getMapId() == 240060201) {
            if (getMap().seduceOrder.get(0) == this) {
                getMap().seduceOrder.remove(0);
            }
        }
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        cancelAllBuffs();
        dispelDebuffs();
        dispelSummons();
        checkFollow();
        dotHP = 0;
        lastDOTTime = 0;

        boolean reduceDeathExp = true;
        if (getMapId() / 1000000 == 109) { // 이벤트맵
            reduceDeathExp = false;
        } else if (getMapId() / 10000000 == 98) { // 몬스터 카니발
            reduceDeathExp = false;
        } else if (getMapId() == 270020211) { // 마법제련술사의 방
            reduceDeathExp = false;
        } else if (getMapId() / 1000000 == 925) { //무릉도장
            reduceDeathExp = false;
        } else if (getPyramidSubway() != null) {
            reduceDeathExp = false;
        }

        if (!GameConstants.isBeginnerJob(job) && reduceDeathExp) {
            int charms = getItemQuantity(5130000, false);
            if (charms > 0) {
                int days = getRemainingExpirationDay(5130000);
                MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5130000, 1, true, false);

                charms--;
                if (charms > 0xFF) {
                    charms = 0xFF;
                }
                client.getSession().write(CSPacket.useCharm((byte) charms, (byte) days));
            } else {
                float diepercentage = 0.0f;
                int expforlevel = getNeededExp();
                if (map.isTown() || FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
                    diepercentage = 0.01f;
                } else {
                    float decRate;
                    if (getJob() / 100 == 3) {
                        decRate = 0.08F;
                    } else {
                        decRate = 0.2F;
                    }
                    diepercentage = decRate / getStat().luk + 0.05F;
                }

                float myExpF = getExp() - expforlevel * diepercentage;
                if (myExpF <= 0.0F) {
                    myExpF = 0.0F;
                }
                float ff = expforlevel - 1;
                if (myExpF < ff - 1.0F) {
                    ff = myExpF;
                }
                this.exp = (int) ff;
            }
            this.updateSingleStat(MapleStat.EXP, this.exp);
        }
        if (reduceDeathExp) {
            getStat().checkEquipDurabilitys(this, -1000, false, true);
        }
        if (getPyramidSubway() != null) {
            getPyramidSubway().fail(this);
            stats.setHp((short) 50, this);
        }
    }

    public void updatePartyMemberHP() {
        if (party != null && client.getChannelServer() != null) {
            final int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    final MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().getSession().write(MaplePacketCreator.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party == null) {
            return;
        }
        int channel = client.getChannel();
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                if (other != null) {
                    client.getSession().write(MaplePacketCreator.updatePartyMemberHP(other.getId(), other.getStat().getHp(), other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    public void healHP(int delta) {
        addHP(delta);
        client.getSession().write(MaplePacketCreator.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, MaplePacketCreator.showHpHealed(getId(), delta), false);
    }

    public void healMP(int delta) {
        addMP(delta);
        client.getSession().write(MaplePacketCreator.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, MaplePacketCreator.showHpHealed(getId(), delta), false);
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @param delta
     * @see MapleCharacter#setHp(int)
     */
    public void addHP(int delta) {
        if (stats.setHp(stats.getHp() + delta, this)) {
            updateSingleStat(MapleStat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @param delta
     * @see MapleCharacter#setMp(int)
     */
    public void addMP(int delta) {
        if (stats.setMp(stats.getMp() + delta, this)) {
            updateSingleStat(MapleStat.MP, stats.getMp());
        }
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        Map<MapleStat, Integer> statups = new EnumMap<MapleStat, Integer>(MapleStat.class);

        if (stats.setHp(stats.getHp() + hpDiff, this)) {
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if (stats.setMp(stats.getMp() + mpDiff, this)) {
            statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
        }
        if (statups.size() > 0) {
            client.getSession().write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
        }
        //dropMessage(6, "stats.getHp(): " + stats.getHp() + " hpDiff: " + hpDiff + " result: " + (stats.getHp() + hpDiff));
    }

    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        if (stat == MapleStat.AVAILABLESP) {
            client.getSession().write(MaplePacketCreator.updateSp(this, itemReaction, false));
            return;
        }
        Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
        statup.put(stat, newval);
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statup, itemReaction, getJob()));
    }

    public void gainExp(int total, final boolean show, final boolean inChat, final boolean white) {
        try {
            int prevexp = getExp();
            int needed = getNeededExp();
            if (total > 0) {
                stats.checkEquipLevels(this, total); //gms like
            }
            if ((level == 250 || (GameConstants.isKOC(job) && level >= 120))) {
                setExp(0);
                //if (exp + total > needed) {
                //    setExp(needed);
                //} else {
                //    exp += total;
                //}
            } else {
                boolean leveled = false;
                long tot = exp + total;
                if (tot >= needed) {
                    exp += total;
                    levelUp();
                    leveled = true;
                    if ((level == 250 || (GameConstants.isKOC(job) && level >= 120))) {
                        setExp(0);
                    } else {
                        needed = getNeededExp();
                        if (exp >= needed) {
                            setExp(needed - 1);
                        }
                    }
                } else {
                    exp += total;
                }
                if (total > 0) {
                    familyRep(prevexp, needed, leveled);
                }
            }
            if (total != 0) {
                if (exp < 0) { // After adding, and negative
                    if (total > 0) {
                        setExp(needed);
                    } else if (total < 0) {
                        setExp(0);
                    }
                }
                updateSingleStat(MapleStat.EXP, getExp());
                if (show) { // still show the expgain even if it's not there
                    if (inChat) {
                        client.getSession().write(GainExpPacket.GainExp_Quest(total));
                    } else {
                        client.getSession().write(MaplePacketCreator.GainExpPacket.GainExp_Monster(total, white, 0, 0, 0, 0, 0, 0, 0));
                    }
                }
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }

    public void familyRep(int prevexp, int needed, boolean leveled) {
        if (mfc != null) {
            int onepercent = needed / 100;
            if (onepercent <= 0) {
                return;
            }
            int percentrep = (getExp() / onepercent - prevexp / onepercent);
            if (leveled) {
                percentrep = 100 - percentrep + (level / 2);
            }
            if (percentrep > 0) {
                int sensen = World.Family.setRep(mfc.getFamilyId(), mfc.getSeniorId(), percentrep * 10, level, name);
                if (sensen > 0) {
                    World.Family.setRep(mfc.getFamilyId(), sensen, percentrep * 5, level, name); //and we stop here
                }
            }
        }
    }

    public void gainExpMonster(int rawexp, int gain, final boolean show, final boolean white, final byte pty, boolean partyBonusMob, final int partyBonusRate) {
        //gain *= GameConstants.getExpModByLevel(level);
        final int trueExpRate = RateManager.getTrueExpByLevel(getLevel());
        int total = gain;
        int partyinc = 0;
        int partyBonusR = 0;
        int prevexp = getExp();
        int equip = 0;
        int weddinginc = 0;
        int eventinc = 0;
        int extrabonus = 0;
        int 맵보너스 = 0;
        int 피시보너스 = 0;
        boolean isExpBonusMap = false;
        boolean isPenaltyMap = false;
        if (getMap().getId() == 103000800) {
            isPenaltyMap = true;
        }
        /*배율 합공식 적용*/
        //기존 exp에다가 합공식으로 rawexp를 얹어주는 방식
        if (!ServerConstants.ExprateByLevel) {
            if (RateManager.EXP == 1) {
                gain += rawexp;//클라 표기
                total += rawexp;
            } else if (RateManager.EXP > 1) {
                gain += rawexp * trueExpRate * (RateManager.EXP - 1);  
                total += rawexp * trueExpRate * (RateManager.EXP - 1);  
            }
        } else {
            int exprate = trueExpRate;
            if (getClient().burning) {
                exprate += RateManager.BURNING;
            }
            gain += rawexp * (exprate - 1);
            total += rawexp * (exprate - 1);
        }

        MapleParty party = getParty();
        if (pty > 1 && party != null) {
            final double rate = (partyBonusRate > 0 ? (partyBonusRate / 100.0) : (map == null || !partyBonusMob || map.getPartyBonusRate() <= 0 ? 0.05 : (map.getPartyBonusRate() / 100.0)));
            partyinc = (int) (((float) (rawexp * trueExpRate * rate)) * (pty + (rate > 0.05 ? -1 : 1)));  
            partyBonusR = partyinc * 100 / rawexp;
            partyinc = rawexp * partyBonusR / 100;
            if (partyinc < 1) {
                partyinc = 1;
            }
            partyinc *= 1;//파티 보너스 두배
            total += partyinc;
            if (marriageId != 0) {
                MarriageDataEntry data = MarriageManager.getInstance().getMarriage(marriageId);
                if (data != null) {
                    int spouseId = id == data.getBrideId() ? data.getGroomId() : data.getBrideId();
                    MapleCharacter spouse = map.getCharacterById(spouseId);
                    if (spouse != null && party.equals(spouse.getParty())) {
                        weddinginc = rawexp * trueExpRate * 10 / 100;  
                        if (weddinginc < 1) {
                            weddinginc = 1;
                        }
                        total += weddinginc;
                    }
                }
            }
        }
        if (isExpBonusMap) {
            맵보너스 = (int) (gain * 1);
            if (맵보너스 < 1) {
                맵보너스 = 1;
            }
            total += 맵보너스;
        }
        if (isPenaltyMap) {
            gain /= 2;
            total /= 2;
        }

        if (checkPcTime()) {//PC방
            if (getPcDate() == GameConstants.getCurrentDate_NoTime()) {
                피시보너스 = (int) (rawexp * trueExpRate * 10 / 100);  
                if (피시보너스 < 1) {
                    피시보너스 = 1;
                }
                total += 피시보너스;
            }
        } else {
            if (pcbang) {
                dropMessage(5, "PC방 정량제 잔여시간이 남아있지 않아 PC방 혜택 장비 아이템들이 사라집니다.");
                removePCitem();
                getClient().getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, getCalcPcTime()));
                pcbang = false;
            }
        }
        mobKilledNo++; // Reset back to 0 when cc
        short percentage = 0;
        double hoursFromLogin = ((System.currentTimeMillis() - firstLoginTime) / (double) (1000 * 60 * 60));
        int eventBonus = 0;//이벤트 보너스
        int time = 0;
        if (mobKilledNo == 3 && ServerConstants.Event_Bonus) { //이벤트 보너스, 반올림 Math.round()
            //dropMessage(6, hoursFromLogin + "ㅇㅇ" + System.currentTimeMillis());
            if (hoursFromLogin >= 1 && hoursFromLogin < 2) {
                time = 1;
                percentage = 10;
            } else if (hoursFromLogin >= 2 && hoursFromLogin < 3) {
                time = 2;
                percentage = 20;
            } else if (hoursFromLogin >= 3 && hoursFromLogin < 4) {
                time = 3;
                percentage = 30;
            } else if (hoursFromLogin >= 4) {
                time = 4;
                percentage = 40;
            }
            eventBonus = (int) ((float) ((rawexp * trueExpRate * percentage / 100)));  
            mobKilledNo = 0;
            if (eventBonus < 1) {
                eventBonus = 1;
            }
            total += eventBonus;
        }
        if (bonusExpR > 0) {
            long l = Math.min((long) rawexp * trueExpRate * bonusExpR / 100, Integer.MAX_VALUE);  
            equip = (int) l;
            if (equip < 1) {
                equip = 1;
            }
            total += equip;
        }
        if (stats.eventExpRate != 0) {
            eventinc = (int) (rawexp * trueExpRate * stats.eventExpRate);  
            if (eventinc < 1) {
                eventinc = 1;
            }
            total += eventinc;
        }
        if (gain > 0 && total < gain) { //just in case
            total = Integer.MAX_VALUE;
        }
        int needed = getNeededExp();
        if (total > 0) {
            stats.checkEquipLevels(this, total); //gms like
        }
        if ((level == 250 || (GameConstants.isKOC(job) && level >= 120))) {
            setExp(0);
            //if (exp + total > needed) {
            //    setExp(needed);
            //} else {
            //    exp += total;
            //}
        } else {
            boolean leveled = false;
            if (exp + total >= needed || exp >= needed) {
                exp += total;
                levelUp();
                leveled = true;
                if ((level == 250 || (GameConstants.isKOC(job) && level >= 120))) {
                    setExp(0);
                } else {
                    needed = getNeededExp();
                    if (exp >= needed) {
                        setExp(needed);
                    }
                }
            } else {
                exp += total;
            }
            if (total > 0) {
                if (getQuestStatus(9875) == 1) { // 화분(새싹)
                    MapleQuestStatus q = getQuestNoAdd(MapleQuest.getInstance(9876));
                    if (q.getCustomData() == null) {
                        q.setCustomData("0");
                    }
                    int qc = 0;
                    try {
                        qc = Integer.parseInt(q.getCustomData());
                    } catch (Exception e) {
                        q.setCustomData("0");
                        qc = 0;
                    }
                    MapleQuest.getInstance(9876).forceStart(this, 0, "" + (qc + total));
                    //MapleQuest.getInstance(9876).forceStart(this, 0, "" + (qc + total));
                    //dropMessage(6, "qc + total*10 : " + (q.getCustomData()));
                }
                if (getQuestStatus(20514) == 1) { // 티티아나 알
                    MapleQuestStatus q = getQuestNoAdd(MapleQuest.getInstance(20514));
                    if (q.getCustomData() == null) {
                        q.setCustomData("0");
                    }
                    int qc = 0;
                    try {
                        qc = Integer.parseInt(q.getCustomData());
                    } catch (Exception e) {
                        q.setCustomData("0");
                        qc = 0;
                    }
                    MapleQuest.getInstance(20514).forceStart(this, 0, "" + Math.min(qc + total, 180000 * 3));
                }
                if (getQuestStatus(23961) == 1) {
                    MapleQuestStatus q = getQuestNoAdd(MapleQuest.getInstance(23980));
                    if (q.getCustomData() == null) {
                        q.setCustomData("0");
                    }
                    int qc = 0;
                    try {
                        qc = Integer.parseInt(q.getCustomData());
                    } catch (Exception e) {
                        q.setCustomData("0");
                        qc = 0;
                    }
                    //System.out.println("qc" + qc);
                    MapleQuest.getInstance(23980).forceStart(this, 0, "" + (qc + total));
                    //MapleQuest.getInstance(9876).forceStart(this, 0, "" + (qc + total));
                    //dropMessage(6, "qc + total*10 : " + (q.getCustomData()));
                }
                if (getQuestStatus(23968) == 1) {
                    MapleQuestStatus q = getQuestNoAdd(MapleQuest.getInstance(23981));
                    if (q.getCustomData() == null) {
                        q.setCustomData("0");
                    }
                    int qc = 0;
                    try {
                        qc = Integer.parseInt(q.getCustomData());
                    } catch (Exception e) {
                        q.setCustomData("0");
                        qc = 0;
                    }
                    MapleQuest.getInstance(23981).forceStart(this, 0, "" + (qc + total));
                    //MapleQuest.getInstance(9876).forceStart(this, 0, "" + (qc + total));
                    //dropMessage(6, "qc + total*10 : " + (q.getCustomData()));
                }
                familyRep(prevexp, needed, leveled);
            }
        }
        if (gain != 0) {
            if (exp < 0) { // After adding, and negative
                if (gain > 0) {
                    setExp(getNeededExp());
                } else if (gain < 0) {
                    setExp(0);
                }
            }
            updateSingleStat(MapleStat.EXP, getExp());
            if (show) { // still show the expgain even if it's not there
                this.client.getSession().write(MaplePacketCreator.GainExpPacket.GainExp_Monster(gain + 맵보너스, white, partyinc, eventinc, percentage, weddinginc, equip, time, 피시보너스));
            }
        }
        //dropMessage(6, "isExpBonusMap : " + isExpBonusMap);
        //dropMessage(6, "게인 : " + gain + " 토탈 : " + total + " 파티보너스R : " + partyBonusR + " 파티사이즈 : " + pty + " 파티inc : " + partyinc + " 맵보너스: " + 맵보너스 + " 피시보너스 : " + 피시보너스 + " exp : " + exp);
    }

    public void openNpc(int id) {
        NPCScriptManager.getInstance().start(getClient(), id);
    }

    public void forceReAddItem_NoUpdate(Item item, MapleInventoryType type) {
        getInventory(type).removeSlot(item.getPosition());
        getInventory(type).addFromDB(item);
    }

    public void forceReAddItem(Item item, MapleInventoryType type) { //used for stuff like durability, item exp/level, probably owner?
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this));
        }
    }

    public void forceReAddItem_Flag(Item item, MapleInventoryType type) { //used for flags
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(MaplePacketCreator.updateSpecialItemUse_(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this, false));
        }
    }

    public void forceReAddItem_Book(Item item, MapleInventoryType type) { //used for mbook
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(MaplePacketCreator.upgradeBook(item, this));
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            World.Party.updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(this));
        }
    }

    public boolean isSuperGM() {
        return gmLevel >= PlayerGMRank.SUPERGM.getLevel();
    }

    public boolean isIntern() {
        return gmLevel >= PlayerGMRank.INTERN.getLevel();
    }

    public boolean isGM() {
        return gmLevel >= PlayerGMRank.GM.getLevel();
    }

    public boolean isAdmin() {
        return gmLevel >= PlayerGMRank.ADMIN.getLevel();
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public void setGMLevel(int gmLevel) {
        this.gmLevel = (byte) gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public final MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public final MapleInventory[] getInventorys() {
        return inventory;
    }

    public final void expirationTask(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    client.getSession().write(CSPacket.itemExpired(z.intValue(), ii.isCash(z.intValue())));//, ii.isCash(z.intValue())));
                    if (!firstLoad) {
                        if (z.intValue() / 10000 == 521) {
                            stats.recalcLocalStats(this);
                        }
                        if (z.intValue() / 10000 == 410) {
                            stats.recalcLocalStats(this);
                        }
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z.intValue());
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessage(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
            if (pendingSkills != null) {
                for (Integer z : pendingSkills) {
                    client.getSession().write(MaplePacketCreator.updateSkill(z, 0, 0, -1));
                    client.getSession().write(MaplePacketCreator.serverNotice(5, "스킬 [" + SkillFactory.getSkillName(z) + "]의 유효기간이 끝나 사라집니다."));
                }
            } //not real msg
            pendingSkills = null;
            return;
        }
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        long expiration;
        final List<Integer> ret = new ArrayList<Integer>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<Triple<MapleInventoryType, Item, Boolean>>(); // This is here to prevent deadlock.
        final List<Item> tobeunlock = new ArrayList<Item>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !GameConstants.isPet(item.getItemId()) && currenttime > expiration) || (firstLoad && ii.isLogoutExpire(item.getItemId()))) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                    }
                } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) {
                    toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                } else if (item.getPosition() == -59) {
                    if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, true));
                    }
                }
            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item.getItemId())).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem = null;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP) {
                        theNewItem = ii.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, (byte) 0);
                    }
                    theNewItem.setGMLog("replace로 생성 된 아이템 주인: " + getName());
                    MapleInventoryManipulator.addFromDrop(getClient(), theNewItem, false);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag((byte) (itemz.getFlag() - ItemFlag.LOCK.getValue()));
        }
        this.pendingExpiration = ret;

        final List<Integer> skilz = new ArrayList<Integer>();
        final List<Skill> toberem = new ArrayList<Skill>();
        for (Entry<Skill, SkillEntry> skil : skills.entrySet()) {
            if (skil.getValue().expiration != -1 && currenttime > skil.getValue().expiration) {
                toberem.add(skil.getKey());
            }
        }
        for (Skill skil : toberem) {
            skilz.add(skil.getId());
            this.skills.remove(skil);
            changed_skills = true;
        }
        this.pendingSkills = skilz;
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) < currenttime) { //expired bro
            quests.remove(MapleQuest.getInstance(7830));
            quests.remove(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        }
    }

    public final void expirationTask2(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        long expiration;
        final List<Integer> ret = new ArrayList<Integer>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<Triple<MapleInventoryType, Item, Boolean>>(); // This is here to prevent deadlock.
        final List<Item> tobeunlock = new ArrayList<Item>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !GameConstants.isPet(item.getItemId()) && currenttime > expiration) || (firstLoad && ii.isLogoutExpire(item.getItemId()))) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                    }
                } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) {
                    toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                } else if (item.getPosition() == -30) {
                    if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, true));
                    }
                }/* else if (expiration != -1 &&item.getItemId() == 4001432 && getMapId() / 1000 == 950100) {
                 warp(950100000);
                 dropMessage(6,"프리미엄 티켓의 시간이 끝나 바깥으로 이동합니다");
                 }*/

            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.left == MapleInventoryType.EQUIPPED) {
                getClient().sendPacket(MaplePacketCreator.dropInventoryItem(MapleInventoryType.EQUIP, item.getPosition()));
            } else {
                client.getSession().write(MaplePacketCreator.clearInventoryItem(itemz.getLeft(), item.getPosition(), false));
            }
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item.getItemId())).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem = null;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP) {
                        theNewItem = ii.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, (byte) 0);
                    }
                    theNewItem.setGMLog("replace로 생성 된 아이템 주인: " + getName());
                    MapleInventoryManipulator.addFromDrop(getClient(), theNewItem, false);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag((byte) (itemz.getFlag() - ItemFlag.LOCK.getValue()));
        }
        this.pendingExpiration = ret;
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    client.getSession().write(CSPacket.itemExpired(z.intValue(), ii.isCash(z.intValue())));
                    equipChanged();
                    if (!firstLoad) {
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z.intValue());
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessage(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
        }
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public int getMeso() {
        return meso;
    }

    public final int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        if (savedLocations[type.getValue()] == -1) {
            switch (getMapId()) {
                case 240080000:
                    return 240030102;
                case 211000002:
                    return 211000001;
                case 300030100:
                    return 300030000;
                case 200080101:
                    return 200080100;
                case 221023300:
                    return 221023200;
                case 251010404:
                    return 251010401;
                case 261000011:
                    return 261000010;
                case 261000021:
                    return 261000020;
                case 200000301:
                    return 200000300;
                default:
                    return -1;
            }
        } else {
            return savedLocations[type.getValue()];
        }
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = getMapId();
        changed_savedlocations = true;
    }

    public void saveLocation(SavedLocationType type, int mapz) {
        savedLocations[type.getValue()] = mapz;
        changed_savedlocations = true;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = -1;
        changed_savedlocations = true;
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false);
    }

    public void gainMeso(int gain, boolean show, boolean inChat) {
        gainMeso(gain, show, inChat, false);
    }

    public void gainMeso(int gain, boolean show, boolean inChat, boolean hangDisable) {
        if (meso + gain < 0) {
            client.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        meso += gain;
//        if (meso >= 1000000) {
//            finishAchievement(31);
//        }
//        if (meso >= 10000000) {
//            finishAchievement(32);
//        }
//        if (meso >= 100000000) {
//            finishAchievement(33);
//        }
//        if (meso >= 1000000000) {
//            finishAchievement(34);
//        }
        updateSingleStat(MapleStat.MESO, meso, hangDisable);
//        client.getSession().write(MaplePacketCreator.enableActions());
        if (show) {
            client.getSession().write(MaplePacketCreator.showMesoGain(gain, inChat));
        }
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        if (monster == null) {
            return;
        }
        monster.setController(this);
        controlledLock.writeLock().lock();
        try {
            controlled.add(monster);
        } finally {
            controlledLock.writeLock().unlock();
        }
        if (!isGSD()) {
            client.getSession().write(MobPacket.controlMonster(monster, false, aggro));
        }
        monster.sendStatus(client);
    }

    public void stopControllingMonster(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        controlledLock.writeLock().lock();
        try {
            if (controlled.contains(monster)) {
                controlled.remove(monster);
            }
        } finally {
            controlledLock.writeLock().unlock();
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        if (monster.getController() == this) {
            monster.setControllerHasAggro(true);
        } else {
            monster.switchController(this, true);
        }
    }

    public int getControlledSize() {
        return controlled.size();
    }

    public int getAccountID() {
        return accountid;
    }

    public void mobKilled(final int id, final int skillID) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() != 1 || !q.hasMobKills()) {
                continue;
            }
            if (q.mobKilled(id, skillID)) {
                client.getSession().write(MaplePacketCreator.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    public void showQuestCompletion(int qid) {
        client.getSession().write(MaplePacketCreator.getShowQuestCompletion(qid));
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<Pair<Integer, Long>> getCompletedMedals() {
        List<Pair<Integer, Long>> ret = new ArrayList<Pair<Integer, Long>>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked() && q.getQuest().getMedalItem() > 0 && GameConstants.getInventoryType(q.getQuest().getMedalItem()) == MapleInventoryType.EQUIP) {
                ret.add(new Pair<Integer, Long>(q.getQuest().getId(), q.getCompletionTime()));
            }
        }
        return ret;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getTotalSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        int toallevel = ret.skillevel;
        if (stats.incAllskill != 0) {
            toallevel += (skill.isBeginnerSkill() ? 0 : stats.incAllskill);
            if (ret.skillevel >= skill.getMaxLevel()) {
                toallevel = skill.getMaxLevel();
            }
        }
        if (stats.getSkillIncrement(skill.getId()) != 0) {
            toallevel += stats.getSkillIncrement(skill.getId());
            if (ret.skillevel >= skill.getMaxLevel()) {
                toallevel = skill.getMaxLevel();
            }
        }
        if (stats.combatOrders != 0) {
            toallevel += stats.combatOrders;
            if (toallevel >= skill.getMaxLevel()) {
                if (!skill.combatOrders()) {
                    toallevel = skill.getMaxLevel();
                }
            }
        }
        return toallevel;
    }

    public int getAllSkillLevels() {
        int rett = 0;
        for (Entry<Skill, SkillEntry> ret : skills.entrySet()) {
            if (!ret.getKey().isBeginnerSkill() && !ret.getKey().isSpecialSkill() && ret.getValue().skillevel > 0) {
                rett += ret.getValue().skillevel;
            }
        }
        return rett;
    }

    public long getSkillExpiry(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.expiration;
    }

    public int getSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.skillevel;
    }

    public byte getMasterLevel(final int skill) {
        return getMasterLevel(SkillFactory.getSkill(skill));
    }

    public byte getMasterLevel(final Skill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public void levelUp() {
        if (level == 49) {
            MapleQuest.getInstance(100200).forceStart(this, 0, "0");
        }
        if (GameConstants.isKOC(job)) {
            if (level <= 70) {
                remainingAp += 6;
            } else {
                remainingAp += 5;
            }
        } else {
            remainingAp += 5;
        }
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(24 + 40, 28 + 40);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(70, 82);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(42, 44);
            maxmp += Randomizer.rand(68, 75);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if (job >= 3300 && job <= 3312) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(44, 48);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(70, 82);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);
            maxmp += Randomizer.rand(50, 100);
        }
        //maxmp += stats.getTotalInt() / 10;

//        if ((getTotalSkillLevel(1000001) > 0)) {
//            getStat().maxhp += SkillFactory.getSkill(1000001).getEffect(getTotalSkillLevel(1000001)).getX();
//        }
//        if ((getTotalSkillLevel(2000001) > 0)) {
//            getStat().maxmp += SkillFactory.getSkill(2000001).getEffect(getTotalSkillLevel(2000001)).getX();
//        }
        exp -= getNeededExp(); // 길드 자동가입
        level += 1;
        ServerLogger.getInstance().logLevelUp(LogType.Etc.LevelUp, "이름: " + getName() + " / " + level + "로 레벨업 / 시간 : " + new java.util.Date().toString(), getAccountID());
        if (level == 2) {
            addGuildMember(1); // 길드 자동가입 길드 ID, 괄호에 길드의 값을 써 주세요
            if (this.getGuildId() != 0) {
                dropMessage(5, "길드에 가입했습니다.");
            } else {
                dropMessage(5, "길드가 가득찼습니다. 운영자에게 문의해 주시기 바랍니다.");
            }
        }

        if (level == 10) {
            getClient().getSession().write(MaplePacketCreator.receiveParcel("[헤라]", true));
            DueyHandler.addNewItemToDb(1122155, 1, getId(), "[헤라]", "정령의 펜던트 지급", true);
        }

        if (level == 100) {
            dropMessage(5, "100 레벨 달성을 축하드립니다. @이동기 명령어를 입력하셔서 Shift 단축키에 플래시 점프와 강화된 텔레포트를 받아가세요!");
            dropMessage(5, "※도적 직업군은 획득이 불가능합니다.");
        }

        if (level == 120) {
            getClient().getSession().write(MaplePacketCreator.receiveParcel("[카르타]", true));
            DueyHandler.addNewItemToDb(2280002, 1, getId(), "[카르타]", "마시면 용사의 의지를 배울 수 있는 포션이야. 따로 내게 찾아오지 않아도 된단다.", true);
        }

        if (level == 200) {
            levelUp();
            dropMessage(5, "200 레벨 달성을 축하드립니다. 이제부터 201 레벨으로 시작합니다.");
        }

        if (level == 200 && getJob() == 2218) {
            final int skillId = 20011005;
            final byte skillLevel = 1;
            final byte masterLevel = 1;

            Skill skill = SkillFactory.getSkill(skillId);
            if (skill != null) {
                int curLevel = getSkillLevel(skill);
                if (curLevel <= 0) {
                    changeSkillLevel(skill, skillLevel, masterLevel);
                    dropMessage(5, "200 레벨 달성 보상으로 스킬을 습득했습니다!");
                }
            } else {
                dropMessage(5, "스킬 지급 오류: 스킬 정보를 찾지 못했습니다. (ID: " + skillId + ")");
            }
        }

        if (level == 250) {
            if (!isGM()) {
                final StringBuilder sb = new StringBuilder("[축하] ");
                final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append("님이 레벨 250을 달성했습니다. 모두 축하해 주세요.");
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()));
                int map = 0;
                switch (getJob() / 100) {
                    case 1:
                        map = 102000004; //전사의 전당(1) 8명
                        break;
                    case 2:
                        map = 101000004; //마법사의 전당(1) 8명
                        break;
                    case 3:
                        map = 100000205; //궁수의 전당
                        break;
                    case 4:
                        map = 103000009; //도적의 전당
                        break;
                    case 5:
                        map = 120000105; //해적의 전당
                        break;
                    case 21:
                        map = 140010110; //기사의 전각
                        break;
                    case 22:
                        map = 100030301; //울창한 전당
                        break;
                }
                int npc = 9901000;
                npc += ((getJob() / 100) - 1) * 100;
                if (getJob() / 100 == 1) {
                    npc += 1;
                } else if (getJob() / 100 == 3) {
                    npc += 520;
                } else if (getJob() / 100 == 4) {
                    npc += 430;
                } else if (getJob() / 100 == 5) {
                    npc += 340;
                } else if (getJob() / 100 == 21) {
                    npc -= 1400;
                } else if (getJob() / 100 == 22) {
                    npc -= 1190;
                }
                MapleMap nmap = getClient().getChannelServer().getMapFactory().getMap(map);
                int count = 0;
                for (PlayerNPC pnpc : getClient().getChannelServer().getAllPlayerNPC()) {
                    if (pnpc.getMapId() == map) {
                        ++count;
                    }
                }
                if (count < 10) {
                    npc += count;
                    MapleNPC npctemplate = nmap.getNPCById(npc);
                    if (null != npctemplate) {
                        PlayerNPC newpnpc = new PlayerNPC(this, npc, nmap, npctemplate.getTruePosition().x, npctemplate.getTruePosition().y, npctemplate.getF(), npctemplate.getFh());
                        newpnpc.addToServer();
                        PlayerNPC.sendBroadcastModifiedNPC(this, nmap, npc, false);
                        String mapString = "";
                        switch (getJob() / 100) {
                            case 1:
                                mapString = "전사의 전당";
                                break;
                            case 2:
                                mapString = "마법사의 전당";
                                break;
                            case 3:
                                mapString = "궁수의 전당";
                                break;
                            case 4:
                                mapString = "도적의 전당";
                                break;
                            case 5:
                                mapString = "해적의 전당";
                                break;
                            case 21:
                                mapString = "달인의 전각";
                                break;
                            case 22:
                                mapString = "울창한 전당";
                                break;
                        }
                        dropMessage(1, "만렙 축하의 의미로 자신의 NPC가 " + mapString + "에 생겨났습니다!");
                        dropMessage(5, "만렙 축하의 의미로 자신의 NPC가 " + mapString + "에 생겨났습니다!");
                    }
                }
            }
        }

        if (level >= 30) {
            finishAchievement(2);
        }
        if (level >= 70) {
            finishAchievement(3);
        }
        if (level >= 120) {
            finishAchievement(4);
        }
        if (level >= 200) {
            finishAchievement(5);
        }

        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));

        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);

        if (!GameConstants.isBeginnerJob(job)) { // Not Beginner, Nobless and Legend
            if (GameConstants.isResist(this.job) || GameConstants.isMercedes(this.job)) {//레지스탕스 스킬포인트
                remainingSp[GameConstants.getSkillBook(this.job, this.level)] += 3;
            } else {
                remainingSp[GameConstants.getSkillBook(this.job)] += 3;
            }
            client.getSession().write(MaplePacketCreator.updateSp(this, false));
        } else {
            if (level <= 10) { //초보자는 올힘
                stats.str += remainingAp;
                remainingAp = 0;
                statup.put(MapleStat.STR, (int) stats.getStr());
            } else if (level == 11) {
                resetStats(4, 4, 4, 4);
            }
        }
        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statup, getJob()));
        map.broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 0), false);
        stats.recalcLocalStats(this);
        healMaxHPMP();
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
    }

    public void healMaxHPMP() {
        Map<MapleStat, Integer> statups = new EnumMap<>(MapleStat.class);
        stats.setHp(stats.getCurrentMaxHp(), this);
        statups.put(MapleStat.HP, stats.getCurrentMaxHp());
        stats.setMp(stats.getCurrentMaxMp(), this);
        statups.put(MapleStat.MP, stats.getCurrentMaxMp());
        client.getSession().write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
    }

    public void familyUpdate() {
        if (mfc == null) {
            return;
        }
        World.Family.memberFamilyUpdate(mfc, this);
    }

    public void changeKeybinding(int key, byte type, int action) {
        if (type != 0) {
            keylayout.Layout().put(Integer.valueOf(key), new Pair<Byte, Integer>(type, action));
        } else {
            keylayout.Layout().remove(Integer.valueOf(key));
        }
    }

    public void sendMacros() {
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] == null) {
                skillMacros[i] = new SkillMacro(0, 0, 0, "", 0, i);
            }
        }
        client.getSession().write(MaplePacketCreator.getMacros(skillMacros));
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        changed_skillmacros = true;
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }

    public void tempban(String reason, Calendar duration, int greason, boolean IPMac, String banby) {
        if (IPMac) {
            client.banHwID();
        }
//        client.getSession().write(MaplePacketCreator.GMPoliceMessage());
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (IPMac) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getIp());
                ps.execute();
                ps.close();
            }

            client.getSession().close(true);

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ?, banby = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setString(4, banby);
            ps.setInt(5, accountid);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }

    }

    public static boolean tempban(String reason, Calendar duration, int greason, String banby, String name) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps;

            int accid = MapleCharacterUtil.getAccIdByName(name);
            if (accid == -1) {
                return false;
            }

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ?, banby = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setString(4, banby);
            ps.setInt(5, accid);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
        return true;
    }

    public final boolean ban(String reason, boolean IPMac, boolean autoban, boolean hellban, String banby) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }

        Connection con = null;

        client.getSession().write(MaplePacketCreator.GMPoliceMessage());
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ?, banby = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setString(3, banby);
            ps.setInt(4, accountid);
            ps.execute();
            ps.close();

            if (IPMac) {
                client.banHwID();
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();

                if (hellban) {
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, accountid);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ?, banby = ? WHERE email = ? OR SessionIP = ?");
                        pss.setInt(1, autoban ? 2 : 1);
                        pss.setString(2, reason);
                        pss.setString(3, banby);
                        pss.setString(4, rsa.getString("email"));
                        pss.setString(5, client.getSessionIPAddress());
                        pss.execute();
                        pss.close();
                    }
                    rsa.close();
                    psa.close();

                }
            }
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex + " target : " + getName() + "(" + getId() + ")");
            return false;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
        client.getSession().close(true);
        return true;
    }

    public static boolean ban(String id, String reason, boolean accountId, int gmlevel, boolean hellban, String banby) {
        int z = 0;
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                z = rs.getInt(1);
                PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = ? WHERE id = ? AND gm < ?");
                psb.setString(1, reason);
                psb.setString(2, banby);
                psb.setInt(3, z);
                psb.setInt(4, gmlevel);
                psb.execute();
                psb.close();

                if (gmlevel > 100) { //admin ban
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, z);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        String sessionIP = rsa.getString("sessionIP");
                        if (rsa.getString("macs") != null) {
                            MapleClient.banHwID(rsa.getString("macs"));
                        }
                        if (hellban) {
                            PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, banby = ? WHERE email = ?" + (sessionIP == null ? "" : " OR SessionIP = ?"));
                            pss.setString(1, reason);
                            pss.setString(2, banby);
                            pss.setString(3, rsa.getString("email"));
                            if (sessionIP != null) {
                                pss.setString(4, sessionIP);
                            }
                            pss.execute();
                            pss.close();
                        }
                    }
                    rsa.close();
                    psa.close();
                }
                ret = true;
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex + " target : " + id + "(" + z + ")");
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    /**
     * Oid of players is always = the cid
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.add(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.remove(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        visibleMapObjectsLock.readLock().lock();
        try {
            return visibleMapObjects.contains(mo);
        } finally {
            visibleMapObjectsLock.readLock().unlock();
        }
    }

    public Collection<MapleMapObject> getAndWriteLockVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().lock();
        return visibleMapObjects;
    }

    public void unlockWriteVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().unlock();
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
        //don't need this, client takes care of it
        /*if (dragon != null) {
         client.getSession().write(MaplePacketCreator.removeDragon(this.getId()));
         }*/
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer().allowedToTarget(this)) {
            client.getSession().write(MaplePacketCreator.spawnPlayerMapobject(this));
            sendTemporaryStats();
            client.getPlayer().receivePartyMemberHP();
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    //pet.setPos(getTruePosition());
                    client.getSession().write(PetPacket.showPet(this, pet, false, false));
                }
            }
            if (dragon != null) {
                client.getSession().write(MaplePacketCreator.spawnDragon(dragon));
            }
            if (summons != null && summons.size() > 0) {
                summonsLock.readLock().lock();
                try {
                    for (final MapleSummon summon : summons) {
                        if (client != getClient()) {
                            //client.getSession().write(MaplePacketCreator.spawnSummon(summon, false));
                        }
                    }
                } finally {
                    summonsLock.readLock().unlock();
                }
            }
            if (followid > 0 && followon) {
                client.getSession().write(MaplePacketCreator.followEffect(followinitiator ? followid : id, followinitiator ? id : followid, null));
            }
        }
    }

    public final void equipChanged() {
        if (map == null) {
            return;
        }
        map.broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
        stats.recalcLocalStats(this);
        if (getMessenger() != null) {
            World.Messenger.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
        }
    }

    public MaplePet getPet(final int index) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (count == index) {
                    return pet;
                }
                count++;
            }
        }
        return null;
    }

    public void removePetCS(MaplePet pet) {
        pets.remove(pet);
    }

    public void addPet(final MaplePet pet) {
        if (pets.contains(pet)) {
            pets.remove(pet);
        }
        pets.add(pet);
        // So that the pet will be at the last
        // Pet index logic :(
    }

    public void addPetz(final MaplePet pet) {
        if (pets.contains(pet)) {
            pets.remove(pet);
        }
        pets.add(pet);
        for (int i = 0; i < 3; ++i) {
            if (petz[i] == null) {
                petz[i] = pet;
                return;
            }
        }

        // So that the pet will be at the last
        // Pet index logic :(
    }

    public void removePet(MaplePet pet, boolean shiftLeft) {
        pet.setSummoned(0);
        int slot = -1;
        for (int i = 0; i < 3; i++) {
            if (petz[i] != null) {
                if (petz[i].getUniqueId() == pet.getUniqueId()) {
                    petz[i] = null;
                    slot = i;
                    break;
                }
            }
        }
        if (shiftLeft) {
            if (slot > -1) {
                for (int i = slot; i < 3; i++) {
                    if (i != 2) {
                        petz[i] = petz[i + 1];
                    } else {
                        petz[i] = null;
                    }
                }
            }
        }
    }

    public final byte getPetIndex(final MaplePet petz) {
        for (byte i = 0; i < 3; i++) {
            if (this.petz[i] != null) {
                if (this.petz[i].getUniqueId() == petz.getUniqueId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public final byte getPetIndex(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final List<MaplePet> getSummonedPets() {
        List<MaplePet> ret = new ArrayList<MaplePet>();
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                ret.add(pet);
            }
        }
        return ret;
    }

    public final byte getPetById(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getPetItemId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final MaplePet[] getPetz() {
        return petz;
    }

    public final List<MaplePet> getPets() {
        return pets;
    }

    public final void unequipAllPets() {
        for (final MaplePet pet : pets) {
            if (pet != null) {
                unequipPet(pet, true, false);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shiftLeft, boolean hunger) {
        if (pet.getSummoned()) {
            pet.saveToDb();

            client.getSession().write(PetPacket.updatePet(pet, getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), false));
            if (map != null) {
                map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger), true);
            }
            removePet(pet, shiftLeft);
            //List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>();
            //stats.put(MapleStat.PET, Integer.valueOf(0)));
            //showpetupdate isn't done here...
            client.getSession().write(PetPacket.petStatUpdate(this));
            //client.getSession().write(MaplePacketCreator.updatePlayerStats(Collections.singletonMap(MapleStat.PET, 0), 0));
            client.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public final long getLastFameTime() {
        return lastfametime;
    }

    public final List<Integer> getFamedCharacters() {
        return lastmonthfameids;
    }

    public final List<Integer> getBattledCharacters() {
        return lastmonthbattleids;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (from == null || lastmonthfameids == null || lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.execute();
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
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
    }

    public boolean canBattle(MapleCharacter to) {
        if (to == null || lastmonthbattleids == null || lastmonthbattleids.contains(Integer.valueOf(to.getAccountID()))) {
            return false;
        }
        return true;
    }

    public final MapleKeyLayout getKeyLayout() {
        return this.keylayout;
    }

    public MapleParty getParty() {
        if (party == null) {
            return null;
        } else if (party.isDisbanded()) {
            party = null;
        }
        return party;
    }

    public byte getWorld() {
        return world;
    }

    public void setWorld(byte world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
//        if (eventInstance == null && isGM()) {
//            dropMessage(5, "심각 : 인스턴스 요청을 했지만 현재 캐릭터의 인스턴스가 발견되지 않음.");
//        }
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<MapleDoor>(doors);
    }

    public void addMechDoor(MechDoor door) {
        mechDoors.add(door);
    }

    public void clearMechDoors() {
        mechDoors.clear();
    }

    public List<MechDoor> getMechDoors() {
        return new ArrayList<MechDoor>(mechDoors);
    }

    public void setSmega() {
        if (smega) {
            smega = false;
            dropMessage(5, "You have set megaphone to disabled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
        }
    }

    public boolean getSmega() {
        return smega;
    }

    public List<MapleSummon> getSummonsReadLock() {
        summonsLock.readLock().lock();
        return summons;
    }

    public int getSummonsSize() {
        return summons.size();
    }

    public void unlockSummonsReadLock() {
        summonsLock.readLock().unlock();
    }

    public void addSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.add(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public void removeSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.remove(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal(this);
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getGuildId() {
        return guildid;
    }

    public byte getGuildRank() {
        return guildrank;
    }

    public int getGuildContribution() {
        return guildContribution;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
            guildContribution = 0;
        }
    }

    public void setGuildRank(byte _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setGuildContribution(int _c) {
        this.guildContribution = _c;
        if (mgc != null) {
            mgc.setGuildContribution(_c);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public void setAllianceRank(byte rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public byte getAllianceRank() {
        return allianceRank;
    }

    public MapleGuild getGuild() {
        if (getGuildId() <= 0) {
            return null;
        }
        return World.Guild.getGuild(getGuildId());
    }

    public void getGuildInfo(int id) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                if (mch != null && mch.getGuildId() == id) {
                    mch.getClient().getSession().write(MaplePacketCreator.showGuildInfo(mch));
                }
            }
        }
    }

    public void setJob(int j) {
        this.job = (short) j;
    }

    public void guildUpdate() {
        if (guildid <= 0) {
            return;
        }
        mgc.setLevel((short) level);
        mgc.setJobId(job);
        World.Guild.memberLevelJobUpdate(mgc);
    }

    public void saveGuildStatus() {
        MapleGuild.setOfflineGuildStatus(guildid, guildrank, guildContribution, allianceRank, id);
    }

    public void modifyCSPoints(int type, int quantity) {
        modifyCSPoints(type, quantity, false);
    }

    public void modifyCSPoints(int type, int quantity, boolean show) {

        if (goDonateCashShop) {
            type = 3;
        }

        switch (type) {
            case 1:
                if (acash + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "캐시를 더 받을 수 없습니다. 캐시가 지급되지 않습니다.");
                    }
                    return;
                }
                acash += quantity;
                break;
            case 2:
                if (maplepoints + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "메이플 포인트를 더 받을 수 없습니다. 캐시가 지급되지 않습니다.");
                    }
                    return;
                }
                maplepoints += quantity;
                break;
            case 3:
                if (donatecash + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "후원 캐시를 더 받을 수 없습니다. 캐시가 지급되지 않습니다.");
                    }
                    return;
                }
                donatecash += quantity;
                break;

            default:
                break;
        }
        if (show && quantity != 0) {
            client.getSession().write(MaplePacketCreator.onChatMessage((short) 6, (type == 1 ? "캐시를 " : type == 3 ? "후원 캐시를 " : "메이플포인트를 ") + (quantity > 0 ? "얻었습니다 (+" + quantity + ")" : "잃었습니다. (" + quantity + ")")));
            //client.getSession().write(MaplePacketCreator.showSpecialEffect(21));
        }
    }

    public int getRealP(int type) {
        if (type == 1) {
            return acash;
        }
        if (type == 2) {
            return maplepoints;
        }
        return 0;
    }

    public int getCSPoints(int type) {
        if (goDonateCashShop) {
            type = 3;
        }
        switch (type) {
            case 1:
                return acash;
            case 2:
                return maplepoints;
            case 3:
                return donatecash;
            default:
                return 0;
        }
    }

    public final boolean hasEquipped(int itemid) {
        return inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }

    public final boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        int possesed = inventory[type.ordinal()].countById(itemid);
        if (checkEquipped && type == MapleInventoryType.EQUIP) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    public final boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, true, true);
    }

    public final boolean haveItem(int itemid) {
        return haveItem(itemid, 1, true, true);
    }

    private void sendTemporaryStats() {
        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        MapleStatEffect eff = getStatForBuff(MapleBuffStat.MONSTER_RIDING);
        if (eff != null) {
            map.broadcastMessage(this, TemporaryStatsPacket.giveForeignMount(id, MapleStatEffect.parseMountInfo_Pure(this, eff.getSourceId()), eff.getSourceId(), stat), false);
        }
        Integer val = getBuffedValue(MapleBuffStat.ENERGY_CHARGE);
        if (val != null) {
            map.broadcastMessage(this, TemporaryStatsPacket.giveEnergyChargeTest(id, 10000, 50), false);
        }
        eff = getStatForBuff(MapleBuffStat.DASH_SPEED);
        if (eff != null) {
            map.broadcastMessage(this, TemporaryStatsPacket.giveForeignPirate(Collections.singletonMap(MapleBuffStat.DASH_SPEED, getBuffedValue(MapleBuffStat.DASH_SPEED)), eff.getDuration() / 1000, id, eff.getSourceId()), false);
        }
        /*eff = getStatForBuff(MapleBuffStat.AURA);
         if (eff != null) {
         map.broadcastMessage(this, TemporaryStatsPacket.giveForeignBuff(getId(), Collections.singletonMap(MapleBuffStat.DARK_AURA, getBuffedValue(MapleBuffStat.DARK_AURA)), eff), false);
         }*/
    }

    public boolean isSearchingParty() {
        return searchingParty;
    }

    public void searchParty() {
        for (MapleCharacter chr : map.getCharacters()) {
            if (chr.getId() != id && chr.getParty() == null && chr.getLevel() >= psearch_minLevel && chr.getLevel() <= psearch_maxLevel && (psearch_jobs.isEmpty() || psearch_jobs.contains(Integer.valueOf(chr.getJob()))) && (isGM() || !chr.isGM())) {
                if (party != null && party.getMembers().size() < 6 && party.getMembers().size() < psearch_membersNeeded) {
                    if (party.getMembers().size() < 6) {
                        //c.getSession().write(MaplePacketCreator.partyStatusMessage(22, invited.getName()));
                        chr.getClient().getSession().write(MaplePacketCreator.partyInvite(this, true));
                    }
                    //chr.setParty(party);
                    //World.Party.updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(chr));
                    //chr.receivePartyMemberHP();
                    //chr.updatePartyMemberHP();
                } else {
                    searchingParty = false;
                    break;
                }
            }
        }
    }

    public void setEquippedTimeAll() {
        long now = System.currentTimeMillis();
        for (Item item : getInventory(MapleInventoryType.EQUIPPED)) {
            item.setEquippedTime(now);
        }
    }

    public void acceptUpdate() {
        this.updateAccepted = true;
    }

    public void updateBonusExp(long now) {
        if (!this.updateAccepted) {
            return;
        }
        int slot = 0;
        String itemName = "";
        int hour = 0;
        int applyExpR = 0;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Item item : getInventory(MapleInventoryType.EQUIPPED)) {
            int itemId = item.getItemId();
            ItemInformation info = ii.getItemInformation(itemId);
            if (info != null) {
                if (info.bonusExps != null) {
                    long equippedTime = item.getEquippedTime();
                    for (StructBonusExp struct : info.bonusExps) {
                        if (struct.checkTerm(now, equippedTime) && applyExpR < struct.incExpR) {
                            slot = -item.getPosition();
                            itemName = ii.getName(itemId);
                            hour = struct.termStart;
                            applyExpR = struct.incExpR;
                        }
                    }
                }
            }
        }
        if (this.bonusExpR != applyExpR) {
            this.bonusExpR = applyExpR;
            if (applyExpR != 0) {
                final String format1 = "%s 장착으로 인해 몬스터 사냥 시 보너스 경험치 %d%%를 추가로 획득하게 됩니다.";
                final String format2 = "%s 장착 후 %d시간이 경과되어 몬스터 사냥 시 보너스 경험치 %d%%를 추가로 획득하게 됩니다.";
                if (hour == 0) {
                    dropMessage(5, String.format(format1, itemName, applyExpR));
                } else {
                    dropMessage(5, String.format(format2, itemName, hour, applyExpR));
                }
            }
        }
    }

    public static enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public byte getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().write(MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length));
    }

    public void removeCooldown(int skillId) {
        if (coolDowns.containsKey(Integer.valueOf(skillId))) {
            coolDowns.remove(Integer.valueOf(skillId));
        }
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        addCooldown(skillid, starttime, length);
    }

    public void giveCoolDowns(final List<MapleCoolDownValueHolder> cooldowns) {
        int time;
        if (cooldowns != null) {
            for (MapleCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
                ps.setInt(1, getId());
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                        continue;
                    }
                    giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                }
                deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");
            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (Exception e) {
                    }
                }
                if (rs != null) {
                    try {
                        rs.close();
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
        }
    }

    public int getCooldownSize() {
        return coolDowns.size();
    }

    public int getDiseaseSize() {
        return diseases.size();
    }

    public List<MapleCoolDownValueHolder> getCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<MapleCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mc : coolDowns.values()) {
            if (mc != null) {
                ret.add(mc);
            }
        }
        return ret;
    }

    public final List<MapleDiseaseValueHolder> getAllDiseases() {
        return new ArrayList<MapleDiseaseValueHolder>(diseases.values());
    }

    public final boolean hasDisease(final MapleDisease dis) {
        return diseases.containsKey(dis);
    }

    public void giveDebuff(final MapleDisease disease, MobSkill skill, short tDelay) {
        giveDebuff(disease, skill.getX(), skill.getDuration(), skill.getSkillId(), skill.getSkillLevel(), tDelay);
    }

    public void giveDebuff(final MapleDisease disease, int x, long duration, int skillid, int level, short tDelay) {
        final List<Pair<MapleDisease, Integer>> debuff = Collections.singletonList(new Pair<MapleDisease, Integer>(disease, Integer.valueOf(x)));
        if (map != null && !hasDisease(disease)) {
            if (!(disease == MapleDisease.SEDUCE || disease == MapleDisease.STUN)) {
                if (getBuffedValue(MapleBuffStat.HOLY_SHIELD) != null) {
                    return;
                }
            }
            if (disease == MapleDisease.SEDUCE && getJob() == 3512) { // 메카닉 용사의 의지
                final int HERO_WILL = 35121008;

                final int slv = getTotalSkillLevel(HERO_WILL);
                if (slv > 0 && !skillisCooling(HERO_WILL)) {

                    final MapleStatEffect eff = SkillFactory.getSkill(HERO_WILL).getEffect(slv);

                    client.getSession().write(MaplePacketCreator.showOwnBuffEffect(HERO_WILL, 1, getLevel(), slv));
                    if (map != null) {
                        map.broadcastMessage(this,
                                MaplePacketCreator.showBuffeffect(getId(), HERO_WILL, 1, getLevel(), slv),
                                false
                        );
                    }

                    if (eff.getCooldown() > 0) {
                        client.getSession().write(MaplePacketCreator.skillCooldown(HERO_WILL, eff.getCooldown()));
                        addCooldown(HERO_WILL, System.currentTimeMillis(), eff.getCooldown() * 1000L);
                    }

                    return;
                }
            }
            diseases.put(disease, new MapleDiseaseValueHolder(disease, System.currentTimeMillis(), duration));
            client.getSession().write(TemporaryStatsPacket.giveDebuff(disease, x, skillid, level, (int) duration, tDelay));
            map.broadcastMessage(this, TemporaryStatsPacket.giveForeignDebuff(id, disease, skillid, level, x, tDelay), false);

            if (x > 0 && disease == MapleDisease.POISON) { //poison, subtract all HP
                addHP((int) -x);
            }
        }
    }

    public final void giveSilentDebuff(final List<MapleDiseaseValueHolder> ld) {
        if (ld != null) {
            for (final MapleDiseaseValueHolder disease : ld) {
                diseases.put(disease.disease, disease);
            }
        }
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            client.getSession().write(TemporaryStatsPacket.cancelDebuff(debuff));
            if (debuff == MapleDisease.SLOW) {
                map.broadcastMessage(this, TemporaryStatsPacket.cancelForeignDebuffSlow(id, debuff), false);
            } else {
                map.broadcastMessage(this, TemporaryStatsPacket.cancelForeignDebuff(id, debuff), false);
            }

            diseases.remove(debuff);
        }
    }

    public void dispelDebuffs() {
        List<MapleDisease> diseasess = new ArrayList<MapleDisease>(diseases.keySet());
        for (MapleDisease d : diseasess) {
            dispelDebuff(d);
        }
    }

    public void cancelAllDebuffs() {
        diseases.clear();
    }

    public void setLevel(final short level) {
        this.level = (short) (level - 1);
    }

    public void sendNote(String to, String msg) {
        sendNote(to, msg, 0);
    }

    public void sendNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, getName(), msg, fame);
    }

    public void showNote() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, getName());
            rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.getSession().write(CSPacket.showNotes(this, rs, count));
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
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
    }

    public void deleteNote(int id, int fame) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT gift FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gift") == fame && fame > 0) { //not exploited! hurray
                    addFame(fame);
                    updateSingleStat(MapleStat.FAME, getFame());
                    client.getSession().write(MaplePacketCreator.getShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
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
    }

    public int getMulungEnergy() {
        return mulung_energy;
    }

    public void mulung_EnergyModify(int inc) {
        if (inc > 0) {
            if (mulung_energy + inc > 300) {
                mulung_energy = 300;
            } else {
                mulung_energy += inc;
            }
        } else {
            mulung_energy = 0;
        }
        client.getSession().write(MaplePacketCreator.MulungEnergy(mulung_energy));
    }

    public void writeMulungEnergy() {
        client.getSession().write(MaplePacketCreator.MulungEnergy(mulung_energy));
    }

    public void writeEnergy(String type, String inc) {
        client.getSession().write(MaplePacketCreator.sendPyramidEnergy(type, inc));
    }

    public void writeStatus(String type, String inc) {
        client.getSession().write(MaplePacketCreator.sendGhostStatus(type, inc));
    }

    public void writePoint(String type, String inc) {
        client.getSession().write(MaplePacketCreator.sendGhostPoint(type, inc));
    }

    public final short getCombo() {
        return combo;
    }

    public void setCombo(final short combo) {
        this.combo = combo;
    }

    public final long getLastCombo() {
        return lastCombo;
    }

    public void setLastCombo(final long combo) {
        this.lastCombo = combo;
    }

    public final long getKeyDownSkill_Time() {
        return keydown_skill;
    }

    public void setKeyDownSkill_Time(final long keydown_skill) {
        this.keydown_skill = keydown_skill;
    }

    public void checkBerserk() { //berserk is special in that it doesn't use worldtimer :)
        if (job != 132/* || lastBerserkTime < 0 || lastBerserkTime + 10000 > System.currentTimeMillis()*/) {
            return;
        }
        final Skill BerserkX = SkillFactory.getSkill(1320006);
        final int skilllevel = getTotalSkillLevel(BerserkX);
        if (skilllevel >= 1 && map != null) {
            lastBerserkTime = System.currentTimeMillis();
            final MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            stats.Berserk = stats.getHp() * 100 / stats.getCurrentMaxHp() >= ampStat.getX();
            client.getSession().write(MaplePacketCreator.showOwnBuffEffect(1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)));
            map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)), false);
        } else {
            lastBerserkTime = -1;
        }
    }

    public void checkFury(boolean check) { //berserk is special in that it doesn't use worldtimer :)
        if (!GameConstants.isEvan(job)) {
            return;
        }
        final Skill FuryX = SkillFactory.getSkill(22160000);
        final int skilllevel = getTotalSkillLevel(FuryX);
        if (check) {
            client.getSession().write(MaplePacketCreator.showOwnBuffEffect(22160000, 1, getLevel(), skilllevel, (byte) 0));
            map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 22160000, 1, getLevel(), skilllevel, (byte) 0), false);
        }
        if (skilllevel >= 1 && map != null) {
            if (stats.getMPPercent() >= FuryX.getEffect(skilllevel).getX() && stats.getMPPercent() <= FuryX.getEffect(skilllevel).getY()) {
                stats.Fury = true;
                client.getSession().write(MaplePacketCreator.showOwnBuffEffect(22160000, 1, getLevel(), skilllevel, (byte) 1));
                map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 22160000, 1, getLevel(), skilllevel, (byte) 1), false);
                stats.FuryValue = FuryX.getEffect(skilllevel).getDamage();
                return;
            } else {
                client.getSession().write(MaplePacketCreator.showOwnBuffEffect(22160000, 1, getLevel(), skilllevel, (byte) 0));
                map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(getId(), 22160000, 1, getLevel(), skilllevel, (byte) 0), false);
            }
        }
        stats.Fury = false;
        stats.FuryValue = 0;
        return;
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
        if (map != null) {
            map.broadcastMessage(CSPacket.useChalkboard(getId(), text));
        }
    }

    public String getChalkboard() {
        return chalktext;
    }

    public MapleMount getMount() {
        return mount;
    }

    public int[] getWishlist() {
        return wishlist;
    }

    public void clearWishlist() {
        for (int i = 0; i < 10; i++) {
            wishlist[i] = 0;
        }
        changed_wishlist = true;
    }

    public int getWishlistSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (wishlist[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public void setWishlist(int[] wl) {
        this.wishlist = wl;
        changed_wishlist = true;
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        for (int i = 0; i < 10; ++i) {
            if (rocks[i] == 999999999) {
                rocks[i] = getMapId();
                changed_trocklocations = true;
                break;
            }
        }
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRegRocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == map) {
                regrocks[i] = 999999999;
                changed_regrocklocations = true;
                break;
            }
        }
    }

    public void addRegRockMap() {
        if (getRegRockSize() >= 5) {
            return;
        }
        for (int i = 0; i < 5; ++i) {
            if (regrocks[i] == 999999999) {
                regrocks[i] = getMapId();
                changed_regrocklocations = true;
                break;
            }
        }

    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getHyperRocks() {
        return hyperrocks;
    }

    public int getHyperRockSize() {
        int ret = 0;
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromHyperRocks(int map) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == map) {
                hyperrocks[i] = 999999999;
                changed_hyperrocklocations = true;
                break;
            }
        }
    }

    public void addHyperRockMap() {
        if (getRegRockSize() >= 13) {
            return;
        }
        hyperrocks[getHyperRockSize()] = getMapId();
        changed_hyperrocklocations = true;
    }

    public boolean isHyperRockMap(int id) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public List<LifeMovementFragment> getLastRes() {
        return lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }

    public void dropMessage(int type, String message) {
        if (type == -1) {
            client.getSession().write(UIPacket.getTopMsg(message));
        } //else if (type == -2) {
        //            client.getSession().write(PlayerShopPacket.shopChat(message, 0)); //0 or what
        //        } else if (type == -3) {
        //            client.getSession().write(MaplePacketCreator.getChatText(getId(), message, isSuperGM(), 0)); //1 = hide
        //        } else if (type == -4) {
        //            client.getSession().write(MaplePacketCreator.getChatText(getId(), message, isSuperGM(), 1)); //1 = hide
        //        } else if (type == -5) {
        //            client.getSession().write(MaplePacketCreator.spouseMessage(message, false)); //pink
        //        } else if (type == -6) {
        //            client.getSession().write(MaplePacketCreator.spouseMessage(message, true)); //white bg
        //        } else if (type == -7) {
        //            client.getSession().write(UIPacket.getMidMsg(message, false, 0));
        //        } else if (type == -8) {
        //            client.getSession().write(UIPacket.getMidMsg(message, true, 0));
        //        } else {
        //            client.getSession().write(MaplePacketCreator.serverNotice(type, message));
        //        }
        else if (type < 0) {
            client.getSession().write(MaplePacketCreator.serverNotice(6, message));
        } else {
            client.getSession().write(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public IMaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(IMaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversation() {
        return inst.get();
    }

    public void setConversation(int inst) {
        this.inst.set(inst);
    }

    public int getDirection() {
        return insd.get();
    }

    public void setDirection(int inst) {
        this.insd.set(inst);
    }

    public MapleCarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(MapleCarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
        totalCP = (short) Math.max(0, totalCP);
        availableCP = (short) Math.max(0, availableCP);
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
        availableCP = (short) Math.max(0, availableCP);
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        totalCP = 0;
        availableCP = 0;
    }

    public void addCarnivalRequest(MapleCarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();
    }

    public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
        client.getSession().write(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
    }

    public void CPUpdate(final boolean party, final int available, final int total, final int team) {
        client.getSession().write(MonsterCarnivalPacket.CPUpdate(party, available, total, team));
    }

    public void playerDiedCPQ(final String name, final int lostCP, final int team) {
        client.getSession().write(MonsterCarnivalPacket.playerDiedMessage(name, lostCP, team));
    }

    public void setAchievementFinished(int id) {
        if (!finishedAchievements.contains(id)) {
            finishedAchievements.add(id);
            changed_achievements = true;
        }
    }

    public boolean achievementFinished(int achievementid) {
        return finishedAchievements.contains(achievementid);
    }

    public void finishAchievement(int id) {
        if (!achievementFinished(id)) {
            if (isAlive()) {
                MapleAchievementList.getInstance().getById(id).finishAchievement(this);
            }
        }
    }

    public List<Integer> getFinishedAchievements() {
        return finishedAchievements;
    }

    public boolean getCanTalk() {
        if (!isStaff() && !map.canTalk()) {
            return false;
        }
        return this.canTalk;
    }

    public void canTalk(boolean talk) {
        this.canTalk = talk;
    }

    public double getEXPMod() {
        return stats.expMod;
    }

    public double getEventEXPRate() {
        return stats.eventExpRate;
    }

    public int getDropMod() {
        return stats.dropMod;
    }

    public int getCashMod() {
        return stats.cashMod;
    }

    public void setPoints(int p) {
        this.points = p;
//        if (this.points >= 1) {
//            finishAchievement(1);
//        }
    }

    public int getPoints() {
        return points;
    }

    public void setVPoints(int p) {
        this.vpoints = p;
    }

    public int getVPoints() {
        return vpoints;
    }

    public CashShop getCashInventory() {
        return cs;
    }

    public void removeItem(int id, int quantity) {
        MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        client.getSession().write(MaplePacketCreator.getShowItemGain(id, (short) quantity, true));
    }

    public void removeAll(int id) {
        removeAll(id, true);
    }

    public void removeAll(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, getId(), getName(), id, -possessed, MapleItemInformationProvider.getInstance().getName(id), 0, "Map : " + getMapId() + " Interaction ? : " + getConversation());

            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possessed, true));
            }
        }
    }

    public void removeAllEquip(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            ServerLogger.getInstance().logItem(LogType.Item.FromScript, getId(), getName(), id, -possessed, MapleItemInformationProvider.getInstance().getName(id), 0, "Map : " + getMapId() + " Interaction ? : " + getConversation());

            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().write(MaplePacketCreator.getShowItemGain(id, (short) -possessed, true));
            }
        }
        if (type == MapleInventoryType.EQUIP) { //check equipped
            type = MapleInventoryType.EQUIPPED;
            possessed = getInventory(type).countById(id);

            if (possessed > 0) {
                Item equip = getInventory(type).findById(id);
                if (equip != null) {
                    getInventory(type).removeSlot(equip.getPosition());
                    equipChanged();
                    getClient().sendPacket(MaplePacketCreator.dropInventoryItem(MapleInventoryType.EQUIP, equip.getPosition()));
                }
            }
        }
    }

    public Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> getRings(boolean equip) {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        if (!equipped.isEmpty()) {
            equipped.addAll(getInventory(MapleInventoryType.EQUIP).list());
            equipped.addAll(getInventory(MapleInventoryType.ETC).list());
            Collections.sort(equipped);
        }
        List<MapleRing> crings = new ArrayList<MapleRing>(), frings = new ArrayList<MapleRing>(), mrings = new ArrayList<MapleRing>();
        MapleRing ring;
        for (Item item : equipped) {
            if (item.getRing() != null) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (GameConstants.isEffectRing(item.getItemId())) {
                    if (equip) {
                        if (GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (GameConstants.isMarriageRing(item.getItemId())) {
                            if (getMarriageId() > 0) {
                                mrings.add(ring);
                            }
                        }
                    } else {
                        if (crings.size() == 0 && GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (frings.size() == 0 && GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (mrings.size() == 0 && GameConstants.isMarriageRing(item.getItemId())) {
                            if (getMarriageId() > 0) {
                                mrings.add(ring);
                            }
                        } //for 3rd person the actual slot doesnt matter, so we'll use this to have both shirt/ring same?
                        //however there seems to be something else behind this, will have to sniff someone with shirt and ring, or more conveniently 3-4 of those
                    }
                }
            }
        }
        if (equip) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item item : iv.list()) {
                if (item.getRing() != null && GameConstants.isCrushRing(item.getItemId())) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (GameConstants.isFriendshipRing(item.getItemId())) {
                        frings.add(ring);
                    } else if (GameConstants.isCrushRing(item.getItemId())) {
                        crings.add(ring);
                    } else if (GameConstants.isMarriageRing(item.getItemId())) {
                        mrings.add(ring);
                    }
                }
            }
        }
        Collections.sort(frings, new MapleRing.RingComparator());
        Collections.sort(crings, new MapleRing.RingComparator());
        Collections.sort(mrings, new MapleRing.RingComparator());
        return new Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>>(crings, frings, mrings);
    }

    public int getFH() {
//        MapleFoothold fh = getMap().getFootholds().findBelow(getTruePosition());
//        if (fh != null) {
//            return fh.getId();
//        }
        return fh;
    }

    public final void setFh(final int Fh) {
        fh = Fh;
    }

    public final boolean canHP(long now, int time) {
        if (lastHPTime + time <= now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMP(long now, int time) {
        if (lastMPTime + time < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canHP2(long now, int time) {
        if (lastHPTime2 + time <= now) {
            lastHPTime2 = now;
            return true;
        }
        return false;
    }

    public final boolean canMP2(long now, int time) {
        if (lastMPTime2 + time < now) {
            lastMPTime2 = now;
            return true;
        }
        return false;
    }

    long lastHPTime2 = 0;
    long lastMPTime2 = 0;

    public final boolean canHPRecover(long now) {
        if (stats.hpRecoverTime > 0 && lastHPTime + stats.hpRecoverTime < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMPRecover(long now) {
        if (stats.mpRecoverTime > 0 && lastMPTime + stats.mpRecoverTime < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public int getTeam() {
        return coconutteam;
    }

    public void setTeam(int v) {
        this.coconutteam = v;
    }

    public void spawnPet(byte slot, boolean lead) {
        final Item item = getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() > 5010000 || item.getItemId() < 5000000) {
            return;
        }
        final MaplePet pet = item.getPet();
        if (getPet(0) != null && getPet(1) != null && getPet(2) != null && getPetIndex(pet) == -1) {
            dropMessage(1, "펫은 최대 3개까지만 장착 가능합니다.");
            client.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        boolean multipet = false;
        Map<String, Integer> petStat = MapleItemInformationProvider.getInstance().getEquipStats(item.getItemId());
        if (petStat.containsKey("multiPet") && petStat.get("multiPet") == 1) {
            multipet = true;
        }
        if (getPetIndex(pet) == -1 && !multipet) {
            if (getPet(0) != null || getPet(1) != null || getPet(2) != null) {
                dropMessage(1, "멀티펫만 소환이 가능합니다.");
                client.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (pet != null && (item.getItemId() != 5000054 || pet.getSecondsLeft() > 0) && (item.getExpiration() == -1 || item.getExpiration() > System.currentTimeMillis())) {
            if (getPetIndex(pet) != -1) {
                unequipPet(pet, false, false);
            } else {
                final Point pos = getTruePosition();
                pet.setPos(pos);
                try {
                    pet.setFh(getMap().getFootholds().findBelow(pos).getId());
                } catch (NullPointerException e) {
                    pet.setFh(0); //lol, it can be fixed by movement
                }
                pet.setStance(0);
                pet.setSummoned(1);
                addPetz(pet);
                pet.setSummoned(getPetIndex(pet) + 1); //then get the index
                if (getMap() != null) {
                    client.getSession().write(PetPacket.updatePet(pet, getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    client.getSession().write(PetPacket.petStatUpdate(this));
                    getMap().broadcastMessage(this, PetPacket.showPet(this, pet, false, false), true);
                    client.sendPacket(PetPacket.loadPetPickupExceptionList(getId(), pet.getUniqueId(), pet.getPickupExceptionList(), (byte) (pet.getSummonedValue() - 1)));
                    if (pet.getCloseness() < 1) {
                        pet.setCloseness(1);
                    }
                }

            }
        }
        client.getSession().write(MaplePacketCreator.enableActions());
    }

    public void shiftPetsRight() {
        if (petz[2] == null) {
            petz[2] = petz[1];
            petz[1] = petz[0];
            petz[0] = null;
        }
    }

    public final void spawnSavedPets() {
        for (int i = 0; i < petStore.length; i++) {
            if (petStore[i] > -1) {
                spawnPet(petStore[i], false);
            }
        }
        petStore = new byte[]{-1, -1, -1};
    }

    public final byte[] getPetStores() {
        return petStore;
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk) {
        Map<MapleStat, Integer> stat = new EnumMap<MapleStat, Integer>(MapleStat.class);
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();

        total -= str;
        stats.str = (short) str;

        total -= dex;
        stats.dex = (short) dex;

        total -= int_;
        stats.int_ = (short) int_;

        total -= luk;
        stats.luk = (short) luk;

        setRemainingAp((short) total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, str);
        stat.put(MapleStat.DEX, dex);
        stat.put(MapleStat.INT, int_);
        stat.put(MapleStat.LUK, luk);
        stat.put(MapleStat.AVAILABLEAP, total);
        client.getSession().write(MaplePacketCreator.updatePlayerStats(stat, false, getJob()));
    }

    public byte getSubcategory() {
        if (job >= 430 && job <= 434) {
            return 1; //dont set it
        }
        if (GameConstants.isCannon(job) || job == 1) {
            return 2;
        }
        if (job != 0 && job != 400) {
            return 0;
        }
        return subcategory;
    }

    public void setSubcategory(int z) {
        this.subcategory = (byte) z;
    }

    public Event_PyramidSubway getPyramidSubway() {
        return pyramidSubway;
    }

    public void setPyramidSubway(Event_PyramidSubway ps) {
        this.pyramidSubway = ps;
    }

    public int itemQuantity(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).countById(itemid);
    }

    public void setRPS(RockPaperScissors rps) {
        this.rps = rps;
    }

    public RockPaperScissors getRPS() {
        return rps;
    }

    public long getNextConsume() {
        return nextConsume;
    }

    public void setNextConsume(long nc) {
        this.nextConsume = nc;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public void changeChannel(final int channel) {
        final ChannelServer toch = ChannelServer.getInstance(channel);

        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            client.getSession().write(MaplePacketCreator.serverBlocked(1));
            return;
        }
        changeRemoval();

        final MaplePortal closest = map.findClosestSpawnpoint(getTruePosition());
        setSpawnpoint((byte) closest.getId());
        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(getId(), getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(this), getId(), channel);
        ch.removePlayer(this);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        LoginServer.setCodeHash(getId(), client.getCodeHash());
        client.getSession().write(MaplePacketCreator.getChannelChange(client, toch.getPort()));
        saveToDB(false, false);
        getMap().removePlayer(this);

        client.setPlayer(null);
        client.setReceiving(false);
    }

    public void expandInventory(byte type, int amount) {
        final MapleInventory inv = getInventory(MapleInventoryType.getByType(type));
        inv.addSlot((byte) amount);
        client.getSession().write(MaplePacketCreator.getSlotUpdate(type, (byte) inv.getSlotLimit()));
    }

    public boolean allowedToTarget(MapleCharacter other) {
        return other != null && (!other.isHidden() || getGMLevel() >= other.getGMLevel());
    }

    public int getFollowId() {
        return followid;
    }

    public void setFollowId(int fi) {
        this.followid = fi;
        if (fi == 0) {
            this.followinitiator = false;
            this.followon = false;
        }
    }

    public void setFollowInitiator(boolean fi) {
        this.followinitiator = fi;
    }

    public void setFollowOn(boolean fi) {
        this.followon = fi;
    }

    public boolean isFollowOn() {
        return followon;
    }

    public boolean isFollowInitiator() {
        return followinitiator;
    }

    public void checkFollow() {
        if (followid <= 0) {
            return;
        }
        if (followon) {
            map.broadcastMessage(MaplePacketCreator.followEffect(id, 0, null));
            map.broadcastMessage(MaplePacketCreator.followEffect(followid, 0, null));
        }
        MapleCharacter tt = map.getCharacterById(followid);
        //      client.getSession().write(MaplePacketCreator.getFollowMessage("Follow canceled."));
        if (tt != null) {
            tt.setFollowId(0);
            //       tt.getClient().getSession().write(MaplePacketCreator.getFollowMessage("Follow canceled."));
        }
        setFollowId(0);
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(final int mi) {
        this.marriageId = mi;
    }

    public int getEngageId() {
        return engageId;
    }

    public void setEngageId(final int mi) {
        this.engageId = mi;
    }

    public int getMarriageItemId() {
        return marriageItemId;
    }

    public void setMarriageItemId(final int mi) {
        this.marriageItemId = mi;
    }

    public boolean isStaff() {
        return this.gmLevel >= ServerConstants.PlayerGMRank.INTERN.getLevel();
    }

    public boolean isDonator() {
        return this.gmLevel >= ServerConstants.PlayerGMRank.DONATOR.getLevel();
    }

    // TODO: gvup, vic, lose, draw, VR
    public boolean startPartyQuest(final int questid) {
        boolean ret = false;
        MapleQuest q = MapleQuest.getInstance(questid);
        if (q == null || !q.isPartyQuest()) {
            return false;
        }
        if (!quests.containsKey(q) || !questinfo.containsKey(questid)) {
            final MapleQuestStatus status = getQuestNAdd(q);
            status.setStatus((byte) 1);
            updateQuest(status);
            switch (questid) {
                case 1300:
                case 1301:
                case 1302: //carnival, ariants.
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0;gvup=0;vic=0;lose=0;draw=0");
                    break;
                case 1303: //ghost pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0;vic=0;lose=0;draw=0");
                    break;
                case 1204: //herb town pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;have2=0;have3=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                case 1206: //ellin pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                default:
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
            }
            ret = true;
        } //started the quest.
        return ret;
    }

    public String getOneInfo(final int questid, final String key) {
        if (!questinfo.containsKey(questid) || key == null || MapleQuest.getInstance(questid) == null) {
            return null;
        }
        final String[] split = questinfo.get(questid).split(";");
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length == 2 && split2[0].equals(key)) {
                return split2[1];
            }
        }
        return null;
    }

    public void updateOneInfo(final int questid, final String key, final String value) {
        if (!questinfo.containsKey(questid) || key == null || value == null || MapleQuest.getInstance(questid) == null) {
            return;
        }
        final String[] split = questinfo.get(questid).split(";");
        boolean changed = false;
        final StringBuilder newQuest = new StringBuilder();
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length != 2) {
                continue;
            }
            if (split2[0].equals(key)) {
                newQuest.append(key).append("=").append(value);
            } else {
                newQuest.append(x);
            }
            newQuest.append(";");
            changed = true;
        }

        updateInfoQuest(questid, changed ? newQuest.toString().substring(0, newQuest.toString().length() - 1) : newQuest.toString());
    }

    public void recalcPartyQuestRank(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        if (!startPartyQuest(questid)) {
            final String oldRank = getOneInfo(questid, "rank");
            if (oldRank == null || oldRank.equals("S")) {
                return;
            }
            String newRank = null;
            if (oldRank.equals("A")) {
                newRank = "S";
            } else if (oldRank.equals("B")) {
                newRank = "A";
            } else if (oldRank.equals("C")) {
                newRank = "B";
            } else if (oldRank.equals("D")) {
                newRank = "C";
            } else if (oldRank.equals("F")) {
                newRank = "D";
            } else {
                return;
            }
            final List<Pair<String, Pair<String, Integer>>> questInfo = MapleQuest.getInstance(questid).getInfoByRank(newRank);
            if (questInfo == null) {
                return;
            }
            if (questid == 1200/*월묘*/ || questid == 1203/*여신의 흔적*/ || questid == 1204/*뎁존*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        found = vall < q.right.right;
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                        found = vall == q.right.right;//"HAVE"
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1201/*커파*/ || questid == 1202/*루파*/ || questid == 1205/*rnj*/ || questid == 1206) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        found = vall < q.right.right;
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall == q.right.right;//"HAVE"
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1209/*렉스*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        found = vall < q.right.right;
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1210/*드래곤라이더*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }//시간 아이템 갯수 횟수체크순으로 다시 봐보장
                    if (q.left.equals("less")) {//시간통과
                        //dropMessage(6, "1: " + found + " vall: " + vall + " q.right.right: " + q.right.right);
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1211/*피라미드*/ || questid == 1212/*임차장*/ || questid == 1213/*무릉도장*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    if (q.left.equals("less")) {//시간통과
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                        found = vall == q.right.right;//"해브"
                    }
                    if (!found) {
                        return;
                    }
                }
            } else if (questid == 1303/*안개바다*/) {
                for (Pair<String, Pair<String, Integer>> q : questInfo) {
                    boolean found = false;
                    final String val = getOneInfo(questid, q.right.left);
                    //dropMessage(6, "val" + val);
                    if (val == null) {
                        return;
                    }
                    int vall = 0;
                    try {
                        vall = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return;
                    }
                    if (q.left.equals("less")) {//시간통과
                    } else if (q.left.equals("more")) {
                        found = vall >= q.right.right;//"클리어횟수"
                    } else if (q.left.equals("equal")) {
                        found = vall == q.right.right;//"해브"
                    }
                    if (!found) {
                        return;
                    }
                }
            }
            //perfectly safe
            updateOneInfo(questid, "rank", newRank);
        }
    }

    public void tryPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            pqStartTime = System.currentTimeMillis();
            updateOneInfo(questid, "try", String.valueOf(Integer.parseInt(getOneInfo(questid, "try")) + 1));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("tryPartyQuest error");
        }
    }

    public void endPartyQuest(final int questid) { // 기존 이 메소드를 쓰고있는 스크립트가 에러나지 않게 하기 위함인데 모든 스크립트 다 바꾸는게 신상에 좋음
        endPartyQuest(questid, 1);
    }

    public void endPartyQuest(final int questid, int result) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            if (isGM() && pqStartTime == 0) {
                pqStartTime = System.currentTimeMillis() - 5;
            }
            if (pqStartTime > 0) {
                final long changeTime = System.currentTimeMillis() - pqStartTime;
                final int mins = (int) (changeTime / 1000 / 60), secs = (int) (changeTime / 1000 % 60);
                final int mins2 = Integer.parseInt(getOneInfo(questid, "min"));
                if (mins2 <= 0 || mins < mins2) {
                    updateOneInfo(questid, "min", String.valueOf(mins));
                    updateOneInfo(questid, "sec", String.valueOf(secs));
                    updateOneInfo(questid, "date", FileoutputUtil.CurrentReadable_Date());
                }
                switch (questid) { // 95 기준으로 한건데 카니발 같이 vs 개념인 애들은 여기다 case 걸어야함
                    case 1300: // 아리안트 ver 95
                    case 1301: // 카니발 ver 95
                    case 1302: // 카니발2 ver 95
                    case 1303: // 안개바다의 유령선 ver 109 << 따로추가했는데 다른파퀘 확인필요
                        int newVic = Integer.parseInt(getOneInfo(questid, "vic"));
                        int newLose = Integer.parseInt(getOneInfo(questid, "lose"));
                        int newDraw = Integer.parseInt(getOneInfo(questid, "draw"));
                        switch (result) {
                            case 1:
                                updateOneInfo(questid, "vic", String.valueOf(++newVic));
                                break;
                            case 2:
                                updateOneInfo(questid, "lose", String.valueOf(++newLose));
                                break;
                            case 3:
                                updateOneInfo(questid, "draw", String.valueOf(++newDraw));
                                break;
                            default:
                                break;
                        }
                        updateOneInfo(questid, "VR", String.valueOf((int) (newVic * 100.0 / (newVic + newLose + newDraw))));
                        break;
                    default:
                        final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
                        updateOneInfo(questid, "cmp", String.valueOf(newCmp));
                        updateOneInfo(questid, "CR", String.valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
                        break;
                }
                recalcPartyQuestRank(questid);
                pqStartTime = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("endPartyQuest error");
        }
    }

//    public void endPartyQuest(final int questid) {
//        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
//            return;
//        }
//        try {
//            startPartyQuest(questid);
//            if (pqStartTime > 0) {
//                final long changeTime = System.currentTimeMillis() - pqStartTime;
//                final int mins = (int) (changeTime / 1000 / 60);//신규
//                final int secs = (int) (changeTime / 1000 % 60);
//                final int mins2 = Integer.parseInt(getOneInfo(questid, "min"));//기존
//                final int secs2 = Integer.parseInt(getOneInfo(questid, "sec"));
//                if (mins2 + secs2 == 0/*첫 트라이*/ || mins * 60 + secs < mins2 * 60 + secs2) {     <<<<<<<<< 이부분 왜 한건지 모르겠다 컨텐츠때매?
//                    updateOneInfo(questid, "min", String.valueOf(mins));
//                    updateOneInfo(questid, "sec", String.valueOf(secs));
//                    updateOneInfo(questid, "date", FileoutputUtil.CurrentReadable_Date());
//                }
//                final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
//                //final int newCmp = 120;
//                updateOneInfo(questid, "cmp", String.valueOf(newCmp));
//                updateOneInfo(questid, "CR", String.valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
//                recalcPartyQuestRank(questid);
//                pqStartTime = 0;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("endPartyQuest error");
//        }
//    }
    public int getCmp(int questid) {
        final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp"));
        return newCmp;
    }

    public void havePartyQuest(final int itemId) {
        int questid = 0, index = -1;
        switch (itemId) {
            case 1002798:
                questid = 1200; //henesys
                break;
            case 1072369:
                questid = 1201; //kerning
                break;
            case 1022073:
                questid = 1202; //ludi
                break;
            case 1082232:
                questid = 1203; //orbis
                break;
            case 1002571:
            case 1002572:
            case 1002573:
            case 1002574:
                questid = 1204; //herbtown
                index = itemId - 1002571;
                break;
            case 1102226:
                questid = 1303; //ghost
                break;
            case 1102227:
                questid = 1303; //ghost
                index = 1;
                break;
            case 1122010:
                questid = 1205; //magatia
                break;
            case 1032061:
            case 1032060:
                questid = 1206; //ellin
                index = itemId - 1032060;
                break;
            case 3010018:
                questid = 1300; //ariant
                break;
            case 1122007:
                questid = 1301; //carnival
                break;
            case 1122058:
                questid = 1302; //carnival2
                break;
            default:
                return;
        }
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        startPartyQuest(questid);
        updateOneInfo(questid, "have" + (index == -1 ? "" : index), "1");//퀘스트 장비 업뎃부분
        if (itemId / 10000 == 521) {
            stats.recalcLocalStats(this);
        }
        if (itemId / 10000 == 410) {
            stats.recalcLocalStats(this);
        }
    }

    public void addCanGainFame() {
        canGainNoteFame++;
    }

    public boolean checkCanGainFameNGive() {
        if (canGainNoteFame > 0) {
            canGainNoteFame--;
            addFame(1);
            updateFame();
            return true;
        }
        return false;
    }

    public void resetStatsByJob(boolean beginnerJob) {
        int baseJob = (beginnerJob ? (job % 1000) : (((job % 1000) / 100) * 100)); //1112 -> 112 -> 1 -> 100
        boolean UA = getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER)) != null;
        if (baseJob == 100) { //first job = warrior
            resetStats(UA ? 4 : 35, 4, 4, 4);
        } else if (baseJob == 200) {
            resetStats(4, 4, UA ? 4 : 20, 4);
        } else if (baseJob == 300 || baseJob == 400) {
            resetStats(4, UA ? 4 : 25, 4, 4);
        } else if (baseJob == 500) {
            resetStats(4, UA ? 4 : 20, 4, 4);
        } else if (baseJob == 0) {
            resetStats(4, 4, 4, 4);
        }
    }

    public boolean hasSummon() {
        return hasSummon;
    }

    public void setHasSummon(boolean summ) {
        this.hasSummon = summ;
    }

    public void removeDoor() {
        final MapleDoor door = getDoors().iterator().next();
        for (final MapleCharacter chr : door.getTarget().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleCharacter chr : door.getTown().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleDoor destroyDoor : getDoors()) {
            door.getTarget().removeMapObject(destroyDoor);
            door.getTown().removeMapObject(destroyDoor);
        }
        clearDoors();
    }

    public void removeMechDoor() {
        for (final MechDoor destroyDoor : getMechDoors()) {
            for (final MapleCharacter chr : getMap().getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            getMap().removeMapObject(destroyDoor);
        }
        clearMechDoors();
    }

    public void removeMechDoor(final MapleMap fromMap) {
        for (final MechDoor destroyDoor : getMechDoors()) {
            for (final MapleCharacter chr : fromMap.getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            fromMap.removeMapObject(destroyDoor);
        }
        clearMechDoors();
    }

    public void changeRemoval() {
        changeRemoval(false);
    }

    public void changeRemoval(boolean dc) {
        if (getCheatTracker() != null && dc) {
            getCheatTracker().dispose();
        }
        //dispelSummonsCC();
        if (!dc) {
            //(MapleBuffStat.MONSTER_RIDING);
            cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
            cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
        }
        if (getPyramidSubway() != null) {
            getPyramidSubway().dispose(this);
        }
        if (playerShop != null && !dc) {
            playerShop.removeVisitor(this, true);
            if (playerShop.isOwner(this)) {
                playerShop.setOpen(true);
            }
        }
        if (!getDoors().isEmpty()) {
            removeDoor();
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        NPCScriptManager.getInstance().dispose(client);
    }

    public void updateTick(int newTick) {
        anticheat.updateTick(newTick);
    }

    public String getTeleportName() {
        return teleportname;
    }

    public void setTeleportName(final String tname) {
        teleportname = tname;
    }

    public boolean isInBlockedMap() {
        if (!isAlive() || getMap().getSquadByMap() != null || getEventInstance() != null || getMap().getEMByMap() != null) {
            return true;
        }
        if ((getMapId() >= 680000210 && getMapId() <= 680000502) || (getMapId() / 10000 == 92502 && getMapId() >= 925020100) || (getMapId() / 10000 == 92503) || getMapId() == GameConstants.JAIL) {
            return true;
        }
        if ((getMapId() >= 270010200 && getMapId() <= 270030630)) {
            return true;
        }
        if ((getMapId() >= 271030000 && getMapId() <= 271030600)) {
            return true;
        }
        if ((getMapId() >= 310070000 && getMapId() <= 320000100)) {
            return true;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return true;
            }
        }
        return false;
    }

    public boolean canUseReturnScroll() {
        if (!isAlive() || getMap().getSquadByMap() != null || getEventInstance() != null || getMap().getEMByMap() != null) {
            return false;
        }
        if ((getMapId() >= 680000210 && getMapId() <= 680000502) || (getMapId() / 10000 == 92502 && getMapId() >= 925020100) || (getMapId() / 10000 == 92503) || getMapId() == GameConstants.JAIL) {
            return false;
        }
        if ((getMapId() >= 310070000 && getMapId() <= 320000100)) {
            return false;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }

    public boolean isInTownMap() {
        if (hasBlockedInventory() || !getMap().isTown() || FieldLimitType.VipRock.check(getMap().getFieldLimit()) || getEventInstance() != null) {
            return false;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }

    public boolean hasBlockedInventory() {
        return !isAlive() || /*getTrade() != null ||*/ getConversation() > 0 || getDirection() >= 0 || getPlayerShop() != null || map == null;
    }

    public void startPartySearch(final List<Integer> jobs, final int maxLevel, final int minLevel, final int membersNeeded) {
        psearch_jobs = jobs;
        psearch_maxLevel = maxLevel;
        psearch_membersNeeded = membersNeeded;
        psearch_minLevel = minLevel;
        searchingParty = true;
        searchParty();
    }

    public void stopPartySearch() {
        searchingParty = false;
    }

    public int getChallenge() {
        return challenge;
    }

    public void setChallenge(int c) {
        this.challenge = c;
    }

    public void fakeRelog() {
        client.getSession().write(MaplePacketCreator.getCharInfo(this));
        final MapleMap mapp = getMap();
        mapp.setCheckStates(false);
        mapp.removePlayer(this);
        mapp.addPlayer(this);
        mapp.setCheckStates(true);
    }

    public void giveBuff(int itemID) {
        MapleItemInformationProvider.getInstance().getItemEffect(itemID).applyTo(this);
    }

    public boolean canSummon() {
        return canSummon(5000);
    }

    public boolean canSummon(int g) {
        if (lastSummonTime + g < System.currentTimeMillis()) {
            lastSummonTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int getIntNoRecord(int questID) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public int getIntRecord(int questID) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public void updatePetAuto() {
        if (getIntNoRecord(GameConstants.HP_ITEM) > 0) {
            client.getSession().write(MaplePacketCreator.petAutoHP(getIntRecord(GameConstants.HP_ITEM)));
        } else {
            client.getSession().write(MaplePacketCreator.petAutoHP(0));
        }
        if (getIntNoRecord(GameConstants.MP_ITEM) > 0) {
            client.getSession().write(MaplePacketCreator.petAutoMP(getIntRecord(GameConstants.MP_ITEM)));
        } else {
            client.getSession().write(MaplePacketCreator.petAutoMP(0));
        }
    }

    public void sendEnglishQuiz(String msg) {
        client.getSession().write(MaplePacketCreator.englishQuizMsg(msg));
    }

    public void setChangeTime() {
        mapChangeTime = System.currentTimeMillis();
    }

    public long getChangeTime() {
        return mapChangeTime;
    }

    public Map<ReportType, Integer> getReports() {
        return reports;
    }

    public void addReport(ReportType type) {
        Integer value = reports.get(type);
        reports.put(type, value == null ? 1 : (value + 1));
        changed_reports = true;
    }

    public void clearReports(ReportType type) {
        reports.remove(type);
        changed_reports = true;
    }

    public void clearReports() {
        reports.clear();
        changed_reports = true;
    }

    public final int getReportPoints() {
        int ret = 0;
        for (Integer entry : reports.values()) {
            ret += entry;
        }
        return ret;
    }

    public final String getReportSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<Pair<ReportType, Integer>> offenseList = new ArrayList<Pair<ReportType, Integer>>();
        for (final Entry<ReportType, Integer> entry : reports.entrySet()) {
            offenseList.add(new Pair<ReportType, Integer>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(offenseList, new Comparator<Pair<ReportType, Integer>>() {
            @Override
            public final int compare(final Pair<ReportType, Integer> o1, final Pair<ReportType, Integer> o2) {
                final int thisVal = o1.getRight();
                final int anotherVal = o2.getRight();
                return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        for (int x = 0; x < offenseList.size(); x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).left.name()));
            ret.append(": ");
            ret.append(offenseList.get(x).right);
            ret.append(" ");
        }
        return ret.toString();
    }

    public short getScrolledPosition() {
        return scrolledPosition;
    }

    public void setScrolledPosition(short s) {
        this.scrolledPosition = s;
    }

    public final CRand32 getCRand1() {
        return rndGenForCharacter;
    }

    public final CRand32 getCRand2() {
        return rndForCheckDamageMiss;
    }

    public final CRand32 getCRand3() {
        return rndGenForMob;
    }

    //    public MapleTrait getTrait(MapleTraitType t) {
//        return traits.get(t);
//    }
    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 9270035); //troll
    }

    public void changeTeam(int newTeam) {
        this.coconutteam = newTeam;
        client.getSession().write(MaplePacketCreator.showEquipEffect(newTeam));

    }

    public void disease(int type, int level) {
        if (MapleDisease.getBySkill(type) == null) {
            return;
        }
        chair = 0;
        client.getSession().write(MaplePacketCreator.cancelChair(-1));
        map.broadcastMessage(this, MaplePacketCreator.showChair(id, 0), false);
        giveDebuff(MapleDisease.getBySkill(type), MobSkillFactory.getMobSkill(type, level), (short) 0);
    }

    public void clearAllCooldowns() {
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            final int skil = m.skillId;
            removeCooldown(skil);
            client.getSession().write(MaplePacketCreator.skillCooldown(skil, 0));
        }
    }

    public Pair<Double, Boolean> modifyDamageTaken(double damage, MapleMapObject attacke, boolean mana_reflect) {
        Pair<Double, Boolean> ret = new Pair<Double, Boolean>(damage, false);

        final Integer div = getBuffedValue(MapleBuffStat.DIVINE_SHIELD);
        if (damage <= 0 && div == null) {
            return ret;
        }
        if (div != null) {
            if (div <= 2) {
                cancelEffectFromBuffStat(MapleBuffStat.DIVINE_SHIELD);
            } else {
                setBuffedValue(MapleBuffStat.DIVINE_SHIELD, div - 1);
                damage = 0;
            }
        }
        if (damage > 0) {
            if (getJob() == 122 && !skillisCooling(1220013)) {
                final Skill divine = SkillFactory.getSkill(1220013);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(MaplePacketCreator.skillCooldown(1220013, divineShield.getCooldown()));
                        addCooldown(1220013, System.currentTimeMillis(), divineShield.getCooldown() * 1000);
                    }
                }
            } else if (getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC) != null && getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB) != null && getBuffedValue(MapleBuffStat.PUPPET) != null) {
                double buff = getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC).doubleValue();
                double buffz = getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB).doubleValue();
                final Skill safety = SkillFactory.getSkill(35121006);
                final MapleStatEffect safety2 = safety.getEffect(getTotalSkillLevel(safety));
                if ((int) ((buff / 100.0) * getStat().getMaxHp()) <= damage) {
                    damage -= ((buffz / 100.0) * damage);
                    cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                    cancelEffectFromBuffStat(MapleBuffStat.SATELLITESAFE_PROC);
                    cancelEffectFromBuffStat(MapleBuffStat.SATELLITESAFE_ABSORB);
                    client.getSession().write(MaplePacketCreator.skillCooldown(35121006, safety2.getCooldown()));
                    addCooldown(35121006, System.currentTimeMillis(), safety2.getCooldown() * 1000);
                }
            } else if (attacke != null) {
                final MapleStatEffect MREF = getStatForBuff(MapleBuffStat.MANA_REFLECTION);
                int damr = 0;
                if (damr <= 0 && getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null && mana_reflect) {
                    damr = getBuffedSkill_X(MapleBuffStat.MANA_REFLECTION) != null ? getBuffedSkill_X(MapleBuffStat.MANA_REFLECTION) : 0;
                }
                final int bouncedam_ = damr;
                if (bouncedam_ > 0) {
                    long bouncedamage = (long) (damage * bouncedam_ / 100);
                    if (!mana_reflect) {
                    } else {
                        // 여기 마나리플렉션 이미지
                        client.getSession().write(MaplePacketCreator.showOwnBuffEffect(MREF.getSourceId(), 7, getLevel(), MREF.getLevel()));
                        map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(id, MREF.getSourceId(), 7, getLevel(), MREF.getLevel()), false);
                    }
                    if (attacke instanceof MapleMonster) {
                        final MapleMonster attacker = (MapleMonster) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / (mana_reflect ? 20 : 10));
                        //dropMessage(6, "bouncedamage2" + bouncedamage);
                        attacker.damage(this, bouncedamage, true);
                        getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage), getTruePosition());
                        checkMonsterAggro(attacker);
                    }
                    ret.right = true;
                }
            }
            if ((getJob() == 512 || getJob() == 522) && getBuffedValue(MapleBuffStat.PIRATES_REVENGE) == null) {
                final Skill divine = SkillFactory.getSkill(getJob() == 512 ? 5120011 : 5220012);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(MaplePacketCreator.skillCooldown(divine.getId(), divineShield.getX()));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getX() * 1000);
                    }
                }
            }

            if (getJob() == 312 && attacke != null) {
                client.getSession().write(MaplePacketCreator.APPLYVENGEANCE());
                //확률계산은 클라이언트에서
            }

            List<Integer> attack = attacke instanceof MapleMonster || attacke == null ? null : (new ArrayList<Integer>());
            if (getParty() != null || (getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422 || getJob() == 132) && (getBuffedValue(MapleBuffStat.SUMMON) != null || getBuffedValue(MapleBuffStat.BEHOLDER) != null) && attacke != null) {

                final List<MapleSummon> ss = getSummonsReadLock();
                try {
                    for (MapleSummon sum : ss) {
                        MapleCharacter SummonOwner = map.getCharacterById(sum.getOwnerId());
                        if ((getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422 || getJob() == 132) && (getBuffedValue(MapleBuffStat.SUMMON) != null || getBuffedValue(MapleBuffStat.BEHOLDER) != null) && attacke != null) {
                            if (sum.getTruePosition().distanceSq(getTruePosition()) < 400000.0 && (sum.getSkill() == 4111007 || sum.getSkill() == 4211007)) {
                                final List<Pair<Integer, Integer>> allDamage = new ArrayList<>();
                                if (attacke instanceof MapleMonster) {
                                    final MapleMonster attacker = (MapleMonster) attacke;
                                    final int theDmg = (int) (SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX() * damage / 100.0);
                                    allDamage.add(new Pair<>(attacker.getObjectId(), theDmg));
                                    getMap().broadcastMessage(MaplePacketCreator.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x84, allDamage, getLevel(), true));
                                    attacker.damage(this, theDmg, true);
                                    checkMonsterAggro(attacker);
                                    //dropMessage(6, "getParty()" + getParty() + "스킬데미지" + (int) SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX() + "처맞은뎀지" + damage + "계산된뎀지" + theDmg);
                                    if (!attacker.isAlive()) {
                                        getClient().getSession().write(MobPacket.killMonster(attacker.getObjectId(), 1));
                                    }
                                } else {
                                    final MapleCharacter chr = (MapleCharacter) attacke;
                                    final int dmg = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX();
                                    chr.addHP(-dmg);
                                    attack.add(dmg);
                                }
                            } else if (sum.getSkill() == 1321007) {
                                final Skill divine = SkillFactory.getSkill(1320011);
                                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())) {
                                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                                    if (divineShield.makeChanceResult()) {
                                        final List<Pair<Integer, Integer>> allDamage = new ArrayList<>();
                                        if (attacke instanceof MapleMonster) {
                                            final MapleMonster attacker = (MapleMonster) attacke;
                                            int theDmg = (int) (divineShield.getDamage() * damage / 100.0);
                                            //dropMessage(6, "스킬데미지" + divineShield.getDamage() + "처맞은뎀지" + damage + "계산된뎀지" + theDmg);
                                            allDamage.add(new Pair<>(attacker.getObjectId(), theDmg));
                                            getMap().broadcastMessage(MaplePacketCreator.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x84, allDamage, getLevel(), true));
                                            attacker.damage(this, theDmg, true);
                                            checkMonsterAggro(attacker);
                                            if (!attacker.isAlive()) {
                                                getClient().getSession().write(MobPacket.killMonster(attacker.getObjectId(), 1));
                                            }
                                            client.getSession().write(MaplePacketCreator.skillCooldown(divine.getId(), divineShield.getCooldown()));
                                            addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown() * 1000);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    unlockSummonsReadLock();
                }
            }
        }

        ret.left = damage;
        return ret;
    }

    public void onAttack(long maxhp, int maxmp, int skillid, int oid, int totDamage) {
        if (stats.hpRecoverProp > 0) {
            if (Randomizer.nextInt(100) <= stats.hpRecoverProp) {//i think its out of 100, anyway
                if (stats.hpRecover > 0) {
                    healHP(stats.hpRecover);
                }
                if (stats.hpRecoverPercent > 0) {
                    int 회복량 = (int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) stats.hpRecoverPercent / 100.0)), stats.getMaxHp() / 2));
                    addHP(회복량);
                    //dropMessage(6, "5퍼센트 확률로 피가 찼다!" + 회복량);
                }
            }
        }
        if (stats.mpRecoverProp > 0) {
            if (Randomizer.nextInt(100) <= stats.mpRecoverProp) {//i think its out of 100, anyway

                healMP(stats.mpRecover);

            }
        }
        if (stats.hpRecoverPropPotential > 0) {
            byte realProp;
            if (stats.hpRecoverPropPotential >= 15) {
                realProp = 15;
            } else {
                realProp = 3;
            }
            if (Randomizer.nextInt(100) <= realProp) {//i think its out of 100, anyway
                if (stats.hpRecoverPotential > 0) {
                    healHP(stats.hpRecoverPotential);
                }
            }
            //dropMessage(6, "HP확률" + realProp + "회복량" + stats.hpRecoverPotential);
        }
        if (stats.mpRecoverPropPotential > 0) {
            byte realProp;
            if (stats.mpRecoverPropPotential >= 15) {
                realProp = 15;
            } else {
                realProp = 3;
            }
            if (Randomizer.nextInt(100) <= realProp) {//i think its out of 100, anyway
                if (stats.mpRecoverPotential > 0) {
                    healMP(stats.mpRecoverPotential);
                }
            }
            //dropMessage(6, "MP확률" + realProp + "회복량" + stats.mpRecoverPotential);
        }
        if (getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
            addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxHp() / 2))));
        }
        // effects
        if (skillid > 0) {
            final Skill skil = SkillFactory.getSkill(skillid);
            final MapleStatEffect effect = skil.getEffect(getTotalSkillLevel(skil));
            switch (skillid) {
                case 4101005:
                case 5111004: //에너지 드레인(버커니어)
                case 14101006: //뱀파이어
                case 15111001: //에너지 드레인(스트라이커)
                case 33111006: //클로우 컷
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), stats.getMaxHp() / 2))));
                    break;
                case 5211006:
                case 5220011: //homing
                case 22151002: { //killer wing
                    clearLinkMid();
                    setLinkMid(oid, effect.getX());
                    break;
                }
                case 33101007: { //jaguar
                    clearLinkMid();
                    break;
                }
            }
        }

    }

    public void clearLinkMid() {
        linkMobs.clear();
        cancelEffectFromBuffStat(MapleBuffStat.HOMING_BEACON);
    }

    public int getFirstLinkMid() {
        for (Integer lm : linkMobs.keySet()) {
            return lm.intValue();
        }
        return 0;
    }

    public Map<Integer, Integer> getAllLinkMid() {
        return linkMobs;
    }

    public void setLinkMid(int lm, int x) {
        linkMobs.put(lm, x);
    }

    public int getDamageIncrease(int lm) {
        if (linkMobs.containsKey(lm)) {
            return linkMobs.get(lm);
        }
        return 0;
    }

    public void setMonsterBookCover(int bookCover) {
        this.bookCover = bookCover;
    }

    public int getMonsterBookCover() {
        return bookCover;
    }

    public void afterAttack(int mobCount, int attackCount, int skillid) {
        cancelEffectFromBuffStat(MapleBuffStat.INFILTRATE);
        switch (getJob()) {
            case 511:
            case 512: {
                handleEnergyCharge(5110001, 5 * mobCount * attackCount);
                break;
            }
            case 1510:
            case 1511:
            case 1512: {
                handleEnergyCharge(15100004, 5 * mobCount * attackCount);
                break;
            }
            case 400:
            case 410:
            case 411:
            case 412: {
                if (skillid != 4001003 & getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
                break;
            }
            case 1300:
            case 1310:
            case 1311:
            case 1312: {
                if (skillid != 13101006 & getBuffedValue(MapleBuffStat.WIND_WALK) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.WIND_WALK);
                }
                break;
            }
            case 1400:
            case 1410:
            case 1411:
            case 1412: {
                if (skillid != 14001003 & getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
                break;
            }
            case 111:
            case 112:
            case 1111:
            case 1112://에오스 무한 패닉 방지
                if (getBuffedValue(MapleBuffStat.COMBO) != null) { // shout should not give orbs
                    if (GameConstants.isComboSkill(skillid)) {
                        handleOrbgain();
                    }
                }
                break;
        }
        if (getBuffedValue(MapleBuffStat.OWL_SPIRIT) != null) {
            if (currentBattleshipHP() > 0) {
                decreaseBattleshipHP();
            }
            if (currentBattleshipHP() <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.OWL_SPIRIT);
            }
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(getTruePosition().x - 25, getTruePosition().y - 75, 50, 75);
    }

    public void goDonateShop(boolean bln) {
        goDonateCashShop = bln;
    }

    public boolean isDonateShop() {
        return goDonateCashShop;
    }

    public final void changeMusic(final String songName) {
        getClient().getSession().write(MaplePacketCreator.musicChange(songName));
    }

    public void toggleStrongBuff() {
        this.usingStrongBuff = !usingStrongBuff;
    }

    public boolean isStrongBuff() {
        return usingStrongBuff;
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public int getFamilyId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getFamilyId();
    }

    public int getSeniorId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getSeniorId();
    }

    public int getJunior1() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior1();
    }

    public int getJunior2() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior2();
    }

    public int getCurrentRep() {
        return currentrep;
    }

    public int getTotalRep() {
        return totalrep;
    }

    public void setCurrentRep(int _rank) {
        currentrep = _rank;
        if (mfc != null) {
            mfc.setCurrentRep(_rank);
        }
    }

    public void setTotalRep(int _rank) {
        totalrep = _rank;
        if (mfc != null) {
            mfc.setTotalRep(_rank);
        }
    }

    public int getNoJuniors() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getNoJuniors();
    }

    public MapleFamilyCharacter getMFC() {
        return mfc;
    }

    public void makeMFC(final int familyid, final int seniorid, final int junior1, final int junior2) {
        if (familyid > 0) {
            MapleFamily f = World.Family.getFamily(familyid);
            if (f == null) {
                mfc = null;
            } else {
                mfc = f.getMFC(id);
                if (mfc == null) {
                    mfc = f.addFamilyMemberInfo(this, seniorid, junior1, junior2);
                }
                if (mfc.getSeniorId() != seniorid) {
                    mfc.setSeniorId(seniorid);
                }
                if (mfc.getJunior1() != junior1) {
                    mfc.setJunior1(junior1);
                }
                if (mfc.getJunior2() != junior2) {
                    mfc.setJunior2(junior2);
                }
            }
        } else {
            mfc = null;
        }
    }

    public void setFamily(final int newf, final int news, final int newj1, final int newj2) {
        if (mfc == null || newf != mfc.getFamilyId() || news != mfc.getSeniorId() || newj1 != mfc.getJunior1() || newj2 != mfc.getJunior2()) {
            makeMFC(newf, news, newj1, newj2);
        }
    }

    public boolean canUseFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(buff.questID));
        if (stat == null) {
            return true;
        }
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Long.parseLong(stat.getCustomData()) + (24 * 3600000) < System.currentTimeMillis();
    }

    public void useFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(buff.questID));
        stat.setCustomData(String.valueOf(System.currentTimeMillis()));
    }

    public List<Integer> usedBuffs() {
        //assume count = 1
        List<Integer> used = new ArrayList<Integer>();
        MapleFamilyBuff[] z = MapleFamilyBuff.values();
        for (int i = 0; i < z.length; i++) {
            if (!canUseFamilyBuff(z[i])) {
                used.add(i);
            }
        }
        return used;
    }

    public void saveFamilyStatus() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE characters SET familyid = ?, seniorid = ?, junior1 = ?, junior2 = ? WHERE id = ?");
            if (mfc == null) {
                ps.setInt(1, 0);
                ps.setInt(2, 0);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
            } else {
                ps.setInt(1, mfc.getFamilyId());
                ps.setInt(2, mfc.getSeniorId());
                ps.setInt(3, mfc.getJunior1());
                ps.setInt(4, mfc.getJunior2());
            }
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
            se.printStackTrace();
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
        //MapleFamily.setOfflineFamilyStatus(familyid, seniorid, junior1, junior2, currentrep, totalrep, id);
    }

    public int maxBattleshipHP(int skillid) {
        return (getTotalSkillLevel(skillid) * 5000) + ((getLevel() - 120) * 3000);//빅뱅패치후 공식
    }

    public int currentBattleshipHP() {
        return battleshipHP;
    }

    public void setBattleshipHP(int v) {
        this.battleshipHP = v;
    }

    public void decreaseBattleshipHP() {
        this.battleshipHP--;
    }

    public int getFinalCut() {
        return finalcut;
    }

    public void setFinalCut(int cut) {
        finalcut = cut;
    }

    public long getFirstLoginTime() {
        return firstLoginTime;
    }

    public void setWeddingGive(int l) {
        weddingGiftGive = l;
    }

    public int getWeddingGive() {
        return weddingGiftGive;
    }

    public final void handleEnergyCharge(final int skillid, final int targets) {
        final Skill echskill = SkillFactory.getSkill(skillid);
        final int skilllevel = getTotalSkillLevel(echskill);
        if (skilllevel > 0) {
            final MapleStatEffect echeff = echskill.getEffect(skilllevel);
            if (targets > 0) {
                if (getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null) {
                    echeff.applyEnergyBuff(this, true); // Infinity time
                } else {
                    Integer energyLevel = getBuffedValue(MapleBuffStat.ENERGY_CHARGE);
                    //TODO: bar going down
                    if (energyLevel < 10000) {
                        energyLevel += (echeff.getX() * targets);

                        client.getSession().write(MaplePacketCreator.showOwnBuffEffect(skillid, 2, getLevel(), skilllevel));
                        map.broadcastMessage(this, MaplePacketCreator.showBuffeffect(id, skillid, 2, getLevel(), skilllevel), false);

                        if (energyLevel >= 10000) {
                            energyLevel = 10000;
                        }
                        client.getSession().write(TemporaryStatsPacket.giveEnergyChargeTest(energyLevel, echeff.getDuration() / 1000));
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(energyLevel));
                    } else if (energyLevel == 10000) {
                        echeff.applyEnergyBuff(this, false); // One with time
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(10001));
                    }
                }
            }
        }
    }

    public String getQuestInfo(int key) {
        MapleQuestStatus quest = getQuestNoAdd(MapleQuest.getInstance(key));
        if (quest == null) {
            return "";
        }
        String data = quest.getCustomData();
        if (data == null) {
            return "";
        }
        return data;
    }

    public void setQuestInfo(int key, String value) {
        getQuestNAdd(MapleQuest.getInstance(key)).setCustomData(value);
    }

    public boolean checkDailyLimitCount(int countKey, int dateKey, int incCount, int maxCount) {
        int today = DateUtil.today();
        String countStr = getQuestInfo(countKey);
        String dateStr = getQuestInfo(dateKey);
        int count, date;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException ex) {
            count = 0;
        }
        try {
            date = Integer.parseInt(dateStr);
        } catch (NumberFormatException ex) {
            date = 0;
        }
        if (date < today) {
            count = 0;
            date = today;
        }
        boolean result = count < maxCount;
        if (result) {
            count += incCount;
        }
        setQuestInfo(countKey, Integer.toString(count));
        setQuestInfo(dateKey, Integer.toString(date));
        return result;
    }

    public long remainingCooltime(int key, long time) {
        long ctm = System.currentTimeMillis();
        String info = getQuestInfo(key);
        long infol;
        try {
            infol = Long.parseLong(info);
        } catch (Exception ex) {
            infol = ctm;
        }
        if (infol > ctm) {
            return infol - ctm;
        }
        if (time != 0) {
            setQuestInfo(key, Long.toString(ctm + time));
        }
        return 0;
    }

    public boolean checkCooltime(int key) {
        long t = remainingCooltime(key, 0);
        return t > 0;
    }

    public String remainingCooltime(int key) {
        long t = remainingCooltime(key, 0);
        if (t == 0) {
            return "0";
        } else {
            return StringUtil.getReadableMillis(0, t);
        }
    }

    public boolean cooltime(int key, long time) {
        return remainingCooltime(key, time) == 0;
    }

    public int getPossibleReports() {
        return possibleReports;
    }

    public void decreaseReports() {
        this.possibleReports--;
    }

    public static int getIdByName(String name) {
        try {
            int id;
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return -1;
                    }
                    id = rs.getInt("id");
                }
            } finally {
                con.close();
            }
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private transient MapleLieDetector antiMacro;

    public final MapleLieDetector getAntiMacro() {
        return antiMacro;
    }

    public CalcDamage getCalcDamage() {
        return calcDamage;
    }

    public void setDailyQuestBonus(int status, int count) {
        getQuestNAdd(MapleQuest.getInstance(GameConstants.DAILY_QUEST_BONUS)).setCustomData(status + "/" + count);
    }

    public String getDailyQuestBonus() {
        MapleQuestStatus mqs = getQuestNAdd(MapleQuest.getInstance(GameConstants.DAILY_QUEST_BONUS));
        return mqs.getCustomData();
    }

    public int getDailyQuestBonus(int type) {
        MapleQuestStatus mqs = getQuestNAdd(MapleQuest.getInstance(GameConstants.DAILY_QUEST_BONUS));
        return Integer.parseInt(mqs.getCustomData().split("/")[type - 1]);
        // 0 accid, 1 status, 2 count
    }

    public void giveHolySymbol(int duration) {
        final MapleStatEffect eff = SkillFactory.getSkill(9001002).getEffect(1);
        this.holySymbol = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        holySymbol.put(MapleBuffStat.HOLY_SYMBOL, 50);
        this.cancelEffect(eff, -1, holySymbol, true, false);
        this.getClient().getSession().write(TemporaryStatsPacket.giveBuff(9001002, duration, holySymbol, null));
        final long starttime = System.currentTimeMillis();
        final MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(this, eff, starttime, holySymbol);
        final ScheduledFuture<?> schedule = Timer.BuffTimer.getInstance().schedule(cancelAction, duration);
        this.registerEffect(eff, starttime, schedule, holySymbol, false, duration, this.getId());
    }

    public static void initDailyQuestBonus() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM dailyquestbonus");
            rs = ps.executeQuery();
            while (rs.next()) {
                ps = con.prepareStatement("UPDATE dailyquestbonus SET status = 0, count = 0");
                ps.executeUpdate();
            }
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public String getBanJum(Long S) {
        StringBuilder SS = new StringBuilder("" + S);
        if (S < 0) {
            return SS.toString();
        }
        SS.reverse();
        int SSS = 0;
        for (int i = 0; i < SS.length(); i++) {
            SSS++;
            if (SSS == 4) {
                SS.insert(i, ",");
                SSS = 0;
            }
        }
        SS.reverse();
        return SS.toString();
    }

    public String getJobName(int job) {
        switch (job) {
            case 0:
                return "초보자";
            case 100:
                return "전사";
            case 110:
                return "파이터";
            case 111:
                return "크루세이더";
            case 112:
                return "히어로";
            case 120:
                return "페이지";
            case 121:
                return "나이트";
            case 122:
                return "팔라딘";
            case 130:
                return "스피어맨";
            case 131:
                return "용기사";
            case 132:
                return "다크나이트";
            case 200:
                return "마법사";
            case 210:
                return "위자드(불,독)";
            case 211:
                return "메이지(불,독)";
            case 212:
                return "아크메이지(불,독)";
            case 220:
                return "위자드(썬,콜)";
            case 221:
                return "메이지(썬,콜)";
            case 222:
                return "아크메이지(썬,콜)";
            case 230:
                return "클레릭";
            case 231:
                return "프리스트";
            case 232:
                return "비숍";
            case 300:
                return "궁수";
            case 310:
                return "헌터";
            case 311:
                return "레인저";
            case 312:
                return "보우마스터";
            case 320:
                return "사수";
            case 321:
                return "저격수";
            case 322:
                return "신궁";
            case 400:
                return "도적";
            case 410:
                return "어쌔신";
            case 411:
                return "허밋";
            case 412:
                return "나이트로드";
            case 420:
                return "시프";
            case 421:
                return "시프마스터";
            case 422:
                return "섀도어";
            case 430:
                return "세미듀어러";
            case 431:
                return "듀어러";
            case 432:
                return "듀얼마스터";
            case 433:
                return "슬래셔";
            case 434:
                return "듀얼블레이더";
            case 500:
                return "해적";
            case 510:
                return "인파이터";
            case 511:
                return "버커니어";
            case 512:
                return "바이퍼";
            case 520:
                return "건슬링거";
            case 521:
                return "발키리";
            case 522:
                return "캡틴";
            case 800:
                return "매니저";
            case 900:
                return "GM";
            case 1000:
                return "시그너스";
            case 1100:
                return "소울마스터 1차";
            case 1110:
                return "소울마스터 2차";
            case 1111:
                return "소울마스터 3차";
            case 1112:
                return "소울마스터 4차";
            case 1200:
                return "플레임위자드 1차";
            case 1210:
                return "플레임위자드 2차";
            case 1211:
                return "플레임위자드 3차";
            case 1212:
                return "플레임위자드 4차";
            case 1300:
                return "윈드브레이커 1차";
            case 1310:
                return "윈드브레이커 2차";
            case 1311:
                return "윈드브레이커 3차";
            case 1312:
                return "윈드브레이커 4차";
            case 1400:
                return "나이트워커 1차";
            case 1410:
                return "나이트워커 2차";
            case 1411:
                return "나이트워커 3차";
            case 1412:
                return "나이트워커 4차";
            case 1500:
                return "스트라이커 1차";
            case 1510:
                return "스트라이커 2차";
            case 1511:
                return "스트라이커 3차";
            case 1512:
                return "스트라이커 4차";
            case 2000:
                return "레전드 (아란)";
            case 2100:
                return "아란 1차";
            case 2110:
                return "아란 2차";
            case 2111:
                return "아란 3차";
            case 2112:
                return "아란 4차";
            case 2001:
                return "레전드 (에반)";
            case 2200:
                return "에반 1차";
            case 2210:
                return "에반 2차";
            case 2211:
                return "에반 3차";
            case 2212:
                return "에반 4차";
            case 2213:
                return "에반 5차";
            case 2214:
                return "에반 6차";
            case 2215:
                return "에반 7차";
            case 2216:
                return "에반 8차";
            case 2217:
                return "에반 9차";
            case 2218:
                return "에반 10차";
            case 3000:
                return "시티즌";
            case 3200:
                return "배틀메이지 1차";
            case 3210:
                return "배틀메이지 2차";
            case 3211:
                return "배틀메이지 3차";
            case 3212:
                return "배틀메이지 4차";
            case 3300:
                return "와일드헌터 1차";
            case 3310:
                return "와일드헌터 2차";
            case 3311:
                return "와일드헌터 3차";
            case 3312:
                return "와일드헌터 4차";
            case 3500:
                return "메카닉 1차";
            case 3510:
                return "메카닉 2차";
            case 3511:
                return "메카닉 3차";
            case 3512:
                return "메카닉 4차";
            default:
                return "알 수 없음";
        }
    }
    //PC방
    private int pcdate;
    private long pctime;
    private boolean pccheck, pcbang;

    public long getPcTime() {
        return pctime;
    }

    public void setPcTime(Long i) {
        pctime = System.currentTimeMillis() + i;
        pccheck = true;
        pcbang = true;
    }

    public void addPcTime(Long i) {
        pctime += i;
        pccheck = true;
        pcbang = true;
    }

    public void clearPc(boolean bool) {
        pccheck = bool;
        pcbang = bool;
    }

    public int getPcDate() {
        return pcdate;
    }

    public void setPcDate(int i) {
        pcdate = i;
    }

    public int getCalcPcTime() {
        if (pctime == 0 || pctime < System.currentTimeMillis()) {
            return 0;
        }
        return (int) ((pctime - System.currentTimeMillis()) / 60000);
    }

    public boolean checkPcTime() {
        if (pctime > System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    public void getPcManager() {
        if (checkPcTime()) {
            if (pccheck) {
                if (getCalcPcTime() == 10) {
                    pccheck = false;
                    dropMessage(5, "PC방 정량제 잔여시간이 10분 남았습니다.");
                }
            }
        } else {
            if (pcbang) {
                dropMessage(5, "PC방 정량제 잔여시간이 남아있지 않아 PC방 혜택 및 장비 아이템들이 모두 사라집니다.");
                removePCitem();
                getClient().getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, getCalcPcTime()));
                pcbang = false;
                if (getMapId() >= 190000000 && getMapId() <= 198000000) {
                    int returnMap = getSavedLocation(SavedLocationType.ARDENTMILL);
                    final MapleMap mapp = getClient().getChannelServer().getMapFactory().getMap(returnMap);
                    changeMap(mapp);
                    clearSavedLocation(SavedLocationType.ARDENTMILL);
                    //dropMessage(5, "나가 이썌끼야");
                }
            }
        }
    }

    public void removePCitem() {
        removeAllEquip(1142145, true);//피시방 훈장
        for (int i = 1302050; i <= 1302054; ++i) { // 한손검
            removeAllEquip(i, true);
        }
        for (int i = 1312026; i <= 1312029; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1312046; i <= 1312050; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1332043; i <= 1332047; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1372027; i <= 1372030; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1382030; i <= 1382034; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1402030; i <= 1402034; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1412022; i <= 1412025; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1422023; i <= 1422026; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1432031; i <= 1432035; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1442040; i <= 1442043; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1452038; i <= 1452042; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1462033; i <= 1462037; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1472045; i <= 1472049; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1482015; i <= 1482019; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1492015; i <= 1492019; ++i) {
            removeAllEquip(i, true);
        }
        /*메이플 무기*/
        for (int i = 1302124; i <= 1302126; ++i) {
            removeAllEquip(i, true);
        }
        removeAllEquip(1312046, true);
        removeAllEquip(1322074, true);
        removeAllEquip(1402068, true);
        for (int i = 1412043; i <= 1412044; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1422046; i <= 1422047; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1432058; i <= 1432059; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1442083; i <= 1442084; ++i) {
            removeAllEquip(i, true);
        }
        removeAllEquip(1372056, true);
        for (int i = 1382074; i <= 1382076; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1452077; i <= 1452079; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1462069; i <= 1462071; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1472093; i <= 1472095; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1332093; i <= 1332095; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1492042; i <= 1492044; ++i) {
            removeAllEquip(i, true);
        }
        for (int i = 1482041; i <= 1482043; ++i) {
            removeAllEquip(i, true);
        }
    }

    public void saveToPC() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM c_pctime WHERE acc = ?");
            ps.setInt(1, getAccountID());
            rs = ps.executeQuery();
            if (rs.next()) {
                ps.close();
                rs.close();
                ps = con.prepareStatement("UPDATE c_pctime SET time = ?, date = ? WHERE acc = ?");
                ps.setLong(1, pctime);
                ps.setInt(2, pcdate);
                ps.setInt(3, getAccountID());
                ps.executeUpdate();
            } else {
                ps.close();
                rs.close();
                ps = con.prepareStatement("INSERT INTO c_pctime (acc, time, date) VALUES(?, ?, ?)");
                ps.setInt(1, getAccountID());
                ps.setLong(2, pctime);
                ps.setInt(3, pcdate);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("PC err...");
            e.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public String searchMob(String search) {
        StringBuilder Text = new StringBuilder().append("아래의 몬스터가 검색되었습니다.\r\n");
        String query = search.replace(" ", "").toLowerCase();
        if (query.isEmpty()) {
            return "";
        }
        Map<Integer, String> searchMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : MapleMonsterStats.MobNameMap.entrySet()) {
            int key = entry.getKey();
            String value = entry.getValue();
            switch (key) { //검색 대상 제외몹
                case 9999998:
                case 9999999:
                    continue;
            }
            String r = value.replace(" ", "").toLowerCase();
            if (r.contains(query)) {
                searchMap.put(key, value);
            }
        }
        if (searchMap.isEmpty()) {
            return "존재하지 않는 몬스터 입니다.";
        }
        for (Map.Entry<Integer, String> entry : searchMap.entrySet()) {
            int key = entry.getKey();
            String value = entry.getValue();
            Text.append("\r\n#b").append("#L").append(key).append("#").append(value).append(" [").append(key).append("]").append("#l");
        }
        return Text.toString();
    }

    public String checkDrop(int mobId) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        ArrayList<Integer> ret2 = new ArrayList<Integer>();
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        Collections.sort(ranks, new Comparator<MonsterDropEntry>() {
            @Override
            public final int compare(final MonsterDropEntry o1, final MonsterDropEntry o2) {
                final int thisVal = o1.itemId;
                final int anotherVal = o2.itemId;
                return (thisVal > anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        if (ranks != null && ranks.size() > 0) {
            int num = 0, itemId = 0;
            double ch = 0;
            double droprate = RateManager.DROP;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            StringBuilder name2 = new StringBuilder();
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    if (num == 0) {
                        MapleMonster mob = null;
                        mob = MapleLifeFactory.getMonster(mobId);
                        if (mob == null) {
                            return "존재하지 않는 몬스터입니다.";
                        }
                        switch (mobId) {
                            case 9400409://두꺼비 영주
                            case 8810122:
                            case 8800102:
                                name.append("   #o" + mobId + "#은(는) 이미지가 너무 커 생략\r\n\r\n");
                                break;
                            case 8220013: // 니벨룽
                            case 8220014: // 니벨룽
                            case 8220015: // 니벨룽
                            case 8800002:
                            case 8810018:
                                name.append("#fMob/" + mobId + ".img/info/default/0#\r\n");
                                break;
                            case 8510000:
                            case 8520000:
                            case 9300294: // 능력의 피아누스
                                name.append("#fMob/8510000.img/info/default/0#\r\n");
                                break;
                            default:
                                name.append("   ");
                                if (mob.getStats().getFly()) {
                                    if (mob.getStats().getLink() > 0) {
                                        if (mob.getStats().getLink() < 1000000) {
                                            name.append("#fMob/0" + mob.getStats().getLink() + ".img/fly/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                        } else {
                                            name.append("#fMob/" + mob.getStats().getLink() + ".img/fly/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                        }
                                    } else if (mobId < 1000000) {
                                        name.append("#fMob/0" + mobId + ".img/fly/0#\r\n");
                                    } else {
                                        name.append("#fMob/" + mobId + ".img/fly/0#\r\n");
                                    }
                                } else if (mob.getStats().getLink() > 0) {
                                    if (mob.getStats().getLink() < 1000000) {
                                        name.append("#fMob/0" + mob.getStats().getLink() + ".img/stand/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                    } else {
                                        name.append("#fMob/" + mob.getStats().getLink() + ".img/stand/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                    }
                                } else if (mobId < 1000000) {
                                    name.append("#fMob/0" + mobId + ".img/stand/0#\r\n");
                                } else {
                                    name.append("#fMob/" + mobId + ".img/stand/0#\r\n");
                                }
                                break;
                        }
                        name.append("   #b" + mob.getStats().getName() + "#k (Lv. " + mob.getStats().getLevel() + ") (몬스터 코드 : " + mob.getId() + ")\r\n\r\n");
                        name.append("   체력 : " + getBanJum(mob.getStats().getHp()) + " / 마나 : " + getBanJum((long) mob.getStats().getMp()) + "\r\n");
                        name.append("   물리 : " + mob.getStats().getPhysicalAttack() + " / 마법 : " + mob.getStats().getMagicAttack() + " / 물방 : " + mob.getStats().getPDDamage() + " / 마방 : " + mob.getStats().getMDDamage() + " / 방어율 : " + mob.getStats().getPDRate() + "\r\n");
                        name.append("   명중 : " + mob.getStats().getAcc() + " / 회피 : " + mob.getStats().getEva() + " / ");
                        name.append("경험치 : " + getBanJum((long) (mob.getStats().getExp() * RateManager.EXP)) + "\r\n\r\n");
                        name.append("   #r아래 아이템을 눌러서 다른 정보도 확인해보세요.#k\r\n");
                    }
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        name2.append((de.Minimum * RateManager.MESO) + " ~ " + (de.Maximum * RateManager.MESO) + " 메소");
                    } else {
                        name2.append("#z" + itemId + "#");
                    }
                    ch = de.chance * droprate / 10000.0;
                    if (!ret.contains(itemId)) {
                        if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                            name.append("#L" + itemId + "#" + (num + 1) + ") #i" + itemId + ":# " + name2.toString() + " - ");
                        } else {
                            name.append("#L" + 4031041 + "#" + (num + 1) + ") #i4031039:# " + itemId + " wz에 없음 - ");
                        }
                        ret.add(itemId);
                        if (ch > 100) {
                            name.append("100%#l");
                        } else if (ch < 0.001) {
                            name.append("0.000" + (int) ch + "%#l");
                        } else {
                            if ((int) ch == ch) {
                                name.append((int) ch + "%#l");
                            } else {
                                name.append(ch + "%#l");
                            }
                        }
                        name.append((de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("\r\n\r\n				(퀘스트 : " + MapleQuest.getInstance(de.questid).getName() + ")") : "") + "\r\n");
                        num++;
                    } else {
                        if (!ret2.contains(itemId)) {
                            if (num < 10) {
                                name.append("\r\n		#i" + itemId + ":# " + name2.toString() + " - ");
                            } else {
                                name.append("\r\n		  #i" + itemId + ":# " + name2.toString() + " - ");
                            }
                            ret2.add(itemId);
                        } else if (num < 10) {
                            name.append("		#i" + itemId + ":# " + name2.toString() + " - ");
                        } else {
                            name.append("		  #i" + itemId + ":# " + name2.toString() + " - ");
                        }
                        if (ch > 100) {
                            name.append("100%#l");
                        } else if (ch < 0.001) {
                            name.append("0.000" + (int) ch + "%#l");
                        } else {
                            if ((int) ch == ch) {
                                name.append((int) ch + "%#l");
                            } else {
                                name.append(ch + "%#l");
                            }
                        }
                        name.append((de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("\r\n\r\n				(퀘스트 : " + MapleQuest.getInstance(de.questid).getName() + ")") : "") + "\r\n");
                    }
                }
                name2.setLength(0);
            }
            if (name.length() > 0) {
                return name.toString();
            }
        }
        return "아무것도 드롭하지 않는 몬스터입니다.";
    }

    public String SearchDropMonster(int itemid) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        StringBuilder text = new StringBuilder(" #i" + itemid + ":# #b#z" + itemid + "##k (코드 : " + itemid + ")\r\n\r\n");
        double chance = 0;
        double droprate = RateManager.DROP;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM drop_data WHERE itemid = ?");
            ps.setInt(1, itemid);
            rs = ps.executeQuery();
            MapleMonster mob = null;
            List<MonsterDropEntry> ranks = new ArrayList<>();
            List<Integer> mobids = new ArrayList<>();
            while (rs.next()) {
                if (!ret.contains(rs.getInt("dropperid"))) {
                    ret.add(rs.getInt("dropperid"));
                    mob = MapleLifeFactory.getMonster(rs.getInt("dropperid"));
                    if (mob == null) {
                        continue;
                    }
                    mobids.add(mob.getId());
                }
            }
            Collections.sort(mobids);
            for (int zz : mobids) {
                mob = MapleLifeFactory.getMonster(zz);
                ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mob.getId());
                for (MonsterDropEntry de : ranks) {
                    if (de.itemId == itemid) {
                        chance = de.chance;
                    }
                }
                text.append("   #b" + mob.getStats().getName() + "#k (Lv. " + mob.getStats().getLevel() + ") (몬스터 코드 : " + mob.getId() + ")\r\n\r\n");
                text.append("   몬스터 체력 : " + getBanJum(mob.getStats().getHp()) + "\r\n");
                text.append("   획득 경험치 : " + getBanJum((long) (mob.getStats().getExp() * RateManager.EXP)) + "\r\n");
                chance = chance / 10000.0;
                if (chance > 100) {
                    text.append("   드롭 확률 : 100%\r\n");
                } else if (chance < 0.001) {
                    text.append("   드롭 확률 : " + chance + "%\r\n");
                } else {
                    if ((int) chance == chance) {
                        text.append("   드롭 확률 : " + (int) chance + "%\r\n");
                    } else {
                        text.append("   드롭 확률 : " + chance + "%\r\n");
                    }
                }
                text.append("#r#L" + mob.getId() + "#" + mob.getStats().getName() + "의 전체 드롭 목록 보기#k#l\r\n\r\n\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
        if (text.length() < 47) {
            return "해당 아이템을 드롭하는 몬스터가 없습니다.";
        }
        return text.toString();
    }

    public String QuestDropMonster() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        MapleMonster mob = null;
        StringBuilder text = new StringBuilder();
        double chance = 0;
        int count = 0;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM drop_data WHERE questid > 0 && dropperid >= 8180000");
            rs = ps.executeQuery();
            while (rs.next()) {
                mob = MapleLifeFactory.getMonster(rs.getInt("dropperid"));
                if (mob == null) {
                    continue;
                }
                count++;
                text.append("#b" + mob.getStats().getName() + "#k (몬스터 코드 : " + mob.getId() + ")\r\n");
                chance = (double) ((double) 100 * ((double) rs.getInt("chance") * (double) RateManager.DROP) / (double) 1000000);
                text.append("#b#z" + rs.getInt("itemid") + "##k");
                if (chance > 100) {
                    text.append(" - 드롭 확률 : 100%");
                } else if (chance < 0.001) {
                    text.append(" - 드롭 확률 : 0.000" + rs.getInt("chance") + "%");
                } else {
                    text.append(" - 드롭 확률 : " + chance + "%");
                }
                text.append("\r\n\r\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
        if (text.length() < 5) {
            return "해당 아이템을 드롭하는 몬스터가 없습니다.";
        }
        return "count : " + count + "\r\n" + text.toString();
    }

    public String globalDrop() {
        final List<MonsterGlobalDropEntry> global = MapleMonsterInformationProvider.getInstance().getGlobalDrop();
        StringBuilder Text = new StringBuilder();
        StringBuilder Text2 = new StringBuilder();
        Text.append("모든 몬스터에게 나오는 드롭정보입니다.\r\n");
        double chance = 0;
        int itemid = 0;
        if (global.size() > 0) {
            for (int i = 0; i < global.size(); i++) {
                chance = (double) ((double) 100 * ((double) global.get(i).chance * (double) RateManager.DROP) / (double) 1000000);
                if (chance > 100) {
                    Text2.append("- 100%");
                } else if (chance < 0.001) {
                    Text2.append("- 0.000" + global.get(i).chance + "%");
                } else {
                    Text2.append("- " + chance + "%");
                }
                itemid = global.get(i).itemId;
                if (itemid == 0) {
                    itemid = 4031041;
                }
                if (!MapleItemInformationProvider.getInstance().itemExists(itemid)) {
                    itemid = 4031039;
                }
                Text.append("\r\n" + (1 + i) + ") #i" + itemid + ":# #b#z" + itemid + "##k " + Text2.toString() + " (" + global.get(i).Minimum + "개 ~ " + global.get(i).Maximum + "개)");
                Text2.setLength(0);
            }
            return Text.toString();
        }
        return "글로벌 드롭이 존재하지 않습니다.";
    }

    public String SearchDropItems(String dropItemName) {
        StringBuilder Text = new StringBuilder("아래의 아이템들이 검색되었어요.\r\n");
        if (dropItemName.length() < 2) {
            return "두 글자는 입력해주셔야 해요.";
        }
        String query = dropItemName.replace(" ", "").toLowerCase();
        if (query.isEmpty()) {
            return "";
        }
        Collection<ItemInformation> itemCollection = MapleItemInformationProvider.getInstance().getAllItems();
        final List<ItemInformation> itemlist = new ArrayList<>();
        for (ItemInformation itemPair : itemCollection) {
            itemlist.add(itemPair);
        }
        Collections.sort(itemlist, new Comparator<ItemInformation>() {
            @Override
            public final int compare(final ItemInformation o1, final ItemInformation o2) {
                final int thisVal = o1.itemId;
                final int anotherVal = o2.itemId;
                return (thisVal > anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        Map<Integer, String> searchMap = new HashMap<>();
        for (ItemInformation itemPair : itemlist) {
            String r = itemPair.name.replace(" ", "").toLowerCase();
            if (r.contains(query)) {
                searchMap.put(itemPair.itemId, itemPair.name);
            }
        }
        for (Map.Entry<Integer, String> entry : searchMap.entrySet()) {
            int itemid = entry.getKey();
            if (itemid / 10000 != 238 && itemid / 10000 != 380) {
                String itemname = entry.getValue();
                Text.append("#L" + itemid + "##v" + itemid + "# #b#z" + itemid + "##k (코드 : " + itemid + ")\r\n");
            }
        }
        if (Text.length() < 20) {
            return "검색된 아이템이 없습니다.";
        }
        return Text.toString();
    }

    public void questTimeLimit(final MapleQuest quest, int seconds) {
        registerQuestExpire(quest, seconds * 1000);
        getClient().getSession().write(MaplePacketCreator.addQuestTimeLimit(quest.getId(), seconds * 1000));
    }

    public void questTimeLimit2(final MapleQuest quest, long expires) {
        long timeLeft = expires - System.currentTimeMillis();

        if (timeLeft <= 0) {
            expireQuest(quest);
        } else {
            registerQuestExpire(quest, timeLeft);
        }
    }

    private Map<MapleQuest, Long> questExpirations = new LinkedHashMap<>();
    private ScheduledFuture<?> questExpireTask = null;

    private void registerQuestExpire(MapleQuest quest, long time) {
        //evtLock.lock();
        try {
            if (questExpireTask == null) {
                questExpireTask = EventTimer.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        runQuestExpireTask();
                    }
                }, 10 * 1000);
            }

            questExpirations.put(quest, System.currentTimeMillis() + time);
        } finally {
            //evtLock.unlock();
        }
    }

    private void runQuestExpireTask() {
        //evtLock.lock();
        try {
            long timeNow = System.currentTimeMillis();
            List<MapleQuest> expireList = new LinkedList<>();

            for (Entry<MapleQuest, Long> qe : questExpirations.entrySet()) {
                if (qe.getValue() <= timeNow) {
                    expireList.add(qe.getKey());
                }
            }

            if (!expireList.isEmpty()) {
                for (MapleQuest quest : expireList) {
                    expireQuest(quest);
                    questExpirations.remove(quest);
                }

                if (questExpirations.isEmpty()) {
                    questExpireTask.cancel(false);
                    questExpireTask = null;
                }
            }
        } finally {
            //evtLock.unlock();
        }
    }

    private void expireQuest(MapleQuest quest) {
        if (getQuestStatus(quest.getId()) == 2) {
            return;
        }
        if (System.currentTimeMillis() < getMapleQuestStatus(quest.getId()).getExpirationTime()) {
            return;
        }

        getClient().getSession().write(MaplePacketCreator.questExpire(quest.getId()));
        getClient().getSession().write(MaplePacketCreator.removeQuestTimeLimit(quest.getId()));
        MapleQuestStatus newStatus = new MapleQuestStatus(quest, 0);
        newStatus.setForfeited(getQuest(quest).getForfeited() + 1);
        updateQuest(newStatus);
    }

    public final MapleQuestStatus getMapleQuestStatus(final int quest) {
        synchronized (quests) {
            for (final MapleQuestStatus q : quests.values()) {
                if (q.getQuest().getId() == quest) {
                    return q;
                }
            }
            return null;
        }
    }

    public int getSwallowedMobID() {
        return swallowedMobId;
    }

    public void setSwallowedMobID(int mobid) {
        swallowedMobId = mobid;
    }

    public final int getInvSlots(final int i) {
        return (getInventory(MapleInventoryType.getByType((byte) i)).getNumFreeSlot());
    }

    public final boolean canHoldSlots(final int slot) {
        for (int i = 1; i <= 5; i++) {
            if (getInventory(MapleInventoryType.getByType((byte) i)).isFull(slot)) {
                return false;
            }
        }
        return true;
    }

    public int getHairCoupon() {
        return hairCoupon;
    }

    public void setHairCoupon(int hairCoupon) {
        this.hairCoupon = hairCoupon;
    }

    public int getExtraDamage() {
        return extraDamage;
    }

    public void setExtraDamage(int extraDamage) {
        this.extraDamage = extraDamage;
    }

    public long getLastBattleTime() {
        return lastBattleTime;
    }

    public void setLastBattleTime(long lastBattleTime) {
        this.lastBattleTime = lastBattleTime;
    }

    public void loadMagnetSettings() { //자석 온오프 템제한
        if (magnetLoaded) {
            return;
        }
        magnetLoaded = true;

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        java.sql.ResultSet rs = null;

        try {
            con = database.DatabaseConnection.getConnection();
            ps = con.prepareStatement(
                    "SELECT banitemid, value FROM magnetitemban WHERE characterid = ?"
            );
            ps.setInt(1, this.id);
            rs = ps.executeQuery();

            while (rs.next()) {
                final int itemId = rs.getInt("banitemid");
                final int val = rs.getInt("value");
                if (itemId == 0) {
                    magnetEnabled = (val == 1);
                } else if (val == 1) {
                    magnetBanItems.add(itemId);
                }
            }
        } catch (Exception e) {
            // 로드 실패 시: 기본 OFF 유지
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
        }
    }

    private void ensureMagnetLoaded() {
        if (!magnetLoaded) {
            loadMagnetSettings();
        }
    }

    public boolean isMagnetEnabled() {
        ensureMagnetLoaded();
        return magnetEnabled;
    }

    public boolean isMagnetBanned(final int itemId) {
        ensureMagnetLoaded();
        return magnetBanItems.contains(itemId);
    }

    public java.util.List<Integer> getMagnetBanList() {
        ensureMagnetLoaded();
        return new java.util.ArrayList<Integer>(magnetBanItems);
    }

    public void setMagnetEnabled(final boolean enabled) {
        ensureMagnetLoaded();
        magnetEnabled = enabled;

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        try {
            con = database.DatabaseConnection.getConnection();
            // banitemid=0 row upsert
            ps = con.prepareStatement(
                    "INSERT INTO magnetitemban (characterid, banitemid, value) VALUES (?, 0, ?) "
                    + "ON DUPLICATE KEY UPDATE value = VALUES(value)"
            );
            ps.setInt(1, this.id);
            ps.setInt(2, enabled ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    public void addMagnetBanItem(final int itemId) {
        if (itemId <= 0) {
            return;
        }
        ensureMagnetLoaded();
        magnetBanItems.add(itemId);

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        try {
            con = database.DatabaseConnection.getConnection();
            ps = con.prepareStatement(
                    "INSERT INTO magnetitemban (characterid, banitemid, value) VALUES (?, ?, 1) "
                    + "ON DUPLICATE KEY UPDATE value = 1"
            );
            ps.setInt(1, this.id);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    public void removeMagnetBanItem(final int itemId) {
        if (itemId <= 0) {
            return;
        }
        ensureMagnetLoaded();
        magnetBanItems.remove(itemId);

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        try {
            con = database.DatabaseConnection.getConnection();
            // "삭제"로 처리 (원하면 value=0 업데이트로 바꿔도 됨)
            ps = con.prepareStatement(
                    "DELETE FROM magnetitemban WHERE characterid = ? AND banitemid = ?"
            );
            ps.setInt(1, this.id);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    public boolean tryMagnetVacuum(final long cooldownMs) {
        final long now = System.currentTimeMillis();
        while (true) {
            final long prev = lastMagnetVacuumTime.get();
            if (now - prev < cooldownMs) {
                return false;
            }
            if (lastMagnetVacuumTime.compareAndSet(prev, now)) {
                return true;
            }
        }
    }

    public void loadOneClickCubeSettings() {
        if (oneClickCubeLoaded) {
            return;
        }
        oneClickCubeLoaded = true;

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        java.sql.ResultSet rs = null;

        try {
            con = database.DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT prefkey, value FROM oneclickcube_pref WHERE characterid = ?");
            ps.setInt(1, this.id);
            rs = ps.executeQuery();

            while (rs.next()) {
                final int key = rs.getInt("prefkey");
                final int val = rs.getInt("value");
                if (key == 0) {
                    oneClickCubeEnabled = (val == 1);
                } else if (key >= 1 && key <= 3) {
                    oneClickCubeDesired[key - 1] = val;
                } else if (key == 6) {
                    oneClickCubeDupMajorEnabled = (val == 1);
                }
            }
        } catch (Exception e) {
            // 실패 시 기본 OFF + 0 유지
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
        }
    }

    private void ensureOneClickCubeLoaded() {
        if (!oneClickCubeLoaded) {
            loadOneClickCubeSettings();
        }
    }

    public boolean isOneClickCubeEnabled() {
        ensureOneClickCubeLoaded();
        return oneClickCubeEnabled;
    }

    public void setOneClickCubeEnabled(final boolean enabled) {
        ensureOneClickCubeLoaded();
        oneClickCubeEnabled = enabled;

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        try {
            con = database.DatabaseConnection.getConnection();
            ps = con.prepareStatement(
                    "INSERT INTO oneclickcube_pref (characterid, prefkey, value) VALUES (?, 0, ?) "
                    + "ON DUPLICATE KEY UPDATE value = VALUES(value)"
            );
            ps.setInt(1, this.id);
            ps.setInt(2, enabled ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    public boolean isOneClickCubeDupMajorEnabled() {
        ensureOneClickCubeLoaded();
        return oneClickCubeDupMajorEnabled;
    }

    public void setOneClickCubeDupMajorEnabled(final boolean enabled) {
        ensureOneClickCubeLoaded();
        oneClickCubeDupMajorEnabled = enabled;

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        try {
            con = database.DatabaseConnection.getConnection();
            ps = con.prepareStatement(
                    "INSERT INTO oneclickcube_pref (characterid, prefkey, value) VALUES (?, 6, ?) "
                    + "ON DUPLICATE KEY UPDATE value = VALUES(value)"
            );
            ps.setInt(1, this.id);
            ps.setInt(2, enabled ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    // (추가) confirm NPC에서 사용
    public void clearPendingOneClickCube() {
        pendingOneClickCube = false;
        pendingOneClickCubeItemId = 0;
        pendingOneClickCubeReason = null;
    }

    public boolean hasPendingOneClickCube() {
        return pendingOneClickCube;
    }

    public String getPendingOneClickCubeReason() {
        return pendingOneClickCubeReason;
    }

    public void setPendingOneClickCube(final int oneClickItemId, final String reason, final byte slot) {
        pendingOneClickCube = true;
        pendingOneClickCubeItemId = oneClickItemId;
        pendingOneClickCubeReason = reason;
        pendingOneClickCubeSlot = slot;
    }

    public int getOneClickCubeDesired(final int slot1to3) {
        ensureOneClickCubeLoaded();
        if (slot1to3 < 1 || slot1to3 > 3) {
            return 0;
        }
        return oneClickCubeDesired[slot1to3 - 1];
    }

    public void setOneClickCubeDesired(final int slot1to3, final int optionKey) {
        ensureOneClickCubeLoaded();
        if (slot1to3 < 1 || slot1to3 > 3) {
            return;
        }

        oneClickCubeDesired[slot1to3 - 1] = optionKey;

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        try {
            con = database.DatabaseConnection.getConnection();
            ps = con.prepareStatement(
                    "INSERT INTO oneclickcube_pref (characterid, prefkey, value) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE value = VALUES(value)"
            );
            ps.setInt(1, this.id);
            ps.setInt(2, slot1to3);
            ps.setInt(3, optionKey);
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    public void resetOneClickCubeDesired() {
        ensureOneClickCubeLoaded();
        oneClickCubeDesired[0] = oneClickCubeDesired[1] = oneClickCubeDesired[2] = 0;

        java.sql.Connection con = null;
        java.sql.PreparedStatement ps = null;
        try {
            con = database.DatabaseConnection.getConnection();
            ps = con.prepareStatement("DELETE FROM oneclickcube_pref WHERE characterid = ? AND prefkey BETWEEN 1 AND 3");
            ps.setInt(1, this.id);
            ps.executeUpdate();
        } catch (Exception e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    public void resumePendingOneClickCube() {
        if (!pendingOneClickCube || pendingOneClickCubeItemId <= 0 || getClient() == null) {
            clearPendingOneClickCube();
            return;
        }

        final int itemId = pendingOneClickCubeItemId;
        final byte slot = pendingOneClickCubeSlot;
        clearPendingOneClickCube();

        oneClickCubeIgnoreBreakOnce = true;
        handling.channel.handler.InventoryHandler.processOneClickCubeFromScript(getClient(), this, itemId, slot);
    }
}
