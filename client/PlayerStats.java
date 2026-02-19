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

import client.inventory.Equip;
import client.inventory.EquipAdditions;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import constants.GameConstants;
import handling.channel.handler.DamageParse;
import handling.world.CharacterTransfer;
import server.ItemInfo;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.SetItemInfo;
import tools.MaplePacketCreator;
import tools.data.MaplePacketLittleEndianWriter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import server.MapleInventoryManipulator;
import server.Randomizer;
import server.StructPotentialItem;
import server.StructSetItem;
import server.StructSetItem.SetItem;
import server.life.Element;
import tools.Pair;

public class PlayerStats implements Serializable {

    private static final long serialVersionUID = -679541993413738569L;
    private Map<Integer, Integer> setHandling = new HashMap<Integer, Integer>(), skillsIncrement = new HashMap<Integer, Integer>(), damageIncrease = new HashMap<Integer, Integer>();
    private EnumMap<Element, Integer> elemBoosts = new EnumMap<Element, Integer>(Element.class);
    private transient float shouldHealHP, shouldHealMP;
    public short str, dex, luk, int_;
    public int hp, maxhp, mp, maxmp;
    private transient short passive_sharpeye_min_percent, passive_sharpeye_percent, passive_sharpeye_rate;
    private transient int passive_sharpeye_damage;
    private transient int localstr, localdex, localluk, localint_, localmaxhp, localmaxmp;
    private transient int magic, watk, hands, accuracy;
    public transient boolean equippedWelcomeBackRing, hasClone, hasPartyBonus, Berserk, Fury, canFish, canFishVIP;
    public transient double expBuff, dropBuff, mesoBuff, cashBuff, mesoGuard, mesoGuardMeso, expMod, eventExpRate, pickupRange;
    //same with incMesoProp/incRewardProp for now
    public transient double dam_r, bossdam_r, localmaxbasedamage, localminbasedamage, localmaxmagicdamage;
    public transient int recoverHP, recoverMP, mpconReduce, mpconPercent, incMesoProp, incRewardProp, DAMreflect, DAMreflect_rate, mpRestore, TER,
            mpRecoverW, hpRecoverW, hpRecover, hpRecoverProp, hpRecoverPotential, hpRecoverPropPotential, mpRecoverPotential, mpRecoverPropPotential, hpRecoverPercent, mpRecover, mpRecoverProp, RecoveryUP, BuffUP, RecoveryUP_Skill, BuffUP_Skill,
            incAllskill, combatOrders, ignoreTargetDEF, defRange, BuffUP_Summon, dodgeChance, speed, jump, harvestingTool,
            equipmentBonusExp, dropMod, cashMod, levelBonus, ASR, pickRate, decreaseDebuff, equippedFairy, equippedSummon,
            percent_hp, percent_mp, percent_str, percent_dex, percent_int, percent_luk, percent_acc, percent_atk, percent_matk, percent_wdef, percent_mdef,
            pvpDamage, hpRecoverTime = 0, mpRecoverTime = 0, dot, dotTime, questBonus, pvpRank, pvpExp, wdef, mdef, trueMastery, FuryValue;
    private transient float localmaxbasepvpdamage, localmaxbasepvpdamageL;
    // Elemental properties
    public transient int def, element_ice, element_fire, element_light, element_psn;
    public List<Equip> equipLevelHandling = new ArrayList<Equip>();
    public List<Equip> durabilityHandling = new ArrayList<Equip>();

    //acc/avoid: 4000000, 5000000
    //POTENTIALS:
    //incMesoProp, incRewardProp
    public final void init(MapleCharacter chra) {
        recalcLocalStats(chra);
    }

    public final short getStr() {
        return str;
    }

    public final short getDex() {
        return dex;
    }

    public final short getLuk() {
        return luk;
    }

    public final short getInt() {
        return int_;
    }

    public final void setStr(final short str, MapleCharacter chra) {
        this.str = str;
        recalcLocalStats(chra);
    }

    public final void setDex(final short dex, MapleCharacter chra) {
        this.dex = dex;
        recalcLocalStats(chra);
    }

    public final void setLuk(final short luk, MapleCharacter chra) {
        this.luk = luk;
        recalcLocalStats(chra);
    }

    public final void setInt(final short int_, MapleCharacter chra) {
        this.int_ = int_;
        recalcLocalStats(chra);
    }

    public final void setInfo(final int maxhp, final int maxmp, final int hp, final int mp) {
        this.maxhp = maxhp;
        this.maxmp = maxmp;
        this.hp = hp;
        this.mp = mp;
    }

    public final void setMaxHp(final int hp, MapleCharacter chra) {
        this.maxhp = hp;
        recalcLocalStats(chra);
    }

    public final void setMaxMp(final int mp, MapleCharacter chra) {
        this.maxmp = mp;
        recalcLocalStats(chra);
    }

    public final int getHp() {
        return hp;
    }

    public final int getMaxHp() {
        return maxhp;
    }

    public final int getMp() {
        return mp;
    }

    public final int getMaxMp() {
        return maxmp;
    }

    public final int getTotalDex() {
        return localdex;
    }

    public final int getTotalInt() {
        return localint_;
    }

    public final int getTotalStr() {
        return localstr;
    }

    public final int getTotalLuk() {
        return localluk;
    }

    public final int getTotalMagic() {
        return magic;
    }

    public final int getSpeed() {
        return speed;
    }

    public final int getJump() {
        return jump;
    }

    public final int getTotalWatk() {
        return watk;
    }

    public final int getCurrentMaxHp() {
        return localmaxhp;
    }

    public final int getCurrentMaxMp() {
        return localmaxmp;
    }

    public final int getHands() {
        return hands;
    }

    public final double getCurrentMaxBaseDamage() {
        return Math.max(localmaxbasedamage, localmaxmagicdamage);
    }

    public final double getCurrentMinBaseDamage() {
        return localminbasedamage;
    }

    public final float getCurrentMaxBasePVPDamage() {
        return localmaxbasepvpdamage;
    }

    public final float getCurrentMaxBasePVPDamageL() {
        return localmaxbasepvpdamageL;
    }

    public void recalcLocalStats(MapleCharacter chra) {
        recalcLocalStats(false, chra);
    }

    public void recalcLocalStats(boolean first_login, MapleCharacter chra) {
        recalcLocalStats(false, chra, true);
    }

    public void recalcLocalStats(boolean first_login, MapleCharacter chra, boolean again) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int oldmaxhp = localmaxhp;
        int localmaxhp_ = getMaxHp();
        int localmaxmp_ = getMaxMp();
        StructPotentialItem pot;
        accuracy = 0;
        wdef = 0;
        mdef = 0;
        localdex = getDex();
        localint_ = getInt();
        localstr = getStr();
        localluk = getLuk();
        speed = 100;
        jump = 100;
        pickupRange = 0.0;
        decreaseDebuff = 0;
        ASR = 0;
        TER = 0;
        dot = 0;
        questBonus = 1;
        dotTime = 0;
        trueMastery = 0;
        percent_wdef = 0;
        percent_mdef = 0;
        percent_hp = 0;
        percent_mp = 0;
        percent_str = 0;
        percent_dex = 0;
        percent_int = 0;
        percent_luk = 0;
        percent_acc = 0;
        percent_atk = 0;
        percent_matk = 0;
        passive_sharpeye_rate = 5;
        passive_sharpeye_min_percent = 20;
        passive_sharpeye_percent = 50;
        incAllskill = 0;
        combatOrders = 0;
        magic = 0;
        watk = 0;
        localmaxmagicdamage = 0;
        if (chra.getJob() == 500 || (chra.getJob() >= 520 && chra.getJob() <= 522)) {
            watk = 20; //bullet
        } else if (chra.getJob() == 400 || (chra.getJob() >= 410 && chra.getJob() <= 412) || (chra.getJob() >= 1400 && chra.getJob() <= 1412)) {
            watk = 30; //stars
        }

        equipLevelHandling.clear();
        skillsIncrement.clear();
        damageIncrease.clear();
        setHandling.clear();

        dodgeChance = 0;
        pvpDamage = 0;
        mesoGuard = 0.0;
        mesoGuardMeso = 0.0;
        dam_r = 100.0;
        bossdam_r = 100.0;
        expBuff = 100.0;
        cashBuff = 100.0;
        dropBuff = 100.0;
        mesoBuff = 100.0;
        recoverHP = 0;
        recoverMP = 0;
        mpconReduce = 0;
        mpconPercent = 100;
        incMesoProp = 0;
        incRewardProp = 0;
        DAMreflect = 0;
        DAMreflect_rate = 0;
        ignoreTargetDEF = 0;
        hpRecover = 0;
        hpRecoverProp = 0;
        hpRecoverPotential = 0;
        hpRecoverPropPotential = 0;
        hpRecoverPercent = 0;
        mpRecover = 0;
        mpRecoverProp = 0;
        mpRecoverPotential = 0;
        mpRecoverPropPotential = 0;
        mpRestore = 0;
        pickRate = 0;
        Berserk = false;
        equipmentBonusExp = 0;
        expMod = 1.0;
        cashMod = 1;
        defRange = 0;
        durabilityHandling.clear();
        BuffUP = 100;
        RecoveryUP = 100;
        RecoveryUP_Skill = 100;
        BuffUP_Skill = 100;
        BuffUP_Summon = 100;
        def = 100;
        if (!first_login) {
            //createOrUpdateSetOptionEquip(chra, true);
        }

        int lvHp = 0;
        int lvMp = 0;
        if (chra.getJob() >= 100 && chra.getJob() <= 132 || chra.getJob() >= 1100 && chra.getJob() <= 1112 || chra.getJob() >= 2100 && chra.getJob() <= 2112) {
            lvHp += localstr * 4;
            lvHp += localdex * 5;
        }
        if (chra.getJob() >= 3200 && chra.getJob() <= 3212) {
            lvHp += localint_ * 4;
            lvHp += localluk * 5;
        }
        if (chra.getJob() >= 200 && chra.getJob() <= 232 || chra.getJob() >= 1200 && chra.getJob() <= 1212 || chra.getJob() >= 2200 && chra.getJob() <= 2218 || chra.getJob() >= 3200 && chra.getJob() <= 3212) {
            lvMp += localint_ * 10;
            lvMp += localluk * 15;
        }
        if (chra.getJob() >= 300 && chra.getJob() <= 322 || chra.getJob() >= 1300 && chra.getJob() <= 1312 || chra.getJob() >= 3300 && chra.getJob() <= 3312) {
            lvHp += localdex * 4;
            lvHp += localstr * 5;
        }
        if (chra.getJob() >= 400 && chra.getJob() <= 434 || chra.getJob() >= 1400 && chra.getJob() <= 1412) {
            lvHp += localluk * 4;
            lvHp += localdex * 5;
        }
        if (chra.getJob() >= 420 && chra.getJob() <= 434) {//시프부터 듀블까지
            lvHp += localstr * 3;
        }
        if (chra.getJob() == 500) {
            lvHp += localdex * 4;
            lvHp += localstr * 5;
        }
        if (chra.getJob() >= 520 && chra.getJob() <= 522 || chra.getJob() >= 3500 && chra.getJob() <= 3512) {
            lvHp += localdex * 4;
            lvHp += localstr * 5;
        }
        if (chra.getJob() >= 510 && chra.getJob() <= 512 || chra.getJob() >= 1500 && chra.getJob() <= 1512) {
            lvHp += localstr * 4;
            lvHp += localdex * 5;
        }
        if (chra.getJob() == 0 || chra.getJob() == 1000 || chra.getJob() == 2000) {
            lvHp += localstr * 4;
            lvHp += localdex * 5;
        }
        int finalmaxhp = maxhp + lvHp;
        if (finalmaxhp >= 99999) {
            finalmaxhp = 99999;
        }
        int finalmaxmp = maxmp + lvMp;
        if (finalmaxmp >= 99999) {
            finalmaxmp = 99999;
        }
        if (!first_login & again) {//
            chra.updateSingleStat(MapleStat.MAXHP, finalmaxhp);
            chra.updateSingleStat(MapleStat.MAXMP, finalmaxmp);
        }
        localmaxhp_ += lvHp;
        localmaxmp_ += lvMp;

        final Iterator<Item> itera = chra.getInventory(MapleInventoryType.EQUIPPED).newList().iterator();
        while (itera.hasNext()) {
            final Equip equip = (Equip) itera.next();
            accuracy += equip.getAcc();
            localmaxhp_ += equip.getHp();
            localmaxmp_ += equip.getMp();
            localdex += equip.getDex();
            localint_ += equip.getInt();
            localstr += equip.getStr();
            localluk += equip.getLuk();
            magic += equip.getMatk();
            watk += equip.getWatk();
            wdef += equip.getWdef();
            mdef += equip.getMdef();
            speed += equip.getSpeed();
            jump += equip.getJump();
            percent_hp += equip.getHpR();
            percent_mp += equip.getMpR();
            Integer set = ii.getSetItemID(equip.getItemId());
            if (set != null && set > 0) {
                int value = 1;
                if (setHandling.containsKey(set)) {
                    value += setHandling.get(set).intValue();
                }
                setHandling.put(set, value); //id of Set, number of items to go with the set
            }

            if (equip.getIncSkill() > 0 && ii.getEquipSkills(equip.getItemId()) != null) { //장비스킬레벨 부분같음
                for (int zzz : ii.getEquipSkills(equip.getItemId())) {
                    final Skill skil = SkillFactory.getSkill(zzz);
                    if (skil != null && skil.canBeLearnedBy(chra.getJob())) { //dont go over masterlevel :D
                        int value = 1;
                        if (skillsIncrement.get(skil.getId()) != null) {
                            value += skillsIncrement.get(skil.getId());
                        }
                        skillsIncrement.put(skil.getId(), value);
                    }
                }
            }
            EnumMap<EquipAdditions, Pair<Integer, Integer>> additions = ii.getEquipAdditions(equip.getItemId()); //세트아이템
            if (additions != null) {
                for (Map.Entry<EquipAdditions, Pair<Integer, Integer>> add : additions.entrySet()) {
                    switch (add.getKey()) {
                        case elemboost:
                            int value = add.getValue().right;
                            Element key = Element.getFromId(add.getValue().left);
                            if (elemBoosts.get(key) != null) {
                                value += elemBoosts.get(key);
                            }
                            elemBoosts.put(key, value);//뎀지계산용
                            break;
                        case mobcategory: //skip the category, thinkings too expensive to have yet another Map<Integer, Integer> for damage calculations
                            dam_r *= (add.getValue().right + 100.0) / 100.0;
                            bossdam_r += (add.getValue().right + 100.0) / 100.0;
                            break;
                        case critical:
                            passive_sharpeye_rate += add.getValue().left;
                            passive_sharpeye_min_percent += add.getValue().right;
                            passive_sharpeye_percent += add.getValue().right; //???CONFIRM - not sure if this is max or minCritDmg
                            break;
                        case boss:
                            bossdam_r *= (add.getValue().right + 100.0) / 100.0;
                            break;
                        case mobdie://잘못된 방식인듯
                            if (add.getValue().left > 0) {
                                hpRecoverW += add.getValue().left; //no indication of prop, so i made myself
                                //hpRecoverProp += 5;
                            }
                            if (add.getValue().right > 0) {
                                mpRecoverW += add.getValue().right; //no indication of prop, so i made myself
                                //mpRecoverProp += 5;
                            }
                            break;
                        case skill: //now, i'm a bit iffy on this one
                            if (first_login) {
                                chra.changeSkillLevel_Skip(SkillFactory.getSkill(add.getValue().left), (byte) (int) add.getValue().right, (byte) 0);
                            }
                            break;
                        case hpmpchange:
                            recoverHP += add.getValue().left;
                            recoverMP += add.getValue().right;
                            break;
                    }
                }
            }

            if (equip.getState() > 1) {
                int[] potentials = {equip.getPotential1(), equip.getPotential2(), equip.getPotential3()};
                for (int i : potentials) {
                    if (i > 0) {
                        byte level = (byte) (ii.getReqLevel(equip.getItemId()) / 10);
                        if (i == 5 || i == 6 || i == 10005 || i == 10006) { //maxhp mp는 다 이러네 시발, 다른것도 그럴수있음 서버에 처리해주는부분 다 -1해야할지도? 이유를 몰겠네
                            level -= 1;
                            if (level < 0) {
                                level = 0;
                            }
                        }
                        pot = ii.getPotentialInfo(i).get(level);
                        if (pot != null) {
                            localmaxhp_ += pot.incMHP;
                            localmaxmp_ += pot.incMMP;
                            handlePotential(pot, chra, first_login);
                        }
                    }
                }
            }
            if (equip.getDurability() > 0) {
                durabilityHandling.add((Equip) equip);
            }
            if (GameConstants.getMaxLevel(equip.getItemId()) > 0 && (GameConstants.getStatFromWeapon(equip.getItemId()) == null ? (equip.getEquipLevel() <= GameConstants.getMaxLevel(equip.getItemId())) : (equip.getEquipLevel() < GameConstants.getMaxLevel(equip.getItemId())))) {
                equipLevelHandling.add((Equip) equip);
            }

        }

        eventExpRate = 0;
        for (Item item : chra.getInventory(MapleInventoryType.CASH).newList()) {
            if (item.getItemId() / 10000 == 521) {
                eventExpRate = 1; //super-hardcoded
            }
        }
        final Iterator<Map.Entry<Integer, Integer>> iter = setHandling.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<Integer, Integer> entry = iter.next();
            final StructSetItem set = ii.getSetItem(entry.getKey());
            if (set != null) {
                final Map<Integer, SetItem> itemz = set.getItems();
                for (Map.Entry<Integer, SetItem> ent : itemz.entrySet()) {
                    if (ent.getKey() <= entry.getValue()) {
                        SetItem se = ent.getValue();
                        localstr += se.incSTR + se.incAllStat;
                        localdex += se.incDEX + se.incAllStat;
                        localint_ += se.incINT + se.incAllStat;
                        localluk += se.incLUK + se.incAllStat;
                        watk += se.incPAD;
                        magic += se.incMAD;
                        speed += se.incSpeed;
                        accuracy += se.incACC;
                        localmaxhp_ += se.incMHP;
                        localmaxmp_ += se.incMMP;
                        percent_hp += se.incMHPr;
                        percent_mp += se.incMMPr;
                        wdef += se.incPDD;
                        mdef += se.incMDD;
                        if (se.option1 > 0 && se.option1Level > 0) {
                            pot = ii.getPotentialInfo(se.option1).get(se.option1Level);
                            if (pot != null) {
                                localmaxhp_ += pot.incMHP;
                                localmaxmp_ += pot.incMMP;
                                handlePotential(pot, chra, first_login);
                            }
                        }
                        if (se.option2 > 0 && se.option2Level > 0) {
                            pot = ii.getPotentialInfo(se.option2).get(se.option2Level);
                            if (pot != null) {
                                localmaxhp_ += pot.incMHP;
                                localmaxmp_ += pot.incMMP;
                                handlePotential(pot, chra, first_login);
                            }
                        }
                    }
                }
            }
        }
        //잠재능력 퍼센트
        this.localstr += Math.floor((localstr * percent_str) / 100.0f);
        this.localdex += Math.floor((localdex * percent_dex) / 100.0f);
        this.localint_ += Math.floor((localint_ * percent_int) / 100.0f);
        this.localluk += Math.floor((localluk * percent_luk) / 100.0f);

        Skill bx;
        int bof;
        MapleStatEffect eff = chra.getStatForBuff(MapleBuffStat.MONSTER_RIDING);
        if (eff != null && eff.getSourceId() == 33001001) { //jaguar
            passive_sharpeye_rate += eff.getW();
            percent_hp += eff.getZ();
        }
        MapleStatEffect eff2 = chra.getStatForBuff(MapleBuffStat.FELINE_BERSERK);
        if (eff2 != null && eff2.getSourceId() == 33121006) { //jaguar
            percent_hp += eff2.getX();
        }

        Integer buff = chra.getBuffedValue(MapleBuffStat.DICE_ROLL);
        if (buff != null) {
            percent_wdef += GameConstants.getDiceStat(buff.intValue(), 2);
            percent_mdef += GameConstants.getDiceStat(buff.intValue(), 2);
            percent_hp += GameConstants.getDiceStat(buff.intValue(), 3);
            percent_mp += GameConstants.getDiceStat(buff.intValue(), 3);
            passive_sharpeye_rate += GameConstants.getDiceStat(buff.intValue(), 4);
            dam_r *= (GameConstants.getDiceStat(buff.intValue(), 5) + 100.0) / 100.0;
            bossdam_r *= (GameConstants.getDiceStat(buff.intValue(), 5) + 100.0) / 100.0;
            expBuff += (GameConstants.getDiceStat(buff.intValue(), 6));
        }

        switch (chra.getJob()) {
            case 200:
            case 210:
            case 211:
            case 212:
            case 220:
            case 221:
            case 222:
            case 230:
            case 231:
            case 232: {
                bx = SkillFactory.getSkill(12);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                }
                bx = SkillFactory.getSkill(2000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                break;
            }
            case 1200:
            case 1210:
            case 1211:
            case 1212: {
                bx = SkillFactory.getSkill(12000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                break;
            }
            case 1100:
            case 1110:
            case 1111:
            case 1112: {
                bx = SkillFactory.getSkill(11000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            /*case 3001:
             case 3100:
             case 3110:
             case 3111:
             case 3112:
             mpRecoverProp = 100;
             bx = SkillFactory.getSkill(31000003);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             percent_hp += bx.getEffect(bof).getHpR();
             }
             bx = SkillFactory.getSkill(31100007);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             damageIncrease.put(31000004, (int) eff.getDAMRate());
             damageIncrease.put(31001006, (int) eff.getDAMRate());
             damageIncrease.put(31001007, (int) eff.getDAMRate());
             damageIncrease.put(31001008, (int) eff.getDAMRate());
             }
             bx = SkillFactory.getSkill(31100005);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             localstr += eff.getStrX();
             localdex += eff.getDexX();
             }
             bx = SkillFactory.getSkill(31100010);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             damageIncrease.put(31000004, (int) eff.getX());
             damageIncrease.put(31001006, (int) eff.getX());
             damageIncrease.put(31001007, (int) eff.getX());
             damageIncrease.put(31001008, (int) eff.getX());
             }
             bx = SkillFactory.getSkill(31111007);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
             bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
             }
             bx = SkillFactory.getSkill(31110008);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             dodgeChance += eff.getX();
             hpRecoverPercent += eff.getY();
             hpRecoverProp += eff.getX();
             mpRecover += eff.getY();
             mpRecoverProp += eff.getX();
             }
             bx = SkillFactory.getSkill(31110009);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             mpRecover += 1;
             mpRecoverProp += eff.getProb();
             }
             bx = SkillFactory.getSkill(31111006);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             dam_r *= (eff.getX() + 100.0) / 100.0;
             bossdam_r *= (eff.getX() + 100.0) / 100.0;
             passive_sharpeye_rate += eff.getY();
             }
             bx = SkillFactory.getSkill(31121006);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             ignoreTargetDEF += bx.getEffect(bof).getIgnoreMob();
             }
             bx = SkillFactory.getSkill(31120011);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             damageIncrease.put(31000004, (int) eff.getX());
             damageIncrease.put(31001006, (int) eff.getX());
             damageIncrease.put(31001007, (int) eff.getX());
             damageIncrease.put(31001008, (int) eff.getX());
             }
             bx = SkillFactory.getSkill(31120008);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             watk += eff.getAttackX();
             trueMastery += eff.getMastery();
             passive_sharpeye_min_percent += eff.getCriticalMin();
             }
             bx = SkillFactory.getSkill(31120010);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             percent_wdef += bx.getEffect(bof).getT();
             }
             bx = SkillFactory.getSkill(30010112);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             bossdam_r += eff.getBossDamage();
             mpRecover += eff.getX();
             mpRecoverProp += eff.getBossDamage(); //yes
             }
             bx = SkillFactory.getSkill(30010185);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             chra.getTrait(MapleTraitType.will).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getY()));
             chra.getTrait(MapleTraitType.charisma).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getZ()));
             }
             bx = SkillFactory.getSkill(30010111);
             bof = chra.getTotalSkillLevel(bx);
             if (bof > 0) {
             eff = bx.getEffect(bof);
             hpRecoverPercent += eff.getX();
             hpRecoverProp += eff.getProb(); //yes
             }
             //TODO LEGEND: 31121007 (consumes fury,free of fury), 31111004 (increase wdef ASR ER), 
             break;*/
            case 510:
            case 511:
            case 512: {
                bx = SkillFactory.getSkill(12);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                }
                bx = SkillFactory.getSkill(5100009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 1510:
            case 1511:
            case 1512: {
                bx = SkillFactory.getSkill(15100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 400:
            case 410:
            case 411:
            case 412: {
                bx = SkillFactory.getSkill(12);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                }
                defRange = 200;
                bx = SkillFactory.getSkill(4000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                bx = SkillFactory.getSkill(4100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                }
                break;
            }
            case 420:
            case 421:
            case 422: {
                bx = SkillFactory.getSkill(4200006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }

                bx = SkillFactory.getSkill(4210000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                }
                break;
            }
            case 431:
            case 432:
            case 433:
            case 434: {
                bx = SkillFactory.getSkill(12);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                }
                bx = SkillFactory.getSkill(4310004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                }
                bx = SkillFactory.getSkill(4341006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate();
                    percent_mdef += eff.getMDEFRate();
                }
                break;
            }
            case 100:
            case 110:
            case 111:
            case 112:
            case 120:
            case 121:
            case 122:
            case 130:
            case 131:
            case 132: {
                bx = SkillFactory.getSkill(12);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                }
                bx = SkillFactory.getSkill(1000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }

                bx = SkillFactory.getSkill(1210001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                }

                bx = SkillFactory.getSkill(1220005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_wdef += bx.getEffect(bof).getT();
                }
                bx = SkillFactory.getSkill(1220010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    trueMastery += bx.getEffect(bof).getMastery();
                }
                break;
            }
            case 322: { // Crossbowman                
                bx = SkillFactory.getSkill(12);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                }
                bx = SkillFactory.getSkill(3220004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                bx = SkillFactory.getSkill(3220009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            }
            case 312: { // Bowmaster                
                bx = SkillFactory.getSkill(12);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                }
                bx = SkillFactory.getSkill(3120005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getX();
                }
                bx = SkillFactory.getSkill(3120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            }
            case 3510:
            case 3511:
            case 3512:
                defRange = 200;
                bx = SkillFactory.getSkill(35100000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(35120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    trueMastery += bx.getEffect(bof).getMastery();
                }
                break;
            case 3211:
            case 3212:
                bx = SkillFactory.getSkill(32110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                }
                bx = SkillFactory.getSkill(32110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                bx = SkillFactory.getSkill(32120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    magic += bx.getEffect(bof).getMagicX();
                }
                bx = SkillFactory.getSkill(32120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(32120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            case 3300:
            case 3310:
            case 3311:
            case 3312:
                defRange = 200;
                bx = SkillFactory.getSkill(33120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                bx = SkillFactory.getSkill(33110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(33120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ignoreTargetDEF += eff.getIgnoreMob();
                    dodgeChance += eff.getER();
                }
                bx = SkillFactory.getSkill(32110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                break;
            case 2200:
            case 2210:
            case 2211:
            case 2212:
            case 2213:
            case 2214:
            case 2215:
            case 2216:
            case 2217:
            case 2218: {
                magic += chra.getTotalSkillLevel(SkillFactory.getSkill(22000000));
                bx = SkillFactory.getSkill(22150000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                bx = SkillFactory.getSkill(22160000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(22170001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            }
            case 2112: {
                bx = SkillFactory.getSkill(21120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            }
        }
        //샤프아이즈, 패시브
        MapleStatEffect ss = chra.getStatForBuff(MapleBuffStat.SHARP_EYES);
        if (ss != null) {
            passive_sharpeye_rate += ss.getX();
            //passive_sharpeye_percent += eff.getCriticalMax();
        }
        ss = chra.getStatForBuff(MapleBuffStat.PICKPOCKET);
        if (ss != null) {
            pickRate += ss.getProb();
        }
        Integer val = chra.getBuffedValue(MapleBuffStat.ECHO_OF_HERO);
        if (val != null) {
            watk += watk * val.intValue() / 100;
            magic += magic * val.intValue() / 100;
        }//영매 스공계산
        val = chra.getBuffedValue(MapleBuffStat.WATK);
        if (val != null) {
            watk += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.ENHANCED_WATK);
        if (val != null) {
            watk += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.ENHANCED_MAXHP);
        if (val != null) {
            localmaxhp_ += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.ENHANCED_MAXMP);
        if (val != null) {
            localmaxmp_ += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.ENHANCED_WDEF);
        if (val != null) {
            wdef += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.ENHANCED_MDEF);
        if (val != null) {
            mdef += val.intValue();
        }
        ss = chra.getStatForBuff(MapleBuffStat.ENERGY_CHARGE);
        if (ss != null) {
            watk += ss.getWatk();
            accuracy += ss.getAcc();
        }
        val = chra.getBuffedValue(MapleBuffStat.DRAGONBLOOD);
        if (val != null) {
            watk += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.MATK);
        if (val != null) {
            magic += val.intValue();
        }
        val = chra.getBuffedSkill_X(MapleBuffStat.COMBAT_ORDERS);
        if (val != null) {
            combatOrders += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.CONVERSION);
        if (val != null) {
            percent_hp += val.intValue();
        } else {
            val = chra.getBuffedValue(MapleBuffStat.MAXHP);
            if (val != null) {
                percent_hp += val.intValue();
            }
        }
        val = chra.getBuffedValue(MapleBuffStat.MAXMP);
        if (val != null) {
            percent_mp += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.MP_BUFF);
        if (val != null) {
            percent_mp += val.intValue();
        }
        val = chra.getBuffedValue(MapleBuffStat.CONCENTRATE);
        if (val != null) {
            mpconReduce = val;
        }
        val = chra.getBuffedValue(MapleBuffStat.MESOGUARD);
        if (val != null) {
            mesoGuardMeso = val / 100.0;
        }
        val = chra.getBuffedValue(MapleBuffStat.ACC);
        if (val != null) {
            accuracy += val;
        }
        val = chra.getBuffedValue(MapleBuffStat.EXPRATE);
        if (val != null) {
            expBuff += val.doubleValue() - 100;
        }

        if (chra.getSkillLevel(3000001) > 0) {
            ss = SkillFactory.getSkill(3000001).getEffect(chra.getSkillLevel(3000001));
            passive_sharpeye_rate += ss.getProb();
            passive_sharpeye_damage += ss.getDamage();
        }
        if (chra.getSkillLevel(4100001) > 0) {
            ss = SkillFactory.getSkill(4100001).getEffect(chra.getSkillLevel(4100001));
            passive_sharpeye_rate += ss.getProb();
            passive_sharpeye_damage += ss.getDamage();
        }
        if (chra.getSkillLevel(15110000) > 0) {
            ss = SkillFactory.getSkill(15110000).getEffect(chra.getSkillLevel(15110000));
            passive_sharpeye_rate += ss.getProb();
            passive_sharpeye_damage += ss.getDamage();
        }

        localmaxhp_ += Math.floor((percent_hp * localmaxhp_) / 100.0f);//hp 총결산
        localmaxmp_ += Math.floor((percent_mp * localmaxmp_) / 100.0f);//mp 총결산
        localmaxhp = Math.min(99999, Math.abs(Math.max(-99999, localmaxhp_)));
        localmaxmp = Math.min(99999, Math.abs(Math.max(-99999, localmaxmp_)));

        //damage increase
        switch (chra.getJob()) {
            case 210:
            case 211:
            case 212: { // IL
                /*bx = SkillFactory.getSkill(2110000);
                 bof = chra.getTotalSkillLevel(bx);
                 if (bof > 0) {
                 eff = bx.getEffect(bof);
                 dotTime += eff.getX();
                 dot += eff.getZ();
                 }*/ //패럴라이즈 데몬 어쩌고 도트지속증가 
                bx = SkillFactory.getSkill(2110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                /*bx = SkillFactory.getSkill(2121003);
                 bof = chra.getTotalSkillLevel(bx);
                 if (bof > 0) {
                 eff = bx.getEffect(bof);
                 damageIncrease.put(2111003, (int) eff.getX());
                 }*/ //뭐지 이건
                bx = SkillFactory.getSkill(2120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                break;
            }
            case 220:
            case 221:
            case 222: { // IL
                /*bx = SkillFactory.getSkill(2210000);
                 bof = chra.getTotalSkillLevel(bx);
                 if (bof > 0) {
                 dot += bx.getEffect(bof).getZ();
                 }*/ //도트지속스킬은 없음
                bx = SkillFactory.getSkill(2210001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                bx = SkillFactory.getSkill(2220009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                break;
            }
            case 1211:
            case 1212: { // flame
                bx = SkillFactory.getSkill(12110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                break;
            }
            case 230:
            case 231:
            case 232: { // Bishop
                bx = SkillFactory.getSkill(2310008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    passive_sharpeye_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(2320010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                bx = SkillFactory.getSkill(2321010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    magic += bx.getEffect(bof).getMagicX();
                }
                bx = SkillFactory.getSkill(2320005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                }
                bx = SkillFactory.getSkill(2320011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    ignoreTargetDEF += eff.getIgnoreMob();
                }
                break;
            }
            case 1300:
            case 1310:
            case 1311:
            case 1312:
                defRange = 200;
                bx = SkillFactory.getSkill(13000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                bx = SkillFactory.getSkill(13110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(13110003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            case 300:
            case 310:
            case 311:
            case 312:
                defRange = 200;
                bx = SkillFactory.getSkill(3000002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                bx = SkillFactory.getSkill(3100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(3001004, eff.getX());
                    damageIncrease.put(3001005, eff.getY());
                }
                bx = SkillFactory.getSkill(3110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(3120005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                break;
            case 320:
            case 321:
            case 322:
                defRange = 200;
                bx = SkillFactory.getSkill(3000002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                bx = SkillFactory.getSkill(3200006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(3001004, eff.getX());
                    damageIncrease.put(3001005, eff.getY());
                }
                bx = SkillFactory.getSkill(3220010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(3211006, bx.getEffect(bof).getDamage() - 150);
                }
                bx = SkillFactory.getSkill(3210007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                break;

            case 422:
                bx = SkillFactory.getSkill(4221007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Savage Blow, Steal, and Assaulter
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4201005, (int) eff.getDAMRate());
                    damageIncrease.put(4201004, (int) eff.getDAMRate());
                    damageIncrease.put(4211002, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(4220009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mesoBuff *= (eff.getMesoRate() + 100.0) / 100.0;
                    pickRate += eff.getU();
                    mesoGuard += eff.getV();
                    mesoGuardMeso -= eff.getW() / 100.0;
                    damageIncrease.put(4211006, eff.getX());
                }
                break;
            case 433:
            case 434:
                bx = SkillFactory.getSkill(4330007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    hpRecoverProp += eff.getProb();
                    hpRecoverPercent += eff.getX();
                }
                bx = SkillFactory.getSkill(4341002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Fatal Blow, Slash Storm, Tornado Spin, Bloody Storm, Upper Stab, and Flying Assaulter
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4311002, (int) eff.getDAMRate());
                    damageIncrease.put(4311003, (int) eff.getDAMRate());
                    damageIncrease.put(4321000, (int) eff.getDAMRate());
                    damageIncrease.put(4321001, (int) eff.getDAMRate());
                    damageIncrease.put(4331000, (int) eff.getDAMRate());
                    damageIncrease.put(4331004, (int) eff.getDAMRate());
                    damageIncrease.put(4331005, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(4341006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                break;
            case 2110:
            case 2111:
            case 2112: { // Aran
                bx = SkillFactory.getSkill(21101006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(21110002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(21000004, bx.getEffect(bof).getW());
                }
                bx = SkillFactory.getSkill(21111010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ignoreTargetDEF += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(21120002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(21100007, bx.getEffect(bof).getZ());
                }
                bx = SkillFactory.getSkill(21120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(21100002, (int) eff.getDAMRate());
                    damageIncrease.put(21110003, (int) eff.getDAMRate());
                }
                break;
            }
            case 3511:
            case 3512:
                bx = SkillFactory.getSkill(35110014);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //ME-07 Drillhands, Atomic Hammer
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35001003, (int) eff.getDAMRate());
                    damageIncrease.put(35101003, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(35121006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Satellite
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35111001, (int) eff.getDAMRate());
                    damageIncrease.put(35111009, (int) eff.getDAMRate());
                    damageIncrease.put(35111010, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(35120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Satellite
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35111005, eff.getX());
                    damageIncrease.put(35111011, eff.getX());
                    damageIncrease.put(35121009, eff.getX());
                    damageIncrease.put(35121010, eff.getX());
                    damageIncrease.put(35121011, eff.getX());
                    //BuffUP_Summon += eff.getY();
                }
                break;
            case 110:
            case 111:
            case 112:
                bx = SkillFactory.getSkill(1100009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(1001004, eff.getX());
                    damageIncrease.put(1001005, eff.getY());
                }
                bx = SkillFactory.getSkill(1110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= eff.getDamage() / 100.0;
                    bossdam_r *= eff.getDamage() / 100.0;
                }
                bx = SkillFactory.getSkill(1120012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ignoreTargetDEF += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(1120013);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    damageIncrease.put(1100002, (int) eff.getDamage());
                }
                break;
            case 120:
            case 121:
            case 122:
                bx = SkillFactory.getSkill(1200009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(1001004, eff.getX());
                    damageIncrease.put(1001005, eff.getY());
                }
                bx = SkillFactory.getSkill(1220006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                }
                eff = chra.getStatForBuff(MapleBuffStat.DIVINE_SHIELD);
                if (eff != null) {
                    watk += eff.getEnhancedWatk();
                }
                break;
            case 511:
            case 512:
                bx = SkillFactory.getSkill(5110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Backspin Blow, Double Uppercut, and Corkscrew Blow
                    eff = bx.getEffect(bof);
                    damageIncrease.put(5101002, eff.getX());
                    damageIncrease.put(5101003, eff.getY());
                    damageIncrease.put(5101004, eff.getZ());
                }
                break;
            case 520:
            case 521:
            case 522:
                defRange = 200;
                bx = SkillFactory.getSkill(5220001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Flamethrower and Ice Splitter
                    eff = bx.getEffect(bof);
                    damageIncrease.put(5211004, (int) eff.getDamage());
                    damageIncrease.put(5211005, (int) eff.getDamage());
                }
                break;
            case 130:
            case 131:
            case 132:
                bx = SkillFactory.getSkill(1300009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(1001004, eff.getX());
                    damageIncrease.put(1001005, eff.getY());
                }
                bx = SkillFactory.getSkill(1310009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    passive_sharpeye_rate += eff.getCr();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                    hpRecoverProp += eff.getProb();
                    hpRecoverPercent += eff.getX();
                }
                bx = SkillFactory.getSkill(1320006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                break;
            case 411:
            case 412:
                bx = SkillFactory.getSkill(4110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    //RecoveryUP += eff.getX() - 100;
                    BuffUP += eff.getY() - 100;
                }
                bx = SkillFactory.getSkill(4120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Lucky Seven, Drain, Avenger, Shadow Meso
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4001344, (int) eff.getDAMRate());
                    damageIncrease.put(4101005, (int) eff.getDAMRate());
                    damageIncrease.put(4111004, (int) eff.getDAMRate());
                    damageIncrease.put(4111005, (int) eff.getDAMRate());
                }
                break;
            case 1400:
            case 1410:
            case 1411:
            case 1412:
                defRange = 200;
                bx = SkillFactory.getSkill(14110003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    RecoveryUP += eff.getX() - 100;
                    BuffUP += eff.getY() - 100;
                }
                bx = SkillFactory.getSkill(14000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                break;
        }
        if (GameConstants.isResist(chra.getJob())) {
            bx = SkillFactory.getSkill(30000002);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                RecoveryUP += bx.getEffect(bof).getX() - 100;
            }
        }

        if (first_login) {
            chra.silentEnforceMaxHpMp();
            relocHeal(chra);
        } else {
            chra.enforceMaxHpMp();
        }

        //magic += localint_;//마력은 마지막에 처리
        //     localminbasedamage = calculateMinBaseDamage(watk, chra);
        //calculateMaxBaseDamage(chra);
        //     trueMastery = calculateMastery(chra)
//        localminbasedamage = calculateMinBaseDamage(watk, chra);
//        localmaxbasedamage = calculateMaxBaseDamage(watk, chra);
        trueMastery = calculateMastery(chra);
        passive_sharpeye_min_percent = (short) Math.min(passive_sharpeye_min_percent, passive_sharpeye_percent);
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            chra.updatePartyMemberHP();
        }
    }

    private int calculateMastery(MapleCharacter chra) {
        if (chra == null) {
            return 0;
        }
        int mastery;
        final Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        if (weapon_item != null) {
            mastery = DamageParse.getMastery(chra, weapon_item);
        } else {
            mastery = 0;
        }
        return mastery;
    }

    public short getSharpEyeRate() {
        return passive_sharpeye_rate;
    }

    public int getSharpEyeDam() {
        return passive_sharpeye_damage;
    }

    public final void calculateMaxBaseDamage(MapleCharacter chra) {
        // Recompute from current stats every call to avoid using stale cached values.
        localmaxbasedamage = 0;
        localmaxmagicdamage = 0;
        localmaxbasepvpdamage = 0;
        localmaxbasepvpdamageL = 0;
        if (watk <= 0 && magic <= 0) {
            localmaxbasedamage = 1;
            localmaxmagicdamage = 1;
            localmaxbasepvpdamage = 1;
        } else {
            final Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            final Item weapon_item2 = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
            final int job = chra.getJob();
            final MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.NOT_A_WEAPON : GameConstants.getWeaponType(weapon_item.getItemId());
            final MapleWeaponType weapon2 = weapon_item2 == null ? MapleWeaponType.NOT_A_WEAPON : GameConstants.getWeaponType(weapon_item2.getItemId());
            int mainstat, secondarystat, mainstatpvp, secondarystatpvp;
            final boolean mage = (job >= 200 && job <= 232) || (job >= 1200 && job <= 1212) || (job >= 2200 && job <= 2218) || (job >= 3200 && job <= 3212);
            switch (weapon) {
                case BOW:
                case CROSSBOW:
                case GUN:
                    mainstat = localdex;
                    secondarystat = localstr;
                    mainstatpvp = dex;
                    secondarystatpvp = str;
                    break;
                case DAGGER:
                case KATARA:
                case CLAW:
                    mainstat = localluk;
                    secondarystat = localdex + localstr;
                    mainstatpvp = luk;
                    secondarystatpvp = dex + str;
                    break;
                default:
                    if (mage) {
                        mainstat = localint_;
                        secondarystat = localluk;
                        mainstatpvp = int_;
                        secondarystatpvp = luk;
                    } else {
                        mainstat = localstr;
                        secondarystat = localdex;
                        mainstatpvp = str;
                        secondarystatpvp = dex;
                    }
                    break;
            }
            localmaxbasepvpdamage = weapon.getMaxDamageMultiplier() * (4 * mainstatpvp + secondarystatpvp) * (100.0f + (pvpDamage / 100.0f));
            localmaxbasepvpdamageL = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (100.0f + (pvpDamage / 100.0f));
            if (weapon2 != MapleWeaponType.NOT_A_WEAPON && weapon_item != null && weapon_item2 != null) {
                Equip we1 = (Equip) weapon_item;
                Equip we2 = (Equip) weapon_item2;
                localmaxbasedamage = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * ((watk - (mage ? we2.getMatk() : we2.getWatk())) / 100.0f);
                localmaxbasedamage += weapon2.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * ((watk - (mage ? we1.getMatk() : we1.getWatk())) / 100.0f);
            } else if (mage) {
                localmaxmagicdamage = Math.max(1, (float) Math.ceil(weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (mage ? magic : watk) / 100.0f));
                localmaxbasedamage = localmaxmagicdamage;
            } else {
                //localmaxbasedamage = (float) Math.ceil(weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (mage ? magic : watk) / 100.0f);
                localmaxbasedamage = ((0 + secondarystat + 4 * mainstat) * (watk * weapon.getMaxDamageMultiplier()) * 0.01 + 0.5);//ida기준
            }
        }
        //if (chra.isGM()) {///getDebug(2)) {
        //chra.dropMessage(5, "마력: " + magic + " 스공 " + (float) localmaxbasedamage + " / 마공 " + (float) localmaxmagicdamage);
        //}
    }

    public final double calculateMinBaseDamage(final int watk, MapleCharacter chra) {
        if (chra == null) {
            return 0;
        }
        double minbasedamage;
        if (watk == 0) {
            minbasedamage = 1;
        } else {
            final Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weapon_item != null) {
                minbasedamage = localmaxbasedamage * DamageParse.getMastery(chra, weapon_item) * 0.009000000000000001;
            } else {
                minbasedamage = 0;
            }
            if (minbasedamage >= 99999) {
                minbasedamage = 99999;
            }
        }
        return minbasedamage;
    }

    public final int getMastery() {
        return trueMastery;
    }

    public final float getHealHP() {
        return shouldHealHP;
    }

    public final float getHealMP() {
        return shouldHealMP;
    }

    public final void relocHeal(MapleCharacter chra) {
        final int playerjob = chra.getJob();

        shouldHealHP = 10; // Reset
        shouldHealMP = GameConstants.isDemon(chra.getJob()) ? 0 : (3 + mpRestore + recoverMP + (localint_ / 10)); // i think
        mpRecoverTime = 0;
        hpRecoverTime = 0;
        if (playerjob == 111 || playerjob == 112) {
            final Skill effect = SkillFactory.getSkill(1110000); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                MapleStatEffect eff = effect.getEffect(lvl);
                if (eff.getHp() > 0) {
                    shouldHealHP += eff.getHp();
                    hpRecoverTime = 4000;
                }
                shouldHealMP += eff.getMp();
                mpRecoverTime = 4000;
            }
        }
        if (chra.getChair() != 0) { // Is sitting on a chair.
            shouldHealHP += 99; // Until the values of Chair heal has been fixed,
            shouldHealMP += 99; // MP is different here, if chair data MP = 0, heal + 1.5
        } else if (chra.getMap() != null) { // Because Heal isn't multipled when there's a chair :)
            final float recvRate = chra.getMap().getRecoveryRate();
            if (recvRate > 0) {
                shouldHealHP *= recvRate;
                shouldHealMP *= recvRate;
            }
        }
    }

    public final void connectData(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(str); // str
        mplew.writeShort(dex); // dex
        mplew.writeShort(int_); // int
        mplew.writeShort(luk); // luk
        mplew.writeInt(hp); // hp -- INT after bigbang
        mplew.writeInt(maxhp); // maxhp
        mplew.writeInt(mp); // mp
        mplew.writeInt(maxmp); // maxmp
    }

    private final static int[] allJobs = {0, 10000, 10000000, 20000000, 20010000, 20020000, 30000000, 30010000};
    public final static int[] pvpSkills = {1000007, 2000007, 3000006, 4000010, 5000006, 5010004, 11000006, 12000006, 13000005, 14000006, 15000005, 21000005, 22000002, 23000004, 31000005, 32000012, 33000004, 35000005};

    public final static int getSkillByJob(final int skillID, final int job) {
        if (GameConstants.isKOC(job)) {
            return skillID + 10000000;
        } else if (GameConstants.isAran(job)) {
            return skillID + 20000000;
        } else if (GameConstants.isEvan(job)) {
            return skillID + 20010000;
        } else if (GameConstants.isMercedes(job)) {
            return skillID + 20020000;
        } else if (GameConstants.isDemon(job)) {
            return skillID + 30010000;
        } else if (GameConstants.isResist(job)) {
            return skillID + 30000000;
            //} else if (GameConstants.isCannon(job)) {
            //    return skillID + 10000;
        }
        return skillID;
    }

    public final int getSkillIncrement(final int skillID) {
        if (skillsIncrement.containsKey(skillID)) {
            return skillsIncrement.get(skillID);
        }
        return 0;
    }

    public final int getAccuracy() {
        return accuracy;
    }

    public void heal_noUpdate(MapleCharacter chra) {
        setHp(getCurrentMaxHp(), chra);
        setMp(getCurrentMaxMp(), chra);
    }

    public void heal(MapleCharacter chra) {
        heal_noUpdate(chra);
        chra.updateSingleStat(MapleStat.HP, getCurrentMaxHp());
        chra.updateSingleStat(MapleStat.MP, getCurrentMaxMp());
    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / localmaxhp);
    }

    public int getMPPercent() {
        return (int) (mp * 100.0 / localmaxmp);
    }

    public boolean checkEquipLevels(final MapleCharacter chr, int gain) {
        boolean changed = false;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
//        equipLevelHandling.clear();
//        recalcLocalStats(chr);
        List<Equip> all = new ArrayList<>(equipLevelHandling);
        for (Equip eq : all) {
            int lvlz = eq.getEquipLevel();
            eq.setItemEXP(eq.getItemEXP() + gain);

            if (eq.getEquipLevel() > lvlz) { //lvlup
                for (int i = eq.getEquipLevel() - lvlz; i > 0; i--) {
                    //now for the equipment increments...
                    final Map<Integer, Map<String, Integer>> inc = ii.getEquipIncrements(eq.getItemId());
                    if (inc != null && inc.containsKey(lvlz + i)) { //flair = 1
                        eq = ii.levelUpEquip(eq, inc.get(lvlz + i));
                    }
                    //UGH, skillz
                    if (GameConstants.getStatFromWeapon(eq.getItemId()) == null //null 방지
                            && GameConstants.getMaxLevel(eq.getItemId()) < (lvlz + i) //레벨업 했다는 뜻
                            && Randomizer.rand(1, 100) < 9 //확률 위젯에 9로 돼있는데 캐싱하는게 좋은듯 
                            && eq.getIncSkill() <= 0 //스킬증가가 없을떄
                            && ii.getEquipSkills(eq.getItemId()) != null //장비템에 스킬증가 옵션이 있다면
                            ) {
                        for (int zzz : ii.getEquipSkills(eq.getItemId())) {
                            final Skill skil = SkillFactory.getSkill(zzz);
                            if (skil != null && skil.canBeLearnedBy(chr.getJob())) { //dont go over masterlevel :D
                                eq.setIncSkill(skil.getId());
                            }
                        }
                    }
                }
                changed = true;
            }
            chr.forceReAddItem(eq.copy(), MapleInventoryType.EQUIPPED);
        }
        if (changed) {
            chr.equipChanged();
            chr.getClient().getSession().write(MaplePacketCreator.showItemLevelupEffect());
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showForeignItemLevelupEffect(chr.getId()), false);
        }
        return changed;
    }

    public void checkEquipDurabilitys(final MapleCharacter chr, int gain, boolean aboveZero, boolean dead) {
        List<Equip> all = new ArrayList<>(durabilityHandling);
        for (Equip item : all) {
            if (item != null) {
                if (item.getDurability() != -1) {
                    if (dead) {
                        chr.dropMessage(5, "사망하여 " + MapleItemInformationProvider.getInstance().getName(item.getItemId()) + " 아이템의 내구도가 감소합니다.");
                    }
                    int dura = item.getDurability();
                    dura += gain;
                    if (dura < 1) {
                        dura = 0;
                        chr.dropMessage(5, MapleItemInformationProvider.getInstance().getName(item.getItemId()) + " 아이템의 내구도가 0이 되었습니다. (아이템제작 NPC를 통해 수리 가능)");
                    }
                    item.setDurability(dura);
                    chr.forceReAddItem(item.copy(), MapleInventoryType.EQUIPPED);
                }
            }
        }
    }

    public void createOrUpdateSetOptionEquip(MapleCharacter chra) {
        createOrUpdateSetOptionEquip(chra, false);
    }

    public void createOrUpdateSetOptionEquip(MapleCharacter chra, boolean silent) {
        Equip eqp = null;
        for (SetItemInfo setItemInfo : ItemInfo.setItemInfoList) {
            int count = setItemInfo.totalCount(chra.getInventory(MapleInventoryType.EQUIPPED)::countById);
            if (count > 0) {
                eqp = setItemInfo.totalEquip(eqp, count);
                if (!silent) {
                    chra.dropMessage(5, setItemInfo.getName() + " " + count + "개를 장착하고 있습니다.");
                }
            }
        }
        if (eqp != null) {
            if (chra.getInventory(MapleInventoryType.EQUIPPED).addFromDB(eqp)) {
                //chra.getClient().sendPacket(MaplePacketCreator.clearInventoryItem(MapleInventoryType.EQUIP, (short) -122, false));
            }
            //chra.getClient().sendPacket(MaplePacketCreator.addInventorySlot(MapleInventoryType.EQUIP, eqp, false));
        } else {
            if (chra.getInventory(MapleInventoryType.EQUIPPED).removeSlot((short) -122)) {
                //chra.getClient().sendPacket(MaplePacketCreator.clearInventoryItem(MapleInventoryType.EQUIP, (short) -122, false));
            }
        }
    }

    public final boolean setHp(final int newhp, MapleCharacter chra) {
        return setHp(newhp, false, chra);
    }

    public final boolean setHp(int newhp, boolean silent, MapleCharacter chra) {
        final int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
            //System.out.println("localmaxhp: " + localmaxhp + " thp: " + thp);
        }
        this.hp = thp;

        if (chra != null) {
            if (!silent) {
                chra.checkBerserk();
                chra.updatePartyMemberHP();
            }
            if (oldHp > hp && !chra.isAlive()) {
                chra.playerDead();
            }
        }
        return hp != oldHp;
    }

    public final boolean setMp(final int newmp, final MapleCharacter chra) {
        final int oldMp = mp;
        int tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > localmaxmp) {
            tmp = localmaxmp;
        }
        this.mp = tmp;
        if (chra != null) {
            chra.checkFury(false);
        }
        return mp != oldMp;
    }

    public void handlePotential(StructPotentialItem pot, MapleCharacter chra, boolean first_login) {
        localstr += pot.incSTR;
        localdex += pot.incDEX;
        localint_ += pot.incINT;
        localluk += pot.incLUK;
        wdef += pot.incPDD;
        mdef += pot.incMDD;
        watk += pot.incPAD;
        magic += pot.incMAD;
        accuracy += pot.incACC;
        incAllskill += pot.incAllskill;
        ignoreTargetDEF += pot.ignoreTargetDEF;
        bossdam_r *= (pot.incDAMr + 100.0) / 100.0;
        if (!pot.boss) {
            dam_r *= (pot.incDAMr + 100.0) / 100.0;
        }
        recoverHP += pot.RecoveryHP;
        recoverMP += pot.RecoveryMP;
        RecoveryUP += pot.RecoveryUP;
        RecoveryUP_Skill += pot.RecoveryUP;
        if (pot.HP > 0) {
            hpRecoverPotential += pot.HP;
            hpRecoverPropPotential += pot.prop;
        }
        if (pot.MP > 0 && !GameConstants.isDemon(chra.getJob())) {
            mpRecoverPotential += pot.MP;
            mpRecoverPropPotential += pot.prop;
        }
        if (pot.time > 0 && pot.prop == 0) {
            decreaseDebuff += pot.time * 1000;
        }
        mpconReduce += pot.mpconReduce;
        incMesoProp += pot.incMesoProp;
        incRewardProp += pot.incRewardProp;
        if (pot.potentialID == 20656 || pot.potentialID == 30656) {
            // Force these custom potential lines to contribute exactly +10% item drop each.
            incRewardProp += (10 - pot.incRewardProp);
        }
        if (pot.DAMreflect > 0) {
            DAMreflect += pot.DAMreflect;
            DAMreflect_rate += pot.prop;
        }
        percent_hp += pot.incMHPr;
        percent_mp += pot.incMMPr;
        percent_str += pot.incSTRr;
        percent_dex += pot.incDEXr;
        percent_int += pot.incINTr;
        percent_luk += pot.incLUKr;
        percent_acc += pot.incACCr;
        percent_atk += pot.incPADr;
        percent_matk += pot.incMADr;
        percent_wdef += pot.incPDDr;
        percent_mdef += pot.incMDDr;
        passive_sharpeye_rate += pot.incCr;
        mpRestore += pot.mpRestore;
        if (first_login && pot.skillID > 0) {
            chra.changeSkillLevel_Skip(SkillFactory.getSkill(getSkillByJob(pot.skillID, chra.getJob())), (byte) 1, (byte) 0);
        }
    }
}
