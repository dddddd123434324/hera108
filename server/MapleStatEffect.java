package server;

import client.*;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.channel.ChannelServer;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleDataType;
import server.MapleCarnivalFactory.MCSkill;
import server.Timer.BuffTimer;
import server.life.MapleMonster;
import server.maps.*;
import tools.*;
import tools.packet.TemporaryStatsPacket;

import java.awt.*;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class MapleStatEffect implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private byte mastery,
            mhpR,
            mmpR,
            mobCount,
            attackCount,
            bulletCount,
            //            reqGuildLevel,
            period,
            //            expR,
            //            iceGageCon, //1 = party 2 = nearby
            //            recipeUseCount,
            //            recipeValidDay,
            //            reqSkillLevel,
            slotCount,
            //            effectedOnAlly,
            //            effectedOnEnemy,
            type,
            preventslip,
            immortal,
            bs;
    private short hp,
            mp,
            watk,
            matk,
            wdef,
            mdef,
            acc,
            avoid,
            hands,
            speed,
            jump,
            mpCon,
            hpCon,
            //            forceCon,
            //            bdR,
            damage,
            prop,
            ehp,
            emhp,
            emp,
            emmp,
            ewatk,
            epad,
            emdd,
            epdd,
            ewdef,
            emdef,
            ignoreMob,
            dot,
            dotTime,
            criticaldamageMin,
            criticaldamageMax,
            pddR,
            mddR,
            asrR,
            er,
            damR,
            padX,
            madX,
            mesoR,
            thaw,
            terR,
            selfDestruction,
            //            PVPdamage,
            //            indiePad,
            //            indieMad,
            fatigueChange,
            //            str,
            //            dex,
            //            int_,
            //            luk,
            //            strX,
            //            dexX,
            //            intX,
            //            lukX,
            lifeId,
            //            imhp,
            //            immp,
            inflation,
            useLevel,
            mpConReduce,
            //            indieMhp,
            //            indieMmp,
            //            indieAllStat,
            //            indieSpeed,
            //            indieJump,
            //            indieAcc,
            //            indieEva,
            //            incPVPdamage,
            mobSkill,
            mobSkillLevel; //ar = accuracy rate
    private double hpR, mpR;
    private int duration,
            sourceid,
            //            recipe,
            moveTo,
            t, u, v, w, x, y, z, cr,
            itemCon, itemConNo, bulletConsume, moneyCon,
            cooldown, morphId = 0, expinc, exp, consumeOnPickup, range, price, extendPrice, charColor, interval, rewardMeso, totalprob, cosmetic;
    private boolean overTime, skill, isPotion, partyBuff = true;
    private EnumMap<MapleBuffStat, Integer> statups;
    private ArrayList<Pair<Integer, Integer>> availableMap;
    private EnumMap<MonsterStatus, Integer> monsterStatus;
    private Point lt, rb;
    private int expBuff, itemup, mesoup, cashup, berserk, illusion, booster, berserk2, cp, nuffSkill;
    private byte level;
    //    private List<Pair<Integer, Integer>> randomMorph;
    private List<MapleDisease> cureDebuffs;
    private List<Integer> petsCanConsume, randomPickup;
    private List<Triple<Integer, Integer, Integer>> rewardItem;

    public static final MapleStatEffect loadSkillEffectFromData(final MapleData source, final int skillid, final boolean overtime, final int level, final String variables) {
        return loadFromData(source, skillid, true, overtime, level, variables);
    }

    public static final MapleStatEffect loadItemEffectFromData(final MapleData source, final int itemid) {
        return loadFromData(source, itemid, false, false, 1, null);
    }

    private static final void addBuffStatPairToListIfNotZero(final EnumMap<MapleBuffStat, Integer> list, final MapleBuffStat buffstat, final Integer val) {
        if (val.intValue() != 0) {
            list.put(buffstat, val);
        }
    }

    private final static int parseEval(String path, MapleData source, int def, String variables, int level) {
        if (variables == null) {
            return MapleDataTool.getIntConvert(path, source, def);
        } else {
            final MapleData dd = source.getChildByPath(path);
            if (dd == null) {
                return def;
            }
            if (dd.getType() != MapleDataType.STRING) {
                return MapleDataTool.getIntConvert(path, source, def);
            }
            String dddd = MapleDataTool.getString(dd).replace(variables, String.valueOf(level));
            if (dddd.substring(0, 1).equals("-")) { //-30+3*x
                dddd = "n" + dddd.substring(1, dddd.length()); //n30+3*x
            } else if (dddd.substring(0, 1).equals("=")) { //lol nexon and their mistakes
                dddd = dddd.substring(1, dddd.length());
            }
            return (int) (new CaltechEval(dddd).evaluate());
        }
    }

    private static MapleStatEffect loadFromData(final MapleData source, final int sourceid, final boolean skill, final boolean overTime, final int level, final String variables) {
        final MapleStatEffect ret = new MapleStatEffect();
        ret.sourceid = sourceid;
        ret.skill = skill;
        ret.level = (byte) level;
        if (source == null) {
            ret.isPotion = false;
            return ret;
        }
        ret.isPotion = true;
        ret.pddR = (short) parseEval("pddR", source, 0, variables, level);
        ret.mddR = (short) parseEval("mddR", source, 0, variables, level);
        ret.asrR = (short) parseEval("asrR", source, 0, variables, level);
        ret.terR = (short) parseEval("terR", source, 0, variables, level);
//        ret.bdR = (short) parseEval("bdR", source, 0, variables, level);
        ret.damR = (short) parseEval("damR", source, 0, variables, level);
        ret.criticaldamageMin = (short) parseEval("criticaldamageMin", source, 0, variables, level);
        ret.criticaldamageMax = (short) parseEval("criticaldamageMax", source, 0, variables, level);
//        ret.forceCon = (short) parseEval("forceCon", source, 0, variables, level);
//        ret.iceGageCon = (byte) parseEval("iceGageCon", source, 0, variables, level);
//        ret.expR = (byte) parseEval("expR", source, 0, variables, level);
//        ret.reqGuildLevel = (byte) parseEval("reqGuildLevel", source, 0, variables, level);
        ret.duration = parseEval("time", source, -1, variables, level);
        ret.hp = (short) parseEval("hp", source, 0, variables, level);
        ret.hpR = parseEval("hpR", source, 0, variables, level) / 100.0;
        ret.mp = (short) parseEval("mp", source, 0, variables, level);
        ret.mpR = parseEval("mpR", source, 0, variables, level) / 100.0;
        ret.mhpR = (byte) parseEval("mhpR", source, 0, variables, level);
        ret.mmpR = (byte) parseEval("mmpR", source, 0, variables, level);
        ret.ignoreMob = (short) parseEval("ignoreMobpdpR", source, 0, variables, level);
        ret.mesoR = (short) parseEval("mesoR", source, 0, variables, level);
        ret.thaw = (short) parseEval("thaw", source, 0, variables, level);
//        ret.padX = (short) parseEval("padX", source, 0, variables, level);
//        ret.madX = (short) parseEval("madX", source, 0, variables, level);
        ret.dot = (short) parseEval("dot", source, 0, variables, level);
        ret.dotTime = (short) parseEval("dotTime", source, 0, variables, level);
        ret.mpConReduce = (short) parseEval("mpConReduce", source, 0, variables, level);
        ret.mpCon = (short) parseEval("mpCon", source, 0, variables, level);
        ret.hpCon = (short) parseEval("hpCon", source, 0, variables, level);
        ret.prop = (short) parseEval("prop", source, 100, variables, level);
        ret.cooldown = Math.max(0, parseEval("cooltime", source, 0, variables, level));
        ret.interval = parseEval("interval", source, 0, variables, level);
        ret.expinc = parseEval("expinc", source, 0, variables, level);
        ret.exp = parseEval("exp", source, 0, variables, level);
        ret.range = parseEval("range", source, 0, variables, level);
        ret.morphId = parseEval("morph", source, 0, variables, level);
        ret.cp = parseEval("cp", source, 0, variables, level);
        ret.cosmetic = parseEval("cosmetic", source, 0, variables, level);
        ret.er = (short) parseEval("er", source, 0, variables, level);
        ret.slotCount = (byte) parseEval("slotCount", source, 0, variables, level);
        ret.preventslip = (byte) parseEval("preventslip", source, 0, variables, level);
        ret.useLevel = (short) parseEval("useLevel", source, 0, variables, level);
        ret.nuffSkill = parseEval("nuffSkill", source, 0, variables, level);
        ret.mobCount = (byte) parseEval("mobCount", source, 1, variables, level);
        ret.immortal = (byte) parseEval("immortal", source, 0, variables, level);
        ret.period = (byte) parseEval("period", source, 0, variables, level);
        ret.type = (byte) parseEval("type", source, 0, variables, level);
        ret.bs = (byte) parseEval("bs", source, 0, variables, level);
        ret.attackCount = (byte) parseEval("attackCount", source, 1, variables, level);
        ret.bulletCount = (byte) parseEval("bulletCount", source, 1, variables, level);
        ret.emhp = (short) parseEval("emhp", source, 0, variables, level); // 메탈아머
        ret.emmp = (short) parseEval("emmp", source, 0, variables, level);
        ret.epad = (short) parseEval("epad", source, 0, variables, level);
        ret.epdd = (short) parseEval("epdd", source, 0, variables, level);
        ret.emdd = (short) parseEval("emdd", source, 0, variables, level); // 메탈아머
        int priceUnit = parseEval("priceUnit", source, 0, variables, level);
        if (priceUnit > 0) {
            ret.price = parseEval("price", source, 0, variables, level) * priceUnit;
            ret.extendPrice = parseEval("extendPrice", source, 0, variables, level) * priceUnit;
        } else {
            ret.price = 0;
            ret.extendPrice = 0;
        }

        if (ret.skill) {
            switch (sourceid) {
                case 1100002:
                case 1200002:
                case 1300002:
                case 3100001:
                case 3200001:
                case 11101002: // 파이널 어택(소울마스터)
                case 13101002: // 파이널 어택(윈드브레이커)
                case 2111007: //텔포 마스터리
                case 2211007: //텔포 마스터리
                case 2311007: //텔포 마스터리
                case 32111010://텔포 마스터리
                case 1120013:
                case 3120008:

                    ret.mobCount = 6;
                    break;
            }
            if (GameConstants.isNoDelaySkill(sourceid)) {
                ret.mobCount = 6;
            }
        }

        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000; // items have their times stored in ms, of course
            ret.overTime = overTime || ret.isMorph() || ret.isPirateMorph() || ret.isFinalAttack() || ret.isAngel();
        }
        ret.statups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        ret.mastery = (byte) parseEval("mastery", source, 0, variables, level);
        ret.watk = (short) parseEval("pad", source, 0, variables, level);
        ret.wdef = (short) parseEval("pdd", source, 0, variables, level);
        ret.matk = (short) parseEval("mad", source, 0, variables, level);
        ret.mdef = (short) parseEval("mdd", source, 0, variables, level);
        ret.acc = (short) parseEval("acc", source, 0, variables, level);
        ret.avoid = (short) parseEval("eva", source, 0, variables, level);
        ret.speed = (short) parseEval("speed", source, 0, variables, level);
        ret.jump = (short) parseEval("jump", source, 0, variables, level);
        ret.expBuff = parseEval("expBuff", source, 0, variables, level);
        ret.cashup = parseEval("cashBuff", source, 0, variables, level);
        ret.itemup = parseEval("itemupbyitem", source, 0, variables, level);
        ret.mesoup = parseEval("mesoupbyitem", source, 0, variables, level);
        ret.berserk = parseEval("berserk", source, 0, variables, level);
        ret.berserk2 = parseEval("berserk2", source, 0, variables, level);
        ret.booster = parseEval("booster", source, 0, variables, level);
        ret.lifeId = (short) parseEval("lifeId", source, 0, variables, level);
        ret.inflation = (short) parseEval("inflation", source, 0, variables, level);
//        ret.imhp = (short) parseEval("imhp", source, 0, variables, level);
//        ret.immp = (short) parseEval("immp", source, 0, variables, level);
        ret.illusion = parseEval("illusion", source, 0, variables, level);
        ret.consumeOnPickup = parseEval("consumeOnPickup", source, 0, variables, level);
        if (ret.consumeOnPickup == 1) {
            if (parseEval("party", source, 0, variables, level) > 0) {
                ret.consumeOnPickup = 2;
            }
        }
        ret.charColor = 0;
        String cColor = MapleDataTool.getString("charColor", source, null);
        if (cColor != null) {
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(0, 2));
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(2, 4) + "00");
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(4, 6) + "0000");
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(6, 8) + "000000");
        }

        ret.ehp = (short) parseEval("emhp", source, 0, variables, level);
        ret.emp = (short) parseEval("emmp", source, 0, variables, level);
        ret.ewatk = (short) parseEval("epad", source, 0, variables, level);
        ret.ewdef = (short) parseEval("epdd", source, 0, variables, level);
        ret.emdef = (short) parseEval("emdd", source, 0, variables, level);
//        ret.indiePad = (short) parseEval("indiePad", source, 0, variables, level);
//        ret.indieMad = (short) parseEval("indieMad", source, 0, variables, level);
//        ret.indieMhp = (short) parseEval("indieMhp", source, 0, variables, level);
//        ret.indieMmp = (short) parseEval("indieMmp", source, 0, variables, level);
//        ret.indieSpeed = (short) parseEval("indieSpeed", source, 0, variables, level);
//        ret.indieJump = (short) parseEval("indieJump", source, 0, variables, level);
//        ret.indieAcc = (short) parseEval("indieAcc", source, 0, variables, level);
//        ret.indieEva = (short) parseEval("indieEva", source, 0, variables, level);
//        ret.indiePdd = (short) parseEval("indiePdd", source, 0, variables, level);
//        ret.indieMdd = (short) parseEval("indieMdd", source, 0, variables, level);
//        ret.indieAllStat = (short) parseEval("indieAllStat", source, 0, variables, level);
//        ret.str = (short) parseEval("str", source, 0, variables, level);
//        ret.dex = (short) parseEval("dex", source, 0, variables, level);
//        ret.int_ = (short) parseEval("int", source, 0, variables, level);
//        ret.luk = (short) parseEval("luk", source, 0, variables, level);
//        ret.strX = (short) parseEval("strX", source, 0, variables, level);
//        ret.dexX = (short) parseEval("dexX", source, 0, variables, level);
//        ret.intX = (short) parseEval("intX", source, 0, variables, level);
//        ret.lukX = (short) parseEval("lukX", source, 0, variables, level);
//        ret.recipe = parseEval("recipe", source, 0, variables, level);
//        ret.recipeUseCount = (byte) parseEval("recipeUseCount", source, 0, variables, level);
//        ret.recipeValidDay = (byte) parseEval("recipeValidDay", source, 0, variables, level);
//        ret.reqSkillLevel = (byte) parseEval("reqSkillLevel", source, 0, variables, level);
//        ret.effectedOnAlly = (byte) parseEval("effectedOnAlly", source, 0, variables, level);
//        ret.effectedOnEnemy = (byte) parseEval("effectedOnEnemy", source, 0, variables, level);
        List<MapleDisease> cure = new ArrayList<MapleDisease>(5);
        if (parseEval("poison", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.POISON);
        }
        if (parseEval("seal", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.SEAL);
        }
        if (parseEval("darkness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.DARKNESS);
        }
        if (parseEval("weakness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.WEAKEN);
        }
        if (parseEval("curse", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.CURSE);
        }
        ret.cureDebuffs = cure;

        ret.petsCanConsume = new ArrayList<Integer>();
        for (int i = 0; true; i++) {
            final int dd = parseEval(String.valueOf(i), source, 0, variables, level);
            if (dd > 0) {
                ret.petsCanConsume.add(dd);
            } else {
                break;
            }
        }
        final MapleData mdd = source.getChildByPath("0");
        if (mdd != null && mdd.getChildren().size() > 0) {
            ret.mobSkill = (short) parseEval("mobSkill", mdd, 0, variables, level);
            ret.mobSkillLevel = (short) parseEval("level", mdd, 0, variables, level);
        } else {
            ret.mobSkill = 0;
            ret.mobSkillLevel = 0;
        }
        final MapleData pd = source.getChildByPath("randomPickup");
        if (pd != null) {
            ret.randomPickup = new ArrayList<Integer>();
            for (MapleData p : pd) {
                ret.randomPickup.add(MapleDataTool.getInt(p));
            }
        }
        final MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();
        }

        final MapleData ltc = source.getChildByPath("con");
        if (ltc != null) {
            ret.availableMap = new ArrayList<Pair<Integer, Integer>>();
            for (MapleData ltb : ltc) {
                ret.availableMap.add(new Pair<Integer, Integer>(MapleDataTool.getInt("sMap", ltb, 0), MapleDataTool.getInt("eMap", ltb, 999999999)));
            }
        }
        int totalprob = 0;
        final MapleData lta = source.getChildByPath("reward");
        if (lta != null) {
            ret.rewardMeso = parseEval("meso", lta, 0, variables, level);
            final MapleData ltz = lta.getChildByPath("case");
            if (ltz != null) {
                ret.rewardItem = new ArrayList<Triple<Integer, Integer, Integer>>();
                for (MapleData lty : ltz) {
                    ret.rewardItem.add(new Triple<Integer, Integer, Integer>(MapleDataTool.getInt("id", lty, 0), MapleDataTool.getInt("count", lty, 0), MapleDataTool.getInt("prop", lty, 0)));
                    totalprob += MapleDataTool.getInt("prob", lty, 0);
                }
            }
        } else {
            ret.rewardMeso = 0;
        }
        ret.totalprob = totalprob;
        ret.cr = parseEval("criticalDamage", source, 0, variables, level);
        ret.t = parseEval("t", source, 0, variables, level);
        ret.u = parseEval("u", source, 0, variables, level);
        ret.v = parseEval("v", source, 0, variables, level);
        ret.w = parseEval("w", source, 0, variables, level);
        ret.x = parseEval("x", source, 0, variables, level);
        ret.y = parseEval("y", source, 0, variables, level);
        ret.z = parseEval("z", source, 0, variables, level);
        ret.damage = (short) parseEval("damage", source, 100, variables, level);
        ret.selfDestruction = (short) parseEval("selfDestruction", source, 0, variables, level);
        ret.bulletConsume = parseEval("bulletConsume", source, 0, variables, level);
        ret.moneyCon = parseEval("moneyCon", source, 0, variables, level);

        ret.itemCon = parseEval("itemCon", source, 0, variables, level);
        ret.itemConNo = parseEval("itemConNo", source, 0, variables, level);
        ret.moveTo = parseEval("moveTo", source, -1, variables, level);
        ret.monsterStatus = new EnumMap<MonsterStatus, Integer>(MonsterStatus.class);
        if (ret.overTime && ret.getSummonMovementType() == null && ret.sourceid != 35001002 && ret.sourceid != 2301004) {
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.WATK, Integer.valueOf(ret.watk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.WDEF, Integer.valueOf(ret.wdef));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MATK, Integer.valueOf(ret.matk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MDEF, Integer.valueOf(ret.mdef));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ACC, Integer.valueOf(ret.acc));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.AVOID, Integer.valueOf(ret.avoid));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.SPEED, sourceid == 32120001 || sourceid == 32101003 ? Integer.valueOf(ret.x) : Integer.valueOf(ret.speed));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.JUMP, Integer.valueOf(ret.jump));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.BOOSTER, Integer.valueOf(ret.booster));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.HP_LOSS_GUARD, Integer.valueOf(ret.thaw));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EXPRATE, Integer.valueOf(ret.expBuff)); // EXP
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.DROP_RATE, Integer.valueOf(GameConstants.getModifier(ret.sourceid, ret.itemup))); // defaults to 2x
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MESO_RATE, Integer.valueOf(GameConstants.getModifier(ret.sourceid, ret.mesoup))); // defaults to 2x 이게 메소2배인지 메소드롭율2배인지 체크
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.HP_LOSS_GUARD, Integer.valueOf(ret.thaw));
            /*addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_MAXHP, Integer.valueOf(ret.ehp));
             addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_MAXMP, Integer.valueOf(ret.emp));
             addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_WATK, Integer.valueOf(ret.ewatk));//ewatk
             addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_WDEF, Integer.valueOf(ret.ewdef));//ewdef
             addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ENHANCED_MDEF, Integer.valueOf(ret.emdef));//emdef*/
        }
        if (ret.skill) { // hack because we can't get from the datafile...
            switch (sourceid) {
                case 2001002: // magic guard
                case 12001001: // 플위
                case 22111001: // 에반
                    ret.statups.put(MapleBuffStat.MAGIC_GUARD, ret.x);
                    break;
                case 2301003: // invincible
                    ret.statups.put(MapleBuffStat.INVINCIBLE, ret.x);
                    break;
                case 2301004: // 블레스 수동
                    ret.statups.put(MapleBuffStat.ENHANCED_WATK, (int) ret.watk);
                    ret.statups.put(MapleBuffStat.MATK, (int) ret.matk);
                    ret.statups.put(MapleBuffStat.WDEF, (int) ret.wdef);
                    ret.statups.put(MapleBuffStat.MDEF, (int) ret.mdef);
                    ret.statups.put(MapleBuffStat.ACC, (int) ret.acc);
                    ret.statups.put(MapleBuffStat.AVOID, (int) ret.avoid);
                    break;
                case 13101006: // Wind Walk
                    ret.statups.put(MapleBuffStat.WIND_WALK, ret.x);
                    break;
                case 4001003: // darksight
                case 14001003: // 다크 사이트(나이트워커)
                    ret.statups.put(MapleBuffStat.DARKSIGHT, ret.x);
                    break;
                case 4211003: // pickpocket
                    ret.statups.put(MapleBuffStat.PICKPOCKET, ret.x);
                    break;
                case 4211005: // mesoguard
                    ret.statups.put(MapleBuffStat.MESOGUARD, ret.x);
                    break;
                case 4111001: // mesoup
                    ret.statups.put(MapleBuffStat.MESOUP, ret.x);
                    break;
                case 4331002: // 미러이미징
                    ret.statups.put(MapleBuffStat.SHADOWPARTNER, ret.x);
                    break;
                case 14111000: // 쉐도우 파트너(나이트워커)
                    ret.statups.put(MapleBuffStat.SHADOWPARTNER, (int) ret.level);
                    break;
                case 4111002: // shadowpartner
                case 4211008:
                    ret.statups.put(MapleBuffStat.SHADOWPARTNER, (int) ret.level);
                    break;
                case 4331003:
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.OWL_SPIRIT, 1);
                    ret.statups.put(MapleBuffStat.OWL_SPIRIT, ret.x);
                    ret.statups.put(MapleBuffStat.OWL_SPIRIT, ret.y);
                    break;
                case 4341007: // 쏜즈이펙트
                    ret.statups.put(MapleBuffStat.THORNS_EFFECT, (ret.x << 8) + ret.criticaldamageMin);
                    break;
                case 11101002: // All Final attack
                    ret.statups.put(MapleBuffStat.FINALATTACK, ret.x);//전사 따로
                    break;
                case 13101002:
                    ret.statups.put(MapleBuffStat.FINALATTACK2, ret.x);//윈브 따로
                    break;
                case 8001: //쓸만한 미스틱 도어(모험가)
                case 10008001: //쓸만한 미스틱 도어(시그너스)
                case 20008001: //쓸만한 미스틱 도어(아란)
                case 20018001: //쓸만한 미스틱 도어(에반)
                case 30008001: //쓸만한 미스틱 도어(레지스탕스)
                case 2311002: // mystic door - hacked buff icon(아이콘용인듯?)
                case 3101004: // soul arrow
                case 3201004:
                case 13101003: // 소울 에로우(윈드브레이커)
                case 33101003: // 소울 에로우(와일드헌터)
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 35101005:// 메카닉 오픈게이트
                    ret.statups.remove(MapleBuffStat.SUMMON);
                    ret.statups.remove(MapleBuffStat.SUMMON2);
                    ret.statups.remove(MapleBuffStat.PUPPET);
                    ret.statups.remove(MapleBuffStat.REAPER);
                    ret.statups.put(MapleBuffStat.SOULARROW, ret.x);
                    break;
                case 1211004: // 파이어
                case 1211006: // 아이스
                case 1221004: // 디바인
                case 11111007: // 소울 차지(소울마스터)
                case 15101006: // 라이트닝 차지
                case 21111005:
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 1211008: //라이트닝 차지
                    ret.statups.put(MapleBuffStat.LIGHTNING_CHARGE, ret.x);
                    ret.statups.put(MapleBuffStat.WK_CHARGE, ret.x);
                    break;
                case 2111008: // 모험가
                case 2211008: // 모험가
                case 12101005: // 플위 
                case 22121001: // 에반 엘리멘탈 리셋
                    ret.statups.put(MapleBuffStat.ELEMENT_RESET, ret.x);
                    break;
                case 5211006: // Homing Beacon
                case 5220011: // Bullseye
                case 22151002: //killer wings
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.HOMING_BEACON, ret.x);
                    break;
                case 2111007:
                case 2211007:
                case 2311007:
                case 32111010:
                    ret.mpCon = (short) ret.y;
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.TELEPORT_MASTERY, ret.x);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 1101004: // 소드 부스터
                case 1101005: // 엑스 부스터
                case 1201004: // 소드 부스터
                case 1201005: // 메이스 부스터
                case 1301004: // 스피어 부스터
                case 1301005: // 폴암 부스터
                case 3101002: // 보우 부스터
                case 3201002: // 크로스보우 부스터
                case 4101003: // 자벨린 부스터
                case 4201002: // 대거 부스터
                case 4301002: // 이도류 부스터
                case 2111005: // 매직 부스터
                case 2211005: // 매직 부스터
                case 5101006: // 너클 부스터
                case 5201003: // 건 부스터
                case 11101001: // 소드 부스터(소울마스터)
                case 12101004: // 매직 부스터(플레임위자드)
                case 13101001: // 보우 부스터(윈드브레이커)
                case 14101002: // 자벨린 부스터(나이트워커)
                case 15101002: // 너클 부스터(스트라이커)
                case 21001003: // 폴암 부스터(아란)
                case 22141002: // 매직 부스터(에반)
                case 32101005: // 스태프 부스터(배틀메이지)
                case 33001003: // 크로스보우 부스터(와일드헌터)
                case 35101006: // 메카닉 부스터
                    ret.statups.put(MapleBuffStat.BOOSTER, ret.x);
                    break;
                case 2311006: // 서먼 드래곤
                case 2121005: // 이프리트
                case 2321003: // bahamut
                case 4111007: // 다크 플레어
                case 4211007: // 다크 플레어
                case 11001004: // 소울
                case 12001004: // 플레임
                case 12111004: // 이프리트(플레임위자드)
                case 13001004: // 스톰
                case 14001005: // 다크니스
                case 15001004: // 라이트닝
                case 33101008: //레이닝 마인 히든스킬
                case 35111011: //힐링로봇
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    break;
                case 35111005: //액셀러레이터
                    ret.statups.remove(MapleBuffStat.SUMMON);
                    ret.statups.put(MapleBuffStat.REAPER, 1);
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, -Math.abs(ret.y));
                    break;
                case 35121009: //로보 팩토리
                    ret.statups.remove(MapleBuffStat.SUMMON2);
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 1321007: // Beholder
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.statups.put(MapleBuffStat.BEHOLDER, (int) ret.level);
                    break;
                case 5211002:
                    ret.statups.put(MapleBuffStat.SUMMON2, 1);
                    break;
                case 1101007: // pguard
                case 1201007:
                    ret.statups.put(MapleBuffStat.POWERGUARD, ret.x);
                    break;
                case 32111004: //conversion
                    ret.statups.put(MapleBuffStat.CONVERSION, ret.x);
                    break;
                case 8003: //쓸만한 하이퍼바디(모험가)
                case 10008003: //쓸만한 하이퍼바디(시그너스)
                case 20008003: //쓸만한 하이퍼바디(아란)
                case 20018003: //쓸만한 하이퍼바디(에반)
                case 30008003: //쓸만한 하이퍼바디(레지)
                case 1301007: // hyper body
                case 9001008: // GM hyper body
                    ret.statups.put(MapleBuffStat.MAXHP, ret.x);
                    ret.statups.put(MapleBuffStat.MAXMP, ret.y);
                    break;
                case 1111002: // combo
                case 11111001: // combo
                    ret.statups.put(MapleBuffStat.COMBO, 1);
                    break;
                case 21120007: //combo barrier
                    ret.statups.put(MapleBuffStat.COMBO_BARRIER, ret.x);
                    break;
                case 22131001: //magic shield
                    ret.statups.put(MapleBuffStat.MAGIC_SHIELD, ret.x);
                    break;
                case 22151003: //magic resistance
                    ret.statups.put(MapleBuffStat.MAGIC_RESISTANCE, ret.x);
                    break;
                case 22181003: //soul stone
                    ret.statups.put(MapleBuffStat.SOUL_STONE, 1);
                    break;
                case 32121003: //twister
                    ret.statups.put(MapleBuffStat.TORNADO, ret.x);
                    break;
                case 32111005: //body boost
                    ret.duration = 60000;
                    ret.statups.put(MapleBuffStat.BODY_BOOST, (int) ret.level); //lots of variables
                    break;
                case 1311006: //dragon roar
                    //ret.statups.put(MapleBuffStat.STUN, 1); //새로운 프라이드다.
                    ret.hpR = -ret.x / 100.0;
                    ret.duration = ret.y * 1000;
                    break;
                case 1211010: //NOT A BUFF - HP Recover
                    ret.hpR = ret.x / 100.0;
                    break;
                case 4341002:
                    ret.statups.put(MapleBuffStat.FINAL_CUT, ret.y);
                    break;
                case 4341003: // Monster Bomb
                    ret.monsterStatus.put(MonsterStatus.MONSTER_BOMB, 1);
                    break;
                case 1311008: // dragon blood
                    ret.statups.put(MapleBuffStat.DRAGONBLOOD, ret.x);
                    break;
                case 1121000: // maple warrior, all classes
                case 1221000:
                case 1321000:
                case 2121000:
                case 2221000:
                case 2321000:
                case 3121000:
                case 3221000:
                case 4121000:
                case 4221000:
                case 4341000:
                case 5221000:
                case 5121000:
                case 21121000: // Aran - Maple Warrior
                case 22171000: //에반?
                case 32121007: //레지
                case 33121007: //레지
                case 35121007: //레지
                    ret.statups.put(MapleBuffStat.MAPLE_WARRIOR, ret.x);
                    break;
                case 5221009: // Mind Control
                    ret.monsterStatus.put(MonsterStatus.HYPNOTIZE, 1);
                    break;
                case 8002: //쓸만한 샤프 아이즈(모험가)
                case 10008002: //쓸만한 샤프 아이즈(시그너스)
                case 20008002: //쓸만한 샤프 아이즈(아란)
                case 20018002: //쓸만한 샤프 아이즈(에반)
                case 30008002: //쓸만한 샤프 아이즈(레지스탕스)
                case 3121002: // sharp eyes bow master
                case 3221002: // sharp eyes marksmen
                case 33121004: // sharp eyes 와일드 헌터
                    ret.statups.put(MapleBuffStat.SHARP_EYES, (ret.x << 8) + ret.criticaldamageMax);
                    break;
                case 3121008:
//                    ret.statups.clear(); //집중은 다른 공격력 버프와 중첩
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    ret.statups.put(MapleBuffStat.CONCENTRATE, Integer.valueOf(ret.x));
                    ret.statups.put(MapleBuffStat.ENHANCED_WATK, Integer.valueOf(ret.ewatk));
                    break;
                case 5111005:
                case 5121003:
                case 15111002:
                case 13111005:
                    ret.statups.put(MapleBuffStat.ENHANCED_WDEF, Integer.valueOf(ret.ewdef));
                    ret.statups.put(MapleBuffStat.SPEED, Integer.valueOf(ret.speed));
                    ret.statups.put(MapleBuffStat.JUMP, Integer.valueOf(ret.jump));
                    ret.statups.put(MapleBuffStat.ENHANCED_WATK, Integer.valueOf(ret.ewatk));
                    ret.statups.put(MapleBuffStat.ENHANCED_MDEF, Integer.valueOf(ret.emdef));
                    break;
                case 5221006:
                    ret.statups.put(MapleBuffStat.ENHANCED_WDEF, Integer.valueOf(ret.ewdef));
                    ret.statups.put(MapleBuffStat.ENHANCED_MDEF, Integer.valueOf(ret.emdef));
                    ret.statups.put(MapleBuffStat.ENHANCED_MAXMP, Integer.valueOf(ret.emp));
                    ret.statups.put(MapleBuffStat.ENHANCED_WATK, Integer.valueOf(ret.ewatk));
                    ret.statups.put(MapleBuffStat.ENHANCED_MAXHP, Integer.valueOf(ret.ehp));
                    ret.duration = 2100000000;
                    break;
                case 5110001: // Energy Charge
                case 15100004:
                    ret.statups.put(MapleBuffStat.ENERGY_CHARGE, 0);
                    break;
                case 15111006: // 스파크
                    ret.statups.put(MapleBuffStat.SPARK, ret.x);
                    break;
                case 4001002: // disorder
                    ret.monsterStatus.put(MonsterStatus.WATK, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case 21101003: // Body Pressure
                    ret.statups.put(MapleBuffStat.BODY_PRESSURE, ret.x);
                    //ret.monsterStatus.put(MonsterStatus.NEUTRALISE, 1);
                    break;
                case 21000000: // Aran Combo
                    ret.statups.put(MapleBuffStat.ARAN_COMBO, 100);
                    break;
                case 21100005: // Combo Drain
                case 32101004:
                    ret.statups.put(MapleBuffStat.COMBO_DRAIN, ret.x);
                    break;
                case 4331004: // 어퍼스탭
                case 21110003: // 파이널 토스
                    ret.monsterStatus.put(MonsterStatus.RISE_BY_TOSS, ret.x);
                    break;
                case 21111001: // Smart Knockback
                    ret.statups.put(MapleBuffStat.SMART_KNOCKBACK, ret.x);
                    break;
                case 1004:
                case 10001004:
                case 33001001:
                //case 5221006: //배틀쉽

                case 20001004:
                case 20011004:

                case 1027:
                case 10001027:
                case 20001027:
                case 20011027:

                case 1028:
                case 10001028:
                case 20001028:
                case 20011028:

                case 1029:
                case 10001029:
                case 20001029:
                case 20011029:

                case 1030:
                case 10001030:
                case 20001030:
                case 20011030:

                case 1031:
                case 10001031:
                case 20001031:
                case 20011031:

                case 1033:
                case 10001033:
                case 20001033:
                case 20011033:

                case 1034:
                case 10001034:
                case 20001034:
                case 20011034:

                case 1035:
                case 10001035:
                case 20001035:
                case 20011035:

                case 1042:
                case 10001042:
                case 20001042:
                case 20011042:
                    ret.statups.put(MapleBuffStat.MONSTER_RIDING, 1);
                    break;
                case 1201006: // threaten
                    ret.monsterStatus.put(MonsterStatus.WATK, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    ret.monsterStatus.put(MonsterStatus.BLIND, ret.z);
                    break;
                case 1211011:
                    ret.statups.put(MapleBuffStat.COMBAT_ORDERS, ret.x);
                    break;
                case 1220013:
                    ret.statups.put(MapleBuffStat.DIVINE_SHIELD, ret.x + 1);
                    break;
                case 1111008: // shout
                case 1211002:
                case 4211002: // assaulter
                case 3101005: // arrow bomb
                case 1111005: // coma: sword
                case 1111006: // coma: sword
                case 4221007: // boomerang step
                case 4121008: // Ninja Storm
                case 4201004: //steal, new
                case 1121001: //magnet
                case 1221001:
                case 1321001:
                case 2211003:
                case 2311004:
                case 2221006:
                case 5101002:
                case 5101003:
                case 5121005:
                case 5111002:
                case 15101005:
                case 5201004:
                case 22151001: // 에반 브레스
                case 32101001: // 다크 체인
                case 33101001: // 봄 샷(와일드헌터)
                case 33101002: // 재규어 로어(와일드헌터)
                case 33121002: // 소닉 붐(와일드헌터)
                case 35101003: // 아토믹 해머
                case 35111015: // 로켓 펀치
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case 33101004: //it's raining mines
                    ret.statups.put(MapleBuffStat.RAINING_MINES, ret.x); //x?
                    break;
                case 35111002:
                    ret.duration = ret.y * 1000;
                    ret.statups.remove(MapleBuffStat.SUMMON);
                    ret.statups.put(MapleBuffStat.SUMMON2, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                /*case 3120010:
                 //ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                 ret.monsterStatus.put(MonsterStatus.STUN, ret.x);
                 System.out.println("ret.x: " + ret.x);
                 break;*/
                case 4321000: //tornado spin uses same buffstats
                    ret.duration = 1000;
                    ret.statups.put(MapleBuffStat.DASH_SPEED, 100 + ret.x);
                    ret.statups.put(MapleBuffStat.DASH_JUMP, ret.y); //always 0 but its there
                    break;
                case 5001005: // Dash
                case 15001003:
                    ret.statups.put(MapleBuffStat.DASH_SPEED, ret.x);
                    ret.statups.put(MapleBuffStat.DASH_JUMP, ret.y);
                    break;
                case 1111003: //blind does not work
                case 1111004:
                case 11111002:
                case 4321002://플래시 뱅
                    ret.monsterStatus.put(MonsterStatus.BLIND, ret.x);
                    break;
                case 2221003: // 아이스 데몬
                case 2121003: // 파이어 데몬
                    ret.monsterStatus.put(MonsterStatus.DAMAGED_ELEM_ATTR, 1);
                    break;
                case 4121003:
                case 4221003:
                    ret.monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    ret.monsterStatus.put(MonsterStatus.MDEF, ret.x);
                    break;
                case 33121005:
                    ret.monsterStatus.put(MonsterStatus.CHEMICAL_SHELL, ret.x);
                    ret.monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    ret.monsterStatus.put(MonsterStatus.MDEF, ret.x);
                    break;
                case 2201004: // cold beam
                case 2211002: // ice strike
                case 3211003: // blizzard
                case 2121006: // 패럴라이즈
                case 2211006: // il elemental compo
                case 2221007: // Blizzard
                case 22121000: // 아이스 브레스 
                case 2221001:
                case 5211005:
                case 90001006: // 드래고닉 무기 스킬
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
//                    ret.duration *= 2; // freezing skills are a little strange
                    break;
                /*//monsterStatus.put(MonsterStatus.POISON, ret.effects.getStats("dot"));
                 ret.monsterStatus.put(MonsterStatus.STUN, 1);
                 ret.duration = ret.x * 1000;
                 break;*/
                case 21120006: // 콩벌레 Tempest
                    ret.monsterStatus.put(MonsterStatus.FREEZE, 1);
                    ret.monsterStatus.put(MonsterStatus.IMPRINT, 1);//맞네 뎀지 증폭
                case 2101003: // fp slow
                case 2201003: // il slow
                case 12101001:
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 22141003: // 에반 슬로우 얘만 따로??;;
                    ret.statups.put(MapleBuffStat.SLOW, ret.x);
                    break;
                case 1121010: //enrage
                    ret.statups.put(MapleBuffStat.ENRAGE, ret.x * 100 + ret.mobCount);
                    break;
                case 22161002: // 고스트 레터링(에반)
                    ret.monsterStatus.put(MonsterStatus.IMPRINT, ret.x);
                    break;
                case 4121004: // Ninja ambush
                case 4221004:
                    ret.monsterStatus.put(MonsterStatus.NINJA_AMBUSH, (int) ret.damage);
                    break;
                case 2311005:
                    ret.monsterStatus.put(MonsterStatus.DOOM, 1);
                    break;
                case 3111002: // puppet ranger
                case 3211002: // puppet sniper
                case 4341006: // 더미이펙트
                case 5211001: // 문어1
                case 5220002: // 문어2
                case 13111004: // 퍼펫(윈드브레이커)
                case 33111003: // 와일드 트랩
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 3201007:
                case 3101007:
                case 3211005: // golden eagle
                case 3111005: // golden hawk
                case 3121006: // phoenix
                case 33111005: // phoenix
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.STUN, Integer.valueOf(1));
                    break;
                case 3221005: // frostprey
                case 2221005: // 엘퀴네스
                    ret.statups.put(MapleBuffStat.SUMMON, 1);
                    ret.monsterStatus.put(MonsterStatus.FREEZE, Integer.valueOf(1));
                    break;
//                case 1321007: // Beholder
//                    ret.statups.put(MapleBuffStat.BEHOLDER, (int) ret.level);
//                    break;
                case 2311003: // hs
                case 9001002: // GM hs
                    ret.statups.put(MapleBuffStat.HOLY_SYMBOL, ret.x);
                    break;
                case 35111013:
                case 5111007:
                case 5211007:
                case 5311005:
                case 5320007:
                    ret.statups.put(MapleBuffStat.DICE_ROLL, 0);
                    break;
                case 5120011:
                case 5220012:
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, (int) ret.damR); //i think
                    break;
                case 5121009:
                case 15111005:
                    ret.statups.put(MapleBuffStat.SPEED_INFUSION, ret.x);
                    break;
                case 2211004: // il seal
                case 2111004: // fp seal
                case 12111002: // cygnus seal
                    ret.monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case 4111003: // shadow web
                case 14111001:
                    ret.monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                    break;
                case 4121006: // spirit claw
                    ret.statups.put(MapleBuffStat.SPIRIT_CLAW, 0);
                    break;
                case 2121004:
                case 2221004:
                case 2321004: // Infinity
                    ret.hpR = ret.y / 100.0;
                    ret.mpR = ret.y / 100.0;
                    ret.statups.put(MapleBuffStat.INFINITY, ret.x);
                    break;
                case 1121002:
                case 1221002:
                case 1321002: // Stance
                case 21121003: // Aran - Freezing Posture
                case 32121005:
                    ret.statups.put(MapleBuffStat.STANCE, (int) ret.prop);
                    break;
                case 2121002: // mana reflection
                case 2221002:
                case 2321002:
                    ret.statups.put(MapleBuffStat.MANA_REFLECTION, ret.prop - 30);
                    break;
                case 2321005: // holy shield, TODO JUMP
                    ret.statups.put(MapleBuffStat.HOLY_SHIELD, ret.x);
                    break;
                case 3121007: // Hamstring
                    ret.statups.put(MapleBuffStat.HAMSTRING, ret.x);
                    ret.monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case 3221006: // Blind
                case 33111004:// 블라인드
                    ret.statups.put(MapleBuffStat.BLIND, ret.x);
                    ret.monsterStatus.put(MonsterStatus.ACC, ret.x);
                    break;
                case 33121006: //feline berserk
                    ret.statups.put(MapleBuffStat.SPEED, ret.z);
                    ret.statups.put(MapleBuffStat.MorewildDamageUp, ret.y);
                    ret.statups.put(MapleBuffStat.FELINE_BERSERK, ret.x);
                    break;
                case 32120000:
                /*ret.dot = ret.damage;
                 ret.dotTime = 3;*/
                case 32001003: //dark aura
                case 32110007:
                    ret.duration = (sourceid == 32110007 ? 60000 : 2100000000);
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.DARK_AURA, ret.x);
                    break;
                case 32101002: //blue aura
                case 32110000:
                case 32110008:
                    ret.duration = (sourceid == 32110008 ? 60000 : 2100000000);
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.BLUE_AURA, (int) ret.level);
                    break;
                case 32120001:
                    ret.monsterStatus.put(MonsterStatus.SPEED, (int) ret.speed);
                case 32101003: //yellow aura
                case 32110009:
                    ret.duration = (sourceid == 32110009 ? 60000 : 2100000000);
                    ret.statups.put(MapleBuffStat.AURA, (int) ret.level);
                    ret.statups.put(MapleBuffStat.YELLOW_AURA, (int) ret.level);
                    break;
                case 32111006:
                    ret.statups.put(MapleBuffStat.REAPER, 1);
                    break;
                case 35001002:
                case 35120000:
                    ret.statups.put(MapleBuffStat.ENHANCED_WDEF, Integer.valueOf(ret.epdd));
                    ret.statups.put(MapleBuffStat.ENHANCED_MDEF, Integer.valueOf(ret.emdd));
                    ret.statups.put(MapleBuffStat.ENHANCED_MAXMP, Integer.valueOf(ret.emmp));
                    ret.statups.put(MapleBuffStat.ENHANCED_WATK, Integer.valueOf(ret.epad));
                    ret.statups.put(MapleBuffStat.ENHANCED_MAXHP, Integer.valueOf(ret.emhp));
                    ret.statups.put(MapleBuffStat.STANCE, 100);
                    //ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) ret.level);
                    ret.duration = 2100000000;
                    break;
                case 35001001: //flame
                case 35101009:
                    ret.duration = 8000;
                    ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) level); //ya wtf
                    break;
                case 35111001:
                case 35111009:
                case 35111010:
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.PUPPET, 1);
                    break;
                case 35101007: //perfect armor
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.PERFECT_ARMOR, ret.x);
                    break;
                case 35121006: //satellite safety
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.SATELLITESAFE_PROC, ret.x);
                    ret.statups.put(MapleBuffStat.SATELLITESAFE_ABSORB, ret.y);
                    break;
                case 35121005: //미사일 탱크
                    ret.duration = 2100000000;
                    ret.statups.put(MapleBuffStat.MECH_CHANGE, (int) level); //ya wtf
                    break;
                case 35121010:
                    ret.duration = 60000;
                    ret.statups.remove(MapleBuffStat.SUMMON);
                    ret.statups.put(MapleBuffStat.PIRATES_REVENGE, ret.x);
                    break;
                default:
                    break;
            }
            if (GameConstants.isBeginnerJob(sourceid / 10000)) {
                switch (sourceid % 10000) {
                    //angelic blessing: HACK, we're actually supposed to use the passives for atk/matk buff
                    case 1001:
                        if (sourceid / 10000 == 3001 || sourceid / 10000 == 3000) { //resistance is diff
                            ret.statups.put(MapleBuffStat.INFILTRATE, ret.x);
                        } else {
                            ret.statups.put(MapleBuffStat.RECOVERY, ret.x);
                        }
                        break;
                    case 1011: // Berserk fury
                        ret.statups.put(MapleBuffStat.BERSERK_FURY, ret.x);
                        break;
                    case 1010:
                        ret.statups.put(MapleBuffStat.DIVINE_BODY, 1);
                        break;
                    case 1005:
                        ret.statups.put(MapleBuffStat.ECHO_OF_HERO, ret.x);
                        break;
                    case 1026: // Soaring
                        ret.duration = 60 * 120 * 1000;
                        ret.statups.put(MapleBuffStat.SOARING, 1);
                        break;
                }
            }
        }

        if (!ret.isSkill()) {
            switch (sourceid) {
                case 2022125:
                    ret.statups.put(MapleBuffStat.WDEF, 1);
                    break;
                case 2022126:
                    ret.statups.put(MapleBuffStat.MDEF, 1);
                    break;
                case 2022127:
                    ret.statups.put(MapleBuffStat.ACC, 1);
                    break;
                case 2022128:
                    ret.statups.put(MapleBuffStat.AVOID, 1);
                    break;
                case 2022129:
                    ret.statups.put(MapleBuffStat.WATK, 1);
                    break;
                case 2022033://2배쿠폰
                    ret.statups.put(MapleBuffStat.HOLY_SYMBOL2, 100);
                    break;
                case 2210007:
                case 2210008:
                case 2360000:
                case 2360001:
                    ret.statups.put(MapleBuffStat.GHOST_MORPH, 1);
                    break;
                case 2022616:
                case 2022617: // 강화 버프 1,2
                case 2022585:
                case 2022586:
                case 2022587:
                case 2022588:
                    ret.statups.put(MapleBuffStat.PYRAMID_PQ, ret.berserk);
                    break;
            }
        }

        if (ret.isPoison()) {
            ret.monsterStatus.put(MonsterStatus.POISON, 1);
        }
        if (ret.isMorph() || ret.isPirateMorph()) {
            ret.statups.put(MapleBuffStat.MORPH, ret.getMorph());
        }

        return ret;
    }

    /**
     * @param applyto
     * @param obj
     * @param attack damage done by the skill
     */
    public final void applyPassive(final MapleCharacter applyto, final MapleMapObject obj) {
        if (makeChanceResult()) {
            switch (sourceid) { // MP eater
                case 2100000:
                case 2200000:
                case 2300000:
                    if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                    final MapleMonster mob = (MapleMonster) obj; // x is absorb percentage
                    if (!mob.getStats().isBoss()) {
                        final int absorbMp = Math.min((int) (mob.getMobMaxMp() * (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.getStat().setMp(applyto.getStat().getMp() + absorbMp, applyto);
                            applyto.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 1, applyto.getLevel(), level));
                            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, applyto.getLevel(), level), false);
                        }
                    }
                    break;
            }
        }
    }

    public final boolean applyTo(MapleCharacter chr, boolean mp) {
        return applyTo(chr, chr, true, null, duration, mp);
    }

    public final boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null, duration, true);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos, duration, true);
    }

    public List<Integer> octoOID = new ArrayList<Integer>();

    private static boolean isMechanicRobotSummon(final int skillId) {
        switch (skillId) {
            case 35111002: // 마그네틱 필드
            case 35111011: // 힐링로봇
            case 35111005: // 액셀레이터
            case 35121009: // 로보 팩토리
            case 35121010: // 앰플리파이어
                return true;
        }
        return false;
    }

    public final boolean applyTo(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary, final Point pos, int newDuration, boolean mp) {
        if (isShadow() && applyfrom.getJob() / 100 % 10 != 4 && !applyfrom.isGM()) { //pirate/shadow = dc
            applyfrom.getClient().getSession().write(MaplePacketCreator.enableActions());
            return false;
        }

//        if (!skill && sourceid >= 2022125 && sourceid <= 2022129) {
//            Skill bHealing = SkillFactory.getSkill(1320009); //비홀더 버프
//            final int bHealingLvl = applyfrom.getTotalSkillLevel(bHealing);
//            if (bHealingLvl <= 0 || bHealing == null) {
//                return false;
//            }
//            final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
//            Pair stats[] = {
//                new Pair(MapleBuffStat.WDEF, healEffect.getWdef()),
//                new Pair(MapleBuffStat.MDEF, healEffect.getMdef()),
//                new Pair(MapleBuffStat.ACC, healEffect.getAcc()),
//                new Pair(MapleBuffStat.AVOID, healEffect.getAvoid()),
//                new Pair(MapleBuffStat.WATK, healEffect.getWatk())};
//            int buffEff = sourceid - 2022125;
//            if ((Short) stats[buffEff].getRight() > 0) {
//                statups.put((MapleBuffStat) stats[buffEff].getLeft(), ((Short) stats[buffEff].getRight()).intValue());
//            } else {
//                return false;
//            }
//            duration = healEffect.getDuration();
//            overTime = true;
//        }
        int hpchange = calcHPChange(applyfrom, applyto, primary);
        int mpchange = calcMPChange(applyfrom, primary);
        if (!mp) {
            hpchange = 0;
            mpchange = 0;
        }
        final PlayerStats stat = applyto.getStat();

        if (primary) {
            if (itemConNo != 0) {
                if (!applyto.haveItem(itemCon, itemConNo, false, true)) {
                    applyto.getClient().getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
                MapleInventoryManipulator.removeById(applyto.getClient(), GameConstants.getInventoryType(itemCon), itemCon, itemConNo, false, true);
            }
        } else if (!primary && isResurrection()) {
            hpchange = stat.getMaxHp();
            applyto.dispelDebuff(MapleDisease.STUN);
            applyto.dispelDebuff(MapleDisease.POISON);
            applyto.dispelDebuff(MapleDisease.SEAL);
            applyto.dispelDebuff(MapleDisease.DARKNESS);
            applyto.dispelDebuff(MapleDisease.WEAKEN);
            applyto.dispelDebuff(MapleDisease.CURSE);
            applyto.dispelDebuff(MapleDisease.SLOW);
            applyto.dispelDebuff(MapleDisease.SEDUCE);
            applyto.dispelDebuff(MapleDisease.ZOMBIFY);
            applyto.dispelDebuff(MapleDisease.REVERSE_DIRECTION);
            applyto.setStance(0); //TODO fix death bug, player doesnt spawn on other screen
        }
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuff(MapleDisease.CURSE);
            applyto.dispelDebuff(MapleDisease.DARKNESS);
            applyto.dispelDebuff(MapleDisease.POISON);
            applyto.dispelDebuff(MapleDisease.SEAL);
            applyto.dispelDebuff(MapleDisease.SLOW);
            applyto.dispelDebuff(MapleDisease.WEAKEN);
        } else if (isHeroWill()) {
            applyfrom.dispelDebuff(MapleDisease.SEDUCE);
        } else if (cureDebuffs.size() > 0) {
            for (final MapleDisease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        } else if (isMPRecovery()) {
            final int toDecreaseHP = ((stat.getCurrentMaxHp() * 10 / 100));
            if (stat.getHp() > toDecreaseHP) {
                hpchange += -toDecreaseHP; // -10% of max HP
                mpchange += ((toDecreaseHP / 100) * getY());
            } else {
                hpchange = stat.getHp() == 1 ? 0 : stat.getHp() - 1;
            }
        }
        final Map<MapleStat, Integer> hpmpupdate = new EnumMap<MapleStat, Integer>(MapleStat.class);
        if (hpchange != 0) {
//            if (hpchange < 0 && (-hpchange) > stat.getHp() && !applyto.hasDisease(MapleDisease.ZOMBIFY)) {
//                applyto.getClient().getSession().write(MaplePacketCreator.enableActions());
//                return false;
//            }

            if (!primary && applyfrom.getId() != applyto.getId() && isHeal()) { //힐 경험치
                int realHealedHp = Math.max(0, Math.min(stat.getCurrentMaxHp() - stat.getHp(), hpchange));
                if (realHealedHp > 0) {
                    int maxmp = applyfrom.getStat().getCurrentMaxMp() / 256;
                    int expa = 20 * (realHealedHp) / (8 * maxmp + 190);
                    applyfrom.gainExp(expa, true, false, true);
                }
            }
            stat.setHp(stat.getHp() + hpchange, applyto);
        }
        if (mpchange != 0) {
            if (mpchange < 0 && (-mpchange) > stat.getMp()) {
                if (getSourceId() == 35121005 && stat.getMp() != 0) { //미사일 탱크 마나
                    applyto.getClient().getSession().write(MaplePacketCreator.enableActions());
                    return false;
                }
            }
            if (getSourceId() == 2321008) {
//                mpchange *= 1.4;
                stat.setMp(Math.max(0, stat.getMp() + mpchange), applyto);
            } else if (getSourceId() == 2311004) {
//                mpchange *= 1.1;
                stat.setMp(Math.max(0, stat.getMp() + mpchange), applyto);
            } else {
                stat.setMp(stat.getMp() + mpchange, applyto);
            }
            //short converting needs math.min cuz of overflow

            hpmpupdate.put(MapleStat.MP, Integer.valueOf(stat.getMp()));
        }
        hpmpupdate.put(MapleStat.HP, Integer.valueOf(stat.getHp()));
//
//            Timer.BuffTimer.getInstance().schedule(new Runnable() {
//                @Override
//                public void run() {
//                    long ss = System.currentTimeMillis();
//                    while (ss + 1800L > System.currentTimeMillis()) {
//                        applyto.getClient().getSession().write(MaplePacketCreator.enableActions());
//                    }
//                }
//            }, 2800L);
//        }

        boolean disableHang = true;

        if (applyto.getId() != applyfrom.getId()) {
            disableHang = false;
        }
        if (GameConstants.isNoEnableSkill(getSourceId())) {
            applyfrom.addMP(-getMpCon());
            applyfrom.getClient().getSession().write(MaplePacketCreator.updatePlayerStats(Collections.singletonMap(MapleStat.MP, applyfrom.getStat().getMp()), true, applyfrom.getJob()));
        } else {
            applyto.getClient().getSession().write(MaplePacketCreator.updatePlayerStats(hpmpupdate, disableHang, applyto.getJob()));
        }
        int spiritClawItem = 0;
        if (expinc != 0) {
            applyto.gainExp(expinc, true, true, false);
        } else if (GameConstants.isMonsterCard(sourceid)) {
            applyto.getMonsterBook().addCard(applyto.getClient(), sourceid);
        } //        else if (isMistEruption()) {
        //            int i = y;
        //            for (MapleMist m : applyto.getMap().getAllMistsThreadsafe()) {
        //                if (m.getOwnerId() == applyto.getId() && m.getSourceSkill().getId() == 2111003) {
        //                    if (m.getSchedule() != null) {
        //                        m.getSchedule().cancel(false);
        //                        m.setSchedule(null);
        //                    }
        //                    if (m.getPoisonSchedule() != null) {
        //                        m.getPoisonSchedule().cancel(false);
        //                        m.setPoisonSchedule(null);
        //                    }
        //                    applyto.getMap().broadcastMessage(MaplePacketCreator.removeMist(m.getObjectId(), true));
        //                    applyto.getMap().removeMapObject(m);
        //
        //                    i--;
        //                    if (i <= 0) {
        //                        break;
        //                    }
        //                }
        //            }
        //        } 
        else if (cosmetic > 0) {
            if (cosmetic >= 30000) {
                applyto.setHair(cosmetic);
                applyto.updateSingleStat(MapleStat.HAIR, cosmetic);
            } else if (cosmetic >= 20000) {
                applyto.setFace(cosmetic);
                applyto.updateSingleStat(MapleStat.FACE, cosmetic);
            } else if (cosmetic < 100) {
                applyto.setSkinColor((byte) cosmetic);
                applyto.updateSingleStat(MapleStat.SKIN, cosmetic);
            }
            applyto.equipChanged();
        } else if (isSpiritClaw()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            short quantity = 0;
            for (int i = 1; i <= use.getSlotLimit(); i++) {
                Item item = use.getItem((byte) i);
                if (item != null && GameConstants.isThrowingStar(item.getItemId()) && item.getQuantity() > 0 && MapleItemInformationProvider.getInstance().getReqLevel(item.getItemId()) <= applyfrom.getLevel()) {
                    quantity += item.getQuantity();
                }
                if (quantity >= 200) {
                    break;
                }
            }
            if (quantity < 200) {
                return false;
            }
            quantity = 200;
            for (int i = 1; i <= use.getSlotLimit(); i++) {
                Item item = use.getItem((byte) i);
                if (item != null && GameConstants.isThrowingStar(item.getItemId()) && item.getQuantity() > 0 && MapleItemInformationProvider.getInstance().getReqLevel(item.getItemId()) <= applyfrom.getLevel()) {
                    if (item.getQuantity() < quantity) {
                        quantity -= item.getQuantity();
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i, (short) (200 - quantity), false, true);
                    } else {
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i, (short) quantity, false, true);
                        break;
                    }
                }
            }
        } else if (cp != 0 && applyto.getCarnivalParty() != null) {
            applyto.getCarnivalParty().addCP(applyto, cp);
            applyto.CPUpdate(false, applyto.getAvailableCP(), applyto.getTotalCP(), 0);
            for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                chr.CPUpdate(true, applyto.getCarnivalParty().getAvailableCP(), applyto.getCarnivalParty().getTotalCP(), applyto.getCarnivalParty().getTeam());
            }
        } else if (nuffSkill != 0 && applyto.getParty() != null) {
            final MCSkill skil = MapleCarnivalFactory.getInstance().getSkill(nuffSkill);
            if (skil != null) {
                final MapleDisease dis = skil.getDisease();
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (applyto.getParty() == null || chr.getParty() == null || (chr.getParty().getId() != applyto.getParty().getId())) {
                        if (skil.targetsAll || Randomizer.rand(0, 9) < 8) {
                            if (dis == null) {
                                chr.dispel();
                            } else if (skil.getSkill() == null) {
                                chr.giveDebuff(dis, 1, 10000, dis.getDisease(), 1, (short) 0);
                                //카니발 임시 주석
                            } else {
                                chr.giveDebuff(dis, skil.getSkill(), (short) 0);
                            }
                            if (!skil.targetsAll) {
                                break;
                            }
                        }
                    }
                }
            }
        } else if (mobSkill > 0 && mobSkillLevel > 0 && primary) {
            applyto.disease(mobSkill, mobSkillLevel);
        } else if (randomPickup != null && randomPickup.size() > 0) {
            MapleItemInformationProvider.getInstance().getItemEffect(randomPickup.get(Randomizer.nextInt(randomPickup.size()))).applyTo(applyto);
        }
        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && (sourceid != 32111006 || (applyfrom.getBuffedValue(MapleBuffStat.REAPER) != null && !primary))) {
            int summId = sourceid;
            if (sourceid == 3111002) {
                final Skill elite = SkillFactory.getSkill(3120012);
                if (applyfrom.getTotalSkillLevel(elite) > 0) {
                    return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary, pos, newDuration, true);
                }
            } else if (sourceid == 3211002) {
                final Skill elite = SkillFactory.getSkill(3220012);
                if (applyfrom.getTotalSkillLevel(elite) > 0) {
                    return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary, pos, newDuration, true);
                }
            }
            Point newPos;
            if (pos == null) {
                newPos = applyfrom.getPosition();
                if (summonMovementType == SummonMovementType.STATIONARY) {
                    if (applyfrom.isFacingLeft()) {
                        newPos.x -= 180;
                    } else {
                        newPos.x += 180;
                    }
                }
                Point calcedPoint = applyfrom.getMap().calcPointBelow(newPos);
                if (calcedPoint != null) {
                    newPos = calcedPoint;
                }
            } else {
                newPos = pos;
            }
            final MapleSummon tosummon = new MapleSummon(applyfrom, summId, getLevel(), new Point(getSourceId() == 35121003 ? applyfrom.getTruePosition() : pos == null ? applyfrom.getTruePosition() : pos), summonMovementType);
            if (!tosummon.isPuppet()) {
                applyfrom.getCheatTracker().resetSummonAttack();
            }
            applyfrom.cancelEffect(this, -1, statups, true, false);
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.addSummon(tosummon);
            if (isMechanicRobotSummon(summId)) {
                final int pct = getMechanicRobotMasteryPercent(applyfrom); // 예: 50이면 +50%
                if (pct > 0) {
                    // newDuration은 ms 단위로 들어오는 구조입니다.
                    newDuration = (int) ((long) newDuration * (100L + pct) / 100L);
                    tosummon.setServerExpireTime(System.currentTimeMillis() + (long) newDuration);
                }
            }
            tosummon.addHP((short) x);
            if (isBeholder()) {
                tosummon.addHP((short) 1);
            } else if (getSourceId() == 1301007) { // 하이퍼 바디
                applyfrom.cancelEffect(this, -1, statups, false, false);
            } else if (sourceid == 32111006) {
                applyfrom.cancelEffectFromBuffStat(MapleBuffStat.REAPER);
            } else if (sourceid == 35111002) {
                List<Integer> count = new ArrayList<Integer>();
                final List<MapleSummon> ss = applyfrom.getSummonsReadLock();
                try {
                    for (MapleSummon s : ss) {
                        if (s.getSkill() == sourceid) {
                            count.add(s.getObjectId());
                        }
                    }
                } finally {
                    applyfrom.unlockSummonsReadLock();
                }
                if (count.size() != 3) {
                    return true; //no buff until 3
                }
                applyfrom.getClient().getSession().write(MaplePacketCreator.skillCooldown(sourceid, getCooldown()));
                applyfrom.addCooldown(sourceid, System.currentTimeMillis(), getCooldown() * 1000);
                applyfrom.getMap().broadcastMessage(MaplePacketCreator.teslaTriangle(applyfrom.getId(), count.get(0), count.get(1), count.get(2)));
            }
            if (sourceid == 35121003) {
                return false;
            }
            /*else if (sourceid == 5211001 || sourceid == 5220002) {
             final List<MapleSummon> ss = applyfrom.getSummonsReadLock();
             try {
             for (MapleSummon s : ss) {
             for (int i = 0; i < octoOID.size(); i++) {
             MapleSummon summon = applyfrom.getMap().getSummonByOid(octoOID.get(i));
             if (summon == null) {
             octoOID.remove(i);
             }
             }
             if (s.getSkill() == sourceid && !octoOID.contains(s.getObjectId())) {
             octoOID.add(s.getObjectId());
             System.out.println("octoOID: " + octoOID);
             }
             }
             } finally {
             applyfrom.unlockSummonsReadLock();
             }
             }*/
        } else if (isMechDoor()) {
            int newId = 0;
            boolean applyBuff = false;
            if (applyto.getMechDoors().size() >= 2) {
                MechDoor remove = applyto.getMechDoors().remove(0);
                newId = remove.getId();
                applyto.getMap().broadcastMessage(MaplePacketCreator.removeMechDoor(remove, true));
                applyto.getMap().removeMapObject(remove);
                remove = applyto.getMechDoors().remove(1);
                newId = remove.getId();
                applyto.getMap().broadcastMessage(MaplePacketCreator.removeMechDoor(remove, true));
                applyto.getMap().removeMapObject(remove);
                applyto.clearMechDoors();
                newId = 0;
            } else {
                for (MechDoor d : applyto.getMechDoors()) {
                    if (d.getId() == newId) {
                        applyBuff = true;
                        newId = 1;
                        break;
                    }
                }
            }
            final MechDoor door = new MechDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), newId);
            applyto.getMap().spawnMechDoor(door);
            applyto.addMechDoor(door);
            applyto.getClient().getSession().write(MaplePacketCreator.mechPortal(door.getTruePosition()));
            if (!applyBuff) {
                return true; //do not apply buff until 2 doors spawned
            }
        }
        if (primary && availableMap != null) {
            for (Pair<Integer, Integer> e : availableMap) {
                if (applyto.getMapId() < e.left || applyto.getMapId() > e.right) {
                    applyto.getClient().getSession().write(MaplePacketCreator.enableActions());
                    return true;
                }
            }
        }
        if (overTime && !isEnergyCharge() && (sourceid != 22181003 || !primary)) {
            applyBuffEffect(applyfrom, applyto, primary, newDuration, spiritClawItem);
        }
        if (primary) {
            if (overTime || isHeal() || isDispel()) {
                applyBuff(applyfrom, newDuration);
            }
            if (isMonsterBuff()) {
                applyMonsterBuff(applyfrom);
            }
        }
        if (isMagicDoor()) { // Magic Door
            MapleDoor door = new MapleDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), sourceid); // Current Map door

            if (door.getTownPortal() != null) {
                door.updateTownDoorPosition(applyto.getParty());

                applyto.getMap().spawnDoor(door);
                applyto.addDoor(door);

                MapleDoor townDoor = new MapleDoor(door); // Town door
                applyto.addDoor(townDoor);
                door.getTown().spawnDoor(townDoor);

                if (applyto.getParty() != null) { // update town doors
                    applyto.silentPartyUpdate();
                    applyto.addDoor(door);// 미스틱도어 파탈?
                }
            } else {
                applyto.dropMessage(5, "마을의 미스틱 도어 지점이 꽉 차서 지금은 사용할 수 없습니다.");
            }
        } else if (isMist()) {
            final Rectangle bounds = calculateBoundingBox(pos != null ? pos : applyfrom.getPosition(), applyfrom.isFacingLeft());
            final MapleMist mist = new MapleMist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), false);
        } else if (isTimeLeap()) { // Time Leap
            for (MapleCoolDownValueHolder i : applyto.getCooldowns()) {
                if (i.skillId != 5121010) {
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().getSession().write(MaplePacketCreator.skillCooldown(i.skillId, 0));
                }
            }
        }
        if (rewardMeso != 0) {
            applyto.gainMeso(rewardMeso, false);
        }
        if (rewardItem != null && totalprob > 0) {
            for (Triple<Integer, Integer, Integer> reward : rewardItem) {
                if (MapleInventoryManipulator.checkSpace(applyto.getClient(), reward.left, reward.mid, "") && reward.right > 0 && Randomizer.nextInt(totalprob) < reward.right) { // Total prob
                    if (GameConstants.getInventoryType(reward.left) == MapleInventoryType.EQUIP) {
                        final Item item = MapleItemInformationProvider.getInstance().getEquipById(reward.left);
                        item.setGMLog("Reward item (effect): " + sourceid + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.addbyItem(applyto.getClient(), item);
                    } else {
                        MapleInventoryManipulator.addById(applyto.getClient(), reward.left, reward.mid.shortValue(), "Reward item (effect): " + sourceid + " on " + FileoutputUtil.CurrentReadable_Date());
                    }
                }
            }
        }
        /*if (sourceid == 2121007 || sourceid == 2221007 || sourceid == 2321008) { //이것도 너프해 보시지~
         applyfrom.giveDebuff(MapleDisease.STUN, 1, 10000L, sourceid, 1, (short) 0); // By.갓무스 셀리노짱 ㅎㅎ
         }*/
        return true;
    }

    public final boolean applyReturnScroll(final MapleCharacter applyto) {
        if (moveTo != -1) {
            if (moveTo != applyto.getMapId() || sourceid == 2031010 || sourceid == 2030021) {
                MapleMap target;
                if (moveTo == 999999999) {
                    target = applyto.getMap().getReturnMap();
                } else {
                    target = ChannelServer.getInstance(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
                    if (target.getId() / 10000000 != 60 && applyto.getMapId() / 10000000 != 61) {
                        if (target.getId() / 10000000 != 21 && applyto.getMapId() / 10000000 != 20) {
                            if (target.getId() / 10000000 != 12 && applyto.getMapId() / 10000000 != 10) {
                                if (target.getId() / 10000000 != 10 && applyto.getMapId() / 10000000 != 12) {
                                    if (target.getId() / 10000000 != applyto.getMapId() / 10000000) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
                applyto.changeMap(target, target.getPortal(0));
                return true;
            } else {
            }
        }
        return false;
    }

    private final void applyBuff(final MapleCharacter applyfrom, int newDuration) {
        if (skill && sourceid == 22181003) { // 소울스톤
            if (applyfrom.getParty() != null) {
                final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
                final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapleMapObjectType.PLAYER));

                List<MapleCharacter> chrList = new ArrayList<MapleCharacter>();
                for (final MapleMapObject affectedmo : affecteds) {
                    final MapleCharacter chr = (MapleCharacter) affectedmo;
                    if (chr.getParty() != null && chr.getParty().getId() == applyfrom.getParty().getId()) {
                        chrList.add(chr);
                    }
                }
                int a = -1;
                int b = -1;
                int c = 0;
                switch (chrList.size()) {
                    case 1:
                        a = 0;
                        break;
                    case 2:
                        a = 0;
                        b = 1;
                        break;
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        while (true) {
                            c++;
                            if (c > 1000) {
                                break;
                            }
                            if (a == -1) {
                                a = (int) (Math.random() * chrList.size());
                            }
                            if (b == -1) {
                                b = (int) (Math.random() * chrList.size());
                            }
                            if (a == b) {
                                b = (int) (Math.random() * chrList.size());
                            }
                            if (a > -1 && b > -1 && a != b) {
                                break;
                            }
                        }
                        break;
                }
                if (y < 2) {
                    b = -1;
                }
                for (int i = 0; i < chrList.size(); i++) {
                    MapleCharacter chr = chrList.get(i);
                    if (chr != null && (i == a || i == b)) {
                        applyTo(applyfrom, chr, false, null, newDuration, true);
                        chr.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 2, applyfrom.getLevel(), level));
                        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), sourceid, 2, applyfrom.getLevel(), level), false);
                    }
                }
            } else {
                applyTo(applyfrom, applyfrom, false, null, newDuration, true);
                applyfrom.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 2, applyfrom.getLevel(), level));
                applyfrom.getMap().broadcastMessage(applyfrom, MaplePacketCreator.showBuffeffect(applyfrom.getId(), sourceid, 2, applyfrom.getLevel(), level), false);
            }
        } else if (isPartyBuff() && (applyfrom.getParty() != null || isGmBuff() || (applyfrom.isStrongBuff() && isCanStrongBuff()))) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
            final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapleMapObjectType.PLAYER));

            for (final MapleMapObject affectedmo : affecteds) {
                final MapleCharacter affected = (MapleCharacter) affectedmo;

                if (affected.getId() != applyfrom.getId() && (isGmBuff() || (applyfrom.getParty() != null && affected.getParty() != null && applyfrom.getParty().getId() == affected.getParty().getId()) || (applyfrom.isStrongBuff() && isCanStrongBuff()))) {
                    if ((isResurrection() && !affected.isAlive()) || (!isResurrection() && affected.isAlive())) {
                        if (applyfrom.isStrongBuff() && (isCanStrongBuff() || isGmBuff())) {
                            newDuration = 3600000; //60분
                        }
                        applyTo(applyfrom, affected, false, null, newDuration, true);
                        affected.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 2, applyfrom.getLevel(), level));
                        affected.getMap().broadcastMessage(affected, MaplePacketCreator.showBuffeffect(affected.getId(), sourceid, 2, applyfrom.getLevel(), level), false);
                    }
                    if (isTimeLeap()) {
                        for (MapleCoolDownValueHolder i : affected.getCooldowns()) {
                            if (i.skillId != 5121010) {
                                affected.removeCooldown(i.skillId);
                                affected.getClient().getSession().write(MaplePacketCreator.skillCooldown(i.skillId, 0));
                            }
                        }
                    }
                }
            }
        }
    }

    public final void applyMonsterBuff(final MapleCharacter applyfrom) {
        final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
        final MapleMapObjectType type = MapleMapObjectType.MONSTER;
        final List<MapleMapObject> affected = sourceid == 35111005 ? applyfrom.getMap().getMapObjectsInRange(applyfrom.getTruePosition(), Double.POSITIVE_INFINITY, Arrays.asList(type)) : applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(type));
        int i = 0;

        for (final MapleMapObject mo : affected) {
            if (makeChanceResult()) {
                for (Map.Entry<MonsterStatus, Integer> stat : getMonsterStati().entrySet()) {
                    MapleMonster mons = (MapleMonster) mo;
                    if (sourceid == 35111005 && mons.getStats().isBoss()) {
                        break;
                    }
                    mons.applyStatus(applyfrom, new MonsterStatusEffect(stat.getKey(), stat.getValue(), sourceid, null, false), isPoison(), getDuration(), true, this);
                }
            }
            i++;
            if (i >= mobCount && sourceid != 35111005) {
                break;
            }
        }
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, range);
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, int addedRange) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, range + addedRange);
    }

    public final static Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, final Point lt, final Point rb, final int range) {
        if (lt == null || rb == null) {
            return new Rectangle((facingLeft ? (-200 - range) : 0) + posFrom.x, (-100 - range) + posFrom.y, 200 + range, 100 + range);
        }
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x - range, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x + range, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public final double getMaxDistanceSq() { //lt = infront of you, rb = behind you; not gonna distanceSq the two points since this is in relative to player position which is (0,0) and not both directions, just one
        final int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        final int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        return (maxX * maxX) + (maxY * maxY);
    }

    public final void setDuration(int d) {
        this.duration = d;
    }

    private int getMechanicRobotMasteryPercent(final MapleCharacter chr) {
        final Skill mastery = SkillFactory.getSkill(35120001);
        final int slv = chr.getTotalSkillLevel(mastery);
        if (slv <= 0) {
            return 0;
        }
        return mastery.getEffect(slv).getY(); // 예: 50이면 +50%
    }

    private int applyPercent(final int baseDurationMs, final int percent) {
        if (percent <= 0) {
            return baseDurationMs;
        }
        // overflow 방지 위해 long 사용
        return (int) ((long) baseDurationMs * (100L + percent) / 100L);
    }

    public final void silentApplyBuff(final MapleCharacter chr, final long starttime, final int localDuration, final Map<MapleBuffStat, Integer> statup, final int cid) {
        chr.registerEffect(this, starttime, BuffTimer.getInstance().schedule(new CancelEffectAction(chr, this, starttime, statup),
                ((starttime + localDuration) - System.currentTimeMillis())), statup, true, localDuration, cid);

        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final MapleSummon tosummon = new MapleSummon(chr, this, chr.getTruePosition(), summonMovementType);
            if (!tosummon.isPuppet()) {
                chr.getCheatTracker().resetSummonAttack();
                chr.getMap().spawnSummon(tosummon);
                chr.addSummon(tosummon);
                tosummon.addHP((short) x);
                if (isBeholder()) {
                    tosummon.addHP((short) 1);
                }
            }
        }
    }

    private final void applyBuffEffect(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary, final int newDuration, int spiritClawItem) {
        //Debug
//                    for (Entry<MapleBuffStat, Integer> p : statups.entrySet()) {
//                        applyto.dropMessage(5, p.getKey() + " : " + p.getValue());
//                    }

        int localDuration = newDuration;
        if (primary) {
            final boolean mechRobot
                    = sourceid == 35111011 || sourceid == 35111005 || sourceid == 35121009 || sourceid == 35121010;
            if (!mechRobot) {
                localDuration = Math.max(newDuration, alchemistModifyVal2(applyfrom, localDuration, false));
            } else {
                localDuration = newDuration;
            }
        }
        Map<MapleBuffStat, Integer> localstatups = statups, maskedStatups = null;
        boolean normal = true, showEffect = primary;
        int maskedDuration = 0;
        if (!isMonsterRiding() && !isMechDoor()) {
            switch (getSourceId()) {
                case 1301007:
                case 9001008:
                    applyto.cancelEffect(this, -1, localstatups, false, false); //cancel before apply buff
                    break;
                case 35111004:
                case 35121013: //4차 헤비는 캔슬해주지마!
                case 35121003: //워머신도!
                    break;
                default:
                    applyto.cancelEffect(this, -1, localstatups, true, false); //cancel before apply buff
                    break;
            }
        }
        switch (sourceid) {
            case 4001003:
            case 14001003: { // Dark Sight
                if (applyto.isHidden()) {
                    return; //don't even apply the buff
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.DARKSIGHT, 0);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);

                if (this.statups.containsKey(MapleBuffStat.SPEED) && statups.get(MapleBuffStat.SPEED) == 0) {
                    statups.remove(MapleBuffStat.SPEED);
                }
                break;
            }

            case 4121006:
                maskedStatups = new HashMap<>();
                maskedStatups.put(MapleBuffStat.SPIRIT_CLAW, spiritClawItem);
                break;

            case 1211002: // wk charges
            case 1211003:
            case 1211004:
            case 1211006:
            case 1211005:
            case 1211007:
            case 1221003:
            case 1221004:
            case 11111007: // 소울 차지 
            case 15101006: // 라이트닝 차지
            case 21111005: {
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.WK_CHARGE, 1);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 1211008: { //lightning
                localstatups.clear();
                if (applyto.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
                    if (applyto.getBuffSource(MapleBuffStat.WK_CHARGE) == sourceid) {
                        localstatups.put(MapleBuffStat.WK_CHARGE, 1);
                        localstatups.put(MapleBuffStat.LIGHTNING_CHARGE, 1);
                    } else {
                        localstatups.put(MapleBuffStat.LIGHTNING_CHARGE, 1);
                    }
                } else {
                    localstatups.put(MapleBuffStat.WK_CHARGE, 1);
                    localstatups.put(MapleBuffStat.LIGHTNING_CHARGE, 1);
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.WK_CHARGE, 1);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 35111004: {//siege
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.MECH_CHANGE, (int) level);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, localstatups, this));
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.MECH_CHANGE, (int) level);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveMechForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 35121013: {//siege
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.MECH_CHANGE, (int) level);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, localstatups, this));
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.MECH_CHANGE, (int) level);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveMechForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 35101006:
            case 35101004:
            case 35001001: //flame
            case 35101009:
            case 35121005: { //missile
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.MECH_CHANGE, (int) level);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveMechForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 1220013: {
                /*if (applyto.isHidden()) {
                 break;
                 }
                 final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                 stat.put(MapleBuffStat.DIVINE_SHIELD, 1);
                 localstatups.put(MapleBuffStat.DIVINE_SHIELD, (int) 100);//공격력
                 //localstatups.put(MapleBuffStat.WATK, (int) 10 + (2 * level));//공격력
                 //applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, localstatups, this));
                 applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                 break;*/
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.DIVINE_SHIELD, 1);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 2121004:
            case 2221004:
            case 2321004: { //Infinity
                maskedDuration = alchemistModifyVal2(applyfrom, 4000, false);
                break;
            }
            case 1111002:
            case 11111001: { // Combo
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.COMBO, 0);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 4331003: { // 아울 데드
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.OWL_SPIRIT, y);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, localstatups, this));
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), localstatups, this), false);
                applyto.setBattleshipHP(x); //a variable that wouldnt' be used by a db
                normal = false;
                break;
            }
            case 4341002: { // 파이널 컷
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.FINAL_CUT, applyto.getFinalCut());
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), localstatups, this), false);
                break;
            }
            case 3101004:
            case 3201004:
            case 13101003: { // Soul Arrow
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SOULARROW, 0);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 4211008:
            case 4111002:
            case 14111000: { // Shadow Partner
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SHADOWPARTNER, x);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 4331002: {
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SHADOWPARTNER, x);
                //stat.put(MapleBuffStat.MIRROR_IMAGE, 0);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 2360000: // 유령 사탕
            case 2360001: {
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.GHOST_MORPH, 1);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 1026: // 플라잉
            case 10001026:
            case 20001026:
            case 20011026:
            case 30001026: {
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SOARING, 1);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 15111006: { // Spark
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.SPARK, x);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, localstatups, this));
                normal = false;
                break;
            }
            case 32121003: { //twister
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.TORNADO, x);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 32111005: { //body boost
                applyto.cancelEffectFromBuffStat(MapleBuffStat.BODY_BOOST);
                Pair<MapleBuffStat, Integer> statt;
                int sourcez = 0;
                if (applyfrom.getStatForBuff(MapleBuffStat.DARK_AURA) != null) {
                    sourcez = 32001003;
                    statt = new Pair<MapleBuffStat, Integer>(MapleBuffStat.DARK_AURA, (int) (level + 10 + applyto.getTotalSkillLevel(32001003))); //i think
                } else if (applyfrom.getStatForBuff(MapleBuffStat.YELLOW_AURA) != null) {
                    sourcez = 32101003;
                    statt = new Pair<MapleBuffStat, Integer>(MapleBuffStat.YELLOW_AURA, (int) applyto.getTotalSkillLevel(32101003));
                } else {
                    return;
                }
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.BODY_BOOST, (int) level);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, localstatups, this));
                localstatups.put(statt.left, statt.right);
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(statt.left, statt.right);
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.BLUE_AURA, applyfrom.getId());
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.YELLOW_AURA, applyfrom.getId());
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.DARK_AURA, applyfrom.getId());
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourcez, localDuration, stat, this));
                normal = false;
                break;
            }

            case 32001003: {//dark aura
                if (applyfrom.getTotalSkillLevel(32120000) > 0) {
                    SkillFactory.getSkill(32120000).getEffect(applyfrom.getTotalSkillLevel(32120000)).applyBuffEffect(applyfrom, applyto, primary, newDuration, spiritClawItem);
                    return;
                }
            }
            case 32110007:
            case 32120000: { // adv dark aura
                if (applyto.getBuffedValue(MapleBuffStat.BODY_BOOST) != null) {
                    applyto.dropMessage(5, "슈퍼 중에는 오라를 사용하실 수 없습니다.");
                    return;
                }
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.DARK_AURA);
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.BLUE_AURA);
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.YELLOW_AURA);
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                statt.put(sourceid == 32110007 ? MapleBuffStat.BODY_BOOST : MapleBuffStat.AURA, (int) (sourceid == 32120000 ? applyfrom.getTotalSkillLevel(32001003) : level));
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid == 32120000 ? 32001003 : sourceid, localDuration, statt, this));
                statt.clear();
                statt.put(MapleBuffStat.DARK_AURA, x);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                normal = false;
                break;
            }
            case 32101002: { // blue aura
                if (applyfrom.getTotalSkillLevel(32110000) > 0) {
                    SkillFactory.getSkill(32110000).getEffect(applyfrom.getTotalSkillLevel(32110000)).applyBuffEffect(applyfrom, applyto, primary, newDuration, spiritClawItem);
                    return;
                }
            }
            case 32110008: {
                localDuration = 10000;
            }
            case 32110000: { // advanced blue aura
                if (applyto.getBuffedValue(MapleBuffStat.BODY_BOOST) != null) {
                    applyto.dropMessage(5, "슈퍼 바디 중에는 오라를 사용하실 수 없습니다.");
                    return;
                }
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.BLUE_AURA);
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.DARK_AURA);
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.YELLOW_AURA);
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                statt.put(sourceid == 32110008 ? MapleBuffStat.BODY_BOOST : MapleBuffStat.BLUE_AURA, (int) (sourceid == 32110000 ? applyfrom.getTotalSkillLevel(32101002) : level));
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid == 32110000 ? 32101002 : sourceid, localDuration, statt, this));
                statt.clear();
                statt.put(MapleBuffStat.BLUE_AURA, (int) level);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                normal = false;
                break;
            }

            case 32101003: { //옐로우 오라
                if (applyfrom.getTotalSkillLevel(32120001) > 0) {
                    SkillFactory.getSkill(32120001).getEffect(applyfrom.getTotalSkillLevel(32120001)).applyBuffEffect(applyfrom, applyto, primary, newDuration, spiritClawItem);
                    return;
                }
            }
            case 32110009:
            case 32120001: { // advanced yellow aura
                if (applyto.getBuffedValue(MapleBuffStat.BODY_BOOST) != null) {
                    applyto.dropMessage(5, "슈퍼 바디 중에는 오라를 사용하실 수 없습니다.");
                    return;
                }
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.YELLOW_AURA);
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.BLUE_AURA);
                //applyto.cancelEffectFromBuffStat(MapleBuffStat.DARK_AURA);
                final EnumMap<MapleBuffStat, Integer> statt = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                statt.put(sourceid == 32110009 ? MapleBuffStat.BODY_BOOST : MapleBuffStat.YELLOW_AURA, (int) (sourceid == 32120001 ? applyfrom.getTotalSkillLevel(32101003) : level));
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid == 32120001 ? 32101003 : sourceid, localDuration, statt, this));
                statt.clear();
                statt.put(MapleBuffStat.YELLOW_AURA, (int) level);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, statt, this));
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), statt, this), false);
                normal = false;
                break;
            }
            case 1121010: // Enrage
                applyto.handleOrbconsume(10);
                break;
            case 8006:
            case 10008006:
            case 20008006:
            case 20018006:
            case 20028006:
            case 30008006:
            case 30018006:
            case 5121009: // Speed Infusion
            case 15111005:
            case 5001005: // Dash
            case 4321000: //tornado spin
            case 15001003: {
                applyto.getClient().getSession().write(TemporaryStatsPacket.givePirate(statups, localDuration / 1000, sourceid));
                if (!applyto.isHidden()) {
                    applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignPirate(statups, localDuration / 1000, applyto.getId(), sourceid), false);
                }
                normal = false;
                break;
            }
            case 35111013:
            case 5111007:
            case 5311005:
            case 5211007: {//dice
                final int zz = Randomizer.rand(1, 6);
                applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showDiceEffect(applyto.getId(), sourceid, zz, -1, level), false);
                applyto.getClient().getSession().write(MaplePacketCreator.showOwnDiceEffect(sourceid, zz, -1, level));
                if (zz <= 1) {
                    return;
                }
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(MapleBuffStat.DICE_ROLL, zz);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveDice(zz, sourceid, localDuration, localstatups));
                normal = false;
                showEffect = false;
                break;
            }
            case 33101006: {//jaguar oshi
                applyto.clearLinkMid();
                MapleBuffStat theBuff = null;
                int theStat = x;
                switch (Randomizer.rand(1, 5)) {
                    case 1:
                        theBuff = MapleBuffStat.CRITICAL_RATE_BUFF;
                        theStat = y;
                        break;
                    case 2:
                        theBuff = MapleBuffStat.MP_BUFF;
                        theStat = y;
                        break;
                    case 3:
                        theBuff = MapleBuffStat.ATTACK_BUFF;
                        theStat = z;
                        break;
                    case 4:
                        theBuff = MapleBuffStat.DAMAGE_TAKEN_BUFF;
                        break;
                    case 5:
                        theBuff = MapleBuffStat.DODGE_CHANGE_BUFF;
                        break;
                }
                applyto.cancelBuffStats(true, MapleBuffStat.CRITICAL_RATE_BUFF);
                applyto.cancelBuffStats(true, MapleBuffStat.MP_BUFF);
                applyto.cancelBuffStats(true, MapleBuffStat.DAMAGE_TAKEN_BUFF);
                applyto.cancelBuffStats(true, MapleBuffStat.DODGE_CHANGE_BUFF);
                applyto.cancelBuffStats(true, MapleBuffStat.ATTACK_BUFF);
                localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                localstatups.put(theBuff, theStat);
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, localDuration, localstatups, this));
                normal = false;
                break;
            }
            case 5211006: // Homing Beacon
            case 22151002: //killer wings
            case 5220011: {// Bullseye
                if (applyto.getFirstLinkMid() > 0) {
                    applyto.getClient().getSession().write(TemporaryStatsPacket.cancelHoming());
                    applyto.getClient().getSession().write(TemporaryStatsPacket.giveHoming(sourceid, applyto.getFirstLinkMid(), Math.max(1, applyto.getDamageIncrease(applyto.getFirstLinkMid()))));
                    if (!applyto.isHidden()) {
                        applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignHoming(sourceid, applyto.getFirstLinkMid(), Math.max(1, applyto.getDamageIncrease(applyto.getFirstLinkMid())), applyto.getId()), false);
                    }
                } else {
                    return;
                }
                normal = false;
                break;
            }
            case 13101006: { // Wind Walk
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.WIND_WALK, 0);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 30001001: { // Wind Walk
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.INFILTRATE, 0);
                applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                break;
            }
            case 35001002:
                if (applyfrom.getTotalSkillLevel(35120000) > 0) {
                    SkillFactory.getSkill(35120000).getEffect(applyfrom.getTotalSkillLevel(35120000)).applyBuffEffect(applyfrom, applyto, primary, newDuration, spiritClawItem);
                    return;
                }
            //fallthrough intended
            default:
                if (isPirateMorph()) {
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.MORPH, getMorph(applyto));
                    localstatups.put(MapleBuffStat.MORPH, getMorph(applyto));
                    applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isMorph()) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.MORPH, getMorph(applyto));
                    applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isMonsterRiding()) {
                    localDuration = 2100000000;
                    localstatups = new EnumMap<MapleBuffStat, Integer>(statups);
                    final int mountid = parseMountInfo(applyto, sourceid);
                    final int mountid2 = parseMountInfo_Pure(applyto, sourceid);
                    if (mountid != 0 && mountid2 != 0) {
                        int ridingLevel = mountid2 - 1902000 + 1;
                        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                        stat.put(MapleBuffStat.MONSTER_RIDING, 0);
                        localstatups.put(MapleBuffStat.MONSTER_RIDING, ridingLevel);
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.POWERGUARD);
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.MANA_REFLECTION);
                        applyto.getClient().getSession().write(TemporaryStatsPacket.giveMount(mountid2, sourceid, stat));
                        applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignMount(applyto.getId(), mountid2, mountid, stat), false);
                    } else {
                        return;
                    }
                    maskedStatups = new EnumMap<MapleBuffStat, Integer>(localstatups);
                    maskedStatups.remove(MapleBuffStat.MONSTER_RIDING);
                    normal = maskedStatups.size() > 0;
                } else if (isBerserkFury() || berserk2 > 0) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.BERSERK_FURY, 1);
                    applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                } else if (isDivineBody()) {
                    if (applyto.isHidden()) {
                        break;
                    }
                    final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                    stat.put(MapleBuffStat.DIVINE_BODY, 1);
                    applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveForeignBuff(applyto.getId(), stat, this), false);
                }
                break;
        }
        if (showEffect && !applyto.isHidden()) {
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, applyto.getLevel(), level), false);
        }
        // Broadcast effect to self
        if (normal && localstatups.size() > 0) {
            if (isMonsterRiding()) {
                int bid = (sourceid == 35001002 || sourceid == 35120000) ? sourceid : 0;
                applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(bid, localDuration, maskedStatups == null ? localstatups : maskedStatups, this));
            } else {
                if (sourceid != 35111002) {
                    applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff((skill ? sourceid : -sourceid), localDuration, maskedStatups == null ? localstatups : maskedStatups, this));
                }
            }
        }
        if (sourceid == 35111002) { //하드코딩 이상하게 25초로 들어오고 위젯에도 애매함
            localDuration = 200000;
        }
        final long starttime = System.currentTimeMillis();
        final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, localstatups);
        final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, maskedDuration > 0 ? maskedDuration : localDuration);
        applyto.registerEffect(this, starttime, schedule, localstatups, false, localDuration, applyfrom.getId());
    }

    public static final int parseMountInfo(final MapleCharacter player, final int skillid) {
        switch (skillid) {
            case 80001000:
            case 1004: // Monster riding
            case 11004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
            case 20021004:
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -118) != null && player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -119) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -118).getItemId();
                }
                return parseMountInfo_Pure(player, skillid);
            default:
                return GameConstants.getMountItem(skillid, player);
        }
    }

    public static final int parseMountInfo_Pure(final MapleCharacter player, final int skillid) {
        switch (skillid) {
            case 80001000:
            case 1004: // Monster riding
            case 11004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
            case 20021004:
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (-18)) != null && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (-19)) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (-18)).getItemId();
                }
                return 0;
            default:
                return GameConstants.getMountItem(skillid, player);
        }
    }

    private final int calcHPChange(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
                if (applyto.hasDisease(MapleDisease.ZOMBIFY)) {
                    hpchange /= 2;
                }
            } else { // assumption: this is heal
                hpchange += applyfrom.getStat().RecoveryUP * (makeHealHP(hp / 100.0, applyfrom.getStat().getTotalMagic(), 3, 5)) / 100;
                //applyfrom.dropMessage(6, "applyfrom" + hpchange);
                if (applyto.hasDisease(MapleDisease.ZOMBIFY)) {
                    hpchange = -hpchange;
                }
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getStat().getCurrentMaxHp() * hpR) / (applyto.hasDisease(MapleDisease.ZOMBIFY) ? 2 : 1);
//            hpchange += (int) (applyfrom.getStat().getCurrentMaxHp() * hpR);
        }
        // actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        switch (this.sourceid) {
            case 4211001: // Chakra
                final PlayerStats stat = applyfrom.getStat();
                int v42 = getY() + 100;
                int v38 = Randomizer.rand(1, 100) + 100;
                applyto.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 1, applyto.getLevel(), level));
                applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, applyto.getLevel(), level), false);
                hpchange = (int) ((v38 * stat.getLuk() * 0.033 + stat.getDex()) * v42 * 0.002);
                hpchange += makeHealHP(getY() / 100.0, applyfrom.getStat().getTotalLuk(), 2.3, 3.5);
                break;
        }
        return hpchange;
    }

    private static final int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1)) + (int) (stat * lowerfactor * rate));
    }

    private final int calcMPChange(final MapleCharacter applyfrom, final boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, true);
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getStat().getCurrentMaxMp() * mpR);
        }
        if (primary) {
            if (mpCon != 0) {
                /*int hacked = 0;
                 if (applyfrom.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                 hacked = 1;
                 }
                 if (applyfrom.getStat().getMp() < mpCon && applyfrom.getBuffedValue(MapleBuffStat.MAGIC_GUARD) == null && applyfrom.getStat().getMp() != 1 && this.sourceid != 3121004) {
                 //applyfrom.ban(applyfrom.getName(), false, true, false, "TEST");
                 //FileoutputUtil.log(FileoutputUtil.PacketHack_Log, "[GM Message] Level : " + applyfrom.getLevel() + "// " + applyfrom.getName() + " 님이 MP 소모량 변조에 의해 밴 되었습니다. 사용한 스킬 : " + this.sourceid + " 매직 가드 1이면 사용 : " + hacked);
                 //System.out.print("MP 소모량 변조 감지."); // 이렇게면될려나? 일단은 로그만 남겨볼려고했는데 오진있을까봐
                 //applyfrom.getClient().disconnect(true, false);

                 } else if (applyfrom.getStat().getMp() < mpCon && applyfrom.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                 applyfrom.cancelBuffStats(true, MapleBuffStat.MAGIC_GUARD);
                 }
                 if (applyfrom.getBuffedValue(MapleBuffStat.POWERGUARD) != null) {
                 applyfrom.cancelBuffStats(true, MapleBuffStat.MONSTER_RIDING);
                 }*/
                if (applyfrom.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                    mpchange = 0;
                } else {
                    Integer s = applyfrom.getBuffedValue(MapleBuffStat.CONCENTRATE);
                    int reduce = 0;
                    if (s != null) {
                        reduce += Math.floor(mpCon * s / 100.0D);
                    }
                    double mod = 1.0;

                    boolean isAFpMage = applyfrom.getJob() == 211 || applyfrom.getJob() == 212;
                    if (isAFpMage || (applyfrom.getJob() == 221 || applyfrom.getJob() == 222)) {
                        if (!overTime && (matk > 0 || watk > 0 || damage != 100)) {
                            Skill amp;
                            if (isAFpMage) {
                                amp = SkillFactory.getSkill(2110001);
                            } else {
                                amp = SkillFactory.getSkill(2210001);
                            }
                            int ampLevel = applyfrom.getSkillLevel(amp);
                            if (ampLevel > 0) {
                                MapleStatEffect ampStat = amp.getEffect(ampLevel);
                                mod = ampStat.getX() / 100.0;
                            }
                        }
                    }

                    //applyfrom.dropMessage(6, "applyfrom.getStat().mpconPercent" + applyfrom.getStat().mpconPercent);
                    mpchange -= ((mpCon * mod) - reduce) * applyfrom.getStat().mpconPercent / 100;
                    // applyfrom.dropMessage(6, "mpchange" + mpchange);

//                    mpchange -= (mpCon - (mpCon * applyfrom.getStat().mpconReduce / 100.0D)) * (applyfrom.getStat().mpconPercent / 100.0);
                }
            }
        }

        return mpchange;
    }

    public final int alchemistModifyVal2(final MapleCharacter chr, final int val, final boolean withX) {
        if (!skill) {
            return (val * (chr.getStat().BuffUP) / 100);
        }
        return (val * (withX ? chr.getStat().RecoveryUP : (chr.getStat().BuffUP_Skill + (getSummonMovementType() == null ? 0 : chr.getStat().BuffUP_Summon))) / 100);
    }//버프타이버

    //from odin
    public int alchemistModifyVal(MapleCharacter chr, int val, boolean withX) {
        //chr.dropMessage(6, "chr.getStat().RecoveryUP" + chr.getStat().RecoveryUP);
        int RecoveryUP = chr.getStat().RecoveryUP;
        if (!skill && chr.getJob() >= 411 && chr.getJob() <= 412 && RecoveryUP <= 100) {
            //chr.dropMessage(6, "d1");
            MapleStatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                return (int) (val * (alchemistEffect.getX() / 100.0));
            }
        } else if (!skill && chr.getJob() >= 411 && chr.getJob() <= 412 && RecoveryUP > 100) {
            MapleStatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                //chr.dropMessage(6, "dd?" + alchemistEffect.getX() + "dd??" + RecoveryUP);
                return (int) (val * ((alchemistEffect.getX() + RecoveryUP - 100) / 100.0));
            }
        } else if (RecoveryUP > 100) {
            return (int) ((val * (RecoveryUP)) / 100.0);
        }
        return val;
    }

    private MapleStatEffect getAlchemistEffect(MapleCharacter chr) {
        if (chr.getJob() >= 411 && chr.getJob() <= 412) {
            Skill alchemist = SkillFactory.getSkill(4110000);
            int alchemistLevel = chr.getSkillLevel(alchemist);
            if (alchemistLevel == 0) {
                return null;
            }
            return alchemist.getEffect(alchemistLevel);
        } else if (chr.getJob() == 1411) {
            Skill alchemist = SkillFactory.getSkill(14110003);
            int alchemistLevel = chr.getSkillLevel(alchemist);
            if (alchemistLevel == 0) {
                return null;
            }
            return alchemist.getEffect(alchemistLevel);
        }
        return null;
    }

    public final void setSourceId(final int newid) {
        sourceid = newid;
    }

    public final boolean isCanStrongBuff() {
        switch (sourceid) {
            case 2311003:
            case 3121002:
            case 3121000:
            case 1301007:
            case 9001008:
            case 4111001:
                return true;
        }
        return false;
    }

    public final boolean isGmBuff() {
        switch (sourceid) {
            case 9001000: // GM dispel
            case 9001001: // GM haste
            case 9001002: // GM Holy Symbol
            case 9001003: // GM Bless
            case 9001005: // GM resurrection
            case 9001008: // GM hyperbody
                return true;
            default:
                return sourceid % 10000 == 1005;
        }
    }

    public final boolean isDispelImmuneBuff() { //버프해제에 면역인 스킬
        switch (sourceid) {
            case 1026: // 플라잉
            case 10001026: // 플라잉
            case 20001026: // 플라잉
            case 20011026: // 플라잉
            case 30001026: // 플라잉

            case 8002:
            case 10008002:
            case 20008002:
            case 20018002:
            case 30008002: //쓸만한 샤프아이즈(전 직업)    

            case 1121010://인레이지    
            case 1211008://라이트닝 차지(나이트)
            case 2001002://매직 가드 모험가
            case 22111001://매직 가드 에반    
            case 2121004:// 인피니티
            case 2221004:// 인피니티
            case 2321004:// 인피니티
            case 2301004: //클레릭 블레스
            case 3121008: //집중
            case 4211005: //메소 가드
            case 21000000:// 아란 콤보
            case 22181003:// 소울스톤
            case 32111005: //슈퍼바디
            case 32001003:
            case 32101003:
            case 32101002:
            case 32110000:
            case 32120000:
            case 32120001: //오라 버프해제 면역

            case 1111002: //콤보 어택
            case 1120003: //어드밴스드 콤보
            case 1321007: //비홀더
            case 4331002: //미러 이미징
            case 4341002: //파이널 컷

            case 5211007:
            case 5111007: //럭키 다이스

            //메이플 용사
            case 1121000:
            case 1221000:
            case 1321000:
            case 2121000:
            case 2221000:
            case 2321000:
            case 3121000:
            case 3221000:
            case 4121000:
            case 4221000:
            case 4341000:
            case 5121000:
            case 5221000:
            case 21121000:
            case 22171000:

            case 35001002:
            case 35101005:
            case 35101007:
            case 35111001:
            case 35111009:
            case 35111010:
            case 35111004:
            case 35121013:
            case 35111002:
            case 35111011:
            case 35111005:
            case 35111013:
            case 35120000:
            case 35121005:
            case 35121006:
            case 35121009:
            case 35121010:
            case 35121007: //메카닉
                return true;
            default:
                return false;
        }
    }

    public final boolean isInflation() {
        return inflation > 0;
    }

    public final int getInflation() {
        return inflation;
    }

    public final boolean isEnergyCharge() {
        return skill && (sourceid == 5110001 || sourceid == 15100004);
    }

    private final boolean isMonsterBuff() {
        switch (sourceid) {
            case 1111003: // 패닉
            case 1111004:
            case 1111005: // 코마
            case 1111006:
            case 1111008: // 샤우트
            case 1201006: // 위협
            case 1311006: // 로어
            case 2101003: // fp slow
            case 2111004: // fp seal

            case 2201003: // il slow
            case 5011002:
            case 2211004: // il seal
            case 2311005: // doom
            case 4111003: // shadow web
            case 4121004: // Ninja ambush
            case 4221004: // Ninja ambush

            case 4341003: // 듀블 몬스터 봄
            case 4321002: // 듀블 플래시 뱅
            case 22121000: // 에반 아이스 브레스
            case 22151001: // 브레스
            case 22161002: // 고스트 레터링

            case 32120000:
            case 32120001: //배메

            case 90001002:
            case 90001003:
            case 90001004:
            case 90001005:
            case 90001006: // 드래고닉 무기 스킬
                return skill;
        }
        return false;
    }

    public final void setPartyBuff(boolean pb) {
        this.partyBuff = pb;
    }

    private final boolean isPartyBuff() {
        if (lt == null || rb == null || !partyBuff) {
            return false;
        }
        if (isDispel()) {
            return true;
        }
        switch (sourceid) {
            case 2311003: // Holy Symbol: self-only
            case 1211003:
            case 1211004:
            case 1211005:
            case 1211006:
            case 1211007:
            case 1211008:
            case 1221003:
            case 1221004:
            case 1311006: //dragon roar
            case 4311001: // 셀프 헤이스트
            case 12101005:
            case 21111005://스노우 차지
            case 33101006://스왈로우 파티버프가 아님
                return false;
        }
        if (GameConstants.isNoDelaySkill(sourceid)) {
            return false;
        } else if (GameConstants.isDecentSkill(sourceid)) {
            return false;
        }
        return true;
    }

    public final boolean isArcane() {
        return skill && (sourceid == 2320011 || sourceid == 2220010 || sourceid == 2120010);
    }

    public final boolean isHeal() {
        return skill && (sourceid == 2301002 || sourceid == 9001000 || sourceid == 9001000);
    }

    public final boolean isResurrection() {
        return skill && (sourceid == 9001005 || sourceid == 2321006 || sourceid == 9001005);
    }

    public final boolean isTimeLeap() {
        return skill && sourceid == 5121010;
    }

    public final short getHp() {
        return hp;
    }

    public final short getMp() {
        return mp;
    }

    public final double getHpR() {
        return hpR;
    }

    public final double getMpR() {
        return mpR;
    }

    public final byte getMastery() {
        return mastery;
    }

    public final short getWatk() {
        return watk;
    }

    public final short getMatk() {
        return matk;
    }

    public final short getWdef() {
        return wdef;
    }

    public final short getMdef() {
        return mdef;
    }

    public final int getAcc() {
        return acc;
    }

    public final int getAvoid() {
        return avoid;
    }

    public final short getHands() {
        return hands;
    }

    public final short getSpeed() {
        return speed;
    }

    public final short getJump() {
        return jump;
    }

    public final int getDuration() {
        return duration;
    }

    public final boolean isOverTime() {
        return overTime;
    }

    public final Map<MapleBuffStat, Integer> getStatups() {
        return statups;
    }

    public final boolean sameSource(final MapleStatEffect effect) {
        boolean sameSrc = this.sourceid == effect.sourceid;
        switch (this.sourceid) { // All these are passive skills, will have to cast the normal ones.
            case 32120000: // Advanced Dark Aura
                sameSrc = effect.sourceid == 32120000;
                break;
            case 32110000: // Advanced Blue Aura
                sameSrc = effect.sourceid == 32110000;
                break;
            case 32120001: // Advanced Yellow Aura
                sameSrc = effect.sourceid == 32120001;
                break;
            case 35120000: // Extreme Mech
                sameSrc = effect.sourceid == 35001002;
                break;
//            case 35121013: // Mech: Siege Mode
//                sameSrc = effect.sourceid == 35121013;
//                break;
        }
        return effect != null && sameSrc && this.skill == effect.skill;

    }

    public final short getCriticalMax() {
        return criticaldamageMax;
    }

    public final short getCriticalMin() {
        return criticaldamageMin;
    }

    public final short getASRRate() {
        return asrR;
    }

    public final short getTERRate() {
        return terR;
    }

    public short getWDEFRate() {
        return pddR;
    }

    public short getMDEFRate() {
        return mddR;
    }

    public final short getDAMRate() {
        return damR;
    }

    public final int getCr() {
        return cr;
    }

    public final int getT() {
        return t;
    }

    public final int getU() {
        return u;
    }

    public final int getV() {
        return v;
    }

    public final int getW() {
        return w;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getZ() {
        return z;
    }

    public final short getDamage() {
        return damage;
    }

    public final byte getAttackCount() {
        return attackCount;
    }

    public final byte getBulletCount() {
        return bulletCount;
    }

    public final int getBulletConsume() {
        return bulletConsume;
    }

    public final byte getMobCount() {
        return mobCount;
    }

    public final int getMoneyCon() {
        return moneyCon;
    }

    public final int getCooldown() {
        return cooldown;
    }

    public final Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }

    public final int getBerserk() {
        return berserk;
    }

    public final boolean isHide() {
        return skill && (sourceid == 9001004 || sourceid == 9001004);
    }

    public final boolean isDragonBlood() {
        return skill && sourceid == 1311008;
    }

    public final boolean isRecovery() {
        return skill && (sourceid == 1001 || sourceid == 10001001 || sourceid == 20001001 || sourceid == 20011001 || sourceid == 20021001 || sourceid == 11001 || sourceid == 35121005);
    }

    public final boolean isBerserk() {
        return skill && sourceid == 1320006;
    }

    public final boolean isBeholder() {
        return skill && sourceid == 1321007;
    }

    public final boolean isMPRecovery() {
        return skill && sourceid == 5101005;
    }

    public final boolean isInfinity() {
        return skill && (sourceid == 2121004 || sourceid == 2221004 || sourceid == 2321004);
    }

    public final boolean isMonsterRiding_() {
        return skill && (sourceid == 1004 || sourceid == 10001004 || sourceid == 20001004 || sourceid == 20011004 || sourceid == 11004 || sourceid == 30001004 || sourceid == 80001000);
    }

    public final boolean isMonsterRiding() {
        return skill && (isMonsterRiding_() || GameConstants.getMountItem(sourceid, null) != 0);
    }

    public final boolean isMagicDoor() {
        return skill && (sourceid == 2311002 || sourceid % 10000 == 8001);
    }

    public final boolean isMesoGuard() {
        return skill && sourceid == 4211005;
    }

    public final boolean isMechDoor() {
        return skill && sourceid == 35101005;
    }

    public final boolean isComboRecharge() {
        return skill && sourceid == 21111009;
    }

    public final boolean isDragonBlink() {
        return skill && sourceid == 22141004;
    }

    public final boolean isCharge() {
        switch (sourceid) {
            case 1211003:
            case 1211008:
            case 11111007:
            case 12101005:
            case 15101006:
            case 21111005:
                return skill;
        }
        return false;
    }

    public final boolean isPoison() {
        return (dot > 0 && dotTime > 0) || sourceid == 2101005 || sourceid == 2111006 || sourceid == 2121003 || sourceid == 2221003 || sourceid == 5211004 || sourceid == 32120000;
    }

    private final boolean isMist() {
        return skill && (sourceid == 2111003/*포이즌 미스트*/ || sourceid == 4221006/*연만탄*/ || sourceid == 12111005 /*플레임 기어*/ || sourceid == 14111006/*포이즌 붐*/ || sourceid == 22161003/*리커버리 오로라*/ || sourceid == 32121006/*쉘터*/ || sourceid == 1076/*오즈의 플레임 기어*/);
    }

    private final boolean isSpiritClaw() {
        return skill && sourceid == 4121006;
    }

    private final boolean isDispel() {
        return skill && (sourceid == 9001000 || sourceid == 2311001 || sourceid == 9001000);
    }

    private final boolean isHeroWill() {
        switch (sourceid) {
            case 1121011:
            case 1221012:
            case 1321010:
            case 2121008:
            case 2221008:
            case 2321009:
            case 3121009:
            case 3221008:
            case 4121009:
            case 4221008:
            case 5121008:
            case 5221010:
            case 21121008:
            case 22171004:
            case 4341008:
            case 32121008:
            case 33121008:
            case 35121008:
            case 5321008:
            case 23121008:
                return skill;
        }
        return false;
    }

    public final boolean isAranCombo() {
        return sourceid == 21000000;
    }

    public final boolean isCombo() {
        switch (sourceid) {
            case 1111002:
            case 11111001: // Combo
                return skill;
        }
        return false;
    }

    public final boolean isPirateMorph() {
        switch (sourceid) {
            case 13111005:
            case 15111002:
            case 5111005:
            case 5121003:
                return skill;
        }
        return false;
    }

    public final boolean isMorph() {
        return morphId > 0;
    }

    public final int getMorph() {
        switch (sourceid) {
            case 15111002:
            case 5111005:
                return 1000;
            case 5121003:
                return 1001;
            case 5101007:
                return 1002;
            case 13111005:
                return 1003;
        }
        return morphId;
    }

    public final boolean isDivineBody() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1010;
    }

    public final boolean isDivineShield() {
        switch (sourceid) {
            case 1220013:
                return skill;
        }
        return false;
    }

    public final boolean isBerserkFury() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1011;
    }

    public final int getMorph(final MapleCharacter chr) {
        final int morph = getMorph();
        switch (morph) {
            case 1000:
            case 1001:
            case 1003:
                return morph + (chr.getGender() == 1 ? 100 : 0);
        }
        return morph;
    }

    public final byte getLevel() {
        return level;
    }

    public final SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        switch (sourceid) {
            case 3211002: // puppet sniper
            case 3111002: // puppet ranger
            case 4111007: // 다크 플레어
            case 4211007: // 다크 플레어
            case 4341006: // 더미 이펙트
            case 5211001: // 문어
            case 5220002: // 향상된 문어
            case 13111004: // 퍼펫(윈드브레이커)
            case 33101008: // 레이닝 마인
            case 33111003: // 와일드 트랩
            case 35111002: // 마그네틱 필드
            case 35111005: // 엑셀러레이터
            case 35111011: // 힐링로봇
            case 35121003: // 워머신
            case 35121009: // 로보 팩토리
            case 35121010: // 앰플리파이어
                return SummonMovementType.STATIONARY;
            case 3211005: // golden eagle
            case 3111005: // golden hawk
            case 2311006: // summon dragon
            case 3221005: // frostprey
            case 3121006: // phoenix
            case 5211002: // bird - pirate
            case 33111005: // golden hawk
                return SummonMovementType.CIRCLE_FOLLOW;
            case 32111006: //reaper
                return SummonMovementType.WALK_STATIONARY;
            case 1321007: // beholder
            case 2121005: // 이프리트
            case 2221005: // 엘퀴네스
            case 2321003: // bahamut
            case 11001004: // 소울
            case 12001004: // 플레임
            case 12111004: // 이프리트(플레임위자드)
            case 13001004: // 스톰
            case 14001005: // 다크니스
            case 15001004: // 라이트닝
            case 35111001: // 새틀라이트
            case 35111009: // 새틀라이트
            case 35111010: // 새틀라이트
                return SummonMovementType.FOLLOW;
        }
        return null;
    }

    public final boolean isAngel() {
        return GameConstants.isAngel(sourceid);
    }

    public final boolean isSkill() {
        return skill;
    }

    public final boolean isPotion() {
        return isPotion;
    }

    public final int getSourceId() {
        return sourceid;
    }

    public final boolean isIceKnight() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1105;
    }

    public final boolean isSoaring() {
        return isSoaring_Normal() || isSoaring_Mount();
    }

    public final boolean isSoaring_Normal() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1026;
    }

    public final boolean isSoaring_Mount() {
        return skill && ((GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1142) || sourceid == 80001089);
    }

    public final boolean isFinalAttack() {
        switch (sourceid) {
            case 13101002:
            case 11101002:
                return skill;
        }
        return false;
    }

    public final boolean isMistEruption() {
        switch (sourceid) {
            case 2111003:
                return skill;
        }
        return false;
    }

    public final boolean isShadow() {
        switch (sourceid) {
            case 4111002: // shadowpartner
            case 14111000: // 쉐도우 파트너(나이트워커)
            case 4211008:
                return skill;
        }
        return false;
    }

    /**
     * @return true if the effect should happen based on it's probablity, false
     * otherwise
     */
    public final boolean makeChanceResult() {
        return prop >= 100 || Randomizer.nextInt(100) < prop;
    }

    public final int getEnhancedHP() {
        return ehp;
    }

    public final int getEnhancedMP() {
        return emp;
    }

    public final int getEnhancedWatk() {
        return ewatk;
    }

    public final int getEnhancedWdef() {
        return ewdef;
    }

    public final int getEnhancedMdef() {
        return emdef;
    }

    public final short getProb() {
        return prop;
    }

    public final short getIgnoreMob() {
        return ignoreMob;
    }

    public final short getDOT() {
        return dot;
    }

    public final short getDOTTime() {
        return dotTime;
    }

    public final short getMesoRate() {
        return mesoR;
    }

    public final int getEXP() {
        return exp;
    }

    public final short getAttackX() {
        return padX;
    }

    public final short getMagicX() {
        return madX;
    }

    public final int getPercentHP() {
        return mhpR;
    }

    public final int getPercentMP() {
        return mmpR;
    }

    public final int getConsume() {
        return consumeOnPickup;
    }

    public final int getSelfDestruction() {
        return selfDestruction;
    }

    public final int getCharColor() {
        return charColor;
    }

    public final List<Integer> getPetsCanConsume() {
        return petsCanConsume;
    }

    public final boolean isMechChange() {
        switch (sourceid) {
            case 35001002:
            case 35111004: //siege
            case 35001001: //flame
            case 35101009:
            case 35121013:
            case 35120000:
            case 35121005:
                return skill;
        }
        return false;
    }

    public final int getRange() {
        return range;
    }

    public final short getER() {
        return er;
    }

    public final int getPrice() {
        return price;
    }

    public final int getExtendPrice() {
        return extendPrice;
    }

    public final byte getPeriod() {
        return period;
    }

    public final short getLifeID() {
        return lifeId;
    }

    public final short getUseLevel() {
        return useLevel;
    }

    public final byte getSlotCount() {
        return slotCount;
    }

    public final short getMPConReduce() {
        return mpConReduce;
    }

    public final short getMPCon() {
        return mpCon;
    }

    public final byte getType() {
        return type;
    }

    public int getInterval() {
        return interval;
    }

    public ArrayList<Pair<Integer, Integer>> getAvailableMaps() {
        return availableMap;
    }

    public final void applyComboBuff(final MapleCharacter applyto, short combo) {
        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        stat.put(MapleBuffStat.ARAN_COMBO, (int) combo);
        applyto.getClient().getSession().write(TemporaryStatsPacket.giveBuff(sourceid, 99999, stat, this)); // Hackish timing, todo find out

        final long starttime = System.currentTimeMillis();
//	final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
//	final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + 99999) - System.currentTimeMillis()));
        applyto.registerEffect(this, starttime, null, applyto.getId());
    }

    public final void applyEnergyBuff(final MapleCharacter applyto, final boolean infinity) {
        final long starttime = System.currentTimeMillis();
        if (infinity) {
            applyto.getClient().getSession().write(TemporaryStatsPacket.giveEnergyChargeTest(0, duration / 1000));
            applyto.registerEffect(this, starttime, null, applyto.getId());
        } else {
            final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
            stat.put(MapleBuffStat.ENERGY_CHARGE, 10000);
            applyto.cancelEffect(this, -1, stat, true, false);
            applyto.getClient().getSession().write(TemporaryStatsPacket.giveEnergyChargeTest(10000, duration / 1000));
            applyto.getMap().broadcastMessage(applyto, TemporaryStatsPacket.giveEnergyChargeTest(applyto.getId(), 10000, duration / 1000), false);
            final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, stat);
            final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, ((starttime + duration) - System.currentTimeMillis()));
            applyto.registerEffect(this, starttime, schedule, stat, false, duration, applyto.getId());

        }
    }

    private int getMpCon() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static class CancelEffectAction implements Runnable {

        private final MapleStatEffect effect;
        private final WeakReference<MapleCharacter> target;
        private final long startTime;
        private final Map<MapleBuffStat, Integer> statup;

        public CancelEffectAction(final MapleCharacter target, final MapleStatEffect effect, final long startTime, final Map<MapleBuffStat, Integer> statup) {
            this.effect = effect;
            this.target = new WeakReference<MapleCharacter>(target);
            this.startTime = startTime;
            this.statup = statup;
        }

        @Override
        public void run() {
            final MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, startTime, statup, true, false);
            }
        }
    }
}
